package com.emergencyringer.app

import android.content.Intent
import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.launch
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.ContactsContract
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.fragment.app.FragmentActivity

import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import com.emergencyringer.app.EmergencyContactRepository
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import android.app.NotificationManager

// Stitch Design System — Yellow/Gold palette
private val VibrantPurple   = Color(0xFFFFB703)   // maps to NeoYellow for back-compat
private val DeepPurple      = Color(0xFFE6A200)
private val LightBackground = Color(0xFFFAFAFA)
private val SurfaceWhite    = Color(0xFFFFFFFF)
private val GlassFrost      = Color(0x33000000)
private val AccentPurple    = Color(0xFFFFD569)

class MainActivity : FragmentActivity() {

    private lateinit var billingManager: BillingManager

    private val readContactsLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (!granted) {
            Toast.makeText(this, "Contacts permission required", Toast.LENGTH_LONG).show()
        }
    }

    private val contactPickerLauncher = registerForActivityResult(
        ActivityResultContracts.PickContact()
    ) { uri ->
        uri?.let { handleContactPicked(it) }
    }
    
    private val ringtonePickerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            val uri = result.data?.getParcelableExtra<android.net.Uri>(android.media.RingtoneManager.EXTRA_RINGTONE_PICKED_URI)
            if (uri != null) {
                // Try to persist read permission so the URI survives app restarts
                try {
                    contentResolver.takePersistableUriPermission(
                        uri, 
                        android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )
                } catch (_: SecurityException) {
                    // Some URIs (like built-in ringtones) don't support persistable permissions
                    // That's OK - they're always accessible
                }
                
                // Resolve and save the display name NOW (while we have guaranteed access)
                val displayName = try {
                    val ringtone = android.media.RingtoneManager.getRingtone(this, uri)
                    val title = ringtone?.getTitle(this)
                    if (!title.isNullOrBlank() && !title.all { it.isDigit() } && title.length > 1) title
                    else uri.lastPathSegment ?: "Custom Sound"
                } catch (_: Exception) {
                    uri.lastPathSegment ?: "Custom Sound"
                }
                
                EmergencyContactRepository.setRingtoneUri(this, uri.toString())
                EmergencyContactRepository.setRingtoneName(this, displayName)
                // Auto-switch to "Custom Sound" when user picks a ringtone
                EmergencyContactRepository.setRingtoneSource(this, EmergencyContactRepository.RINGTONE_SOURCE_CUSTOM)
                Toast.makeText(this, "Ringtone selected: $displayName", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        EmergencyContactRepository.init(this)
        billingManager = BillingManager(this)
        

        // Request phone state permission for call end detection
        // Android 13+ uses READ_BASIC_PHONE_STATE, older uses READ_PHONE_STATE
        val phonePermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            android.Manifest.permission.READ_BASIC_PHONE_STATE
        } else {
            android.Manifest.permission.READ_PHONE_STATE
        }
        if (checkSelfPermission(phonePermission) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(phonePermission), 1001)
        }

        setContent {
            var showSplash by remember { mutableStateOf(true) }
            val prefs = remember { getSharedPreferences("emergency_ringer_intro", MODE_PRIVATE) }
            var showIntro by remember { mutableStateOf(!prefs.getBoolean("intro_completed", false)) }
            
            // Premium state
            var showPaywall by remember { mutableStateOf(false) }

            EmergencyRingerTheme {
                if (showSplash) {
                    SplashScreen(onFinished = { showSplash = false })
                } else if (showIntro) {
                    IntroScreen(onComplete = {
                        prefs.edit().putBoolean("intro_completed", true).apply()
                        showIntro = false
                    })
                } else {
                    Box(modifier = Modifier.fillMaxSize()) {
                MainScreen(
                    onRequestNotificationAccess = { openNotificationListenerSettings() },
                    onRequestDndAccess = { requestDndAccessIfNeeded() },
                    onRequestContactsPermission = { readContactsLauncher.launch(android.Manifest.permission.READ_CONTACTS) },
                    onRequestBatteryOptimization = { requestBatteryOptimizationExemption() },
                    onAddContact = {
                        if (hasContactsPermission()) {
                            try {
                                contactPickerLauncher.launch(null)
                            } catch (e: Exception) {
                                Toast.makeText(this@MainActivity, "Unable to open contacts app", Toast.LENGTH_SHORT).show()
                            }
                        } else {
                            readContactsLauncher.launch(android.Manifest.permission.READ_CONTACTS)
                        }
                    },
                    onRemoveContact = { name, number ->
                        EmergencyContactRepository.removeContact(this@MainActivity, name, number)
                    },
                    hasNotificationAccess = { isNotificationServiceEnabled() },
                    hasContactsPermission = { hasContactsPermission() },
                    hasDndAccess = { hasDndAccess() },
                    isBatteryOptimizationDisabled = { isBatteryOptimizationDisabled() },
                    onTestRinger = {
                        RingerManager.triggerEmergencyRinger(this@MainActivity)
                    },
                    onStopRinger = {
                        RingerManager.stopCurrentRinger(this@MainActivity)
                    },
                    onSelectRingtoneInSettings = {
                        val intent = Intent(android.media.RingtoneManager.ACTION_RINGTONE_PICKER).apply {
                            putExtra(android.media.RingtoneManager.EXTRA_RINGTONE_TYPE, android.media.RingtoneManager.TYPE_RINGTONE)
                            putExtra(android.media.RingtoneManager.EXTRA_RINGTONE_TITLE, "Select Emergency Ringtone")
                            val currentUri = EmergencyContactRepository.getRingtoneUri(this@MainActivity)
                            if (currentUri != null) {
                                putExtra(android.media.RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, android.net.Uri.parse(currentUri))
                            }
                        }
                        ringtonePickerLauncher.launch(intent)
                    },
                    onShowPaywall = { showPaywall = true }
                )
                
                // Show Paywall Overlay if triggered
                if (showPaywall) {
                    PremiumScreen(
                        billingManager = billingManager,
                        onClose = { showPaywall = false }
                    )
                }
                
                    } // end Box
                } // end else (main app)
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
                // Try to open our app's specific DND settings page directly
                val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    // Android 10+ supports deep-linking directly to our app's DND page
                    // Use string literals since constants may not be in older SDK imports
                    Intent("android.settings.NOTIFICATION_POLICY_ACCESS_DETAIL_SETTINGS").apply {
                        putExtra("android.provider.extra.APP_PACKAGE", packageName)
                    }
                } else {
                    // Fallback: open the general DND access list
                    Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS)
                }
                try {
                    startActivity(intent)
                } catch (e: Exception) {
                    // If deep-link fails, fall back to general settings
                    startActivity(Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS))
                }
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
        try {
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
        } catch (e: Exception) {
            Toast.makeText(this, "Error reading contact", Toast.LENGTH_SHORT).show()
        }
    }
}

