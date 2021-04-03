package eu.macphail;

import io.dropwizard.Application;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;

public class jsdApplication extends Application<jsdConfiguration> {

    public static void main(final String[] args) throws Exception {
        new jsdApplication().run(args);
    }

    @Override
    public String getName() {
        return "jsd";
    }

    @Override
    public void initialize(final Bootstrap<jsdConfiguration> bootstrap) {
        // TODO: application initialization
    }

    @Override
    public void run(final jsdConfiguration configuration,
                    final Environment environment) {
        // TODO: implement application
    }

}
