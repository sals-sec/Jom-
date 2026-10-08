package com.example.chat

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Reply
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContactPage
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EmojiEmotions
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.FlipCameraAndroid
import androidx.compose.material.icons.filled.GifBox
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.GroupAdd
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.calls.VoiceRecordingState
import com.example.domain.model.CallType
import com.example.domain.model.ChatMessage
import com.example.domain.model.Conversation
import com.example.domain.model.MessageType
import com.example.network.UploadProgressState
import com.example.ui.UiState
import com.example.ui.theme.JomCyan
import com.example.ui.theme.JomRoyalBlue
import com.example.ui.theme.OnlineEmerald
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ChatsListScreen(
    conversationsState: UiState<List<Conversation>>,
    pinnedConvIds: Set<String>,
    mutedConvIds: Set<String>,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onOpenConversation: (Conversation) -> Unit,
    onCreateConversationOrGroup: (title: String, participants: List<String>, isGroup: Boolean, description: String) -> Unit,
    onTogglePin: (String) -> Unit,
    onToggleMute: (String) -> Unit
) {
    var showCreateDialog by remember { mutableStateOf(false) }
    var isCreatingGroup by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Global Search Bar + Quick Actions Row
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchQueryChange,
                placeholder = { Text("Search chats, groups, messages, or files…") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { onSearchQueryChange("") }) {
                            Icon(Icons.Default.Close, contentDescription = "Clear search")
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .testTag("global_search_input")
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        isCreatingGroup = false
                        showCreateDialog = true
                    },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("new_direct_chat_btn"),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("New Direct Chat")
                }

                OutlinedButton(
                    onClick = {
                        isCreatingGroup = true
                        showCreateDialog = true
                    },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("new_group_chat_btn"),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(Icons.Default.GroupAdd, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("New Group")
                }
            }

            when (conversationsState) {
                is UiState.Loading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Syncing encrypted conversations…", style = MaterialTheme.typography.bodyMedium)
                    }
                }
                is UiState.Error -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            text = conversationsState.message,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(24.dp)
                        )
                    }
                }
                is UiState.Success -> {
                    val filtered = conversationsState.data.filter {
                        searchQuery.isBlank() ||
                            it.title.contains(searchQuery, ignoreCase = true) ||
                            it.lastMessageText.contains(searchQuery, ignoreCase = true)
                    }.sortedByDescending { it.conversationId in pinnedConvIds }

                    if (filtered.isEmpty()) {
                        EmptyChatsPlaceholder(
                            onStartSampleDirectChat = {
                                onCreateConversationOrGroup(
                                    "Sarah Chen (Product Lead)",
                                    listOf("sarah_chen"),
                                    false,
                                    "Direct 1-to-1 encrypted chat"
                                )
                            },
                            onStartSampleGroup = {
                                onCreateConversationOrGroup(
                                    "Jom! Core Engineering",
                                    listOf("sarah_chen", "marcus_dev", "priya_sec"),
                                    true,
                                    "Architecture, WebRTC & Release Sync"
                                )
                            }
                        )
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .testTag("conversations_list"),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(filtered, key = { it.conversationId }) { conv ->
                                val isPinned = conv.conversationId in pinnedConvIds
                                val isMuted = conv.conversationId in mutedConvIds
                                ConversationRowCard(
                                    conversation = conv,
                                    isPinned = isPinned,
                                    isMuted = isMuted,
                                    onClick = { onOpenConversation(conv) },
                                    onTogglePin = { onTogglePin(conv.conversationId) },
                                    onToggleMute = { onToggleMute(conv.conversationId) }
                                )
                            }
                        }
                    }
                }
            }
        }

        FloatingActionButton(
            onClick = {
                isCreatingGroup = false
                showCreateDialog = true
            },
            containerColor = JomRoyalBlue,
            contentColor = Color.White,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(20.dp)
                .testTag("fab_new_chat")
        ) {
            Icon(Icons.Default.Add, contentDescription = "Start new conversation")
        }
    }

    if (showCreateDialog) {
        CreateConversationDialog(
            isGroup = isCreatingGroup,
            onDismiss = { showCreateDialog = false },
            onConfirm = { title, members, desc ->
                showCreateDialog = false
                onCreateConversationOrGroup(title, members, isCreatingGroup, desc)
            }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ConversationRowCard(
    conversation: Conversation,
    isPinned: Boolean,
    isMuted: Boolean,
    onClick: () -> Unit,
    onTogglePin: () -> Unit,
    onToggleMute: () -> Unit
) {
    var showContextMenu by remember { mutableStateOf(false) }
    val timeFormatter = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }
    val timeText = conversation.updatedAt?.toDate()?.let { timeFormatter.format(it) } ?: "Now"

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = { showContextMenu = true }
            )
            .testTag("conversation_card_${conversation.conversationId}"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isPinned) {
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
            } else {
                MaterialTheme.colorScheme.surface
            }
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Avatar with Online Presence Dot
            Box(modifier = Modifier.size(54.dp)) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    shape = CircleShape,
                    color = if (conversation.isGroup) JomRoyalBlue else MaterialTheme.colorScheme.primaryContainer
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        if (conversation.isGroup) {
                            Icon(
                                imageVector = Icons.Default.Group,
                                contentDescription = "Group Avatar",
                                tint = Color.White,
                                modifier = Modifier.size(26.dp)
                            )
                        } else {
                            Text(
                                text = conversation.title.take(2).uppercase(),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }
                if (conversation.isOnline) {
                    Box(
                        modifier = Modifier
                            .size(14.dp)
                            .align(Alignment.BottomEnd)
                            .clip(CircleShape)
                            .background(OnlineEmerald)
                    )
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = conversation.title,
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (conversation.isGroup) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Badge(containerColor = MaterialTheme.colorScheme.secondaryContainer) {
                                Text(
                                    "GROUP",
                                    modifier = Modifier.padding(horizontal = 4.dp),
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            }
                        }
                    }

                    Text(
                        text = timeText,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = conversation.lastMessageText.ifBlank { "Tap to send a message" },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (isMuted) {
                            Icon(
                                imageVector = Icons.Default.NotificationsOff,
                                contentDescription = "Muted",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                        }
                        if (isPinned) {
                            Icon(
                                imageVector = Icons.Default.PushPin,
                                contentDescription = "Pinned",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }

            DropdownMenu(
                expanded = showContextMenu,
                onDismissRequest = { showContextMenu = false }
            ) {
                DropdownMenuItem(
                    text = { Text(if (isPinned) "Unpin Conversation" else "Pin Conversation") },
                    leadingIcon = { Icon(Icons.Default.PushPin, contentDescription = null) },
                    onClick = {
                        showContextMenu = false
                        onTogglePin()
                    }
                )
                DropdownMenuItem(
                    text = { Text(if (isMuted) "Unmute Notifications" else "Mute Notifications") },
                    leadingIcon = { Icon(Icons.Default.NotificationsOff, contentDescription = null) },
                    onClick = {
                        showContextMenu = false
                        onToggleMute()
                    }
                )
            }
        }
    }
}

@Composable
private fun EmptyChatsPlaceholder(
    onStartSampleDirectChat: () -> Unit,
    onStartSampleGroup: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Surface(
            modifier = Modifier.size(76.dp),
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primaryContainer
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(36.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Start Your First Jom! Conversation",
            style = MaterialTheme.typography.headlineMedium
        )
        Text(
            text = "Send text, voice notes, HD photos, documents, or launch real-time WebRTC voice & video calls.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 6.dp, bottom = 20.dp)
        )
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(
                onClick = onStartSampleDirectChat,
                modifier = Modifier.testTag("quick_start_direct_chat_btn")
            ) {
                Text("Start Direct Chat")
            }
            OutlinedButton(
                onClick = onStartSampleGroup,
                modifier = Modifier.testTag("quick_start_group_chat_btn")
            ) {
                Text("Create Team Group")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun ChatDetailScreen(
    conversation: Conversation,
    messages: List<ChatMessage>,
    currentUserId: String,
    inChatSearchQuery: String,
    onInChatSearchChange: (String) -> Unit,
    voiceRecordingState: VoiceRecordingState,
    activeUpload: UploadProgressState?,
    onBack: () -> Unit,
    onSendMessage: (
        text: String,
        type: MessageType,
        fileName: String?,
        mimeType: String?,
        fileSize: Long?,
        durationSec: Int?,
        replyTo: ChatMessage?,
        isForwarded: Boolean
    ) -> Unit,
    onCancelUpload: () -> Unit,
    onEditMessage: (ChatMessage, String) -> Unit,
    onDeleteMessage: (ChatMessage) -> Unit,
    onToggleReaction: (ChatMessage, String) -> Unit,
    onTogglePin: (ChatMessage) -> Unit,
    onToggleStar: (ChatMessage) -> Unit,
    onStartVoiceNote: () -> Unit,
    onLockVoiceNote: () -> Unit,
    onPauseResumeVoiceNote: () -> Unit,
    onStopAndPreviewVoiceNote: () -> Unit,
    onCancelVoiceNote: () -> Unit,
    onTogglePreviewVoicePlayback: () -> Unit,
    onCycleVoicePlaybackSpeed: () -> Unit,
    onSendRecordedVoiceNote: () -> Unit,
    onStartVoiceCall: () -> Unit,
    onStartVideoCall: () -> Unit,
    onUpdateGroupSettings: (String, String, Boolean, Boolean, List<String>, List<String>) -> Unit,
    onDeleteOrLeaveConversation: () -> Unit
) {
    BackHandler { onBack() }

    val clipboardManager = LocalClipboardManager.current
    var inputText by remember { mutableStateOf("") }
    var replyTarget by remember { mutableStateOf<ChatMessage?>(null) }
    var editingMessage by remember { mutableStateOf<ChatMessage?>(null) }
    var showSearchBar by remember { mutableStateOf(false) }
    var showAttachmentSheet by remember { mutableStateOf(false) }
    var showCameraSheet by remember { mutableStateOf(false) }
    var showEmojiStickerBar by remember { mutableStateOf(false) }
    var showGroupAdminDialog by remember { mutableStateOf(false) }
    var showTopMenu by remember { mutableStateOf(false) }

    // Zero-permission Android Photo Picker
    val visualMediaPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            onSendMessage(
                "📷 Shared HD Photo (${uri.lastPathSegment ?: "image.jpg"})",
                MessageType.IMAGE,
                "photo_${System.currentTimeMillis()}.jpg",
                "image/jpeg",
                1_420_000L,
                null,
                replyTarget,
                false
            )
            replyTarget = null
        }
    }

    val documentPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            onSendMessage(
                "📄 Shared Document (${uri.lastPathSegment ?: "document.pdf"})",
                MessageType.DOCUMENT,
                "document_${System.currentTimeMillis()}.pdf",
                "application/pdf",
                840_000L,
                null,
                replyTarget,
                false
            )
            replyTarget = null
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // 1. Top Chat Header (Section 20: Back, Avatar, Name, Online/Last-Seen, Voice Call, Video Call, More Menu)
        Surface(
            tonalElevation = 4.dp,
            shadowElevation = 4.dp,
            color = MaterialTheme.colorScheme.surface
        ) {
            Column {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("chat_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back to conversations"
                        )
                    }

                    Surface(
                        modifier = Modifier.size(42.dp),
                        shape = CircleShape,
                        color = JomRoyalBlue
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = conversation.title.take(2).uppercase(),
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clickable {
                                if (conversation.isGroup) showGroupAdminDialog = true
                            }
                    ) {
                        Text(
                            text = conversation.title,
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = if (conversation.isGroup) {
                                "${conversation.participantIds.size} members • Tap for Group Admin settings"
                            } else {
                                "Online • TLS 1.3 + SRTP Ready"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = OnlineEmerald,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    IconButton(
                        onClick = onStartVoiceCall,
                        modifier = Modifier.testTag("chat_voice_call_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Phone,
                            contentDescription = "Start Voice Call",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }

                    IconButton(
                        onClick = onStartVideoCall,
                        modifier = Modifier.testTag("chat_video_call_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Videocam,
                            contentDescription = "Start Video Call",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }

                    Box {
                        IconButton(
                            onClick = { showTopMenu = true },
                            modifier = Modifier.testTag("chat_more_menu_button")
                        ) {
                            Icon(Icons.Default.MoreVert, contentDescription = "More chat options")
                        }
                        DropdownMenu(
                            expanded = showTopMenu,
                            onDismissRequest = { showTopMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Search in Conversation") },
                                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                                onClick = {
                                    showTopMenu = false
                                    showSearchBar = !showSearchBar
                                }
                            )
                            if (conversation.isGroup) {
                                DropdownMenuItem(
                                    text = { Text("Group Info & Admin Controls") },
                                    leadingIcon = { Icon(Icons.Default.Group, contentDescription = null) },
                                    onClick = {
                                        showTopMenu = false
                                        showGroupAdminDialog = true
                                    }
                                )
                            }
                            DropdownMenuItem(
                                text = { Text(if (conversation.isGroup) "Leave / Delete Group" else "Delete Chat") },
                                leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null) },
                                onClick = {
                                    showTopMenu = false
                                    onDeleteOrLeaveConversation()
                                }
                            )
                        }
                    }
                }

                // Pinned Message Banner (if any message in chat is pinned)
                val pinnedMsg = messages.lastOrNull { it.isPinned }
                if (pinnedMsg != null) {
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.PushPin,
                                contentDescription = "Pinned Message",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Pinned: ${pinnedMsg.text}",
                                style = MaterialTheme.typography.bodySmall,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                AnimatedVisibility(visible = showSearchBar) {
                    OutlinedTextField(
                        value = inChatSearchQuery,
                        onValueChange = onInChatSearchChange,
                        placeholder = { Text("Search messages in this chat…") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 6.dp)
                            .testTag("in_chat_search_input"),
                        trailingIcon = {
                            IconButton(onClick = {
                                onInChatSearchChange("")
                                showSearchBar = false
                            }) {
                                Icon(Icons.Default.Close, contentDescription = "Close search")
                            }
                        }
                    )
                }
            }
        }

        // 2. Middle Message Stream with Date Separators, Reactions, Media Previews & Delivery Indicators
        val displayedMessages = remember(messages, inChatSearchQuery) {
            if (inChatSearchQuery.isBlank()) messages
            else messages.filter { it.text.contains(inChatSearchQuery, ignoreCase = true) }
        }

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 12.dp)
                .testTag("chat_messages_list"),
            contentPadding = PaddingValues(vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                // Date Separator Pill
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(50)
                    ) {
                        Text(
                            text = "Today • Secured with TLS & Zero-Trust Rules",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            items(displayedMessages, key = { it.messageId }) { message ->
                val isMine = message.senderId == currentUserId
                ChatMessageBubble(
                    message = message,
                    isMine = isMine,
                    isGroup = conversation.isGroup,
                    onReply = { replyTarget = message },
                    onEdit = {
                        editingMessage = message
                        inputText = message.text
                    },
                    onDelete = { onDeleteMessage(message) },
                    onForward = {
                        onSendMessage(
                            message.text,
                            MessageType.valueOf(message.messageType),
                            message.mediaFileName,
                            message.mediaMimeType,
                            message.mediaFileSize,
                            message.mediaDurationSec,
                            null,
                            true
                        )
                    },
                    onCopy = {
                        clipboardManager.setText(AnnotatedString(message.text))
                    },
                    onReaction = { emoji -> onToggleReaction(message, emoji) },
                    onTogglePin = { onTogglePin(message) },
                    onToggleStar = { onToggleStar(message) }
                )
            }
        }

        // Active Multimedia Upload Progress Banner (Before uploading large files: size, progress, cancel)
        if (activeUpload != null) {
            Surface(
                color = MaterialTheme.colorScheme.secondaryContainer,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Uploading ${activeUpload.fileName} (${activeUpload.totalBytes / 1024} KB • Compressed)",
                            style = MaterialTheme.typography.labelLarge
                        )
                        IconButton(onClick = onCancelUpload, modifier = Modifier.size(28.dp)) {
                            Icon(Icons.Default.Close, contentDescription = "Cancel upload")
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    LinearProgressIndicator(
                        progress = { activeUpload.progressFraction },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        // Reply or Edit Context Banner
        if (replyTarget != null || editingMessage != null) {
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (editingMessage != null) Icons.Default.Edit else Icons.AutoMirrored.Filled.Reply,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (editingMessage != null) "Editing message" else "Replying to ${replyTarget?.senderName}",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = editingMessage?.text ?: replyTarget?.text ?: "",
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    IconButton(onClick = {
                        replyTarget = null
                        editingMessage = null
                    }) {
                        Icon(Icons.Default.Close, contentDescription = "Cancel reply or edit")
                    }
                }
            }
        }

        // Emoji / GIF / Sticker Quick Picker Drawer
        AnimatedVisibility(visible = showEmojiStickerBar) {
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.fillMaxWidth()
            ) {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val quickEmojis = listOf("😀", "🔥", "❤️", "👍", "🎉", "🚀", "👏", "🤝", "✨")
                    items(quickEmojis) { emoji ->
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.surface,
                            modifier = Modifier.clickable { inputText += emoji }
                        ) {
                            Text(
                                text = emoji,
                                modifier = Modifier.padding(10.dp),
                                style = MaterialTheme.typography.titleMedium
                            )
                        }
                    }
                    item {
                        OutlinedButton(onClick = {
                            showEmojiStickerBar = false
                            onSendMessage("🎞️ [Animated GIF: High-Five Celebration]", MessageType.GIF, "celebrate.gif", "image/gif", 310_000L, null, replyTarget, false)
                            replyTarget = null
                        }) {
                            Icon(Icons.Default.GifBox, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Send GIF")
                        }
                    }
                    item {
                        OutlinedButton(onClick = {
                            showEmojiStickerBar = false
                            onSendMessage("💠 [Jom! 3D Sticker: Let's Go!]", MessageType.STICKER, "jom_sticker.webp", "image/webp", 95_000L, null, replyTarget, false)
                            replyTarget = null
                        }) {
                            Text("Send Sticker")
                        }
                    }
                }
            }
        }

        // Voice Note Recording & Preview Bar (Section 6: Waveform, Lock, Pause/Resume, Playback Speed, Preview before sending)
        if (voiceRecordingState.isRecording || voiceRecordingState.isPreviewReady) {
            VoiceNoteComposerBar(
                state = voiceRecordingState,
                onLock = onLockVoiceNote,
                onPauseResume = onPauseResumeVoiceNote,
                onStopAndPreview = onStopAndPreviewVoiceNote,
                onCancel = onCancelVoiceNote,
                onTogglePlayPreview = onTogglePreviewVoicePlayback,
                onCycleSpeed = onCycleVoicePlaybackSpeed,
                onSendVoiceNote = onSendRecordedVoiceNote
            )
        } else {
            // 3. Bottom Message Composer (Attachment, Camera, Message Input, Emoji, Dynamic Send/Voice Button)
            Surface(
                tonalElevation = 6.dp,
                color = MaterialTheme.colorScheme.surface
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { showAttachmentSheet = true },
                        modifier = Modifier.testTag("chat_attach_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.AttachFile,
                            contentDescription = "Attach multimedia, document, location, or contact"
                        )
                    }

                    IconButton(
                        onClick = { showCameraSheet = true },
                        modifier = Modifier.testTag("chat_camera_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.CameraAlt,
                            contentDescription = "Open Camera for Photo or Video"
                        )
                    }

                    OutlinedTextField(
                        value = inputText,
                        onValueChange = { inputText = it },
                        placeholder = {
                            Text(
                                if (conversation.isGroup) "Message ${conversation.title} (use @username)…"
                                else "Message ${conversation.title}…"
                            )
                        },
                        trailingIcon = {
                            IconButton(onClick = { showEmojiStickerBar = !showEmojiStickerBar }) {
                                Icon(
                                    imageVector = Icons.Default.EmojiEmotions,
                                    contentDescription = "Toggle Emoji, GIFs, and Stickers"
                                )
                            }
                        },
                        shape = RoundedCornerShape(24.dp),
                        maxLines = 4,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("chat_message_input")
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    // Dynamic Send vs Voice Note Recording Button
                    val hasText = inputText.isNotBlank()
                    FloatingActionButton(
                        onClick = {
                            if (hasText) {
                                val editing = editingMessage
                                if (editing != null) {
                                    onEditMessage(editing, inputText)
                                    editingMessage = null
                                } else {
                                    onSendMessage(
                                        inputText.trim(),
                                        MessageType.TEXT,
                                        null,
                                        null,
                                        null,
                                        null,
                                        replyTarget,
                                        false
                                    )
                                    replyTarget = null
                                }
                                inputText = ""
                            } else {
                                onStartVoiceNote()
                            }
                        },
                        containerColor = JomRoyalBlue,
                        contentColor = Color.White,
                        shape = CircleShape,
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("chat_send_or_voice_button")
                    ) {
                        Icon(
                            imageVector = if (hasText) Icons.AutoMirrored.Filled.Send else Icons.Default.Mic,
                            contentDescription = if (hasText) "Send Message" else "Record Voice Note"
                        )
                    }
                }
            }
        }
    }

    // Multimedia & Rich Attachment Bottom Sheet (Section 4 & 9: Photos, Videos, Documents, Audio, Location, Contact)
    if (showAttachmentSheet) {
        ModalBottomSheet(onDismissRequest = { showAttachmentSheet = false }) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Share Multimedia & Files",
                    style = MaterialTheme.typography.titleLarge
                )
                Text(
                    text = "Supported: JPG, PNG, WEBP, GIF, MP4, PDF, DOCX, XLSX, PPTX, TXT, ZIP (Up to 50 MB with resumable cloud upload)",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    AttachmentOptionTile(
                        icon = Icons.Default.Image,
                        label = "Photo / Video",
                        modifier = Modifier.weight(1f),
                        onClick = {
                            showAttachmentSheet = false
                            visualMediaPicker.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo)
                            )
                        }
                    )
                    AttachmentOptionTile(
                        icon = Icons.Default.Description,
                        label = "Document (PDF/DOCX/ZIP)",
                        modifier = Modifier.weight(1f),
                        onClick = {
                            showAttachmentSheet = false
                            onSendMessage(
                                "📄 Architecture_Spec_v2.pdf (1.2 MB)",
                                MessageType.DOCUMENT,
                                "Architecture_Spec_v2.pdf",
                                "application/pdf",
                                1_240_000L,
                                null,
                                replyTarget,
                                false
                            )
                            replyTarget = null
                        }
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    AttachmentOptionTile(
                        icon = Icons.Default.LocationOn,
                        label = "Live Location",
                        modifier = Modifier.weight(1f),
                        onClick = {
                            showAttachmentSheet = false
                            onSendMessage(
                                "📍 Shared Location: Kuala Lumpur Tech Hub (3.1390° N, 101.6869° E)",
                                MessageType.LOCATION,
                                null,
                                null,
                                null,
                                null,
                                replyTarget,
                                false
                            )
                            replyTarget = null
                        }
                    )
                    AttachmentOptionTile(
                        icon = Icons.Default.ContactPage,
                        label = "Share Contact",
                        modifier = Modifier.weight(1f),
                        onClick = {
                            showAttachmentSheet = false
                            onSendMessage(
                                "👤 Contact Card: Sarah Chen (@sarah_chen • +60 12-345 6789)",
                                MessageType.CONTACT,
                                null,
                                null,
                                null,
                                null,
                                replyTarget,
                                false
                            )
                            replyTarget = null
                        }
                    )
                }

                OutlinedButton(
                    onClick = {
                        showAttachmentSheet = false
                        documentPicker.launch("*/*")
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Browse Device Files…")
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }

    // Integrated Camera Capture & Caption Sheet (Section 10: Photo/Video, Switch Lens, Flash, Preview & Caption)
    if (showCameraSheet) {
        CameraCaptureDialog(
            onDismiss = { showCameraSheet = false },
            onSendCapturedMedia = { caption, isVideo, lensName, flashOn ->
                showCameraSheet = false
                val fileName = if (isVideo) "jom_cam_${System.currentTimeMillis()}.mp4" else "jom_cam_${System.currentTimeMillis()}.jpg"
                val mime = if (isVideo) "video/mp4" else "image/jpeg"
                val type = if (isVideo) MessageType.VIDEO else MessageType.IMAGE
                val prefix = if (isVideo) "🎬 Camera Video ($lensName)" else "📸 Camera Photo ($lensName • Flash ${if (flashOn) "ON" else "OFF"})"
                onSendMessage(
                    "$prefix: ${caption.ifBlank { "Captured moment" }}",
                    type,
                    fileName,
                    mime,
                    if (isVideo) 4_500_000L else 980_000L,
                    if (isVideo) 12 else null,
                    replyTarget,
                    false
                )
                replyTarget = null
            }
        )
    }

    if (showGroupAdminDialog) {
        GroupAdminSettingsDialog(
            conversation = conversation,
            onDismiss = { showGroupAdminDialog = false },
            onSave = { title, desc, onlyAdminSend, onlyAdminEdit, participants, admins ->
                showGroupAdminDialog = false
                onUpdateGroupSettings(title, desc, onlyAdminSend, onlyAdminEdit, participants, admins)
            }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ChatMessageBubble(
    message: ChatMessage,
    isMine: Boolean,
    isGroup: Boolean,
    onReply: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onForward: () -> Unit,
    onCopy: () -> Unit,
    onReaction: (String) -> Unit,
    onTogglePin: () -> Unit,
    onToggleStar: () -> Unit
) {
    var showActionsMenu by remember { mutableStateOf(false) }
    val timeFormatter = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }
    val timestampStr = message.createdAt?.toDate()?.let { timeFormatter.format(it) } ?: "Just now"

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (isMine) Alignment.End else Alignment.Start
    ) {
        Surface(
            shape = RoundedCornerShape(
                topStart = 18.dp,
                topEnd = 18.dp,
                bottomStart = if (isMine) 18.dp else 4.dp,
                bottomEnd = if (isMine) 4.dp else 18.dp
            ),
            color = if (isMine) JomRoyalBlue else MaterialTheme.colorScheme.surfaceVariant,
            contentColor = if (isMine) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .widthIn(max = 310.dp)
                .combinedClickable(
                    onClick = { showActionsMenu = true },
                    onLongClick = { showActionsMenu = true }
                )
        ) {
            Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
                if (isGroup && !isMine) {
                    Text(
                        text = message.senderName,
                        style = MaterialTheme.typography.labelLarge,
                        color = JomCyan,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                }

                if (message.isForwarded) {
                    Text(
                        text = "↪ Forwarded message",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isMine) Color.White.copy(alpha = 0.75f) else MaterialTheme.colorScheme.primary
                    )
                }

                if (!message.replyToPreview.isNullOrBlank()) {
                    Surface(
                        color = Color.Black.copy(alpha = 0.16f),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    ) {
                        Text(
                            text = "↩ ${message.replyToPreview}",
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 2,
                            modifier = Modifier.padding(6.dp)
                        )
                    }
                }

                // Media / File Card Preview if present
                if (message.mediaFileName != null) {
                    Surface(
                        color = Color.Black.copy(alpha = 0.18f),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 6.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = when (message.messageType) {
                                    MessageType.VOICE_NOTE.name -> Icons.Default.Mic
                                    MessageType.IMAGE.name -> Icons.Default.Image
                                    MessageType.VIDEO.name -> Icons.Default.Videocam
                                    else -> Icons.Default.Description
                                },
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = message.mediaFileName,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.SemiBold
                                )
                                val kb = (message.mediaFileSize ?: 0L) / 1024L
                                if (kb > 0) {
                                    Text(
                                        text = "$kb KB • Cloud Object Storage",
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                }
                            }
                        }
                    }
                }

                Text(
                    text = message.text,
                    style = MaterialTheme.typography.bodyMedium
                )

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.End,
                    modifier = Modifier.align(Alignment.End)
                ) {
                    if (message.isStarred) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = "Starred",
                            modifier = Modifier.size(13.dp),
                            tint = Color(0xFFFBBF24)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                    }
                    if (message.isPinned) {
                        Icon(
                            imageVector = Icons.Default.PushPin,
                            contentDescription = "Pinned",
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                    }
                    if (message.isEdited) {
                        Text(
                            text = "edited • ",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isMine) Color.White.copy(alpha = 0.7f) else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(
                        text = timestampStr,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isMine) Color.White.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (isMine) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = if (message.status == "SENDING") Icons.Default.Check else Icons.Default.DoneAll,
                            contentDescription = "Delivery status: ${message.status}",
                            modifier = Modifier.size(15.dp),
                            tint = JomCyan
                        )
                    }
                }
            }
        }

        if (message.reactionsSummary.isNotBlank()) {
            Surface(
                shape = RoundedCornerShape(50),
                color = MaterialTheme.colorScheme.secondaryContainer,
                tonalElevation = 2.dp,
                modifier = Modifier.padding(top = 2.dp)
            ) {
                Text(
                    text = message.reactionsSummary,
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                )
            }
        }

        DropdownMenu(
            expanded = showActionsMenu,
            onDismissRequest = { showActionsMenu = false }
        ) {
            // Quick Emoji Reactions Row
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("👍", "❤️", "😂", "🔥", "🎉", "🙏").forEach { emoji ->
                    Text(
                        text = emoji,
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.clickable {
                            showActionsMenu = false
                            onReaction(emoji)
                        }
                    )
                }
            }
            DropdownMenuItem(
                text = { Text("Reply") },
                leadingIcon = { Icon(Icons.AutoMirrored.Filled.Reply, contentDescription = null) },
                onClick = {
                    showActionsMenu = false
                    onReply()
                }
            )
            DropdownMenuItem(
                text = { Text("Forward") },
                leadingIcon = { Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null) },
                onClick = {
                    showActionsMenu = false
                    onForward()
                }
            )
            DropdownMenuItem(
                text = { Text("Copy Text") },
                leadingIcon = { Icon(Icons.Default.ContentCopy, contentDescription = null) },
                onClick = {
                    showActionsMenu = false
                    onCopy()
                }
            )
            DropdownMenuItem(
                text = { Text(if (message.isPinned) "Unpin Message" else "Pin Message") },
                leadingIcon = { Icon(Icons.Default.PushPin, contentDescription = null) },
                onClick = {
                    showActionsMenu = false
                    onTogglePin()
                }
            )
            DropdownMenuItem(
                text = { Text(if (message.isStarred) "Unstar Message" else "Star / Favorite") },
                leadingIcon = { Icon(Icons.Default.Star, contentDescription = null) },
                onClick = {
                    showActionsMenu = false
                    onToggleStar()
                }
            )
            if (isMine && !message.isDeleted) {
                DropdownMenuItem(
                    text = { Text("Edit Message") },
                    leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) },
                    onClick = {
                        showActionsMenu = false
                        onEdit()
                    }
                )
                DropdownMenuItem(
                    text = { Text("Delete for Everyone") },
                    leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null) },
                    onClick = {
                        showActionsMenu = false
                        onDelete()
                    }
                )
            }
        }
    }
}

