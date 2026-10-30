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
    api("org.tavall.filesystem:tavall-filesystem-api:0.1.2")
    implementation("org.tavall.filesystem:tavall-filesystem:0.1.2")
    implementation("org.tavall:tavall-database-postgres:1.0.0")
    implementation("org.tavall:tavall-di:1.0.0")
    implementation("jakarta.persistence:jakarta.persistence-api:3.1.0")
    implementation("org.springframework:spring-context:6.2.3")
    implementation("com.fasterxml.jackson.core:jackson-databind:2.18.3")
}
