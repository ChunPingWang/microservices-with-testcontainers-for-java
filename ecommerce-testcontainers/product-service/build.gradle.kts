plugins {
    id("org.springframework.boot")
    id("io.spring.dependency-management")
    java
}

dependencies {
    implementation(project(":shared-kernel"))

    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-data-redis")
    implementation("org.springframework.boot:spring-boot-starter-data-elasticsearch")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-oauth2-resource-server")
    implementation("org.springframework.kafka:spring-kafka")
    implementation("org.flywaydb:flyway-core")
    implementation("org.flywaydb:flyway-database-postgresql")

    runtimeOnly("org.postgresql:postgresql")

    // MinIO (S3-compatible) via AWS SDK v2
    implementation("software.amazon.awssdk:s3:2.29.34")

    testImplementation(project(":test-infrastructure"))
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.kafka:spring-kafka-test")
    testImplementation("org.testcontainers:testcontainers")
    testImplementation("org.testcontainers:junit-jupiter")
    testImplementation("org.testcontainers:postgresql")
    testImplementation("org.testcontainers:kafka")
    testImplementation("org.testcontainers:elasticsearch")
    testImplementation("org.testcontainers:minio")
    testImplementation("com.tngtech.archunit:archunit-junit5:1.3.0")

    // Keycloak Testcontainers (Dasniko module)
    testImplementation("com.github.dasniko:testcontainers-keycloak:3.5.1")
}

springBoot {
    mainClass.set("com.example.ecommerce.product.ProductServiceApplication")
}

tasks.withType<Test>().configureEach {
    // Use distinct JVM args to isolate test classes that share Testcontainers
    maxHeapSize = "1g"
}
