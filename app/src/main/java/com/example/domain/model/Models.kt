package com.example.domain.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.FieldValue

enum class PrivacyVisibility {
    EVERYONE, CONTACTS, NOBODY
}

enum class MessageType {
    TEXT, IMAGE, VIDEO, AUDIO, VOICE_NOTE, DOCUMENT, LOCATION, CONTACT, GIF, STICKER
}

enum class MessageDeliveryStatus {
    SENDING, SENT, DELIVERED, READ, FAILED
}

enum class CallType {
    VOICE, VIDEO
}

enum class CallState {
    RINGING, ACCEPTED, CONNECTED, REJECTED, ENDED, MISSED, TIMEOUT
}

enum class NotificationPrivacyMode(val label: String) {
    FULL_MESSAGE("Show sender and full message"),
    SENDER_ONLY("Show sender name only"),
    HIDE_ALL("Hide sender and message content")
}

data class UserProfile(
    val userId: String = "",
    val username: String = "",
    val displayName: String = "",
    val bio: String = "Hey there! I am using Jom!",
    val avatarUrl: String = "",
    val phone: String = "",
    val email: String = "",
    val isOnline: Boolean = true,
    val lastSeen: Timestamp? = null,
    val privacyLastSeen: String = PrivacyVisibility.EVERYONE.name,
    val privacyAvatar: String = PrivacyVisibility.EVERYONE.name,
    val privacyBio: String = PrivacyVisibility.EVERYONE.name,
    val readReceipts: Boolean = true,
    val typingIndicator: Boolean = true,
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null
) {
    fun toCreateMap(): Map<String, Any> {
        return mapOf(
            "userId" to userId,
            "username" to username,
            "displayName" to displayName,
            "bio" to bio,
            "avatarUrl" to avatarUrl,
            "phone" to phone,
            "email" to email,
            "isOnline" to isOnline,
            "lastSeen" to FieldValue.serverTimestamp(),
            "privacyLastSeen" to privacyLastSeen,
            "privacyAvatar" to privacyAvatar,
            "privacyBio" to privacyBio,
            "readReceipts" to readReceipts,
            "typingIndicator" to typingIndicator,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        ).filterValues { it != null }
    }
}

data class ContactEntry(
    val contactId: String = "",
    val ownerId: String = "",
    val contactUserId: String = "",
    val username: String = "",
    val displayName: String = "",
    val phone: String = "",
    val avatarUrl: String = "",
    val bio: String = "",
    val isOnline: Boolean = true,
    val isBlocked: Boolean = false,
    val isFavorite: Boolean = false,
    val createdAt: Timestamp? = null
) {
    fun toCreateMap(): Map<String, Any> {
        return mapOf(
            "contactId" to contactId,
            "ownerId" to ownerId,
            "contactUserId" to contactUserId,
            "username" to username,
            "displayName" to displayName,
            "phone" to phone,
            "avatarUrl" to avatarUrl,
            "bio" to bio,
            "isOnline" to isOnline,
            "isBlocked" to isBlocked,
            "isFavorite" to isFavorite,
            "createdAt" to FieldValue.serverTimestamp()
        ).filterValues { it != null }
    }
}

data class Conversation(
    val conversationId: String = "",
    val ownerId: String = "",
    val participantIds: List<String> = emptyList(),
    val adminIds: List<String> = emptyList(),
    val title: String = "",
    val description: String = "",
    val avatarUrl: String = "",
    val isGroup: Boolean = false,
    val lastMessageText: String = "",
    val lastMessageSenderId: String = "",
    val lastMessageType: String = MessageType.TEXT.name,
    val inviteLink: String = "",
    val onlyAdminsCanSend: Boolean = false,
    val onlyAdminsCanEditInfo: Boolean = false,
    val isPinned: Boolean = false,
    val isMuted: Boolean = false,
    val unreadCount: Int = 0,
    val isOnline: Boolean = true,
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null
) {
    fun toCreateMap(): Map<String, Any> {
        return mapOf(
            "conversationId" to conversationId,
            "ownerId" to ownerId,
            "participantIds" to participantIds,
            "adminIds" to adminIds,
            "title" to title,
            "description" to description,
            "avatarUrl" to avatarUrl,
            "isGroup" to isGroup,
            "lastMessageText" to lastMessageText,
            "lastMessageSenderId" to lastMessageSenderId,
            "lastMessageType" to lastMessageType,
            "inviteLink" to inviteLink,
            "onlyAdminsCanSend" to onlyAdminsCanSend,
            "onlyAdminsCanEditInfo" to onlyAdminsCanEditInfo,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        ).filterValues { it != null }
    }
}

