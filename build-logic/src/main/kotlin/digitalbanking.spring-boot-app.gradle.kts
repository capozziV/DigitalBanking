plugins {
    id("digitalbanking.spring-module")
    id("digitalbanking.integration-test")
    id("org.springframework.boot")
}

// Executable jar named after the service (services/accounts/bootstrap -> accounts.jar), so build/libs, CI artifacts
// and `java -jar` say which service they run. The e2e finds the jars through the bootJar task, not by name.
tasks.named<org.springframework.boot.gradle.tasks.bundling.BootJar>("bootJar") {
    val service = requireNotNull(project.parent) { "spring-boot-app must be a services/<service>/bootstrap module" }.name
    archiveFileName.set("$service.jar")
}
