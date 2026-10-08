# Jom! — Production Android Messaging, Voice & Video Calling Platform

**Jom!** is a modern, secure, real-time Android messaging and voice/video calling application built with **Kotlin**, **Jetpack Compose**, **Material Design 3**, **Firebase Authentication (Google Sign-In via Jetpack Credential Manager)**, **Cloud Firestore Enterprise**, **Room SQLite Offline Cache**, **Retrofit + OkHttp WebSocket**, and a **WebRTC Signaling & TURN/STUN Engine**.

---

## 1. Architecture & Modular Package Structure

The codebase follows **Clean Architecture + MVVM** with unidirectional data flow (`StateFlow` + `collectAsStateWithLifecycle`):

* `com.example.authentication`: Google Sign-In via Jetpack `CredentialManager` (`GetSignInWithGoogleOption` & silent `GetGoogleIdOption`), session management, and `AuthScreen`.
* `com.example.chat`: One-to-one and Group messaging UI, dynamic text/voice-note composer, message reactions, replies, forwarding, editing, deletion, pinning, starring, multimedia upload progress, and in-chat search.
* `com.example.calls`: Real-time WebRTC signaling (`SDP Offer`, `SDP Answer`, `ICE Candidates`), STUN/TURN configuration, audio routing (`Speakerphone`, `Earpiece`, `Bluetooth`), Camera lens switching, Picture-in-Picture (PiP) overlay, and Voice Note recording with live waveform visualization.
* `com.example.data.local`: Room SQLite database (`JomDatabase`, `JomDao`) providing offline message caching, pending message queues, and instant local search.
* `com.example.data.repository`: `JomRepository` coordinating Cloud Firestore real-time `snapshots()` streams, Room offline caching, and structured JSON error diagnostics (`handleFirestoreError`).
* `com.example.network`: `JomNetworkManager` (Retrofit + OkHttp WebSocket hybrid gateway) and `MediaStorageService` (file validation, compression, and chunked upload progress).
* `com.example.notifications`: `JomNotificationDispatcher` with high-priority notification channels for messages and incoming calls plus lock-screen privacy modes (`Show full message`, `Show sender only`, `Hide message content`).
* `com.example.settings`: Profile editor, granular privacy visibility (`Everyone`, `Contacts`, `Nobody`), read receipts & typing toggles, dark/light theme switcher, offline queue simulator, blocked users manager, and account deletion.

---

## 2. Security & Zero-Trust Firestore Rules

* **Zero-Trust ABAC Security Rules (`firestore.rules`)**: Enforces strict authentication (`request.auth != null`), participant membership checks (`request.auth.uid in resource.data.participantIds`), schema validation blueprints (`isValidUserProfile`, `isValidConversation`, `isValidChatMessage`, `isValidCallSession`, `isValidAbuseReport`), and terminal call state locking.
* **Secrets & Environment Configuration (`.env.example` / `BuildConfig`)**: No API keys, backend URLs, or TURN credentials are hardcoded in source code. Configure `JOM_TURN_SERVER_URI`, `JOM_TURN_USERNAME`, `JOM_TURN_CREDENTIAL`, `JOM_API_BASE_URL`, and `JOM_WS_SIGNALING_URL` in the **AI Studio Secrets panel**.

---

## 3. Testing & Verification

1. **JavaScript Firestore Rules Unit Tests (`firestore.test.js`)**:
   ```bash
   FIRESTORE_EMULATOR_HOST="127.0.0.1:8085" node --test firestore.test.js
   ```
2. **Android JVM & Robolectric Integration Tests (`JomRepositoryRuleTest.kt`, `ExampleRobolectricTest.kt`)**:
   ```bash
   gradle :app:testDebugUnitTest
   ```
