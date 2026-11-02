package com.pydlove.frametalk.boot;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.Test;

class ModuleBoundaryTest {

    private final JavaClasses classes =
            new ClassFileImporter().importPackages("com.pydlove.frametalk");

    private static final ArchRule DOMAIN_AND_APPLICATION_MUST_NOT_DEPEND_ON_FRAMEWORKS =
            noClasses()
                    .that()
                    .resideInAnyPackage("..domain..", "..application..")
                    .should()
                    .dependOnClassesThat()
                    .resideInAnyPackage(
                            "org.springframework..",
                            "org.apache.ibatis..",
                            "jakarta.servlet..",
                            "com.pydlove.frametalk..infrastructure..",
                            "com.pydlove.frametalk..interfaces.."
                    )
                    // Remove once the first production domain/application classes exist.
                    .allowEmptyShould(true);

    private static final ArchRule INTERFACES_MUST_NOT_DEPEND_ON_INFRASTRUCTURE =
            noClasses()
                    .that()
                    .resideInAPackage("..interfaces..")
                    .should()
                    .dependOnClassesThat()
                    .resideInAPackage("com.pydlove.frametalk..infrastructure..")
                    .allowEmptyShould(true);

    private static final ArchRule INFRASTRUCTURE_MUST_NOT_DEPEND_ON_INTERFACES =
            noClasses()
                    .that()
                    .resideInAPackage("..infrastructure..")
                    .should()
                    .dependOnClassesThat()
                    .resideInAPackage("com.pydlove.frametalk..interfaces..")
                    .allowEmptyShould(true);

    @Test
    void domainAndApplicationMustNotDependOnFrameworks() {
        DOMAIN_AND_APPLICATION_MUST_NOT_DEPEND_ON_FRAMEWORKS.check(classes);
    }

    @Test
    void interfacesMustNotDependOnInfrastructure() {
        INTERFACES_MUST_NOT_DEPEND_ON_INFRASTRUCTURE.check(classes);
    }

    @Test
    void infrastructureMustNotDependOnInterfaces() {
        INFRASTRUCTURE_MUST_NOT_DEPEND_ON_INTERFACES.check(classes);
    }
}
