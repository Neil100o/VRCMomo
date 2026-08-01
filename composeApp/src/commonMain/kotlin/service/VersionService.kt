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
     * 获取最新的版本号,和版本链接
     * @param checkRemember 是否检查记住版本
     * @return 最新版本号和最新版本链接
     */
    suspend fun checkVersion(checkRemember: Boolean): Result<VersionDto> =
        gitHubApi.latestRelease(AppConst.APP_GITHUB_LATEST_RELEASE_URL).let { it ->
            when {
                it.isSuccess -> {
                    val releaseData = it.getOrNull()!!
                    val tagName = releaseData.tagName
                    val downloadUrl = releaseData.assets.map { asset -> asset.browserDownloadUrl }
                    if (AppConst.APP_VERSION == tagName
                        || (checkRemember && settingsDao.rememberVersion == tagName)
                    ) {
                        // 当前版本是最新版本
                        Result.success(VersionDto(tagName, releaseData.htmlUrl, releaseData.body, false,downloadUrl))
                    } else {
                        // 当前版本不是最新版本
                        Result.success(VersionDto(tagName, releaseData.htmlUrl, releaseData.body, true,downloadUrl))
                    }
                }

                else -> {
                    val error = it.exceptionOrNull()!!
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
