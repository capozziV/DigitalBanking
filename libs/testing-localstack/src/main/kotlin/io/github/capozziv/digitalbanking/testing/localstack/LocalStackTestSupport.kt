package io.github.capozziv.digitalbanking.testing.localstack

import org.junit.jupiter.api.Assumptions.abort
import org.testcontainers.localstack.LocalStackContainer
import org.testcontainers.utility.DockerImageName
import org.testcontainers.utility.MountableFile
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.sns.SnsClient
import software.amazon.awssdk.services.sqs.SqsClient

/**
 * Single place for how integration tests run LocalStack: pinned image, auth token from the
 * environment, optional egress proxy, and the skip-locally / fail-on-CI rule for a missing token.
 */
object LocalStackTestSupport {
    /** Pinned release; the compose and the CI use the same tag. */
    const val IMAGE = "localstack/localstack:2026.10.2"
    const val AUTH_TOKEN_VARIABLE = "LOCALSTACK_AUTH_TOKEN"

    /** Proxy URL reachable from the container, for hosts where outbound HTTPS only works through a proxy. */
    const val HTTPS_PROXY_VARIABLE = "LOCALSTACK_HTTPS_PROXY"

    /** Host path of the CA that the proxy uses to re-terminate TLS. */
    const val CA_BUNDLE_VARIABLE = "LOCALSTACK_CA_BUNDLE"
    const val CONTAINER_CA_BUNDLE = "/etc/ssl/certs/egress-proxy-ca.crt"

    /**
     * Returns the token. Without it the test is aborted (reported as skipped) on a developer machine
     * and fails on CI, so a missing secret never turns into a silent green build.
     */
    fun authToken(environment: Map<String, String> = System.getenv()): String {
        val token = environment[AUTH_TOKEN_VARIABLE]
        if (!token.isNullOrBlank()) return token
        check(environment["CI"] != "true") { "$AUTH_TOKEN_VARIABLE is not set on CI; LocalStack tests cannot run" }
        abort<Unit>("$AUTH_TOKEN_VARIABLE is not set; skipping LocalStack tests (load infra/local/.env into the environment)")
        error("unreachable")
    }

    /** A container (not started) for [services], with the token and, when configured, the egress proxy. */
    fun container(
        services: List<String>,
        environment: Map<String, String> = System.getenv(),
    ): LocalStackContainer {
        val container =
            LocalStackContainer(DockerImageName.parse(IMAGE))
                .withServices(*services.toTypedArray())
                .withEnv(AUTH_TOKEN_VARIABLE, authToken(environment))
        environment[HTTPS_PROXY_VARIABLE]?.takeIf { it.isNotBlank() }?.let { proxy ->
            container.withEnv("HTTPS_PROXY", proxy).withEnv("NO_PROXY", "localhost,127.0.0.1")
        }
        environment[CA_BUNDLE_VARIABLE]?.takeIf { it.isNotBlank() }?.let { caBundle ->
            container
                .withCopyFileToContainer(MountableFile.forHostPath(caBundle), CONTAINER_CA_BUNDLE)
                .withEnv("REQUESTS_CA_BUNDLE", CONTAINER_CA_BUNDLE)
                .withEnv("SSL_CERT_FILE", CONTAINER_CA_BUNDLE)
        }
        return container
    }

    fun snsClient(container: LocalStackContainer): SnsClient =
        SnsClient
            .builder()
            .endpointOverride(container.endpoint)
            .region(Region.of(container.region))
            .credentialsProvider(credentials(container))
            .build()

    fun sqsClient(container: LocalStackContainer): SqsClient =
        SqsClient
            .builder()
            .endpointOverride(container.endpoint)
            .region(Region.of(container.region))
            .credentialsProvider(credentials(container))
            .build()

    private fun credentials(container: LocalStackContainer) =
        StaticCredentialsProvider.create(AwsBasicCredentials.create(container.accessKey, container.secretKey))
}
