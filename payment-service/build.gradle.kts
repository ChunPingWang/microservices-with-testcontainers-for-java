plugins {
    id("org.springframework.boot")
    id("io.spring.dependency-management")
    java
}

dependencies {
    implementation(project(":shared-kernel"))

    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.kafka:spring-kafka")
    implementation("org.flywaydb:flyway-core")
    implementation("org.flywaydb:flyway-database-postgresql")

    runtimeOnly("org.postgresql:postgresql")

    // Vault
    implementation("org.springframework.cloud:spring-cloud-starter-vault-config:4.1.3")

    // MinIO via AWS SDK
    implementation("software.amazon.awssdk:s3:2.29.34")

    // GCP Pub/Sub (notification adapter)
    implementation("com.google.cloud:google-cloud-pubsub:1.151.0")

    testImplementation(project(":test-infrastructure"))
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.kafka:spring-kafka-test")
    testImplementation("org.testcontainers:testcontainers")
    testImplementation("org.testcontainers:junit-jupiter")
    testImplementation("org.testcontainers:postgresql")
    testImplementation("org.testcontainers:kafka")
    testImplementation("org.testcontainers:minio")
    testImplementation("org.testcontainers:vault")
    testImplementation("org.testcontainers:gcloud")
    testImplementation("com.tngtech.archunit:archunit-junit5:1.3.0")
}

springBoot {
    mainClass.set("com.example.ecommerce.payment.PaymentServiceApplication")
}

tasks.withType<Test>().configureEach {
    maxHeapSize = "1g"
}
