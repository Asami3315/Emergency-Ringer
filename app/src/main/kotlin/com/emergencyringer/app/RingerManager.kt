package com.emergencyringer.app

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.media.ToneGenerator
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.os.Vibrator
import android.os.VibrationEffect
import android.hardware.camera2.CameraManager
import android.app.NotificationManager
import android.telephony.TelephonyManager
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.isActive

/**
 * Overrides silent/DND and plays alarm sound.
 * - Bypasses DND, sets ringer to normal
 * - Plays alarm sound (USAGE_ALARM bypasses silent on most devices)
 * - Supports vibration, flashlight strobe, custom volume
 */
object RingerManager {

    private const val TAG = "EmergencyRinger"

    @Volatile
    private var mediaPlayer: MediaPlayer? = null
    private var ringtone: android.media.Ringtone? = null  // Ringtone API fallback
    private var toneGenerator: ToneGenerator? = null
    private var sirenJob: Job? = null
    private var vibrator: Vibrator? = null
    private var flashlightJob: Job? = null
    private var callStateWatcherJob: Job? = null  // polls call state every 1s to auto-stop alarm
    
    private var stopHandler: Handler? = null
    private var stopRunnable: Runnable? = null
    
    // Saved state to restore after alarm stops
    private var savedRingerMode: Int = AudioManager.RINGER_MODE_NORMAL
    private var savedDndFilter: Int = -1

    fun triggerEmergencyRinger(
        context: Context,
        durationMs: Long? = null,
        tempSoundType: String? = null
    ) {
        Log.i(TAG, "═══════════════════════════════════════")
        Log.i(TAG, "EMERGENCY RINGER TRIGGERED!")
        Log.i(TAG, "═══════════════════════════════════════")
        AppLog.log("🔔 EMERGENCY RINGER TRIGGERED!", context)

        try {
            val am = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
                ?: run { AppLog.log("❌ AudioManager null!", context); return }
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager

            // ══════════════════════════════════════
            // STEP 1: Disable DND (if permission granted)
            // ══════════════════════════════════════
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && nm != null) {
                val hasAccess = nm.isNotificationPolicyAccessGranted
                val currentFilter = nm.currentInterruptionFilter
                AppLog.log("🔕 DND: filter=$currentFilter access=$hasAccess", context)
                if (hasAccess && currentFilter != NotificationManager.INTERRUPTION_FILTER_ALL) {
                    try {
                        savedDndFilter = currentFilter
                        nm.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_ALL)
                        AppLog.log("✅ DND turned OFF (was=$currentFilter)", context)
                        // NOTE: No Thread.sleep here — safe from any thread/context
                    } catch (e: Exception) {
                        AppLog.log("⚠️ DND off failed: ${e.message}", context)
                    }
                } else if (!hasAccess) {
                    AppLog.log("⚠️ No DND permission - alarm uses USAGE_ALARM to bypass", context)
                }
            }

            // ══════════════════════════════════════
            // STEP 2: Force ringer mode to NORMAL + Max ALARM volume
            // ══════════════════════════════════════
            try {
                // Save original ringer mode so we can restore later
                savedRingerMode = am.ringerMode
                AppLog.log("📱 Original ringer mode: ${savedRingerMode} (0=SILENT,1=VIBRATE,2=NORMAL)", context)
                
                // Force ringer mode to NORMAL — critical for DND/silent bypass on most OEMs
                am.ringerMode = AudioManager.RINGER_MODE_NORMAL
                AppLog.log("📱 Ringer mode set to NORMAL", context)
            } catch (e: Exception) {
                AppLog.log("⚠️ Ringer mode error: ${e.message}", context)
            }
            
