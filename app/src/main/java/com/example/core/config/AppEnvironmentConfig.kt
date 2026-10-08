package com.example.core.config

import com.example.BuildConfig

object AppEnvironmentConfig {
    val apiBaseUrl: String
        get() = BuildConfig.JOM_API_BASE_URL.ifBlank { "https://api.jom-messenger.internal/v1/" }

    val wsSignalingUrl: String
        get() = BuildConfig.JOM_WS_SIGNALING_URL.ifBlank { "wss://signaling.jom-messenger.internal/ws" }

    val turnServerUri: String
        get() = BuildConfig.JOM_TURN_SERVER_URI.ifBlank { "turn:openrelay.metered.ca:80" }

    val turnUsername: String
        get() = BuildConfig.JOM_TURN_USERNAME.ifBlank { "openrelayproject" }

    val turnCredential: String
        get() = BuildConfig.JOM_TURN_CREDENTIAL.ifBlank { "openrelayproject" }

    val stunServers: List<String> = listOf(
        "stun:stun.l.google.com:19302",
        "stun:stun1.l.google.com:19302"
    )

    val supportedExtensions: Set<String> = setOf(
        "jpg", "jpeg", "png", "webp", "gif", "mp4",
        "pdf", "doc", "docx", "xls", "xlsx", "ppt", "pptx", "txt", "zip"
    )

    const val MAX_UPLOAD_SIZE_BYTES: Long = 50L * 1024L * 1024L // 50 MB
    const val RATE_LIMIT_MESSAGES_PER_10_SEC: Int = 12
}
