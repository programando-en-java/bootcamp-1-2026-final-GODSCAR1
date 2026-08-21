package com.programandoenjava.airline.payment.architecture;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.library.Architectures;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

@AnalyzeClasses(
        packages = "com.programandoenjava.airline.payment",
        importOptions = ImportOption.DoNotIncludeTests.class)
class HexagonalArchitectureTest {

    private static final String DOMAIN = "..payment.domain..";
    private static final String APPLICATION = "..payment.application..";
    private static final String INFRASTRUCTURE = "..payment.infrastructure..";

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
                    "tools.jackson..")
            .as("payment-domain must not depend on any framework");

    @ArchTest
    static final ArchRule applicationIsFrameworkFree = noClasses()
            .that().resideInAPackage(APPLICATION)
            .should().dependOnClassesThat().resideInAnyPackage(
                    "org.springframework..",
                    "jakarta.persistence..",
                    "org.hibernate..")
            .as("payment-application must not depend on Spring or the persistence provider");

    @ArchTest
    static final ArchRule theCallToBookingServiceStaysInTheAdapter = noClasses()
            .that().resideInAnyPackage(DOMAIN, APPLICATION)
            .should().dependOnClassesThat().resideInAnyPackage(
                    "feign..",
                    "org.springframework.cloud..",
                    "io.github.resilience4j..")
            .as("Feign and Resilience4j belong to the outbound adapter, not to a use case");

    @ArchTest
    static final ArchRule inboundAdaptersMustNotReachOutboundAdapters = noClasses()
            .that().resideInAPackage(INBOUND_ADAPTERS)
            .should().dependOnClassesThat().resideInAPackage(OUTBOUND_ADAPTERS)
            .as("inbound adapters must go through a use case, never straight to an outbound adapter");

    @ArchTest
    static final ArchRule jpaEntitiesStayInsideThePersistenceAdapter = noClasses()
            .that().resideOutsideOfPackage(PERSISTENCE_ADAPTER)
            .should().dependOnClassesThat().areAnnotatedWith("jakarta.persistence.Entity")
            .as("JPA entities must not escape the persistence adapter");

    @ArchTest
    static final ArchRule onlyTheGatewaySeesACardNumber = noClasses()
            .that().resideOutsideOfPackages(
                    "..payment.domain.payment..",
                    "..application.port.in.paybooking..",
                    "..application.port.out.gateway..",
                    "..application.usecase..",
                    "..infrastructure.adapter.in.web..",
                    "..infrastructure.adapter.out.gateway..")
            .should().dependOnClassesThat()
            .haveFullyQualifiedName("com.programandoenjava.airline.payment.domain.payment.CardNumber")
            .as("a card number must not travel beyond the charge that needs it");
}
