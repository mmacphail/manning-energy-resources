package eu.macphail.energy.device.control;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.dropwizard.Configuration;
import io.dropwizard.db.DataSourceFactory;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

public class AppConfiguration extends Configuration {

    private String kafkaRawDeviceEventTopicName;
    private String kafkaSlowRawDeviceEventTopicName;
    private String kafkaDeviceEventTopicName;
    private String kafkaInvalidTopicName;

    private long bigMessageSize;

    private String kafkaBootstrapServer;

    private String kafkaSchemaRegistryUrl;

    public String getKafkaRawDeviceEventTopicName() {
        return kafkaRawDeviceEventTopicName;
    }

    public void setKafkaRawDeviceEventTopicName(String kafkaRawDeviceEventTopicName) {
        this.kafkaRawDeviceEventTopicName = kafkaRawDeviceEventTopicName;
    }

    public String getKafkaSlowRawDeviceEventTopicName() {
        return kafkaSlowRawDeviceEventTopicName;
    }

    public void setKafkaSlowRawDeviceEventTopicName(String kafkaSlowRawDeviceEventTopicName) {
        this.kafkaSlowRawDeviceEventTopicName = kafkaSlowRawDeviceEventTopicName;
    }

    public void setKafkaBootstrapServer(String kafkaBootstrapServer) {
        this.kafkaBootstrapServer = kafkaBootstrapServer;
    }

    public void setKafkaSchemaRegistryUrl(String kafkaSchemaRegistryUrl) {
        this.kafkaSchemaRegistryUrl = kafkaSchemaRegistryUrl;
    }

    public DataSourceFactory getDatabase() {
        return database;
    }

    public void setDatabase(DataSourceFactory database) {
        this.database = database;
    }

    public String getKafkaBootstrapServer() {
        return kafkaBootstrapServer;
    }

    public String getKafkaSchemaRegistryUrl() {
        return kafkaSchemaRegistryUrl;
    }

    @Valid
    @NotNull
    private DataSourceFactory database = new DataSourceFactory();

    @JsonProperty("database")
    public void setDataSourceFactory(DataSourceFactory factory) {
        this.database = factory;
    }

    @JsonProperty("database")
    public DataSourceFactory getDataSourceFactory() {
        return database;
    }

    public String getKafkaDeviceEventTopicName() {
        return kafkaDeviceEventTopicName;
    }

    public void setKafkaDeviceEventTopicName(String kafkaDeviceEventTopicName) {
        this.kafkaDeviceEventTopicName = kafkaDeviceEventTopicName;
    }

    public String getKafkaInvalidTopicName() {
        return kafkaInvalidTopicName;
    }

    public void setKafkaInvalidTopicName(String kafkaInvalidTopicName) {
        this.kafkaInvalidTopicName = kafkaInvalidTopicName;
    }

    public long getBigMessageSize() {
        return bigMessageSize;
    }

    public void setBigMessageSize(long bigMessageSize) {
        this.bigMessageSize = bigMessageSize;
    }
}
