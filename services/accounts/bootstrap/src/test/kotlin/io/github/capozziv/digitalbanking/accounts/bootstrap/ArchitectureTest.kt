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
    fun `domain nao depende de spring nem de outras camadas`() {
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
    fun `application nao depende de spring nem de adapters`() {
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
    fun `adapters web e persistence sao independentes`() {
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
