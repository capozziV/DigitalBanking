package io.github.capozziv.digitalbanking.transfers.bootstrap.web

import io.github.capozziv.digitalbanking.testing.security.AwsProfileConfigAssertions
import org.junit.jupiter.api.Test

/** With the `aws` profile alone (no test properties) the AWS endpoints of the service are blank, never LocalStack. */
class AwsProfileConfigIntegrationTest {
    @Test
    fun `the aws profile resolves the AWS endpoints to blank`() {
        AwsProfileConfigAssertions.assertEndpointsBlank("transfers.aws.endpoint")
    }
}
