pluginManagement {
    val webSource = providers.gradleProperty("tavallWebBuild")
        .orElse(providers.environmentVariable("TAVALL_WEB_BUILD"))
        .orNull
    if (!webSource.isNullOrBlank()) {
        val pluginDir = file(webSource).canonicalFile.resolve("tavall-web-frontend-gradle")
        if (pluginDir.isDirectory) {
            includeBuild(pluginDir) {
                name = "tavall-web-frontend-gradle-plugin-source"
            }
        }
    }
    val ciDependencyRepository = providers.environmentVariable("TAVALL_CI_DEPENDENCY_REPOSITORY")
        .orNull
        ?.trim()
        ?.takeIf(String::isNotBlank)

    repositories {
        if (ciDependencyRepository != null) {
            maven {
                name = "TavallCiDependencyRepository"
                url = uri(ciDependencyRepository)
                content { includeGroupByRegex("org\\.tavall(?:\\..*)?") }
            }
        }
        val privateMavenRepository = providers.environmentVariable("TAVALL_PRIVATE_MAVEN_REPOSITORY")
            .orNull
            ?.trim()
            ?.takeIf(String::isNotBlank)
        if (privateMavenRepository != null) {
            maven {
                name = "tavallPrivateMaven"
                url = uri(privateMavenRepository)
                content {
                    includeGroupByRegex("org\\.tavall(?:\\..*)?")
                    includeGroupByRegex("com\\.tavall(?:\\..*)?")
                }
            }
        }
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
        ivy {
            name = "Node.js"
            url = uri("https://nodejs.org/dist/")
            patternLayout { artifact("v[revision]/[artifact](-v[revision]-[classifier]).[ext]") }
            metadataSources { artifact() }
            content { includeModule("org.nodejs", "node") }
        }
    }
}

rootProject.name = "tavall-qa"

include(
    "tavall-qa-api",
    "tavall-qa-cli",
    "tavall-qa-system",
    "tavall-qa-test-suite",
    "tavall-qa-web-frontend"
)

fun includeSourceComposite(
    environmentName: String,
    compositeName: String,
    artifactProjects: Map<String, String>,
    propertyName: String? = null
) {
    val configuredPath = (if (propertyName != null) {
        providers.gradleProperty(propertyName).orElse(providers.environmentVariable(environmentName))
    } else {
        providers.environmentVariable(environmentName)
    }).orNull
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
includeSourceComposite(
    "TAVALL_CUSTOM_ENUM_JAVA_BUILD",
    "tavall-custom-enum-java-source",
    mapOf("tavall-custom-enum-java" to ":")
)
includeSourceComposite(
    "TAVALL_LOGGING_BUILD",
    "tavall-logging-source",
    mapOf("tavall-logging" to ":")
)
includeSourceComposite(
    "TAVALL_FILESYSTEM_BUILD",
    "tavall-filesystem-source",
    mapOf(
        "tavall-filesystem-api" to ":tavall-filesystem-api",
        "tavall-filesystem" to ":tavall-filesystem"
    )
)
includeSourceComposite(
    "TAVALL_CLI_BUILD",
    "tavall-cli-source",
    mapOf(
        "tavall-cli-api" to ":tavall-cli-api",
        "tavall-cli" to ":"
    )
)
includeSourceComposite(
    "TAVALL_EVENTBUS_BUILD",
    "tavall-eventbus-source",
    mapOf("tavall-eventbus" to ":")
)
includeSourceComposite(
    "TAVALL_REGISTRY_BUILD",
    "tavall-registry-source",
    mapOf("tavall-registry" to ":")
)
includeSourceComposite(
    "TAVALL_CONCURRENCY_BUILD",
    "tavall-concurrency-source",
    mapOf("tavall-concurrency" to ":")
)
includeSourceComposite(
    "TAVALL_WEB_BUILD",
    "tavall-web-source",
    mapOf(
        "tavall-web-api" to ":tavall-web-api",
        "tavall-web-frontend" to ":tavall-web-frontend",
        "tavall-web-frontend-codegen" to ":tavall-web-frontend-codegen"
    ),
    "tavallWebBuild"
)
