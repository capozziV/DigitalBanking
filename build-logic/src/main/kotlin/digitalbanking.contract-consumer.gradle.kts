import org.gradle.api.artifacts.VersionCatalogsExtension

/*
 * Consumer side of the consumer-driven contracts (Stubborn Contract, ADR 0022).
 *
 * Declare the producers whose stubs the tests run against:
 *
 *     dependencies { contractStubs(project(":services:accounts:bootstrap")) }
 *
 * The stubs jar of each producer (see digitalbanking.contract-producer) is built from its contracts in this same build
 * and put on the runtime classpath of the test suites (test, and integrationTest when the module has it, which extends
 * test). The tests start the stubs with the Stub Runner JUnit extension in classpath mode, with no remote repository:
 *
 *     @RegisterExtension
 *     val stubs = StubRunnerExtension()
 *         .stubsMode(StubsMode.CLASSPATH)
 *         .downloadStub("io.github.capozziv.digitalbanking.accounts", "bootstrap")
 *
 * and read the port of the stub server from `stubs.findStubUrl(...)`. A contract changed by the producer changes the
 * stubs, so a consumer that no longer matches fails its own build.
 */
val libs = extensions.getByType<VersionCatalogsExtension>().named("libs")

val contractStubs =
    configurations.dependencyScope("contractStubs") {
        description = "Producers whose contract stubs the tests run against (project dependencies)."
    }

val contractStubsClasspath =
    configurations.resolvable("contractStubsClasspath") {
        extendsFrom(contractStubs.get())
        // The Usage value is the one digitalbanking.contract-producer exposes.
        attributes {
            attribute(Usage.USAGE_ATTRIBUTE, objects.named(Usage::class.java, "digitalbanking-contract-stubs"))
        }
    }

dependencies {
    "testImplementation"(platform(libs.findLibrary("stubborn-contract-bom").get()))
    "testImplementation"("sh.stubborn:stubborn-contract-stub-runner")
    "testRuntimeOnly"(files(contractStubsClasspath))
}
