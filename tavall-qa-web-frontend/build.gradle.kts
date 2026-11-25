import org.gradle.api.publish.maven.MavenPublication

plugins {
    `java-library`
    id("org.tavall.web.frontend") version "0.1.1-SNAPSHOT"
}

group = "org.tavall"
version = "0.1.1-SNAPSHOT"

base {
    archivesName.set("tavall-web-qa")
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(25))
    }
    withSourcesJar()
}

dependencies {
    api("org.tavall:tavall-web-api:0.1.1-SNAPSHOT")
    api("org.tavall:tavall-web-frontend:0.1.1-SNAPSHOT")
    api(project(":tavall-qa-api"))

    testImplementation("org.junit.jupiter:junit-jupiter:5.11.4")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher:1.11.4")
}

tavallFrontend {
    generatedPackage.set("org.tavall.qa.web.generated")
}

configure<com.github.gradle.node.NodeExtension> {
    download.set(false)
}

tasks.withType<JavaCompile>().configureEach {
    options.release.set(25)
    options.encoding = "UTF-8"
    options.compilerArgs.add("-parameters")
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}
