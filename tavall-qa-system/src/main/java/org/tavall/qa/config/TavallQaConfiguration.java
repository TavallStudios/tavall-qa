package org.tavall.qa.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.tavall.database.postgres.IPostgresDatabase;
import org.tavall.dependency.maps.interfaces.IDependencyMap;
import org.tavall.qa.persistence.ITaskBoardDataHandler;
import org.tavall.qa.persistence.TaskBoardDataHandler;
import org.tavall.qa.persistence.UnavailableTaskBoardDataHandler;

import java.util.Objects;

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
    public org.tavall.qa.persistence.ITaskBoardNotionSyncDataHandler taskBoardNotionSyncDataHandler() {
        IPostgresDatabase database = dependencyMap.findInstance(IPostgresDatabase.class);
        org.tavall.filesystem.api.FilesystemAtomicOperations atomicOps = dependencyMap.findInstance(org.tavall.filesystem.api.FilesystemAtomicOperations.class);
        if (atomicOps == null) {
            atomicOps = new org.tavall.filesystem.system.atomic.AtomicFilesystemOperations();
            dependencyMap.registerInstance(org.tavall.filesystem.api.FilesystemAtomicOperations.class, atomicOps);
        }
        com.fasterxml.jackson.databind.ObjectMapper mapper = dependencyMap.findInstance(com.fasterxml.jackson.databind.ObjectMapper.class);
        if (mapper == null) {
            mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            dependencyMap.registerInstance(com.fasterxml.jackson.databind.ObjectMapper.class, mapper);
        }
        return new org.tavall.qa.persistence.TaskBoardNotionSyncDataHandler(database, atomicOps, mapper);
    }

    @Bean
    public org.tavall.qa.handler.ITaskBoardNotionSyncHandler taskBoardNotionSyncHandler(
            org.tavall.qa.persistence.ITaskBoardNotionSyncDataHandler syncDataHandler,
            @Lazy ITaskBoardDataHandler dataHandler,
            @Lazy org.tavall.qa.handler.ITaskBoardMutationHandler mutationHandler
    ) {
        java.time.Clock clock = dependencyMap.findInstance(java.time.Clock.class);
        if (clock == null) {
            clock = java.time.Clock.systemUTC();
            dependencyMap.registerInstance(java.time.Clock.class, clock);
        }
        return new org.tavall.qa.handler.TaskBoardNotionSyncHandler(syncDataHandler, dataHandler, mutationHandler, clock);
    }

    @Bean
    public org.tavall.qa.handler.ITaskBoardAgentViewHandler taskBoardAgentViewHandler(
            @Lazy org.tavall.qa.handler.ITaskBoardQueryHandler queryHandler,
            @Lazy org.tavall.qa.handler.ITaskBoardMutationHandler mutationHandler,
            @Lazy ITaskBoardDataHandler dataHandler,
            org.tavall.qa.handler.ITaskBoardNotionSyncHandler syncHandler
    ) {
        return new org.tavall.qa.handler.TaskBoardAgentViewHandler(queryHandler, mutationHandler, dataHandler, syncHandler);
    }
}
