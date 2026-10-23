plugins {
    `java-library`
}

dependencies {
    testImplementation(project(":tavall-qa-api"))
    testImplementation(project(":tavall-qa-system"))
    testImplementation("org.tavall:tavall-database-postgres:1.0.0")
    testImplementation("org.junit.jupiter:junit-jupiter:5.12.0")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}
