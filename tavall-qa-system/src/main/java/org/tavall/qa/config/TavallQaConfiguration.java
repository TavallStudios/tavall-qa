package org.tavall.qa.config;

import org.springframework.beans.factory.DisposableBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.tavall.database.postgres.IPostgresDatabase;
import org.tavall.dependency.maps.interfaces.IDependencyMap;
import org.tavall.dependency.annotations.CompositionBoundary;
import org.tavall.qa.handler.TaskBoardAgentViewHandler;
import org.tavall.qa.handler.TaskBoardAgentViewHandlerDependencyAccess;
import org.tavall.qa.handler.ITaskBoardAgentViewHandler;
import org.tavall.qa.handler.ITaskBoardNotionSyncHandler;
import org.tavall.qa.handler.TaskBoardNotionSyncHandler;
import org.tavall.qa.handler.TaskBoardNotionSyncHandlerDependencyAccess;
import org.tavall.qa.persistence.ITaskBoardNotionSyncDataHandler;
import org.tavall.qa.handler.TaskBoardExternalReferenceHandler;
import org.tavall.qa.handler.TaskBoardExternalReferenceHandlerDependencyAccess;
import org.tavall.qa.handler.TaskBoardMutationHandler;
import org.tavall.qa.handler.TaskBoardMutationHandlerDependencyAccess;
import org.tavall.qa.persistence.ITaskBoardDataHandler;
import org.tavall.qa.persistence.TaskBoardDataHandler;
import org.tavall.qa.persistence.UnavailableTaskBoardDataHandler;
import org.tavall.qa.persistence.ITaskBoardVentureDataHandler;
import org.tavall.qa.persistence.TaskBoardVentureDataHandler;
import org.tavall.qa.persistence.UnavailableTaskBoardVentureDataHandler;
import org.tavall.qa.persistence.ITaskBoardExternalReferenceDataHandler;
import org.tavall.qa.persistence.TaskBoardExternalReferenceDataHandler;
import org.tavall.qa.persistence.UnavailableTaskBoardExternalReferenceDataHandler;
import org.tavall.qa.persistence.ITaskBoardExecutionCheckpointDataHandler;
import org.tavall.qa.persistence.TaskBoardExecutionCheckpointDataHandler;
import org.tavall.qa.handler.ITaskBoardExecutionCheckpointHandler;
import org.tavall.qa.handler.TaskBoardExecutionCheckpointHandler;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.time.Clock;

@Configuration
@CompositionBoundary("Binds Spring-owned QA capabilities into the existing process dependency map")
public class TavallQaConfiguration implements DisposableBean {
    private final IDependencyMap dependencyMap;
    private final List<RegisteredDependency> registeredDependencies = new ArrayList<>();

    private record RegisteredDependency(Class<?> token, Object instance) {
    }

    public TavallQaConfiguration(IDependencyMap dependencyMap) {
        this.dependencyMap = Objects.requireNonNull(dependencyMap, "dependencyMap");
    }

    /**
     * Registers one Tavall-managed instance under the given token and remembers the registration so
     * {@link #destroy()} can retire it in reverse order. Registrations displaced by a newer
     * composition are skipped on teardown because they no longer own the token.
     */
    private <T> T registerOwned(Class<T> token, T instance) {
        dependencyMap.registerInstance(token, instance);
        synchronized (registeredDependencies) {
            registeredDependencies.add(new RegisteredDependency(token, instance));
        }
        return instance;
    }

    @Override
    public void destroy() {
        List<RegisteredDependency> registrations;
        synchronized (registeredDependencies) {
            registrations = new ArrayList<>(registeredDependencies);
            registeredDependencies.clear();
        }
        for (int index = registrations.size() - 1; index >= 0; index--) {
            RegisteredDependency registration = registrations.get(index);
            if (dependencyMap.findInstance(registration.token()) != registration.instance()) {
                continue;
            }
            dependencyMap.removeDependency(registration.token());
        }
    }

    @Bean
    public ITaskBoardDataHandler taskBoardDataHandler() {
        IPostgresDatabase database = dependencyMap.findInstance(IPostgresDatabase.class);
        ITaskBoardDataHandler handler =
                (database == null || !database.isAvailable() || !database.entities().isOpen())
                        ? new UnavailableTaskBoardDataHandler()
                        : new TaskBoardDataHandler(database);
        return registerOwned(ITaskBoardDataHandler.class, handler);
    }

    @Bean
    public ITaskBoardVentureDataHandler taskBoardVentureDataHandler() {
        IPostgresDatabase database = dependencyMap.findInstance(IPostgresDatabase.class);
        if (database == null || !database.isAvailable() || !database.entities().isOpen()) {
            return new UnavailableTaskBoardVentureDataHandler();
        }
        return new TaskBoardVentureDataHandler(database);
    }

