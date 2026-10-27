plugins { id("digitalbanking.kotlin-library") }

dependencies {
    implementation(project(":services:transfers:domain"))
}
