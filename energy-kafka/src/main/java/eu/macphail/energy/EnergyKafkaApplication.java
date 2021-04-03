package eu.macphail.energy;

import eu.macphail.energy.device.boundary.*;
import eu.macphail.energy.device.control.DeviceDAO;
import eu.macphail.energy.device.control.AppConfiguration;
import eu.macphail.energy.device.control.DeviceEventKafkaProducer;
import io.dropwizard.Application;
import io.dropwizard.jdbi3.JdbiFactory;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import org.jdbi.v3.core.Jdbi;

public class EnergyKafkaApplication extends Application<AppConfiguration> {

  public static void main(String[] args) throws Exception {
    new EnergyKafkaApplication().run(args);
  }

  @Override
  public void run(AppConfiguration config, Environment environment) throws Exception {
    JdbiFactory factory = new JdbiFactory();
    Jdbi jdbi = factory.build(environment, config.getDataSourceFactory(), "postgresql");

    DeviceEventKafkaProducer deviceEventKafkaProducer = new DeviceEventKafkaProducer(config);
    environment.lifecycle().manage(deviceEventKafkaProducer);

    DeviceDAO deviceDAO = jdbi.onDemand(DeviceDAO.class);
    DeviceController controller = new DeviceController(config.getBigMessageSize(), deviceEventKafkaProducer, deviceDAO);
    environment.jersey().register(controller);
  }

  @Override
  public void initialize(Bootstrap<AppConfiguration> bootstrap) {
    bootstrap.addCommand(new RawDeviceEventsToCanonicalEventsCommand(this));
    bootstrap.addCommand(new DeviceEventToDatabaseCommand(this));
  }

  @Override
  public String getName() {
    return "energy";
  }
}
