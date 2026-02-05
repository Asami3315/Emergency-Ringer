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
}
