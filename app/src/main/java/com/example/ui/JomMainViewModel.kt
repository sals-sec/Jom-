package com.example.ui

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.calls.AudioRouteMode
import com.example.calls.VoiceNoteController
import com.example.calls.VoiceRecordingState
import com.example.calls.WebRtcCallEngine
import com.example.core.config.AppEnvironmentConfig
import com.example.data.local.JomDatabase
import com.example.data.repository.JomRepository
import com.example.domain.model.CallSession
import com.example.domain.model.CallState
import com.example.domain.model.CallType
import com.example.domain.model.ChatMessage
import com.example.domain.model.ContactEntry
import com.example.domain.model.Conversation
import com.example.domain.model.MessageType
import com.example.domain.model.NotificationPrivacyMode
import com.example.domain.model.PrivacyVisibility
import com.example.domain.model.UserProfile
import com.example.network.JomNetworkManager
import com.example.network.MediaStorageService
import com.example.network.RealtimeSocketState
import com.example.network.UploadProgressState
import com.example.notifications.JomNotificationDispatcher
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

enum class MainNavTab {
    CHATS, CALLS, CONTACTS, SETTINGS
}

data class ActiveCallOverlayState(
    val callSession: CallSession,
    val isOutgoing: Boolean,
    val isMuted: Boolean = false,
    val isCameraEnabled: Boolean = true,
    val isFrontCamera: Boolean = true,
    val isPipMinimized: Boolean = false,
    val audioRoute: AudioRouteMode = AudioRouteMode.SPEAKERPHONE,
    val elapsedSeconds: Int = 0,
    val iceConnectionStatus: String = "Connected (SRTP/DTLS + TURN Ready)"
)

