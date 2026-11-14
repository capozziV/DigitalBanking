plugins { id("digitalbanking.kotlin-library") }

dependencies {
    implementation(project(":services:transfers:domain"))
    // Caller only (no Spring): the use cases decide whether the caller owns the transfer or the account.
    implementation(project(":libs:security-caller"))
}
