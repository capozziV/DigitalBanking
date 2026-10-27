package io.github.capozziv.digitalbanking.testing.architecture.samples.bad.application

import software.amazon.awssdk.services.sns.SnsClient

class ApplicationWithAws(
    val client: SnsClient,
)
