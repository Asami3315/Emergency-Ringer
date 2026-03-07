package com.emergencyringer.app

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.telephony.TelephonyManager
import android.util.Log
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
                if (!EmergencyContactRepository.isMonitoringEnabled(context)) {
                    AppLog.log("⏸️ [CallReceiver] Monitoring disabled", context)
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

                // ── FALLBACK: Wait 4s for notification to trigger via name ──
                // If NotificationListenerService matched by name, it fires first.
                // Otherwise we trigger as fallback for any incoming call when contacts are set.
                CoroutineScope(Dispatchers.Default).launch {
                    delay(4_000)
                    if (!EmergencyContactRepository.isRingerPlaying
                        && EmergencyContactRepository.isMonitoringEnabled(context)) {
                        AppLog.log("⚡ [CallReceiver] 4s fallback trigger — name match didn't fire!", context)
                        lastReceiverTriggerTime = System.currentTimeMillis()
                        RingerManager.triggerEmergencyRinger(context)
                    }
                }
            }

            TelephonyManager.EXTRA_STATE_IDLE -> {
                if (EmergencyContactRepository.isRingerPlaying) {
                    AppLog.log("📵 [CallReceiver] IDLE — stopping ringer", context)
                    RingerManager.stopCurrentRinger()
                    RingerManager.restoreAudioState(context)
                }
            }

            TelephonyManager.EXTRA_STATE_OFFHOOK -> {
                if (EmergencyContactRepository.isRingerPlaying) {
                    AppLog.log("📞 [CallReceiver] OFFHOOK — stopping ringer", context)
                    RingerManager.stopCurrentRinger()
                    RingerManager.restoreAudioState(context)
                }
            }
        }
    }
}
