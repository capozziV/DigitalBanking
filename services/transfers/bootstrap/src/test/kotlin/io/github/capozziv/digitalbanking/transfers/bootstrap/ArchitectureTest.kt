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
            .importPackages(TRANSFERS_PACKAGE)

    @TestFactory
    fun `transfers respects the shared service rules`(): List<DynamicTest> =
        ServiceArchitectureRules.all(TRANSFERS_PACKAGE).map { rule ->
            DynamicTest.dynamicTest(rule.description) { rule.check(classes) }
        }

    /** Dependencies are checked by package name, so the accounts classes need not be imported. */
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
