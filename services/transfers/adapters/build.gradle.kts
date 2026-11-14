plugins { id("digitalbanking.spring-module") }

dependencies {
    implementation(project(":services:transfers:domain"))
    implementation(project(":services:transfers:application"))
    implementation(project(":libs:outbox"))
    // JWT resource server, Caller, route rules and the machine token; correlation id filter and propagation.
    implementation(project(":libs:security"))
    implementation(project(":libs:observabilidade"))

    implementation("org.springframework.boot:spring-boot-starter-webmvc")
    implementation("org.springframework.boot:spring-boot-starter-jdbc")
    implementation("org.springframework.boot:spring-boot-starter-jackson")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation(libs.springdoc.openapi.starter.webmvc.ui)
    implementation("tools.jackson.module:jackson-module-kotlin")
    implementation("org.springframework.boot:spring-boot-starter-flyway")
    implementation(libs.flyway.postgresql)
    implementation(libs.java.uuid.generator)
    // Daily limit counter (Lua scripts) and its metrics.
    implementation("org.springframework.boot:spring-boot-starter-data-redis")
    implementation("io.micrometer:micrometer-core")
    runtimeOnly(libs.postgresql)
}
