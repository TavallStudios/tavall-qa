package org.tavall.qa.cli;

import org.tavall.cli.api.bootstrap.ICLIBootstrap;
import org.tavall.cli.api.registry.ICLICommandRegistry;
import org.tavall.qa.cli.client.IQaExecutionApiClient;
import org.tavall.qa.cli.client.HttpQaExecutionApiClient;
import org.tavall.dependency.maps.DependencyMap;
import org.tavall.dependency.maps.interfaces.IDependencyMap;

import java.util.Objects;

/** Registers the canonical {@code qa} task board commands and their typed execution client. */
public final class QaCLIBootstrap implements ICLIBootstrap {

    @Override
    public void registerCommands(ICLICommandRegistry registry) {
        Objects.requireNonNull(registry, "registry");
        IDependencyMap map = DependencyMap.getDependencyMap();
        if (!map.isInstanceRegistered(IQaExecutionApiClient.class)) {
            map.registerInstance(IQaExecutionApiClient.class, new HttpQaExecutionApiClient());
        }
        registry.registerCommand(new QaTaskUpsertCommand());
        registry.registerCommand(new QaDesignRegisterCommand());
        registry.registerCommand(new QaDesignDecisionCommand());
    }
}
