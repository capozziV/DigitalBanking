plugins { id("digitalbanking.spring-boot-app") }

dependencies {
    implementation(project(":services:transfers:adapters"))
    implementation(project(":services:transfers:domain"))
    implementation(project(":services:transfers:application"))
    implementation("org.springframework.boot:spring-boot-starter-actuator")

    testImplementation(project(":libs:testing-architecture"))
    testImplementation("org.springframework.boot:spring-boot-starter-test")

    integrationTestImplementation(testFixtures(project(":libs:outbox")))
    integrationTestImplementation("org.springframework.boot:spring-boot-starter-webmvc")
    integrationTestImplementation("org.springframework.boot:spring-boot-starter-jdbc")
    integrationTestImplementation(libs.testcontainers.postgresql)
    integrationTestImplementation(libs.testcontainers.junit.jupiter)
}
