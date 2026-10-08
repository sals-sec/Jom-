package com.example.data.local

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Index
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow

@Entity(
    tableName = "cached_conversations",
    indices = [Index(value = ["updatedAtEpochMs"]), Index(value = ["isPinned"])]
)
data class CachedConversationEntity(
    @PrimaryKey val conversationId: String,
    val ownerId: String,
    val participantIdsCsv: String,
    val adminIdsCsv: String,
    val title: String,
    val description: String,
    val avatarUrl: String,
    val isGroup: Boolean,
    val lastMessageText: String,
    val lastMessageSenderId: String,
    val lastMessageType: String,
    val inviteLink: String,
    val onlyAdminsCanSend: Boolean,
    val onlyAdminsCanEditInfo: Boolean,
    val isPinned: Boolean,
    val isMuted: Boolean,
    val unreadCount: Int,
    val isOnline: Boolean,
    val updatedAtEpochMs: Long
)

@Entity(
    tableName = "cached_messages",
    indices = [
        Index(value = ["conversationId", "createdAtEpochMs"]),
        Index(value = ["status"]),
        Index(value = ["isStarred"]),
        Index(value = ["isPinned"])
    ]
)
data class CachedMessageEntity(
    @PrimaryKey val messageId: String,
    val conversationId: String,
    val senderId: String,
    val senderName: String,
    val participantIdsCsv: String,
    val text: String,
    val messageType: String,
    val mediaUrl: String?,
    val mediaFileName: String?,
    val mediaMimeType: String?,
    val mediaFileSize: Long?,
    val mediaDurationSec: Int?,
    val replyToMessageId: String?,
    val replyToPreview: String?,
    val isForwarded: Boolean,
    val isEdited: Boolean,
    val isDeleted: Boolean,
    val isPinned: Boolean,
    val isStarred: Boolean,
    val reactionsSummary: String,
    val status: String,
    val createdAtEpochMs: Long
)

@Entity(
    tableName = "cached_contacts",
    indices = [Index(value = ["username"]), Index(value = ["isBlocked"])]
)
data class CachedContactEntity(
    @PrimaryKey val contactId: String,
    val ownerId: String,
    val contactUserId: String,
    val username: String,
    val displayName: String,
    val phone: String,
    val avatarUrl: String,
    val bio: String,
    val isOnline: Boolean,
    val isBlocked: Boolean,
    val isFavorite: Boolean,
    val createdAtEpochMs: Long
)

@Entity(
    tableName = "cached_calls",
    indices = [Index(value = ["createdAtEpochMs"]), Index(value = ["callType"])]
)
data class CachedCallEntity(
    @PrimaryKey val callId: String,
    val callerId: String,
    val callerName: String,
    val calleeId: String,
    val calleeName: String,
    val callType: String,
    val state: String,
    val durationSec: Int,
    val networkQuality: String,
    val createdAtEpochMs: Long
)

@Dao
interface JomDao {
    @Query("SELECT * FROM cached_conversations ORDER BY isPinned DESC, updatedAtEpochMs DESC")
    fun observeConversations(): Flow<List<CachedConversationEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertConversations(items: List<CachedConversationEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertConversation(item: CachedConversationEntity)

    @Query("UPDATE cached_conversations SET isPinned = :pinned WHERE conversationId = :conversationId")
    suspend fun setConversationPinned(conversationId: String, pinned: Boolean)

    @Query("UPDATE cached_conversations SET isMuted = :muted WHERE conversationId = :conversationId")
    suspend fun setConversationMuted(conversationId: String, muted: Boolean)

    @Query("UPDATE cached_conversations SET unreadCount = :count WHERE conversationId = :conversationId")
    suspend fun setConversationUnread(conversationId: String, count: Int)

    @Query("DELETE FROM cached_conversations WHERE conversationId = :conversationId")
    suspend fun deleteConversation(conversationId: String)

    @Query("SELECT * FROM cached_messages WHERE conversationId = :conversationId ORDER BY createdAtEpochMs ASC LIMIT :limit")
    fun observeMessages(conversationId: String, limit: Int = 200): Flow<List<CachedMessageEntity>>

    @Query("SELECT * FROM cached_messages WHERE status = 'SENDING' OR status = 'FAILED' ORDER BY createdAtEpochMs ASC")
    suspend fun getPendingOfflineMessages(): List<CachedMessageEntity>

    @Query("SELECT * FROM cached_messages WHERE text LIKE '%' || :query || '%' OR mediaFileName LIKE '%' || :query || '%' ORDER BY createdAtEpochMs DESC LIMIT 50")
    suspend fun searchMessages(query: String): List<CachedMessageEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertMessages(items: List<CachedMessageEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertMessage(item: CachedMessageEntity)

    @Query("DELETE FROM cached_messages WHERE messageId = :messageId")
    suspend fun deleteMessage(messageId: String)

    @Query("SELECT * FROM cached_contacts ORDER BY isFavorite DESC, displayName ASC")
    fun observeContacts(): Flow<List<CachedContactEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertContacts(items: List<CachedContactEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertContact(item: CachedContactEntity)

    @Query("DELETE FROM cached_contacts WHERE contactId = :contactId")
    suspend fun deleteContact(contactId: String)

    @Query("SELECT * FROM cached_calls ORDER BY createdAtEpochMs DESC")
    fun observeCalls(): Flow<List<CachedCallEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertCalls(items: List<CachedCallEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertCall(item: CachedCallEntity)

    @Query("DELETE FROM cached_conversations")
    suspend fun clearAllConversations()

    @Query("DELETE FROM cached_messages")
    suspend fun clearAllMessages()

    @Query("DELETE FROM cached_contacts")
    suspend fun clearAllContacts()

    @Query("DELETE FROM cached_calls")
    suspend fun clearAllCalls()
}

@Database(
    entities = [
        CachedConversationEntity::class,
        CachedMessageEntity::class,
        CachedContactEntity::class,
        CachedCallEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class JomDatabase : RoomDatabase() {
    abstract fun jomDao(): JomDao

    companion object {
        @Volatile
        private var INSTANCE: JomDatabase? = null

        fun getInstance(context: Context): JomDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    JomDatabase::class.java,
                    "jom_local_cache.db"
                ).fallbackToDestructiveMigration().build().also { INSTANCE = it }
            }
        }
    }
}
