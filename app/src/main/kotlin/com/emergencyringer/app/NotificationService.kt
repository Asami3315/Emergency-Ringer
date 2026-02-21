package com.emergencyringer.app

import android.app.Notification
import android.os.Build
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.telephony.PhoneStateListener
import android.telephony.TelephonyCallback
import android.telephony.TelephonyManager
import android.util.Log
import androidx.core.app.NotificationCompat

/**
 * NotificationListenerService that intercepts incoming call notifications
 * from WhatsApp and the Phone app. When a whitelisted contact matches,
 * triggers the Loudness Protocol via RingerManager.
 *
 * Also monitors phone call state via TelephonyManager to automatically
 * stop the ringer when the call ends.
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
        
        // Track if we triggered the ringer (so we know to auto-stop)
        @Volatile
        var ringerWasTriggered = false
    }
    
    // Phone state listener for call end detection
    private var phoneStateListener: PhoneStateListener? = null
    private var telephonyCallback: Any? = null  // TelephonyCallback for API 31+

    override fun onListenerConnected() {
        super.onListenerConnected()
        isServiceConnected = true
        Log.i(TAG, "✅ NotificationListenerService CONNECTED")
        AppLog.log("✅ SERVICE CONNECTED - listening for calls!", applicationContext)
        
        // Register phone state listener for call end detection
        registerPhoneStateListener()
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        isServiceConnected = false
        Log.w(TAG, "⚠️ NotificationListenerService DISCONNECTED")
        AppLog.log("⚠️ SERVICE DISCONNECTED - re-enable in Settings!", applicationContext)
        
        // Unregister phone state listener
        unregisterPhoneStateListener()
        
        // Try to rebind
        requestRebind(android.content.ComponentName(this, NotificationService::class.java))
    }
    
    /**
     * Register TelephonyManager listener to detect when calls end.
     * This is much more reliable than onNotificationRemoved.
     */
    private fun registerPhoneStateListener() {
        try {
            // Check permission first
            val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                android.Manifest.permission.READ_BASIC_PHONE_STATE
            } else {
                android.Manifest.permission.READ_PHONE_STATE
            }
            if (checkSelfPermission(permission) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                Log.w(TAG, "⚠️ Phone state permission not granted - call end detection disabled")
                AppLog.log("⚠️ Phone permission missing - grant it in app settings!", applicationContext)
                return
            }
            
            val tm = getSystemService(TELEPHONY_SERVICE) as? TelephonyManager ?: return
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                // API 31+ uses TelephonyCallback
                val callback = object : TelephonyCallback(), TelephonyCallback.CallStateListener {
                    override fun onCallStateChanged(state: Int) {
                        handleCallStateChange(state)
                    }
                }
                tm.registerTelephonyCallback(mainExecutor, callback)
                telephonyCallback = callback
                Log.i(TAG, "📞 TelephonyCallback registered (API 31+)")
            } else {
                // Older API uses PhoneStateListener
                @Suppress("DEPRECATION")
                val listener = object : PhoneStateListener() {
                    @Deprecated("Deprecated in Java")
                    override fun onCallStateChanged(state: Int, phoneNumber: String?) {
                        handleCallStateChange(state)
                    }
                }
                @Suppress("DEPRECATION")
                tm.listen(listener, PhoneStateListener.LISTEN_CALL_STATE)
                phoneStateListener = listener
                Log.i(TAG, "📞 PhoneStateListener registered (legacy)")
            }
            AppLog.log("📞 Phone state monitoring active", applicationContext)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to register phone state listener: ${e.message}")
        }
    }
    
    private fun unregisterPhoneStateListener() {
        try {
            val tm = getSystemService(TELEPHONY_SERVICE) as? TelephonyManager ?: return
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                (telephonyCallback as? TelephonyCallback)?.let {
                    tm.unregisterTelephonyCallback(it)
                }
                telephonyCallback = null
            } else {
                phoneStateListener?.let {
                    @Suppress("DEPRECATION")
                    tm.listen(it, PhoneStateListener.LISTEN_NONE)
                }
                phoneStateListener = null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error unregistering phone state listener: ${e.message}")
        }
    }
    
    /**
     * Called when phone call state changes (IDLE, RINGING, OFFHOOK).
     * Auto-stops the ringer when the call ends (state becomes IDLE).
     */
    private fun handleCallStateChange(state: Int) {
        val stateName = when (state) {
            TelephonyManager.CALL_STATE_IDLE -> "IDLE"
            TelephonyManager.CALL_STATE_RINGING -> "RINGING"
            TelephonyManager.CALL_STATE_OFFHOOK -> "OFFHOOK"
            else -> "UNKNOWN($state)"
        }
        val ringerActive = EmergencyContactRepository.isRingerPlaying || ringerWasTriggered
        Log.i(TAG, "📞 Call state: $stateName | ringerActive=$ringerActive")
        
        // Stop alarm when:
        // - OFFHOOK = user picked up the call
        // - IDLE = call ended / rejected
        if ((state == TelephonyManager.CALL_STATE_OFFHOOK || state == TelephonyManager.CALL_STATE_IDLE)
            && ringerActive) {
            Log.i(TAG, "📵 Call $stateName - auto-stopping ringer")
            AppLog.log("📵 Call $stateName - stopping alarm", applicationContext)
            
            ringerWasTriggered = false
            RingerManager.stopCurrentRinger()
            // manualStopTime is set inside stopCurrentRinger() — blocks missed-call re-trigger
        }
    }
    
    /**
     * Fallback for MIUI/Redmi: stop alarm when the call notification is removed.
     * On many Redmi phones READ_PHONE_STATE requires a runtime grant the user may skip,
     * so onNotificationRemoved is the most reliable call-end signal.
     */
    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        if (sbn == null) return
        val pkg = sbn.packageName ?: return
        
        // Only care about monitored packages (phone/dialer/WhatsApp)
        if (pkg !in MONITORED_PACKAGES) return
        
        // Only stop if the ringer is actually playing
        val ringerActive = EmergencyContactRepository.isRingerPlaying || ringerWasTriggered
        if (!ringerActive) return
        
        Log.i(TAG, "📵 Call notification removed ($pkg) - stopping alarm")
        AppLog.log("📵 Notification removed ($pkg) - stopping alarm", applicationContext)
        
        ringerWasTriggered = false
        RingerManager.stopCurrentRinger()
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
        
        // Prevent re-triggering for the same caller within 60 seconds
        // (avoids restart when "call ended" / "missed call" notification appears)
        val currentTime = System.currentTimeMillis()
        if (lastTriggeredCaller == callerText && (currentTime - lastTriggerTime) < 60_000) {
            Log.i(TAG, "⏭️ Skipping - same caller triggered recently (prevents restart)")
            AppLog.log("⏭️ Skip - already triggered for this call", applicationContext)
            return
        }
        
        // Block re-trigger if user manually stopped alarm within last 90 seconds
        // This prevents "missed call" / "call ended" notifications from double-ringing
        val timeSinceManualStop = currentTime - EmergencyContactRepository.manualStopTime
        if (EmergencyContactRepository.manualStopTime > 0 && timeSinceManualStop < 90_000) {
            Log.i(TAG, "⏭️ Skipping - alarm was stopped ${timeSinceManualStop / 1000}s ago")
            AppLog.log("⏭️ Blocked - user stopped alarm ${timeSinceManualStop / 1000}s ago", applicationContext)
            return
        }

        Log.i(TAG, "🚨 EMERGENCY CALL - triggering ringer for: $title")
        AppLog.log("🚨 EMERGENCY - triggering ringer for: $title", applicationContext)
        
        // Update tracking before triggering
        lastTriggeredCaller = callerText
        lastTriggerTime = currentTime
        ringerWasTriggered = true
        
        RingerManager.triggerEmergencyRinger(applicationContext)
    }
}
