package com.example.data.repository

import com.example.base.FirestoreEmulatorTestBase
import com.example.domain.model.CallType
import com.example.domain.model.MessageType
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class JomRepositoryRuleTest : FirestoreEmulatorTestBase() {

    @Test
    fun upsertUserProfile_validPayload_succeedsAndReadsBack() = runBlocking {
        val aliceUid = signInTestUser(ALICE_EMAIL)
        val repository = JomRepository(firestore)

        val result = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.upsertUserProfile(
                username = "alice_jom",
                displayName = "Alice Wonder",
                bio = "Security Architect at Jom!"
            )
        }
        assertTrue(result.isSuccess)
        val profile = result.getOrThrow()
        assertEquals(aliceUid, profile.userId)
        assertEquals("alice_jom", profile.username)
    }

    @Test
    fun createConversationAndSendMessage_authenticatedParticipant_emitsRealtimeUpdates() = runBlocking {
        val aliceUid = signInTestUser(ALICE_EMAIL)
        val repository = JomRepository(firestore)

        val convId = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.createConversation(
                title = "Core Engineering Team",
                participantIds = listOf(aliceUid, "bob_uid_999"),
                isGroup = true,
                description = "Architecture discussion"
            ).getOrThrow()
        }
        assertTrue(convId.isNotEmpty())

        val msgId = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.sendMessage(
                conversationId = convId,
                participantIds = listOf(aliceUid, "bob_uid_999"),
                senderName = "Alice",
                text = "Deploying WebRTC signaling and TURN relay!",
                messageType = MessageType.TEXT
            ).getOrThrow()
        }
        assertTrue(msgId.isNotEmpty())

        val emittedMessages = withTimeout(FLOW_TIMEOUT_MS) {
            repository.observeMessages(convId).first { list -> list.any { it.messageId == msgId } }
        }
        assertTrue(emittedMessages.any { it.messageId == msgId })
    }

    @Test
    fun getConversation_crossUserNonParticipantAccess_failsWithPermissionDenied() = runBlocking {
        val aliceUid = signInTestUser(ALICE_EMAIL)
        val aliceRepo = JomRepository(firestore)
        val secretConvId = withTimeout(DEFAULT_TIMEOUT_MS) {
            aliceRepo.createConversation(
                title = "Alice Private Vault",
                participantIds = listOf(aliceUid),
                isGroup = false
            ).getOrThrow()
        }

        // Switch to Bob who is NOT in participantIds
        signInTestUser(BOB_EMAIL)
        val bobRepo = JomRepository(firestore)
        val bobResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            bobRepo.getConversationById(secretConvId)
        }
        assertTrue(bobResult.isFailure)
        val exception = bobResult.exceptionOrNull() as? FirebaseFirestoreException
        assertNotNull(exception)
        assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, exception?.code)
    }

    @Test
    fun observeConversations_unauthenticatedUser_failsWithPermissionDenied() = runBlocking {
        auth.signOut()
        val repository = JomRepository(firestore)

        try {
            withTimeout(FLOW_TIMEOUT_MS) {
                repository.observeConversations().first()
            }
            fail("Expected FirebaseFirestoreException PERMISSION_DENIED for unauthenticated caller")
        } catch (e: Throwable) {
            var current: Throwable? = e
            var firestoreEx: FirebaseFirestoreException? = null
            while (current != null) {
                if (current is FirebaseFirestoreException) {
                    firestoreEx = current
                    break
                }
                current = current.cause
            }
            assertNotNull("Expected FirebaseFirestoreException in cause chain, got $e", firestoreEx)
            assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, firestoreEx?.code)
        }
    }

    @Test
    fun startCallSession_validWebRtcOffer_createsRingingSession() = runBlocking {
        signInTestUser(ALICE_EMAIL)
        val repository = JomRepository(firestore)

        val callResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.startCallSession(
                callerName = "Alice",
                calleeId = "bob_peer_01",
                calleeName = "Bob",
                callType = CallType.VIDEO,
                sdpOffer = "v=0\r\no=- 4611731400430051336 2 IN IP4 127.0.0.1\r\ns=-\r\nt=0 0\r\n"
            )
        }
        assertTrue(callResult.isSuccess)
        assertEquals("RINGING", callResult.getOrThrow().state)
    }

    private companion object {
        const val ALICE_EMAIL = "alice_jom@test.com"
        const val BOB_EMAIL = "bob_jom@test.com"
        const val DEFAULT_TIMEOUT_MS = 5000L
        const val FLOW_TIMEOUT_MS = 3500L
    }
}
