package com.emergencyringer.app

import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.ContactsContract
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.content.getSystemService
import android.app.NotificationManager
import android.media.AudioManager
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Delete

class MainActivity : ComponentActivity() {

    private val readContactsLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (!granted) {
            Toast.makeText(this, "Contacts permission required to add emergency contacts", Toast.LENGTH_LONG).show()
        }
    }

    private val contactPickerLauncher = registerForActivityResult(
        ActivityResultContracts.PickContact()
    ) { uri ->
        uri?.let { handleContactPicked(it) }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        EmergencyContactRepository.init(this)

        setContent {
            MaterialTheme {
                MainScreen(
                    onRequestNotificationAccess = { openNotificationListenerSettings() },
                    onRequestDndAccess = { requestDndAccessIfNeeded() },
                    onRequestContactsPermission = { readContactsLauncher.launch(android.Manifest.permission.READ_CONTACTS) },
                    onRequestBatteryOptimization = { requestBatteryOptimizationExemption() },
                    onAddContact = {
                        if (hasContactsPermission()) {
                            contactPickerLauncher.launch(null)
                        } else {
                            readContactsLauncher.launch(android.Manifest.permission.READ_CONTACTS)
                        }
                    },
                    onRemoveContact = { name, number ->
                        EmergencyContactRepository.removeContact(this, name, number)
                    },
                    hasNotificationAccess = { isNotificationServiceEnabled() },
                    hasContactsPermission = { hasContactsPermission() },
                    hasDndAccess = { hasDndAccess() },
                    isBatteryOptimizationDisabled = { isBatteryOptimizationDisabled() }
                )
            }
        }
    }

    private fun openNotificationListenerSettings() {
        startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
    }

    private fun requestDndAccessIfNeeded() {
        val nm = getSystemService(NotificationManager::class.java) ?: return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!nm.isNotificationPolicyAccessGranted) {
                startActivity(Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS))
            }
        }
    }

    private fun isNotificationServiceEnabled(): Boolean {
        val pkgName = packageName
        val flat = Settings.Secure.getString(contentResolver, "enabled_notification_listeners") ?: return false
        return flat.split(":").any { it.contains(pkgName) }
    }

    private fun hasContactsPermission(): Boolean =
        ContextCompat.checkSelfPermission(this, android.Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED

    private fun hasDndAccess(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            (getSystemService(NotificationManager::class.java)?.isNotificationPolicyAccessGranted == true)
        } else true
    }

    private fun requestBatteryOptimizationExemption() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val intent = Intent().apply {
                action = Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS
                data = Uri.parse("package:$packageName")
            }
            try {
                startActivity(intent)
            } catch (_: Exception) {
                startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
            }
        }
    }

    private fun isBatteryOptimizationDisabled(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return true
        val pm = getSystemService(android.os.PowerManager::class.java) ?: return true
        return pm.isIgnoringBatteryOptimizations(packageName)
    }

    private fun handleContactPicked(uri: Uri) {
        contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val nameIdx = cursor.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME)
                val idIdx = cursor.getColumnIndex(ContactsContract.Contacts._ID)
                val name = if (nameIdx >= 0) cursor.getString(nameIdx) ?: "Unknown" else "Unknown"
                val contactId = if (idIdx >= 0) cursor.getString(idIdx) else null

                val number = contactId?.let { id ->
                    contentResolver.query(
                        ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                        null,
                        ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = ?",
                        arrayOf(id),
                        null
                    )?.use { phoneCursor ->
                        if (phoneCursor.moveToFirst()) {
                            val numIdx = phoneCursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                            if (numIdx >= 0) phoneCursor.getString(numIdx) ?: "" else ""
                        } else ""
                    } ?: ""
                } ?: ""

                EmergencyContactRepository.addContact(this, name, number)
                Toast.makeText(this, "Added: $name", Toast.LENGTH_SHORT).show()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    onRequestNotificationAccess: () -> Unit,
    onRequestDndAccess: () -> Unit,
    onRequestContactsPermission: () -> Unit,
    onRequestBatteryOptimization: () -> Unit,
    onAddContact: () -> Unit,
    onRemoveContact: (String, String) -> Unit,
    hasNotificationAccess: () -> Boolean,
    hasContactsPermission: () -> Boolean,
    hasDndAccess: () -> Boolean,
    isBatteryOptimizationDisabled: () -> Boolean
) {
    val context = LocalContext.current
    var contacts by remember { mutableStateOf(EmergencyContactRepository.getWhitelistSync(context)) }
    var showDisclosure by remember { mutableStateOf(false) }
    var refreshTrigger by remember { mutableStateOf(0) }

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        refreshTrigger++
    }

    LaunchedEffect(refreshTrigger) {
        contacts = EmergencyContactRepository.getWhitelistSync(context)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Emergency Ringer") },
                actions = {
                    Button(onClick = { showDisclosure = true }) {
                        Icon(Icons.Default.Info, contentDescription = null, Modifier.padding(end = 4.dp))
                        Text("Why?")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddContact) {
                Icon(Icons.Default.Add, contentDescription = "Add contact")
            }
        }
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize().padding(16.dp)) {
            Text("Permissions", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))

            PermissionCard(
                title = "Notification Access",
                description = "Required to detect incoming calls from WhatsApp and Phone",
                granted = hasNotificationAccess(),
                onGrant = onRequestNotificationAccess
            )
            Spacer(Modifier.height(8.dp))

            PermissionCard(
                title = "Do Not Disturb Access",
                description = "Allows bypassing DND when an emergency contact calls",
                granted = hasDndAccess(),
                onGrant = onRequestDndAccess
            )
            Spacer(Modifier.height(8.dp))

            PermissionCard(
                title = "Contacts",
                description = "Required to pick emergency contacts",
                granted = hasContactsPermission(),
                onGrant = onRequestContactsPermission
            )
            Spacer(Modifier.height(8.dp))

            PermissionCard(
                title = "Battery Optimization",
                description = "Don't optimize for reliable background monitoring",
                granted = isBatteryOptimizationDisabled(),
                onGrant = onRequestBatteryOptimization
            )

            Spacer(Modifier.height(24.dp))
            Text("Emergency Contacts", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))

            if (contacts.isEmpty()) {
                Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                    Text("No emergency contacts yet. Tap + to add.", style = MaterialTheme.typography.bodyMedium)
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(contacts) { contact ->
                        ContactCard(
                            name = contact.name,
                            number = contact.number,
                            onRemove = {
                                onRemoveContact(contact.name, contact.number)
                                contacts = EmergencyContactRepository.getWhitelistSync(context)
                            }
                        )
                    }
                }
            }
        }
    }

    if (showDisclosure) {
        androidx.compose.ui.window.Dialog(onDismissRequest = { showDisclosure = false }) {
            Card(Modifier.padding(16.dp)) {
                Column(Modifier.padding(24.dp)) {
                    Text("Why We Need Notification Access", style = MaterialTheme.typography.titleLarge)
                    Spacer(Modifier.height(12.dp))
                    Text(
                        "Emergency Ringer monitors incoming call notifications from your Phone app and WhatsApp. " +
                        "When a call matches one of your emergency contacts, the app overrides Silent and Do Not Disturb modes to play a loud alarm. " +
                        "We do not store, transmit, or share any notification content. All processing happens on your device.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(Modifier.height(16.dp))
                    Button(onClick = { showDisclosure = false }) { Text("OK") }
                }
            }
        }
    }
}

@Composable
fun PermissionCard(
    title: String,
    description: String,
    granted: Boolean,
    onGrant: () -> Unit
) {
    Card(Modifier.fillMaxWidth()) {
        Row(
            Modifier.padding(16.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleSmall)
                Text(description, style = MaterialTheme.typography.bodySmall)
            }
            Button(onClick = onGrant, enabled = !granted) {
                Text(if (granted) "Granted" else "Grant")
            }
        }
    }
}

@Composable
fun ContactCard(name: String, number: String, onRemove: () -> Unit) {
    Card(Modifier.fillMaxWidth()) {
        Row(
            Modifier.padding(16.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(name, style = MaterialTheme.typography.titleSmall)
                if (number.isNotBlank()) Text(number, style = MaterialTheme.typography.bodySmall)
            }
            Button(onClick = onRemove, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) {
                Icon(Icons.Default.Delete, contentDescription = null, Modifier.padding(end = 4.dp))
                Text("Remove")
            }
        }
    }
}
