import java.io.File
import java.util.TreeMap
import java.util.zip.ZipFile

plugins {
    `java-library`
}

val dependencyAccessLowerer by configurations.creating

val lowerDependencyAccess by tasks.registering(JavaExec::class) {
    classpath = dependencyAccessLowerer
    mainClass.set("org.tavall.dependency.access.DependencyAccessSourceLowererMain")
    javaLauncher.set(javaToolchains.launcherFor {
        languageVersion.set(JavaLanguageVersion.of(25))
    })
    val authored = layout.projectDirectory.dir("src/main/java")
    val lowered = layout.buildDirectory.dir("generated/sources/dependencyAccess/main")
    inputs.dir(authored)
    outputs.dir(lowered)
    args("--source", authored.asFile.absolutePath, "--output", lowered.get().asFile.absolutePath)
}

sourceSets.main {
    java.setSrcDirs(listOf(layout.buildDirectory.dir("generated/sources/dependencyAccess/main")))
}

tasks.named("compileJava") {
    dependsOn(lowerDependencyAccess)
}

// The library jar keeps a distinct archive name so the CI-published self-contained
// artifact (allJar) owns the canonical `tavall-qa-cli-<version>.jar` path while
// `maven-publish` still uploads the thin library under the module coordinates.
tasks.jar {
    archiveFileName.set("tavall-qa-cli-${project.version}-lib.jar")
}

val allJarServices = layout.buildDirectory.dir("generated/allJar-services")

val mergeAllJarServices = tasks.register("mergeAllJarServices") {
    group = "build"
    description = "Merges duplicate META-INF/services provider declarations from the runtime classpath."
    inputs.files(sourceSets.main.get().output).withPropertyName("mainOutput")
    inputs.files(configurations.runtimeClasspath).withPropertyName("runtimeClasspath")
    outputs.dir(allJarServices)
    doLast {
        val target = allJarServices.get().asFile
        target.deleteRecursively()
        target.mkdirs()
        val serviceProviders = TreeMap<String, MutableSet<String>>()
        fun collect(serviceName: String, content: String) {
            val lines = content.lines().map { it.trim() }
                .filter { it.isNotEmpty() && !it.startsWith("#") }
            if (lines.isNotEmpty()) {
                serviceProviders.getOrPut(serviceName) { linkedSetOf() }.addAll(lines)
            }
        }
        sourceSets.main.get().output.resourcesDir?.let { resourcesDir ->
            val servicesDir = File(resourcesDir, "META-INF/services")
            if (servicesDir.isDirectory) {
                servicesDir.walkTopDown().filter { it.isFile }.forEach {
                    collect(it.name, it.readText())
                }
            }
        }
        configurations.runtimeClasspath.get().forEach { file ->
            if (file.isFile && file.extension == "jar") {
                ZipFile(file).use { jar ->
                    jar.entries().asSequence()
                        .filter { !it.isDirectory && it.name.startsWith("META-INF/services/") }
                        .forEach { entry ->
                            collect(
                                entry.name.removePrefix("META-INF/services/"),
                                jar.getInputStream(entry).readBytes().decodeToString()
                            )
                        }
                }
            } else if (file.isDirectory) {
                val servicesDir = File(file, "META-INF/services")
                if (servicesDir.isDirectory) {
                    servicesDir.walkTopDown().filter { it.isFile }.forEach {
                        collect(it.name, it.readText())
                    }
                }
            }
        }
        serviceProviders.forEach { (serviceName, providerLines) ->
            val serviceFile = File(target, "META-INF/services/$serviceName")
            serviceFile.parentFile.mkdirs()
            serviceFile.writeText(providerLines.joinToString("\n", postfix = "\n"))
        }
    }
}

tasks.register<Jar>("allJar") {
    group = "build"
    description = "Builds the self-contained runnable tavall-qa-cli artifact published by Tavall CI."
    archiveFileName.set("tavall-qa-cli-${project.version}.jar")
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    manifest {
        attributes["Main-Class"] = "org.tavall.qa.cli.QaCliMain"
    }
    from(sourceSets.main.get().output) {
        exclude("META-INF/services/**")
    }
    dependsOn(configurations.runtimeClasspath, mergeAllJarServices)
    from({ configurations.runtimeClasspath.get().map { if (it.isDirectory) it else zipTree(it) } }) {
        // Strip signature files so `java -jar` never fails with SecurityException.
        exclude("META-INF/*.SF", "META-INF/*.DSA", "META-INF/*.RSA", "META-INF/*.EC")
        // Raw provider declarations are replaced by the merged service files.
        exclude("META-INF/services/**")
    }
    from(allJarServices)
}

dependencies {
    dependencyAccessLowerer("org.tavall:tavall-di:1.0.0")
    api(project(":tavall-qa-api"))
    api("org.tavall:tavall-cli-api:0.1.0-SNAPSHOT")
    api("org.tavall:tavall-di:1.0.0")
    implementation("org.tavall:tavall-cli:0.1.0-SNAPSHOT")
    implementation("com.fasterxml.jackson.core:jackson-databind:2.18.3")
    testImplementation("org.junit.jupiter:junit-jupiter:5.12.0")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}
