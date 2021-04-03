package eu.macphail.energy.device.control;

import eu.macphail.energy.device.control.AppConfiguration;
import eu.macphail.energy.device.entity.RawDeviceEvent;
import io.confluent.kafka.serializers.KafkaAvroSerializer;
import io.confluent.kafka.serializers.KafkaAvroSerializerConfig;
import io.dropwizard.lifecycle.Managed;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;

import java.time.Duration;
import java.util.Properties;

public class DeviceEventKafkaProducer implements Managed {

    private final AppConfiguration configuration;
    private final String rawEventTopic;
    private final String slowRawEventTopic;
    private KafkaProducer<String, RawDeviceEvent> producer;

    public DeviceEventKafkaProducer(AppConfiguration config) {
        this.configuration = config;
        this.rawEventTopic = configuration.getKafkaRawDeviceEventTopicName();
        this.slowRawEventTopic = configuration.getKafkaSlowRawDeviceEventTopicName();
    }

    @Override
    public void start() throws Exception {
        Properties props = new Properties();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, configuration.getKafkaBootstrapServer());
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringSerializer");
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, KafkaAvroSerializer.class);
        props.put(KafkaAvroSerializerConfig.SCHEMA_REGISTRY_URL_CONFIG, configuration.getKafkaSchemaRegistryUrl());
        props.put(ProducerConfig.ACKS_CONFIG, "all");
        props.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, "true");
        props.put(ProducerConfig.LINGER_MS_CONFIG, 200);
        props.put(ProducerConfig.BATCH_SIZE_CONFIG, 16384);

        producer = new KafkaProducer<>(props);
    }

    @Override
    public void stop() throws Exception {
        producer.close(Duration.ofMinutes(5));
    }

    public void sendSlowRawEnergyEvent(RawDeviceEvent event) {
        sendEvent(event, slowRawEventTopic);
    }

    public void sendRawEnergyEvent(RawDeviceEvent event) {
        sendEvent(event, rawEventTopic);
    }

    private void sendEvent(RawDeviceEvent event, String topic) {
        ProducerRecord<String, RawDeviceEvent> producerRecordEvent = new ProducerRecord<>(topic, event);
        producer.send(producerRecordEvent, (recordMetadata, e) -> {
            if (e != null) {
                System.err.println("Impossible to send record " + e.getMessage());
                e.printStackTrace();
            }
        });
    }
}
