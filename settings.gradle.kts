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
    ":libs:outbox",
    ":libs:outbox-relay",
    ":libs:testing-localstack",
    ":services:accounts:domain",
    ":services:accounts:application",
    ":services:accounts:adapters",
    ":services:accounts:bootstrap",
    ":services:transfers:domain",
)
