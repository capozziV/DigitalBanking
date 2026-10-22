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
    ":services:accounts:domain",
    ":services:accounts:application",
    ":services:accounts:adapters",
    ":services:accounts:bootstrap",
)
