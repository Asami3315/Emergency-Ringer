package com.emergencyringer.app

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.app.NotificationManager
import android.util.Log

/**
 * Overrides silent/DND and plays alarm sound.
 * - Bypasses DND, sets ringer to normal
 * - Plays alarm sound (USAGE_ALARM bypasses silent on most devices)
 */
object RingerManager {

    private const val TAG = "EmergencyRinger"
    private const val AUTO_STOP_DELAY_MS = 30_000L  // 30 seconds

    @Volatile
    private var mediaPlayer: MediaPlayer? = null
    
    private var stopHandler: Handler? = null
    private var stopRunnable: Runnable? = null

    fun triggerEmergencyRinger(context: Context) {
        Log.i(TAG, "═══════════════════════════════════════")
        Log.i(TAG, "EMERGENCY RINGER TRIGGERED!")
        Log.i(TAG, "═══════════════════════════════════════")
        AppLog.log("═══════════════════════════════════════", context)
        AppLog.log("🔔 EMERGENCY RINGER TRIGGERED!", context)
        
        try {
            val am = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager

            if (am == null) {
                Log.e(TAG, "❌ AudioManager is null!")
                AppLog.log("❌ AudioManager is null!", context)
                return
            }

            // ══════════════════════════════════════
            // LOG CURRENT STATE (BEFORE CHANGES)
            // ══════════════════════════════════════
            val currentRingerMode = am.ringerMode
            val ringerModeName = when (currentRingerMode) {
                AudioManager.RINGER_MODE_SILENT -> "SILENT"
                AudioManager.RINGER_MODE_VIBRATE -> "VIBRATE"
                AudioManager.RINGER_MODE_NORMAL -> "NORMAL"
                else -> "UNKNOWN"
            }
            val currentVolume = am.getStreamVolume(AudioManager.STREAM_RING)
            val maxVolume = am.getStreamMaxVolume(AudioManager.STREAM_RING)
            
            AppLog.log("📊 BEFORE: Ringer=$ringerModeName, Vol=$currentVolume/$maxVolume", context)
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                val currentFilter = nm?.currentInterruptionFilter ?: -1
                val filterName = when (currentFilter) {
                    NotificationManager.INTERRUPTION_FILTER_NONE -> "TOTAL SILENCE"
                    NotificationManager.INTERRUPTION_FILTER_PRIORITY -> "DND (Priority)"
                    NotificationManager.INTERRUPTION_FILTER_ALARMS -> "DND (Alarms)"
                    NotificationManager.INTERRUPTION_FILTER_ALL -> "OFF (Normal)"
                    else -> "UNKNOWN($currentFilter)"
                }
                val hasAccess = nm?.isNotificationPolicyAccessGranted == true
                AppLog.log("📵 DND Status: $filterName | Access=$hasAccess", context)
                Log.i(TAG, "📵 DND=$filterName Access=$hasAccess")
            }

            // ══════════════════════════════════════
            // STEP 1: Bypass DND (CRITICAL FIRST STEP)
            // ══════════════════════════════════════
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (nm?.isNotificationPolicyAccessGranted == true) {
                    AppLog.log("🚨 Disabling DND...", context)
                    nm.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_ALL)
                    
                    // Small delay to let the system process
                    Thread.sleep(50)
                    
                    // Verify it worked
                    val newFilter = nm.currentInterruptionFilter
                    if (newFilter == NotificationManager.INTERRUPTION_FILTER_ALL) {
                        AppLog.log("✅ DND DISABLED!", context)
                        Log.i(TAG, "✅ DND DISABLED!")
                    } else {
                        val newFilterName = when (newFilter) {
                            NotificationManager.INTERRUPTION_FILTER_NONE -> "TOTAL SILENCE"
                            NotificationManager.INTERRUPTION_FILTER_PRIORITY -> "DND (Priority)"
                            NotificationManager.INTERRUPTION_FILTER_ALARMS -> "DND (Alarms)"
                            else -> "UNKNOWN($newFilter)"
                        }
                        AppLog.log("❌ DND STILL ON: $newFilterName", context)
                        Log.e(TAG, "❌ DND STILL ACTIVE: $newFilter")
                        
                        // Try once more
                        nm.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_ALL)
                    }
                } else {
                    AppLog.log("❌ NO DND ACCESS! Go to Settings > Apps > Emergency Ringer > Notifications > Do Not Disturb access", context)
                    Log.e(TAG, "❌ NO DND ACCESS!")
                }
            }

            // ══════════════════════════════════════
            // STEP 2: Unmute and set ringer mode
            // ══════════════════════════════════════
            AppLog.log("🔔 Setting ringer to NORMAL...", context)
            am.adjustVolume(AudioManager.ADJUST_UNMUTE, 0)
            am.adjustStreamVolume(AudioManager.STREAM_RING, AudioManager.ADJUST_UNMUTE, 0)
            am.ringerMode = AudioManager.RINGER_MODE_NORMAL

            // Verify ringer mode changed
            val newRingerMode = am.ringerMode
            val newRingerName = when (newRingerMode) {
                AudioManager.RINGER_MODE_SILENT -> "SILENT"
                AudioManager.RINGER_MODE_VIBRATE -> "VIBRATE"
                AudioManager.RINGER_MODE_NORMAL -> "NORMAL"
                else -> "UNKNOWN"
            }
            if (newRingerMode == AudioManager.RINGER_MODE_NORMAL) {
                AppLog.log("✅ Ringer mode: $newRingerName", context)
            } else {
                AppLog.log("❌ Ringer still: $newRingerName", context)
            }

            // ══════════════════════════════════════
            // STEP 3: Crank volume to maximum
            // ══════════════════════════════════════
            AppLog.log("📢 Setting volume to MAX...", context)
            
            val flags = AudioManager.FLAG_ALLOW_RINGER_MODES or
                AudioManager.FLAG_PLAY_SOUND or
                AudioManager.FLAG_VIBRATE

            // Set RING volume
            val maxRinger = am.getStreamMaxVolume(AudioManager.STREAM_RING)
            am.setStreamVolume(AudioManager.STREAM_RING, maxRinger, flags)
            val newRingVol = am.getStreamVolume(AudioManager.STREAM_RING)
            AppLog.log("🔊 Ring Vol: $newRingVol/$maxRinger ${if (newRingVol == maxRinger) "✅" else "❌"}", context)

            // Set VOICE CALL volume
            val maxVoice = am.getStreamMaxVolume(AudioManager.STREAM_VOICE_CALL)
            am.setStreamVolume(AudioManager.STREAM_VOICE_CALL, maxVoice, flags)
            val newVoiceVol = am.getStreamVolume(AudioManager.STREAM_VOICE_CALL)
            
            // Also set NOTIFICATION volume
            val maxNotif = am.getStreamMaxVolume(AudioManager.STREAM_NOTIFICATION)
            am.setStreamVolume(AudioManager.STREAM_NOTIFICATION, maxNotif, flags)

            // Also set ALARM volume (for our alarm sound)
            val maxAlarm = am.getStreamMaxVolume(AudioManager.STREAM_ALARM)
            am.setStreamVolume(AudioManager.STREAM_ALARM, maxAlarm, 0)
            AppLog.log("🔔 Alarm Vol: ${am.getStreamVolume(AudioManager.STREAM_ALARM)}/$maxAlarm", context)

            // ══════════════════════════════════════
            // STEP 4: Play alarm sound (bypasses silent on most devices)
            // ══════════════════════════════════════
            stopCurrentRinger()
            try {
                val pm = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
                val wakeLock = pm?.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "EmergencyRinger::WakeLock")
                wakeLock?.acquire(60_000)

                val player = MediaPlayer().apply {
                    setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_ALARM)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .build()
                    )
                    isLooping = true
                    setOnCompletionListener { release() }
                    setOnErrorListener { _, _, _ -> release(); true }
                }
                
                // Use custom ringtone if selected, otherwise default alarm
                val ringtoneUriString = EmergencyContactRepository.getRingtoneUri(context)
                val uri = if (ringtoneUriString != null) {
                    android.net.Uri.parse(ringtoneUriString)
                } else {
                    getAlarmUri(context)
                }
                
                player.setDataSource(context, uri)
                player.prepare()
                player.start()
                mediaPlayer = player
                EmergencyContactRepository.isRingerPlaying = true  // Set playing state
                player.setOnCompletionListener { 
                    wakeLock?.let { if (it.isHeld) it.release() }
                    EmergencyContactRepository.isRingerPlaying = false
                }
                AppLog.log("🎵 Alarm PLAYING! (auto-stop in 30s)", context)
                
                // Schedule auto-stop after 30 seconds
                stopHandler = Handler(Looper.getMainLooper())
                stopRunnable = Runnable {
                    AppLog.log("⏱️ 30s timeout - stopping alarm", context)
                    stopCurrentRinger()
                }
                stopHandler?.postDelayed(stopRunnable!!, AUTO_STOP_DELAY_MS)
                
            } catch (e: Exception) {
                Log.e(TAG, "Alarm sound failed: ${e.message}")
                AppLog.log("⚠️ Alarm failed: ${e.message}", context)
            }

            // ══════════════════════════════════════
            // FINAL STATUS LOG
            // ══════════════════════════════════════
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                val finalFilter = nm?.currentInterruptionFilter ?: -1
                val finalDndName = when (finalFilter) {
                    NotificationManager.INTERRUPTION_FILTER_NONE -> "TOTAL SILENCE"
                    NotificationManager.INTERRUPTION_FILTER_PRIORITY -> "DND (Priority)"
                    NotificationManager.INTERRUPTION_FILTER_ALARMS -> "DND (Alarms)"
                    NotificationManager.INTERRUPTION_FILTER_ALL -> "OFF"
                    else -> "UNKNOWN"
                }
                AppLog.log("📊 AFTER: DND=$finalDndName, Ringer=$newRingerName, Vol=$newRingVol/$maxRinger", context)
            }
            AppLog.log("═══════════════════════════════════════", context)
            AppLog.log("✅ OVERRIDE COMPLETE", context)

        } catch (e: Exception) {
            Log.e(TAG, "❌ EXCEPTION: ${e.message}", e)
            AppLog.log("❌ Error: ${e.message}", context)
        }
    }

    private fun getAlarmUri(context: Context): android.net.Uri {
        val resId = context.resources.getIdentifier("emergency_ring", "raw", context.packageName)
        return if (resId != 0) {
            android.net.Uri.parse("android.resource://${context.packageName}/$resId")
        } else {
            RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        }
    }

    fun stopCurrentRinger() {
        // Cancel any pending auto-stop timer
        stopRunnable?.let { stopHandler?.removeCallbacks(it) }
        stopHandler = null
        stopRunnable = null
        
        mediaPlayer?.let { mp ->
            try {
                if (mp.isPlaying) mp.stop()
                mp.release()
            } catch (_: Exception) {}
            mediaPlayer = null
        }
        EmergencyContactRepository.isRingerPlaying = false  // Clear playing state
    }
}
