package org.tavall.qa.handler;

import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.tavall.database.postgres.IPostgresDatabase;
import org.tavall.dependency.maps.DependencyMap;
import org.tavall.dependency.maps.interfaces.IDependencyMap;
import org.tavall.qa.config.TavallQaConfiguration;

import java.time.Clock;
import java.util.Objects;

/** Production Spring composition backed by the integration fixture's isolated PostgreSQL database. */
final class TaskBoardPostgresComposition implements AutoCloseable {
    private final DependencyMap dependencies;
    private final AnnotationConfigApplicationContext context;

    TaskBoardPostgresComposition(IPostgresDatabase database, Clock clock) {
        dependencies = new DependencyMap();
        dependencies.registerInstance(IPostgresDatabase.class, Objects.requireNonNull(database, "database"));
        dependencies.registerInstance(Clock.class, Objects.requireNonNull(clock, "clock"));

        context = new AnnotationConfigApplicationContext();
        context.register(TavallQaConfiguration.class);
        context.registerBean(IDependencyMap.class, () -> dependencies);
        context.refresh();
    }

    DependencyMap dependencies() {
        return dependencies;
    }

    <T> T bean(Class<T> type) {
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
