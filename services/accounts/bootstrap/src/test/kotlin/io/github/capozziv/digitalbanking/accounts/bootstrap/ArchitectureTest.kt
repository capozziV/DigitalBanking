package io.github.capozziv.digitalbanking.accounts.bootstrap

import com.tngtech.archunit.core.importer.ClassFileImporter
import com.tngtech.archunit.core.importer.ImportOption
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses
import org.junit.jupiter.api.Test

class ArchitectureTest {
    private val classes =
        ClassFileImporter()
            .withImportOption(ImportOption.DoNotIncludeTests())
            .importPackages("io.github.capozziv.digitalbanking.accounts")

    @Test
    fun `domain does not depend on spring or other layers`() {
        noClasses()
            .that()
            .resideInAPackage("..accounts.domain..")
            .should()
            .dependOnClassesThat()
            .resideInAnyPackage("org.springframework..", "..accounts.application..", "..accounts.adapters..")
            .allowEmptyShould(true)
            .check(classes)
    }

    @Test
    fun `application does not depend on spring or adapters`() {
        noClasses()
            .that()
            .resideInAPackage("..accounts.application..")
            .should()
            .dependOnClassesThat()
            .resideInAnyPackage("org.springframework..", "..accounts.adapters..")
            .allowEmptyShould(true)
            .check(classes)
    }

    @Test
    fun `web and persistence adapters are independent`() {
        noClasses()
            .that()
            .resideInAPackage("..adapters.web..")
            .should()
            .dependOnClassesThat()
            .resideInAPackage("..adapters.persistence..")
            .allowEmptyShould(true)
            .check(classes)
        noClasses()
            .that()
            .resideInAPackage("..adapters.persistence..")
            .should()
            .dependOnClassesThat()
            .resideInAPackage("..adapters.web..")
            .allowEmptyShould(true)
            .check(classes)
    }
}
