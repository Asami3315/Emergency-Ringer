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
import android.media.session.MediaSession
import android.media.session.PlaybackState
import android.media.VolumeProvider
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
    private var volumeJob: Job? = null
    private var vibrator: Vibrator? = null
    private var flashlightJob: Job? = null
    private var callStateWatcherJob: Job? = null  // polls call state every 1s to auto-stop alarm
    
    private var mediaSession: MediaSession? = null
    
    private var stopHandler: Handler? = null
    private var stopRunnable: Runnable? = null
    
    // Saved state to restore after alarm stops
    private var savedRingerMode: Int = AudioManager.RINGER_MODE_NORMAL
    private var savedDndFilter: Int = -1
    private var savedRingVolume: Int = -1
    private var savedAlarmVolume: Int = -1

    fun isPhoneSilentOrDnd(context: Context): Boolean {
        val am = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        val isDnd = nm?.currentInterruptionFilter != NotificationManager.INTERRUPTION_FILTER_ALL
        val isSilent = am?.ringerMode != AudioManager.RINGER_MODE_NORMAL
        return isDnd || isSilent
    }

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
            // STEP 1: PRE-MUTE the default dialer's RING stream
            //         (must happen BEFORE DND is lifted so the
            //          system ringer never gets a chance to play)
            // ══════════════════════════════════════
            try {
                if (!EmergencyContactRepository.isRingerPlaying) {
                    savedRingVolume = am.getStreamVolume(AudioManager.STREAM_RING)
                }
                am.setStreamVolume(AudioManager.STREAM_RING, 0, 0)
                // Also mute NOTIFICATION stream (some OEMs route call audio here)
                am.setStreamVolume(AudioManager.STREAM_NOTIFICATION, 0, 0)
                AppLog.log("🔇 Ring+Notification pre-muted (saved ring vol: $savedRingVolume)", context)
            } catch (e: Exception) {
                AppLog.log("⚠️ Pre-mute failed: ${e.message}", context)
            }

            // ══════════════════════════════════════
            // STEP 2: Save ringer mode + set to NORMAL
            //         (needed so our ALARM stream can play at full volume)
            // ══════════════════════════════════════
            try {
                if (!EmergencyContactRepository.isRingerPlaying) {
                    savedRingerMode = am.ringerMode
                    AppLog.log("📱 Original ringer mode: ${savedRingerMode} (0=SILENT,1=VIBRATE,2=NORMAL)", context)
                }
                am.ringerMode = AudioManager.RINGER_MODE_NORMAL
                // Immediately re-mute ring after mode change (NORMAL can reset ring vol)
                am.setStreamVolume(AudioManager.STREAM_RING, 0, 0)
                AppLog.log("📱 Ringer mode set to NORMAL (ring re-muted)", context)
            } catch (e: Exception) {
                AppLog.log("⚠️ Ringer mode error: ${e.message}", context)
            }

            // ══════════════════════════════════════
            // STEP 3: Disable DND (if permission granted)
            //         Ring stream is already muted, so the default
            //         dialer cannot produce sound when DND drops.
            // ══════════════════════════════════════
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && nm != null) {
                val hasAccess = nm.isNotificationPolicyAccessGranted
                val currentFilter = nm.currentInterruptionFilter
                AppLog.log("🔕 DND: filter=$currentFilter access=$hasAccess", context)
                if (hasAccess && currentFilter != NotificationManager.INTERRUPTION_FILTER_ALL) {
                    try {
                        savedDndFilter = currentFilter
                        nm.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_ALL)
                        // Re-mute ring once more after DND is lifted (belt and suspenders)
                        am.setStreamVolume(AudioManager.STREAM_RING, 0, 0)
                        AppLog.log("✅ DND turned OFF (was=$currentFilter), ring re-muted", context)
                    } catch (e: Exception) {
                        AppLog.log("⚠️ DND off failed: ${e.message}", context)
                    }
                } else if (!hasAccess) {
                    AppLog.log("⚠️ No DND permission - alarm uses USAGE_ALARM to bypass", context)
                }
            }

            // Silence ringer via TelecomManager (API 24+)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                val telecomManager = context.getSystemService(Context.TELECOM_SERVICE) as? android.telecom.TelecomManager
                try {
                    telecomManager?.silenceRinger()
                    AppLog.log("🔇 TelecomManager silenceRinger called", context)
                } catch (e: SecurityException) {
                    AppLog.log("⚠️ Silence ringer permission denied", context)
                }
            }

            val isPremium = EmergencyContactRepository.isPremium(context)

            // ══════════════════════════════════════
            // STEP 4: Max ALARM volume (our sound plays on this stream)
            // ══════════════════════════════════════
            try {
                am.adjustStreamVolume(AudioManager.STREAM_ALARM, AudioManager.ADJUST_UNMUTE, 0)
                if (!EmergencyContactRepository.isRingerPlaying) {
                    savedAlarmVolume = am.getStreamVolume(AudioManager.STREAM_ALARM)
                }
                val maxAlarm = am.getStreamMaxVolume(AudioManager.STREAM_ALARM)
                val volumePercent = if (isPremium) EmergencyContactRepository.getVolumePercent(context) else 100
                val targetAlarmVol = java.lang.Math.max(1, (maxAlarm * (volumePercent / 100f)).toInt())

                val isEscalating = if (isPremium) EmergencyContactRepository.isEscalatingVolumeEnabled(context) else false
                if (isEscalating) {
                    am.setStreamVolume(AudioManager.STREAM_ALARM, 1, 0)
                    AppLog.log("🔔 Alarm vol escalating to $targetAlarmVol/$maxAlarm", context)
                    volumeJob?.cancel()
                    volumeJob = CoroutineScope(Dispatchers.Default).launch {
                        var currentVol = 1
                        while (isActive && currentVol < targetAlarmVol && EmergencyContactRepository.isRingerPlaying) {
                            delay(1500)
                            currentVol++
                            am.setStreamVolume(AudioManager.STREAM_ALARM, currentVol, 0)
                        }
                    }
                } else {
                    am.setStreamVolume(AudioManager.STREAM_ALARM, targetAlarmVol, 0)
                    AppLog.log("🔔 Alarm vol = $targetAlarmVol/$maxAlarm", context)
                }
                
                // Also max out MUSIC stream as backup (some custom ringtones use it)
                am.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_UNMUTE, 0)
                val maxMusic = am.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
                am.setStreamVolume(AudioManager.STREAM_MUSIC, maxMusic, 0)
                AppLog.log("🎵 Music vol = $maxMusic/$maxMusic", context)
            } catch (e: Exception) {
                AppLog.log("⚠️ Volume error: ${e.message}", context)
            }


            // ══════════════════════════════════════
            // STEP 4: Play alarm sound
            // ══════════════════════════════════════
            stopCurrentRinger(context)
            val pm = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
            val wakeLock = pm?.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "EmergencyRinger::WakeLock")
            wakeLock?.acquire(60_000)

            val soundType = if (isPremium) {
                tempSoundType ?: EmergencyContactRepository.getAlarmSoundType(context)
            } else {
                EmergencyContactRepository.SOUND_TYPE_RINGTONE
            }

            when (soundType) {
                EmergencyContactRepository.SOUND_TYPE_BEEP -> {
                    AppLog.log("🔔 Playing BEEP", context)
                    toneGenerator = ToneGenerator(AudioManager.STREAM_ALARM, 100)
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
                    toneGenerator = ToneGenerator(AudioManager.STREAM_ALARM, 100)
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

                    val ringtoneSource = if (isPremium) EmergencyContactRepository.getRingtoneSource(context) else EmergencyContactRepository.RINGTONE_SOURCE_PHONE
                    val uri = if (ringtoneSource == EmergencyContactRepository.RINGTONE_SOURCE_CUSTOM) {
                        EmergencyContactRepository.getRingtoneUri(context)
                            ?.let { android.net.Uri.parse(it) }
                            ?: getAlarmUri(context)
                    } else {
                        getAlarmUri(context)
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
                            val fallbackUri = getAlarmUri(context)
                            AppLog.log("🔁 Fallback URI: $fallbackUri", context)

                            // Play fallback on ALARM stream. We DO NOT unmute STREAM_RING
                            // because that would cause the default system dialer to ring too.
                            val rt = RingtoneManager.getRingtone(context, fallbackUri)
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                                rt?.audioAttributes = AudioAttributes.Builder()
                                    .setUsage(AudioAttributes.USAGE_ALARM)
                                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                                    .build()
                            } else {
                                @Suppress("DEPRECATION")
                                rt?.streamType = AudioManager.STREAM_ALARM
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
                stopCurrentRinger(context)
            }
            stopHandler?.postDelayed(stopRunnable!!, autoStopDuration)
            
            // Start real-time call state watcher (most reliable stop mechanism)
            if (durationMs == null) startCallStateWatcher(context)
            
            // Capture hardware volume buttons to stop the alarm
            try {
                mediaSession?.release()
                mediaSession = MediaSession(context, "EmergencyRingerSession").apply {
                    setPlaybackState(PlaybackState.Builder().setState(PlaybackState.STATE_PLAYING, 0, 1.0f).build())
                    setPlaybackToRemote(object : VolumeProvider(VOLUME_CONTROL_RELATIVE, 100, 50) {
                        override fun onAdjustVolume(direction: Int) {
                            Log.i(TAG, "Volume button pressed ($direction) - stopping alarm")
                            AppLog.log("🔘 Volume button pressed — stopping alarm", context)
                            stopCurrentRinger(context)
                        }
                    })
                    isActive = true
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to start MediaSession for volume keys: ${e.message}")
            }

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
                        stopCurrentRinger(context)
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

    fun stopCurrentRinger(context: Context) {
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
        
        // Cancel siren and volume coroutines
        sirenJob?.cancel()
        sirenJob = null
        volumeJob?.cancel()
        volumeJob = null
        
        // Cancel call state watcher
        callStateWatcherJob?.cancel()
        callStateWatcherJob = null
        
        // Release media session
        try {
            mediaSession?.isActive = false
            mediaSession?.release()
        } catch (_: Exception) {}
        mediaSession = null
        
        stopVibration()
        stopFlashlight()
        
        EmergencyContactRepository.isRingerPlaying = false
        
        // 🔥 CRITICAL: Restore audio state to release volume buttons and DND
        restoreAudioState(context)
    }
    
    /**
     * Restore the phone to its original ringer/DND state.
     * Call this after stopping the ringer.
     */
    fun restoreAudioState(context: Context) {
        try {
            val am = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
            if (am != null) {
                // Restore ringer mode
                if (savedRingerMode != AudioManager.RINGER_MODE_NORMAL) {
                    am.ringerMode = savedRingerMode
                    AppLog.log("📱 Ringer mode restored to $savedRingerMode", context)
                }
                // Restore ring volume
                if (savedRingVolume != -1) {
                    am.setStreamVolume(AudioManager.STREAM_RING, savedRingVolume, 0)
                    AppLog.log("🔊 Ring volume restored to $savedRingVolume", context)
                    savedRingVolume = -1
                }
                // Restore alarm volume
                if (savedAlarmVolume != -1) {
                    am.setStreamVolume(AudioManager.STREAM_ALARM, savedAlarmVolume, 0)
                    AppLog.log("🔊 Alarm volume restored to $savedAlarmVolume", context)
                    savedAlarmVolume = -1
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Could not restore audio state: ${e.message}")
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
                try {
                    while (isActive) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                            cameraManager.setTorchMode(cameraId, true)
                            delay(300)
                            cameraManager.setTorchMode(cameraId, false)
                            delay(300)
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Flashlight failed or cancelled: ${e.message}")
                } finally {
                    try {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                            cameraManager.setTorchMode(cameraId, false)
                        }
                    } catch (ignore: Exception) {}
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
