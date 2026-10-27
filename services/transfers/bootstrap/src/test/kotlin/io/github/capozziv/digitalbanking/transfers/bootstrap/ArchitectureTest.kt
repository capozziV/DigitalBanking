package io.github.capozziv.digitalbanking.transfers.bootstrap

import com.tngtech.archunit.core.importer.ClassFileImporter
import com.tngtech.archunit.core.importer.ImportOption
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses
import io.github.capozziv.digitalbanking.testing.architecture.ServiceArchitectureRules
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory

/**
 * Architecture rules of the transfers service (shared with the other services through `libs/testing-architecture`)
 * and the one rule that only makes sense here: transfers never depends on the accounts service's classes.
 */
class ArchitectureTest {
    private val classes =
        ClassFileImporter()
            .withImportOption(ImportOption.DoNotIncludeTests())
            .importPackages(TRANSFERS_PACKAGE, ACCOUNTS_PACKAGE)

    // No shared rule accepts an empty package, so only the rules whose packages already hold classes run here. The
    // rest join as the task that creates each package lands; the last one replaces this list by
    // ServiceArchitectureRules.all(TRANSFERS_PACKAGE):
    //  - application rules (independence, use case suffix, execute) -> when `application` holds use cases;
    //  - web / persistence separation, controller and DTO suffix -> when `adapters.web` and `adapters.persistence` exist.
    @TestFactory
    fun `transfers respects the shared service rules`(): List<DynamicTest> =
        listOf(ServiceArchitectureRules.domainIsIndependent(TRANSFERS_PACKAGE)).map { rule ->
            DynamicTest.dynamicTest(rule.description) { rule.check(classes) }
        }

    @Test
    fun `transfers does not depend on the accounts service`() {
        noClasses()
            .that()
            .resideInAPackage("$TRANSFERS_PACKAGE..")
            .should()
            .dependOnClassesThat()
            .resideInAPackage("$ACCOUNTS_PACKAGE..")
            .check(classes)
    }

    private companion object {
        const val TRANSFERS_PACKAGE = "io.github.capozziv.digitalbanking.transfers"
        const val ACCOUNTS_PACKAGE = "io.github.capozziv.digitalbanking.accounts"
    }
}
