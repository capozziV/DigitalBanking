package io.github.capozziv.digitalbanking.notification.bootstrap

import com.tngtech.archunit.core.importer.ClassFileImporter
import com.tngtech.archunit.core.importer.ImportOption
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses
import io.github.capozziv.digitalbanking.testing.architecture.ServiceArchitectureRules
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory

/**
 * Architecture rules of the notification service (shared with the other services through `libs/testing-architecture`)
 * and the rule that only makes sense here: notification reads the event JSON and never depends on the classes of the
 * services that publish it.
 */
class ArchitectureTest {
    private val classes =
        ClassFileImporter()
            .withImportOption(ImportOption.DoNotIncludeTests())
            .importPackages(NOTIFICATION_PACKAGE)

    @TestFactory
    fun `notification respects the shared service rules`(): List<DynamicTest> =
        ServiceArchitectureRules.all(NOTIFICATION_PACKAGE).map { rule ->
            // TODO(etapa 3, Task A6): drop allowEmptyShould once domain, application, persistence and web have classes;
            // the shared rules forbid an empty `should` so that a renamed package fails instead of passing silently.
            DynamicTest.dynamicTest(rule.description) { rule.allowEmptyShould(true).check(classes) }
        }

    /** Dependencies are checked by package name, so the publishers' classes need not be imported. */
    @Test
    fun `notification does not depend on the accounts or transfers services`() {
        noClasses()
            .that()
            .resideInAPackage("$NOTIFICATION_PACKAGE..")
            .should()
            .dependOnClassesThat()
            .resideInAnyPackage("$ACCOUNTS_PACKAGE..", "$TRANSFERS_PACKAGE..")
            .check(classes)
    }

    private companion object {
        const val NOTIFICATION_PACKAGE = "io.github.capozziv.digitalbanking.notification"
        const val ACCOUNTS_PACKAGE = "io.github.capozziv.digitalbanking.accounts"
        const val TRANSFERS_PACKAGE = "io.github.capozziv.digitalbanking.transfers"
    }
}
