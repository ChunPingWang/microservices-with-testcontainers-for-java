package com.example.ecommerce.product.arch;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.library.Architectures;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

/**
 * Enforces Hexagonal Architecture for product-service:
 *   - domain layer is framework-free
 *   - application layer only depends on domain
 *   - inbound/outbound adapters depend on domain (port interfaces),
 *     not the other way round
 */
@AnalyzeClasses(
        packages = "com.example.ecommerce.product",
        importOptions = ImportOption.DoNotIncludeTests.class)
class HexagonalArchitectureTest {

    @ArchTest
    static final ArchRule LAYERS = Architectures.layeredArchitecture()
            .consideringAllDependencies()
            .layer("Domain").definedBy("..product.domain..")
            .layer("Application").definedBy("..product.application..")
            .layer("AdapterInbound").definedBy("..product.adapter.inbound..")
            .layer("AdapterOutbound").definedBy("..product.adapter.outbound..")
            .layer("Config").definedBy("..product.config..")

            .whereLayer("Config").mayNotBeAccessedByAnyLayer()
            .whereLayer("AdapterInbound").mayNotBeAccessedByAnyLayer()
            .whereLayer("AdapterOutbound").mayNotBeAccessedByAnyLayer()
            .whereLayer("Application").mayOnlyBeAccessedByLayers("AdapterInbound", "Config")
            .whereLayer("Domain").mayOnlyBeAccessedByLayers(
                    "Application", "AdapterInbound", "AdapterOutbound", "Config");

    @ArchTest
    static final ArchRule DOMAIN_NO_SPRING = noClasses()
            .that().resideInAPackage("..product.domain..")
            .should().dependOnClassesThat().resideInAnyPackage(
                    "org.springframework..",
                    "jakarta.persistence..",
                    "org.hibernate..",
                    "org.apache.kafka..",
                    "org.elasticsearch..",
                    "org.redisson..",
                    "software.amazon.awssdk.."
            )
            .because("domain must remain framework-agnostic so adapters can be swapped freely");

    @ArchTest
    static final ArchRule APPLICATION_NO_INFRASTRUCTURE = noClasses()
            .that().resideInAPackage("..product.application..")
            .should().dependOnClassesThat().resideInAnyPackage(
                    "jakarta.persistence..",
                    "org.hibernate..",
                    "org.apache.kafka..",
                    "org.elasticsearch.."
            )
            .because("application services use domain ports, not concrete infrastructure");
}
