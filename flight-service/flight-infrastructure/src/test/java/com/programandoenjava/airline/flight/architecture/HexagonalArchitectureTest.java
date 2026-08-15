package com.programandoenjava.airline.flight.architecture;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.library.Architectures;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

/**
 * Architecture rules for flight-service.
 */
@AnalyzeClasses(
        packages = "com.programandoenjava.airline.flight",
        importOptions = ImportOption.DoNotIncludeTests.class)
class HexagonalArchitectureTest {

    private static final String DOMAIN = "..flight.domain..";
    private static final String APPLICATION = "..flight.application..";
    private static final String INFRASTRUCTURE = "..flight.infrastructure..";

    private static final String INBOUND_ADAPTERS = "..infrastructure.adapter.in..";
    private static final String OUTBOUND_ADAPTERS = "..infrastructure.adapter.out..";
    private static final String PERSISTENCE_ADAPTER = "..infrastructure.adapter.out.persistence..";

    @ArchTest
    static final ArchRule layersPointInwardsOnly = Architectures.layeredArchitecture()
            .consideringOnlyDependenciesInLayers()
            .optionalLayer("Domain").definedBy(DOMAIN)
            .optionalLayer("Application").definedBy(APPLICATION)
            .optionalLayer("Infrastructure").definedBy(INFRASTRUCTURE)
            .whereLayer("Infrastructure").mayNotBeAccessedByAnyLayer()
            .whereLayer("Application").mayOnlyBeAccessedByLayers("Infrastructure")
            .whereLayer("Domain").mayOnlyBeAccessedByLayers("Application", "Infrastructure")
            .as("layers may only point inwards: infrastructure to application to domain");

    @ArchTest
    static final ArchRule domainIsFrameworkFree = noClasses()
            .that().resideInAPackage(DOMAIN)
            .should().dependOnClassesThat().resideInAnyPackage(
                    "org.springframework..",
                    "jakarta.persistence..",
                    "jakarta.validation..",
                    "org.hibernate..",
                    "com.fasterxml.jackson..")
            .as("flight-domain must not depend on any framework")
            .allowEmptyShould(true); // TODO remove once flight-domain has classes

    @ArchTest
    static final ArchRule applicationIsFrameworkFree = noClasses()
            .that().resideInAPackage(APPLICATION)
            .should().dependOnClassesThat().resideInAnyPackage(
                    "org.springframework..",
                    "jakarta.persistence..",
                    "org.hibernate..")
            .as("flight-application must not depend on Spring or the persistence provider")
            .allowEmptyShould(true); // TODO remove once flight-application has classes

    @ArchTest
    static final ArchRule inboundAdaptersMustNotReachOutboundAdapters = noClasses()
            .that().resideInAPackage(INBOUND_ADAPTERS)
            .should().dependOnClassesThat().resideInAPackage(OUTBOUND_ADAPTERS)
            .as("inbound adapters must go through a use case, never straight to an outbound adapter")
            .allowEmptyShould(true); // TODO remove once the first web adapter exists

    @ArchTest
    static final ArchRule jpaEntitiesStayInsideThePersistenceAdapter = noClasses()
            .that().resideOutsideOfPackage(PERSISTENCE_ADAPTER)
            .should().dependOnClassesThat().areAnnotatedWith("jakarta.persistence.Entity")
            .as("JPA entities must not escape the persistence adapter")
            .allowEmptyShould(true); // TODO remove once the first entity exists
}
