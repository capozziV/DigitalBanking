package io.github.capozziv.digitalbanking.testing.architecture.samples.bad.domain

import io.github.capozziv.digitalbanking.testing.architecture.samples.bad.application.usecase.PlaceOrderHandler

class DomainUsesApplication(
    val handler: PlaceOrderHandler,
)
