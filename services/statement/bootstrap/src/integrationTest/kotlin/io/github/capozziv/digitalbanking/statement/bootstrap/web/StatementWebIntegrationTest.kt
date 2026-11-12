package io.github.capozziv.digitalbanking.statement.bootstrap.web

import io.github.capozziv.digitalbanking.statement.bootstrap.StatementIntegrationTest
import org.springframework.boot.test.context.SpringBootTest

/** Base of the tests that call the HTTP API: the same context as [StatementIntegrationTest], on a random port. */
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = [
        // These properties replace the base class's, so the queue listeners are stopped here too.
        "spring.cloud.aws.sqs.listener.auto-startup=false",
    ],
)
abstract class StatementWebIntegrationTest : StatementIntegrationTest()
