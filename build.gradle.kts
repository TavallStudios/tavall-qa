import org.gradle.api.publish.maven.MavenPublication

plugins {
    `java-library`
    `maven-publish`
    id("org.tavall.web.frontend") version "0.1.1-SNAPSHOT"
}

group = "org.tavall"
version = "0.1.1-SNAPSHOT"

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(25))
    }
    withSourcesJar()
}

dependencies {
    api("org.tavall:tavall-web-api:0.1.1-SNAPSHOT")
    api("org.tavall:tavall-web-frontend:0.1.1-SNAPSHOT")
    api("org.tavall:tavall-qa-api:0.1.0-SNAPSHOT")

    testImplementation("org.junit.jupiter:junit-jupiter:5.11.4")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher:1.11.4")
}

tavallFrontend {
    generatedPackage.set("org.tavall.qa.web.generated")
}

tasks.withType<JavaCompile>().configureEach {
    options.release.set(25)
    options.encoding = "UTF-8"
    options.compilerArgs.add("-parameters")
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            from(components["java"])
            artifactId = "tavall-web-qa"
        }
    }
    repositories {
        maven {
            name = "TavallPrivateSnapshots"
            url = uri("/srv/dev-storage/deps/private/snapshots")
        }
    }
}
