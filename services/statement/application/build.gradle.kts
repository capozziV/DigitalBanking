plugins { id("digitalbanking.kotlin-library") }

dependencies {
    implementation(project(":services:statement:domain"))
    // Only the Caller type (plain Kotlin, no Spring): the use case checks the owner of the account.
    implementation(project(":libs:security-caller"))
}
