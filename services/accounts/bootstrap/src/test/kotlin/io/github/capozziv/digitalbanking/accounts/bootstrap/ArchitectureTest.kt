package io.github.capozziv.digitalbanking.accounts.bootstrap

import com.tngtech.archunit.core.importer.ClassFileImporter
import com.tngtech.archunit.core.importer.ImportOption
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses
import io.github.capozziv.digitalbanking.testing.architecture.ServiceArchitectureRules
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory

/**
 * Architecture rules of the accounts service (shared with the other services through `libs/testing-architecture`) and
 * the one rule that only makes sense here: the outbox libraries (outbox and outbox-relay) do not know the service.
 */
class ArchitectureTest {
    private val classes =
        ClassFileImporter()
            .withImportOption(ImportOption.DoNotIncludeTests())
            .importPackages(ACCOUNTS_PACKAGE, OUTBOX_PACKAGE)

    @TestFactory
    fun `accounts respects the shared service rules`(): List<DynamicTest> =
        ServiceArchitectureRules.all(ACCOUNTS_PACKAGE).map { rule ->
            DynamicTest.dynamicTest(rule.description) { rule.check(classes) }
        }

    @Test
    fun `outbox library does not depend on the accounts service`() {
        noClasses()
            .that()
            .resideInAPackage("$OUTBOX_PACKAGE..")
            .should()
            .dependOnClassesThat()
            .resideInAPackage("$ACCOUNTS_PACKAGE..")
            .check(classes)
    }

    private companion object {
        const val ACCOUNTS_PACKAGE = "io.github.capozziv.digitalbanking.accounts"
        const val OUTBOX_PACKAGE = "io.github.capozziv.digitalbanking.outbox"
    }
}
