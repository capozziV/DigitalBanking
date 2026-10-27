plugins { id("digitalbanking.spring-boot-app") }

dependencies {
    implementation(project(":services:transfers:adapters"))
    implementation(project(":services:transfers:domain"))
    implementation("org.springframework.boot:spring-boot-starter-actuator")

    testImplementation(project(":libs:testing-architecture"))
    testImplementation("org.springframework.boot:spring-boot-starter-test")

    integrationTestImplementation("org.springframework.boot:spring-boot-starter-webmvc")
    integrationTestImplementation("org.springframework.boot:spring-boot-starter-jdbc")
    integrationTestImplementation(libs.testcontainers.postgresql)
    integrationTestImplementation(libs.testcontainers.junit.jupiter)
}
