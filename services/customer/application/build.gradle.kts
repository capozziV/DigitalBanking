plugins { id("digitalbanking.kotlin-library") }

dependencies {
    implementation(project(":services:customer:domain"))
    implementation(project(":libs:security-caller"))
}
