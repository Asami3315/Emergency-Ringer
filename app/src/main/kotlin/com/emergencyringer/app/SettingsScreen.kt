package com.emergencyringer.app

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.emergencyringer.app.BuildConfig

private val NeoBg      = Color(0xFFF4F1EA)   // warm cream matching Stitch
private val SttCard    = Color(0xFFFFFFFF)
private val NeoPrimary = Color(0xFFFFB703)
private val NeoTextC   = Color(0xFF1A1A1A)
private val NeoMutedC  = Color(0xFF8C8882)
private val NeoBorderC = Color(0xFFF0EDE6)

@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onSelectRingtone: () -> Unit,
    onTestRinger: () -> Unit,
    onStopRinger: () -> Unit,
    vibrantPurple: Color,
    deepPurple: Color,
    hasNotificationAccess: Boolean = false,
    hasDndAccess: Boolean = false,
    isBatteryOptDisabled: Boolean = false,
    onRequestNotification: () -> Unit = {},
    onRequestDnd: () -> Unit = {},
    onRequestBattery: () -> Unit = {}
) {
    val context = LocalContext.current
    var autoStopDuration by remember { mutableStateOf(EmergencyContactRepository.getAutoStopDuration(context)) }
    var vibrateEnabled   by remember { mutableStateOf(EmergencyContactRepository.isVibrateEnabled(context)) }
    var flashEnabled     by remember { mutableStateOf(EmergencyContactRepository.isFlashlightEnabled(context)) }
    var msgEnabled       by remember { mutableStateOf(EmergencyContactRepository.isMessageAlertEnabled(context)) }
    var ringtoneSource   by remember { mutableStateOf(EmergencyContactRepository.getRingtoneSource(context)) }
    var isPlaying        by remember { mutableStateOf(false) }
    var ringtoneRefresh  by remember { mutableStateOf(0) }

    val ringtoneName = remember(ringtoneRefresh) {
        val saved = EmergencyContactRepository.getRingtoneName(context)
        if (!saved.isNullOrBlank()) saved else "Pick your favorite alarm sound"
    }

    LaunchedEffect(Unit) {
        while (true) {
            kotlinx.coroutines.delay(300)
            isPlaying = EmergencyContactRepository.isRingerPlaying
        }
    }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        ringtoneRefresh++
        ringtoneSource = EmergencyContactRepository.getRingtoneSource(context)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(listOf(Color(0xFFFDFBF7), Color(0xFFF4F1EA)))
            )
    ) {
        Column(modifier = Modifier.fillMaxSize()) {

            // ── Header ───────────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(top = 52.dp, bottom = 12.dp)
            ) {
                Text(
                    "System Control",
                    modifier = Modifier.align(Alignment.Center),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = NeoTextC,
                    letterSpacing = (-0.3).sp
                )
                CircleIconButton(
                    icon = Icons.Default.MoreHoriz,
                    onClick = {},
                    modifier = Modifier.align(Alignment.CenterEnd)
                )
            }

            // ── Scrollable content ────────────────────────────
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 120.dp),  // extra space for floating nav bar
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {

                // ── Permissions card (moved from Home tab) ─────
                SttPermissionsCard(
                    hasNotificationAccess = hasNotificationAccess,
                    hasDndAccess          = hasDndAccess,
                    isBatteryOptDisabled  = isBatteryOptDisabled,
                    onRequestNotification = onRequestNotification,
                    onRequestDnd          = onRequestDnd,
                    onRequestBattery      = onRequestBattery
                )

                // ── Shield Status Card (gradient border) ──────
                ShieldStatusCard()

                // ── Message Alerts ────────────────────────────
                NeoSettingsCard(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            Box(
                                modifier = Modifier
                                    .size(56.dp)
                                    .background(Color(0xFFFFF3E0), RoundedCornerShape(20.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Chat, contentDescription = null, tint = Color(0xFFE67E22), modifier = Modifier.size(24.dp))
                            }
                            Column {
                                Text("Message Alerts", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = NeoTextC)
                                Text("SMS & chat override", fontSize = 12.sp, color = NeoMutedC, fontWeight = FontWeight.Medium)
                            }
                        }
                        Switch(
                            checked = msgEnabled,
                            onCheckedChange = {
                                msgEnabled = it
                                EmergencyContactRepository.setMessageAlertEnabled(context, it)
                            },
                            colors = SwitchDefaults.colors(
                                checkedTrackColor = NeoPrimary,
                                checkedThumbColor = Color.White,
                                uncheckedTrackColor = Color(0xFFE0E0E0)
                            )
                        )
                    }
                }

                // ── Alarm Configuration ───────────────────────
                NeoSettingsCard(modifier = Modifier.fillMaxWidth()) {
                    Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
                        Text(
                            "ALARM CONFIGURATION",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = NeoMutedC,
                            letterSpacing = 1.5.sp
                        )

                        // Auto-stop
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Auto-stop", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = NeoTextC)
                                Box(
                                    modifier = Modifier
                                        .background(Color(0xFFF8F8F8), RoundedCornerShape(20.dp))
                                        .padding(horizontal = 12.dp, vertical = 5.dp)
                                ) {
                                    Text("Silence timer", fontSize = 11.sp, color = NeoMutedC, fontWeight = FontWeight.Medium)
                                }
                            }
                            // Pill button row
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color(0xFFF5F5F5), RoundedCornerShape(22.dp))
                                    .padding(6.dp),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                listOf("30s" to 30_000L, "1m" to 60_000L, "5m" to 300_000L).forEach { (label, ms) ->
                                    val selected = autoStopDuration == ms
                                    Card(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clickable {
                                                autoStopDuration = ms
                                                EmergencyContactRepository.setAutoStopDuration(context, ms)
                                            },
                                        shape = RoundedCornerShape(16.dp),
                                        colors = CardDefaults.cardColors(
                                            containerColor = if (selected) SttCard else Color.Transparent
                                        ),
                                        elevation = CardDefaults.cardElevation(
                                            defaultElevation = if (selected) 2.dp else 0.dp
                                        )
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 12.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                label,
                                                fontSize = 14.sp,
                                                fontWeight = if (selected) FontWeight.ExtraBold else FontWeight.SemiBold,
                                                color = if (selected) NeoPrimary else NeoMutedC
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        Divider(color = NeoBorderC, thickness = 1.dp)

                        // Vibrate toggle
                        NeoToggleRow(
                            icon = Icons.Default.Vibration,
                            label = "Haptic Pattern",
                            checked = vibrateEnabled,
                            onCheckedChange = {
                                vibrateEnabled = it
                                EmergencyContactRepository.setVibrateEnabled(context, it)
                            }
                        )

                        // Flashlight toggle
                        NeoToggleRow(
                            icon = Icons.Default.FlashlightOn,
                            label = "Strobe Light",
                            checked = flashEnabled,
                            onCheckedChange = {
                                flashEnabled = it
                                EmergencyContactRepository.setFlashlightEnabled(context, it)
                            }
                        )
                    }
                }

                // ── Audio Output ──────────────────────────────
                NeoSettingsCard(modifier = Modifier.fillMaxWidth()) {
                    Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "AUDIO OUTPUT",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = NeoMutedC,
                                letterSpacing = 1.5.sp
                            )
                            Icon(Icons.Default.VolumeUp, contentDescription = null, tint = NeoPrimary, modifier = Modifier.size(22.dp))
                        }

                        // Alarm Tone label
                        Text("Alarm Tone", fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, color = NeoMutedC, letterSpacing = 0.5.sp)

                        // Phone Ringtone option
                        val isPhone = ringtoneSource == EmergencyContactRepository.RINGTONE_SOURCE_PHONE
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            ToneChip(
                                label = "Phone Ringtone",
                                icon = Icons.Default.PhoneAndroid,
                                selected = isPhone,
                                onClick = {
                                    ringtoneSource = EmergencyContactRepository.RINGTONE_SOURCE_PHONE
                                    EmergencyContactRepository.setRingtoneSource(context, EmergencyContactRepository.RINGTONE_SOURCE_PHONE)
                                }
                            )
                            ToneChip(
                                label = "Custom Sound",
                                icon = Icons.Default.MusicNote,
                                selected = !isPhone,
                                onClick = {
                                    ringtoneSource = EmergencyContactRepository.RINGTONE_SOURCE_CUSTOM
                                    EmergencyContactRepository.setRingtoneSource(context, EmergencyContactRepository.RINGTONE_SOURCE_CUSTOM)
                                }
                            )
                        }

                        // Custom sound row
                        if (!isPhone) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color(0xFFFAFAFA), RoundedCornerShape(20.dp))
                                    .border(1.dp, NeoPrimary.copy(alpha = 0.2f), RoundedCornerShape(20.dp))
                                    .padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                Column {
                                    Text("Selected", fontSize = 11.sp, color = NeoPrimary, fontWeight = FontWeight.Bold)
                                    Text(ringtoneName, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = NeoTextC)
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    OutlinedButton(
                                        onClick = onSelectRingtone,
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(14.dp),
                                        border = BorderStroke(1.dp, NeoPrimary),
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = NeoPrimary)
                                    ) {
                                        Icon(Icons.Default.LibraryMusic, null, Modifier.size(16.dp))
                                        Spacer(Modifier.width(6.dp))
                                        Text("Change")
                                    }
                                    Button(
                                        onClick = if (isPlaying) onStopRinger else onTestRinger,
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(14.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = if (isPlaying) Color(0xFFC53030) else NeoPrimary
                                        )
                                    ) {
                                        Icon(if (isPlaying) Icons.Default.Stop else Icons.Default.PlayArrow, null, Modifier.size(16.dp))
                                        Spacer(Modifier.width(6.dp))
                                        Text(if (isPlaying) "Stop" else "Preview", color = Color.White)
                                    }
                                }
                            }
                        }
                    }
                }

                // ── Links (Support / Privacy / Share) ─────────
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(28.dp),
                    colors = CardDefaults.cardColors(containerColor = SttCard),
                    elevation = CardDefaults.cardElevation(1.dp)
                ) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        LinkRow(icon = Icons.Default.SupportAgent, label = "Support")
                        Divider(color = Color(0xFFF5F5F5), thickness = 1.dp, modifier = Modifier.padding(horizontal = 16.dp))
                        LinkRow(icon = Icons.Default.Policy, label = "Privacy Policy")
                        Divider(color = Color(0xFFF5F5F5), thickness = 1.dp, modifier = Modifier.padding(horizontal = 16.dp))
                        LinkRow(icon = Icons.Default.Share, label = "Share App")
                    }
                }

                // ── Version footer ────────────────────────────
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        "Version ${BuildConfig.VERSION_NAME}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = NeoMutedC.copy(alpha = 0.5f)
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "NEXUS-TEC SHIELD",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = NeoMutedC.copy(alpha = 0.3f),
                        letterSpacing = 3.sp
                    )
                }
            }
        }
    }
}

