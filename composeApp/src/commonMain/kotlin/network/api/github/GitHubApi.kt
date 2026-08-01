package io.github.vrcmteam.vrcm.network.api.github

import io.github.vrcmteam.vrcm.network.api.github.data.ReleaseData
import io.github.vrcmteam.vrcm.network.api.github.data.TestingChannelData
import io.github.vrcmteam.vrcm.network.extensions.checkSuccess
import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.bodyAsText
import io.ktor.http.*
import kotlinx.serialization.json.Json

class GitHubApi(
    private val client: HttpClient
) {

    suspend fun latestRelease(releaseUrl: String): Result<ReleaseData> =
        runCatching {
            client.get(releaseUrl) {
                githubAuthToken()?.let { token ->
                    header(HttpHeaders.Authorization, "Bearer $token")
                }
            }.checkSuccess<ReleaseData>()
        }

    suspend fun testingChannel(channelUrl: String): Result<TestingChannelData> =
        runCatching {
            client.get(channelUrl) {
                githubAuthToken()?.let { token ->
                    header(HttpHeaders.Authorization, "Bearer $token")
                }
            }.checkSuccess {
                // raw.githubusercontent.com serves .json files as text/plain. Decoding the
                // text explicitly avoids Ktor rejecting the otherwise valid update manifest.
                Json { ignoreUnknownKeys = true }.decodeFromString<TestingChannelData>(bodyAsText())
            }
        }

}
