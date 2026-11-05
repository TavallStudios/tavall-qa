package org.tavall.qa.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.tavall.dependency.maps.DependencyMap;
import org.tavall.dependency.maps.interfaces.IDependencyMap;
import org.tavall.filesystem.api.FilesystemAtomicOperations;
import org.tavall.filesystem.system.atomic.AtomicFilesystemOperations;
import org.tavall.database.postgres.IPostgresDatabase;
import org.tavall.qa.config.TavallQaConfiguration;
import org.tavall.qa.persistence.ITaskBoardDataHandler;
import org.tavall.qa.persistence.ITaskBoardNotionSyncDataHandler;
import org.tavall.qa.persistence.TaskBoardNotionSyncDataHandler;

import java.nio.file.Path;
import java.time.Clock;
import java.util.Objects;

/** Test composition that exercises the production Spring configuration with boundary-owned storage. */
public final class TaskBoardTestComposition implements AutoCloseable {
    private final DependencyMap dependencies;
    private final AnnotationConfigApplicationContext context;
    private final InMemoryTaskBoardDataHandler dataHandler;

    private TaskBoardTestComposition(DependencyMap dependencies,
            AnnotationConfigApplicationContext context, InMemoryTaskBoardDataHandler dataHandler) {
        this.dependencies = dependencies;
        this.context = context;
        this.dataHandler = dataHandler;
    }

    public static TaskBoardTestComposition open(Path mirrorRoot, Clock clock) {
        return open(new DependencyMap(), mirrorRoot, clock, new InMemoryTaskBoardDataHandler(), true);
    }

    public static TaskBoardTestComposition open(DependencyMap dependencies, Path mirrorRoot, Clock clock) {
        return open(dependencies, mirrorRoot, clock, new InMemoryTaskBoardDataHandler(), true);
    }

    public static TaskBoardTestComposition openWithoutTaskDatabase(Path mirrorRoot, Clock clock) {
        return open(new DependencyMap(), mirrorRoot, clock, null, false);
    }

    public static TaskBoardTestComposition openWithPostgresBoundary(Path mirrorRoot, Clock clock,
            IPostgresDatabase database) {
        DependencyMap dependencies = new DependencyMap();
        dependencies.registerInstance(IPostgresDatabase.class, Objects.requireNonNull(database, "database"));
        return open(dependencies, mirrorRoot, clock, null, false);
    }

    private static TaskBoardTestComposition open(DependencyMap dependencies, Path mirrorRoot, Clock clock,
            InMemoryTaskBoardDataHandler dataHandler, boolean replaceTaskDataHandler) {
        Objects.requireNonNull(dependencies, "dependencies");
        Objects.requireNonNull(mirrorRoot, "mirrorRoot");
        Objects.requireNonNull(clock, "clock");
        if (dependencies.findInstance(Clock.class) == null) {
            dependencies.registerInstance(Clock.class, clock);
        }

        AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
        context.getDefaultListableBeanFactory().setAllowBeanDefinitionOverriding(true);
        context.register(TavallQaConfiguration.class);
        context.register(org.tavall.qa.handler.TaskBoardProgressAggregator.class);
        context.registerBean(IDependencyMap.class, () -> dependencies);
        if (replaceTaskDataHandler) {
            context.registerBean("taskBoardDataHandler", ITaskBoardDataHandler.class, () -> dataHandler);
        }
        context.registerBean("taskBoardNotionSyncDataHandler", ITaskBoardNotionSyncDataHandler.class,
                () -> {
                    FilesystemAtomicOperations atomicOperations = new AtomicFilesystemOperations();
                    return new TaskBoardNotionSyncDataHandler(null, atomicOperations,
                            new ObjectMapper(), mirrorRoot);
                });
        context.refresh();
        return new TaskBoardTestComposition(dependencies, context, dataHandler);
    }

    public InMemoryTaskBoardDataHandler dataHandler() {
        return dataHandler;
    }

    public <T> T bean(Class<T> type) {
        return context.getBean(type);
    }

    @Override
    public void close() {
        try {
            context.close();
        } finally {
            dependencies.clear();
        }
    }
}
