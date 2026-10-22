plugins {
    java
    `maven-publish`
    id("org.springframework.boot") version "3.5.4" apply false
    id("io.spring.dependency-management") version "1.1.7" apply false
}

allprojects {
    group = "org.tavall"
    version = "0.1.0-SNAPSHOT"

    repositories {
        maven {
            name = "TavallPrivate"
            url = uri("/srv/dev-storage/deps/private")
        }
        maven {
            name = "TavallPrivateSnapshots"
            url = uri("/srv/dev-storage/deps/private/snapshots")
        }
        mavenCentral()
    }
}

subprojects {
    apply(plugin = "java")
    apply(plugin = "maven-publish")

    java {
        toolchain {
            languageVersion = JavaLanguageVersion.of(25)
        }
    }

    dependencies {
        "testImplementation"("org.junit.jupiter:junit-jupiter:5.12.0")
        "testRuntimeOnly"("org.junit.platform:junit-platform-launcher")
    }

    tasks.withType<Test>().configureEach {
        useJUnitPlatform()
    }

    publishing {
        publications {
            create<MavenPublication>("mavenJava") {
                from(components["java"])
            }
        }
        repositories {
            maven {
                name = "TavallPrivateSnapshots"
                url = uri("/srv/dev-storage/deps/private/snapshots")
            }
        }
    }
}

