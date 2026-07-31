package io.github.vrcmteam.vrcm.network.api.notification

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class NotificationApiTest {
    @Test
    fun friendRequestsAreLocallyFilteredWhenTheServerReturnsMixedNotificationTypes() = runBlocking {
        var sentTypeParameter: String? = "not checked"
        val client = HttpClient(MockEngine) {
            engine {
                addHandler { request ->
                    sentTypeParameter = request.url.parameters["type"]
                    respond(
                        content = """
                            [
                              {"created_at":"2026-01-01T00:00:00Z","details":"{}","id":"frq_1","message":"","seen":false,"senderUserId":"usr_friend","receiverUserId":"usr_me","type":"friendRequest"},
                              {"created_at":"2026-01-01T00:01:00Z","details":"{}","id":"not_1","message":"","seen":false,"senderUserId":"usr_boop","receiverUserId":"usr_me","type":"boop"}
                            ]
                        """.trimIndent(),
                        status = HttpStatusCode.OK,
                        headers = headersOf(HttpHeaders.ContentType, "application/json"),
                    )
                }
            }
            install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
        }

        val notifications = NotificationApi(client).fetchFriendRequestNotifications()

        assertEquals(listOf("frq_1"), notifications.map { it.id })
        assertNull(sentTypeParameter)
        client.close()
    }
}