// ── Sub-components ────────────────────────────────────────────

@Composable
private fun ShieldStatusCard() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .border(2.dp, Brush.linearGradient(listOf(Color(0xFFFFE5A0), Color(0xFFE0F2FE), Color(0xFFFFE5A0))), RoundedCornerShape(32.dp))
            .background(SttCard, RoundedCornerShape(32.dp))
            .padding(28.dp)
    ) {
        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.padding(bottom = 14.dp)
                ) {
                    Box(modifier = Modifier.size(10.dp)) {
                        Box(modifier = Modifier.fillMaxSize().background(Color(0xFF34D399).copy(alpha = 0.4f), CircleShape))
                        Box(modifier = Modifier.size(6.dp).align(Alignment.Center).background(Color(0xFF10B981), CircleShape))
                    }
                    Text("SYSTEM ACTIVE", fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF059669), letterSpacing = 1.sp)
                }
                Text("Emergency\nShield", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = NeoTextC, lineHeight = 32.sp)
                Spacer(Modifier.height(10.dp))
                Text(
                    "Whitelisted contacts bypass Do Not Disturb. Monitoring active.",
                    fontSize = 13.sp,
                    color = NeoMutedC,
                    fontWeight = FontWeight.Medium,
                    lineHeight = 18.sp
                )
            }
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .background(NeoPrimary.copy(alpha = 0.1f), RoundedCornerShape(22.dp))
                    .border(1.dp, NeoPrimary.copy(alpha = 0.2f), RoundedCornerShape(22.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Shield, contentDescription = null, tint = NeoPrimary, modifier = Modifier.size(36.dp))
            }
        }
    }
}

