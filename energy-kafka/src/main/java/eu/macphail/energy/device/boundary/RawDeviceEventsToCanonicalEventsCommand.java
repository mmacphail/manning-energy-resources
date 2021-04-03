package eu.macphail.energy.device.boundary;

import eu.macphail.energy.device.control.AppConfiguration;
import eu.macphail.energy.device.control.InvalidMessageHandler;
import eu.macphail.energy.device.control.InvalidMessageProducer;
import eu.macphail.energy.device.control.RawEventParsedSerde;
import eu.macphail.energy.device.entity.Device;
import eu.macphail.energy.device.entity.DeviceEvent;
import eu.macphail.energy.device.entity.RawDeviceEvent;
import eu.macphail.energy.device.entity.RawDeviceEventParsed;
import io.confluent.kafka.serializers.AbstractKafkaAvroSerDeConfig;
import io.confluent.kafka.streams.serdes.avro.SpecificAvroSerde;
import io.dropwizard.Application;
import io.dropwizard.cli.EnvironmentCommand;
import io.dropwizard.setup.Environment;
import net.sourceforge.argparse4j.impl.Arguments;
import net.sourceforge.argparse4j.inf.Namespace;
import net.sourceforge.argparse4j.inf.Subparser;
import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.*;
import org.apache.kafka.streams.kstream.Consumed;
import org.apache.kafka.streams.kstream.KStream;
import org.apache.kafka.streams.kstream.Produced;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.CountDownLatch;
import java.util.stream.Collectors;

public class RawDeviceEventsToCanonicalEventsCommand extends EnvironmentCommand<AppConfiguration> {
    public RawDeviceEventsToCanonicalEventsCommand(Application<AppConfiguration> application) {
        super(application, "raw-device-events-to-canonical-events", "Parse the raw device events and process them as canonical events");
    }

    @Override
    public void configure(Subparser subparser) {
        super.configure(subparser);
        subparser.addArgument("-s", "--slow")
                .action(Arguments.storeTrue())
                .help("Launches the command from the slow lane");
    }

    @Override
    protected void run(Environment environment, Namespace namespace, AppConfiguration config) throws Exception {
        final Serde<String> stringSerde = Serdes.String();
        final Map<String, String> serdeConfig = Collections.singletonMap("schema.registry.url", config.getKafkaSchemaRegistryUrl());
        final Serde<RawDeviceEvent> rawEventSerde = new SpecificAvroSerde<>();
        rawEventSerde.configure(serdeConfig, false);
        final Serde<DeviceEvent> deviceEventSerde = new SpecificAvroSerde<>();
        deviceEventSerde.configure(serdeConfig, false);
        final RawEventParsedSerde rawEventParsedSerde = new RawEventParsedSerde(rawEventSerde);

        StreamsBuilder builder = new StreamsBuilder();

        boolean slow = namespace.getBoolean("slow");
        String topic = slow ? config.getKafkaSlowRawDeviceEventTopicName() : config.getKafkaRawDeviceEventTopicName();
        KStream<String, DeviceEvent> deviceEventStream = builder.stream(topic,
                Consumed.with(stringSerde, rawEventParsedSerde))
                .flatMap(this::rawParsedEventToDeviceEvents);

        deviceEventStream.to(config.getKafkaDeviceEventTopicName(), Produced.with(stringSerde, deviceEventSerde));

        Topology topology = builder.build();
        System.out.println(topology.describe().toString());

        Properties props = configureKafkaProperties(config);
        KafkaStreams app = new KafkaStreams(topology, props);
        final CountDownLatch latch = new CountDownLatch(1);

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            app.close();
            latch.countDown();
        }));

        app.start();
        latch.await();
    }

    private List<KeyValue<String, DeviceEvent>> rawParsedEventToDeviceEvents(String key, RawDeviceEventParsed rawDeviceEvent) {
        List<Device> devices = rawDeviceEvent.getDevices();
        return devices.stream().map(device -> deviceEventFromRaw(rawDeviceEvent, device))
                .map(deviceEvent -> KeyValue.pair(deviceEvent.getDeviceID().toString(), deviceEvent))
                .collect(Collectors.toList());
    }

    private DeviceEvent deviceEventFromRaw(RawDeviceEventParsed event, Device device) {
        return new DeviceEvent(
                device.getDeviceID(),
                event.getReceivedTimestamp(),
                event.getRegionId(),
                device.getCharging().intValue()
        );
    }

    private Properties configureKafkaProperties(AppConfiguration config) {
        Properties props = new Properties();
        props.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, config.getKafkaBootstrapServer());
        props.put(StreamsConfig.APPLICATION_ID_CONFIG, "raw-devices-to-canonical-stream");
        props.put(StreamsConfig.EXACTLY_ONCE, true);
        props.put(AbstractKafkaAvroSerDeConfig.SCHEMA_REGISTRY_URL_CONFIG, config.getKafkaSchemaRegistryUrl());
        props.put(StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG, Serdes.String().getClass().getName());
        props.put(StreamsConfig.DEFAULT_DESERIALIZATION_EXCEPTION_HANDLER_CLASS_CONFIG, InvalidMessageHandler.class);
        props.put("invalidMessageManager", new InvalidMessageProducer(config));

        return props;
    }
}