    @Bean
    public org.tavall.qa.handler.ITaskBoardVentureQueryHandler taskBoardVentureQueryHandler(
            @Lazy ITaskBoardVentureDataHandler ventures,
            @Lazy ITaskBoardDataHandler tasks
    ) {
        return new org.tavall.qa.handler.TaskBoardVentureQueryHandler(ventures, tasks);
    }

    @Bean
    public ITaskBoardExternalReferenceDataHandler taskBoardExternalReferenceDataHandler() {
        IPostgresDatabase database = dependencyMap.findInstance(IPostgresDatabase.class);
        Clock clock = registerClockIfAbsent();
        ITaskBoardExternalReferenceDataHandler handler =
                (database == null || !database.isAvailable() || !database.entities().isOpen())
                        ? new UnavailableTaskBoardExternalReferenceDataHandler()
                        : new TaskBoardExternalReferenceDataHandler(database, clock);
        return registerOwned(ITaskBoardExternalReferenceDataHandler.class, handler);
    }

    @Bean
    public org.tavall.qa.handler.ITaskBoardExternalReferenceHandler taskBoardExternalReferenceHandler(
            @Lazy ITaskBoardExternalReferenceDataHandler externalReferences,
            @Lazy ITaskBoardDataHandler tasks
    ) {
        registerClockIfAbsent();
        registerOwned(TaskBoardExternalReferenceHandlerDependencyAccess.class,
                new TaskBoardExternalReferenceHandlerDependencyAccess(dependencyMap));
        return registerOwned(org.tavall.qa.handler.ITaskBoardExternalReferenceHandler.class,
                new TaskBoardExternalReferenceHandler());
    }

    @Bean
    public org.tavall.qa.handler.ITaskBoardActionTargetResolver taskBoardActionTargetResolver() {
        return registerOwned(org.tavall.qa.handler.ITaskBoardActionTargetResolver.class,
                new org.tavall.qa.handler.TaskBoardActionTargetResolver());
    }

    @Bean
    public org.tavall.qa.handler.ITaskBoardQueryHandler taskBoardQueryHandler(
            @Lazy ITaskBoardDataHandler dataHandler,
            org.tavall.qa.handler.ITaskBoardActionTargetResolver resolver
    ) {
        return new org.tavall.qa.handler.TaskBoardQueryHandler(dataHandler, resolver);
    }

    @Bean
    public org.tavall.qa.handler.ITaskBoardMutationHandler taskBoardMutationHandler() {
        registerOwned(TaskBoardMutationHandlerDependencyAccess.class,
                new TaskBoardMutationHandlerDependencyAccess(dependencyMap));
        return registerOwned(org.tavall.qa.handler.ITaskBoardMutationHandler.class,
                new TaskBoardMutationHandler());
    }

    /** Registers the process Clock when no composition before this one already owns it. */
    private Clock registerClockIfAbsent() {
        Clock clock = dependencyMap.findInstance(Clock.class);
        if (clock == null) {
            clock = registerOwned(Clock.class, Clock.systemUTC());
        }
        return clock;
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
        ITaskBoardDataHandler current = dependencyMap.findInstance(ITaskBoardDataHandler.class);
        if (current == null) {
            registerOwned(ITaskBoardDataHandler.class, tasks);
        } else if (current != tasks) {
            dependencyMap.replaceInstance(ITaskBoardDataHandler.class, () -> tasks);
        }
        registerClockIfAbsent();
        return registerOwned(ITaskBoardExecutionCheckpointHandler.class,
                new TaskBoardExecutionCheckpointHandler());
    }

    @Bean
    public org.tavall.qa.domain.notion.INotionTaskMappingRegistry notionTaskMappingRegistry() {
        org.tavall.qa.domain.notion.INotionTaskMappingRegistry registry = dependencyMap.findInstance(org.tavall.qa.domain.notion.INotionTaskMappingRegistry.class);
        if (registry == null) {
            registry = org.tavall.qa.domain.notion.NotionTaskMappingRegistry.canonical();
            dependencyMap.registerInstance(org.tavall.qa.domain.notion.INotionTaskMappingRegistry.class, registry);
        }
        return registry;
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
    public ITaskBoardNotionSyncHandler taskBoardNotionSyncHandler(
            ITaskBoardNotionSyncDataHandler syncDataHandler
    ) {
        registerOwned(ITaskBoardNotionSyncDataHandler.class, syncDataHandler);
        registerOwned(TaskBoardNotionSyncHandlerDependencyAccess.class,
                new TaskBoardNotionSyncHandlerDependencyAccess(dependencyMap));
        return registerOwned(ITaskBoardNotionSyncHandler.class,
                new TaskBoardNotionSyncHandler(registerClockIfAbsent()));
    }

    @Bean
    public ITaskBoardAgentViewHandler taskBoardAgentViewHandler() {
        registerOwned(TaskBoardAgentViewHandlerDependencyAccess.class,
                new TaskBoardAgentViewHandlerDependencyAccess(dependencyMap));
        return registerOwned(ITaskBoardAgentViewHandler.class,
                new TaskBoardAgentViewHandler());
    }
}
