plugins {
    `java-library`
}

dependencies {
    testImplementation(project(":tavall-qa-api"))
    testImplementation(project(":tavall-qa-core"))
    testImplementation("org.junit.jupiter:junit-jupiter:5.12.0")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}
