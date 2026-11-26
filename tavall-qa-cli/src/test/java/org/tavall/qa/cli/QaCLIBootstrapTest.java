package org.tavall.qa.cli;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.tavall.cli.api.bootstrap.ICLIBootstrap;
import org.tavall.cli.api.command.CLICommandPath;
import org.tavall.cli.api.command.ICLICommand;
import org.tavall.cli.api.registry.ICLICommandRegistry;
import org.tavall.cli.command.registry.CLICommandRegistry;
import org.tavall.dependency.maps.DependencyMap;
import org.tavall.dependency.maps.interfaces.IDependencyMap;
import org.tavall.qa.cli.client.IQaExecutionApiClient;

import java.util.List;
import java.util.ServiceLoader;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class QaCLIBootstrapTest {

    @BeforeEach
    void resetDependencies() {
        DependencyMap.getDependencyMap().clear();
    }

    @AfterEach
    void clearDependencies() {
        DependencyMap.getDependencyMap().clear();
    }

    @Test
    void registersAllThreeCommandsAndClient() {
        IDependencyMap map = DependencyMap.getDependencyMap();
        ICLICommandRegistry registry = new CLICommandRegistry();

        new QaCLIBootstrap().registerCommands(registry);

        assertCommandRegistered(registry, "qa task upsert");
        assertCommandRegistered(registry, "qa design register");
        assertCommandRegistered(registry, "qa design decision");
        assertEquals(3, registry.listCommands().size());
        assertTrue(map.isInstanceRegistered(IQaExecutionApiClient.class),
                "the typed QA execution client must be DI-registered before commands run");
    }

    @Test
    void commandPathsAreLowerCaseCanonical() {
        ICLICommandRegistry registry = new CLICommandRegistry();
        new QaCLIBootstrap().registerCommands(registry);

        List<String> paths = registry.listCommands().stream()
                .map(command -> command.path().asString())
                .sorted()
                .collect(Collectors.toList());
        assertEquals(List.of("qa design decision", "qa design register", "qa task upsert"), paths);
    }

    @Test
    void bootstrapIsServiceLoaderDiscoverable() {
        List<String> bootstrapNames = ServiceLoader.load(ICLIBootstrap.class).stream()
                .map(provider -> provider.type().getName())
                .collect(Collectors.toList());
        assertTrue(bootstrapNames.contains(QaCLIBootstrap.class.getName()),
                "META-INF/services must expose QaCLIBootstrap");
    }

    private static void assertCommandRegistered(ICLICommandRegistry registry, String path) {
        java.util.Optional<ICLICommand<?>> command = registry.findCommand(CLICommandPath.parse(path));
        assertTrue(command.isPresent(), "missing command: " + path);
        assertNotNull(command.get().settings());
    }
}
