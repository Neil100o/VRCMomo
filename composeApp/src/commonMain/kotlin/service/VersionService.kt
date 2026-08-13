package io.github.vrcmteam.vrcm.service

import io.github.vrcmteam.vrcm.core.shared.AppConst
import io.github.vrcmteam.vrcm.network.api.github.GitHubApi
import io.github.vrcmteam.vrcm.network.supports.VRCApiException
import io.github.vrcmteam.vrcm.service.data.VersionDto
import io.github.vrcmteam.vrcm.storage.SettingsDao

private val GITHUB_OPTIONAL_UPDATE_CODES = setOf(403, 404, 429)

class VersionService(
    private val gitHubApi: GitHubApi,
    private val settingsDao: SettingsDao,
) {

    /**
     * Check the old-signature migration channel first.
     *
     * A 0.3.16-era installation and the permanent-release installation share the same Android
     * package name but not the same certificate. The migration manifest is intentionally kept at
     * 0.3.20: old clients see it first and can back up activity data before moving to Releases;
     * permanent clients already at 0.3.20 or newer skip it and then check GitHub Releases.
     */
    suspend fun checkVersion(checkRemember: Boolean): Result<VersionDto> =
        gitHubApi.testingChannel(AppConst.APP_TESTING_CHANNEL_URL).fold(
            onSuccess = { channel ->
                val testingUpdate = VersionDto(
                    tagName = channel.version,
                    htmlUrl = channel.pageUrl,
                    body = channel.notes,
                    hasNewVersion = isNewerVersion(channel.version, AppConst.APP_VERSION) &&
                        (!checkRemember || settingsDao.rememberVersion != channel.version),
                    downloadUrl = listOf(channel.apkUrl),
                )
                if (testingUpdate.hasNewVersion) Result.success(testingUpdate)
                else checkLatestRelease(checkRemember)
            },
            onFailure = { testingError -> checkLatestRelease(checkRemember, testingError) },
        )

    private suspend fun checkLatestRelease(
        checkRemember: Boolean,
        fallbackError: Throwable? = null,
    ): Result<VersionDto> =
        gitHubApi.latestRelease(AppConst.APP_GITHUB_LATEST_RELEASE_URL).fold(
            onSuccess = { release ->
                Result.success(
                    VersionDto(
                        tagName = release.tagName,
                        htmlUrl = release.htmlUrl,
                        body = release.body,
                        hasNewVersion = isNewerVersion(release.tagName, AppConst.APP_VERSION) &&
                            (!checkRemember || settingsDao.rememberVersion != release.tagName),
                        downloadUrl = release.assets.map { asset -> asset.browserDownloadUrl },
                    ),
                )
            },
            onFailure = { releaseError ->
                val error = fallbackError ?: releaseError
                if (error is VRCApiException && error.code in GITHUB_OPTIONAL_UPDATE_CODES) {
                    // GitHub's public API is shared by all users behind the same network address.
                    // A missing release, rate limit, or temporary GitHub refusal must never affect login.
                    noUpdateAvailable()
                } else {
                    Result.failure(error)
                }
            },
        )

    private fun isNewerVersion(candidate: String, current: String): Boolean {
        val candidateParts = candidate.removePrefix("v").split('.').map { it.toIntOrNull() ?: 0 }
        val currentParts = current.removePrefix("v").split('.').map { it.toIntOrNull() ?: 0 }
        val maxLength = maxOf(candidateParts.size, currentParts.size)
        return (0 until maxLength).firstNotNullOfOrNull { index ->
            (candidateParts.getOrElse(index) { 0 }).compareTo(currentParts.getOrElse(index) { 0 })
                .takeIf { it != 0 }
        }?.let { it > 0 } ?: false
    }

    private fun noUpdateAvailable(): Result<VersionDto> = Result.success(
        VersionDto(
            tagName = AppConst.APP_VERSION,
            htmlUrl = AppConst.APP_GITHUB_URL,
            body = "",
            hasNewVersion = false,
        )
    )

    fun rememberVersion(version: String?) {
        settingsDao.rememberVersion = version
    }

}
