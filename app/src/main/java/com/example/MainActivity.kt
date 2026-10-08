package com.example

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Contacts
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.outlined.Call
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.Contacts
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.authentication.AuthScreen
import com.example.authentication.signOut
import com.example.calls.ActiveWebRtcCallOverlay
import com.example.calls.CallsHistoryScreen
import com.example.calls.ContactsScreen
import com.example.chat.ChatDetailScreen
import com.example.chat.ChatsListScreen
import com.example.data.local.JomDatabase
import com.example.data.repository.JomRepository
import com.example.domain.model.CallType
import com.example.network.RealtimeSocketState
import com.example.settings.SettingsScreen
import com.example.ui.JomMainViewModel
import com.example.ui.MainNavTab
import com.example.ui.components.JomBrandEmblem
import com.example.ui.theme.JomRoyalBlue
import com.example.ui.theme.JomTheme
import com.example.ui.theme.OnlineEmerald
import com.example.ui.theme.WarningAmber
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            JomAppRoot()
        }
    }
}

internal fun FirebaseAuth.authStateFlow(): Flow<FirebaseUser?> = callbackFlow {
    val listener = FirebaseAuth.AuthStateListener { auth ->
        trySend(auth.currentUser)
    }
    addAuthStateListener(listener)
    awaitClose { removeAuthStateListener(listener) }
}

