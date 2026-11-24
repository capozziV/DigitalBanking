package io.github.capozziv.digitalbanking.testing.architecture.samples.bad.domain

import io.opentelemetry.api.trace.Tracer

class DomainWithOpenTelemetry(
    val tracer: Tracer,
)
