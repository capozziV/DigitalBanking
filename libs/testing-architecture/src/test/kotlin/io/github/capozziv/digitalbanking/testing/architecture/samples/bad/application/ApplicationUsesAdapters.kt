package io.github.capozziv.digitalbanking.testing.architecture.samples.bad.application

import io.github.capozziv.digitalbanking.testing.architecture.samples.bad.adapters.web.OrderResource

class ApplicationUsesAdapters(
    val resource: OrderResource,
)
