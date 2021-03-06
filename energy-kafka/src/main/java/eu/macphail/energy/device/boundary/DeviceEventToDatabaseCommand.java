package eu.macphail.energy.device.boundary;

import eu.macphail.energy.device.control.AppConfiguration;
import eu.macphail.energy.device.control.DeviceDAO;
import eu.macphail.energy.device.entity.DeviceEvent;
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

import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collection;
import java.util.Properties;

public class DeviceEventToDatabaseCommand extends EnvironmentCommand<AppConfiguration> {
    public DeviceEventToDatabaseCommand(Application<AppConfiguration> application) {
        super(application, "device-event-to-database", "Sends the device events to the database");
    }

    @Override
    public void configure(Subparser subparser) {
        super.configure(subparser);
    }

    @Override
    protected void run(Environment environment, Namespace namespace, AppConfiguration config) throws Exception {
        DataSourceFactory dsf = config.getDataSourceFactory();
        Jdbi jdbi = Jdbi.create(dsf.getUrl(), dsf.getUser(), dsf.getPassword())
                .installPlugin(new SqlObjectPlugin());

        consumeDeviceEvents(jdbi, config);
    }

    private void consumeDeviceEvents(Jdbi jdbi,
                                     AppConfiguration config) {

        DeviceDAO deviceDAO = jdbi.onDemand(DeviceDAO.class);

        Properties props = configureKafkaProperties(config);
        KafkaConsumer<String, DeviceEvent> consumer = new KafkaConsumer<>(props);
        try(Handle handle = jdbi.open()) {
            consumer.subscribe(Arrays.asList(config.getKafkaDeviceEventTopicName()), new ConsumerRebalanceListener() {
                @Override
                public void onPartitionsRevoked(Collection<TopicPartition> collection) {
                    consumer.commitSync();
                }

                @Override
                public void onPartitionsAssigned(Collection<TopicPartition> collection) {

                }
            });

            while (true) {
                final ConsumerRecords<String, DeviceEvent> records = consumer.poll(Duration.ofMillis(100));
                try {
                    handle.begin();
                    for (ConsumerRecord<String, DeviceEvent> record : records) {
                        DeviceEvent event = record.value();
                        upsertDevice(deviceDAO, event);
                    }
                    handle.commit();
                } catch(Exception e) {
                    handle.rollback();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        } finally {
            consumer.close(Duration.ofMinutes(5));
        }
    }

    private Properties configureKafkaProperties(AppConfiguration config) {
        Properties props = new Properties();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, config.getKafkaBootstrapServer());
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "device-event-to-database");
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

    private void upsertDevice(DeviceDAO deviceDAO, DeviceEvent event) {
        Timestamp eventTimestamp = Timestamp.from(Instant.ofEpochMilli(event.getReceivedTimestamp()));
        deviceDAO.upsertDevice(
                event.getDeviceID().toString(),
                eventTimestamp,
                event.getRegionId().toString(),
                event.getCharging()
        );
    }
}
