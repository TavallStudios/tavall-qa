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

dependencies {
    dependencyAccessLowerer("org.tavall:tavall-di:1.0.0")
    api(project(":tavall-qa-api"))
    api("org.tavall:tavall-cli-api:0.1.0-SNAPSHOT")
    api("org.tavall:tavall-di:1.0.0")
    implementation("com.fasterxml.jackson.core:jackson-databind:2.18.3")
    testImplementation("org.tavall:tavall-cli:0.1.0-SNAPSHOT")
    testImplementation("org.junit.jupiter:junit-jupiter:5.12.0")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}
