package eu.macphail.energy.device.control;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;

import java.util.Properties;

public class InvalidMessageProducer {

    private final AppConfiguration config;
    private final String invalidMessageTopic;
    private KafkaProducer<byte[], byte[]> producer;

    public InvalidMessageProducer(AppConfiguration config) {
        this.config = config;
        this.invalidMessageTopic = config.getKafkaInvalidTopicName();

        Properties props = new Properties();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, config.getKafkaBootstrapServer());
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, org.apache.kafka.common.serialization.ByteArraySerializer.class);
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, org.apache.kafka.common.serialization.ByteArraySerializer.class);
        props.put(ProducerConfig.ACKS_CONFIG, "all");
        props.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, "true");

        producer = new KafkaProducer<>(props);
    }

    public void sendMessage(byte[] key, byte[] value) {
        ProducerRecord<byte[], byte[]> producerRecordEvent = new ProducerRecord<>(invalidMessageTopic, key, value);
        producer.send(producerRecordEvent, (recordMetadata, e) -> {
            if (e != null) {
                System.err.println("Impossible to send record to invalid message topic: " + e.getMessage());
                e.printStackTrace();
            }
        });
    }
}
