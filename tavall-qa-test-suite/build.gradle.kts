plugins {
    `java-library`
}

dependencies {
    testImplementation(project(":tavall-qa-api"))
    testImplementation(project(":tavall-qa-system"))
    testImplementation("org.tavall:tavall-database-postgres:1.0.0")
    testImplementation("org.tavall:tavall-di:1.0.0")
    testImplementation("org.junit.jupiter:junit-jupiter:5.12.0")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

val executionCheckpointIntegrationTest by tasks.registering(Test::class) {
    description = "Verifies checkpoint leases and mapped persistence against a real private PostgreSQL process."
    group = "verification"
    testClassesDirs = sourceSets.test.get().output.classesDirs
    classpath = sourceSets.test.get().runtimeClasspath
    useJUnitPlatform()
    filter { includeTestsMatching("*TaskBoardExecutionPostgresIntegrationTest") }
    systemProperty("tavall.qa.checkpoint.postgres.required", "true")
}
