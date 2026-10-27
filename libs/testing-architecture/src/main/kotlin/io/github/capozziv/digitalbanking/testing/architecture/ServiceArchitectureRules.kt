package io.github.capozziv.digitalbanking.testing.architecture

import com.tngtech.archunit.base.DescribedPredicate
import com.tngtech.archunit.core.domain.JavaClass
import com.tngtech.archunit.core.domain.JavaModifier
import com.tngtech.archunit.lang.ArchRule
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses
import kotlin.reflect.KVisibility

/**
 * Architecture rules every service must satisfy, parametrized by the service root package
 * (for example `io.github.capozziv.digitalbanking.transfers`). Each service checks them with
 * `ServiceArchitectureRules.all(ROOT).forEach { it.check(classes) }`.
 *
 * No rule allows an empty `should`: each one must match classes, so a renamed package makes the rule fail
 * instead of passing silently.
 */
object ServiceArchitectureRules {
    /** Suffixes of the types a use case package may hold besides the use cases themselves. */
    val USE_CASE_SUPPORT_SUFFIXES = listOf("Command", "Result", "Outcome", "View")

    /** Infrastructure that neither the domain nor the application layer may use. */
    private val INFRASTRUCTURE_PACKAGES = arrayOf("org.springframework..", "java.sql..", "software.amazon..")

    fun all(rootPackage: String): List<ArchRule> =
        listOf(
            domainIsIndependent(rootPackage),
            applicationIsIndependent(rootPackage),
            webDoesNotUsePersistence(rootPackage),
            persistenceDoesNotUseWeb(rootPackage),
            useCasePackageHoldsOnlyUseCases(rootPackage),
            executeClassesAreUseCases(rootPackage),
            controllersEndWithController(rootPackage),
            webBodiesEndWithDto(rootPackage),
        )

    fun domainIsIndependent(rootPackage: String): ArchRule =
        noClasses()
            .that()
            .resideInAPackage("$rootPackage.domain..")
            .should()
            .dependOnClassesThat()
            .resideInAnyPackage(*INFRASTRUCTURE_PACKAGES, "$rootPackage.application..", "$rootPackage.adapters..")

    fun applicationIsIndependent(rootPackage: String): ArchRule =
        noClasses()
            .that()
            .resideInAPackage("$rootPackage.application..")
            .should()
            .dependOnClassesThat()
            .resideInAnyPackage(*INFRASTRUCTURE_PACKAGES, "$rootPackage.adapters..")

    fun webDoesNotUsePersistence(rootPackage: String): ArchRule =
        noClasses()
            .that()
            .resideInAPackage("$rootPackage.adapters.web..")
            .should()
            .dependOnClassesThat()
            .resideInAPackage("$rootPackage.adapters.persistence..")

    fun persistenceDoesNotUseWeb(rootPackage: String): ArchRule =
        noClasses()
            .that()
            .resideInAPackage("$rootPackage.adapters.persistence..")
            .should()
            .dependOnClassesThat()
            .resideInAPackage("$rootPackage.adapters.web..")

    /** The use case package holds use cases and their commands, results, outcomes and views, nothing else. */
    fun useCasePackageHoldsOnlyUseCases(rootPackage: String): ArchRule =
        classes()
            .that()
            .resideInAPackage("$rootPackage.application.usecase..")
            .and()
            .areTopLevelClasses()
            .and(notKotlinInternal)
            .and(
                DescribedPredicate.describe<JavaClass>("are not a ${USE_CASE_SUPPORT_SUFFIXES.joinToString("/")} type") { javaClass ->
                    USE_CASE_SUPPORT_SUFFIXES.none { javaClass.simpleName.endsWith(it) }
                },
            ).should()
            .haveSimpleNameEndingWith("UseCase")

    /** A public class with a public `execute` anywhere in the application layer is a use case and says so. */
    fun executeClassesAreUseCases(rootPackage: String): ArchRule =
        classes()
            .that()
            .resideInAPackage("$rootPackage.application..")
            .and()
            .areTopLevelClasses()
            .and()
            .arePublic()
            .and(notKotlinInternal)
            .and(
                DescribedPredicate.describe<JavaClass>("declare a public method named execute") { javaClass ->
                    javaClass.methods.any { it.name == "execute" && it.modifiers.contains(JavaModifier.PUBLIC) }
                },
            ).should()
            .haveSimpleNameEndingWith("UseCase")

    /** `@Controller` + `@ResponseBody` is a `@RestController` spelled out, so it gets the same suffix. */
    fun controllersEndWithController(rootPackage: String): ArchRule =
        classes()
            .that()
            .resideInAPackage("$rootPackage..")
            .and(
                DescribedPredicate.describe<JavaClass>("are REST controllers") { javaClass ->
                    javaClass.isAnnotatedWith(REST_CONTROLLER) ||
                        (javaClass.isAnnotatedWith(CONTROLLER) && javaClass.isAnnotatedWith(RESPONSE_BODY))
                },
            ).should()
            .haveSimpleNameEndingWith("Controller")

    fun webBodiesEndWithDto(rootPackage: String): ArchRule =
        // Top-level only: Kotlin companion objects compile to nested `...$Companion` classes.
        classes()
            .that()
            .resideInAPackage("$rootPackage.adapters.web.dto..")
            .and()
            .areTopLevelClasses()
            .should()
            .haveSimpleNameEndingWith("DTO")

    private const val REST_CONTROLLER = "org.springframework.web.bind.annotation.RestController"
    private const val CONTROLLER = "org.springframework.stereotype.Controller"
    private const val RESPONSE_BODY = "org.springframework.web.bind.annotation.ResponseBody"

    /** Kotlin `internal` compiles to a public JVM class; only the Kotlin metadata knows the visibility. */
    private val notKotlinInternal =
        DescribedPredicate.describe<JavaClass>("are not Kotlin internal") { javaClass ->
            javaClass.reflect().kotlin.visibility != KVisibility.INTERNAL
        }
}
