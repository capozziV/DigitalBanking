package io.github.capozziv.digitalbanking.statement.bootstrap

import com.tngtech.archunit.core.importer.ClassFileImporter
import com.tngtech.archunit.core.importer.ImportOption
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses
import io.github.capozziv.digitalbanking.testing.architecture.ServiceArchitectureRules
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory

/**
 * Architecture rules of the statement service (shared with the other services through `libs/testing-architecture`)
 * and the rule that only makes sense here: statement reads the event JSON and never depends on the classes of the
 * other services.
 */
class ArchitectureTest {
    private val classes =
        ClassFileImporter()
            .withImportOption(ImportOption.DoNotIncludeTests())
            .importPackages(STATEMENT_PACKAGE)

    /**
     * The web adapter (controller and DTOs) arrives in Task 6 of stage 4. Until then these rules match no classes,
     * and a rule over no classes fails by design. Task 6 empties this set.
     */
    private val rulesWaitingForTheWebAdapter =
        setOf(
            ServiceArchitectureRules.webDoesNotUsePersistence(STATEMENT_PACKAGE).description,
            ServiceArchitectureRules.controllersEndWithController(STATEMENT_PACKAGE).description,
            ServiceArchitectureRules.webBodiesEndWithDto(STATEMENT_PACKAGE).description,
        )

    @TestFactory
    fun `statement respects the shared service rules`(): List<DynamicTest> =
        ServiceArchitectureRules.all(STATEMENT_PACKAGE).filterNot { it.description in rulesWaitingForTheWebAdapter }.map { rule ->
            DynamicTest.dynamicTest(rule.description) { rule.check(classes) }
        }

    /** Dependencies are checked by package name, so the other services' classes need not be imported. */
    @Test
    fun `statement does not depend on the other services`() {
        noClasses()
            .that()
            .resideInAPackage("$STATEMENT_PACKAGE..")
            .should()
            .dependOnClassesThat()
            .resideInAnyPackage("$ACCOUNTS_PACKAGE..", "$TRANSFERS_PACKAGE..", "$NOTIFICATION_PACKAGE..")
            .check(classes)
    }

    private companion object {
        const val STATEMENT_PACKAGE = "io.github.capozziv.digitalbanking.statement"
        const val ACCOUNTS_PACKAGE = "io.github.capozziv.digitalbanking.accounts"
        const val TRANSFERS_PACKAGE = "io.github.capozziv.digitalbanking.transfers"
        const val NOTIFICATION_PACKAGE = "io.github.capozziv.digitalbanking.notification"
    }
}
