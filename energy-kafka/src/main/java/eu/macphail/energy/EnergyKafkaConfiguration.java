package eu.macphail.energy;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.dropwizard.Configuration;
import io.dropwizard.db.DataSourceFactory;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

public class EnergyKafkaConfiguration extends Configuration {

    private String kafkaRawEnergyEventTopicName;

    private String kafkaBootstrapServer;

    private String kafkaSchemaRegistryUrl;

    public String getKafkaBootstrapServer() {
        return kafkaBootstrapServer;
    }

    public String getKafkaSchemaRegistryUrl() {
        return kafkaSchemaRegistryUrl;
    }

    public String getKafkaRawEnergyEventTopicName() {
        return kafkaRawEnergyEventTopicName;
    }

    public void setKafkaRawEnergyEventTopicName(String kafkaRawEnergyEventTopicName) {
        this.kafkaRawEnergyEventTopicName = kafkaRawEnergyEventTopicName;
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
}
