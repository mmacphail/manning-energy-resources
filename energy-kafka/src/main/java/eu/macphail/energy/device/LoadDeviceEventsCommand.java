package eu.macphail.energy.device;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import eu.macphail.energy.EnergyKafkaConfiguration;
import eu.macphail.energy.RawEnergyEvent;
import io.confluent.kafka.serializers.KafkaAvroDeserializer;
import io.confluent.kafka.serializers.KafkaAvroDeserializerConfig;
import io.confluent.kafka.serializers.KafkaAvroSerializerConfig;
import io.dropwizard.Application;
import io.dropwizard.cli.EnvironmentCommand;
import io.dropwizard.db.DataSourceFactory;
import io.dropwizard.setup.Environment;
import net.sourceforge.argparse4j.inf.Namespace;
import net.sourceforge.argparse4j.inf.Subparser;
import org.apache.kafka.clients.consumer.*;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.sqlobject.SqlObjectPlugin;

import java.io.IOException;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;

public class LoadDeviceEventsCommand extends EnvironmentCommand<EnergyKafkaConfiguration> {
    public LoadDeviceEventsCommand(Application<EnergyKafkaConfiguration> application) {
        super(application, "load-device-events", "Load the database with device charging events from the Kafka topic");
    }

    @Override
    public void configure(Subparser subparser) {
        super.configure(subparser);
    }

    @Override
    protected void run(Environment environment, Namespace namespace, EnergyKafkaConfiguration config) throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();
        DataSourceFactory dsf = config.getDataSourceFactory();
        Jdbi jdbi = Jdbi.create(dsf.getUrl(), dsf.getUser(), dsf.getPassword())
                .installPlugin(new SqlObjectPlugin());

        DeviceDAO deviceDAO = jdbi.onDemand(DeviceDAO.class);

        Properties props = configureKafkaProperties(config);

        jdbi.useTransaction(handle -> consumeDeviceEvents(handle, config, objectMapper, deviceDAO, props));
    }

    private void consumeDeviceEvents(Handle handle, EnergyKafkaConfiguration config, ObjectMapper objectMapper, DeviceDAO deviceDAO, Properties props) {
        KafkaConsumer<String, RawEnergyEvent> consumer = new KafkaConsumer<>(props);
        try {

            consumer.subscribe(Arrays.asList(config.getKafkaRawEnergyEventTopicName()), new ConsumerRebalanceListener() {
                @Override
                public void onPartitionsRevoked(Collection<TopicPartition> collection) {
                    consumer.commitSync();
                }

                @Override
                public void onPartitionsAssigned(Collection<TopicPartition> collection) {

                }
            });

            while (true) {
                final ConsumerRecords<String, RawEnergyEvent> records = consumer.poll(Duration.ofMillis(100));
                handle.begin();
                for (ConsumerRecord<String, RawEnergyEvent> record : records) {
                    RawEnergyEvent rawEnergyEvent = record.value();
                    String payload = parsePayload(rawEnergyEvent.getPayload().toString());
                    List<Device> devices = deserializeDevices(objectMapper, payload);

                    devices.forEach(device -> upsertDevice(deviceDAO, rawEnergyEvent, device));
                }
                handle.commit();
            }
        } catch (Exception e) {
            handle.rollback();
            e.printStackTrace();
        } finally {
            consumer.close(Duration.ofMinutes(5));
        }
    }

    private Properties configureKafkaProperties(EnergyKafkaConfiguration config) {
        Properties props = new Properties();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, config.getKafkaBootstrapServer());
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "charging-command");
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, KafkaAvroDeserializer.class);
        props.put(KafkaAvroDeserializerConfig.SPECIFIC_AVRO_READER_CONFIG, "true");
        props.put(KafkaAvroSerializerConfig.SCHEMA_REGISTRY_URL_CONFIG, config.getKafkaSchemaRegistryUrl());
        props.put(ConsumerConfig.ISOLATION_LEVEL_CONFIG, "read_committed");
        // props.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, 10000);
        props.put(ConsumerConfig.FETCH_MIN_BYTES_CONFIG, 500);
        props.put(ConsumerConfig.FETCH_MAX_WAIT_MS_CONFIG, 200);
        return props;
    }

    private String parsePayload(String payload) {
        return "[" + String.join(",", payload.split("\n")) + "]";
    }

    private List<Device> deserializeDevices(ObjectMapper objectMapper, String payload) throws IOException {
        JsonNode array = objectMapper.readTree(payload);
        List<Device> deviceList = new ArrayList<>();
        for(int i=0; i<array.size(); i++) {
            JsonNode node = array.get(i);
            deviceList.add(new Device(node.get("device_id").asText(), node.get("charging").asLong()));
        }
        return deviceList;
    }

    private void upsertDevice(DeviceDAO deviceDAO, RawEnergyEvent rawEnergyEvent, Device device) {
        Timestamp eventTimestamp = Timestamp.from(Instant.ofEpochMilli(rawEnergyEvent.getReceivedTimestamp()));
        deviceDAO.upsertDevice(
                device.getDeviceID(),
                eventTimestamp,
                rawEnergyEvent.getRegionId().toString(),
                device.getCharging().intValue()
        );
    }
}
