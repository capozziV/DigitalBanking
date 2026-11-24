package io.github.capozziv.digitalbanking.testing.architecture.samples.bad.domain

import io.micrometer.core.instrument.MeterRegistry

class DomainWithMicrometer(
    val registry: MeterRegistry,
)
