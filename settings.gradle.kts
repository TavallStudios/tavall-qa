pluginManagement {
    repositories {
        maven {
            name = "TavallPrivate"
            url = uri(providers.gradleProperty("tavallPrivateMavenRepository")
                .orElse(providers.environmentVariable("TAVALL_PRIVATE_MAVEN_REPOSITORY"))
                .orElse("/srv/dev-storage/deps/private/snapshots"))
        }
        gradlePluginPortal()
        mavenCentral()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.PREFER_SETTINGS)
    repositories {
        val privateMavenRepository = providers.environmentVariable("TAVALL_PRIVATE_MAVEN_REPOSITORY")
            .orNull
            ?.trim()
            ?.takeIf(String::isNotEmpty)
        if (privateMavenRepository != null) {
            maven {
                name = "TavallPrivateEnvironment"
                url = uri(privateMavenRepository)
            }
        }
        maven {
            name = "TavallPrivateDefault"
            url = uri("/srv/dev-storage/deps/private/snapshots")
        }
        mavenCentral()
    }
}

rootProject.name = "tavall-qa"

include(
    "tavall-qa-api",
    "tavall-qa-core",
    "tavall-qa-test-suite"
)
