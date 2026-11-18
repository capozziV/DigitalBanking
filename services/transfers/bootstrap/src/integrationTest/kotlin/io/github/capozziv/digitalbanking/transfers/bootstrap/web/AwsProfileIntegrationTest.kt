package io.github.capozziv.digitalbanking.transfers.bootstrap.web

import io.github.capozziv.digitalbanking.testing.security.ApiDocsAssertions
import io.github.capozziv.digitalbanking.transfers.bootstrap.TransfersIntegrationTest
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.core.env.ConfigurableEnvironment
import org.springframework.test.context.ActiveProfiles

/** The `aws` profile (deploy of stage 8): no OpenAPI contract nor Swagger UI, and no AWS endpoint pointing to LocalStack. */
@ActiveProfiles("aws")
class AwsProfileIntegrationTest : TransfersIntegrationTest() {
    @LocalServerPort
    private var port: Int = 0

    @Autowired
    private lateinit var environment: ConfigurableEnvironment

    @Test
    fun `the documentation routes are 404 and application-aws yml uses the real AWS endpoints`() {
        ApiDocsAssertions.assertAwsProfile(port, environment)
    }
}
