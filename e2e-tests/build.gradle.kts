plugins {
    java
    id("io.spring.dependency-management")
}

evaluationDependsOn(":product-service")
evaluationDependsOn(":payment-service")
evaluationDependsOn(":inventory-service")

dependencies {
    // We DO NOT put the service classes on the test classpath any more —
    // the multi-Spring-Boot-context approach proved fragile. Instead each
    // service runs as its own bootJar in its own JVM (see FullChainE2ETest).
    testImplementation(project(":shared-kernel"))
    testImplementation(project(":test-infrastructure"))

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.testcontainers:testcontainers")
    testImplementation("org.testcontainers:junit-jupiter")
    testImplementation("org.testcontainers:postgresql")
    testImplementation("org.testcontainers:kafka")
    testImplementation("org.testcontainers:vault")
    testImplementation("org.testcontainers:minio")
    testImplementation("org.springframework:spring-webflux")
    testImplementation("org.springframework.vault:spring-vault-core:3.1.2")
    testImplementation("org.awaitility:awaitility:4.2.2")
    testImplementation("io.projectreactor.netty:reactor-netty-http")
    testRuntimeOnly("org.postgresql:postgresql")
}

val productBootJar = project(":product-service").tasks.named("bootJar")
val paymentBootJar = project(":payment-service").tasks.named("bootJar")
val inventoryBootJar = project(":inventory-service").tasks.named("bootJar")

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
    maxHeapSize = "1g"
    environment("TESTCONTAINERS_RYUK_DISABLED", System.getenv("TESTCONTAINERS_RYUK_DISABLED") ?: "true")
    dependsOn(productBootJar, paymentBootJar, inventoryBootJar)
    systemProperty("e2e.product.jar", productBootJar.get().outputs.files.singleFile.absolutePath)
    systemProperty("e2e.payment.jar", paymentBootJar.get().outputs.files.singleFile.absolutePath)
    systemProperty("e2e.inventory.jar", inventoryBootJar.get().outputs.files.singleFile.absolutePath)
    systemProperty("e2e.java.home", System.getProperty("java.home"))
}
