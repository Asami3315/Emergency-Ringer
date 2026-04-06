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

class MainActivity : ComponentActivity() {

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
            var showIntro by remember { mutableStateOf(true) } // TEMP: always show for testing
            EmergencyRingerTheme {
                if (showSplash) {
                    SplashScreen(onFinished = { showSplash = false })
                } else if (showIntro) {
                    IntroScreen(onComplete = {
                        prefs.edit().putBoolean("intro_completed", true).apply()
                        showIntro = false
                    })
                } else {
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
                    isBatteryOptimizationDisabled = { isBatteryOptimizationDisabled() },
                    onTestRinger = {
                        RingerManager.triggerEmergencyRinger(this)
                    },
                    onStopRinger = {
                        RingerManager.stopCurrentRinger()
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
                    }
                )
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
    onSelectRingtoneInSettings: () -> Unit
) {
    val context = LocalContext.current
    var contacts by remember { mutableStateOf(EmergencyContactRepository.getWhitelistSync(context)) }
    var monitoringEnabled by remember { mutableStateOf(EmergencyContactRepository.isMonitoringEnabled(context)) }
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
                onAddContact              = onAddContact,
                onRequestNotification     = onRequestNotificationAccess,
                onRequestDnd              = onRequestDndAccess,
                onRequestBattery          = onRequestBatteryOptimization,
                onRemoveContact           = onRemoveContact
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
                onRequestBattery      = onRequestBatteryOptimization
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
                GlassNavItem(icon = Icons.Default.NotificationsActive,  label = "Activity",  selected = currentTab == 1) { currentTab = 1 }
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


@Composable
fun ServiceStatusCard(isActive: Boolean, serviceConnected: Boolean) {
    val context = LocalContext.current
    var monitoringEnabled by remember { mutableStateOf(EmergencyContactRepository.isMonitoringEnabled(context)) }
    
    // Real-time sync with SharedPreferences (updates instantly when Quick Settings Tile changes)
    DisposableEffect(Unit) {
        val prefs = context.getSharedPreferences("emergency_contacts", Context.MODE_PRIVATE)
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key == "monitoring_enabled") {
                monitoringEnabled = EmergencyContactRepository.isMonitoringEnabled(context)
            }
        }
        prefs.registerOnSharedPreferenceChangeListener(listener)
        
        onDispose {
            prefs.unregisterOnSharedPreferenceChangeListener(listener)
        }
    }
    
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = EaseInOutCubic),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    val statusTitle = when {
        !monitoringEnabled -> "Monitoring Paused"
        isActive && serviceConnected -> "Protection is ON"
        !isActive -> "Setup Required"
        else -> "Protection is OFF"
    }
    
    val statusMessage = when {
        !monitoringEnabled -> "Tap the switch above to start monitoring calls."
        isActive && serviceConnected -> "Your phone will ring loudly for emergency contacts."
        !isActive -> "Enable permissions to activate protection"
        else -> "Tap the switch to start monitoring calls."
    }
    
    val statusColor = when {
        monitoringEnabled && isActive && serviceConnected -> VibrantPurple
        else -> Color(0xFFE53935) // Red for inactive/paused states
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .then(
                if (monitoringEnabled && isActive && serviceConnected)
                    Modifier.background(
                        Brush.horizontalGradient(
                            listOf(VibrantPurple.copy(alpha = 0.12f), DeepPurple.copy(alpha = 0.08f))
                        )
                    )
                else
                    Modifier.background(Color(0xFFF5F5F5))
            )
            .border(
                1.dp, 
                if (monitoringEnabled && isActive && serviceConnected) VibrantPurple.copy(alpha = 0.3f) else Color(0xFFE0E0E0), 
                RoundedCornerShape(24.dp)
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    // Pulsing status indicator
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .scale(if (monitoringEnabled && isActive && serviceConnected) pulseScale else 1f)
                            .background(
                                if (monitoringEnabled && isActive && serviceConnected) VibrantPurple else Color(0xFFE53935),
                                CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            when {
                                monitoringEnabled && isActive && serviceConnected -> Icons.Default.Shield
                                !monitoringEnabled -> Icons.Default.Block
                                else -> Icons.Default.Warning
                            },
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            statusTitle,
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold
                            ),
                            color = statusColor
                        )
                        Text(
                            statusMessage,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF666666),
                            maxLines = 2
                        )
                    }
                }
                
                // Master Toggle Switch
                Switch(
                    checked = monitoringEnabled,
                    onCheckedChange = { enabled ->
                        monitoringEnabled = enabled
                        EmergencyContactRepository.setMonitoringEnabled(context, enabled)
                        AppLog.log(if (enabled) "✅ Monitoring enabled" else "⏸️ Monitoring paused", context)
                    },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = VibrantPurple,
                        uncheckedThumbColor = Color.White,
                        uncheckedTrackColor = Color(0xFFCCCCCC)
                    )
                )
            }
            
            // Action button - only show when monitoring is ON but permissions are missing
            if (monitoringEnabled && (!isActive || !serviceConnected)) {
                Spacer(Modifier.height(12.dp))
                Button(
                    onClick = {
                        context.startActivity(android.content.Intent(android.provider.Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = VibrantPurple,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Enable Service", fontWeight = FontWeight.SemiBold)
                }
            }
            
            // Success banner when fully active
            if (monitoringEnabled && isActive && serviceConnected) {
                Spacer(Modifier.height(12.dp))
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    color = VibrantPurple.copy(alpha = 0.1f),
                    border = BorderStroke(1.dp, VibrantPurple.copy(alpha = 0.3f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = VibrantPurple,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            "All systems operational • Ready to protect",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                            color = VibrantPurple
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun PermissionsSection(
    hasNotificationAccess: Boolean,
    hasDndAccess: Boolean,
    hasContactsPermission: Boolean,
    isBatteryOptDisabled: Boolean,
    onRequestNotification: () -> Unit,
    onRequestDnd: () -> Unit,
    onRequestContacts: () -> Unit,
    onRequestBattery: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val rotation by animateFloatAsState(
        targetValue = if (expanded) 180f else 0f,
        animationSpec = tween(300),
        label = "arrow"
    )
    
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Dropdown Header
        Surface(
            onClick = { expanded = !expanded },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(12.dp),
            color = VibrantPurple.copy(alpha = 0.1f)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_settings_gear),
                        contentDescription = null,
                        tint = Color.Unspecified,
                        modifier = Modifier.size(24.dp)
                    )
                    Text(
                        "Setup Required",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.ExtraBold,
                            brush = Brush.linearGradient(
                                colors = listOf(
                                    Color(0xFF7C3AED),
                                    Color(0xFFD946EF)
                                )
                            )
                        ),
                        modifier = Modifier.padding(start = 4.dp)
                    )
                }
                Icon(
                    Icons.Default.ArrowDropDown,
                    contentDescription = if (expanded) "Collapse" else "Expand",
                    tint = VibrantPurple,
                    modifier = Modifier
                        .size(24.dp)
                        .graphicsLayer { rotationZ = rotation }
                )
            }
        }
        
        // Animated Permission Chips
        AnimatedVisibility(
            visible = expanded,
            enter = expandVertically(animationSpec = tween(300)) + fadeIn(),
            exit = shrinkVertically(animationSpec = tween(300)) + fadeOut()
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Subtitle text
                Text(
                    "Enable these 4 settings to allow the ringer to bypass silence.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF666666),
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
                CompactPermissionChip("Notifications", hasNotificationAccess, onRequestNotification)
                CompactPermissionChip("Do Not Disturb", hasDndAccess, onRequestDnd)
                CompactPermissionChip("Contacts", hasContactsPermission, onRequestContacts)
                CompactPermissionChip("Battery", isBatteryOptDisabled, onRequestBattery)
            }
        }
    }
}

