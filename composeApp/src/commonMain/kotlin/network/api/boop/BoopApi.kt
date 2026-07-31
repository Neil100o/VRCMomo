package io.github.vrcmteam.vrcm.network.api.boop

import io.github.vrcmteam.vrcm.network.api.attributes.VRChatResponse
import io.github.vrcmteam.vrcm.network.api.boop.data.SendBoopData
import io.github.vrcmteam.vrcm.network.extensions.checkSuccess
import io.ktor.client.*
import io.ktor.client.request.*

class BoopApi(private val client: HttpClient) {
    suspend fun sendBoop(
        userId: String,
        data: SendBoopData = SendBoopData(),
    ): VRChatResponse = client.post("/api/1/users/$userId/boop") {
        setBody(data)
    }.checkSuccess()
}
