package com.example.ecommerce.payment.arch;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.library.Architectures;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

@AnalyzeClasses(
        packages = "com.example.ecommerce.payment",
        importOptions = ImportOption.DoNotIncludeTests.class)
class HexagonalArchitectureTest {

    @ArchTest
    static final ArchRule LAYERS = Architectures.layeredArchitecture()
            .consideringAllDependencies()
            .layer("Domain").definedBy("..payment.domain..")
            .layer("Application").definedBy("..payment.application..")
            .layer("AdapterInbound").definedBy("..payment.adapter.inbound..")
            .layer("AdapterOutbound").definedBy("..payment.adapter.outbound..")
            .layer("Config").definedBy("..payment.config..")

            .whereLayer("Config").mayNotBeAccessedByAnyLayer()
            .whereLayer("AdapterInbound").mayNotBeAccessedByAnyLayer()
            .whereLayer("AdapterOutbound").mayNotBeAccessedByAnyLayer()
            .whereLayer("Application").mayOnlyBeAccessedByLayers("AdapterInbound", "Config")
            .whereLayer("Domain").mayOnlyBeAccessedByLayers(
                    "Application", "AdapterInbound", "AdapterOutbound", "Config");

    @ArchTest
    static final ArchRule DOMAIN_NO_SPRING = noClasses()
            .that().resideInAPackage("..payment.domain..")
            .should().dependOnClassesThat().resideInAnyPackage(
                    "org.springframework..",
                    "jakarta.persistence..",
                    "org.hibernate..",
                    "org.apache.kafka..",
                    "com.google.cloud..",
                    "software.amazon.awssdk.."
            )
            .because("payment domain must be infrastructure-free");
}
