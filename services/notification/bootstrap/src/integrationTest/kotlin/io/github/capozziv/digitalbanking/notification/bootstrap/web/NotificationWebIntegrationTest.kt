package io.github.capozziv.digitalbanking.notification.bootstrap.web

import io.github.capozziv.digitalbanking.notification.bootstrap.NotificationIntegrationTest
import org.springframework.boot.test.context.SpringBootTest

/** Base of the tests that call the HTTP API: the same context as [NotificationIntegrationTest], on a random port. */
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = [
        "spring.cloud.aws.region.static=us-east-1",
        "spring.cloud.aws.credentials.access-key=test",
        "spring.cloud.aws.credentials.secret-key=test",
    ],
)
abstract class NotificationWebIntegrationTest : NotificationIntegrationTest()
