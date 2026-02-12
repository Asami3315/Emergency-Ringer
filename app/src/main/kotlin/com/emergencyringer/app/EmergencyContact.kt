package com.emergencyringer.app

import android.content.Context
import android.content.SharedPreferences

private const val PREFS_NAME = "emergency_contacts"
private const val KEY_CONTACTS = "whitelist"

/**
 * Data model for emergency contact and persistence layer.
 * Uses SharedPreferences for sync access (needed by NotificationService).
 */
object EmergencyContactRepository {

    data class Contact(val name: String, val number: String) {
        val persistenceValue: String get() = "$name|$number"
        companion object {
            fun fromPersistence(value: String): Contact? {
                val parts = value.split("|", limit = 2)
                return if (parts.size >= 2) Contact(parts[0], parts[1]) else null
            }
        }
    }

    private var prefs: SharedPreferences? = null

    fun init(context: Context) {
        if (prefs == null) prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    private fun getPrefs(context: Context): SharedPreferences {
        init(context)
        return prefs!!
    }

    fun getWhitelistSync(context: Context): List<Contact> {
        val set = getPrefs(context).getStringSet(KEY_CONTACTS, null) ?: return emptyList()
        return set.mapNotNull { Contact.fromPersistence(it) }
    }

    fun getWhitelistedNames(context: Context): List<String> =
        getWhitelistSync(context).map { it.name }

    fun addContact(context: Context, name: String, number: String) {
        val prefs = getPrefs(context)
        val current = prefs.getStringSet(KEY_CONTACTS, null)?.toMutableSet() ?: mutableSetOf()
        current.add(Contact(name, number).persistenceValue)
        prefs.edit().putStringSet(KEY_CONTACTS, current).apply()
    }

    fun removeContact(context: Context, name: String, number: String) {
        val prefs = getPrefs(context)
        val current = prefs.getStringSet(KEY_CONTACTS, null)?.toMutableSet() ?: return
        current.remove(Contact(name, number).persistenceValue)
        prefs.edit().putStringSet(KEY_CONTACTS, current).apply()
    }
    
    // Master toggle for monitoring
    private const val KEY_MONITORING_ENABLED = "monitoring_enabled"
    
    fun setMonitoringEnabled(context: Context, enabled: Boolean) {
        getPrefs(context).edit().putBoolean(KEY_MONITORING_ENABLED, enabled).apply()
        
        // Update Quick Settings Tile
        ProtectionTileService.requestTileUpdate(context)
    }
    
    fun isMonitoringEnabled(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_MONITORING_ENABLED, true) // Default enabled
    }
    
    // Custom ringtone
    private const val KEY_RINGTONE_URI = "ringtone_uri"
    private const val KEY_RINGTONE_SOURCE = "ringtone_source"
    
    const val RINGTONE_SOURCE_PHONE = "phone"
    const val RINGTONE_SOURCE_CUSTOM = "custom"
    
    fun setRingtoneUri(context: Context, uri: String?) {
        getPrefs(context).edit().putString(KEY_RINGTONE_URI, uri).apply()
    }
    
    fun getRingtoneUri(context: Context): String? {
        return getPrefs(context).getString(KEY_RINGTONE_URI, null)
    }
    
    fun setRingtoneSource(context: Context, source: String) {
        getPrefs(context).edit().putString(KEY_RINGTONE_SOURCE, source).apply()
    }
    
    fun getRingtoneSource(context: Context): String {
        return getPrefs(context).getString(KEY_RINGTONE_SOURCE, RINGTONE_SOURCE_PHONE) ?: RINGTONE_SOURCE_PHONE
    }
    
    // Ringer playing state (to show End Call button)
    @Volatile
    var isRingerPlaying: Boolean = false
    
    // ═══════════════════════════════════════
    // SETTINGS PREFERENCES
    // ═══════════════════════════════════════
    
    // Auto-stop timer (in milliseconds)
    private const val KEY_AUTO_STOP_DURATION = "auto_stop_duration"
    private const val DEFAULT_AUTO_STOP_MS = 30_000L  // 30 seconds
    
    fun setAutoStopDuration(context: Context, durationMs: Long) {
        getPrefs(context).edit().putLong(KEY_AUTO_STOP_DURATION, durationMs).apply()
    }
    
    fun getAutoStopDuration(context: Context): Long {
        return getPrefs(context).getLong(KEY_AUTO_STOP_DURATION, DEFAULT_AUTO_STOP_MS)
    }
    
    // Vibration
    private const val KEY_VIBRATE_ENABLED = "vibrate_enabled"
    
    fun setVibrateEnabled(context: Context, enabled: Boolean) {
        getPrefs(context).edit().putBoolean(KEY_VIBRATE_ENABLED, enabled).apply()
    }
    
    fun isVibrateEnabled(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_VIBRATE_ENABLED, true)  // Default ON
    }
    
    // Flashlight strobe
    private const val KEY_FLASHLIGHT_ENABLED = "flashlight_enabled"
    
    fun setFlashlightEnabled(context: Context, enabled: Boolean) {
        getPrefs(context).edit().putBoolean(KEY_FLASHLIGHT_ENABLED, enabled).apply()
    }
    
    fun isFlashlightEnabled(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_FLASHLIGHT_ENABLED, false)  // Default OFF
    }
    
    // Volume (0-100)
    private const val KEY_VOLUME_PERCENT = "volume_percent"
    
    fun setVolumePercent(context: Context, percent: Int) {
        getPrefs(context).edit().putInt(KEY_VOLUME_PERCENT, percent.coerceIn(0, 100)).apply()
    }
    
    fun getVolumePercent(context: Context): Int {
        return getPrefs(context).getInt(KEY_VOLUME_PERCENT, 100)  // Default 100%
    }
    
    // Sync with mobile volume
    private const val KEY_SYNC_MOBILE_VOLUME = "sync_mobile_volume"
    
    fun setSyncWithMobileVolume(context: Context, enabled: Boolean) {
        getPrefs(context).edit().putBoolean(KEY_SYNC_MOBILE_VOLUME, enabled).apply()
    }
    
    fun isSyncWithMobileVolumeEnabled(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_SYNC_MOBILE_VOLUME, false) // Default disabled
    }
    
    // Alarm sound type
    private const val KEY_ALARM_SOUND_TYPE = "alarm_sound_type"
    const val SOUND_TYPE_RINGTONE = "ringtone"
    const val SOUND_TYPE_SIREN = "siren"
    const val SOUND_TYPE_BEEP = "beep"
    
    fun setAlarmSoundType(context: Context, type: String) {
        getPrefs(context).edit().putString(KEY_ALARM_SOUND_TYPE, type).apply()
    }
    
    fun getAlarmSoundType(context: Context): String {
        return getPrefs(context).getString(KEY_ALARM_SOUND_TYPE, SOUND_TYPE_RINGTONE) ?: SOUND_TYPE_RINGTONE
    }
    
    // Dark mode
    private const val KEY_DARK_MODE = "dark_mode"
    const val DARK_MODE_SYSTEM = "system"
    const val DARK_MODE_LIGHT = "light"
    const val DARK_MODE_DARK = "dark"
    
    fun setDarkMode(context: Context, mode: String) {
        getPrefs(context).edit().putString(KEY_DARK_MODE, mode).apply()
    }
    
    fun getDarkMode(context: Context): String {
        return getPrefs(context).getString(KEY_DARK_MODE, DARK_MODE_SYSTEM) ?: DARK_MODE_SYSTEM
    }
}
