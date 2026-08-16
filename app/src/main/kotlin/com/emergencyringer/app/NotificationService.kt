package com.emergencyringer.app

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.media.AudioAttributes
import android.content.Context
import android.media.AudioManager
import android.media.RingtoneManager
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

        // Packages that send message notifications
        private val MESSAGE_PACKAGES = setOf(
            "com.whatsapp",
            "com.whatsapp.w4b",
            "org.telegram.messenger",
            "org.telegram.plus",
            "com.facebook.orca",     // Messenger
            "com.google.android.apps.messaging", // Google Messages (SMS)
            "com.android.mms",       // Stock SMS
            "com.samsung.android.messaging",
            "com.xiaomi.msg",
            "com.huawei.mms"
        )

        // Dedup: track last message alert per sender (avoid sound from same notification repost)
        private val lastMessageAlertTime = mutableMapOf<String, Long>()
        private const val MESSAGE_ALERT_COOLDOWN_MS = 3_000L // 3 seconds — prevents same-notification duplicate only
        
        // Track last triggered call to prevent re-triggering
        @Volatile
        private var lastTriggeredCaller: String? = null
        @Volatile
        private var lastTriggerTime: Long = 0
        
        // Key of the exact notification that triggered the alarm
        // onNotificationRemoved uses this to stop ONLY the right alarm
        @Volatile
        var lastTriggerSbnKey: String? = null
        
        // Track if we triggered the ringer (so we know to auto-stop)
        @Volatile
        var ringerWasTriggered = false

        // true = phone went through RINGING (incoming call)
        // false = phone went directly to OFFHOOK (outgoing call — do NOT trigger alarm)
        @Volatile
        var isCallIncoming: Boolean = false

        @Volatile
        var isCallOutgoing: Boolean = false
    }

    // Phone state listener for call end detection
    private var phoneStateListener: PhoneStateListener? = null
    private var telephonyCallback: Any? = null  // TelephonyCallback for API 31+

    override fun onListenerConnected() {
        super.onListenerConnected()
        isServiceConnected = true
        Log.i(TAG, "✅ NotificationListenerService CONNECTED")
        AppLog.log("✅ SERVICE CONNECTED - listening for calls!", applicationContext)

        // Keep service alive on MIUI — foreground prevents the OS from killing us mid-call
        try {
            val channelId = "er_guard"
            val nm = getSystemService(NotificationManager::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val ch = NotificationChannel(channelId, "Emergency Ringer", NotificationManager.IMPORTANCE_MIN)
                ch.setShowBadge(false); ch.setSound(null, null); ch.enableVibration(false)
                nm?.createNotificationChannel(ch)
            }
            val n = android.app.Notification.Builder(this, channelId)
                .setSmallIcon(R.drawable.ic_shield_on)
                .setContentTitle("Emergency Ringer active")
                .setContentText("Monitoring for emergency calls")
                .setOngoing(true).build()
            startForeground(1001, n)
        } catch (e: Exception) {
            Log.w(TAG, "Foreground start failed (non-fatal): ${e.message}")
        }
        
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
    
    override fun onDestroy() {
        super.onDestroy()
        Log.i(TAG, "⚠️ NotificationService onDestroy called - stopping alarm if active")
        // Force stop ringer and restore audio state just in case the user force-killed the app while it was ringing!
        if (EmergencyContactRepository.isRingerPlaying || ringerWasTriggered) {
            ringerWasTriggered = false
            RingerManager.stopCurrentRinger(applicationContext)
        }
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
     * Tracks incoming vs outgoing calls and auto-stops alarm on call end.
     */
    private fun handleCallStateChange(state: Int) {
        val stateName = when (state) {
            TelephonyManager.CALL_STATE_IDLE    -> "IDLE"
            TelephonyManager.CALL_STATE_RINGING -> "RINGING"
            TelephonyManager.CALL_STATE_OFFHOOK -> "OFFHOOK"
            else -> "UNKNOWN($state)"
        }
        val ringerActive = EmergencyContactRepository.isRingerPlaying || ringerWasTriggered
        Log.i(TAG, "📞 Call state: $stateName | ringerActive=$ringerActive | incoming=$isCallIncoming")

        when (state) {
            TelephonyManager.CALL_STATE_RINGING -> {
                // Phone is ringing = this is an INCOMING call
                isCallIncoming = true
                isCallOutgoing = false
                Log.i(TAG, "📲 Incoming call detected")
            }
            TelephonyManager.CALL_STATE_OFFHOOK -> {
                // If we never went through RINGING, user is MAKING a call (outgoing)
                if (!isCallIncoming) {
                    isCallOutgoing = true
                    Log.i(TAG, "📤 Outgoing call — alarm will be suppressed")
                    AppLog.log("📤 Outgoing call — alarm suppressed", applicationContext)
                }
                // Stop alarm if it's ringing (user answered incoming call)
                if (ringerActive && isCallIncoming) {
                    Log.i(TAG, "✅ Call answered — stopping alarm")
                    AppLog.log("✅ Call answered — stopping alarm", applicationContext)
                    ringerWasTriggered = false
                    RingerManager.stopCurrentRinger(applicationContext)
                }
            }
            TelephonyManager.CALL_STATE_IDLE -> {
                // Call ended — stop alarm if it's still ringing
                if (ringerActive) {
                    Log.i(TAG, "📵 Call IDLE — stopping alarm")
                    AppLog.log("📵 Call ended — stopping alarm", applicationContext)
                    ringerWasTriggered = false
                    RingerManager.stopCurrentRinger(applicationContext)
                }
                // Reset flags ready for next call
                isCallIncoming = false
                isCallOutgoing = false
            }
        }
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        if (sbn == null) return
        val pkg = sbn.packageName ?: return

        // Log ALL notifications for debugging (helps identify the right package)
        AppLog.log("📥 [$pkg]", applicationContext)

        // ══════════════════════════════════════════════════════════════════
        // ⚡ FAST PATH — runs FIRST before anything else.
        // Detects incoming call notifications and triggers immediately.
        // 
        // CRITICAL: MIUI/HyperOS on Xiaomi/Redmi/POCO strips CATEGORY_CALL
        // from WhatsApp notifications, so we CANNOT rely on it alone.
        // We also enter the Fast Path for WhatsApp if the text looks like a call.
        // ══════════════════════════════════════════════════════════════════
        val fastNotif = sbn.notification
        val isWhatsApp = pkg.contains("whatsapp", ignoreCase = true)
        
        if (fastNotif != null) {
            val isCategoryCall = fastNotif.category == Notification.CATEGORY_CALL
            val fastExtras = fastNotif.extras
            val fastTitle = fastExtras?.getCharSequence(NotificationCompat.EXTRA_TITLE)?.toString()?.trim() ?: ""
            val fastText  = fastExtras?.getCharSequence(NotificationCompat.EXTRA_TEXT)?.toString()?.trim() ?: ""
            
            // On MIUI, WhatsApp call notifications arrive WITHOUT CATEGORY_CALL.
            // Detect them by checking if the text mentions a call.
            val textLower = fastText.lowercase()
            val titleLower = fastTitle.lowercase()
            val isWhatsAppCallByText = isWhatsApp && (
                textLower.contains("incoming") ||
                textLower.contains("voice call") ||
                textLower.contains("video call") ||
                textLower.contains("audio call") ||
                textLower.contains("call from") ||
                textLower.contains("calling") ||
                textLower.contains("ringing") ||
                textLower.contains("dialing") ||
                textLower.contains("call") ||
                // Non-English
                textLower.contains("anruf") ||
                textLower.contains("appel") ||
                textLower.contains("llamada") ||
                textLower.contains("مكالمة") ||
                textLower.contains("कॉल") ||
                textLower.contains("آنے والی")
            )
            
            val shouldEnterFastPath = isCategoryCall || isWhatsAppCallByText
            
            if (shouldEnterFastPath) {
                AppLog.log("⚡ FAST PATH: pkg=$pkg cat=${fastNotif.category} title='$fastTitle' text='$fastText'", applicationContext)
                
                // ── Outgoing Call Detection ──
                // SIMPLE: If text is EXACTLY "Calling..." / "Ringing..." / "Dialing..." it's outgoing.
                // Incoming calls say "Incoming voice call", "Incoming video call", etc.
                val hasIncomingIndicator = textLower.contains("incoming") || 
                                           textLower.contains("voice call") || 
                                           textLower.contains("video call") ||
                                           textLower.contains("audio call") ||
                                           textLower.contains("call from") ||
                                           textLower.contains("eingehender") ||
                                           textLower.contains("appel entrant") ||
                                           textLower.contains("llamada entrante") ||
                                           textLower.contains("آنے والی")
                                           
                val hasOutgoingIndicator = textLower == "calling" || textLower == "calling..." || 
                                           textLower == "ringing" || textLower == "ringing..." || 
                                           textLower == "dialing" || textLower == "dialing..." ||
                                           textLower.startsWith("dialing ")
                
                val hasGenericWhatsAppTitle = isWhatsApp && (
                    titleLower.contains("whatsapp audio call") || 
                    titleLower.contains("whatsapp video call") ||
                    titleLower.contains("whatsapp voice call")
                )
                
                val blockTelephonyOutgoing = isCallOutgoing && !isWhatsApp
                
                val isOutgoing = blockTelephonyOutgoing || 
                                 (hasOutgoingIndicator && !hasIncomingIndicator) ||
                                 hasGenericWhatsAppTitle

                if (isOutgoing) {
                    AppLog.log("📤 FAST PATH: Outgoing call blocked (text='$fastText')", applicationContext)
                    return
                }
                
                // If notification indicates ongoing/active call, stop the alarm.
                val usesChronometer = fastExtras?.getBoolean(Notification.EXTRA_SHOW_CHRONOMETER, false) ?: false
                val isOngoing = usesChronometer || 
                    textLower.contains("ongoing") || 
                    textLower.contains("active") || 
                    fastText.matches(Regex(".*\\d{2}:\\d{2}.*"))

                if (isOngoing) {
                    if (EmergencyContactRepository.isRingerPlaying) {
                        Log.i(TAG, "✅ Ongoing call detected — stopping alarm")
                        AppLog.log("✅ Ongoing call detected — stopping alarm", applicationContext)
                        ringerWasTriggered = false
                        RingerManager.stopCurrentRinger(applicationContext)
                    }
                    return
                }
                
                val isMissed = textLower.contains("missed") || titleLower.contains("missed")
                if (isMissed) {
                    if (EmergencyContactRepository.isRingerPlaying) {
                        Log.i(TAG, "📵 Missed call — stopping alarm")
                        AppLog.log("📵 Missed call (declined) — stopping alarm", applicationContext)
                        ringerWasTriggered = false
                        RingerManager.stopCurrentRinger(applicationContext)
                    }
                    return
                }
                
                // ── TRIGGER THE ALARM ──
                if (EmergencyContactRepository.isCurrentlyActive(applicationContext)) {
                    
                    if (!RingerManager.isPhoneSilentOrDnd(applicationContext)) {
                        AppLog.log("⏸️ [FastPath] Phone not in Silent/DND, skipping", applicationContext)
                        return
                    }

                    val wl = EmergencyContactRepository.getWhitelistedNames(applicationContext)
                    val hit = wl.any { ContactNormalizer.matches(it, fastTitle) || ContactNormalizer.matches(it, fastText) }
                    AppLog.log("⚡ whitelist=$wl match=$hit title='$fastTitle'", applicationContext)
                    if (hit) {
                        val now = System.currentTimeMillis()
                        if (lastTriggeredCaller == fastTitle && (now - lastTriggerTime) < 60_000 && EmergencyContactRepository.isRingerPlaying) {
                            AppLog.log("⏭️ FAST PATH: ringer already active", applicationContext)
                        } else {
                            AppLog.log("🚨 FAST PATH TRIGGER: $fastTitle", applicationContext)
                            lastTriggeredCaller = fastTitle
                            lastTriggerTime = now
                            lastTriggerSbnKey = sbn.key
                            ringerWasTriggered = true
                            isCallIncoming = true
                            EmergencyContactRepository.addTriggerRecord(fastTitle, "Emergency Contact", applicationContext)
                            RingerManager.triggerEmergencyRinger(applicationContext)
                            return
                        }
                    }
                }
            }
        }

        // Handle message notifications (separate from call notifications)
        if (pkg in MESSAGE_PACKAGES) {
            handleMessageNotification(sbn)
            // Message packages can also be call packages (WhatsApp), so fall through
            if (pkg !in MONITORED_PACKAGES) return
        } else if (pkg !in MONITORED_PACKAGES) {
            if (pkg.contains("phone", true) || pkg.contains("call", true) ||
                pkg.contains("dialer", true) || pkg.contains("telecom", true) ||
                pkg.contains("whatsapp", true)) {
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

        // Skip missed-call / call-ended notifications by category or text
        // (these arrive after the call ends and should never re-trigger the alarm)
        val isMissedCall = notification.category == android.app.Notification.CATEGORY_MISSED_CALL ||
            combinedText.contains("missed", ignoreCase = true) ||
            title.contains("missed", ignoreCase = true)
        if (isMissedCall) {
            Log.i(TAG, "⏭️ Missed-call notification detected")
            if (EmergencyContactRepository.isRingerPlaying) {
                AppLog.log("📵 Missed call (declined) — stopping alarm", applicationContext)
                ringerWasTriggered = false
                RingerManager.stopCurrentRinger(applicationContext)
            }
            return
        }

        // Block outgoing calls — CATEGORY_CALL is set on outgoing notifications too.
        // isCallIncoming is true ONLY if phone went through RINGING state first.
        // If user is making a call, isCallIncoming = false → suppress alarm.
        if (!isCallIncoming) {
            Log.i(TAG, "📤 Outgoing call notification — skipping alarm trigger")
            AppLog.log("📤 Outgoing call \u2014 alarm skipped", applicationContext)
            return
        }

        // Check if monitoring is enabled and active in schedule
        if (!EmergencyContactRepository.isCurrentlyActive(applicationContext)) {
            Log.i(TAG, "⏸️ Monitoring disabled by user or schedule - skipping emergency ringer")
            AppLog.log("⏸️ Monitoring disabled - ringer not triggered", applicationContext)
            return
        }

        // Check if answered call
        val usesChronometer = extras.getBoolean(Notification.EXTRA_SHOW_CHRONOMETER, false)
        val isOngoingCallText = combinedText.contains("ongoing", ignoreCase = true) ||
            combinedText.matches(Regex(".*\\d{2}:\\d{2}.*"))

        if (usesChronometer || isOngoingCallText) {
            Log.i(TAG, "✅ Ongoing call detected (answered) — stopping alarm")
            AppLog.log("✅ Ongoing call detected — stopping alarm", applicationContext)
            if (EmergencyContactRepository.isRingerPlaying) {
                ringerWasTriggered = false
                RingerManager.stopCurrentRinger(applicationContext)
            }
            return
        }

        val callerText = "$title $text $bigText $subText"
        val callerName = title.ifBlank { text }.trim()

        if (!RingerManager.isPhoneSilentOrDnd(applicationContext)) {
            Log.i(TAG, "⏸️ Phone not in Silent/DND, skipping normal call path")
            AppLog.log("⏸️ Phone not in Silent/DND, skipping", applicationContext)
            return
        }

        // Prevent re-triggering for the SAME call within 60 seconds ONLY if alarm is still ringing
        // (guards against the same notification being posted multiple times during one call)
        val currentTime = System.currentTimeMillis()
        if (lastTriggeredCaller == callerText
            && (currentTime - lastTriggerTime) < 60_000
            && EmergencyContactRepository.isRingerPlaying) {
            Log.i(TAG, "⏭️ Skipping - same caller, alarm already playing")
            AppLog.log("⏭️ Skip - alarm already playing for this call", applicationContext)
            return
        }

        // PATH A: Emergency contact matched → trigger immediately
        val whitelist = EmergencyContactRepository.getWhitelistedNames(applicationContext)
        val matchesWhitelist = whitelist.any { ContactNormalizer.matches(it, callerText) }
        Log.i(TAG, "🎯 Whitelist=$whitelist | Matches=$matchesWhitelist (checked: $callerText)")
        AppLog.log("🎯 Whitelist match=$matchesWhitelist caller='$callerName'", applicationContext)

        if (matchesWhitelist) {
            Log.i(TAG, "🚨 EMERGENCY CONTACT - triggering ringer for: $callerName")
            AppLog.log("🚨 EMERGENCY CONTACT: $callerName", applicationContext)
            lastTriggeredCaller = callerText
            lastTriggerTime = currentTime
            lastTriggerSbnKey = sbn.key  // store so onNotificationRemoved knows which to stop on
            ringerWasTriggered = true
            EmergencyContactRepository.addTriggerRecord(callerName, "Emergency Contact", applicationContext)
            RingerManager.triggerEmergencyRinger(applicationContext)
            return
        }

        // PATH B: Not in whitelist → track repeated calls (configured count within configured window)
        val callerKey = callerName.lowercase().trim()
        val callCount = EmergencyContactRepository.recordIncomingCall(applicationContext, callerKey)
        val threshold = EmergencyContactRepository.getRepeatedCallThreshold(applicationContext)
        Log.i(TAG, "🔁 Repeated caller '$callerKey' count=$callCount/$threshold")
        AppLog.log("🔁 '$callerName' called $callCount/$threshold times", applicationContext)

        if (callCount >= threshold) {
            Log.i(TAG, "🚨 REPEATED CALLER ($callCount×) - triggering ringer for: $callerName")
            AppLog.log("🚨 REPEATED CALLER $callCount×: $callerName", applicationContext)
            lastTriggeredCaller = callerText
            lastTriggerTime = currentTime
            lastTriggerSbnKey = sbn.key
            ringerWasTriggered = true
            val priors = EmergencyContactRepository.getRecentCallTimestamps(applicationContext, callerKey)
            EmergencyContactRepository.addTriggerRecord(callerName, "Repeated Caller (${callCount}×)", applicationContext, priors)
            // Reset count so they need 3 more calls to trigger again
            EmergencyContactRepository.resetCallCount(callerKey)
            RingerManager.triggerEmergencyRinger(applicationContext)
        }
    }

    /**
     * Stops the alarm when the EXACT notification that triggered it is removed.
     * Key guard prevents unrelated WhatsApp background notifications from stopping the alarm.
     */
    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        if (sbn == null) return
        val ringerActive = EmergencyContactRepository.isRingerPlaying || ringerWasTriggered
        if (!ringerActive) return
        
        // ONLY stop if it's the EXACT notification that triggered the alarm.
        // We do NOT check packageName because WhatsApp frequently posts and removes
        // background/message notifications while a call is ringing, which would kill the alarm prematurely.
        if (sbn.key == lastTriggerSbnKey && lastTriggerSbnKey != null) {
            Log.i(TAG, "📵 Trigger notification removed (${sbn.packageName}) - stopping alarm")
            AppLog.log("📵 Notification removed - stopping alarm", applicationContext)
            lastTriggerSbnKey = null
            ringerWasTriggered = false
            RingerManager.stopCurrentRinger(applicationContext)
        }
    }

    /**
     * Checks if a message notification is from an emergency contact.
     * If so, and message alerts are enabled, plays the default notification sound.
     */
    private fun handleMessageNotification(sbn: StatusBarNotification) {
        // Feature must be enabled by user and active in schedule
        if (!EmergencyContactRepository.isMessageAlertEnabled(applicationContext)) return
        if (!EmergencyContactRepository.isCurrentlyActive(applicationContext)) return

        val notification = sbn.notification ?: return
        val extras = notification.extras ?: return

        // Skip call-type and missed-call notifications entirely
        val isCall = notification.category == Notification.CATEGORY_CALL ||
            notification.category == Notification.CATEGORY_MISSED_CALL
        if (isCall) return

        // Must be a message/email category OR have text — skip generic system notifications
        val isMessage = notification.category == Notification.CATEGORY_MESSAGE ||
            notification.category == Notification.CATEGORY_EMAIL ||
            notification.category == null
        if (!isMessage) return

        // Extract sender name — try conversation title first, then regular title
        val title     = extras.getCharSequence(Notification.EXTRA_CONVERSATION_TITLE)?.toString()
            ?: extras.getCharSequence(NotificationCompat.EXTRA_TITLE)?.toString() ?: ""
        val text      = extras.getCharSequence(NotificationCompat.EXTRA_TEXT)?.toString() ?: ""
        val combined  = "$title $text"

        Log.i(TAG, "💬 MSG pkg=${sbn.packageName} cat=${notification.category} title='$title' text='$text'")
        AppLog.log("💬 MSG title='$title' text='$text'", applicationContext)

        if (!RingerManager.isPhoneSilentOrDnd(applicationContext)) {
            AppLog.log("⏸️ Phone not in Silent/DND, skipping message alert", applicationContext)
            return
        }

        // Match whitelist against combined sender info
        val whitelist = EmergencyContactRepository.getWhitelistedNames(applicationContext)
        val matchesWhitelist = whitelist.any {
            ContactNormalizer.matches(it, title) || ContactNormalizer.matches(it, combined)
        }
        Log.i(TAG, "💬 Whitelist=$whitelist matchesMsg=$matchesWhitelist")
        AppLog.log("💬 Whitelist match=$matchesWhitelist for '$title'", applicationContext)
        if (!matchesWhitelist) return

        // 30-second cooldown per sender — avoids sound spam from rapid messages
        val now = System.currentTimeMillis()
        val lastAlert = lastMessageAlertTime[title] ?: 0L
        if (now - lastAlert < MESSAGE_ALERT_COOLDOWN_MS) {
            Log.i(TAG, "⏭️ Message alert cooldown for: $title")
            return
        }
        lastMessageAlertTime[title] = now

        Log.i(TAG, "� MESSAGE ALERT: emergency contact '$title' sent a message")
        AppLog.log("� Message alert: $title", applicationContext)
        EmergencyContactRepository.addTriggerRecord(title, "Message from contact", applicationContext)
        playMessageAlert()
    }

    /** Plays the phone's default notification sound through the ALARM stream (bypasses DND). */
    private fun playMessageAlert() {
        try {
            val am = applicationContext.getSystemService(AUDIO_SERVICE) as AudioManager
            // Use STREAM_ALARM — it bypasses DND on all Android versions including MIUI
            val maxVol = am.getStreamMaxVolume(AudioManager.STREAM_ALARM)
            am.setStreamVolume(AudioManager.STREAM_ALARM, maxVol, 0)

            // Play notification ringtone through the alarm audio path
            val uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            val rt  = RingtoneManager.getRingtone(applicationContext, uri)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                rt?.audioAttributes = AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)          // DND bypass
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            }
            rt?.play()
            Log.i(TAG, "🔔 Message alert sound played (ALARM stream)")
        } catch (e: Exception) {
            Log.e(TAG, "❌ playMessageAlert failed: ${e.message}")
        }
    }
}
