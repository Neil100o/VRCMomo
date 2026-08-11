package io.github.vrcmteam.vrcm.network.api.status

import io.github.vrcmteam.vrcm.network.api.status.data.VrchatStatusData
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import kotlinx.serialization.json.Json

class VrchatStatusApi {
    private val client = HttpClient()
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun fetchStatus(): Result<VrchatStatusData> = runCatching {
        json.decodeFromString<VrchatStatusData>(
            client.get(STATUS_URL).bodyAsText(),
        )
    }

    private companion object {
        const val STATUS_URL = "https://status.vrchat.com/api/v2/status.json"
    }
}
