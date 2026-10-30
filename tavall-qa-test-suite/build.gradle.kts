plugins {
    `java-library`
}

dependencies {
    testImplementation(project(":tavall-qa-api"))
    testImplementation(project(":tavall-qa-system"))
    testImplementation("org.tavall.filesystem:tavall-filesystem-api:0.1.2")
    testImplementation("org.tavall.filesystem:tavall-filesystem:0.1.2")
    testImplementation("org.tavall:tavall-database-postgres:1.0.0")
    testImplementation("org.tavall:tavall-di:1.0.0")
    testImplementation("org.junit.jupiter:junit-jupiter:5.12.0")
    testImplementation("com.fasterxml.jackson.core:jackson-databind:2.18.3")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}
