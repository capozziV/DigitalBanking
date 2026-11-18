package io.github.capozziv.digitalbanking.testing.security

import org.assertj.core.api.Assertions.assertThat
import org.springframework.boot.test.context.ConfigDataApplicationContextInitializer
import org.springframework.boot.test.context.runner.ApplicationContextRunner

/**
 * Loads only `application.yml` and `application-aws.yml` of the service (no test properties, no LocalStack) and checks
 * what the `aws` profile resolves to, so a deleted override in `application-aws.yml` turns the test red.
 */
object AwsProfileConfigAssertions {
    /** Every key in [endpointKeys] resolves blank with the `aws` profile: the SDK uses the real AWS endpoint of the region. */
    fun assertEndpointsBlank(vararg endpointKeys: String) {
        assertThat(endpointKeys).isNotEmpty()
        ApplicationContextRunner()
            .withInitializer(ConfigDataApplicationContextInitializer())
            .withPropertyValues("spring.profiles.active=aws")
            .run { context ->
                assertThat(context).hasNotFailed()
                assertThat(context.environment.activeProfiles).contains("aws")
                for (key in endpointKeys) {
                    assertThat(context.environment.containsProperty(key)).describedAs("$key is configured").isTrue()
                    assertThat(context.environment.getProperty(key)).describedAs(key).isBlank()
                }
            }
    }
}