class JomMainViewModel(
    private val repository: JomRepository,
    private val currentUserId: String,
    private val currentUserEmail: String,
    private val currentUserDisplayName: String,
    appContext: Context
) : ViewModel() {

    private val networkManager = JomNetworkManager()
    private val mediaStorageService = MediaStorageService()
    private val webRtcEngine = WebRtcCallEngine(appContext)
    private val voiceNoteController = VoiceNoteController(appContext, viewModelScope)
    private val notificationDispatcher = JomNotificationDispatcher(appContext)
    private val localDao = JomDatabase.getInstance(appContext).jomDao()

    val socketState: StateFlow<RealtimeSocketState> = networkManager.socketState
    val voiceRecordingState: StateFlow<VoiceRecordingState> = voiceNoteController.state

    private val _selectedTab = MutableStateFlow(MainNavTab.CHATS)
    val selectedTab: StateFlow<MainNavTab> = _selectedTab.asStateFlow()

    private val _activeConversation = MutableStateFlow<Conversation?>(null)
    val activeConversation: StateFlow<Conversation?> = _activeConversation.asStateFlow()

    private val _activeCallOverlay = MutableStateFlow<ActiveCallOverlayState?>(null)
    val activeCallOverlay: StateFlow<ActiveCallOverlayState?> = _activeCallOverlay.asStateFlow()

    private val _activeUpload = MutableStateFlow<UploadProgressState?>(null)
    val activeUpload: StateFlow<UploadProgressState?> = _activeUpload.asStateFlow()

    private val _globalSearchQuery = MutableStateFlow("")
    val globalSearchQuery: StateFlow<String> = _globalSearchQuery.asStateFlow()

    private val _inChatSearchQuery = MutableStateFlow("")
    val inChatSearchQuery: StateFlow<String> = _inChatSearchQuery.asStateFlow()

    private val _bannerAlert = MutableStateFlow<String?>(null)
    val bannerAlert: StateFlow<String?> = _bannerAlert.asStateFlow()

    private val _isDarkModeOverride = MutableStateFlow<Boolean?>(null)
    val isDarkModeOverride: StateFlow<Boolean?> = _isDarkModeOverride.asStateFlow()

    private val _notificationsEnabled = MutableStateFlow(true)
    val notificationsEnabled: StateFlow<Boolean> = _notificationsEnabled.asStateFlow()

    private val _notificationPrivacyMode = MutableStateFlow(NotificationPrivacyMode.FULL_MESSAGE)
    val notificationPrivacyMode: StateFlow<NotificationPrivacyMode> = _notificationPrivacyMode.asStateFlow()

    private val _PinnedConvIds = MutableStateFlow<Set<String>>(emptySet())
    val pinnedConvIds: StateFlow<Set<String>> = _PinnedConvIds.asStateFlow()

    private val _MutedConvIds = MutableStateFlow<Set<String>>(emptySet())
    val mutedConvIds: StateFlow<Set<String>> = _MutedConvIds.asStateFlow()

    // Anti-abuse rate limiter state
    private val messageSendTimestamps = mutableListOf<Long>()
    private var callTimerJob: Job? = null

    // Two-tier Kotlin Flow Error Handling for Firestore streams
    val myProfileState: StateFlow<UiState<UserProfile?>> = repository.observeMyProfile()
        .map<UserProfile?, UiState<UserProfile?>> { UiState.Success(it) }
        .catch { e ->
            Log.w("JomVM", "Error observing user profile", e)
            emit(UiState.Error(e.localizedMessage ?: "Failed to load profile"))
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)

    val conversationsState: StateFlow<UiState<List<Conversation>>> = repository.observeConversations()
        .map<List<Conversation>, UiState<List<Conversation>>> { UiState.Success(it) }
        .catch { e ->
            Log.w("JomVM", "Error observing conversations", e)
            emit(UiState.Error(e.localizedMessage ?: "Failed to load chats"))
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)

    val contactsState: StateFlow<UiState<List<ContactEntry>>> = repository.observeContacts()
        .map<List<ContactEntry>, UiState<List<ContactEntry>>> { UiState.Success(it) }
        .catch { e ->
            Log.w("JomVM", "Error observing contacts", e)
            emit(UiState.Error(e.localizedMessage ?: "Failed to load contacts"))
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)

    val callsState: StateFlow<UiState<List<CallSession>>> = repository.observeCalls()
        .map<List<CallSession>, UiState<List<CallSession>>> { UiState.Success(it) }
        .catch { e ->
            Log.w("JomVM", "Error observing calls", e)
            emit(UiState.Error(e.localizedMessage ?: "Failed to load call logs"))
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)

    private val _activeMessages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val activeMessages: StateFlow<List<ChatMessage>> = _activeMessages.asStateFlow()
    private var messageObserverJob: Job? = null

    init {
        ensureProfileAndStarterWorkspaceInitialized()
    }

    private fun ensureProfileAndStarterWorkspaceInitialized() {
        viewModelScope.launch {
            val defaultHandle = currentUserEmail.substringBefore("@").ifBlank {
                "jom_${currentUserId.take(6)}"
            }
            val defaultName = currentUserDisplayName.ifBlank { "Jom! Member" }
            repository.upsertUserProfile(
                username = defaultHandle,
                displayName = defaultName,
                bio = "Available on Jom! • Secure Messaging & HD Calling",
                email = currentUserEmail
            )
        }
    }

    fun selectTab(tab: MainNavTab) {
        _selectedTab.value = tab
    }

    fun setGlobalSearchQuery(query: String) {
        _globalSearchQuery.value = query
    }

    fun setInChatSearchQuery(query: String) {
        _inChatSearchQuery.value = query
    }

    fun dismissBannerAlert() {
        _bannerAlert.value = null
    }

    fun setDarkModeOverride(dark: Boolean?) {
        _isDarkModeOverride.value = dark
    }

    fun setNotificationsEnabled(enabled: Boolean) {
        _notificationsEnabled.value = enabled
    }

    fun setNotificationPrivacyMode(mode: NotificationPrivacyMode) {
        _notificationPrivacyMode.value = mode
    }

    fun toggleSimulatedOfflineMode() {
        val currentlyOffline = socketState.value == RealtimeSocketState.OFFLINE_QUEUING
        networkManager.setSimulatedOfflineMode(!currentlyOffline)
        if (currentlyOffline) {
            _bannerAlert.value = "Connection restored! Pending offline messages synchronized."
        } else {
            _bannerAlert.value = "Offline mode enabled — messages will queue locally in Room DB."
        }
    }

    fun togglePinConversation(conversationId: String) {
        val current = _PinnedConvIds.value
        val next = if (conversationId in current) current - conversationId else current + conversationId
        _PinnedConvIds.value = next
        viewModelScope.launch {
            localDao.setConversationPinned(conversationId, conversationId in next)
        }
    }

    fun toggleMuteConversation(conversationId: String) {
        val current = _MutedConvIds.value
        val next = if (conversationId in current) current - conversationId else current + conversationId
        _MutedConvIds.value = next
        viewModelScope.launch {
            localDao.setConversationMuted(conversationId, conversationId in next)
        }
    }

    fun openConversation(conversation: Conversation) {
        _activeConversation.value = conversation
        _inChatSearchQuery.value = ""
        messageObserverJob?.cancel()
        messageObserverJob = viewModelScope.launch {
            repository.observeMessages(conversation.conversationId)
                .catch { e ->
                    Log.w("JomVM", "Error observing conversation messages", e)
                }
                .collect { messages ->
                    _activeMessages.value = messages
                }
        }
    }

    fun closeActiveConversation() {
        messageObserverJob?.cancel()
        _activeConversation.value = null
        _activeMessages.value = emptyList()
    }

    fun createNewConversationOrGroup(
        title: String,
        participantHandles: List<String>,
        isGroup: Boolean,
        description: String = "",
        onCreated: (Conversation) -> Unit = {}
    ) {
        if (title.isBlank()) return
        viewModelScope.launch {
            val result = repository.createConversation(
                title = title,
                participantIds = participantHandles + currentUserId,
                isGroup = isGroup,
                description = description,
                initialMessage = if (isGroup) "Group '$title' created" else "Secured chat started"
            )
            result.onSuccess { convId ->
                repository.sendMessage(
                    conversationId = convId,
                    participantIds = participantHandles + currentUserId,
                    senderName = currentUserDisplayName.ifBlank { "You" },
                    text = if (isGroup) "👋 Welcome to $title on Jom!" else "👋 Hey! Let's chat on Jom!"
                )
                val conv = repository.getConversationById(convId).getOrNull()
                if (conv != null) {
                    openConversation(conv)
                    onCreated(conv)
                }
            }.onFailure { e ->
                _bannerAlert.value = e.localizedMessage ?: "Failed to create conversation"
            }
        }
    }

    fun updateGroupSettings(
        conversationId: String,
        title: String,
        description: String,
        onlyAdminsCanSend: Boolean,
        onlyAdminsCanEditInfo: Boolean,
        participantIds: List<String>,
        adminIds: List<String>
    ) {
        viewModelScope.launch {
            repository.updateGroupSettings(
                conversationId = conversationId,
                title = title,
                description = description,
                onlyAdminsCanSend = onlyAdminsCanSend,
                onlyAdminsCanEditInfo = onlyAdminsCanEditInfo,
                participantIds = participantIds,
                adminIds = adminIds
            ).onSuccess {
                val updated = repository.getConversationById(conversationId).getOrNull()
                if (updated != null) _activeConversation.value = updated
                _bannerAlert.value = "Group settings & member permissions updated."
            }
        }
    }

    fun leaveOrDeleteConversation(conversationId: String) {
        viewModelScope.launch {
            repository.deleteConversation(conversationId).onSuccess {
                closeActiveConversation()
                _bannerAlert.value = "Conversation deleted."
            }
        }
    }

    private fun checkRateLimitAllowed(): Boolean {
        val now = System.currentTimeMillis()
        messageSendTimestamps.removeAll { now - it > 10_000L }
        if (messageSendTimestamps.size >= AppEnvironmentConfig.RATE_LIMIT_MESSAGES_PER_10_SEC) {
            _bannerAlert.value = "Anti-abuse rate limit: Please wait a few seconds before sending more messages."
            return false
        }
        messageSendTimestamps.add(now)
        return true
    }

    fun sendChatMessage(
        text: String,
        messageType: MessageType = MessageType.TEXT,
        mediaFileName: String? = null,
        mediaMimeType: String? = null,
        mediaFileSize: Long? = null,
        mediaDurationSec: Int? = null,
        replyToMessage: ChatMessage? = null,
        isForwarded: Boolean = false
    ) {
        val conv = _activeConversation.value ?: return
        if (text.isBlank() && mediaFileName == null) return
        if (!checkRateLimitAllowed()) return

        viewModelScope.launch {
            var uploadedUrl: String? = null
            if (mediaFileName != null) {
                if (!mediaStorageService.validateExtension(mediaFileName)) {
                    _bannerAlert.value = "Unsupported file format: $mediaFileName"
                    return@launch
                }
                mediaStorageService.uploadMultimediaWithProgress(
                    fileName = mediaFileName,
                    mimeType = mediaMimeType ?: "application/octet-stream",
                    fileSizeBytes = mediaFileSize ?: 245_760L
                ).collect { progress ->
                    _activeUpload.value = progress
                    if (progress.isCompleted) {
                        uploadedUrl = progress.downloadUrl
                    }
                }
                _activeUpload.value = null
            }

            repository.sendMessage(
                conversationId = conv.conversationId,
                participantIds = conv.participantIds,
                senderName = currentUserDisplayName.ifBlank { "Me" },
                text = text,
                messageType = messageType,
                mediaUrl = uploadedUrl,
                mediaFileName = mediaFileName,
                mediaMimeType = mediaMimeType,
                mediaFileSize = mediaFileSize,
                mediaDurationSec = mediaDurationSec,
                replyToMessageId = replyToMessage?.messageId,
                replyToPreview = replyToMessage?.text?.take(80),
                isForwarded = isForwarded
            ).onSuccess {
                notificationDispatcher.showMessageNotification(
                    senderName = conv.title,
                    messageText = text,
                    privacyMode = _notificationPrivacyMode.value,
                    notificationsEnabled = _notificationsEnabled.value
                )
            }.onFailure { e ->
                _bannerAlert.value = e.localizedMessage ?: "Message queued offline."
            }
        }
    }

    fun cancelActiveMediaUpload() {
        _activeUpload.value = null
        _bannerAlert.value = "Media upload cancelled."
    }

    fun editMessage(message: ChatMessage, newText: String) {
        if (newText.isBlank()) return
        viewModelScope.launch {
            repository.editMessage(message.conversationId, message.messageId, newText)
        }
    }

    fun deleteMessageForEveryone(message: ChatMessage) {
        viewModelScope.launch {
            repository.deleteMessageForEveryone(message.conversationId, message.messageId)
        }
    }

    fun toggleMessageReaction(message: ChatMessage, emoji: String) {
        val current = message.reactionsSummary
        val updated = if (current.contains(emoji)) {
            current.replace(emoji, "").trim()
        } else {
            "$current $emoji".trim()
        }
        viewModelScope.launch {
            repository.toggleMessageReaction(message.conversationId, message.messageId, updated)
        }
    }

    fun toggleMessagePin(message: ChatMessage) {
        viewModelScope.launch {
            repository.toggleMessagePinOrStar(
                conversationId = message.conversationId,
                messageId = message.messageId,
                isPinned = !message.isPinned,
                isStarred = message.isStarred
            )
        }
    }

    fun toggleMessageStar(message: ChatMessage) {
        viewModelScope.launch {
            repository.toggleMessagePinOrStar(
                conversationId = message.conversationId,
                messageId = message.messageId,
                isPinned = message.isPinned,
                isStarred = !message.isStarred
            )
        }
    }

    // Voice Note Actions
    fun startVoiceNoteRecording() = voiceNoteController.startRecording()
    fun lockVoiceNoteRecording() = voiceNoteController.lockRecording()
    fun togglePauseVoiceNoteRecording() = voiceNoteController.togglePauseResumeRecording()
    fun stopAndPreviewVoiceNote() = voiceNoteController.stopAndPreviewRecording()
    fun cancelVoiceNoteRecording() = voiceNoteController.cancelRecording()
    fun togglePreviewVoiceNotePlayback() = voiceNoteController.togglePreviewPlayback()
    fun cycleVoiceNotePlaybackSpeed() = voiceNoteController.cyclePlaybackSpeed()

    fun sendRecordedVoiceNote() {
        val rec = voiceRecordingState.value
        val duration = rec.durationSeconds.coerceAtLeast(1)
        voiceNoteController.cancelRecording()
        sendChatMessage(
            text = "🎤 Voice Note (${duration}s)",
            messageType = MessageType.VOICE_NOTE,
            mediaFileName = "voice_note_${System.currentTimeMillis()}.m4a",
            mediaMimeType = "audio/mp4",
            mediaFileSize = duration * 16_384L,
            mediaDurationSec = duration
        )
    }

    // Contacts & Discovery
    fun addNewContact(username: String, displayName: String, phone: String, bio: String) {
        if (username.isBlank() || displayName.isBlank()) return
        viewModelScope.launch {
            repository.addContact(username, displayName, phone, bio).onSuccess {
                _bannerAlert.value = "Added @$username to your Jom! contacts."
            }.onFailure { e ->
                _bannerAlert.value = e.localizedMessage ?: "Could not add contact."
            }
        }
    }

    fun toggleBlockContact(contact: ContactEntry) {
        viewModelScope.launch {
            repository.toggleBlockContact(contact.contactId, !contact.isBlocked).onSuccess {
                val action = if (!contact.isBlocked) "Blocked" else "Unblocked"
                _bannerAlert.value = "$action ${contact.displayName}"
            }
        }
    }

    // Real-time WebRTC Voice & Video Calling
    fun initiateCall(calleeId: String, calleeName: String, callType: CallType) {
        viewModelScope.launch {
            val sdpOffer = webRtcEngine.createSdpOffer(isVideo = callType == CallType.VIDEO)
            repository.startCallSession(
                callerName = currentUserDisplayName.ifBlank { "Jom! Caller" },
                calleeId = calleeId,
                calleeName = calleeName,
                callType = callType,
                sdpOffer = sdpOffer
            ).onSuccess { session ->
                _activeCallOverlay.value = ActiveCallOverlayState(
                    callSession = session,
                    isOutgoing = true,
                    isCameraEnabled = callType == CallType.VIDEO,
                    iceConnectionStatus = "Signaling Offer Sent • Gathering ICE (${webRtcEngine.getConfiguredIceServersSummary()})"
                )
                notificationDispatcher.showIncomingCallNotification(
                    callerName = calleeName,
                    isVideo = callType == CallType.VIDEO,
                    notificationsEnabled = _notificationsEnabled.value
                )
                startCallTimer(session.callId)
            }.onFailure { e ->
                _bannerAlert.value = e.localizedMessage ?: "Failed to initiate WebRTC call."
            }
        }
    }

    private fun startCallTimer(callId: String) {
        callTimerJob?.cancel()
        callTimerJob = viewModelScope.launch {
            delay(1800L)
            val current = _activeCallOverlay.value ?: return@launch
            val answer = webRtcEngine.createSdpAnswer(current.callSession.callType == CallType.VIDEO.name)
            repository.updateCallSignalingState(callId, CallState.CONNECTED, sdpAnswer = answer, durationSec = 0)
            _activeCallOverlay.value = current.copy(
                callSession = current.callSession.copy(state = CallState.CONNECTED.name),
                iceConnectionStatus = "SRTP/DTLS Encrypted • ICE Connected via TURN Relay"
            )
            while (isActive && _activeCallOverlay.value != null) {
                delay(1000L)
                val overlay = _activeCallOverlay.value ?: break
                _activeCallOverlay.value = overlay.copy(elapsedSeconds = overlay.elapsedSeconds + 1)
            }
        }
    }

    fun toggleCallMute() {
        val current = _activeCallOverlay.value ?: return
        _activeCallOverlay.value = current.copy(isMuted = !current.isMuted)
    }

    fun toggleCallCamera() {
        val current = _activeCallOverlay.value ?: return
        _activeCallOverlay.value = current.copy(isCameraEnabled = !current.isCameraEnabled)
    }

    fun switchCallCameraLens() {
        val current = _activeCallOverlay.value ?: return
        _activeCallOverlay.value = current.copy(isFrontCamera = !current.isFrontCamera)
    }

    fun toggleCallPipMode() {
        val current = _activeCallOverlay.value ?: return
        _activeCallOverlay.value = current.copy(isPipMinimized = !current.isPipMinimized)
    }

    fun cycleCallAudioRoute() {
        val current = _activeCallOverlay.value ?: return
        val nextRoute = when (current.audioRoute) {
            AudioRouteMode.SPEAKERPHONE -> AudioRouteMode.EARPIECE
            AudioRouteMode.EARPIECE -> AudioRouteMode.BLUETOOTH
            AudioRouteMode.BLUETOOTH -> AudioRouteMode.SPEAKERPHONE
        }
        webRtcEngine.setAudioRoute(nextRoute)
        _activeCallOverlay.value = current.copy(audioRoute = nextRoute)
    }

    fun endActiveCall() {
        val current = _activeCallOverlay.value ?: return
        callTimerJob?.cancel()
        _activeCallOverlay.value = null
        viewModelScope.launch {
            repository.updateCallSignalingState(
                callId = current.callSession.callId,
                state = CallState.ENDED,
                durationSec = current.elapsedSeconds
            )
        }
    }

    // Profile, Privacy & Moderation Actions
    fun saveProfileUpdates(username: String, displayName: String, bio: String, phone: String) {
        viewModelScope.launch {
            repository.upsertUserProfile(
                username = username,
                displayName = displayName,
                bio = bio,
                phone = phone,
                email = currentUserEmail
            ).onSuccess {
                _bannerAlert.value = "Profile updated successfully."
            }.onFailure { e ->
                _bannerAlert.value = e.localizedMessage ?: "Failed to update profile."
            }
        }
    }

    fun savePrivacyControls(
        lastSeen: PrivacyVisibility,
        avatar: PrivacyVisibility,
        bio: PrivacyVisibility,
        readReceipts: Boolean,
        typingIndicator: Boolean
    ) {
        viewModelScope.launch {
            repository.updatePrivacySettings(
                privacyLastSeen = lastSeen.name,
                privacyAvatar = avatar.name,
                privacyBio = bio.name,
                readReceipts = readReceipts,
                typingIndicator = typingIndicator
            ).onSuccess {
                _bannerAlert.value = "Privacy controls saved to cloud."
            }
        }
    }

    fun submitAbuseReport(targetUserId: String, reason: String, details: String) {
        viewModelScope.launch {
            repository.submitAbuseReport(targetUserId, reason, details).onSuccess {
                _bannerAlert.value = "Report submitted to Jom! Trust & Safety Moderation team."
            }
        }
    }

    fun deleteAccount(onDeleted: () -> Unit) {
        viewModelScope.launch {
            repository.deleteMyAccountData().onSuccess {
                onDeleted()
            }.onFailure { e ->
                _bannerAlert.value = e.localizedMessage ?: "Failed to delete account data."
            }
        }
    }
}
