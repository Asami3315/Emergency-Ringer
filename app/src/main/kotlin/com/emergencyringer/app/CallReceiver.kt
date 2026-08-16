package com.emergencyringer.app

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.telephony.TelephonyManager
import android.util.Log
import android.media.AudioManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Standalone BroadcastReceiver for PHONE_STATE changes (regular phone calls only).
 *
 * Fires for every call, independent of NotificationListenerService.
 * For WhatsApp/VoIP calls, onNotificationPosted handles them since
 * those don't go through TelephonyManager.
 */
class CallReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "EmergencyRinger"
        private var lastReceiverTriggerTime = 0L
        private const val TRIGGER_COOLDOWN_MS = 10_000L
    }

    @Suppress("DEPRECATION")
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != TelephonyManager.ACTION_PHONE_STATE_CHANGED) return

        val state = intent.getStringExtra(TelephonyManager.EXTRA_STATE) ?: return
        val incomingNumber = intent.getStringExtra(TelephonyManager.EXTRA_INCOMING_NUMBER) ?: ""
        AppLog.log("📡 [CallReceiver] state=$state num='$incomingNumber'", context)
        Log.i(TAG, "📡 [CallReceiver] state=$state num='$incomingNumber'")

        when (state) {
            TelephonyManager.EXTRA_STATE_RINGING -> {
                if (!EmergencyContactRepository.isCurrentlyActive(context)) {
                    AppLog.log("⏸️ [CallReceiver] Monitoring disabled or outside schedule", context)
                    return
                }

                val am = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
                val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as? android.app.NotificationManager
                val isDnd = nm?.currentInterruptionFilter != android.app.NotificationManager.INTERRUPTION_FILTER_ALL
                val isSilent = am?.ringerMode != AudioManager.RINGER_MODE_NORMAL
                if (!isDnd && !isSilent) {
                    AppLog.log("⏸️ [CallReceiver] Phone is not in Silent/DND, skipping alarm", context)
                    return
                }

                // Cooldown to prevent double-trigger
                val now = System.currentTimeMillis()
                if (now - lastReceiverTriggerTime < TRIGGER_COOLDOWN_MS) {
                    AppLog.log("⏭️ [CallReceiver] Cooldown active", context)
                    return
                }

                val whitelist = EmergencyContactRepository.getWhitelistSync(context)
                if (whitelist.isEmpty()) {
                    AppLog.log("📋 [CallReceiver] No emergency contacts set", context)
                    return
                }

                if (!RingerManager.isPhoneSilentOrDnd(context)) {
                    AppLog.log("⏸️ [CallReceiver] Phone not in Silent/DND, skipping emergency ringer", context)
                    return
                }

                // ── INSTANT MATCH: Check phone number against whitelist ──
                if (incomingNumber.isNotEmpty()) {
                    val normalizedIncoming = ContactNormalizer.normalizePhone(incomingNumber)
                    val matchedContact = whitelist.firstOrNull { contact ->
                        val normalizedWhitelist = ContactNormalizer.normalizePhone(contact.number)
                        normalizedIncoming.isNotEmpty() && normalizedWhitelist.isNotEmpty()
                            && (normalizedIncoming == normalizedWhitelist
                                || normalizedIncoming.endsWith(normalizedWhitelist)
                                || normalizedWhitelist.endsWith(normalizedIncoming))
                    }
                    if (matchedContact != null) {
                        AppLog.log("🚨 [CallReceiver] MATCH: ${matchedContact.name} — triggering NOW!", context)
                        lastReceiverTriggerTime = System.currentTimeMillis()
                        EmergencyContactRepository.addTriggerRecord(matchedContact.name, "Emergency Contact (call)", context)
                        RingerManager.triggerEmergencyRinger(context)
                        return
                    } else {
                        AppLog.log("📋 [CallReceiver] No number match for $normalizedIncoming", context)
                    }
                }

                // ── REPEATED CALLER TRACKING (Fallback) ──
                // If NotificationListenerService matched by name, it fires first.
                // Otherwise we track the number for repeated calls here.
                if (incomingNumber.isNotEmpty()) {
                    val callCount = EmergencyContactRepository.recordIncomingCall(context, incomingNumber)
                    val threshold = EmergencyContactRepository.getRepeatedCallThreshold(context)
                    if (callCount >= threshold) {
                        CoroutineScope(Dispatchers.Default).launch {
                            delay(4_000) // Give NotificationService a chance to trigger first with better name
                            if (!EmergencyContactRepository.isRingerPlaying
                                && EmergencyContactRepository.isCurrentlyActive(context)) {
                                AppLog.log("🚨 [CallReceiver] REPEATED CALLER $callCount×: $incomingNumber", context)
                                lastReceiverTriggerTime = System.currentTimeMillis()
                                val priors = EmergencyContactRepository.getRecentCallTimestamps(context, incomingNumber)
                                EmergencyContactRepository.addTriggerRecord(incomingNumber, "Repeated Caller (${callCount}×)", context, priors)
                                EmergencyContactRepository.resetCallCount(incomingNumber)
                                RingerManager.triggerEmergencyRinger(context)
                            }
                        }
                    } else {
                        AppLog.log("🔁 [CallReceiver] Repeated caller count for $incomingNumber: $callCount/$threshold", context)
                    }
                } else {
                    AppLog.log("📋 [CallReceiver] Empty incoming number, skipping repeated caller check", context)
                }
            }

            TelephonyManager.EXTRA_STATE_IDLE -> {
                if (EmergencyContactRepository.isRingerPlaying) {
                    AppLog.log("📵 [CallReceiver] IDLE — stopping ringer", context)
                    RingerManager.stopCurrentRinger(context)
                }
            }

            TelephonyManager.EXTRA_STATE_OFFHOOK -> {
                if (EmergencyContactRepository.isRingerPlaying) {
                    AppLog.log("📞 [CallReceiver] OFFHOOK — stopping ringer", context)
                    RingerManager.stopCurrentRinger(context)
                }
            }
        }
    }
}
