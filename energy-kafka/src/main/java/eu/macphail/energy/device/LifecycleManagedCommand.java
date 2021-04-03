package eu.macphail.energy.device;

import io.dropwizard.Application;
import io.dropwizard.Configuration;
import io.dropwizard.cli.EnvironmentCommand;
import io.dropwizard.setup.Environment;
import net.sourceforge.argparse4j.inf.Namespace;
import org.eclipse.jetty.util.component.ContainerLifeCycle;
import org.eclipse.jetty.util.thread.ShutdownThread;

// from https://sadique.io/blog/2019/07/28/managing-environment-lifecycles-for-dropwizard-commands/
public abstract class LifecycleManagedCommand<T extends Configuration> extends EnvironmentCommand<T> {

    private final ContainerLifeCycle containerLifeCycle;

    public LifecycleManagedCommand(Application<T> application, String name, String description) {
        super(application, name, description);
        containerLifeCycle = new ContainerLifeCycle();
    }

    @Override
    protected void run(Environment environment, Namespace namespace, T configuration) throws Exception {
        environment.lifecycle().getManagedObjects().forEach(containerLifeCycle::addBean);
        ShutdownThread.register(containerLifeCycle);
        containerLifeCycle.start();

        runManaged(environment, namespace, configuration);

        containerLifeCycle.stop();
    }

    abstract void runManaged(Environment environment, Namespace namespace, T configuration);
}
