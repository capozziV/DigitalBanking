plugins {
    id("digitalbanking.kotlin-library")
    `jvm-test-suite`
}

testing {
    suites {
        register<JvmTestSuite>("integrationTest") {
            useJUnitJupiter()
            dependencies {
                implementation(project())
            }
        }
    }
}

configurations["integrationTestImplementation"].extendsFrom(configurations["testImplementation"])
configurations["integrationTestRuntimeOnly"].extendsFrom(configurations["testRuntimeOnly"])

// LocalStack tests skip without the token and fail without it on CI, so token presence and CI mode
// must invalidate up-to-date checks and the build cache. Only presence is recorded, never the value.
tasks.named<Test>("integrationTest") {
    inputs.property(
        "localstackTokenPresent",
        providers.environmentVariable("LOCALSTACK_AUTH_TOKEN").map { it.isNotBlank() }.orElse(false),
    )
    inputs.property("ci", providers.environmentVariable("CI").map { it == "true" }.orElse(false))
}

tasks.check {
    dependsOn(testing.suites.named("integrationTest"))
}