// Custom Web Font: Plus Jakarta Sans
val PlusJakartaSans = androidx.compose.ui.text.font.FontFamily.SansSerif

@Composable
fun EmergencyRingerTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = lightColorScheme(
            primary            = Color(0xFFFFB703),
            primaryContainer   = Color(0xFFE6A200),
            secondary          = Color(0xFFFFD569),
            background         = Color(0xFFFAFAFA),
            surface            = Color(0xFFFFFFFF),
            onPrimary          = Color.White,
            onBackground       = Color(0xFF1A1A1A),
            onSurface          = Color(0xFF1A1A1A)
        ),
        typography = Typography(
            displayLarge  = TextStyle(fontFamily = PlusJakartaSans, fontSize = 57.sp, fontWeight = FontWeight.Bold),
            headlineLarge = TextStyle(fontFamily = PlusJakartaSans, fontSize = 32.sp, fontWeight = FontWeight.Bold),
            titleLarge    = TextStyle(fontFamily = PlusJakartaSans, fontSize = 22.sp, fontWeight = FontWeight.SemiBold),
            bodyLarge     = TextStyle(fontFamily = PlusJakartaSans, fontSize = 16.sp, fontWeight = FontWeight.Normal),
            labelLarge    = TextStyle(fontFamily = PlusJakartaSans, fontSize = 14.sp, fontWeight = FontWeight.Medium)
        ),
        content = content
    )
}

