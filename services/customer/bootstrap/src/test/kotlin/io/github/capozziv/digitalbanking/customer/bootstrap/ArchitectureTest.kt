package io.github.capozziv.digitalbanking.customer.bootstrap

import com.tngtech.archunit.base.DescribedPredicate
import com.tngtech.archunit.core.domain.JavaClass
import com.tngtech.archunit.core.importer.ClassFileImporter
import com.tngtech.archunit.core.importer.ImportOption
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses
import io.github.capozziv.digitalbanking.testing.architecture.ServiceArchitectureRules
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory

/**
 * Architecture rules of the customer service (shared with the other services through `libs/testing-architecture`) and
 * the rules that only make sense here: the outbox libraries do not know the service, and neither the domain nor the
 * application layer knows the security library beyond the `Caller` type.
 */
class ArchitectureTest {
    private val classes =
        ClassFileImporter()
            .withImportOption(ImportOption.DoNotIncludeTests())
            .importPackages(CUSTOMER_PACKAGE, OUTBOX_PACKAGE, SECURITY_PACKAGE)

    @TestFactory
    fun `customer respects the shared service rules`(): List<DynamicTest> =
        ServiceArchitectureRules.all(CUSTOMER_PACKAGE).map { rule ->
            DynamicTest.dynamicTest(rule.description) { rule.check(classes) }
        }

    @Test
    fun `outbox library does not depend on the customer service`() {
        noClasses()
            .that()
            .resideInAPackage("$OUTBOX_PACKAGE..")
            .should()
            .dependOnClassesThat()
            .resideInAPackage("$CUSTOMER_PACKAGE..")
            .check(classes)
    }

    @Test
    fun `domain and application know only the Caller type of the security library`() {
        noClasses()
            .that()
            .resideInAPackage("$CUSTOMER_PACKAGE.domain..")
            .should()
            .dependOnClassesThat()
            .resideInAPackage("$SECURITY_PACKAGE..")
            .check(classes)
        noClasses()
            .that()
            .resideInAPackage("$CUSTOMER_PACKAGE.application..")
            .should()
            .dependOnClassesThat(
                JavaClass.Predicates.resideInAPackage("$SECURITY_PACKAGE..").and(
                    DescribedPredicate.describe("are not the Caller type") { !it.name.startsWith("$SECURITY_PACKAGE.Caller") },
                ),
            ).check(classes)
    }

    private companion object {
        const val CUSTOMER_PACKAGE = "io.github.capozziv.digitalbanking.customer"
        const val OUTBOX_PACKAGE = "io.github.capozziv.digitalbanking.outbox"
        const val SECURITY_PACKAGE = "io.github.capozziv.digitalbanking.security"
    }
}
