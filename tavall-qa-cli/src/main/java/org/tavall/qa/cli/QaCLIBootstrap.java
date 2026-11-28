package org.tavall.qa.cli;

import org.tavall.cli.api.bootstrap.ICLIBootstrap;
import org.tavall.cli.api.registry.ICLICommandRegistry;
import org.tavall.qa.cli.client.IQaExecutionApiClient;
import org.tavall.qa.cli.client.HttpQaExecutionApiClient;
import org.tavall.qa.cli.github.GhCliGithubIssueReader;
import org.tavall.qa.cli.github.IGithubIssueReader;
import org.tavall.dependency.maps.DependencyMap;
import org.tavall.dependency.maps.interfaces.IDependencyMap;

import java.util.Objects;

/**
 * Registers the canonical {@code qa} task board commands, their typed execution client and the
 * typed GitHub issue reader used by {@code qa github sync}.
 */
public final class QaCLIBootstrap implements ICLIBootstrap {

    @Override
    public void registerCommands(ICLICommandRegistry registry) {
        Objects.requireNonNull(registry, "registry");
        IDependencyMap map = DependencyMap.getDependencyMap();
        if (!map.isInstanceRegistered(IQaExecutionApiClient.class)) {
            map.registerInstance(IQaExecutionApiClient.class, new HttpQaExecutionApiClient());
        }
        if (!map.isInstanceRegistered(IGithubIssueReader.class)) {
            map.registerInstance(IGithubIssueReader.class, new GhCliGithubIssueReader());
        }
        registry.registerCommand(new QaTaskUpsertCommand());
        registry.registerCommand(new QaDesignRegisterCommand());
        registry.registerCommand(new QaDesignDecisionCommand());
        registry.registerCommand(new QaDesignAiReviewCommand());
        registry.registerCommand(new QaGithubSyncCommand());
    }
}
