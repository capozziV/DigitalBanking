import org.gradle.api.artifacts.VersionCatalogsExtension
import sh.stubborn.contract.verifier.config.TestFramework
import sh.stubborn.contract.verifier.config.TestMode

/*
 * Producer side of the consumer-driven contracts (Stubborn Contract, ADR 0022), for a services/<service>/bootstrap
 * module.
 *
 * Layout (one folder per kind of contract under src/contractTest/resources/contracts):
 * - contracts/http/      HTTP contracts. The generated tests extend <package>.HttpContractBase.
 * - contracts/messaging/ message contracts. The generated tests extend <package>.MessagingContractBase, a Spring test
 *                        context with the service's outbox mapper and OutboxMessagingContractConfiguration of
 *                        libs/testing-outbox-contract (the `sentTo` of a contract is the SNS topic name).
 * <package> is io.github.capozziv.digitalbanking.<service>.bootstrap.contract, where the base classes live (in
 * src/contractTest/kotlin) and where the tests are generated (class <Folder>ContractTest, e.g. HttpContractTest).
 *
 * What it adds:
 * - the contractTest suite of the plugin (MockMvc, JUnit 5), run by `check`, so `build` fails when the code no longer
 *   honors a contract. The HTTP base class is a @WebMvcTest of the controllers under contract, with the security and
 *   correlation id auto-configurations of libs and the signed tokens of libs/testing-security; it needs no Docker;
 * - the stubs jar (task verifierStubsJar, classifier `stubs`) with the WireMock mappings under
 *   META-INF/io.github.capozziv.digitalbanking.<service>/bootstrap/, exposed to the other projects of this build
 *   through the consumable configuration contractStubsElements. Consumers apply digitalbanking.contract-consumer and
 *   declare `contractStubs(project(":services:<service>:bootstrap"))`; nothing goes to a remote repository.
 *
 * The group of the project is set to io.github.capozziv.digitalbanking.<service>: every bootstrap module is named
 * `bootstrap`, so the group is what tells the stubs of one service from another's.
 */
plugins {
    id("sh.stubborn.contract")
}

val libs = extensions.getByType<VersionCatalogsExtension>().named("libs")
val service = requireNotNull(project.parent) { "contract-producer must be a services/<service>/bootstrap module" }.name
val contractPackage = "io.github.capozziv.digitalbanking.$service.bootstrap.contract"

group = "io.github.capozziv.digitalbanking.$service"

contracts {
    testFramework.set(TestFramework.JUNIT5)
    testMode.set(TestMode.MOCKMVC)
    basePackageForTests.set(contractPackage)
    nameSuffixForTests.set("ContractTest")
    baseClassMappings {
        baseClassMapping("(.*\\.)?http(\\..*)?", "$contractPackage.HttpContractBase")
        baseClassMapping("(.*\\.)?messaging(\\..*)?", "$contractPackage.MessagingContractBase")
    }
}

dependencies {
    "contractTestImplementation"(platform(libs.findLibrary("stubborn-contract-bom").get()))
    "contractTestImplementation"("sh.stubborn:stubborn-contract-starter-verifier")
    // HTTP contracts run against the @WebMvcTest slice of the service, with the security and correlation id of libs.
    "contractTestImplementation"("org.springframework.boot:spring-boot-starter-webmvc-test")
    // Brings the security auto-configuration into the @WebMvcTest slice.
    "contractTestImplementation"("org.springframework.boot:spring-boot-starter-security-test")
    "contractTestImplementation"(project(":libs:security"))
    "contractTestImplementation"(project(":libs:observabilidade"))
    "contractTestImplementation"(project(":libs:testing-security"))
    // Message contracts: the event goes through the service's outbox mapper into a real outbox table (PostgreSQL in a
    // container, so they need Docker) and is read back as the envelope the relay publishes. The JSON mapper is the one
    // Spring Boot configures from the service's application.yml, as in production.
    "contractTestImplementation"(project(":libs:testing-outbox-contract"))
    "contractTestImplementation"("org.springframework.boot:spring-boot-starter-jackson")
}

// The Usage value is the one digitalbanking.contract-consumer asks for.
configurations.consumable("contractStubsElements") {
    attributes {
        attribute(Usage.USAGE_ATTRIBUTE, objects.named(Usage::class.java, "digitalbanking-contract-stubs"))
    }
    outgoing.artifact(tasks.named("verifierStubsJar"))
}


// ktlint lints every source dir of contractTest, the generated tests included (Java, so nothing to lint there), and
// Gradle refuses to run it before the generator without a declared dependency.
tasks
    .matching { it.name == "runKtlintCheckOverContractTestSourceSet" || it.name == "runKtlintFormatOverContractTestSourceSet" }
    .configureEach { dependsOn("generateContractTests") }
