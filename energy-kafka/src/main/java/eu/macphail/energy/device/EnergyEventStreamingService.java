package eu.macphail.energy.device;

import eu.macphail.energy.EnergyKafkaConfiguration;
import eu.macphail.energy.RawEnergyEvent;
import io.confluent.kafka.serializers.KafkaAvroSerializer;
import io.confluent.kafka.serializers.KafkaAvroSerializerConfig;
import io.dropwizard.lifecycle.Managed;
import org.apache.kafka.clients.producer.*;

import java.time.Duration;
import java.util.Properties;

public class EnergyEventStreamingService implements Managed {

    private final EnergyKafkaConfiguration configuration;
    private final String topicName;
    private KafkaProducer<String, RawEnergyEvent> producer;

    public EnergyEventStreamingService(EnergyKafkaConfiguration energyKafkaConfiguration) {
        this.configuration = energyKafkaConfiguration;
        this.topicName = configuration.getKafkaRawEnergyEventTopicName();
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

    public void sendRawEnergyEvent(RawEnergyEvent rawEnergyEvent) {
        ProducerRecord<String, RawEnergyEvent> producerRecordEvent = new ProducerRecord<>(topicName, rawEnergyEvent);
        producer.send(producerRecordEvent, (recordMetadata, e) -> {
            if(e != null) {
                System.err.println("Impossible to send record " + e.getMessage());
                e.printStackTrace();
            }
        });
    }
}
