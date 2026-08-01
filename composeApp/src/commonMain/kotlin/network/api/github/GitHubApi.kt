package io.github.vrcmteam.vrcm.network.api.github

import io.github.vrcmteam.vrcm.network.api.github.data.ReleaseData
import io.github.vrcmteam.vrcm.network.api.github.data.TestingChannelData
import io.github.vrcmteam.vrcm.network.extensions.checkSuccess
import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.http.*

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
            }.checkSuccess<TestingChannelData>()
        }

}
