package com.themis.engine.architecture;

import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import jakarta.persistence.Entity;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.repository.Repository;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RestController;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

/**
 * Protects the architectural boundaries of Themis Engine.
 *
 * Target dependency direction:
 *
 * API -> Application -> Domain <- Infrastructure
 *
 * Infrastructure may depend on Domain and Application where wiring or
 * adapters require it. Inner layers must never depend on Infrastructure.
 */
@AnalyzeClasses(
        packages = "com.themis.engine",
        importOptions = {
                ImportOption.DoNotIncludeTests.class
        }
)
class ArchitectureTest {

    private static final String ROOT_PACKAGE =
            "com.themis.engine";

    private static final String API_PACKAGE =
            ROOT_PACKAGE + ".api..";

    private static final String APPLICATION_PACKAGE =
            ROOT_PACKAGE + ".application..";

    private static final String DOMAIN_PACKAGE =
            ROOT_PACKAGE + ".domain..";

    private static final String INFRASTRUCTURE_PACKAGE =
            ROOT_PACKAGE + ".infrastructure..";

    /*
     * ============================================================
     * Domain boundary
     * ============================================================
     */

    @ArchTest
    static final ArchRule domain_must_only_depend_on_domain_and_jdk =
            classes()
                    .that()
                    .resideInAPackage(DOMAIN_PACKAGE)
                    .should()
                    .onlyDependOnClassesThat()
                    .resideInAnyPackage(
                            "java..",
                            DOMAIN_PACKAGE
                    )
                    .because(
                            "the domain must remain framework-independent "
                                    + "and must contain only business rules"
                    );

    @ArchTest
    static final ArchRule domain_must_not_be_spring_managed =
            noClasses()
                    .that()
                    .resideInAPackage(DOMAIN_PACKAGE)
                    .should()
                    .beAnnotatedWith(Service.class)
                    .because(
                            "pure domain classes must be instantiated by "
                                    + "infrastructure configuration"
                    );

    @ArchTest
    static final ArchRule domain_must_not_depend_on_api =
            noClasses()
                    .that()
                    .resideInAPackage(DOMAIN_PACKAGE)
                    .should()
                    .dependOnClassesThat()
                    .resideInAPackage(API_PACKAGE);

    @ArchTest
    static final ArchRule domain_must_not_depend_on_application =
            noClasses()
                    .that()
                    .resideInAPackage(DOMAIN_PACKAGE)
                    .should()
                    .dependOnClassesThat()
                    .resideInAPackage(APPLICATION_PACKAGE);

    @ArchTest
    static final ArchRule domain_must_not_depend_on_infrastructure =
            noClasses()
                    .that()
                    .resideInAPackage(DOMAIN_PACKAGE)
                    .should()
                    .dependOnClassesThat()
                    .resideInAPackage(INFRASTRUCTURE_PACKAGE);

    /*
     * ============================================================
     * Application boundary
     * ============================================================
     */

    @ArchTest
    static final ArchRule application_must_not_depend_on_api =
            noClasses()
                    .that()
                    .resideInAPackage(APPLICATION_PACKAGE)
                    .should()
                    .dependOnClassesThat()
                    .resideInAPackage(API_PACKAGE);

    @ArchTest
    static final ArchRule application_must_not_depend_on_infrastructure =
            noClasses()
                    .that()
                    .resideInAPackage(APPLICATION_PACKAGE)
                    .should()
                    .dependOnClassesThat()
                    .resideInAPackage(INFRASTRUCTURE_PACKAGE)
                    .because(
                            "application use cases must depend on outbound ports, "
                                    + "not concrete infrastructure adapters"
                    );

    @ArchTest
    static final ArchRule application_commands_must_be_records =
            classes()
                    .that()
                    .resideInAPackage("..application..command..")
                    .should()
                    .beRecords();

