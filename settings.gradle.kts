pluginManagement {
    includeBuild("build-logic")
}

dependencyResolutionManagement {
    repositoriesMode = RepositoriesMode.FAIL_ON_PROJECT_REPOS
    repositories {
        mavenCentral()
    }
}

rootProject.name = "digitalbanking"

include(
    ":libs:observabilidade",
    ":libs:outbox",
    ":libs:outbox-relay",
    ":libs:security",
    ":libs:security-caller",
    ":libs:testing-architecture",
    ":libs:testing-localstack",
    ":libs:testing-redis",
    ":libs:testing-security",
    ":services:accounts:domain",
    ":services:accounts:application",
    ":services:accounts:adapters",
    ":services:accounts:bootstrap",
    ":services:customer:domain",
    ":services:customer:application",
    ":services:customer:adapters",
    ":services:customer:bootstrap",
    ":services:notification:domain",
    ":services:notification:application",
    ":services:notification:adapters",
    ":services:notification:bootstrap",
    ":services:statement:domain",
    ":services:statement:application",
    ":services:statement:adapters",
    ":services:statement:bootstrap",
    ":services:transfers:domain",
    ":services:transfers:application",
    ":services:transfers:adapters",
    ":services:transfers:bootstrap",
    ":tests:e2e",
)
