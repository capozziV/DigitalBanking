package io.github.capozziv.digitalbanking.notification.bootstrap.web

import io.github.capozziv.digitalbanking.notification.bootstrap.NotificationIntegrationTest
import org.springframework.boot.test.context.SpringBootTest

/**
 * Base of the tests that call the HTTP API: the same context as [NotificationIntegrationTest] (whose test
 * properties are inherited), on a random port.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
abstract class NotificationWebIntegrationTest : NotificationIntegrationTest()
