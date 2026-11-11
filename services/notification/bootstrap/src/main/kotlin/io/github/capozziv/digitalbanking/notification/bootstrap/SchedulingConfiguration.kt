package io.github.capozziv.digitalbanking.notification.bootstrap

import org.springframework.context.annotation.Configuration
import org.springframework.scheduling.annotation.EnableScheduling

/** Turns on `@Scheduled` for the service (today the DLQ depth refresh). */
@Configuration(proxyBeanMethods = false)
@EnableScheduling
class SchedulingConfiguration
