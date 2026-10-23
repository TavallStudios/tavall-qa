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
    "tavall-qa-system",
    "tavall-qa-test-suite"
)

fun includeSourceComposite(
    environmentName: String,
    compositeName: String,
    artifactProjects: Map<String, String>
) {
    val configuredPath = providers.environmentVariable(environmentName)
        .orNull
        ?.trim()
        ?.takeIf(String::isNotEmpty)
        ?: return
    val buildDirectory = file(configuredPath).canonicalFile
    require(buildDirectory.resolve("settings.gradle.kts").isFile) {
        "$compositeName composite is missing settings.gradle.kts at $buildDirectory"
    }
    require(buildDirectory.resolve("build.gradle.kts").isFile) {
        "$compositeName composite is missing build.gradle.kts at $buildDirectory"
    }
    includeBuild(buildDirectory) {
        name = compositeName
        dependencySubstitution {
            artifactProjects.forEach { (artifact, projectPath) ->
                substitute(module("org.tavall:$artifact")).using(project(projectPath))
            }
        }
    }
}

includeSourceComposite(
    "TAVALL_DATABASE_BUILD",
    "tavall-database-source",
    mapOf("tavall-database-postgres" to ":tavall-database-postgres")
)
includeSourceComposite(
    "TAVALL_DI_BUILD",
    "tavall-di-source",
    mapOf("tavall-di" to ":")
)