// ─── New 3-tab bottom-nav screen ──────────────────────────────────────────
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
    isBatteryOptimizationDisabled: () -> Boolean,
    onTestRinger: () -> Unit,
    onStopRinger: () -> Unit,
    onSelectRingtoneInSettings: () -> Unit,
    onShowPaywall: () -> Unit
) {
    val context = LocalContext.current
    var contacts by remember { mutableStateOf(EmergencyContactRepository.getWhitelistSync(context)) }
    var monitoringEnabled by remember { mutableStateOf(EmergencyContactRepository.isMonitoringEnabled(context)) }
    var isPremium by remember { mutableStateOf(EmergencyContactRepository.isPremium(context)) }
    var currentTab by remember { mutableStateOf(0) }   // 0=Home 1=History 2=Settings

    // ── Reactive permission states ───────────────────────
    var permNotification by remember { mutableStateOf(hasNotificationAccess()) }
    var permDnd by remember { mutableStateOf(hasDndAccess()) }
    var permContacts by remember { mutableStateOf(hasContactsPermission()) }
    var permBattery by remember { mutableStateOf(isBatteryOptimizationDisabled()) }

    // Re-read ALL state on resume (including permissions)
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        contacts = EmergencyContactRepository.getWhitelistSync(context)
        monitoringEnabled = EmergencyContactRepository.isMonitoringEnabled(context)
        permNotification = hasNotificationAccess()
        permDnd = hasDndAccess()
        permContacts = hasContactsPermission()
        permBattery = isBatteryOptimizationDisabled()
    }

    // Also sync monitoring toggle in real-time from Quick Settings tile changes
    DisposableEffect(Unit) {
        val prefs = context.getSharedPreferences("emergency_contacts", Context.MODE_PRIVATE)
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key == "monitoring_enabled") {
                monitoringEnabled = EmergencyContactRepository.isMonitoringEnabled(context)
            } else if (key == "whitelist") {
                contacts = EmergencyContactRepository.getWhitelistSync(context)
            } else if (key == "is_premium") {
                isPremium = EmergencyContactRepository.isPremium(context)
            }
        }
        prefs.registerOnSharedPreferenceChangeListener(listener)
        onDispose { prefs.unregisterOnSharedPreferenceChangeListener(listener) }
    }

    Box(modifier = Modifier.fillMaxSize().background(Color(0xFFFAFAFA))) {
        // ── Tab content ──────────────────────────────────────
        when (currentTab) {
            0 -> HomeScreen(
                hasNotificationAccess     = permNotification,
                hasDndAccess              = permDnd,
                hasContactsPermission     = permContacts,
                isBatteryOptDisabled      = permBattery,
                contacts                  = contacts,
                monitoringEnabled         = monitoringEnabled,
                onMonitoringToggle        = { enabled ->
                    monitoringEnabled = enabled
                    EmergencyContactRepository.setMonitoringEnabled(context, enabled)
                    AppLog.log(if (enabled) "✅ Monitoring enabled" else "⏸️ Monitoring paused", context)
                },
                onAddContact              = {
                    if (!isPremium && contacts.size >= 4) {
                        onShowPaywall()
                    } else {
                        onAddContact()
                    }
                },
                onRemoveContact           = onRemoveContact,
                isPremium                 = isPremium
            )
            1 -> HistoryScreen()
            2 -> SettingsScreen(
                onBack              = { currentTab = 0 },
                onSelectRingtone    = onSelectRingtoneInSettings,
                onTestRinger        = onTestRinger,
                onStopRinger        = onStopRinger,
                vibrantPurple       = Color(0xFFFFB703),
                deepPurple          = Color(0xFFE6A200),
                monitoringEnabled   = monitoringEnabled,
                allPermsReady       = permNotification && permDnd && permContacts && permBattery,
                onMonitoringToggle  = { enabled ->
                    monitoringEnabled = enabled
                    EmergencyContactRepository.setMonitoringEnabled(context, enabled)
                    AppLog.log(if (enabled) "✅ Monitoring enabled" else "⏸️ Monitoring paused", context)
                },
                hasNotificationAccess = permNotification,
                isBatteryOptDisabled  = permBattery,
                onRequestNotification = onRequestNotificationAccess,
                onRequestBattery      = onRequestBatteryOptimization,
                isPremium             = isPremium,
                onShowPaywall         = onShowPaywall
            )
        }

        // ── Bottom Navigation (iPhone glassmorphic style) ──────
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 24.dp)
                .padding(horizontal = 52.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(
                        elevation = 20.dp,
                        shape = RoundedCornerShape(26.dp),
                        ambientColor = Color.Black.copy(alpha = 0.10f),
                        spotColor = Color.Black.copy(alpha = 0.07f)
                    )
                    .clip(RoundedCornerShape(26.dp))
                    .background(Color(0xFFFAF9F6).copy(alpha = 0.96f))
                    .border(1.dp, Color.White.copy(alpha = 0.8f), RoundedCornerShape(26.dp))
                    .padding(horizontal = 4.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                GlassNavItem(icon = Icons.Default.Home, label = "Home",     selected = currentTab == 0) { currentTab = 0 }
                GlassNavItem(icon = Icons.Default.NotificationsActive,  label = "Alerts",  selected = currentTab == 1) { currentTab = 1 }
                GlassNavItem(icon = Icons.Default.Settings, label = "Settings", selected = currentTab == 2) { currentTab = 2 }
            }
        }
    }
}

@Composable
private fun RowScope.GlassNavItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    val accentColor = Color(0xFFFFB703)
    val animatedSize by animateDpAsState(
        targetValue = if (selected) 22.dp else 20.dp,
        animationSpec = tween(200), label = "iconSize"
    )

    Column(
        modifier = Modifier
            .weight(1f)
            .weightedSpring()
            .clickable(
                interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                indication = null
            ) { onClick() }
            .padding(horizontal = 16.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Icon(
            icon,
            contentDescription = label,
            tint = if (selected) accentColor else Color(0xFF8A8A8A),
            modifier = Modifier.size(animatedSize)
        )
        Text(
            label,
            fontSize = 9.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            color = if (selected) accentColor else Color(0xFF8A8A8A),
            letterSpacing = 0.2.sp
        )
    }
}


