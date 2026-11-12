package io.github.capozziv.digitalbanking.testing.redis

import org.testcontainers.containers.GenericContainer

/** Single place for how integration tests run Redis: pinned image, exposed port, and pause/unpause to simulate an outage. */
object RedisTestSupport {
    const val IMAGE = "redis:7-alpine"
    const val PORT = 6379

    /** A new, not yet started Redis container. */
    fun container(): GenericContainer<*> = GenericContainer<Nothing>(IMAGE).withExposedPorts(PORT)

    /** Freezes the container: connections are accepted by the kernel but no command is answered. */
    fun pause(container: GenericContainer<*>) {
        container.dockerClient.pauseContainerCmd(container.containerId).exec()
    }

    fun unpause(container: GenericContainer<*>) {
        container.dockerClient.unpauseContainerCmd(container.containerId).exec()
    }
}
