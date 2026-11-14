plugins { id("digitalbanking.kotlin-library") }

dependencies {
    implementation(project(":services:accounts:domain"))
    // Caller only (plain Kotlin): the use cases decide ownership; Spring Security stays in adapters.
    api(project(":libs:security-caller"))
}
