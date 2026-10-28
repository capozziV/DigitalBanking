plugins { id("digitalbanking.spring-module") }

dependencies {
    implementation(project(":services:transfers:domain"))
    implementation(project(":services:transfers:application"))
    implementation(project(":libs:outbox"))

    implementation("org.springframework.boot:spring-boot-starter-webmvc")
    implementation("org.springframework.boot:spring-boot-starter-jdbc")
    implementation("org.springframework.boot:spring-boot-starter-jackson")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation(libs.springdoc.openapi.starter.webmvc.ui)
    implementation("tools.jackson.module:jackson-module-kotlin")
    implementation("org.springframework.boot:spring-boot-starter-flyway")
    implementation(libs.flyway.postgresql)
    implementation(libs.java.uuid.generator)
    runtimeOnly(libs.postgresql)
}