data class ChatMessage(
    val messageId: String = "",
    val conversationId: String = "",
    val senderId: String = "",
    val senderName: String = "",
    val participantIds: List<String> = emptyList(),
    val text: String = "",
    val messageType: String = MessageType.TEXT.name,
    val mediaUrl: String? = null,
    val mediaFileName: String? = null,
    val mediaMimeType: String? = null,
    val mediaFileSize: Long? = null,
    val mediaDurationSec: Int? = null,
    val replyToMessageId: String? = null,
    val replyToPreview: String? = null,
    val isForwarded: Boolean = false,
    val isEdited: Boolean = false,
    val isDeleted: Boolean = false,
    val isPinned: Boolean = false,
    val isStarred: Boolean = false,
    val reactionsSummary: String = "",
    val status: String = MessageDeliveryStatus.SENT.name,
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null
) {
    @Suppress("UNCHECKED_CAST")
    fun toCreateMap(): Map<String, Any> {
        return mapOf<String, Any?>(
            "messageId" to messageId,
            "conversationId" to conversationId,
            "senderId" to senderId,
            "senderName" to senderName,
            "participantIds" to participantIds,
            "text" to text,
            "messageType" to messageType,
            "mediaUrl" to mediaUrl,
            "mediaFileName" to mediaFileName,
            "mediaMimeType" to mediaMimeType,
            "mediaFileSize" to mediaFileSize,
            "mediaDurationSec" to mediaDurationSec,
            "replyToMessageId" to replyToMessageId,
            "replyToPreview" to replyToPreview,
            "isForwarded" to isForwarded,
            "isEdited" to isEdited,
            "isDeleted" to isDeleted,
            "isPinned" to isPinned,
            "isStarred" to isStarred,
            "reactionsSummary" to reactionsSummary,
            "status" to status,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        ).filterValues { it != null } as Map<String, Any>
    }
}

data class CallSession(
    val callId: String = "",
    val callerId: String = "",
    val callerName: String = "",
    val calleeId: String = "",
    val calleeName: String = "",
    val participantIds: List<String> = emptyList(),
    val callType: String = CallType.VOICE.name,
    val state: String = CallState.RINGING.name,
    val sdpOffer: String? = null,
    val sdpAnswer: String? = null,
    val iceCandidatesJson: String? = null,
    val durationSec: Int = 0,
    val networkQuality: String = "HD • 99.8% Stable (SRTP/DTLS)",
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null
) {
    @Suppress("UNCHECKED_CAST")
    fun toCreateMap(): Map<String, Any> {
        return mapOf<String, Any?>(
            "callId" to callId,
            "callerId" to callerId,
            "callerName" to callerName,
            "calleeId" to calleeId,
            "calleeName" to calleeName,
            "participantIds" to participantIds,
            "callType" to callType,
            "state" to state,
            "sdpOffer" to sdpOffer,
            "sdpAnswer" to sdpAnswer,
            "iceCandidatesJson" to iceCandidatesJson,
            "durationSec" to durationSec,
            "networkQuality" to networkQuality,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        ).filterValues { it != null } as Map<String, Any>
    }
}

data class AbuseReport(
    val reportId: String = "",
    val reporterId: String = "",
    val targetUserId: String = "",
    val reason: String = "",
    val details: String = "",
    val status: String = "OPEN",
    val createdAt: Timestamp? = null
) {
    fun toCreateMap(): Map<String, Any> {
        return mapOf(
            "reportId" to reportId,
            "reporterId" to reporterId,
            "targetUserId" to targetUserId,
            "reason" to reason,
            "details" to details,
            "status" to status,
            "createdAt" to FieldValue.serverTimestamp()
        ).filterValues { it != null }
    }
}
