package io.github.capozziv.digitalbanking.statement.bootstrap

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication(scanBasePackages = ["io.github.capozziv.digitalbanking.statement"])
class StatementApplication

fun main(args: Array<String>) {
    runApplication<StatementApplication>(*args)
}
