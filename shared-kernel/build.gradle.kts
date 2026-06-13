plugins {
    `java-library`
}

dependencies {
    // Pure domain — no Spring, no JPA. Only standard JDK + minimal API.
    api("jakarta.annotation:jakarta.annotation-api:3.0.0")
}
