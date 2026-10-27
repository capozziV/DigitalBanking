package io.github.capozziv.digitalbanking.transfers.bootstrap

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication(scanBasePackages = ["io.github.capozziv.digitalbanking.transfers"])
class TransfersApplication

fun main(args: Array<String>) {
    runApplication<TransfersApplication>(*args)
}
