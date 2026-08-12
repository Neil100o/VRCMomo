package io.github.vrcmteam.vrcm.presentation.screens.group

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import io.github.vrcmteam.vrcm.core.shared.SharedFlowCentre
import io.github.vrcmteam.vrcm.network.api.groups.GroupsApi
import io.github.vrcmteam.vrcm.network.api.groups.data.GroupGalleryImage
import io.github.vrcmteam.vrcm.network.api.groups.data.GroupMember
import io.github.vrcmteam.vrcm.network.api.groups.data.GroupPost
import io.github.vrcmteam.vrcm.network.api.instances.data.InstanceData
import io.github.vrcmteam.vrcm.network.api.users.UsersApi
import io.github.vrcmteam.vrcm.network.api.users.data.UserData
import io.github.vrcmteam.vrcm.presentation.compoments.ToastText
import io.github.vrcmteam.vrcm.presentation.screens.group.data.GroupProfileVo
import io.github.vrcmteam.vrcm.service.AuthService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.coroutineScope
import org.koin.core.logger.Logger

class GroupProfileScreenModel(
    private val groupsApi: GroupsApi,
    private val usersApi: UsersApi,
    private val authService: AuthService,
    private val logger: Logger,
) : ScreenModel {

    private val _groupProfileState = MutableStateFlow<GroupProfileVo?>(null)
    val groupProfileState: StateFlow<GroupProfileVo?> = _groupProfileState.asStateFlow()

    private val _members = MutableStateFlow<List<GroupMember>>(emptyList())
    val members: StateFlow<List<GroupMember>> = _members.asStateFlow()

    private val _owner = MutableStateFlow<UserData?>(null)
    val owner: StateFlow<UserData?> = _owner.asStateFlow()

    private val _galleryImages = MutableStateFlow<Map<String, List<GroupGalleryImage>>>(emptyMap())
    val galleryImages: StateFlow<Map<String, List<GroupGalleryImage>>> = _galleryImages.asStateFlow()
    private val _galleryCanLoadMore = MutableStateFlow<Map<String, Boolean>>(emptyMap())
    val galleryCanLoadMore: StateFlow<Map<String, Boolean>> = _galleryCanLoadMore.asStateFlow()
    private val _galleryLoadingMoreIds = MutableStateFlow<Set<String>>(emptySet())
    val galleryLoadingMoreIds: StateFlow<Set<String>> = _galleryLoadingMoreIds.asStateFlow()

    private val _posts = MutableStateFlow<List<GroupPost>>(emptyList())
    val posts: StateFlow<List<GroupPost>> = _posts.asStateFlow()

    private val _postAuthors = MutableStateFlow<Map<String, String>>(emptyMap())
    val postAuthors: StateFlow<Map<String, String>> = _postAuthors.asStateFlow()

    private val _postsLoading = MutableStateFlow(false)
    val postsLoading: StateFlow<Boolean> = _postsLoading.asStateFlow()

    private val _membersLoading = MutableStateFlow(false)
    val membersLoading: StateFlow<Boolean> = _membersLoading.asStateFlow()
    private val _membersLoadingMore = MutableStateFlow(false)
    val membersLoadingMore: StateFlow<Boolean> = _membersLoadingMore.asStateFlow()
    private val _membersCanLoadMore = MutableStateFlow(false)
    val membersCanLoadMore: StateFlow<Boolean> = _membersCanLoadMore.asStateFlow()

    private val _postsLoadingMore = MutableStateFlow(false)
    val postsLoadingMore: StateFlow<Boolean> = _postsLoadingMore.asStateFlow()
    private val _postsCanLoadMore = MutableStateFlow(false)
    val postsCanLoadMore: StateFlow<Boolean> = _postsCanLoadMore.asStateFlow()

    private val _groupInstances = MutableStateFlow<List<InstanceData>>(emptyList())
    val groupInstances: StateFlow<List<InstanceData>> = _groupInstances.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _isActionLoading = MutableStateFlow(false)
    val isActionLoading: StateFlow<Boolean> = _isActionLoading.asStateFlow()

    fun refreshGroupData(groupProfileVo: GroupProfileVo) {
        _groupProfileState.value = groupProfileVo
        _members.value = emptyList()
        _owner.value = null
        _galleryImages.value = emptyMap()
        _galleryCanLoadMore.value = emptyMap()
        _galleryLoadingMoreIds.value = emptySet()
        _posts.value = emptyList()
        _postAuthors.value = emptyMap()
        _postsLoading.value = true
        _postsCanLoadMore.value = false
        _membersLoading.value = true
        _membersCanLoadMore.value = false
        _groupInstances.value = emptyList()
        val groupId = groupProfileVo.groupId
        if (_isLoading.value || groupId.isBlank()) {
            _postsLoading.value = false
            _membersLoading.value = false
            return
        }
        _isLoading.value = true
        screenModelScope.launch(Dispatchers.IO) {
            fetchGroupProfile(groupId).onSuccess {
                _groupProfileState.value = GroupProfileVo(it)
            }.onFailure {
                handleError("GroupProfile", it)
            }
            val group = _groupProfileState.value
            if (group?.ownerId != null) {
                loadOwner(group.ownerId)
            }
            if (group != null) {
                coroutineScope {
                    listOf(
                        async { loadMembers(groupId) },
                        async { loadPosts(groupId) },
                        async { loadGroupInstances(groupId) },
                        async { loadGalleryImages(groupId, group.galleries) }
                    ).awaitAll()
                }
            } else {
                _postsLoading.value = false
                _membersLoading.value = false
            }
            _isLoading.value = false
        }
    }

    /**
     * Role details are useful for members, but VRChat may reject or return a
     * different payload for public groups that the current user has not joined.
     * Retry the same profile without roles so the basic group page and join
     * action remain available.
     */
    private suspend fun fetchGroupProfile(groupId: String) =
        authService.reTryAuthCatching {
            groupsApi.fetchGroup(groupId, includeRoles = true)
        }.recoverCatching {
            groupsApi.fetchGroup(groupId, includeRoles = false)
        }

    fun joinGroup() {
        val groupId = _groupProfileState.value?.groupId ?: return
        if (_isActionLoading.value) return
        _isActionLoading.value = true
        screenModelScope.launch(Dispatchers.IO) {
            authService.reTryAuthCatching {
                groupsApi.joinGroup(groupId)
            }.onFailure {
                handleError("GroupJoin", it)
            }.onSuccess {
                _groupProfileState.value?.let { refreshGroupData(it) }
            }
            _isActionLoading.value = false
        }
    }

    fun leaveGroup() {
        val groupId = _groupProfileState.value?.groupId ?: return
        if (_isActionLoading.value) return
        _isActionLoading.value = true
        screenModelScope.launch(Dispatchers.IO) {
            authService.reTryAuthCatching {
                groupsApi.leaveGroup(groupId)
            }.onFailure {
                handleError("GroupLeave", it)
            }.onSuccess {
                _groupProfileState.value?.let { refreshGroupData(it) }
            }
            _isActionLoading.value = false
        }
    }

    fun loadMoreMembers() {
        val groupId = _groupProfileState.value?.groupId ?: return
        if (_membersLoadingMore.value || !_membersCanLoadMore.value) return
        _membersLoadingMore.value = true
        screenModelScope.launch(Dispatchers.IO) {
            authService.reTryAuthCatching {
                groupsApi.getGroupMembers(groupId = groupId, n = MEMBER_PAGE_SIZE, offset = _members.value.size)
            }.onSuccess { page ->
                _members.value = (_members.value + page).distinctBy { it.userId }
                _membersCanLoadMore.value = page.size >= MEMBER_PAGE_SIZE
            }.onFailure { logger.error(it.message.orEmpty()) }
            _membersLoadingMore.value = false
        }
    }

    private suspend fun loadMembers(groupId: String) {
        _membersLoading.value = true
        authService.reTryAuthCatching {
            groupsApi.getGroupMembers(groupId = groupId, n = MEMBER_PAGE_SIZE, offset = 0)
        }.onSuccess { page ->
            _members.value = page
            _membersCanLoadMore.value = page.size >= MEMBER_PAGE_SIZE
        }.onFailure { logger.error(it.message.orEmpty()) }
        _membersLoading.value = false
    }

    private suspend fun loadOwner(ownerId: String) {
        authService.reTryAuthCatching {
            usersApi.fetchUser(ownerId)
        }.onSuccess {
            _owner.value = it
        }.onFailure {
            logger.error(it.message.orEmpty())
        }
    }

    fun loadMorePosts() {
        val groupId = _groupProfileState.value?.groupId ?: return
        if (_postsLoadingMore.value || !_postsCanLoadMore.value) return
        _postsLoadingMore.value = true
        screenModelScope.launch(Dispatchers.IO) {
            authService.reTryAuthCatching {
                groupsApi.getGroupPosts(groupId = groupId, n = POST_PAGE_SIZE, offset = _posts.value.size)
            }.onSuccess { page ->
                _posts.value = (_posts.value + page.posts).distinctBy { it.id }
                _postsCanLoadMore.value = page.posts.size >= POST_PAGE_SIZE
                resolvePostAuthors(page.posts)
            }.onFailure { logger.error(it.message.orEmpty()) }
            _postsLoadingMore.value = false
        }
    }

    private suspend fun loadPosts(groupId: String) {
        _postsLoading.value = true
        authService.reTryAuthCatching {
            groupsApi.getGroupPosts(groupId = groupId, n = POST_PAGE_SIZE, offset = 0)
        }.onSuccess { postData ->
            _posts.value = postData.posts
            _postsCanLoadMore.value = postData.posts.size >= POST_PAGE_SIZE
            resolvePostAuthors(postData.posts)
        }.onFailure { logger.error(it.message.orEmpty()) }
        _postsLoading.value = false
    }

    private suspend fun resolvePostAuthors(posts: List<GroupPost>) {
        val authorMap = _postAuthors.value.toMutableMap()
        val authorIds = posts.mapNotNull { it.authorId.takeIf(String::isNotBlank) }
            .distinct().filterNot(authorMap::containsKey)
        coroutineScope {
            authorIds.chunked(5).forEach { chunk ->
                chunk.map { userId ->
                    async {
                        authService.reTryAuthCatching { usersApi.fetchUser(userId) }
                            .getOrNull()?.displayName?.let { userId to it }
                    }
                }.awaitAll().forEach { result -> result?.let { (id, name) -> authorMap[id] = name } }
                _postAuthors.value = authorMap.toMap()
            }
        }
    }

    private suspend fun loadGroupInstances(groupId: String) {
        val userId = authService.currentUser().id
        if (userId.isBlank()) return
        authService.reTryAuthCatching {
            groupsApi.getGroupInstances(userId = userId, groupId = groupId)
        }.onSuccess {
            _groupInstances.value = it.instances
        }.onFailure {
            logger.error(it.message.orEmpty())
        }
    }

    fun loadMoreGallery(galleryId: String) {
        val groupId = _groupProfileState.value?.groupId ?: return
        if (_galleryLoadingMoreIds.value.contains(galleryId) || _galleryCanLoadMore.value[galleryId] != true) return
        _galleryLoadingMoreIds.value += galleryId
        screenModelScope.launch(Dispatchers.IO) {
            val offset = _galleryImages.value[galleryId].orEmpty().size
            authService.reTryAuthCatching {
                groupsApi.getGroupGalleryImages(
                    groupId = groupId,
                    groupGalleryId = galleryId,
                    n = GALLERY_PAGE_SIZE,
                    offset = offset,
                )
            }.onSuccess { page ->
                _galleryImages.value = _galleryImages.value.toMutableMap().apply {
                    this[galleryId] = (this[galleryId].orEmpty() + page).distinctBy { it.id }
                }
                _galleryCanLoadMore.value = _galleryCanLoadMore.value + (galleryId to (page.size >= GALLERY_PAGE_SIZE))
            }.onFailure { logger.error(it.message.orEmpty()) }
            _galleryLoadingMoreIds.value -= galleryId
        }
    }

    private suspend fun loadGalleryImages(groupId: String, galleries: List<io.github.vrcmteam.vrcm.network.api.groups.data.Gallery>) {
        if (galleries.isEmpty()) return
        val imagesMap = mutableMapOf<String, List<GroupGalleryImage>>()
        galleries.forEach { gallery ->
            authService.reTryAuthCatching {
                groupsApi.getGroupGalleryImages(groupId = groupId, groupGalleryId = gallery.id, n = GALLERY_PAGE_SIZE, offset = 0)
            }.onSuccess { page ->
                imagesMap[gallery.id] = page
                _galleryCanLoadMore.value = _galleryCanLoadMore.value + (gallery.id to (page.size >= GALLERY_PAGE_SIZE))
            }.onFailure {
                logger.error(it.message.orEmpty())
            }
        }
        _galleryImages.value = imagesMap
    }

    private companion object {
        const val MEMBER_PAGE_SIZE = 24
        const val POST_PAGE_SIZE = 20
        const val GALLERY_PAGE_SIZE = 30
    }

    private suspend fun handleError(tag: String, error: Throwable) {
        logger.error("$tag: ${error.message}")
        SharedFlowCentre.toastText.emit(ToastText.Error(error.message ?: "Unknown error"))
    }
}