    @ArchTest
    static final ArchRule application_commands_must_not_depend_on_api =
            noClasses()
                    .that()
                    .resideInAPackage("..application..command..")
                    .should()
                    .dependOnClassesThat()
                    .resideInAPackage(API_PACKAGE);

    @ArchTest
    static final ArchRule application_services_must_use_explicit_names =
            classes()
                    .that()
                    .resideInAPackage(APPLICATION_PACKAGE)
                    .and()
                    .areAnnotatedWith(Service.class)
                    .should()
                    .haveSimpleNameEndingWith("CommandService")
                    .orShould()
                    .haveSimpleNameEndingWith("QueryService")
                    .because(
                            "application services must be explicitly separated "
                                    + "by command and query responsibility"
                    );

    @ArchTest
    static final ArchRule command_services_must_reside_in_application =
            classes()
                    .that()
                    .haveSimpleNameEndingWith("CommandService")
                    .should()
                    .resideInAPackage(APPLICATION_PACKAGE)
                    .andShould()
                    .beAnnotatedWith(Service.class);

    @ArchTest
    static final ArchRule query_services_must_reside_in_application =
            classes()
                    .that()
                    .haveSimpleNameEndingWith("QueryService")
                    .should()
                    .resideInAPackage(APPLICATION_PACKAGE)
                    .andShould()
                    .beAnnotatedWith(Service.class);

    @ArchTest
    static final ArchRule command_services_must_have_write_transactions =
            classes()
                    .that()
                    .haveSimpleNameEndingWith("CommandService")
                    .should(
                            haveClassLevelTransactionalAnnotation(false)
                    );

    @ArchTest
    static final ArchRule query_services_must_have_read_only_transactions =
            classes()
                    .that()
                    .haveSimpleNameEndingWith("QueryService")
                    .should(
                            haveClassLevelTransactionalAnnotation(true)
                    );

    @ArchTest
    static final ArchRule query_services_must_not_depend_on_command_services =
            noClasses()
                    .that()
                    .haveSimpleNameEndingWith("QueryService")
                    .should()
                    .dependOnClassesThat()
                    .haveSimpleNameEndingWith("CommandService")
                    .because(
                            "queries must never trigger state-changing use cases"
                    );

    @ArchTest
    static final ArchRule command_services_must_not_depend_on_query_services =
            noClasses()
                    .that()
                    .haveSimpleNameEndingWith("CommandService")
                    .should()
                    .dependOnClassesThat()
                    .haveSimpleNameEndingWith("QueryService")
                    .because(
                            "commands should access domain ports directly "
                                    + "instead of invoking query orchestration"
                    );

    /*
     * ============================================================
     * API boundary
     * ============================================================
     */

    @ArchTest
    static final ArchRule api_must_not_depend_on_infrastructure =
            noClasses()
                    .that()
                    .resideInAPackage(API_PACKAGE)
                    .should()
                    .dependOnClassesThat()
                    .resideInAPackage(INFRASTRUCTURE_PACKAGE)
                    .because(
                            "HTTP adapters must call application use cases, "
                                    + "not persistence implementations"
                    );

    @ArchTest
    static final ArchRule controllers_must_reside_in_api =
            classes()
                    .that()
                    .haveSimpleNameEndingWith("Controller")
                    .should()
                    .resideInAPackage(API_PACKAGE)
                    .andShould()
                    .beAnnotatedWith(RestController.class);

    @ArchTest
    static final ArchRule controllers_must_not_depend_on_store_ports =
            noClasses()
                    .that()
                    .haveSimpleNameEndingWith("Controller")
                    .should()
                    .dependOnClassesThat()
                    .haveSimpleNameEndingWith("Store")
                    .because(
                            "controllers must call application services "
                                    + "instead of accessing persistence ports directly"
                    );

    @ArchTest
    static final ArchRule api_mappers_must_reside_in_api =
            classes()
                    .that()
                    .haveSimpleNameEndingWith("ApiMapper")
                    .should()
                    .resideInAPackage(API_PACKAGE)
                    .andShould()
                    .beAnnotatedWith(Component.class);

