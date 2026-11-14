plugins { id("digitalbanking.spring-module") }

dependencies {
    implementation(project(":services:customer:application"))
    implementation(project(":services:customer:domain"))
    implementation(project(":libs:outbox"))
    implementation(project(":libs:security"))
    implementation(project(":libs:observabilidade"))

    implementation("org.springframework.boot:spring-boot-starter-webmvc")
    implementation("org.springframework.boot:spring-boot-starter-jdbc")
    implementation("org.springframework.boot:spring-boot-starter-jackson")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("tools.jackson.module:jackson-module-kotlin")
    implementation("org.springframework.boot:spring-boot-starter-flyway")
    implementation(libs.flyway.postgresql)
    implementation(libs.springdoc.openapi.starter.webmvc.ui)
    runtimeOnly(libs.postgresql)
}
