import org.gradle.api.artifacts.VersionCatalogsExtension

plugins {
    id("digitalbanking.kotlin-library")
    kotlin("plugin.spring")
}

val libs = extensions.getByType<VersionCatalogsExtension>().named("libs")

dependencies {
    implementation(platform(libs.findLibrary("spring-boot-bom").get()))
    testImplementation(platform(libs.findLibrary("spring-boot-bom").get()))
    implementation("org.jetbrains.kotlin:kotlin-reflect")
}
