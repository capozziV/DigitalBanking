package io.github.capozziv.digitalbanking.accounts.bootstrap

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication(scanBasePackages = ["io.github.capozziv.digitalbanking.accounts"])
class AccountsApplication

fun main(args: Array<String>) {
    runApplication<AccountsApplication>(*args)
}
