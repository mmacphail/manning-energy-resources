package eu.macphail.energy;

import eu.macphail.energy.device.LoadDeviceEventsCommand;
import eu.macphail.energy.device.DeviceEventController;
import eu.macphail.energy.device.EnergyEventStreamingService;
import io.dropwizard.Application;
import io.dropwizard.jdbi3.JdbiFactory;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import org.jdbi.v3.core.Jdbi;

public class EnergyKafkaApplication extends Application<EnergyKafkaConfiguration> {

  public static void main(String[] args) throws Exception {
    new EnergyKafkaApplication().run(args);
  }

  @Override
  public void run(EnergyKafkaConfiguration config, Environment environment) throws Exception {
    JdbiFactory factory = new JdbiFactory();
    Jdbi jdbi = factory.build(environment, config.getDataSourceFactory(), "postgresql");

    EnergyEventStreamingService energyEventStreamingService = new EnergyEventStreamingService(config);
    environment.lifecycle().manage(energyEventStreamingService);

    DeviceEventController controller = new DeviceEventController(energyEventStreamingService);
    environment.jersey().register(controller);
  }

  @Override
  public void initialize(Bootstrap<EnergyKafkaConfiguration> bootstrap) {
    bootstrap.addCommand(new LoadDeviceEventsCommand(this));
  }

  @Override
  public String getName() {
    return "energy";
  }
}
