package io.github.capozziv.digitalbanking.testing.architecture

import com.tngtech.archunit.core.domain.JavaClasses
import com.tngtech.archunit.core.importer.ClassFileImporter
import com.tngtech.archunit.lang.ArchRule
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatCode
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test

/**
 * Each rule is checked against a sample service that respects it ([GOOD_ROOT]) and against one that violates it
 * ([BAD_ROOT]); a violation must name the offending sample class, so a rule cannot fail for the wrong reason.
 */
class ServiceArchitectureRulesTest {
    private val good = ClassFileImporter().importPackages(GOOD_ROOT)
    private val bad = ClassFileImporter().importPackages(BAD_ROOT)

    private fun assertPasses(
        rule: (String) -> ArchRule,
        classes: JavaClasses = good,
    ) {
        assertThatCode { rule(GOOD_ROOT).check(classes) }.doesNotThrowAnyException()
    }

    private fun assertViolatedBy(
        rule: (String) -> ArchRule,
        vararg offenders: String,
    ) {
        val failure = assertThatThrownBy { rule(BAD_ROOT).check(bad) }.isInstanceOf(AssertionError::class.java)
        offenders.forEach { failure.hasMessageContaining(it) }
    }

    @Test
    fun `domain rule passes for a clean domain`() = assertPasses(ServiceArchitectureRules::domainIsIndependent)

    @Test
    fun `domain rule rejects spring, sql, aws and the outer layers`() =
        assertViolatedBy(
            ServiceArchitectureRules::domainIsIndependent,
            "DomainWithSpring",
            "DomainWithSql",
            "DomainWithAws",
            "DomainUsesApplication",
            "DomainUsesAdapters",
        )

    @Test
    fun `application rule passes for a clean application`() = assertPasses(ServiceArchitectureRules::applicationIsIndependent)

    @Test
    fun `application rule rejects spring, sql, aws and adapters`() =
        assertViolatedBy(
            ServiceArchitectureRules::applicationIsIndependent,
            "ApplicationWithSpring",
            "ApplicationWithSql",
            "ApplicationWithAws",
            "ApplicationUsesAdapters",
        )

    @Test
    fun `web rule passes when web does not touch persistence`() = assertPasses(ServiceArchitectureRules::webDoesNotUsePersistence)

    @Test
    fun `web rule rejects a web class using persistence`() =
        assertViolatedBy(ServiceArchitectureRules::webDoesNotUsePersistence, "WebUsesPersistence")

    @Test
    fun `persistence rule passes when persistence does not touch web`() = assertPasses(ServiceArchitectureRules::persistenceDoesNotUseWeb)

    @Test
    fun `persistence rule rejects a persistence class using web`() =
        assertViolatedBy(ServiceArchitectureRules::persistenceDoesNotUseWeb, "PersistenceUsesWeb")

    @Test
    fun `use case package rule passes for use cases, commands and results`() =
        assertPasses(ServiceArchitectureRules::useCasePackageHoldsOnlyUseCases)

    @Test
    fun `use case package rule rejects a class without a role suffix`() =
        assertViolatedBy(ServiceArchitectureRules::useCasePackageHoldsOnlyUseCases, "PlaceOrderHandler")

    @Test
    fun `execute rule passes for a use case named UseCase`() = assertPasses(ServiceArchitectureRules::executeClassesAreUseCases)

    @Test
    fun `execute rule rejects a public class with execute outside the use case package`() =
        assertViolatedBy(ServiceArchitectureRules::executeClassesAreUseCases, "OrderRunner")

    @Test
    fun `controller rule passes for RestController and Controller with ResponseBody ending in Controller`() =
        assertPasses(ServiceArchitectureRules::controllersEndWithController)

    @Test
    fun `controller rule rejects both stereotypes without the suffix`() =
        assertViolatedBy(ServiceArchitectureRules::controllersEndWithController, "OrderResource", "LegacyResource")

    @Test
    fun `DTO rule passes for DTO classes with a companion`() = assertPasses(ServiceArchitectureRules::webBodiesEndWithDto)

    @Test
    fun `DTO rule rejects a body without the suffix`() = assertViolatedBy(ServiceArchitectureRules::webBodiesEndWithDto, "OrderBody")

    @Test
    fun `all returns the eight rules and they pass or fail together`() {
        assertThat(ServiceArchitectureRules.all(GOOD_ROOT)).hasSize(8)
        ServiceArchitectureRules.all(GOOD_ROOT).forEach { it.check(good) }
        ServiceArchitectureRules.all(BAD_ROOT).forEach { rule ->
            assertThatThrownBy { rule.check(bad) }.isInstanceOf(AssertionError::class.java)
        }
    }

    @Test
    fun `a rule that matches no class fails instead of passing silently`() {
        val renamed = ClassFileImporter().importPackages("$GOOD_ROOT.domain")
        assertThatThrownBy { ServiceArchitectureRules.webDoesNotUsePersistence(GOOD_ROOT).check(renamed) }
            .isInstanceOf(AssertionError::class.java)
    }

    private companion object {
        const val SAMPLES = "io.github.capozziv.digitalbanking.testing.architecture.samples"
        const val GOOD_ROOT = "$SAMPLES.good"
        const val BAD_ROOT = "$SAMPLES.bad"
    }
}
