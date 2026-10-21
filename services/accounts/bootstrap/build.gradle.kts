plugins { id("digitalbanking.spring-boot-app") }

dependencies {
    implementation(project(":services:accounts:adapters"))
    implementation(project(":services:accounts:application"))
    implementation(project(":services:accounts:domain"))

    testImplementation(libs.archunit)
}

// Temporario: sem classe main ainda, bootJar nao resolve mainClass. Remover quando existir a Application.
tasks.named("bootJar") { enabled = false }
