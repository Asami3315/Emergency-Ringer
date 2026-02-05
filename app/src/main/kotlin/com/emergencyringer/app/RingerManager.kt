package com.emergencyringer.app

import android.content.Context
import android.media.AudioManager
import android.os.Build
import android.app.NotificationManager

/**
 * Overrides silent/DND so the phone's call ringtone can be heard.
 * - Sets ringer mode to NORMAL
 * - Bypasses Do Not Disturb
 * - Cranks ringer volume to max
 */
object RingerManager {

    fun triggerEmergencyRinger(context: Context) {
        try {
            val am = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager ?: return
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager

            // 1. Bypass DND first (must be before ringer mode on many devices)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (nm?.isNotificationPolicyAccessGranted == true) {
                    nm.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_ALL)
                }
            }

            // 2. Set ringer mode to normal (turn off silent)
            am.ringerMode = AudioManager.RINGER_MODE_NORMAL

            // 3. Crank ringer and voice call volume to max
            val flags = AudioManager.FLAG_ALLOW_RINGER_MODES or
                AudioManager.FLAG_PLAY_SOUND or
                AudioManager.FLAG_VIBRATE

            val maxRinger = am.getStreamMaxVolume(AudioManager.STREAM_RING)
            am.setStreamVolume(AudioManager.STREAM_RING, maxRinger, flags)

            val maxVoice = am.getStreamMaxVolume(AudioManager.STREAM_VOICE_CALL)
            am.setStreamVolume(AudioManager.STREAM_VOICE_CALL, maxVoice, flags)
        } catch (e: Exception) {
            // Ignore
        }
    }
}
