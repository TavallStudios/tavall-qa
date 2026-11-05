package org.tavall.qa.handler;

import org.tavall.database.postgres.IPostgresDatabase;
import org.tavall.database.postgres.entity.IPostgresEntityStore;

import java.lang.reflect.Proxy;
import java.util.List;
import java.util.Optional;

/** Empty open store at the PostgreSQL boundary; it never opens a connection. */
public final class FakePostgresDatabaseBoundary {
    private FakePostgresDatabaseBoundary() { }

    public static IPostgresDatabase available() {
        IPostgresEntityStore store = (IPostgresEntityStore) Proxy.newProxyInstance(
                IPostgresEntityStore.class.getClassLoader(), new Class<?>[]{IPostgresEntityStore.class},
                (proxy, method, arguments) -> switch (method.getName()) {
                    case "isOpen" -> true;
                    case "findNamed" -> List.of();
                    case "findOneNamed", "find" -> Optional.empty();
                    case "toString" -> "EmptyPostgresEntityStoreBoundary";
                    case "hashCode" -> System.identityHashCode(proxy);
                    case "equals" -> proxy == arguments[0];
                    default -> defaultValue(method.getReturnType());
                });
        return (IPostgresDatabase) Proxy.newProxyInstance(
                IPostgresDatabase.class.getClassLoader(), new Class<?>[]{IPostgresDatabase.class},
                (proxy, method, arguments) -> switch (method.getName()) {
                    case "isAvailable" -> true;
                    case "entities" -> store;
                    case "toString" -> "AvailablePostgresDatabaseBoundary";
                    case "hashCode" -> System.identityHashCode(proxy);
                    case "equals" -> proxy == arguments[0];
                    default -> defaultValue(method.getReturnType());
                });
    }

    private static Object defaultValue(Class<?> type) {
        if (!type.isPrimitive() || type == void.class) return null;
        if (type == boolean.class) return false;
        if (type == char.class) return '\0';
        if (type == byte.class) return (byte) 0;
        if (type == short.class) return (short) 0;
        if (type == int.class) return 0;
        if (type == long.class) return 0L;
        if (type == float.class) return 0F;
        if (type == double.class) return 0D;
        return null;
    }
}
