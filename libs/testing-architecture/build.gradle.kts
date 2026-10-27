// ArchUnit rules shared by the services, parametrized by the root package of each service. Test support:
// consume it only from testImplementation.
plugins {
    id("digitalbanking.kotlin-library")
}

dependencies {
    api(libs.archunit)
    // The rules read the Kotlin visibility (`internal` compiles to a public JVM class) from the class metadata.
    implementation(kotlin("reflect"))

    // Only the sample classes that violate the rules need these types on the classpath.
    testImplementation(platform(libs.spring.boot.bom))
    testImplementation(platform(libs.aws.sdk.bom))
    testImplementation("org.springframework:spring-context")
    testImplementation("org.springframework:spring-web")
    testImplementation(libs.aws.sdk.sns)
}