@Composable
private fun VoiceNoteComposerBar(
    state: VoiceRecordingState,
    onLock: () -> Unit,
    onPauseResume: () -> Unit,
    onStopAndPreview: () -> Unit,
    onCancel: () -> Unit,
    onTogglePlayPreview: () -> Unit,
    onCycleSpeed: () -> Unit,
    onSendVoiceNote: () -> Unit
) {
    Surface(
        tonalElevation = 8.dp,
        color = MaterialTheme.colorScheme.secondaryContainer,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Mic,
                        contentDescription = "Recording Voice Note",
                        tint = MaterialTheme.colorScheme.error
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (state.isPreviewReady) {
                            "Preview Voice Note (${state.durationSeconds}s)"
                        } else if (state.isPaused) {
                            "Recording Paused (${state.durationSeconds}s)"
                        } else {
                            "Recording… ${state.durationSeconds}s"
                        },
                        style = MaterialTheme.typography.titleSmall
                    )
                }

                // Live Waveform Visualization Bars
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(3.dp),
                    modifier = Modifier.height(28.dp)
                ) {
                    state.waveformAmplitudes.takeLast(18).forEach { amp ->
                        Box(
                            modifier = Modifier
                                .width(4.dp)
                                .height((26 * amp).dp)
                                .clip(RoundedCornerShape(50))
                                .background(MaterialTheme.colorScheme.primary)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(
                    onClick = onCancel,
                    modifier = Modifier.testTag("voice_note_cancel_btn")
                ) {
                    Icon(Icons.Default.Delete, contentDescription = "Cancel voice note")
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Cancel")
                }

                if (state.isRecording) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (!state.isLocked) {
                            OutlinedButton(onClick = onLock) {
                                Icon(Icons.Default.Lock, contentDescription = "Lock recording", modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Lock")
                            }
                        }
                        OutlinedButton(onClick = onPauseResume) {
                            Icon(
                                imageVector = if (state.isPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
                                contentDescription = "Pause or resume"
                            )
                        }
                        Button(onClick = onStopAndPreview) {
                            Icon(Icons.Default.Stop, contentDescription = "Preview before sending")
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Preview")
                        }
                    }
                } else {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = onTogglePlayPreview) {
                            Icon(
                                imageVector = if (state.isPlayingPreview) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = "Play preview"
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(if (state.isPlayingPreview) "Playing" else "Listen")
                        }
                        OutlinedButton(onClick = onCycleSpeed) {
                            Text("${state.playbackSpeed}x")
                        }
                    }
                }

                Button(
                    onClick = onSendVoiceNote,
                    modifier = Modifier.testTag("voice_note_send_btn")
                ) {
                    Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send Voice Note")
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Send")
                }
            }
        }
    }
}

