package com.example.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.R
import com.example.domain.model.NotificationPrivacyMode

class JomNotificationDispatcher(private val context: Context) {

    companion object {
        const val CHANNEL_MESSAGES = "jom_messages_channel"
        const val CHANNEL_CALLS = "jom_calls_channel"
    }

    init {
        createChannels()
    }

    private fun createChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return
            val msgChannel = NotificationChannel(
                CHANNEL_MESSAGES,
                "Jom! Direct & Group Messages",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Real-time notifications for new messages, mentions, and replies"
            }
            val callChannel = NotificationChannel(
                CHANNEL_CALLS,
                "Jom! Voice & Video Calls",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Incoming voice and video call alerts"
            }
            manager.createNotificationChannel(msgChannel)
            manager.createNotificationChannel(callChannel)
        }
    }

    fun showMessageNotification(
        senderName: String,
        messageText: String,
        privacyMode: NotificationPrivacyMode,
        notificationsEnabled: Boolean
    ) {
        if (!notificationsEnabled) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                return
            }
        }

        val (title, body) = when (privacyMode) {
            NotificationPrivacyMode.FULL_MESSAGE -> senderName to messageText
            NotificationPrivacyMode.SENDER_ONLY -> senderName to "Sent you a new message on Jom!"
            NotificationPrivacyMode.HIDE_ALL -> "Jom! Messenger" to "New encrypted message received"
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_MESSAGES)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(body)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(System.currentTimeMillis().toInt(), notification)
        } catch (_: SecurityException) {}
    }

    fun showIncomingCallNotification(
        callerName: String,
        isVideo: Boolean,
        notificationsEnabled: Boolean
    ) {
        if (!notificationsEnabled) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                return
            }
        }

        val callLabel = if (isVideo) "Incoming Video Call" else "Incoming Voice Call"
        val notification = NotificationCompat.Builder(context, CHANNEL_CALLS)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("$callLabel • $callerName")
            .setContentText("Tap to answer or decline in Jom!")
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .setAutoCancel(true)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(9001, notification)
        } catch (_: SecurityException) {}
    }
}
