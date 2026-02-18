package com.emergencyringer.app

import android.app.Notification
import android.os.Build
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
        
        // Track service connection status
        @Volatile
        var isServiceConnected = false
            private set

        private val MONITORED_PACKAGES = setOf(
            "com.whatsapp",
            "com.whatsapp.w4b",  // WhatsApp Business
            "com.android.server.telecom",
            "com.android.dialer",
            "com.google.android.dialer",
            "com.samsung.android.dialer",
            "com.samsung.android.incallui",
            "com.android.incallui",
            "com.android.phone",
            "com.oneplus.dialer",
            "com.asus.contacts",
            "com.huawei.contacts",
            "com.xiaomi.incallui",
            "android"
        )
        
        // Track last triggered call to prevent re-triggering
        @Volatile
        private var lastTriggeredCaller: String? = null
        @Volatile
        private var lastTriggerTime: Long = 0
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        isServiceConnected = true
        Log.i(TAG, "✅ NotificationListenerService CONNECTED")
        AppLog.log("✅ SERVICE CONNECTED - listening for calls!", applicationContext)
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        isServiceConnected = false
        Log.w(TAG, "⚠️ NotificationListenerService DISCONNECTED")
        AppLog.log("⚠️ SERVICE DISCONNECTED - re-enable in Settings!", applicationContext)
        
        // Try to rebind
        requestRebind(android.content.ComponentName(this, NotificationService::class.java))
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        super.onNotificationRemoved(sbn)
        if (sbn == null) return
        
        val pkg = sbn.packageName ?: return
        
        // Only process monitored packages (phone/WhatsApp call notifications)
        if (pkg !in MONITORED_PACKAGES) return
        
        val notification = sbn.notification ?: return
        
        // Check if this was a call notification
        val isCategoryCall = notification.category == Notification.CATEGORY_CALL
        val extras = notification.extras
        val templateClass = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            extras?.getString(Notification.EXTRA_TEMPLATE)
        } else null
        val isCallStyle = templateClass?.contains("Call", ignoreCase = true) == true
        
        if (isCategoryCall || isCallStyle) {
            Log.i(TAG, "📵 Call notification removed - stopping ringer")
            AppLog.log("📵 Call ended - stopping ringer", applicationContext)
            
            // Stop the emergency ringer automatically
            RingerManager.stopCurrentRinger()
            
            // Clear the tracking so a new call can trigger again
            lastTriggeredCaller = null
            lastTriggerTime = 0
        }
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        if (sbn == null) return
        val pkg = sbn.packageName ?: return

        // Log ALL notifications for debugging (helps identify the right package)
        AppLog.log("📥 [$pkg]", applicationContext)
        
        if (pkg !in MONITORED_PACKAGES) {
            if (pkg.contains("phone", true) || pkg.contains("call", true) || pkg.contains("dialer", true) || pkg.contains("telecom", true) || pkg.contains("whatsapp", true)) {
                Log.i(TAG, "⚠️  Unmonitored package (add if needed): $pkg")
                AppLog.log("⚠️ Add this package?: $pkg", applicationContext)
            }
            return
        }

        val notification = sbn.notification ?: return
        val extras = notification.extras ?: return

        // DEBUG: Log ALL extras to see actual notification structure on this device
        val allText = buildString {
            extras.keySet().forEach { key ->
                val v = extras.get(key)
                if (v is CharSequence) append("[$key]=$v ")
            }
        }
        Log.i(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        Log.i(TAG, "📱 PKG=$pkg | Category=${notification.category}")
        Log.i(TAG, "📋 Extras: $allText")
        AppLog.log("📱 $pkg | Cat=${notification.category}", applicationContext)
        AppLog.log("📋 $allText", applicationContext)

        val title = extras.getCharSequence(NotificationCompat.EXTRA_TITLE)?.toString() ?: ""
        val text = extras.getCharSequence(NotificationCompat.EXTRA_TEXT)?.toString() ?: ""
        val bigText = extras.getCharSequence(NotificationCompat.EXTRA_BIG_TEXT)?.toString() ?: ""
        val subText = extras.getCharSequence(NotificationCompat.EXTRA_SUB_TEXT)?.toString() ?: ""
        val templateClass = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            notification.extras?.getString(Notification.EXTRA_TEMPLATE)
        } else null

        val combinedText = "$text $bigText $subText".trim()
        val isCategoryCall = notification.category == Notification.CATEGORY_CALL
        val isCallStyle = templateClass?.contains("Call", ignoreCase = true) == true
        val isIncomingCallText = ContactNormalizer.indicatesIncomingCall(combinedText) ||
            ContactNormalizer.indicatesIncomingCall(text) ||
            ContactNormalizer.indicatesIncomingCall(bigText)
        
        val isIncomingCall = isCategoryCall || isCallStyle || isIncomingCallText
        Log.i(TAG, "☎️  Call? cat=$isCategoryCall callStyle=$isCallStyle text=$isIncomingCallText title='$title' text='$text'")
        AppLog.log("☎️ Call? cat=$isCategoryCall text=$isIncomingCallText title='$title'", applicationContext)
        
        if (!isIncomingCall) return

        val whitelist = EmergencyContactRepository.getWhitelistedNames(applicationContext)
        if (whitelist.isEmpty()) {
            Log.w(TAG, "⚠️  Whitelist empty - add emergency contacts in app")
            AppLog.log("⚠️ Whitelist empty!", applicationContext)
            return
        }

        // Match whitelist against title OR text (caller name can be in either)
        val callerText = "$title $text $bigText $subText"
        val matches = whitelist.any { ContactNormalizer.matches(it, callerText) }
        Log.i(TAG, "🎯 Whitelist=$whitelist | Matches=$matches (checked: $callerText)")
        AppLog.log("🎯 Whitelist=$whitelist | Match=$matches", applicationContext)
        
        if (!matches) return

        // Check if monitoring is enabled
        if (!EmergencyContactRepository.isMonitoringEnabled(applicationContext)) {
            Log.i(TAG, "⏸️ Monitoring disabled by user - skipping emergency ringer")
            AppLog.log("⏸️ Monitoring disabled - ringer not triggered", applicationContext)
            return
        }
        
        // Prevent re-triggering for the same caller within 30 seconds
        // (avoids restart when "call ended" notification appears)
        val currentTime = System.currentTimeMillis()
        if (lastTriggeredCaller == callerText && (currentTime - lastTriggerTime) < 30_000) {
            Log.i(TAG, "⏭️ Skipping - same caller triggered recently (prevents restart)")
            AppLog.log("⏭️ Skip - already triggered for this call", applicationContext)
            return
        }

        Log.i(TAG, "🚨 EMERGENCY CALL - triggering ringer for: $title")
        AppLog.log("🚨 EMERGENCY - triggering ringer for: $title", applicationContext)
        
        // Update tracking before triggering
        lastTriggeredCaller = callerText
        lastTriggerTime = currentTime
        
        RingerManager.triggerEmergencyRinger(applicationContext)
    }
}
