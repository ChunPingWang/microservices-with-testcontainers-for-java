package com.example.ecommerce.inventory.arch;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.library.Architectures;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

@AnalyzeClasses(
        packages = "com.example.ecommerce.inventory",
        importOptions = ImportOption.DoNotIncludeTests.class)
class HexagonalArchitectureTest {

    @ArchTest
    static final ArchRule LAYERS = Architectures.layeredArchitecture()
            .consideringAllDependencies()
            .layer("Domain").definedBy("..inventory.domain..")
            .layer("Application").definedBy("..inventory.application..")
            .layer("AdapterInbound").definedBy("..inventory.adapter.inbound..")
            .layer("AdapterOutbound").definedBy("..inventory.adapter.outbound..")
            .layer("Config").definedBy("..inventory.config..")

            .whereLayer("Config").mayNotBeAccessedByAnyLayer()
            .whereLayer("AdapterInbound").mayNotBeAccessedByAnyLayer()
            .whereLayer("AdapterOutbound").mayNotBeAccessedByAnyLayer()
            .whereLayer("Application").mayOnlyBeAccessedByLayers("AdapterInbound", "Config")
            .whereLayer("Domain").mayOnlyBeAccessedByLayers(
                    "Application", "AdapterInbound", "AdapterOutbound", "Config");

    @ArchTest
    static final ArchRule DOMAIN_NO_SPRING = noClasses()
            .that().resideInAPackage("..inventory.domain..")
            .should().dependOnClassesThat().resideInAnyPackage(
                    "org.springframework..",
                    "jakarta.persistence..",
                    "org.hibernate..",
                    "org.apache.kafka..",
                    "org.redisson.."
            );
}