@Composable
private fun NeoSettingsCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = SttCard),
        elevation = CardDefaults.cardElevation(1.dp)
    ) {
        Column(modifier = Modifier.padding(24.dp), content = content)
    }
}

@Composable
private fun CircleIconButton(
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(SttCard.copy(alpha = 0.7f))
            .border(1.dp, NeoBorderC, CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = null, tint = NeoTextC, modifier = Modifier.size(20.dp))
    }
}

@Composable
private fun NeoToggleRow(icon: ImageVector, label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(
                        if (checked) NeoPrimary.copy(alpha = 0.12f) else Color(0xFFF0EDE6),
                        RoundedCornerShape(14.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = if (checked) NeoPrimary else NeoMutedC, modifier = Modifier.size(22.dp))
            }
            Text(label, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = NeoTextC)
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedTrackColor = NeoPrimary,
                checkedThumbColor = Color.White,
                uncheckedTrackColor = Color(0xFFE0E0E0)
            )
        )
    }
}

@Composable
private fun ToneChip(label: String, icon: ImageVector, selected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(50.dp))
            .background(if (selected) NeoPrimary else Color.White)
            .border(1.dp, if (selected) Color.Transparent else Color(0xFFE5E5E5), RoundedCornerShape(50.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(icon, contentDescription = null, tint = if (selected) Color.White else NeoMutedC, modifier = Modifier.size(18.dp))
        Text(label, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = if (selected) Color.White else NeoMutedC)
    }
}

