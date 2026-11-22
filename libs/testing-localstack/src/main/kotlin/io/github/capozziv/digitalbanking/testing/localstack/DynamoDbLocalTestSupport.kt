package io.github.capozziv.digitalbanking.testing.localstack

import org.testcontainers.containers.GenericContainer
import org.testcontainers.containers.wait.strategy.Wait
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.dynamodb.DynamoDbClient
import java.net.URI

/**
 * Single place for how tests run DynamoDB Local, the emulator of the statement table in the local environment
 * (LocalStack keeps only SNS and SQS). Tests keep the data in memory.
 */
object DynamoDbLocalTestSupport {
    /** Pinned release; infra/local/docker-compose.yml uses the same tag: change both together. */
    const val IMAGE = "amazon/dynamodb-local:3.3.2"
    const val PORT = 8000

    /** A container (not started) with an empty database. A request without an operation answers 400 once it is up. */
    fun container(): GenericContainer<*> =
        GenericContainer<Nothing>(IMAGE).apply {
            withExposedPorts(PORT)
            withCommand("-jar", "DynamoDBLocal.jar", "-disableTelemetry", "-sharedDb", "-inMemory")
            waitingFor(Wait.forHttp("/").forStatusCode(400).withStartupTimeout(LocalStackTestSupport.STARTUP_TIMEOUT))
        }

    fun endpoint(container: GenericContainer<*>): URI = URI.create("http://${container.host}:${container.getMappedPort(PORT)}")

    fun client(container: GenericContainer<*>): DynamoDbClient =
        DynamoDbClient
            .builder()
            .endpointOverride(endpoint(container))
            .region(Region.US_EAST_1)
            .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create("test", "test")))
            .build()
}
