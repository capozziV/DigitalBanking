package io.github.capozziv.digitalbanking.statement.bootstrap

import org.springframework.context.annotation.Configuration
import org.springframework.scheduling.annotation.EnableScheduling

/** Turns on `@Scheduled` for the service (the DLQ depth refresh). */
@Configuration(proxyBeanMethods = false)
@EnableScheduling
class SchedulingConfiguration