    /*
     * ============================================================
     * Ports and infrastructure
     * ============================================================
     */

    @ArchTest
    static final ArchRule store_ports_must_be_domain_interfaces =
            classes()
                    .that()
                    .haveSimpleNameEndingWith("Store")
                    .should()
                    .resideInAPackage(DOMAIN_PACKAGE)
                    .andShould()
                    .beInterfaces()
                    .because(
                            "outbound persistence ports belong to the domain boundary"
                    );

    @ArchTest
    static final ArchRule jpa_entities_must_reside_in_infrastructure =
            classes()
                    .that()
                    .areAnnotatedWith(Entity.class)
                    .should()
                    .resideInAPackage(INFRASTRUCTURE_PACKAGE)
                    .because(
                            "JPA entities are persistence details"
                    );

    @ArchTest
    static final ArchRule spring_data_repositories_must_reside_in_infrastructure =
            classes()
                    .that()
                    .areAssignableTo(Repository.class)
                    .should()
                    .resideInAPackage(INFRASTRUCTURE_PACKAGE)
                    .because(
                            "Spring Data repositories are infrastructure adapters"
                    );

    @ArchTest
    static final ArchRule repository_adapters_must_reside_in_infrastructure =
            classes()
                    .that()
                    .haveSimpleNameEndingWith("RepositoryAdapter")
                    .should()
                    .resideInAPackage(INFRASTRUCTURE_PACKAGE);

    @ArchTest
    static final ArchRule spring_configuration_must_reside_in_infrastructure =
            classes()
                    .that()
                    .areAnnotatedWith(Configuration.class)
                    .should()
                    .resideInAPackage(INFRASTRUCTURE_PACKAGE)
                    .because(
                            "framework wiring belongs to infrastructure"
                    );

    @ArchTest
    static final ArchRule spring_services_must_reside_in_application =
            classes()
                    .that()
                    .areAnnotatedWith(Service.class)
                    .should()
                    .resideInAPackage(APPLICATION_PACKAGE)
                    .because(
                            "domain services must be pure and infrastructure "
                                    + "adapters should use adapter-specific stereotypes"
                    );

    /*
     * ============================================================
     * Cycle protection
     * ============================================================
     */

    @ArchTest
    static final ArchRule top_level_layers_must_be_free_of_cycles =
            slices()
                    .matching(ROOT_PACKAGE + ".(*)..")
                    .should()
                    .beFreeOfCycles()
                    .because(
                            "cyclic dependencies make architectural boundaries unstable"
                    );

    /*
     * ============================================================
     * Custom transaction condition
     * ============================================================
     */

    private static ArchCondition<JavaClass>
    haveClassLevelTransactionalAnnotation(boolean expectedReadOnly) {

        return new ArchCondition<>(
                "declare class-level @Transactional(readOnly = "
                        + expectedReadOnly
                        + ")"
        ) {
            @Override
            public void check(
                    JavaClass javaClass,
                    ConditionEvents events
            ) {
                if (!javaClass.isAnnotatedWith(Transactional.class)) {
                    events.add(
                            new SimpleConditionEvent(
                                    javaClass,
                                    false,
                                    javaClass.getName()
                                            + " does not declare class-level "
                                            + "@Transactional"
                            )
                    );

                    return;
                }

                Transactional transactional =
                        javaClass.getAnnotationOfType(
                                Transactional.class
                        );

                boolean satisfied =
                        transactional.readOnly() == expectedReadOnly;

                String message =
                        javaClass.getName()
                                + " declares @Transactional(readOnly = "
                                + transactional.readOnly()
                                + "), expected readOnly = "
                                + expectedReadOnly;

                events.add(
                        new SimpleConditionEvent(
                                javaClass,
                                satisfied,
                                message
                        )
                );
            }
        };
    }
}
