pluginManagement {
    val tavallWebBuild = providers.gradleProperty("tavallWebBuild")
        .orElse(providers.environmentVariable("TAVALL_WEB_BUILD"))
        .orElse(providers.provider { file("/srv/dev-storage/workspaces/tavall-web/repo_root").takeIf { it.isDirectory }?.absolutePath ?: "" })
        .orNull
    if (!tavallWebBuild.isNullOrBlank()) {
        val buildDirectory = file(tavallWebBuild).canonicalFile
        includeBuild(buildDirectory.resolve("tavall-web-frontend-gradle")) {
            name = "tavall-web-frontend-gradle-plugin-provider"
        }
    }
    repositories {
        maven {
            name = "TavallPrivateSnapshots"
            url = uri("/srv/dev-storage/deps/private/snapshots")
            content {
                includeGroupByRegex("org\\.tavall(?:\\..*)?")
                includeGroupByRegex("com\\.tavall(?:\\..*)?")
            }
        }
        maven {
            name = "TavallPrivate"
            url = uri("/srv/dev-storage/deps/private")
            content {
                includeGroupByRegex("org\\.tavall(?:\\..*)?")
                includeGroupByRegex("com\\.tavall(?:\\..*)?")
            }
        }
        gradlePluginPortal()
        mavenCentral()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.PREFER_SETTINGS)
    repositories {
        maven {
            name = "TavallPrivateSnapshots"
            url = uri(providers.environmentVariable("TAVALL_PRIVATE_MAVEN_REPOSITORY")
                .orElse("/srv/dev-storage/deps/private/snapshots"))
            content {
                includeGroupByRegex("org\\.tavall(?:\\..*)?")
                includeGroupByRegex("com\\.tavall(?:\\..*)?")
            }
        }
        maven {
            name = "TavallPrivate"
            url = uri("/srv/dev-storage/deps/private")
            content {
                includeGroupByRegex("org\\.tavall(?:\\..*)?")
                includeGroupByRegex("com\\.tavall(?:\\..*)?")
            }
        }
        mavenCentral {
            content {
                excludeGroupByRegex("org\\.tavall(?:\\..*)?")
                excludeGroupByRegex("com\\.tavall(?:\\..*)?")
            }
        }
        ivy {
            name = "Node.js"
            url = uri("https://nodejs.org/dist/")
            patternLayout {
                artifact("v[revision]/[artifact](-v[revision]-[classifier]).[ext]")
            }
            metadataSources {
                artifact()
            }
            content {
                includeModule("org.nodejs", "node")
            }
        }
    }
}

rootProject.name = "tavall-web-qa"

val tavallWebBuild = providers.gradleProperty("tavallWebBuild")
    .orElse(providers.environmentVariable("TAVALL_WEB_BUILD"))
    .orElse(providers.provider { file("/srv/dev-storage/workspaces/tavall-web/repo_root").takeIf { it.isDirectory }?.absolutePath ?: "" })
    .orNull
if (!tavallWebBuild.isNullOrBlank()) {
    val buildDirectory = file(tavallWebBuild).canonicalFile
    includeBuild(buildDirectory) {
        name = "tavall-web-source"
        dependencySubstitution {
            substitute(module("org.tavall:tavall-web-api")).using(project(":tavall-web-api"))
            substitute(module("org.tavall:tavall-web-frontend")).using(project(":tavall-web-frontend"))
        }
    }
}

val tavallQaBuild = providers.gradleProperty("tavallQaBuild")
    .orElse(providers.environmentVariable("TAVALL_QA_BUILD"))
    .orElse(providers.provider { file("/srv/dev-storage/workspaces/tavall-qa/repo_root").takeIf { it.isDirectory }?.absolutePath ?: "" })
    .orNull
if (!tavallQaBuild.isNullOrBlank()) {
    val buildDirectory = file(tavallQaBuild).canonicalFile
    includeBuild(buildDirectory) {
        name = "tavall-qa-source"
        dependencySubstitution {
            substitute(module("org.tavall:tavall-qa-api")).using(project(":tavall-qa-api"))
            substitute(module("org.tavall:tavall-qa-core")).using(project(":tavall-qa-core"))
        }
    }
}
