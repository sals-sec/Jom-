package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.core.error.OperationType
import com.example.core.error.handleFirestoreError
import com.example.data.local.CachedCallEntity
import com.example.data.local.CachedContactEntity
import com.example.data.local.CachedConversationEntity
import com.example.data.local.CachedMessageEntity
import com.example.data.local.JomDao
import com.example.domain.model.AbuseReport
import com.example.domain.model.CallSession
import com.example.domain.model.CallState
import com.example.domain.model.CallType
import com.example.domain.model.ChatMessage
import com.example.domain.model.ContactEntry
import com.example.domain.model.Conversation
import com.example.domain.model.MessageDeliveryStatus
import com.example.domain.model.MessageType
import com.example.domain.model.UserProfile
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.util.UUID

class JomRepository(
    private val db: FirebaseFirestore,
    private val dao: JomDao? = null
) {
    constructor(context: Context, dao: JomDao? = null) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        ),
        dao
    )

    private val auth = Firebase.auth

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
    }

    // 1. User Profile CRUD & Real-Time Observation
    fun observeMyProfile(): Flow<UserProfile?> {
        val uid = auth.currentUser?.uid ?: ""
        val path = "users/$uid"
        return db.collection("users").document(uid.ifBlank { "unauthenticated_user" })
            .snapshots()
            .map { snapshot ->
                if (snapshot.exists()) {
                    snapshot.toObject(UserProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                } else {
                    null
                }
            }
            .catch { error ->
                if (error is Exception) handleFirestoreError(error, OperationType.GET, path)
                throw error
            }
    }

    suspend fun upsertUserProfile(
        username: String,
        displayName: String,
        bio: String = "Hey there! I am using Jom!",
        avatarUrl: String = "",
        phone: String = "",
        email: String = ""
    ): Result<UserProfile> = runCatching {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid)
        val existingSnap = try {
            docRef.get().await()
        } catch (e: Exception) {
            null
        }
        val profile = UserProfile(
            userId = uid,
            username = username.trim().lowercase().replace(" ", "_"),
            displayName = displayName.trim(),
            bio = bio.trim(),
            avatarUrl = avatarUrl,
            phone = phone.trim(),
            email = email.trim(),
            isOnline = true
        )
        if (existingSnap != null && existingSnap.exists()) {
            val updateMap = mapOf(
                "username" to profile.username,
                "displayName" to profile.displayName,
                "bio" to profile.bio,
                "avatarUrl" to profile.avatarUrl,
                "phone" to profile.phone,
                "email" to profile.email,
                "isOnline" to true,
                "lastSeen" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            try {
                docRef.update(updateMap).await()
            } catch (e: Exception) {
                handleFirestoreError(e, OperationType.UPDATE, docRef.path)
                throw e
            }
        } else {
            try {
                docRef.set(profile.toCreateMap()).await()
            } catch (e: Exception) {
                handleFirestoreError(e, OperationType.CREATE, docRef.path)
                throw e
            }
        }
        profile
    }

    suspend fun updatePrivacySettings(
        privacyLastSeen: String,
        privacyAvatar: String,
        privacyBio: String,
        readReceipts: Boolean,
        typingIndicator: Boolean
    ): Result<Unit> = runCatching {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid)
        val payload = mapOf(
            "privacyLastSeen" to privacyLastSeen,
            "privacyAvatar" to privacyAvatar,
            "privacyBio" to privacyBio,
            "readReceipts" to readReceipts,
            "typingIndicator" to typingIndicator,
            "updatedAt" to FieldValue.serverTimestamp()
        )
        try {
            docRef.update(payload).await()
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            throw e
        }
    }

    suspend fun deleteMyAccountData(): Result<Unit> = runCatching {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid)
        try {
            docRef.delete().await()
            dao?.clearAllConversations()
            dao?.clearAllMessages()
            dao?.clearAllContacts()
            dao?.clearAllCalls()
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            throw e
        }
    }

    // 2. Contacts CRUD & Real-Time Observation
    fun observeContacts(): Flow<List<ContactEntry>> {
        val uid = auth.currentUser?.uid ?: "unauthenticated_user"
        val path = "users/$uid/contacts"
        return db.collection("users").document(uid).collection("contacts")
            .whereEqualTo("ownerId", uid)
            .snapshots()
            .map { snapshot ->
                val list = snapshot.toObjects(ContactEntry::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                dao?.upsertContacts(
                    list.map { c ->
                        CachedContactEntity(
                            contactId = c.contactId,
                            ownerId = c.ownerId,
                            contactUserId = c.contactUserId,
                            username = c.username,
                            displayName = c.displayName,
                            phone = c.phone,
                            avatarUrl = c.avatarUrl,
                            bio = c.bio,
                            isOnline = c.isOnline,
                            isBlocked = c.isBlocked,
                            isFavorite = c.isFavorite,
                            createdAtEpochMs = c.createdAt?.toDate()?.time ?: System.currentTimeMillis()
                        )
                    }
                )
                list
            }
            .catch { error ->
                if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
                throw error
            }
    }

    suspend fun addContact(
        username: String,
        displayName: String,
        phone: String = "",
        bio: String = "Available on Jom!",
        contactUserId: String = "user_${UUID.randomUUID().toString().take(8)}"
    ): Result<String> = runCatching {
        val uid = requireUserId()
        val contactId = "cnt_${UUID.randomUUID().toString().replace("-", "").take(12)}"
        val docRef = db.collection("users").document(uid).collection("contacts").document(contactId)
        val entry = ContactEntry(
            contactId = contactId,
            ownerId = uid,
            contactUserId = contactUserId,
            username = username.trim().removePrefix("@").lowercase(),
            displayName = displayName.trim(),
            phone = phone.trim(),
            bio = bio.trim(),
            isOnline = true,
            isBlocked = false,
            isFavorite = false
        )
        try {
            docRef.set(entry.toCreateMap()).await()
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            throw e
        }
        contactId
    }

    suspend fun toggleBlockContact(contactId: String, isBlocked: Boolean): Result<Unit> = runCatching {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid).collection("contacts").document(contactId)
        try {
            docRef.update(mapOf("isBlocked" to isBlocked)).await()
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            throw e
        }
    }

    // 3. Conversations (1-to-1 and Group Messaging)
    fun observeConversations(): Flow<List<Conversation>> {
        val uid = auth.currentUser?.uid ?: "unauthenticated_user"
        val path = "conversations"
        return db.collection("conversations")
            .whereArrayContains("participantIds", uid)
            .snapshots()
            .map { snapshot ->
                val list = snapshot.toObjects(Conversation::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .sortedByDescending { (it.updatedAt ?: it.createdAt ?: Timestamp(0, 0)).seconds }
                dao?.upsertConversations(
                    list.map { c ->
                        CachedConversationEntity(
                            conversationId = c.conversationId,
                            ownerId = c.ownerId,
                            participantIdsCsv = c.participantIds.joinToString(","),
                            adminIdsCsv = c.adminIds.joinToString(","),
                            title = c.title,
                            description = c.description,
                            avatarUrl = c.avatarUrl,
                            isGroup = c.isGroup,
                            lastMessageText = c.lastMessageText,
                            lastMessageSenderId = c.lastMessageSenderId,
                            lastMessageType = c.lastMessageType,
                            inviteLink = c.inviteLink,
                            onlyAdminsCanSend = c.onlyAdminsCanSend,
                            onlyAdminsCanEditInfo = c.onlyAdminsCanEditInfo,
                            isPinned = c.isPinned,
                            isMuted = c.isMuted,
                            unreadCount = c.unreadCount,
                            isOnline = c.isOnline,
                            updatedAtEpochMs = c.updatedAt?.toDate()?.time ?: System.currentTimeMillis()
                        )
                    }
                )
                list
            }
            .catch { error ->
                if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
                throw error
            }
    }

    suspend fun createConversation(
        title: String,
        participantIds: List<String>,
        isGroup: Boolean = false,
        description: String = "",
        initialMessage: String = "Conversation started"
    ): Result<String> = runCatching {
        val uid = requireUserId()
        val convId = "conv_${UUID.randomUUID().toString().replace("-", "").take(14)}"
        val allParticipants = (participantIds + uid).distinct()
        val conv = Conversation(
            conversationId = convId,
            ownerId = uid,
            participantIds = allParticipants,
            adminIds = listOf(uid),
            title = title.trim(),
            description = description.trim(),
            isGroup = isGroup,
            lastMessageText = initialMessage,
            lastMessageSenderId = uid,
            lastMessageType = MessageType.TEXT.name,
            inviteLink = if (isGroup) "https://jom.chat/invite/$convId" else "",
            onlyAdminsCanSend = false,
            onlyAdminsCanEditInfo = isGroup
        )
        val docRef = db.collection("conversations").document(convId)
        try {
            docRef.set(conv.toCreateMap()).await()
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            throw e
        }
        convId
    }

    suspend fun getConversationById(conversationId: String): Result<Conversation?> = runCatching {
        val docRef = db.collection("conversations").document(conversationId)
        try {
            val snap = docRef.get().await()
            snap.toObject(Conversation::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, docRef.path)
            throw e
        }
    }

    suspend fun updateGroupSettings(
        conversationId: String,
        title: String,
        description: String,
        onlyAdminsCanSend: Boolean,
        onlyAdminsCanEditInfo: Boolean,
        participantIds: List<String>,
        adminIds: List<String>
    ): Result<Unit> = runCatching {
        val docRef = db.collection("conversations").document(conversationId)
        val payload = mapOf(
            "title" to title.trim(),
            "description" to description.trim(),
            "onlyAdminsCanSend" to onlyAdminsCanSend,
            "onlyAdminsCanEditInfo" to onlyAdminsCanEditInfo,
            "participantIds" to participantIds.distinct(),
            "adminIds" to adminIds.distinct(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        try {
            docRef.update(payload).await()
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            throw e
        }
    }

    suspend fun deleteConversation(conversationId: String): Result<Unit> = runCatching {
        val docRef = db.collection("conversations").document(conversationId)
        try {
            docRef.delete().await()
            dao?.deleteConversation(conversationId)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            throw e
        }
    }

    // 4. Messages inside a Conversation
    fun observeMessages(conversationId: String): Flow<List<ChatMessage>> {
        val uid = auth.currentUser?.uid ?: "unauthenticated_user"
        val path = "conversations/$conversationId/messages"
        return db.collection("conversations")
            .document(conversationId)
            .collection("messages")
            .whereArrayContains("participantIds", uid)
            .snapshots()
            .map { snapshot ->
                val list = snapshot.toObjects(ChatMessage::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .sortedBy { (it.createdAt ?: Timestamp(0, 0)).seconds }
                dao?.upsertMessages(
                    list.map { m ->
                        CachedMessageEntity(
                            messageId = m.messageId,
                            conversationId = m.conversationId,
                            senderId = m.senderId,
                            senderName = m.senderName,
                            participantIdsCsv = m.participantIds.joinToString(","),
                            text = m.text,
                            messageType = m.messageType,
                            mediaUrl = m.mediaUrl,
                            mediaFileName = m.mediaFileName,
                            mediaMimeType = m.mediaMimeType,
                            mediaFileSize = m.mediaFileSize,
                            mediaDurationSec = m.mediaDurationSec,
                            replyToMessageId = m.replyToMessageId,
                            replyToPreview = m.replyToPreview,
                            isForwarded = m.isForwarded,
                            isEdited = m.isEdited,
                            isDeleted = m.isDeleted,
                            isPinned = m.isPinned,
                            isStarred = m.isStarred,
                            reactionsSummary = m.reactionsSummary,
                            status = m.status,
                            createdAtEpochMs = m.createdAt?.toDate()?.time ?: System.currentTimeMillis()
                        )
                    }
                )
                list
            }
            .catch { error ->
                if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
                throw error
            }
    }

    suspend fun sendMessage(
        conversationId: String,
        participantIds: List<String>,
        senderName: String,
        text: String,
        messageType: MessageType = MessageType.TEXT,
        mediaUrl: String? = null,
        mediaFileName: String? = null,
        mediaMimeType: String? = null,
        mediaFileSize: Long? = null,
        mediaDurationSec: Int? = null,
        replyToMessageId: String? = null,
        replyToPreview: String? = null,
        isForwarded: Boolean = false
    ): Result<String> = runCatching {
        val uid = requireUserId()
        val msgId = "msg_${UUID.randomUUID().toString().replace("-", "").take(14)}"
        val allParticipants = (participantIds + uid).distinct()
        val msg = ChatMessage(
            messageId = msgId,
            conversationId = conversationId,
            senderId = uid,
            senderName = senderName.ifBlank { "Jom! User" },
            participantIds = allParticipants,
            text = text,
            messageType = messageType.name,
            mediaUrl = mediaUrl,
            mediaFileName = mediaFileName,
            mediaMimeType = mediaMimeType,
            mediaFileSize = mediaFileSize,
            mediaDurationSec = mediaDurationSec,
            replyToMessageId = replyToMessageId,
            replyToPreview = replyToPreview,
            isForwarded = isForwarded,
            status = MessageDeliveryStatus.DELIVERED.name
        )

        // Optimistically cache locally first for offline resilience
        dao?.upsertMessage(
            CachedMessageEntity(
                messageId = msgId,
                conversationId = conversationId,
                senderId = uid,
                senderName = msg.senderName,
                participantIdsCsv = allParticipants.joinToString(","),
                text = text,
                messageType = messageType.name,
                mediaUrl = mediaUrl,
                mediaFileName = mediaFileName,
                mediaMimeType = mediaMimeType,
                mediaFileSize = mediaFileSize,
                mediaDurationSec = mediaDurationSec,
                replyToMessageId = replyToMessageId,
                replyToPreview = replyToPreview,
                isForwarded = isForwarded,
                isEdited = false,
                isDeleted = false,
                isPinned = false,
                isStarred = false,
                reactionsSummary = "",
                status = MessageDeliveryStatus.SENDING.name,
                createdAtEpochMs = System.currentTimeMillis()
            )
        )

        val msgRef = db.collection("conversations")
            .document(conversationId)
            .collection("messages")
            .document(msgId)

        try {
            msgRef.set(msg.toCreateMap()).await()
            // Update parent conversation lastMessage preview
            db.collection("conversations").document(conversationId).update(
                mapOf(
                    "lastMessageText" to text.take(120),
                    "lastMessageSenderId" to uid,
                    "lastMessageType" to messageType.name,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, msgRef.path)
            throw e
        }
        msgId
    }

    suspend fun editMessage(conversationId: String, messageId: String, newText: String): Result<Unit> = runCatching {
        val msgRef = db.collection("conversations").document(conversationId).collection("messages").document(messageId)
        try {
            msgRef.update(
                mapOf(
                    "text" to newText.trim(),
                    "isEdited" to true,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, msgRef.path)
            throw e
        }
    }

    suspend fun deleteMessageForEveryone(conversationId: String, messageId: String): Result<Unit> = runCatching {
        val msgRef = db.collection("conversations").document(conversationId).collection("messages").document(messageId)
        try {
            msgRef.update(
                mapOf(
                    "text" to "🚫 This message was deleted",
                    "isDeleted" to true,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, msgRef.path)
            throw e
        }
    }

    suspend fun toggleMessageReaction(
        conversationId: String,
        messageId: String,
        newReactionSummary: String
    ): Result<Unit> = runCatching {
        val msgRef = db.collection("conversations").document(conversationId).collection("messages").document(messageId)
        try {
            msgRef.update(
                mapOf(
                    "reactionsSummary" to newReactionSummary,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, msgRef.path)
            throw e
        }
    }

    suspend fun toggleMessagePinOrStar(
        conversationId: String,
        messageId: String,
        isPinned: Boolean,
        isStarred: Boolean
    ): Result<Unit> = runCatching {
        val msgRef = db.collection("conversations").document(conversationId).collection("messages").document(messageId)
        try {
            msgRef.update(
                mapOf(
                    "isPinned" to isPinned,
                    "isStarred" to isStarred,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, msgRef.path)
            throw e
        }
    }

    // 5. WebRTC Calling Signaling & Call Logs
    fun observeCalls(): Flow<List<CallSession>> {
        val uid = auth.currentUser?.uid ?: "unauthenticated_user"
        val path = "calls"
        return db.collection("calls")
            .whereArrayContains("participantIds", uid)
            .snapshots()
            .map { snapshot ->
                val list = snapshot.toObjects(CallSession::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .sortedByDescending { (it.createdAt ?: Timestamp(0, 0)).seconds }
                dao?.upsertCalls(
                    list.map { c ->
                        CachedCallEntity(
                            callId = c.callId,
                            callerId = c.callerId,
                            callerName = c.callerName,
                            calleeId = c.calleeId,
                            calleeName = c.calleeName,
                            callType = c.callType,
                            state = c.state,
                            durationSec = c.durationSec,
                            networkQuality = c.networkQuality,
                            createdAtEpochMs = c.createdAt?.toDate()?.time ?: System.currentTimeMillis()
                        )
                    }
                )
                list
            }
            .catch { error ->
                if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
                throw error
            }
    }

    suspend fun startCallSession(
        callerName: String,
        calleeId: String,
        calleeName: String,
        callType: CallType,
        sdpOffer: String
    ): Result<CallSession> = runCatching {
        val uid = requireUserId()
        val callId = "call_${UUID.randomUUID().toString().replace("-", "").take(14)}"
        val session = CallSession(
            callId = callId,
            callerId = uid,
            callerName = callerName,
            calleeId = calleeId,
            calleeName = calleeName,
            participantIds = listOf(uid, calleeId).distinct().let {
                if (it.size == 1) listOf(uid, "peer_${calleeId}") else it.take(2)
            },
            callType = callType.name,
            state = CallState.RINGING.name,
            sdpOffer = sdpOffer,
            iceCandidatesJson = """[{"candidate":"candidate:1 1 UDP 2122252543 192.168.1.10 54321 typ host","sdpMid":"0","sdpMLineIndex":0}]""",
            durationSec = 0
        )
        val docRef = db.collection("calls").document(callId)
        try {
            docRef.set(session.toCreateMap()).await()
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            throw e
        }
        session
    }

    suspend fun updateCallSignalingState(
        callId: String,
        state: CallState,
        sdpAnswer: String? = null,
        durationSec: Int = 0
    ): Result<Unit> = runCatching {
        val docRef = db.collection("calls").document(callId)
        val payload = mutableMapOf<String, Any>(
            "state" to state.name,
            "durationSec" to durationSec,
            "updatedAt" to FieldValue.serverTimestamp()
        )
        if (sdpAnswer != null) {
            payload["sdpAnswer"] = sdpAnswer
        }
        try {
            docRef.update(payload).await()
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            throw e
        }
    }

    // 6. Moderation & Abuse Reports
    suspend fun submitAbuseReport(
        targetUserId: String,
        reason: String,
        details: String
    ): Result<String> = runCatching {
        val uid = requireUserId()
        val reportId = "rep_${UUID.randomUUID().toString().replace("-", "").take(12)}"
        val report = AbuseReport(
            reportId = reportId,
            reporterId = uid,
            targetUserId = targetUserId,
            reason = reason.trim(),
            details = details.trim(),
            status = "OPEN"
        )
        val docRef = db.collection("reports").document(reportId)
        try {
            docRef.set(report.toCreateMap()).await()
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            throw e
        }
        reportId
    }
}
