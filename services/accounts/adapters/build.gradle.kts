plugins { id("digitalbanking.spring-module") }

dependencies {
    implementation(project(":services:accounts:application"))
    implementation(project(":services:accounts:domain"))
}
