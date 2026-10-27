package io.github.capozziv.digitalbanking.testing.architecture

import com.tngtech.archunit.base.DescribedPredicate
import com.tngtech.archunit.core.domain.JavaClass
import com.tngtech.archunit.core.domain.JavaModifier
import com.tngtech.archunit.lang.ArchRule
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses
import kotlin.reflect.KClass
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

    /**
     * The use case package holds use cases and plain data types (commands, results, outcomes, views), nothing else.
     * The name does not exempt a class: only its shape does (see [isDataType]).
     */
    fun useCasePackageHoldsOnlyUseCases(rootPackage: String): ArchRule =
        classes()
            .that()
            .resideInAPackage("$rootPackage.application.usecase..")
            .and()
            .areTopLevelClasses()
            .and(notKotlinInternal)
            .and(
                DescribedPredicate.describe<JavaClass>("are not data types") { javaClass -> !isDataType(javaClass) },
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

    private const val FILE_FACADE_KIND = 2
    private const val SYNTHETIC_CLASS_KIND = 3
    private const val REST_CONTROLLER = "org.springframework.web.bind.annotation.RestController"
    private const val CONTROLLER = "org.springframework.stereotype.Controller"
    private const val RESPONSE_BODY = "org.springframework.web.bind.annotation.ResponseBody"

    /** Methods a data type may declare besides property accessors: the ones a `data class` generates. */
    private val DATA_METHOD_NAMES = setOf("equals", "hashCode", "toString", "copy", "copy\$default")
    private val COMPONENT_METHOD = Regex("component\\d+")
    private val ACCESSOR_METHOD = Regex("(get[A-Z_].*|is[A-Z_].*)")

    /**
     * A data type holds data, not behavior: an interface, an enum, a sealed type, or a class whose public methods
     * are only property accessors and the ones a `data class` generates.
     */
    private fun isDataType(javaClass: JavaClass): Boolean =
        javaClass.isInterface ||
            javaClass.isEnum ||
            (kotlinClassOrNull(javaClass)?.isSealed == true) ||
            javaClass.methods
                .filter { it.modifiers.contains(JavaModifier.PUBLIC) && !it.modifiers.contains(JavaModifier.SYNTHETIC) }
                .all { method ->
                    // Members touching a value class get a mangled name (`getAmount-3f9a`, `copy-3f9a`).
                    val name = method.name.substringBefore('-')
                    name in DATA_METHOD_NAMES ||
                        COMPONENT_METHOD.matches(name) ||
                        (ACCESSOR_METHOD.matches(name) && method.rawParameterTypes.isEmpty())
                }

    /** Null for what kotlin-reflect cannot inspect: file facades (top-level functions) and synthetic classes. */
    private fun kotlinClassOrNull(javaClass: JavaClass): KClass<*>? =
        runCatching {
            val reflected = javaClass.reflect()
            // A file facade (`FooKt`) has Metadata kind 2 and, on some versions, answers `visibility` without failing.
            val kind = reflected.getAnnotation(Metadata::class.java)?.kind
            if (kind == FILE_FACADE_KIND || kind == SYNTHETIC_CLASS_KIND) null else reflected.kotlin.also { it.visibility }
        }.getOrNull()

    /**
     * Kotlin `internal` compiles to a public JVM class; only the Kotlin metadata knows the visibility.
     * Classes kotlin-reflect cannot inspect (file facades such as `FooKt`, with top-level functions) are no
     * class a person names or injects, so the rules skip them instead of blowing up.
     */
    private val notKotlinInternal =
        DescribedPredicate.describe<JavaClass>("are not Kotlin internal nor file facades") { javaClass ->
            kotlinClassOrNull(javaClass)?.let { it.visibility != KVisibility.INTERNAL } ?: false
        }
}
