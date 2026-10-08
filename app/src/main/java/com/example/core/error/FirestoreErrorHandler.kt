package com.example.core.error

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import org.json.JSONArray
import org.json.JSONObject

enum class OperationType(val value: String) {
    CREATE("create"),
    UPDATE("update"),
    DELETE("delete"),
    LIST("list"),
    GET("get"),
    WRITE("write"),
}

fun handleFirestoreError(exception: Exception, operationType: OperationType, path: String?): String {
    val auth = try {
        FirebaseAuth.getInstance()
    } catch (e: Exception) {
        null
    }
    val currentUser = auth?.currentUser

    val providerInfoList = currentUser?.providerData?.map { provider ->
        JSONObject().apply {
            put("providerId", provider.providerId)
            put("email", provider.email)
        }
    } ?: emptyList()

    val authInfoJson = JSONObject().apply {
        put("userId", currentUser?.uid)
        put("email", currentUser?.email)
        put("emailVerified", currentUser?.isEmailVerified)
        put("tenantId", currentUser?.tenantId)
        put("providerInfo", JSONArray(providerInfoList))
    }

    val errorInfoJson = JSONObject().apply {
        put("error", exception.message ?: exception.toString())
        put("operationType", operationType.value)
        put("path", path)
        put("authInfo", authInfoJson)
    }

    val jsonString = errorInfoJson.toString()
    Log.e("FirestoreError", "Firestore Error: $jsonString")
    return jsonString
}

sealed interface AppError {
    val userMessage: String

    data class NoInternet(override val userMessage: String = "No internet connection. Your messages are queued offline and will sync automatically.") : AppError
    data class ServerUnavailable(override val userMessage: String = "Jom! servers are temporarily unreachable. Retrying shortly.") : AppError
    data class AuthFailure(override val userMessage: String = "Authentication failed. Please sign in with Google again.") : AppError
    data class UploadFailure(override val userMessage: String = "Media upload interrupted. Tap retry to resume.") : AppError
    data class CallConnectionFailure(override val userMessage: String = "Unable to establish WebRTC peer connection. Checking ICE/TURN relay.") : AppError
    data class PermissionDenied(override val userMessage: String = "Required hardware permission was denied. Please grant access in Settings.") : AppError
    data class UnsupportedFile(override val userMessage: String = "Unsupported file type. Supported: JPG, PNG, WEBP, GIF, MP4, PDF, DOCX, XLSX, PPTX, TXT, ZIP.") : AppError
    data class RateLimitExceeded(override val userMessage: String = "Anti-abuse protection triggered: sending too fast. Please wait a moment.") : AppError
    data class Unknown(override val userMessage: String) : AppError
}
