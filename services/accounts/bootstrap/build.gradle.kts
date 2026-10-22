plugins { id("digitalbanking.spring-boot-app") }

dependencies {
    implementation(project(":services:accounts:adapters"))
    implementation(project(":services:accounts:application"))
    implementation(project(":services:accounts:domain"))
    implementation("org.springframework.boot:spring-boot-starter-actuator")

    testImplementation(libs.archunit)
    testImplementation("org.springframework.boot:spring-boot-starter-test")

    integrationTestImplementation(project(":libs:outbox"))
    integrationTestImplementation("org.springframework.boot:spring-boot-starter-webmvc")
    integrationTestImplementation("org.springframework.boot:spring-boot-starter-jdbc")
    integrationTestImplementation("org.springframework.boot:spring-boot-testcontainers")
    integrationTestImplementation(libs.testcontainers.postgresql)
    integrationTestImplementation(libs.testcontainers.junit.jupiter)
}
