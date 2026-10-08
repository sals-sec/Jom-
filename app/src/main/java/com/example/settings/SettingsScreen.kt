package com.example.settings

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.core.config.AppEnvironmentConfig
import com.example.domain.model.ContactEntry
import com.example.domain.model.NotificationPrivacyMode
import com.example.domain.model.PrivacyVisibility
import com.example.domain.model.UserProfile
import com.example.network.RealtimeSocketState
import com.example.ui.UiState
import com.example.ui.components.JomBrandEmblem
import com.example.ui.theme.DangerCrimson
import com.example.ui.theme.JomRoyalBlue

@Composable
fun SettingsScreen(
    myProfileState: UiState<UserProfile?>,
    contactsState: UiState<List<ContactEntry>>,
    currentUserEmail: String,
    socketState: RealtimeSocketState,
    isDarkModeOverride: Boolean?,
    notificationsEnabled: Boolean,
    notificationPrivacyMode: NotificationPrivacyMode,
    onSaveProfile: (username: String, displayName: String, bio: String, phone: String) -> Unit,
    onSavePrivacy: (
        lastSeen: PrivacyVisibility,
        avatar: PrivacyVisibility,
        bio: PrivacyVisibility,
        readReceipts: Boolean,
        typingIndicator: Boolean
    ) -> Unit,
    onToggleDarkMode: (Boolean?) -> Unit,
    onToggleNotifications: (Boolean) -> Unit,
    onSelectNotificationPrivacy: (NotificationPrivacyMode) -> Unit,
    onToggleOfflineSimulation: () -> Unit,
    onUnblockContact: (ContactEntry) -> Unit,
    onSignOut: () -> Unit,
    onDeleteAccount: () -> Unit
) {
    val profile = (myProfileState as? UiState.Success)?.data
    val blockedContacts = (contactsState as? UiState.Success)?.data?.filter { it.isBlocked }.orEmpty()

    var username by remember(profile?.username) { mutableStateOf(profile?.username ?: "jom_user") }
    var displayName by remember(profile?.displayName) { mutableStateOf(profile?.displayName ?: "Jom! Member") }
    var bio by remember(profile?.bio) { mutableStateOf(profile?.bio ?: "Hey there! I am using Jom!") }
    var phone by remember(profile?.phone) { mutableStateOf(profile?.phone ?: "") }

    var lastSeenVisibility by remember(profile?.privacyLastSeen) {
        mutableStateOf(
            runCatching { PrivacyVisibility.valueOf(profile?.privacyLastSeen ?: "EVERYONE") }
                .getOrDefault(PrivacyVisibility.EVERYONE)
        )
    }
    var avatarVisibility by remember(profile?.privacyAvatar) {
        mutableStateOf(
            runCatching { PrivacyVisibility.valueOf(profile?.privacyAvatar ?: "EVERYONE") }
                .getOrDefault(PrivacyVisibility.EVERYONE)
        )
    }
    var bioVisibility by remember(profile?.privacyBio) {
        mutableStateOf(
            runCatching { PrivacyVisibility.valueOf(profile?.privacyBio ?: "EVERYONE") }
                .getOrDefault(PrivacyVisibility.EVERYONE)
        )
    }
    var readReceipts by remember(profile?.readReceipts) { mutableStateOf(profile?.readReceipts ?: true) }
    var typingIndicator by remember(profile?.typingIndicator) { mutableStateOf(profile?.typingIndicator ?: true) }

    var showDeleteConfirmDialog by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("settings_list"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 1. Account & Profile Card
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        JomBrandEmblem(
                            modifier = Modifier.size(56.dp),
                            cornerRadius = 16.dp,
                            showText = true
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text("Account & Profile", style = MaterialTheme.typography.titleLarge)
                            Text(
                                text = currentUserEmail.ifBlank { "Verified Google Session" },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    OutlinedTextField(
                        value = displayName,
                        onValueChange = { displayName = it },
                        label = { Text("Display Name") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("settings_display_name_input")
                    )

                    OutlinedTextField(
                        value = username,
                        onValueChange = { username = it },
                        label = { Text("Username (@handle)") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("settings_username_input")
                    )

                    OutlinedTextField(
                        value = bio,
                        onValueChange = { bio = it },
                        label = { Text("About / Status Message") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = phone,
                        onValueChange = { phone = it },
                        label = { Text("Verified Phone Number") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Button(
                        onClick = { onSaveProfile(username, displayName, bio, phone) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("settings_save_profile_btn"),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Save Profile Changes")
                    }
                }
            }
        }

        // 2. User Privacy Controls (Section 13: Last Seen, Online, Photo, Bio, Read Receipts, Typing Indicator)
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Privacy & Visibility Controls", style = MaterialTheme.typography.titleLarge)
                    }

                    PrivacyVisibilitySelector(
                        title = "Last Seen & Online Status",
                        selected = lastSeenVisibility,
                        onSelect = { lastSeenVisibility = it }
                    )

                    PrivacyVisibilitySelector(
                        title = "Profile Photo Visibility",
                        selected = avatarVisibility,
                        onSelect = { avatarVisibility = it }
                    )

                    PrivacyVisibilitySelector(
                        title = "About / Status Visibility",
                        selected = bioVisibility,
                        onSelect = { bioVisibility = it }
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Read Receipts (Double Checkmarks)", style = MaterialTheme.typography.bodyMedium)
                        Switch(checked = readReceipts, onCheckedChange = { readReceipts = it })
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Real-Time Typing Indicator", style = MaterialTheme.typography.bodyMedium)
                        Switch(checked = typingIndicator, onCheckedChange = { typingIndicator = it })
                    }

                    OutlinedButton(
                        onClick = {
                            onSavePrivacy(
                                lastSeenVisibility,
                                avatarVisibility,
                                bioVisibility,
                                readReceipts,
                                typingIndicator
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Text("Sync Privacy Controls")
                    }
                }
            }
        }

        // 3. Notifications & Notification Privacy (Section 11: Full message, Sender only, Hide content)
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Notifications, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Push Notifications & Privacy", style = MaterialTheme.typography.titleLarge)
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Enable Message & Call Alerts", style = MaterialTheme.typography.bodyMedium)
                        Switch(checked = notificationsEnabled, onCheckedChange = onToggleNotifications)
                    }

                    Text("Lock-Screen Notification Privacy:", style = MaterialTheme.typography.labelLarge)
                    NotificationPrivacyMode.entries.forEach { mode ->
                        FilterChip(
                            selected = notificationPrivacyMode == mode,
                            onClick = { onSelectNotificationPrivacy(mode) },
                            label = { Text(mode.label) },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }

        // 4. Appearance, Offline Queue & Data Storage (Section 15 & Settings)
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.DarkMode, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Appearance, Offline & Storage", style = MaterialTheme.typography.titleLarge)
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = isDarkModeOverride == null,
                            onClick = { onToggleDarkMode(null) },
                            label = { Text("System Theme") }
                        )
                        FilterChip(
                            selected = isDarkModeOverride == false,
                            onClick = { onToggleDarkMode(false) },
                            label = { Text("Light") }
                        )
                        FilterChip(
                            selected = isDarkModeOverride == true,
                            onClick = { onToggleDarkMode(true) },
                            label = { Text("Dark") }
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Offline Queue & Sync Test Mode", style = MaterialTheme.typography.bodyMedium)
                            Text(
                                text = "Current status: ${socketState.name}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        OutlinedButton(onClick = onToggleOfflineSimulation) {
                            Icon(Icons.Default.CloudSync, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(if (socketState == RealtimeSocketState.OFFLINE_QUEUING) "Go Online" else "Simulate Offline")
                        }
                    }

                    Text(
                        text = "WebRTC TURN Server: ${AppEnvironmentConfig.turnServerUri} • Room SQLite Cache Active",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // 5. Blocked Users & Security / Moderation Info
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Block, contentDescription = null, tint = DangerCrimson)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Blocked Users (${blockedContacts.size})", style = MaterialTheme.typography.titleLarge)
                    }

                    if (blockedContacts.isEmpty()) {
                        Text(
                            text = "No blocked contacts. You can block or report abusive accounts anytime from Contacts.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        blockedContacts.forEach { blocked ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("${blocked.displayName} (@${blocked.username})")
                                TextButton(onClick = { onUnblockContact(blocked) }) {
                                    Text("Unblock")
                                }
                            }
                        }
                    }
                }
            }
        }

        // 6. Security, About, Logout & Delete Account
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Security, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Security & Session Management", style = MaterialTheme.typography.titleLarge)
                    }

                    Text(
                        text = "• Transport Encryption: HTTPS/TLS 1.3 + WebRTC SRTP/DTLS\n" +
                            "• Access Control: Zero-Trust Firestore ABAC Rules\n" +
                            "• Anti-Abuse: Client & Server Rate Limiting Active",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    OutlinedButton(
                        onClick = onSignOut,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("settings_logout_button"),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Log Out of Jom!")
                    }

                    Button(
                        onClick = { showDeleteConfirmDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = DangerCrimson, contentColor = Color.White),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("settings_delete_account_button"),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(Icons.Default.DeleteForever, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Delete Account & Wipe Data")
                    }
                }
            }
        }
    }

    if (showDeleteConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            title = { Text("Delete Jom! Account Permanently?") },
            text = {
                Text("This will permanently delete your Jom! user profile, clear your local Room SQLite cache, and sign you out.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteConfirmDialog = false
                        onDeleteAccount()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = DangerCrimson)
                ) {
                    Text("Confirm Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun PrivacyVisibilitySelector(
    title: String,
    selected: PrivacyVisibility,
    onSelect: (PrivacyVisibility) -> Unit
) {
    Column {
        Text(title, style = MaterialTheme.typography.labelLarge)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            PrivacyVisibility.entries.forEach { option ->
                FilterChip(
                    selected = selected == option,
                    onClick = { onSelect(option) },
                    label = { Text(option.name.lowercase().replaceFirstChar { it.uppercase() }) }
                )
            }
        }
    }
}
