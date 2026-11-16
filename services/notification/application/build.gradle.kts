plugins { id("digitalbanking.kotlin-library") }

dependencies {
    implementation(project(":services:notification:domain"))
    // Only the Caller type (plain Kotlin, no Spring).
    implementation(project(":libs:security-caller"))
}
