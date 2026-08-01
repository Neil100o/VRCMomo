package io.github.vrcmteam.vrcm.presentation.screens.avatar

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import io.github.vrcmteam.vrcm.core.shared.SharedFlowCentre
import io.github.vrcmteam.vrcm.network.api.avatars.AvatarsApi
import io.github.vrcmteam.vrcm.network.api.avatars.data.AvatarData
import io.github.vrcmteam.vrcm.network.api.avatars.data.AvatarUpdateData
import io.github.vrcmteam.vrcm.network.api.files.FileApi
import io.github.vrcmteam.vrcm.network.api.files.data.FileTagType
import io.github.vrcmteam.vrcm.presentation.compoments.ToastText
import io.github.vrcmteam.vrcm.presentation.screens.avatar.data.AvatarProfileVo
import io.github.vrcmteam.vrcm.service.AuthService
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.updateAndGet
import kotlinx.coroutines.launch

internal fun interface AvatarProfileLoader {
    suspend fun load(avatarId: String): Result<AvatarData>
}

internal class NetworkAvatarProfileLoader(
    private val avatarsApi: AvatarsApi,
    private val authService: AuthService,
) : AvatarProfileLoader {
    override suspend fun load(avatarId: String): Result<AvatarData> =
        authService.reTryAuthCatching { avatarsApi.getAvatarById(avatarId) }
}

class AvatarProfileScreenModel internal constructor(
    private val avatarProfileLoader: AvatarProfileLoader,
    private val requestDispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val avatarsApi: AvatarsApi? = null,
    private val authService: AuthService? = null,
    private val fileApi: FileApi? = null,
) : ScreenModel {

    private val _avatarProfileState = MutableStateFlow<AvatarProfileVo?>(null)
    val avatarProfileState: StateFlow<AvatarProfileVo?> = _avatarProfileState.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val latestRequestToken = MutableStateFlow(0L)

    private val _isSaving = MutableStateFlow(false)
    val isSaving: StateFlow<Boolean> = _isSaving.asStateFlow()

    fun canEdit(avatar: AvatarProfileVo): Boolean =
        avatar.authorId.isNotBlank() && avatar.authorId == authService?.accountDto()?.userId

    fun updateAvatar(
        avatar: AvatarProfileVo,
        name: String,
        description: String,
        imageUrl: String?,
    ) {
        val api = avatarsApi
        val auth = authService
        if (api == null || auth == null || !canEdit(avatar)) {
            screenModelScope.launch {
                SharedFlowCentre.toastText.emit(ToastText.Error("Only the avatar owner can edit this avatar"))
            }
            return
        }
        _isSaving.value = true
        screenModelScope.launch(requestDispatcher) {
            auth.reTryAuthCatching {
                api.updateAvatar(
                    avatar.avatarId,
                    AvatarUpdateData(
                        name = name.trim().takeIf { it.isNotEmpty() },
                        description = description.trim().takeIf { it.isNotEmpty() },
                        imageUrl = imageUrl?.trim()?.takeIf { it.isNotEmpty() },
                    )
                )
            }.onSuccess {
                _avatarProfileState.value = AvatarProfileVo(it)
                SharedFlowCentre.toastText.emit(ToastText.Success("Avatar updated"))
            }.onFailure {
                SharedFlowCentre.toastText.emit(ToastText.Error(it.message ?: "Failed to update avatar"))
            }
            _isSaving.value = false
        }
    }


    /** Uploads an official `avatarimage` file and immediately assigns it as this avatar's cover. */
    fun uploadCoverAndUpdateAvatar(
        avatar: AvatarProfileVo,
        name: String,
        description: String,
        imageBytes: ByteArray,
        fileName: String,
        mimeType: String,
    ) {
        val api = avatarsApi
        val auth = authService
        val files = fileApi
        if (api == null || auth == null || files == null || !canEdit(avatar)) {
            screenModelScope.launch {
                SharedFlowCentre.toastText.emit(ToastText.Error("Only the avatar owner can edit this avatar"))
            }
            return
        }
        if (_isSaving.value) return
        _isSaving.value = true
        screenModelScope.launch(requestDispatcher) {
            auth.reTryAuthCatching {
                files.uploadImageFile(
                    fileBytes = imageBytes,
                    fileName = fileName,
                    mimeType = mimeType,
                    tagType = FileTagType.AvatarImage,
                ).getOrThrow()
            }.onSuccess { file ->
                val version = file.versions.maxOfOrNull { it.version } ?: 1
                auth.reTryAuthCatching {
                    api.updateAvatar(
                        avatar.avatarId,
                        AvatarUpdateData(
                            name = name.trim().takeIf { it.isNotEmpty() },
                            description = description.trim().takeIf { it.isNotEmpty() },
                            imageUrl = FileApi.imageUrl(file.id, version),
                        )
                    )
                }.onSuccess { updated ->
                    _avatarProfileState.value = AvatarProfileVo(updated)
                    SharedFlowCentre.toastText.emit(ToastText.Success("Avatar cover and metadata updated"))
                }.onFailure { error ->
                    SharedFlowCentre.toastText.emit(ToastText.Error(error.message ?: "Failed to update avatar"))
                }
            }.onFailure { error ->
                SharedFlowCentre.toastText.emit(ToastText.Error(error.message ?: "Failed to upload avatar cover"))
            }
            _isSaving.value = false
        }
    }

    fun refreshAvatarData(avatarProfileVo: AvatarProfileVo) {
        val requestToken = latestRequestToken.updateAndGet { it + 1 }
        _avatarProfileState.value = avatarProfileVo
        val avatarId = avatarProfileVo.avatarId
        if (avatarId.isBlank()) {
            _isLoading.value = false
            return
        }
        _isLoading.value = true
        screenModelScope.launch(requestDispatcher) {
            avatarProfileLoader.load(avatarId)
                .onSuccess { avatarData ->
                    if (requestToken == latestRequestToken.value) {
                        _avatarProfileState.value = AvatarProfileVo(avatarData)
                    }
                }
                .onFailure {
                    if (requestToken == latestRequestToken.value) {
                        SharedFlowCentre.toastText.emit(
                            ToastText.Error(it.message ?: "Failed to load avatar data")
                        )
                    }
                }
            if (requestToken == latestRequestToken.value) {
                _isLoading.value = false
            }
        }
    }
}
