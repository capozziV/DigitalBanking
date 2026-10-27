package io.github.capozziv.digitalbanking.testing.architecture.samples.good.adapters.web

import io.github.capozziv.digitalbanking.testing.architecture.samples.good.application.usecase.PlaceOrderCommand
import io.github.capozziv.digitalbanking.testing.architecture.samples.good.application.usecase.PlaceOrderUseCase
import org.springframework.web.bind.annotation.RestController

@RestController
class OrderController(
    private val useCase: PlaceOrderUseCase,
) {
    fun place(id: String): String = useCase.execute(PlaceOrderCommand(id)).id
}
