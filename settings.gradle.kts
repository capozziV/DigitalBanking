pluginManagement {
    repositories {
        mavenCentral()
        gradlePluginPortal()
    }
    includeBuild("build-logic")
}

dependencyResolutionManagement {
    repositories {
        mavenCentral()
    }
}

rootProject.name = "digitalbanking"

include(
    ":libs:outbox",
    ":libs:testing-localstack",
    ":services:accounts:domain",
    ":services:accounts:application",
    ":services:accounts:adapters",
    ":services:accounts:bootstrap",
)
