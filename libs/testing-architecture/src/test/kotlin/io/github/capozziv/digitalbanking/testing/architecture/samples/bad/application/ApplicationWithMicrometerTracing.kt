package io.github.capozziv.digitalbanking.testing.architecture.samples.bad.application

import io.micrometer.tracing.Tracer

class ApplicationWithMicrometerTracing(
    val tracer: Tracer,
)