@Composable
private fun LinkRow(icon: ImageVector, label: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .clickable { }
            .padding(horizontal = 16.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(Color(0xFFF8F8F8), RoundedCornerShape(14.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = NeoMutedC, modifier = Modifier.size(22.dp))
            }
            Text(label, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = NeoTextC)
        }
        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Color(0xFFCCCCCC), modifier = Modifier.size(22.dp))
    }
}

// ── Permissions Card (moved from Home screen) ─────────────────
@Composable
private fun SttPermissionsCard(
    hasNotificationAccess: Boolean,
    hasDndAccess: Boolean,
    isBatteryOptDisabled: Boolean,
    onRequestNotification: () -> Unit,
    onRequestDnd: () -> Unit,
    onRequestBattery: () -> Unit
) {
    val readyCount = listOf(hasNotificationAccess, hasDndAccess, isBatteryOptDisabled).count { it }
    var expanded by remember { mutableStateOf(readyCount < 3) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = SttCard),
        elevation = CardDefaults.cardElevation(1.dp)
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            // Header row (always visible, tap to expand)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .clickable { expanded = !expanded }
                    .padding(horizontal = 16.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .background(NeoPrimary.copy(alpha = 0.1f), RoundedCornerShape(16.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Tune, contentDescription = null,
                            tint = NeoPrimary, modifier = Modifier.size(24.dp))
                    }
                    Column {
                        Text("System Control", fontWeight = FontWeight.Bold,
                            fontSize = 17.sp, color = NeoTextC)
                        Text("Configuration & Permissions", fontSize = 12.sp,
                            color = NeoMutedC, fontWeight = FontWeight.Medium)
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (readyCount == 3) {
                        Text("ALL READY", fontSize = 10.sp, fontWeight = FontWeight.Bold,
                            color = NeoPrimary,
                            modifier = Modifier
                                .background(NeoPrimary.copy(alpha = 0.1f), RoundedCornerShape(6.dp))
                                .padding(horizontal = 8.dp, vertical = 4.dp))
                    } else {
                        Text("$readyCount/3", fontSize = 10.sp, fontWeight = FontWeight.Bold,
                            color = Color(0xFFC53030),
                            modifier = Modifier
                                .background(Color(0xFFFED7D7).copy(alpha = 0.5f), RoundedCornerShape(6.dp))
                                .padding(horizontal = 8.dp, vertical = 4.dp))
                    }
                    Icon(
                        if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = null,
                        tint = if (expanded) NeoPrimary else NeoMutedC,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            // Expandable permissions list
            if (expanded) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .padding(bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFFF8F6F0), RoundedCornerShape(16.dp))
                            .padding(16.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                            SttPermissionRow("Notification Access", hasNotificationAccess, onRequestNotification)
                            SttPermissionRow("DND Access", hasDndAccess, onRequestDnd)
                            SttPermissionRow("Battery Optimization", isBatteryOptDisabled, onRequestBattery)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SttPermissionRow(title: String, granted: Boolean, onRequest: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = NeoMutedC)
        if (granted) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .background(Color(0xFF4CD964), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Check, contentDescription = null,
                    tint = Color.White, modifier = Modifier.size(14.dp))
            }
        } else {
            TextButton(
                onClick = onRequest,
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                colors = ButtonDefaults.textButtonColors(contentColor = NeoPrimary)
            ) {
                Text("Enable", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}
