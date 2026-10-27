package io.github.capozziv.digitalbanking.testing.architecture.samples.good.application.usecase

import io.github.capozziv.digitalbanking.testing.architecture.samples.good.domain.Order

class PlaceOrderUseCase {
    fun execute(command: PlaceOrderCommand): PlaceOrderResult = PlaceOrderResult(Order(command.id).id)
}
