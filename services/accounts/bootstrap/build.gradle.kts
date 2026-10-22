plugins { id("digitalbanking.spring-boot-app") }

dependencies {
    implementation(project(":services:accounts:adapters"))
    implementation(project(":services:accounts:application"))
    implementation(project(":services:accounts:domain"))
    implementation("org.springframework.boot:spring-boot-starter-actuator")

    testImplementation(libs.archunit)
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.boot:spring-boot-starter-webmvc")
    testImplementation("org.springframework.boot:spring-boot-testcontainers")
    testImplementation(libs.testcontainers.postgresql)
    testImplementation(libs.testcontainers.junit.jupiter)
}
