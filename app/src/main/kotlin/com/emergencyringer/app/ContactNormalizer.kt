package com.emergencyringer.app

import java.util.regex.Pattern

/**
 * Normalizes contact names and notification titles for fuzzy matching.
 * Strips emojis, extra whitespace, and uses contains() for flexible matching.
 */
object ContactNormalizer {

    private val EMOJI_PATTERN = Pattern.compile(
        "[\\p{So}\\p{Sk}\\p{Cn}\\p{InDingbats}\\p{InMiscellaneousSymbols}]"
    )

    private val INCOMING_CALL_PHRASES = setOf(
        "incoming call",
        "voice call",
        "video call",
        "incoming",
        "call",
        "ringing",
        "phone call",
        "anruf",
        "eingehender anruf",
        "appel entrant",
        "llamada entrante",
        "chamada recebida"
    )

    /**
     * Strip emojis and non-printable chars, normalize whitespace.
     */
    fun normalize(text: String): String {
        var result = EMOJI_PATTERN.matcher(text).replaceAll("")
        result = result.replace(Regex("\\s+"), " ").trim()
        return result.lowercase()
    }

    /**
     * Check if whitelisted name matches the notification text (title, body, etc).
     * Uses contains() in both directions for flexible matching.
     */
    fun matches(whitelistedName: String, notificationText: String): Boolean {
        val w = normalize(whitelistedName)
        val n = normalize(notificationText)
        if (w.isEmpty() || n.isEmpty()) return false
        return n.contains(w) || w.contains(n)
    }

    /**
     * Check if notification text indicates an incoming call.
     */
    fun indicatesIncomingCall(text: CharSequence?): Boolean {
        if (text.isNullOrBlank()) return false
        val normalized = normalize(text.toString())
        return INCOMING_CALL_PHRASES.any { normalized.contains(it) }
    }

    /**
     * Normalize phone number (strip country code, spaces, dashes).
     */
    fun normalizePhone(number: String): String =
        number.filter { it.isDigit() }.takeLast(10)
}
