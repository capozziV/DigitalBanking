package io.github.capozziv.digitalbanking.security

import io.github.capozziv.digitalbanking.testing.security.JwtTestSupport
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort
import java.util.UUID

/** On AWS the groups come in `cognito:groups`: the claim name is a property. */
@SpringBootTest(
    classes = [SecurityTestApplication::class],
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = ["digitalbanking.security.groups-claim=cognito:groups"],
)
class GroupsClaimIntegrationTest {
    @LocalServerPort
    private var port: Int = 0

    private val userId = UUID.randomUUID()

    @Test
    fun `the admin group is read from the configured claim`() {
        val http = TestHttp(port)

        val cognitoAdmin = JwtTestSupport.token(userId.toString(), groups = listOf("admin"), groupsClaim = "cognito:groups")
        val defaultClaimAdmin = JwtTestSupport.adminToken(userId)

        assertThat(http.get("/admin/ping", cognitoAdmin).statusCode()).isEqualTo(200)
        assertProblem(http.get("/admin/ping", defaultClaimAdmin), 403, "forbidden")
    }
}
