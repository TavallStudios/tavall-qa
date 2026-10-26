package org.tavall.qa.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.tavall.database.postgres.IPostgresDatabase;
import org.tavall.dependency.maps.interfaces.IDependencyMap;
import org.tavall.qa.persistence.ITaskBoardDataHandler;
import org.tavall.qa.persistence.TaskBoardDataHandler;
import org.tavall.qa.persistence.UnavailableTaskBoardDataHandler;
import org.tavall.qa.persistence.ITaskBoardExecutionCheckpointDataHandler;
import org.tavall.qa.persistence.TaskBoardExecutionCheckpointDataHandler;
import org.tavall.qa.handler.ITaskBoardExecutionCheckpointHandler;
import org.tavall.qa.handler.TaskBoardExecutionCheckpointHandler;

import java.util.Objects;
import java.time.Clock;

@Configuration
public class TavallQaConfiguration {
    private final IDependencyMap dependencyMap;

    public TavallQaConfiguration(IDependencyMap dependencyMap) {
        this.dependencyMap = Objects.requireNonNull(dependencyMap, "dependencyMap");
    }

    @Bean
    public ITaskBoardDataHandler taskBoardDataHandler() {
        IPostgresDatabase database = dependencyMap.findInstance(IPostgresDatabase.class);
        if (database == null || !database.isAvailable() || !database.entities().isOpen()) {
            return new UnavailableTaskBoardDataHandler();
        }
        return new TaskBoardDataHandler(database);
    }

    @Bean
    public org.tavall.qa.handler.ITaskBoardActionTargetResolver taskBoardActionTargetResolver() {
        return new org.tavall.qa.handler.TaskBoardActionTargetResolver();
    }

    @Bean
    public org.tavall.qa.handler.ITaskBoardQueryHandler taskBoardQueryHandler(
            @Lazy ITaskBoardDataHandler dataHandler,
            org.tavall.qa.handler.ITaskBoardActionTargetResolver resolver
    ) {
        return new org.tavall.qa.handler.TaskBoardQueryHandler(dataHandler, resolver);
    }

    @Bean
    public org.tavall.qa.handler.ITaskBoardMutationHandler taskBoardMutationHandler(
            @Lazy ITaskBoardDataHandler dataHandler,
            org.tavall.qa.handler.ITaskBoardActionTargetResolver resolver
    ) {
        return new org.tavall.qa.handler.TaskBoardMutationHandler(dataHandler, resolver);
    }

    @Bean
    public ITaskBoardExecutionCheckpointDataHandler taskBoardExecutionCheckpointDataHandler() {
        return dependencyMap.registerInstance(ITaskBoardExecutionCheckpointDataHandler.class,
                new TaskBoardExecutionCheckpointDataHandler());
    }

    @Bean
    public ITaskBoardExecutionCheckpointHandler taskBoardExecutionCheckpointHandler(
            @Lazy ITaskBoardDataHandler tasks, ITaskBoardExecutionCheckpointDataHandler checkpoints) {
        // Spring is the external composition boundary; consumers resolve these through typed Tavall DI.
        if (dependencyMap.findInstance(ITaskBoardDataHandler.class) == null) {
            dependencyMap.registerInstance(ITaskBoardDataHandler.class, tasks);
        }
        if (dependencyMap.findInstance(Clock.class) == null) {
            dependencyMap.registerInstance(Clock.class, Clock.systemUTC());
        }
        return dependencyMap.registerInstance(ITaskBoardExecutionCheckpointHandler.class,
                new TaskBoardExecutionCheckpointHandler());
    }
}
