package com.emergencyringer.app

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import androidx.core.app.NotificationCompat

/**
 * NotificationListenerService that intercepts incoming call notifications
 * from WhatsApp and the Phone app. When a whitelisted contact matches,
 * triggers the Loudness Protocol via RingerManager.
 */
class NotificationService : NotificationListenerService() {

    companion object {
        private const val TAG = "EmergencyRinger"

        private val MONITORED_PACKAGES = setOf(
            "com.whatsapp",
            "com.android.server.telecom",
            "com.android.dialer",
            "android"
        )
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        if (sbn == null) return
        val pkg = sbn.packageName ?: return
        if (pkg !in MONITORED_PACKAGES) return

        val notification = sbn.notification ?: return
        val extras = notification.extras ?: return

        val title = extras.getCharSequence(NotificationCompat.EXTRA_TITLE)?.toString() ?: ""
        val text = extras.getCharSequence(NotificationCompat.EXTRA_TEXT)?.toString()
        val bigText = extras.getCharSequence(NotificationCompat.EXTRA_BIG_TEXT)?.toString()
        val subText = extras.getCharSequence(NotificationCompat.EXTRA_SUB_TEXT)?.toString()
        val combinedText = buildString {
            text?.let { append(it).append(" ") }
            bigText?.let { append(it).append(" ") }
            subText?.let { append(it) }
        }

        if (!ContactNormalizer.indicatesIncomingCall(combinedText) &&
            !ContactNormalizer.indicatesIncomingCall(text) &&
            !ContactNormalizer.indicatesIncomingCall(bigText)) {
            return
        }

        val whitelist = EmergencyContactRepository.getWhitelistedNames(applicationContext)
        if (whitelist.isEmpty()) return

        val matches = whitelist.any { ContactNormalizer.matches(it, title) }
        if (!matches) return

        Log.i(TAG, "Emergency call detected from whitelisted contact: $title")
        RingerManager.triggerEmergencyRinger(applicationContext)
    }
}
