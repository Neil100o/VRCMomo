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
     * Prefer GitHub Releases. The temporary testing manifest remains only as a fallback for
     * the old-signature migration track, so established installs do not get stranded.
     * @param checkRemember 是否检查记住版本
     * @return 最新版本号和最新版本链接
     */
    suspend fun checkVersion(checkRemember: Boolean): Result<VersionDto> =
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
            onFailure = { releaseError -> checkTestingFallback(checkRemember, releaseError) },
        )

    private suspend fun checkTestingFallback(
        checkRemember: Boolean,
        releaseError: Throwable,
    ): Result<VersionDto> =
        gitHubApi.testingChannel(AppConst.APP_TESTING_CHANNEL_URL).let { it ->
            when {
                it.isSuccess -> {
                    val channel = it.getOrNull() ?: return@let Result.failure(releaseError)
                    Result.success(
                        VersionDto(
                            tagName = channel.version,
                            htmlUrl = channel.pageUrl,
                            body = channel.notes,
                            hasNewVersion = isNewerVersion(channel.version, AppConst.APP_VERSION) &&
                                (!checkRemember || settingsDao.rememberVersion != channel.version),
                            downloadUrl = listOf(channel.apkUrl),
                        ),
                    )
                }

                else -> {
                    val error = it.exceptionOrNull() ?: releaseError
                    if (error is VRCApiException && error.code in GITHUB_OPTIONAL_UPDATE_CODES) {
                        // GitHub's public API is shared by all users behind the same network address.
                        // A missing release, rate limit, or temporary GitHub refusal must never affect login.
                        noUpdateAvailable()
                    } else {
                        Result.failure(error)
                    }
                }
            }
        }

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
