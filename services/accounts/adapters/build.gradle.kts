plugins { id("digitalbanking.spring-module") }

dependencies {
    implementation(project(":services:accounts:application"))
    implementation(project(":services:accounts:domain"))

    implementation("org.springframework.boot:spring-boot-starter-webmvc")
    implementation("org.springframework.boot:spring-boot-starter-jdbc")
    implementation("org.springframework.boot:spring-boot-starter-flyway")
    implementation(libs.flyway.postgresql)
    runtimeOnly(libs.postgresql)
}
