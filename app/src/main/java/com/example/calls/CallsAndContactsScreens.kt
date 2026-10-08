package com.example.calls

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.CallMade
import androidx.compose.material.icons.automirrored.filled.CallMissed
import androidx.compose.material.icons.automirrored.filled.CallReceived
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.FlipCameraAndroid
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.NetworkCheck
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.PictureInPictureAlt
import androidx.compose.material.icons.filled.Report
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.VideocamOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.domain.model.CallSession
import com.example.domain.model.CallState
import com.example.domain.model.CallType
import com.example.domain.model.ContactEntry
import com.example.ui.ActiveCallOverlayState
import com.example.ui.UiState
import com.example.ui.theme.DangerCrimson
import com.example.ui.theme.JomCyan
import com.example.ui.theme.JomDeepBlue
import com.example.ui.theme.JomRoyalBlue
import com.example.ui.theme.OnlineEmerald
import java.text.SimpleDateFormat
import java.util.Locale

@Composable
fun CallsHistoryScreen(
    callsState: UiState<List<CallSession>>,
    currentUserId: String,
    onStartVoiceCall: (calleeId: String, calleeName: String) -> Unit,
    onStartVideoCall: (calleeId: String, calleeName: String) -> Unit
) {
    var showNewCallDialog by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Button(
                onClick = { showNewCallDialog = true },
                modifier = Modifier
                    .weight(1f)
                    .testTag("start_voice_call_btn"),
                shape = RoundedCornerShape(14.dp)
            ) {
                Icon(Icons.Default.Call, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("New Voice Call")
            }

            OutlinedButton(
                onClick = { showNewCallDialog = true },
                modifier = Modifier
                    .weight(1f)
                    .testTag("start_video_call_btn"),
                shape = RoundedCornerShape(14.dp)
            ) {
                Icon(Icons.Default.Videocam, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("New Video Call")
            }
        }

        when (callsState) {
            is UiState.Loading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Loading WebRTC call history…")
                }
            }
            is UiState.Error -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(callsState.message, color = MaterialTheme.colorScheme.error)
                }
            }
            is UiState.Success -> {
                val calls = callsState.data
                if (calls.isEmpty()) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Call,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("No Recent Calls Yet", style = MaterialTheme.typography.headlineMedium)
                        Text(
                            "Start a low-latency WebRTC voice or HD video call with NAT traversal & TURN relay.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 6.dp, bottom = 18.dp)
                        )
                        Button(onClick = { onStartVideoCall("sarah_chen", "Sarah Chen") }) {
                            Text("Test HD Video Call with Sarah Chen")
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .testTag("calls_history_list"),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(calls, key = { it.callId }) { call ->
                            CallHistoryCard(
                                call = call,
                                isOutgoing = call.callerId == currentUserId,
                                onCallBackVoice = {
                                    val peerName = if (call.callerId == currentUserId) call.calleeName else call.callerName
                                    val peerId = if (call.callerId == currentUserId) call.calleeId else call.callerId
                                    onStartVoiceCall(peerId, peerName)
                                },
                                onCallBackVideo = {
                                    val peerName = if (call.callerId == currentUserId) call.calleeName else call.callerName
                                    val peerId = if (call.callerId == currentUserId) call.calleeId else call.callerId
                                    onStartVideoCall(peerId, peerName)
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    if (showNewCallDialog) {
        var peerHandle by remember { mutableStateOf("sarah_chen") }
        var peerName by remember { mutableStateOf("Sarah Chen") }
        AlertDialog(
            onDismissRequest = { showNewCallDialog = false },
            title = { Text("Start Encrypted WebRTC Call") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = peerName,
                        onValueChange = { peerName = it },
                        label = { Text("Contact Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = peerHandle,
                        onValueChange = { peerHandle = it },
                        label = { Text("Username / Peer ID") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = {
                        showNewCallDialog = false
                        onStartVoiceCall(peerHandle.ifBlank { "peer_1" }, peerName.ifBlank { "Peer" })
                    }) {
                        Text("Voice Call")
                    }
                    Button(onClick = {
                        showNewCallDialog = false
                        onStartVideoCall(peerHandle.ifBlank { "peer_1" }, peerName.ifBlank { "Peer" })
                    }) {
                        Text("Video Call")
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { showNewCallDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun CallHistoryCard(
    call: CallSession,
    isOutgoing: Boolean,
    onCallBackVoice: () -> Unit,
    onCallBackVideo: () -> Unit
) {
    val formatter = remember { SimpleDateFormat("MMM d, HH:mm", Locale.getDefault()) }
    val dateStr = call.createdAt?.toDate()?.let { formatter.format(it) } ?: "Today"
    val peerName = if (isOutgoing) call.calleeName else call.callerName
    val isMissed = call.state == CallState.MISSED.name || call.state == CallState.REJECTED.name

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(48.dp),
                shape = CircleShape,
                color = if (isMissed) DangerCrimson.copy(alpha = 0.15f) else MaterialTheme.colorScheme.primaryContainer
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = when {
                            isMissed -> Icons.AutoMirrored.Filled.CallMissed
                            isOutgoing -> Icons.AutoMirrored.Filled.CallMade
                            else -> Icons.AutoMirrored.Filled.CallReceived
                        },
                        contentDescription = null,
                        tint = if (isMissed) DangerCrimson else MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = peerName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "${if (isOutgoing) "Outgoing" else "Incoming"} ${call.callType} • ${call.durationSec}s • $dateStr",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            IconButton(onClick = onCallBackVoice) {
                Icon(
                    imageVector = Icons.Default.Call,
                    contentDescription = "Call back voice",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            IconButton(onClick = onCallBackVideo) {
                Icon(
                    imageVector = Icons.Default.Videocam,
                    contentDescription = "Call back video",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
fun ActiveWebRtcCallOverlay(
    overlayState: ActiveCallOverlayState,
    onToggleMute: () -> Unit,
    onToggleCamera: () -> Unit,
    onSwitchCameraLens: () -> Unit,
    onCycleAudioRoute: () -> Unit,
    onTogglePip: () -> Unit,
    onEndCall: () -> Unit
) {
    val isVideo = overlayState.callSession.callType == CallType.VIDEO.name
    val minutes = overlayState.elapsedSeconds / 60
    val seconds = overlayState.elapsedSeconds % 60
    val durationFormatted = String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)

    if (overlayState.isPipMinimized) {
        // Picture-in-Picture Floating Mini Call Card
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            contentAlignment = Alignment.TopEnd
        ) {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = JomDeepBlue),
                elevation = CardDefaults.cardElevation(defaultElevation = 12.dp),
                modifier = Modifier.width(240.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = overlayState.callSession.calleeName,
                            color = Color.White,
                            style = MaterialTheme.typography.titleSmall
                        )
                        Text(
                            text = durationFormatted,
                            color = JomCyan,
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        OutlinedButton(onClick = onTogglePip) {
                            Text("Expand", color = Color.White)
                        }
                        IconButton(onClick = onEndCall) {
                            Icon(Icons.Default.CallEnd, contentDescription = "End Call", tint = DangerCrimson)
                        }
                    }
                }
            }
        }
        return
    }

    // Full-screen Voice / Video Call UI
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFF04091A), JomDeepBlue, Color(0xFF06102E))
                )
            )
            .testTag("active_call_overlay")
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top Network Quality & PiP Controls
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = Color.White.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(50)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.NetworkCheck,
                            contentDescription = "Network Quality",
                            tint = OnlineEmerald,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = overlayState.iceConnectionStatus,
                            color = Color.White,
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }

                IconButton(onClick = onTogglePip) {
                    Icon(
                        imageVector = Icons.Default.PictureInPictureAlt,
                        contentDescription = "Minimize to Picture-in-Picture",
                        tint = Color.White
                    )
                }
            }

            // Center Peer Video Feed or Voice Avatar
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Surface(
                    modifier = Modifier.size(124.dp),
                    shape = CircleShape,
                    color = JomRoyalBlue
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = overlayState.callSession.calleeName.take(2).uppercase(),
                            style = MaterialTheme.typography.displayLarge,
                            color = Color.White
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = overlayState.callSession.calleeName,
                    style = MaterialTheme.typography.headlineLarge,
                    color = Color.White
                )

                Text(
                    text = if (overlayState.callSession.state == CallState.RINGING.name) {
                        "Ringing via WebRTC Signaling…"
                    } else {
                        "${overlayState.callSession.callType} Call • $durationFormatted"
                    },
                    style = MaterialTheme.typography.titleMedium,
                    color = JomCyan,
                    modifier = Modifier.padding(top = 6.dp)
                )

                if (isVideo && overlayState.isCameraEnabled) {
                    Spacer(modifier = Modifier.height(18.dp))
                    Surface(
                        color = Color.White.copy(alpha = 0.1f),
                        shape = RoundedCornerShape(18.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(170.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = "Local Camera Preview (${if (overlayState.isFrontCamera) "Front" else "Rear"} Lens • VP8 60fps)",
                                color = Color.White.copy(alpha = 0.85f),
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            }

            // Bottom Call Control Deck (Mute, Camera On/Off, Lens Switch, Speaker/BT, End Call)
            Surface(
                color = Color.White.copy(alpha = 0.12f),
                shape = RoundedCornerShape(28.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp, horizontal = 12.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onToggleMute,
                        modifier = Modifier
                            .size(52.dp)
                            .clip(CircleShape)
                            .background(if (overlayState.isMuted) DangerCrimson else Color.White.copy(alpha = 0.18f))
                    ) {
                        Icon(
                            imageVector = if (overlayState.isMuted) Icons.Default.MicOff else Icons.Default.Mic,
                            contentDescription = "Mute or unmute microphone",
                            tint = Color.White
                        )
                    }

                    IconButton(
                        onClick = onCycleAudioRoute,
                        modifier = Modifier
                            .size(52.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.18f))
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                            contentDescription = "Audio Route: ${overlayState.audioRoute.label}",
                            tint = JomCyan
                        )
                    }

                    if (isVideo) {
                        IconButton(
                            onClick = onToggleCamera,
                            modifier = Modifier
                                .size(52.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.18f))
                        ) {
                            Icon(
                                imageVector = if (overlayState.isCameraEnabled) Icons.Default.Videocam else Icons.Default.VideocamOff,
                                contentDescription = "Toggle Camera",
                                tint = Color.White
                            )
                        }

                        IconButton(
                            onClick = onSwitchCameraLens,
                            modifier = Modifier
                                .size(52.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.18f))
                        ) {
                            Icon(
                                imageVector = Icons.Default.FlipCameraAndroid,
                                contentDescription = "Switch front/rear camera",
                                tint = Color.White
                            )
                        }
                    }

                    FloatingActionButton(
                        onClick = onEndCall,
                        containerColor = DangerCrimson,
                        contentColor = Color.White,
                        shape = CircleShape,
                        modifier = Modifier
                            .size(58.dp)
                            .testTag("end_call_button")
                    ) {
                        Icon(Icons.Default.CallEnd, contentDescription = "End Call")
                    }
                }
            }
        }
    }
}

@Composable
fun ContactsScreen(
    contactsState: UiState<List<ContactEntry>>,
    onAddContact: (username: String, displayName: String, phone: String, bio: String) -> Unit,
    onStartChatWithContact: (ContactEntry) -> Unit,
    onVoiceCallContact: (ContactEntry) -> Unit,
    onVideoCallContact: (ContactEntry) -> Unit,
    onToggleBlockContact: (ContactEntry) -> Unit,
    onReportContact: (ContactEntry, String) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var showOnlineOnly by remember { mutableStateOf(false) }
    var showAddDialog by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize()) {
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Discover contacts by @username or phone number…") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search contacts") },
            singleLine = true,
            shape = RoundedCornerShape(18.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .testTag("contacts_search_input")
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = { showAddDialog = true },
                modifier = Modifier
                    .weight(1f)
                    .testTag("add_contact_button"),
                shape = RoundedCornerShape(14.dp)
            ) {
                Icon(Icons.Default.PersonAdd, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Add Contact")
            }

            OutlinedButton(
                onClick = { showOnlineOnly = !showOnlineOnly },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text(if (showOnlineOnly) "Showing: Online" else "Filter: All Contacts")
            }
        }

        when (contactsState) {
            is UiState.Loading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Loading contacts directory…")
                }
            }
            is UiState.Error -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(contactsState.message, color = MaterialTheme.colorScheme.error)
                }
            }
            is UiState.Success -> {
                val filtered = contactsState.data.filter {
                    (!showOnlineOnly || it.isOnline) &&
                        (searchQuery.isBlank() ||
                            it.displayName.contains(searchQuery, ignoreCase = true) ||
                            it.username.contains(searchQuery, ignoreCase = true) ||
                            it.phone.contains(searchQuery, ignoreCase = true))
                }

                if (filtered.isEmpty()) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.PersonAdd,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("Your Jom! Contacts Directory", style = MaterialTheme.typography.headlineMedium)
                        Text(
                            "Discover people by @username, phone number, or sync device contacts with explicit permission.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 6.dp, bottom = 18.dp)
                        )
                        Button(
                            onClick = {
                                onAddContact("sarah_chen", "Sarah Chen", "+60 12-345 6789", "Product Lead @ Jom!")
                                onAddContact("marcus_dev", "Marcus Vance", "+60 17-889 1122", "WebRTC & Rust Systems")
                            },
                            modifier = Modifier.testTag("sync_starter_contacts_btn")
                        ) {
                            Icon(Icons.Default.Sync, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Sync Sample Contacts")
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .testTag("contacts_list"),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(filtered, key = { it.contactId }) { contact ->
                            ContactRowCard(
                                contact = contact,
                                onChat = { onStartChatWithContact(contact) },
                                onVoiceCall = { onVoiceCallContact(contact) },
                                onVideoCall = { onVideoCallContact(contact) },
                                onToggleBlock = { onToggleBlockContact(contact) },
                                onReport = { onReportContact(contact, "Spam or suspicious behavior") }
                            )
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        var username by remember { mutableStateOf("") }
        var displayName by remember { mutableStateOf("") }
        var phone by remember { mutableStateOf("") }
        var bio by remember { mutableStateOf("Available on Jom!") }

        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("Add or Invite Jom! Contact") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = username,
                        onValueChange = { username = it },
                        label = { Text("Username (@handle)") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("dialog_contact_username")
                    )
                    OutlinedTextField(
                        value = displayName,
                        onValueChange = { displayName = it },
                        label = { Text("Display Name") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("dialog_contact_name")
                    )
                    OutlinedTextField(
                        value = phone,
                        onValueChange = { phone = it },
                        label = { Text("Phone Number (Optional)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = bio,
                        onValueChange = { bio = it },
                        label = { Text("Status / Bio") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showAddDialog = false
                        onAddContact(username, displayName, phone, bio)
                    },
                    modifier = Modifier.testTag("dialog_contact_save_btn")
                ) {
                    Text("Save Contact")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun ContactRowCard(
    contact: ContactEntry,
    onChat: () -> Unit,
    onVoiceCall: () -> Unit,
    onVideoCall: () -> Unit,
    onToggleBlock: () -> Unit,
    onReport: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    modifier = Modifier.size(48.dp),
                    shape = CircleShape,
                    color = JomRoyalBlue
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = contact.displayName.take(2).uppercase(),
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = contact.displayName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        if (contact.isBlocked) {
                            Badge(containerColor = DangerCrimson) {
                                Text("BLOCKED", color = Color.White)
                            }
                        } else if (contact.isOnline) {
                            Badge(containerColor = OnlineEmerald) {
                                Text("ONLINE", color = Color.White)
                            }
                        }
                    }
                    Text(
                        text = "@${contact.username} • ${contact.bio}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                IconButton(onClick = onVoiceCall) {
                    Icon(Icons.Default.Call, contentDescription = "Voice Call", tint = MaterialTheme.colorScheme.primary)
                }
                IconButton(onClick = onVideoCall) {
                    Icon(Icons.Default.Videocam, contentDescription = "Video Call", tint = MaterialTheme.colorScheme.primary)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onChat,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Message")
                }
                OutlinedButton(
                    onClick = onToggleBlock,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Block, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(if (contact.isBlocked) "Unblock" else "Block")
                }
                OutlinedButton(
                    onClick = onReport,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Report, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Report")
                }
            }
        }
    }
}
