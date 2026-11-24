package io.github.capozziv.digitalbanking.testing.architecture.samples.bad.application

import io.opentelemetry.api.trace.Tracer

class ApplicationWithOpenTelemetry(
    val tracer: Tracer,
)