@Composable
fun JomAppRoot(auth: FirebaseAuth = Firebase.auth) {
    val currentUser by auth.authStateFlow().collectAsStateWithLifecycle(initialValue = auth.currentUser)
    val user = currentUser

    if (user == null) {
        JomTheme {
            AuthScreen(
                onAuthSuccess = {}
            )
        }
    } else {
        AuthenticatedJomWorkspace(
            currentUserId = user.uid,
            currentUserEmail = user.email.orEmpty(),
            currentUserDisplayName = user.displayName.orEmpty()
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuthenticatedJomWorkspace(
    currentUserId: String,
    currentUserEmail: String,
    currentUserDisplayName: String
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember { CredentialManager.create(context) }

    // ViewModel keyed by currentUserId and configured with R.string.firestore_database_id at factory boundary
    val viewModel: JomMainViewModel = viewModel(
        key = currentUserId,
        factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY]) {
                    "APPLICATION_KEY missing from CreationExtras"
                }
                val databaseId = app.getString(R.string.firestore_database_id)
                val db = FirebaseFirestore.getInstance(databaseId)
                val dao = JomDatabase.getInstance(app).jomDao()
                JomMainViewModel(
                    repository = JomRepository(db, dao),
                    currentUserId = currentUserId,
                    currentUserEmail = currentUserEmail,
                    currentUserDisplayName = currentUserDisplayName,
                    appContext = app
                )
            }
        }
    )

    val selectedTab by viewModel.selectedTab.collectAsStateWithLifecycle()
    val activeConversation by viewModel.activeConversation.collectAsStateWithLifecycle()
    val activeCallOverlay by viewModel.activeCallOverlay.collectAsStateWithLifecycle()
    val activeUpload by viewModel.activeUpload.collectAsStateWithLifecycle()
    val conversationsState by viewModel.conversationsState.collectAsStateWithLifecycle()
    val contactsState by viewModel.contactsState.collectAsStateWithLifecycle()
    val callsState by viewModel.callsState.collectAsStateWithLifecycle()
    val myProfileState by viewModel.myProfileState.collectAsStateWithLifecycle()
    val activeMessages by viewModel.activeMessages.collectAsStateWithLifecycle()
    val voiceRecordingState by viewModel.voiceRecordingState.collectAsStateWithLifecycle()
    val globalSearchQuery by viewModel.globalSearchQuery.collectAsStateWithLifecycle()
    val inChatSearchQuery by viewModel.inChatSearchQuery.collectAsStateWithLifecycle()
    val socketState by viewModel.socketState.collectAsStateWithLifecycle()
    val bannerAlert by viewModel.bannerAlert.collectAsStateWithLifecycle()
    val isDarkModeOverride by viewModel.isDarkModeOverride.collectAsStateWithLifecycle()
    val notificationsEnabled by viewModel.notificationsEnabled.collectAsStateWithLifecycle()
    val notificationPrivacyMode by viewModel.notificationPrivacyMode.collectAsStateWithLifecycle()
    val pinnedConvIds by viewModel.pinnedConvIds.collectAsStateWithLifecycle()
    val mutedConvIds by viewModel.mutedConvIds.collectAsStateWithLifecycle()

    // Request runtime notification permission on Android 13+ when entering authenticated workspace
    val runtimePermissionsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { _ -> }

    LaunchedEffect(Unit) {
        val perms = buildList {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                add(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
        if (perms.isNotEmpty()) {
            runtimePermissionsLauncher.launch(perms.toTypedArray())
        }
    }

    val useDarkTheme = isDarkModeOverride ?: isSystemInDarkTheme()

    JomTheme(darkTheme = useDarkTheme) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val isExpandedScreen = maxWidth >= 680.dp

            Scaffold(
                contentWindowInsets = WindowInsets.safeDrawing,
                topBar = {
                    if (activeConversation == null || isExpandedScreen) {
                        TopAppBar(
                            title = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    JomBrandEmblem(
                                        modifier = Modifier.size(36.dp),
                                        cornerRadius = 10.dp,
                                        showText = false
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = "Jom!",
                                        style = MaterialTheme.typography.headlineMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            },
                            actions = {
                                // Connection Status Pill
                                val isOffline = socketState == RealtimeSocketState.OFFLINE_QUEUING
                                Surface(
                                    color = if (isOffline) WarningAmber.copy(alpha = 0.2f) else OnlineEmerald.copy(alpha = 0.16f),
                                    shape = RoundedCornerShape(50),
                                    modifier = Modifier.padding(end = 12.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = if (isOffline) Icons.Default.CloudOff else Icons.Default.Wifi,
                                            contentDescription = "Network Connection State",
                                            tint = if (isOffline) WarningAmber else OnlineEmerald,
                                            modifier = Modifier.size(15.dp)
                                        )
                                        Spacer(modifier = Modifier.width(5.dp))
                                        Text(
                                            text = if (isOffline) "Offline Queue" else "Live Sync",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = if (isOffline) WarningAmber else OnlineEmerald
                                        )
                                    }
                                }
                            },
                            colors = TopAppBarDefaults.topAppBarColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            )
                        )
                    }
                },
                bottomBar = {
                    if (!isExpandedScreen && activeConversation == null) {
                        NavigationBar(modifier = Modifier.testTag("bottom_nav_bar")) {
                            NavigationBarItem(
                                selected = selectedTab == MainNavTab.CHATS,
                                onClick = { viewModel.selectTab(MainNavTab.CHATS) },
                                icon = {
                                    Icon(
                                        if (selectedTab == MainNavTab.CHATS) Icons.AutoMirrored.Filled.Chat else Icons.Outlined.ChatBubbleOutline,
                                        contentDescription = "Chats"
                                    )
                                },
                                label = { Text("Chats") },
                                modifier = Modifier.testTag("nav_tab_chats")
                            )
                            NavigationBarItem(
                                selected = selectedTab == MainNavTab.CALLS,
                                onClick = { viewModel.selectTab(MainNavTab.CALLS) },
                                icon = {
                                    Icon(
                                        if (selectedTab == MainNavTab.CALLS) Icons.Filled.Call else Icons.Outlined.Call,
                                        contentDescription = "Calls"
                                    )
                                },
                                label = { Text("Calls") },
                                modifier = Modifier.testTag("nav_tab_calls")
                            )
                            NavigationBarItem(
                                selected = selectedTab == MainNavTab.CONTACTS,
                                onClick = { viewModel.selectTab(MainNavTab.CONTACTS) },
                                icon = {
                                    Icon(
                                        if (selectedTab == MainNavTab.CONTACTS) Icons.Filled.Contacts else Icons.Outlined.Contacts,
                                        contentDescription = "Contacts"
                                    )
                                },
                                label = { Text("Contacts") },
                                modifier = Modifier.testTag("nav_tab_contacts")
                            )
                            NavigationBarItem(
                                selected = selectedTab == MainNavTab.SETTINGS,
                                onClick = { viewModel.selectTab(MainNavTab.SETTINGS) },
                                icon = {
                                    Icon(
                                        if (selectedTab == MainNavTab.SETTINGS) Icons.Filled.Settings else Icons.Outlined.Settings,
                                        contentDescription = "Settings"
                                    )
                                },
                                label = { Text("Settings") },
                                modifier = Modifier.testTag("nav_tab_settings")
                            )
                        }
                    }
                }
            ) { innerPadding ->
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    if (isExpandedScreen) {
                        NavigationRail(modifier = Modifier.fillMaxHeight()) {
                            NavigationRailItem(
                                selected = selectedTab == MainNavTab.CHATS,
                                onClick = {
                                    viewModel.closeActiveConversation()
                                    viewModel.selectTab(MainNavTab.CHATS)
                                },
                                icon = { Icon(Icons.AutoMirrored.Filled.Chat, contentDescription = "Chats") },
                                label = { Text("Chats") }
                            )
                            NavigationRailItem(
                                selected = selectedTab == MainNavTab.CALLS,
                                onClick = {
                                    viewModel.closeActiveConversation()
                                    viewModel.selectTab(MainNavTab.CALLS)
                                },
                                icon = { Icon(Icons.Default.Call, contentDescription = "Calls") },
                                label = { Text("Calls") }
                            )
                            NavigationRailItem(
                                selected = selectedTab == MainNavTab.CONTACTS,
                                onClick = {
                                    viewModel.closeActiveConversation()
                                    viewModel.selectTab(MainNavTab.CONTACTS)
                                },
                                icon = { Icon(Icons.Default.Contacts, contentDescription = "Contacts") },
                                label = { Text("Contacts") }
                            )
                            NavigationRailItem(
                                selected = selectedTab == MainNavTab.SETTINGS,
                                onClick = {
                                    viewModel.closeActiveConversation()
                                    viewModel.selectTab(MainNavTab.SETTINGS)
                                },
                                icon = { Icon(Icons.Default.Settings, contentDescription = "Settings") },
                                label = { Text("Settings") }
                            )
                        }
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        // Dismissible Status / Error Alert Banner
                        AnimatedVisibility(visible = bannerAlert != null) {
                            Surface(
                                color = MaterialTheme.colorScheme.secondaryContainer,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = bannerAlert.orEmpty(),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                                        modifier = Modifier.weight(1f)
                                    )
                                    IconButton(
                                        onClick = { viewModel.dismissBannerAlert() },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(Icons.Default.Close, contentDescription = "Dismiss notification")
                                    }
                                }
                            }
                        }

                        val activeConv = activeConversation
                        if (activeConv != null) {
                            ChatDetailScreen(
                                conversation = activeConv,
                                messages = activeMessages,
                                currentUserId = currentUserId,
                                inChatSearchQuery = inChatSearchQuery,
                                onInChatSearchChange = viewModel::setInChatSearchQuery,
                                voiceRecordingState = voiceRecordingState,
                                activeUpload = activeUpload,
                                onBack = viewModel::closeActiveConversation,
                                onSendMessage = viewModel::sendChatMessage,
                                onCancelUpload = viewModel::cancelActiveMediaUpload,
                                onEditMessage = viewModel::editMessage,
                                onDeleteMessage = viewModel::deleteMessageForEveryone,
                                onToggleReaction = viewModel::toggleMessageReaction,
                                onTogglePin = viewModel::toggleMessagePin,
                                onToggleStar = viewModel::toggleMessageStar,
                                onStartVoiceNote = {
                                    runtimePermissionsLauncher.launch(arrayOf(Manifest.permission.RECORD_AUDIO))
                                    viewModel.startVoiceNoteRecording()
                                },
                                onLockVoiceNote = viewModel::lockVoiceNoteRecording,
                                onPauseResumeVoiceNote = viewModel::togglePauseVoiceNoteRecording,
                                onStopAndPreviewVoiceNote = viewModel::stopAndPreviewVoiceNote,
                                onCancelVoiceNote = viewModel::cancelVoiceNoteRecording,
                                onTogglePreviewVoicePlayback = viewModel::togglePreviewVoiceNotePlayback,
                                onCycleVoicePlaybackSpeed = viewModel::cycleVoiceNotePlaybackSpeed,
                                onSendRecordedVoiceNote = viewModel::sendRecordedVoiceNote,
                                onStartVoiceCall = {
                                    runtimePermissionsLauncher.launch(arrayOf(Manifest.permission.RECORD_AUDIO))
                                    viewModel.initiateCall(
                                        calleeId = activeConv.conversationId,
                                        calleeName = activeConv.title,
                                        callType = CallType.VOICE
                                    )
                                },
                                onStartVideoCall = {
                                    runtimePermissionsLauncher.launch(
                                        arrayOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO)
                                    )
                                    viewModel.initiateCall(
                                        calleeId = activeConv.conversationId,
                                        calleeName = activeConv.title,
                                        callType = CallType.VIDEO
                                    )
                                },
                                onUpdateGroupSettings = { title, desc, onlyAdminSend, onlyAdminEdit, participants, admins ->
                                    viewModel.updateGroupSettings(
                                        conversationId = activeConv.conversationId,
                                        title = title,
                                        description = desc,
                                        onlyAdminsCanSend = onlyAdminSend,
                                        onlyAdminsCanEditInfo = onlyAdminEdit,
                                        participantIds = participants,
                                        adminIds = admins
                                    )
                                },
                                onDeleteOrLeaveConversation = {
                                    viewModel.leaveOrDeleteConversation(activeConv.conversationId)
                                }
                            )
                        } else {
                            when (selectedTab) {
                                MainNavTab.CHATS -> {
                                    ChatsListScreen(
                                        conversationsState = conversationsState,
                                        pinnedConvIds = pinnedConvIds,
                                        mutedConvIds = mutedConvIds,
                                        searchQuery = globalSearchQuery,
                                        onSearchQueryChange = viewModel::setGlobalSearchQuery,
                                        onOpenConversation = viewModel::openConversation,
                                        onCreateConversationOrGroup = { title, members, isGroup, desc ->
                                            viewModel.createNewConversationOrGroup(title, members, isGroup, desc)
                                        },
                                        onTogglePin = viewModel::togglePinConversation,
                                        onToggleMute = viewModel::toggleMuteConversation
                                    )
                                }
                                MainNavTab.CALLS -> {
                                    CallsHistoryScreen(
                                        callsState = callsState,
                                        currentUserId = currentUserId,
                                        onStartVoiceCall = { peerId, peerName ->
                                            runtimePermissionsLauncher.launch(arrayOf(Manifest.permission.RECORD_AUDIO))
                                            viewModel.initiateCall(peerId, peerName, CallType.VOICE)
                                        },
                                        onStartVideoCall = { peerId, peerName ->
                                            runtimePermissionsLauncher.launch(
                                                arrayOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO)
                                            )
                                            viewModel.initiateCall(peerId, peerName, CallType.VIDEO)
                                        }
                                    )
                                }
                                MainNavTab.CONTACTS -> {
                                    ContactsScreen(
                                        contactsState = contactsState,
                                        onAddContact = viewModel::addNewContact,
                                        onStartChatWithContact = { contact ->
                                            viewModel.createNewConversationOrGroup(
                                                title = contact.displayName,
                                                participantHandles = listOf(contact.username),
                                                isGroup = false,
                                                description = contact.bio
                                            )
                                        },
                                        onVoiceCallContact = { contact ->
                                            viewModel.initiateCall(contact.contactUserId, contact.displayName, CallType.VOICE)
                                        },
                                        onVideoCallContact = { contact ->
                                            viewModel.initiateCall(contact.contactUserId, contact.displayName, CallType.VIDEO)
                                        },
                                        onToggleBlockContact = viewModel::toggleBlockContact,
                                        onReportContact = { contact, reason ->
                                            viewModel.submitAbuseReport(contact.contactUserId, reason, "Reported via Contacts")
                                        }
                                    )
                                }
                                MainNavTab.SETTINGS -> {
                                    SettingsScreen(
                                        myProfileState = myProfileState,
                                        contactsState = contactsState,
                                        currentUserEmail = currentUserEmail,
                                        socketState = socketState,
                                        isDarkModeOverride = isDarkModeOverride,
                                        notificationsEnabled = notificationsEnabled,
                                        notificationPrivacyMode = notificationPrivacyMode,
                                        onSaveProfile = viewModel::saveProfileUpdates,
                                        onSavePrivacy = viewModel::savePrivacyControls,
                                        onToggleDarkMode = viewModel::setDarkModeOverride,
                                        onToggleNotifications = viewModel::setNotificationsEnabled,
                                        onSelectNotificationPrivacy = viewModel::setNotificationPrivacyMode,
                                        onToggleOfflineSimulation = viewModel::toggleSimulatedOfflineMode,
                                        onUnblockContact = viewModel::toggleBlockContact,
                                        onSignOut = {
                                            signOut(context, credentialManager, {}, scope)
                                        },
                                        onDeleteAccount = {
                                            viewModel.deleteAccount {
                                                signOut(context, credentialManager, {}, scope)
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Active WebRTC Call Overlay (Full-screen or Picture-in-Picture mini card)
            val callOverlay = activeCallOverlay
            if (callOverlay != null) {
                ActiveWebRtcCallOverlay(
                    overlayState = callOverlay,
                    onToggleMute = viewModel::toggleCallMute,
                    onToggleCamera = viewModel::toggleCallCamera,
                    onSwitchCameraLens = viewModel::switchCallCameraLens,
                    onCycleAudioRoute = viewModel::cycleCallAudioRoute,
                    onTogglePip = viewModel::toggleCallPipMode,
                    onEndCall = viewModel::endActiveCall
                )
            }
        }
    }
}
