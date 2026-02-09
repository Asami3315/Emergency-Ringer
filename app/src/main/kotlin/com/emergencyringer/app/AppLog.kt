package com.emergencyringer.app

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * In-app log buffer for debugging. Persists to file so logs survive
 * process restarts (NotificationListenerService may run separately).
 */
object AppLog {

    private const val MAX_LINES = 300
    private const val LOG_FILE = "emergency_ringer_log.txt"

    private val _messages = MutableStateFlow<List<String>>(emptyList())
    val messages: StateFlow<List<String>> = _messages.asStateFlow()

    private val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())

    private var logFile: File? = null

    fun init(context: Context) {
        logFile = File(context.cacheDir, LOG_FILE)
        _messages.value = readFromFile()
    }

    private fun getFile(): File? = logFile ?: kotlin.run {
        // Fallback if init not called (service context)
        null
    }

    fun log(message: String, context: Context? = null) {
        val timestamped = "${timeFormat.format(Date())} $message"
        val updated = (_messages.value + timestamped).takeLast(MAX_LINES)
        _messages.value = updated
        val file = getFile() ?: context?.let { File(it.cacheDir, LOG_FILE) }
        file?.let { f ->
            try {
                f.appendText("$timestamped\n")
            } catch (_: Exception) {}
        }
    }

    fun clear() {
        _messages.value = emptyList()
        getFile()?.let { file ->
            try { file.writeText("") } catch (_: Exception) {}
        }
    }

    fun refreshFromFile() {
        val lines = readFromFile()
        if (lines.isNotEmpty()) _messages.value = lines
    }

    private fun readFromFile(): List<String> {
        return try {
            getFile()?.readLines()?.takeLast(MAX_LINES) ?: emptyList()
        } catch (_: Exception) { emptyList() }
    }
}
