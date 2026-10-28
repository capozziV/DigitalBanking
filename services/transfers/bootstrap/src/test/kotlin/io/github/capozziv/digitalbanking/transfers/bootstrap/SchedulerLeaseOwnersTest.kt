package io.github.capozziv.digitalbanking.transfers.bootstrap

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class SchedulerLeaseOwnersTest {
    @Test
    fun `an owner names the scheduler, the host and the process, and ends with a fresh uuid`() {
        val owners = SchedulerLeaseOwners(hostname = "host-a", pid = 4242)

        val owner = owners.next()

        assertThat(owner).matches("scheduler-host-a-4242-[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}")
    }

    @Test
    fun `every run gets a different owner`() {
        val owners = SchedulerLeaseOwners(hostname = "host-a", pid = 1)

        assertThat(List(100) { owners.next() }).doesNotHaveDuplicates()
    }

    @Test
    fun `the default owners use the local host name and the current pid`() {
        val owner = SchedulerLeaseOwners().next()

        assertThat(owner).startsWith("scheduler-").contains("-${ProcessHandle.current().pid()}-")
    }
}
