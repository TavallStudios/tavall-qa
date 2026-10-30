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

val executionCheckpointIntegrationTest by tasks.registering(Test::class) {
    description = "Verifies checkpoint leases and mapped persistence against a real private PostgreSQL process."
    group = "verification"
    testClassesDirs = sourceSets.test.get().output.classesDirs
    classpath = sourceSets.test.get().runtimeClasspath
    useJUnitPlatform()
    filter { includeTestsMatching("*TaskBoardExecutionPostgresIntegrationTest") }
    systemProperty("tavall.qa.checkpoint.postgres.required", "true")
    outputs.upToDateWhen { false }
    outputs.doNotCacheIf("Requires fresh private PostgreSQL process and restart verification on the current Executor") { true }
}