            try {
                // Unmute and max ALARM stream (USAGE_ALARM bypasses DND)
                am.adjustStreamVolume(AudioManager.STREAM_ALARM, AudioManager.ADJUST_UNMUTE, 0)
                val maxAlarm = am.getStreamMaxVolume(AudioManager.STREAM_ALARM)
                am.setStreamVolume(AudioManager.STREAM_ALARM, maxAlarm, 0)
                AppLog.log("🔔 Alarm vol = $maxAlarm/$maxAlarm", context)
                
                // Also max out MUSIC stream as backup (some custom ringtones use it)
                am.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_UNMUTE, 0)
                val maxMusic = am.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
                am.setStreamVolume(AudioManager.STREAM_MUSIC, maxMusic, 0)
                AppLog.log("🎵 Music vol = $maxMusic/$maxMusic", context)
            } catch (e: Exception) {
                AppLog.log("⚠️ Volume error: ${e.message}", context)
            }

            // ══════════════════════════════════════
            // STEP 3: Mute RING stream (no dual ringtone from default dialer)
            // ══════════════════════════════════════
            try {
                am.setStreamVolume(AudioManager.STREAM_RING, 0, 0)
                AppLog.log("🔇 Ring muted", context)
            } catch (_: Exception) {}

            // ══════════════════════════════════════
            // STEP 4: Play alarm sound
            // ══════════════════════════════════════
            stopCurrentRinger()
            val pm = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
            val wakeLock = pm?.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "EmergencyRinger::WakeLock")
            wakeLock?.acquire(60_000)

            val soundType = tempSoundType ?: EmergencyContactRepository.getAlarmSoundType(context)
            val volumePercent = EmergencyContactRepository.getVolumePercent(context)

            when (soundType) {
                EmergencyContactRepository.SOUND_TYPE_BEEP -> {
                    AppLog.log("🔔 Playing BEEP", context)
                    val toneVolume = volumePercent.coerceIn(0, 100)
                    toneGenerator = ToneGenerator(AudioManager.STREAM_ALARM, toneVolume)
                    EmergencyContactRepository.isRingerPlaying = true
                    CoroutineScope(Dispatchers.Default).launch {
                        try {
                            while (EmergencyContactRepository.isRingerPlaying) {
                                toneGenerator?.startTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 200)
                                delay(500)
                            }
                        } catch (e: Exception) { Log.e(TAG, "Beep error: ${e.message}") }
                    }
                }

                EmergencyContactRepository.SOUND_TYPE_SIREN -> {
                    AppLog.log("🚨 Playing SIREN", context)
                    val toneVolume = volumePercent.coerceIn(0, 100)
                    toneGenerator = ToneGenerator(AudioManager.STREAM_ALARM, toneVolume)
                    EmergencyContactRepository.isRingerPlaying = true
                    sirenJob = CoroutineScope(Dispatchers.Default).launch {
                        try {
                            var high = true
                            while (EmergencyContactRepository.isRingerPlaying) {
                                toneGenerator?.startTone(
                                    if (high) ToneGenerator.TONE_DTMF_1 else ToneGenerator.TONE_DTMF_4, 400
                                )
                                delay(400)
                                high = !high
                            }
                        } catch (e: Exception) { Log.e(TAG, "Siren error: ${e.message}") }
                    }
                }

                else -> {
                    // Ringtone — try MediaPlayer first, fall back to Ringtone API
                    AppLog.log("🎵 Playing RINGTONE", context)

                    val ringtoneSource = EmergencyContactRepository.getRingtoneSource(context)
                    val uri = if (ringtoneSource == EmergencyContactRepository.RINGTONE_SOURCE_CUSTOM) {
                        EmergencyContactRepository.getRingtoneUri(context)
                            ?.let { android.net.Uri.parse(it) }
                            ?: RingtoneManager.getActualDefaultRingtoneUri(context, RingtoneManager.TYPE_ALARM)
                    } else {
                        RingtoneManager.getActualDefaultRingtoneUri(context, RingtoneManager.TYPE_RINGTONE)
                            ?: getAlarmUri(context)
                    }
                    AppLog.log("🎵 URI: $uri", context)

                    // Try MediaPlayer (USAGE_ALARM bypasses DND natively)
                    var playerStarted = false
                    try {
                        val player = MediaPlayer().apply {
                            setAudioAttributes(
                                AudioAttributes.Builder()
                                    .setUsage(AudioAttributes.USAGE_ALARM)
                                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                                    .build()
                            )
                            isLooping = true
                        }
                        player.setDataSource(context, uri)
                        player.prepare()
                        val vol = volumePercent / 100f
                        player.setVolume(vol, vol)
                        player.start()
                        mediaPlayer = player
                        EmergencyContactRepository.isRingerPlaying = true
                        playerStarted = true
                        AppLog.log("✅ MediaPlayer started (USAGE_ALARM)", context)
                    } catch (e: Exception) {
                        AppLog.log("⚠️ MediaPlayer failed: ${e.message} - trying Ringtone API", context)
                        Log.e(TAG, "MediaPlayer failed: ${e.message}", e)
                    }

                    // Fallback: custom URI failed — use system ALARM ringtone (guaranteed accessible)
                    if (!playerStarted) {
                        try {
                            // Use system alarm URI, not the failed custom URI
                            val fallbackUri =
                                RingtoneManager.getActualDefaultRingtoneUri(context, RingtoneManager.TYPE_ALARM)
                                    ?: RingtoneManager.getActualDefaultRingtoneUri(context, RingtoneManager.TYPE_RINGTONE)
                                    ?: getAlarmUri(context)
                            AppLog.log("🔁 Fallback URI: $fallbackUri", context)

                            // Unmute RING stream so fallback is audible even if we muted it earlier
                            try {
                                val maxRing = am.getStreamMaxVolume(AudioManager.STREAM_RING)
                                am.setStreamVolume(AudioManager.STREAM_RING, maxRing, 0)
                            } catch (_: Exception) {}

                            val rt = RingtoneManager.getRingtone(context, fallbackUri)
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                                rt?.audioAttributes = AudioAttributes.Builder()
                                    .setUsage(AudioAttributes.USAGE_ALARM)
                                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                                    .build()
                            }
                            rt?.play()
                            ringtone = rt
                            EmergencyContactRepository.isRingerPlaying = true
                            AppLog.log("✅ Ringtone fallback (ALARM URI) started", context)
                        } catch (e2: Exception) {
                            AppLog.log("❌ Both audio methods failed: ${e2.message}", context)
                        }
                    }
                }
            }

            // ══════════════════════════════════════
            // STEP 5: Vibration + Flashlight + Auto-stop + Call Watcher
            // ══════════════════════════════════════
            val autoStopDuration = durationMs ?: EmergencyContactRepository.getAutoStopDuration(context)
            AppLog.log("⏱ Auto-stop in ${autoStopDuration / 1000}s", context)

            if (durationMs == null && EmergencyContactRepository.isVibrateEnabled(context)) startVibration(context)
            if (durationMs == null && EmergencyContactRepository.isFlashlightEnabled(context)) startFlashlight(context)

            stopHandler = Handler(Looper.getMainLooper())
            stopRunnable = Runnable {
                AppLog.log("⏱ Timeout - stopping alarm", context)
                stopCurrentRinger()
            }
            stopHandler?.postDelayed(stopRunnable!!, autoStopDuration)
            
            // Start real-time call state watcher (most reliable stop mechanism)
            if (durationMs == null) startCallStateWatcher(context)

        } catch (e: Exception) {
            Log.e(TAG, "❌ FATAL: ${e.message}", e)
            AppLog.log("❌ Fatal error: ${e.message}", context)
        }
    }

    /**
     * Polls TelephonyManager.getCallState() every second.
     * Stops alarm as soon as the call is picked up (OFFHOOK) or ended (IDLE).
     * This is the most reliable approach on MIUI/Redmi where callbacks fail.
     */
    private fun startCallStateWatcher(context: Context) {
        callStateWatcherJob?.cancel()
        callStateWatcherJob = CoroutineScope(Dispatchers.Default).launch {
            val tm = context.getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager
                ?: return@launch
            AppLog.log("📞 Call state watcher started", context)
            
            // Wait briefly for the call to fully establish before watching
            delay(2000)
            
            while (isActive && EmergencyContactRepository.isRingerPlaying) {
                try {
                    @Suppress("DEPRECATION")
                    val state = tm.callState  // Works without permission on many devices
                    if (state == TelephonyManager.CALL_STATE_IDLE ||
                        state == TelephonyManager.CALL_STATE_OFFHOOK) {
                        AppLog.log("📞 Watcher: call state=$state → stopping alarm", context)
                        stopCurrentRinger()
                        break
                    }
                } catch (_: Exception) {}
                delay(1000)  // Poll every second
            }
            AppLog.log("📞 Call state watcher ended", context)
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
        
        // Stop media player
        mediaPlayer?.let { mp ->
            try { if (mp.isPlaying) mp.stop(); mp.release() } catch (_: Exception) {}
            mediaPlayer = null
        }
        
        // Stop Ringtone API fallback
        ringtone?.let { rt ->
            try { if (rt.isPlaying) rt.stop() } catch (_: Exception) {}
            ringtone = null
        }
        
        // Stop tone generator (beep/siren)
        toneGenerator?.let { tg ->
            try { tg.stopTone(); tg.release() } catch (_: Exception) {}
            toneGenerator = null
        }
        
        // Cancel siren coroutine
        sirenJob?.cancel()
        sirenJob = null
        
        // Cancel call state watcher
        callStateWatcherJob?.cancel()
        callStateWatcherJob = null
        
        stopVibration()
        stopFlashlight()
        
        EmergencyContactRepository.isRingerPlaying = false
    }
    
    /**
     * Restore the phone to its original ringer/DND state.
     * Call this after stopping the ringer.
     */
    fun restoreAudioState(context: Context) {
        try {
            val am = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
            if (am != null && savedRingerMode != AudioManager.RINGER_MODE_NORMAL) {
                am.ringerMode = savedRingerMode
                AppLog.log("📱 Ringer mode restored to $savedRingerMode", context)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Could not restore ringer mode: ${e.message}")
        }
        
        // Restore DND if it was active before
        try {
            if (savedDndFilter > 0 && savedDndFilter != NotificationManager.INTERRUPTION_FILTER_ALL) {
                val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
                if (nm != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && nm.isNotificationPolicyAccessGranted) {
                    nm.setInterruptionFilter(savedDndFilter)
                    AppLog.log("🔕 DND restored to filter=$savedDndFilter", context)
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Could not restore DND: ${e.message}")
        }
        savedDndFilter = -1
    }
    
    // ═══════════════════════════════════════
    // VIBRATION SUPPORT
    // ═══════════════════════════════════════
    private fun startVibration(context: Context) {
        try {
            vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            vibrator?.let {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    val pattern = longArrayOf(0, 1000, 500)  // 1s on, 0.5s off
                    it.vibrate(VibrationEffect.createWaveform(pattern, 0))
                } else {
                    @Suppress("DEPRECATION")
                    it.vibrate(longArrayOf(0, 1000, 500), 0)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Vibration failed: ${e.message}")
        }
    }
    
    private fun stopVibration() {
        vibrator?.cancel()
        vibrator = null
    }
    
    // ═══════════════════════════════════════
    // FLASHLIGHT STROBE SUPPORT
    // ═══════════════════════════════════════
    private fun startFlashlight(context: Context) {
        try {
            val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as? CameraManager
            val cameraId = cameraManager?.cameraIdList?.firstOrNull() ?: return
            
            flashlightJob = CoroutineScope(Dispatchers.Default).launch {
                while (true) {
                    try {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                            cameraManager?.setTorchMode(cameraId, true)
                            delay(300)
                            cameraManager?.setTorchMode(cameraId, false)
                            delay(300)
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Flashlight failed: ${e.message}")
                        break
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Flashlight init failed: ${e.message}")
        }
    }
    
    private fun stopFlashlight() {
        flashlightJob?.cancel()
        flashlightJob = null
    }
}
