plugins {
    `java-library`
}

dependencies {
    api(project(":tavall-qa-api"))
    implementation("org.tavall:tavall-database-postgres:1.0.0")
    implementation("org.tavall:tavall-di:1.0.0")
    implementation("jakarta.persistence:jakarta.persistence-api:3.1.0")
    implementation("org.springframework:spring-context:6.2.3")
}
