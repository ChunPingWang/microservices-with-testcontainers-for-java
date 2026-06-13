plugins {
    `java-library`
}

dependencies {
    api(project(":shared-kernel"))
    api(platform("org.testcontainers:testcontainers-bom:1.20.4"))
    api("org.testcontainers:testcontainers")
    api("org.testcontainers:junit-jupiter")
    api("org.testcontainers:postgresql")
    api("org.testcontainers:kafka")
    api("org.testcontainers:elasticsearch")
    api("org.testcontainers:minio")
    api("org.testcontainers:vault")
    api("org.testcontainers:gcloud")
    api("org.testcontainers:toxiproxy")
    api("com.github.dasniko:testcontainers-keycloak:3.5.1")
}