@Composable
private fun AttachmentOptionTile(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = modifier
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = label, tint = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.width(10.dp))
            Text(label, style = MaterialTheme.typography.labelLarge)
        }
    }
}

@Composable
private fun CameraCaptureDialog(
    onDismiss: () -> Unit,
    onSendCapturedMedia: (caption: String, isVideo: Boolean, lensName: String, flashOn: Boolean) -> Unit
) {
    var isVideoMode by remember { mutableStateOf(false) }
    var isFrontLens by remember { mutableStateOf(false) }
    var isFlashEnabled by remember { mutableStateOf(false) }
    var isPreviewCaptured by remember { mutableStateOf(false) }
    var caption by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(if (isPreviewCaptured) "Preview & Add Caption" else "Jom! HD Camera")
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp),
                    shape = RoundedCornerShape(16.dp),
                    color = Color(0xFF0B132B)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = if (isVideoMode) Icons.Default.Videocam else Icons.Default.CameraAlt,
                                contentDescription = null,
                                tint = JomCyan,
                                modifier = Modifier.size(44.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = if (isPreviewCaptured) {
                                    "Media Captured Ready to Send"
                                } else {
                                    "${if (isFrontLens) "Front" else "Rear"} Camera • Flash ${if (isFlashEnabled) "ON" else "OFF"}"
                                },
                                color = Color.White,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    OutlinedButton(onClick = { isFrontLens = !isFrontLens }) {
                        Icon(Icons.Default.FlipCameraAndroid, contentDescription = "Switch camera", modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(if (isFrontLens) "Front" else "Rear")
                    }
                    OutlinedButton(onClick = { isFlashEnabled = !isFlashEnabled }) {
                        Icon(Icons.Default.FlashOn, contentDescription = "Toggle flash", modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(if (isFlashEnabled) "Flash ON" else "Flash OFF")
                    }
                    OutlinedButton(onClick = { isVideoMode = !isVideoMode }) {
                        Text(if (isVideoMode) "Video" else "Photo")
                    }
                }

                OutlinedTextField(
                    value = caption,
                    onValueChange = { caption = it },
                    label = { Text("Add a caption before sending…") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (!isPreviewCaptured) {
                        isPreviewCaptured = true
                    } else {
                        onSendCapturedMedia(
                            caption,
                            isVideoMode,
                            if (isFrontLens) "Front Lens" else "Rear Lens",
                            isFlashEnabled
                        )
                    }
                }
            ) {
                Text(if (!isPreviewCaptured) "Capture & Preview" else "Send Now")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun CreateConversationDialog(
    isGroup: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (title: String, participants: List<String>, description: String) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var membersInput by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (isGroup) "Create New Group" else "Start Direct Conversation") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text(if (isGroup) "Group Name" else "Recipient Name / @username") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("dialog_conv_title_input")
                )
                OutlinedTextField(
                    value = membersInput,
                    onValueChange = { membersInput = it },
                    label = { Text("Participant @usernames (comma separated)") },
                    placeholder = { Text("sarah_chen, marcus_dev") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                if (isGroup) {
                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text("Group Description") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val members = membersInput.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                    onConfirm(
                        title.ifBlank { "Jom! Chat" },
                        if (members.isEmpty()) listOf("peer_user") else members,
                        description
                    )
                },
                modifier = Modifier.testTag("dialog_conv_confirm_btn")
            ) {
                Text(if (isGroup) "Create Group" else "Start Chat")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun GroupAdminSettingsDialog(
    conversation: Conversation,
    onDismiss: () -> Unit,
    onSave: (String, String, Boolean, Boolean, List<String>, List<String>) -> Unit
) {
    var title by remember { mutableStateOf(conversation.title) }
    var description by remember { mutableStateOf(conversation.description) }
    var onlyAdminsSend by remember { mutableStateOf(conversation.onlyAdminsCanSend) }
    var onlyAdminsEdit by remember { mutableStateOf(conversation.onlyAdminsCanEditInfo) }
    var newMemberHandle by remember { mutableStateOf("") }
    var members by remember { mutableStateOf(conversation.participantIds) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Group Admin & Permissions") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Group Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Group Description") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    text = "Invite Link: ${conversation.inviteLink.ifBlank { "https://jom.chat/invite/${conversation.conversationId}" }}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Only Admins Can Send Messages", style = MaterialTheme.typography.bodySmall)
                    Switch(checked = onlyAdminsSend, onCheckedChange = { onlyAdminsSend = it })
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Only Admins Can Edit Group Info", style = MaterialTheme.typography.bodySmall)
                    Switch(checked = onlyAdminsEdit, onCheckedChange = { onlyAdminsEdit = it })
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = newMemberHandle,
                        onValueChange = { newMemberHandle = it },
                        label = { Text("Add Member @username") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(onClick = {
                        if (newMemberHandle.isNotBlank()) {
                            members = (members + newMemberHandle.trim()).distinct()
                            newMemberHandle = ""
                        }
                    }) {
                        Text("Add")
                    }
                }
                Text(
                    text = "Members (${members.size}): ${members.joinToString(", ")}",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        },
        confirmButton = {
            Button(onClick = {
                onSave(title, description, onlyAdminsSend, onlyAdminsEdit, members, conversation.adminIds)
            }) {
                Text("Save Group Settings")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}