@Composable
fun CompactPermissionChip(name: String, granted: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp),
        shape = RoundedCornerShape(12.dp),
        color = if (granted) VibrantPurple.copy(alpha = 0.12f) else Color(0xFFF0F0F0)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                name,
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                color = Color(0xFF1A1A1A)
            )
            Icon(
                if (granted) Icons.Default.CheckCircle else Icons.Default.Circle,
                contentDescription = null,
                tint = if (granted) VibrantPurple else Color(0xFFCCCCCC),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
fun BentoContactGrid(
    contacts: List<EmergencyContactRepository.Contact>,
    onRemove: (String, String) -> Unit,
    onUpdate: () -> Unit
) {
    val gridHeight = ((contacts.size / 2 + 1) * 140).dp
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.height(gridHeight)
    ) {
        items(contacts) { contact ->
            OrganicContactCard(
                name = contact.name,
                number = contact.number,
                onRemove = {
                    onRemove(contact.name, contact.number)
                    onUpdate()
                }
            )
        }
    }
}

@Composable
fun OrganicContactCard(name: String, number: String, onRemove: () -> Unit) {
    var showOptions by remember { mutableStateOf(false) }
    
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(130.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(GlassFrost.copy(alpha = 0.08f))
            .border(1.dp, GlassFrost.copy(alpha = 0.12f), RoundedCornerShape(20.dp))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Avatar with organic shape (squircle)
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        Brush.linearGradient(
                            listOf(VibrantPurple, DeepPurple)
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    name.firstOrNull()?.uppercaseChar()?.toString() ?: "?",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    color = Color.White
                )
            }

            Column {
                Text(
                    name,
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    color = Color(0xFF1A1A1A),
                    maxLines = 1
                )
                if (number.isNotBlank()) {
                    Text(
                        number,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF666666),
                        maxLines = 1
                    )
                }
            }
        }

        // Remove button
        IconButton(
            onClick = onRemove,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .size(32.dp)
        ) {
            Icon(
                painter = painterResource(R.drawable.cross),
                contentDescription = "Remove",
                tint = Color.Unspecified,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

@Composable
fun EmptyContactsState(onAdd: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(GlassFrost.copy(alpha = 0.05f)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                Icons.Default.PersonAdd,
                contentDescription = null,
                tint = Color(0xFFCCCCCC),
                modifier = Modifier.size(48.dp)
            )
            Text(
                "No emergency contacts yet",
                style = MaterialTheme.typography.bodyLarge,
                color = Color(0xFF666666)
            )
            TextButton(onClick = onAdd) {
                Text("Add Contact", color = VibrantPurple)
            }
        }
    }
}

@Composable
fun LogsDialog(logs: List<String>, onDismiss: () -> Unit) {
    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.8f),
            shape = RoundedCornerShape(24.dp),
            color = SurfaceWhite
        ) {
            Column(Modifier.padding(20.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Debug Logs",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        color = Color(0xFF1A1A1A)
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        IconButton(onClick = { AppLog.refreshFromFile() }) {
                            Icon(Icons.Default.Refresh, "Refresh", tint = VibrantPurple)
                        }
                        IconButton(onClick = { AppLog.clear() }) {
                            Icon(Icons.Default.Delete, "Clear", tint = Color(0xFF666666))
                        }
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.Close, "Close", tint = Color(0xFF666666))
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))

                LaunchedEffect(Unit) { AppLog.refreshFromFile() }
                
                val scroll = rememberScrollState()
                Column(
                    Modifier
                        .fillMaxSize()
                        .verticalScroll(scroll),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    if (logs.isEmpty()) {
                        Text(
                            "No logs yet. Tap test or receive a call.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFF666666)
                        )
                    } else {
                        logs.forEach { msg ->
                            Text(
                                msg,
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                                ),
                                color = Color(0xFF1A1A1A)
                            )
                        }
                    }
                }
            }
        }
    }
}
