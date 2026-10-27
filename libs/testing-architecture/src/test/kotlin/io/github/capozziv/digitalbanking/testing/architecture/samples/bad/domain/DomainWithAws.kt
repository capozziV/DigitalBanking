package io.github.capozziv.digitalbanking.testing.architecture.samples.bad.domain

import software.amazon.awssdk.services.sns.SnsClient

class DomainWithAws(
    val client: SnsClient,
)
