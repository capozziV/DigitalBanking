package io.github.capozziv.digitalbanking.accounts.bootstrap

import com.tngtech.archunit.base.DescribedPredicate
import com.tngtech.archunit.core.domain.JavaClass
import com.tngtech.archunit.core.importer.ClassFileImporter
import com.tngtech.archunit.core.importer.ImportOption
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses
import org.junit.jupiter.api.Test
import kotlin.reflect.KVisibility

/**
 * Architecture rules of the accounts service and of the outbox libraries (outbox and outbox-relay). No rule allows an empty
 * `should`: each one must match classes, so a renamed package makes the rule fail instead of passing silently.
 */
class ArchitectureTest {
    private val classes =
        ClassFileImporter()
            .withImportOption(ImportOption.DoNotIncludeTests())
            .importPackages(ACCOUNTS_PACKAGE, OUTBOX_PACKAGE)

    @Test
    fun `domain does not depend on spring or other layers`() {
        noClasses()
            .that()
            .resideInAPackage("$ACCOUNTS_PACKAGE.domain..")
            .should()
            .dependOnClassesThat()
            .resideInAnyPackage("org.springframework..", "$ACCOUNTS_PACKAGE.application..", "$ACCOUNTS_PACKAGE.adapters..")
            .check(classes)
    }

    @Test
    fun `application does not depend on spring or adapters`() {
        noClasses()
            .that()
            .resideInAPackage("$ACCOUNTS_PACKAGE.application..")
            .should()
            .dependOnClassesThat()
            .resideInAnyPackage("org.springframework..", "$ACCOUNTS_PACKAGE.adapters..")
            .check(classes)
    }

    @Test
    fun `web adapter does not depend on persistence adapter`() {
        noClasses()
            .that()
            .resideInAPackage("$ACCOUNTS_PACKAGE.adapters.web..")
            .should()
            .dependOnClassesThat()
            .resideInAPackage("$ACCOUNTS_PACKAGE.adapters.persistence..")
            .check(classes)
    }

    @Test
    fun `persistence adapter does not depend on web adapter`() {
        noClasses()
            .that()
            .resideInAPackage("$ACCOUNTS_PACKAGE.adapters.persistence..")
            .should()
            .dependOnClassesThat()
            .resideInAPackage("$ACCOUNTS_PACKAGE.adapters.web..")
            .check(classes)
    }

    @Test
    fun `public classes of the use case package are use cases or their commands, results, outcomes and views`() {
        classes()
            .that()
            .resideInAPackage("$ACCOUNTS_PACKAGE.application.usecase..")
            .and()
            .areTopLevelClasses()
            .and()
            .haveSimpleNameNotEndingWith("Command")
            .and()
            .haveSimpleNameNotEndingWith("Result")
            .and()
            .haveSimpleNameNotEndingWith("Outcome")
            .and()
            .haveSimpleNameNotEndingWith("View")
            .and(notKotlinInternal)
            .should()
            .haveSimpleNameEndingWith("UseCase")
            .check(classes)
    }

    @Test
    fun `rest controllers end with Controller`() {
        classes()
            .that()
            .areAnnotatedWith("org.springframework.web.bind.annotation.RestController")
            .should()
            .haveSimpleNameEndingWith("Controller")
            .check(classes)
    }

    @Test
    fun `request and response bodies of the web adapter end with DTO`() {
        // Top-level only: Kotlin companion objects compile to nested `...$Companion` classes.
        classes()
            .that()
            .resideInAPackage("$ACCOUNTS_PACKAGE.adapters.web.dto..")
            .and()
            .areTopLevelClasses()
            .should()
            .haveSimpleNameEndingWith("DTO")
            .check(classes)
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

        /** Kotlin `internal` compiles to a public JVM class; only the Kotlin metadata knows the visibility. */
        val notKotlinInternal =
            DescribedPredicate.describe<JavaClass>("are not Kotlin internal") { javaClass ->
                javaClass.reflect().kotlin.visibility != KVisibility.INTERNAL
            }
    }
}
