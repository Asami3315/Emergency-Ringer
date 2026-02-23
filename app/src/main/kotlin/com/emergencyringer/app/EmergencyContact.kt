package com.emergencyringer.app

import android.content.Context
import android.content.SharedPreferences
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

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

    // ═══════════════════════════════════════
    // CALL HISTORY & REPEATED-CALLER TRACKING
    // ═══════════════════════════════════════

    /** A single alarm trigger event shown in Recent Triggers history. */
    data class CallRecord(
        val callerName: String,
        val reason: String,         // "Emergency Contact" or "Repeated Caller (3×)"
        val timestampMs: Long = System.currentTimeMillis()
    ) {
        val timeLabel: String get() =
            SimpleDateFormat("hh:mm a, dd MMM", Locale.getDefault()).format(Date(timestampMs))
    }

    /** Number of calls from same unknown number to trigger repeated-caller alarm. */
    const val REPEATED_CALL_THRESHOLD = 3
    /** Time window for repeated-caller detection (1 hour). */
    private const val REPEATED_CALL_WINDOW_MS = 60 * 60 * 1000L

    /** In-memory call timestamps per caller key (resets on service restart). */
    private val recentCallTimestamps: MutableMap<String, MutableList<Long>> = mutableMapOf()

    /**
     * Record an incoming call from [callerKey].
     * Prunes calls older than 1 hour, then returns current count within window.
     */
    fun recordIncomingCall(callerKey: String): Int {
        val now = System.currentTimeMillis()
        val timestamps = recentCallTimestamps.getOrPut(callerKey) { mutableListOf() }
        timestamps.removeAll { now - it > REPEATED_CALL_WINDOW_MS }
        timestamps.add(now)
        return timestamps.size
    }

    /** Returns how many times [callerKey] has called within the last hour. */
    fun getRecentCallCount(callerKey: String): Int {
        val now = System.currentTimeMillis()
        return recentCallTimestamps[callerKey]
            ?.count { now - it <= REPEATED_CALL_WINDOW_MS } ?: 0
    }

    /** Trigger history shown in Recent Triggers UI (max 50 entries, persisted). */
    private val triggerHistory: MutableList<CallRecord> = mutableListOf()
    private var historyLoaded = false
    private const val KEY_TRIGGER_HISTORY = "trigger_history"

    /** Serialize a CallRecord to a storable string. */
    private fun CallRecord.serialize() = "$callerName\t$reason\t$timestampMs"

    /** Deserialize a CallRecord from a stored string. */
    private fun deserializeRecord(s: String): CallRecord? {
        val parts = s.split("\t")
        if (parts.size < 3) return null
        return CallRecord(parts[0], parts[1], parts[2].toLongOrNull() ?: return null)
    }

    /** Load history from SharedPreferences into memory (once per session). */
    private fun ensureHistoryLoaded(context: Context) {
        if (historyLoaded) return
        historyLoaded = true
        val stored = getPrefs(context).getStringSet(KEY_TRIGGER_HISTORY, null) ?: return
        val loaded = stored.mapNotNull { deserializeRecord(it) }
            .sortedByDescending { it.timestampMs }
        triggerHistory.addAll(loaded)
    }

    /** Save current in-memory history to SharedPreferences. */
    private fun saveHistory(context: Context) {
        val set = triggerHistory.map { it.serialize() }.toSet()
        getPrefs(context).edit().putStringSet(KEY_TRIGGER_HISTORY, set).apply()
    }

    /** Add a trigger event to the history (shown in Recent Triggers screen). */
    fun addTriggerRecord(callerName: String, reason: String, context: android.content.Context? = null) {
        synchronized(triggerHistory) {
            context?.let { ensureHistoryLoaded(it) }
            triggerHistory.add(0, CallRecord(callerName, reason))
            if (triggerHistory.size > 50) triggerHistory.removeLastOrNull()
            context?.let { saveHistory(it) }
        }
    }

    /** Returns a snapshot of the trigger history list. */
    fun getTriggerHistory(context: android.content.Context? = null): List<CallRecord> =
        synchronized(triggerHistory) {
            context?.let { ensureHistoryLoaded(it) }
            triggerHistory.toList()
        }

    /** Clears the trigger history. */
    fun clearTriggerHistory(context: android.content.Context? = null) = synchronized(triggerHistory) {
        triggerHistory.clear()
        context?.let { getPrefs(it).edit().remove(KEY_TRIGGER_HISTORY).apply() }
    }

    /** Resets the call count for [callerKey] after they triggered the repeated-caller alarm. */
    fun resetCallCount(callerKey: String) {
        recentCallTimestamps.remove(callerKey)
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
    
    // Ringtone display name (saved at selection time so it survives app restart)
    private const val KEY_RINGTONE_NAME = "ringtone_name"
    
    fun setRingtoneName(context: Context, name: String?) {
        getPrefs(context).edit().putString(KEY_RINGTONE_NAME, name).apply()
    }
    
    fun getRingtoneName(context: Context): String? {
        return getPrefs(context).getString(KEY_RINGTONE_NAME, null)
    }
    
    // Ringer playing state
    @Volatile
    var isRingerPlaying: Boolean = false
    
    // ═══════════════════════════════════════
    // SETTINGS PREFERENCES
    // ═══════════════════════════════════════

    // Message alert (notify when emergency contact sends a message)
    private const val KEY_MESSAGE_ALERT_ENABLED = "message_alert_enabled"

    fun setMessageAlertEnabled(context: Context, enabled: Boolean) {
        getPrefs(context).edit().putBoolean(KEY_MESSAGE_ALERT_ENABLED, enabled).apply()
    }

    fun isMessageAlertEnabled(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_MESSAGE_ALERT_ENABLED, false) // Default OFF
    }

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
