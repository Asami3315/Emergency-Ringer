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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.emergencyringer.app.BuildConfig

private val NeoBg      = Color(0xFFFFFDF8)   // ultra-clean cream from HTML
private val SttCard    = Color(0xFFFFFFFF)
private val NeoPrimary = Color(0xFFFFB703)
private val NeoTextC   = Color(0xFF121212)
private val NeoMutedC  = Color(0xFF949494)
private val NeoBorderC = Color(0xFFF2F0EA)

@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onSelectRingtone: () -> Unit,
    onTestRinger: () -> Unit,
    onStopRinger: () -> Unit,
    vibrantPurple: Color,
    deepPurple: Color,
    hasNotificationAccess: Boolean = false,
    isBatteryOptDisabled: Boolean = false,
    onRequestNotification: () -> Unit = {},
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
    var showIntroPreview by remember { mutableStateOf(false) }

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

    // Intro preview overlay
    if (showIntroPreview) {
        IntroScreen(onComplete = { showIntroPreview = false })
        return
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
    ) {
        // Background image
        Image(
            painter = painterResource(id = R.drawable.home_bg),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )
        // Semi-transparent overlay for readability
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.10f))
        )
        // Top white gradient shade for header readability
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(140.dp)
                .background(Brush.verticalGradient(listOf(Color(0xFFFDFBF7).copy(alpha = 0.9f), Color.Transparent)))
        )

        Column(modifier = Modifier.fillMaxSize()) {

            // ── Header ───────────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(top = 52.dp, bottom = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "SYSTEM CONTROL",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = NeoTextC.copy(alpha = 0.8f),
                    letterSpacing = 0.5.sp
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
                                Text("SMS override enabled", fontSize = 12.sp, color = NeoMutedC, fontWeight = FontWeight.Medium)
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

                // ── Permissions card (below Message Alerts) ─────
                SttPermissionsCard(
                    hasNotificationAccess = hasNotificationAccess,
                    isBatteryOptDisabled  = isBatteryOptDisabled,
                    onRequestNotification = onRequestNotification,
                    onRequestBattery      = onRequestBattery
                )

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
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(16.dp))
                                            .background(if (selected) NeoPrimary else Color.Transparent)
                                            .clickable(
                                                indication = null,
                                                interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
                                            ) {
                                                autoStopDuration = ms
                                                EmergencyContactRepository.setAutoStopDuration(context, ms)
                                            }
                                            .padding(vertical = 12.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            label,
                                            fontSize = 14.sp,
                                            fontWeight = if (selected) FontWeight.ExtraBold else FontWeight.SemiBold,
                                            color = if (selected) Color.White else NeoTextC
                                        )
                                    }
                                }
                            }
                        }

                        // Gradient divider from code.html
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(1.dp)
                                .background(
                                    Brush.horizontalGradient(
                                        listOf(Color.Transparent, NeoBorderC, Color.Transparent)
                                    )
                                )
                        )

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
                var selectedVolume by remember { mutableStateOf(EmergencyContactRepository.getVolumePercent(context)) }
                var previousVolume by remember { mutableStateOf(100) }
                NeoSettingsCard(modifier = Modifier.fillMaxWidth()) {
                    Column(verticalArrangement = Arrangement.spacedBy(28.dp)) {
                        // Section header
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
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
                            // Mute toggle icon
                            val isMuted = selectedVolume == 0
                            Icon(
                                if (isMuted) Icons.Default.VolumeOff else Icons.Default.VolumeUp,
                                contentDescription = if (isMuted) "Unmute" else "Mute",
                                tint = if (isMuted) NeoMutedC else NeoPrimary,
                                modifier = Modifier
                                    .size(24.dp)
                                    .clickable(
                                        indication = null,
                                        interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
                                    ) {
                                        if (isMuted) {
                                            // Unmute → restore to 100%
                                            selectedVolume = 100
                                            EmergencyContactRepository.setVolumePercent(context, 100)
                                        } else {
                                            // Mute → set to 0
                                            previousVolume = selectedVolume
                                            selectedVolume = 0
                                            EmergencyContactRepository.setVolumePercent(context, 0)
                                        }
                                    }
                            )
                        }

                        // ── Volume display + bars ─────────────────
                        val volumeLevels = listOf(40, 60, 75, 90, 100)
                        // Map stored volume to closest bar index (-1 when muted/0)
                        val activeIndex = if (selectedVolume == 0) -1 else volumeLevels.indexOfFirst { it >= selectedVolume }.let { if (it == -1) 4 else it }

                        Column {
                            // Volume % + MAX LOUD row
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.Bottom
                            ) {
                                Column {
                                    Text(
                                        "${if (activeIndex == -1) 0 else volumeLevels[activeIndex]}%",
                                        fontSize = 44.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = NeoTextC,
                                        letterSpacing = (-2).sp
                                    )
                                    Text(
                                        "Current Level",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = NeoMutedC
                                    )
                                }
                                if (activeIndex == -1) {
                                    Text(
                                        "MUTED",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = NeoMutedC,
                                        letterSpacing = 1.sp,
                                        modifier = Modifier
                                            .background(Color(0xFFF3F4F6), RoundedCornerShape(50.dp))
                                            .padding(horizontal = 14.dp, vertical = 7.dp)
                                    )
                                } else if (activeIndex >= 0 && volumeLevels[activeIndex] >= 90) {
                                    Text(
                                        "MAX LOUD",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = NeoPrimary,
                                        letterSpacing = 1.sp,
                                        modifier = Modifier
                                            .background(Color(0xFFFFFBEB), RoundedCornerShape(50.dp))
                                            .border(1.dp, Color(0xFFFEF3C7), RoundedCornerShape(50.dp))
                                            .padding(horizontal = 14.dp, vertical = 7.dp)
                                    )
                                }
                            }

                            Spacer(Modifier.height(24.dp))

                            // Interactive volume bars
                            val barFractions = listOf(0.40f, 0.60f, 0.75f, 0.90f, 1.00f)
                            Row(
                                modifier = Modifier.fillMaxWidth().height(140.dp).padding(horizontal = 4.dp),
                                horizontalArrangement = Arrangement.spacedBy(14.dp),
                                verticalAlignment = Alignment.Bottom
                            ) {
                                barFractions.forEachIndexed { index, fraction ->
                                    val isActive = index == activeIndex
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .fillMaxHeight()
                                            .clickable(
                                                indication = null,
                                                interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
                                            ) {
                                                selectedVolume = volumeLevels[index]
                                                EmergencyContactRepository.setVolumePercent(context, volumeLevels[index])
                                            }
                                    ) {
                                        // The bar itself
                                        Box(
                                            modifier = Modifier
                                                .align(Alignment.BottomCenter)
                                                .fillMaxWidth()
                                                .fillMaxHeight(fraction)
                                                .background(
                                                    if (isActive) NeoPrimary else Color(0xFFF3F4F6),
                                                    RoundedCornerShape(50.dp)
                                                )
                                        )
                                        // VOL label on the active bar
                                        if (isActive) {
                                            Box(
                                                modifier = Modifier
                                                    .align(Alignment.TopCenter)
                                                    .offset(y = (140 * (1f - fraction) - 40).dp)
                                                    .background(NeoTextC, RoundedCornerShape(50.dp))
                                                    .padding(horizontal = 10.dp, vertical = 5.dp)
                                            ) {
                                                Text("VOL", fontSize = 9.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
                                            }
                                        }
                                    }
                                }
                            }

                            Spacer(Modifier.height(16.dp))

                            Text(
                                "Tap bars to adjust override level",
                                fontSize = 12.sp,
                                color = NeoMutedC,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.fillMaxWidth(),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }

                        // ── System Setup (inline selector) ───────────
                        val isPhone = ringtoneSource == EmergencyContactRepository.RINGTONE_SOURCE_PHONE
                        var systemExpanded by remember { mutableStateOf(false) }

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .shadow(8.dp, RoundedCornerShape(24.dp), spotColor = Color.Black.copy(alpha = 0.06f))
                                .border(1.dp, Color.White.copy(alpha = 0.4f), RoundedCornerShape(24.dp))
                                .background(Color.White.copy(alpha = 0.5f), RoundedCornerShape(24.dp))
                                .padding(18.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Box(modifier = Modifier.size(6.dp).background(NeoPrimary, CircleShape))
                                Text("SYSTEM SETUP", fontSize = 10.sp, fontWeight = FontWeight.ExtraBold, color = NeoMutedC, letterSpacing = 1.sp)
                            }

                            // Currently selected option (tap to expand)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color.White, RoundedCornerShape(18.dp))
                                    .clickable(
                                        indication = null,
                                        interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
                                    ) { systemExpanded = !systemExpanded }
                                    .padding(horizontal = 18.dp, vertical = 14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                    Icon(
                                        if (isPhone) Icons.Default.PhoneAndroid else Icons.Default.MusicNote,
                                        contentDescription = null, tint = NeoPrimary, modifier = Modifier.size(20.dp)
                                    )
                                    Text(
                                        if (isPhone) "System Ringtone" else "Custom Sound",
                                        fontSize = 15.sp, fontWeight = FontWeight.Bold, color = NeoTextC
                                    )
                                }
                                Icon(
                                    if (systemExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                    contentDescription = null, tint = NeoMutedC, modifier = Modifier.size(22.dp)
                                )
                            }

                            // Inline options (only the OTHER option shown)
                            androidx.compose.animation.AnimatedVisibility(visible = systemExpanded) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(Color.White, RoundedCornerShape(18.dp))
                                        .clickable(
                                            indication = null,
                                            interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
                                        ) {
                                            if (isPhone) {
                                                ringtoneSource = EmergencyContactRepository.RINGTONE_SOURCE_CUSTOM
                                                EmergencyContactRepository.setRingtoneSource(context, EmergencyContactRepository.RINGTONE_SOURCE_CUSTOM)
                                            } else {
                                                ringtoneSource = EmergencyContactRepository.RINGTONE_SOURCE_PHONE
                                                EmergencyContactRepository.setRingtoneSource(context, EmergencyContactRepository.RINGTONE_SOURCE_PHONE)
                                            }
                                            systemExpanded = false
                                        }
                                        .padding(horizontal = 18.dp, vertical = 14.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Icon(
                                        if (isPhone) Icons.Default.MusicNote else Icons.Default.PhoneAndroid,
                                        contentDescription = null, tint = NeoMutedC, modifier = Modifier.size(20.dp)
                                    )
                                    Text(
                                        if (isPhone) "Custom Sound" else "System Ringtone",
                                        fontSize = 15.sp, fontWeight = FontWeight.Medium, color = NeoTextC
                                    )
                                }
                            }

                            // Custom sound details (shown only when Custom Sound selected)
                            if (!isPhone) {
                                // Selected ringtone name
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(Color.White, RoundedCornerShape(18.dp))
                                        .padding(horizontal = 18.dp, vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("Selected", fontSize = 11.sp, color = NeoPrimary, fontWeight = FontWeight.Bold)
                                        Text(ringtoneName, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = NeoTextC, maxLines = 1)
                                    }
                                }

                                // Change + Preview buttons (stacked, full width like dropdown)
                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    OutlinedButton(
                                        onClick = onSelectRingtone,
                                        modifier = Modifier.fillMaxWidth().height(48.dp),
                                        shape = RoundedCornerShape(14.dp),
                                        border = BorderStroke(1.dp, NeoPrimary),
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = NeoPrimary)
                                    ) {
                                        Icon(Icons.Default.LibraryMusic, null, Modifier.size(18.dp))
                                        Spacer(Modifier.width(8.dp))
                                        Text("Change Ringtone", fontWeight = FontWeight.Bold)
                                    }
                                    Button(
                                        onClick = if (isPlaying) onStopRinger else onTestRinger,
                                        modifier = Modifier.fillMaxWidth().height(48.dp),
                                        shape = RoundedCornerShape(14.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = if (isPlaying) Color(0xFFC53030) else NeoPrimary
                                        )
                                    ) {
                                        Icon(if (isPlaying) Icons.Default.Stop else Icons.Default.PlayArrow, null, Modifier.size(18.dp))
                                        Spacer(Modifier.width(8.dp))
                                        Text(if (isPlaying) "Stop" else "Preview Ringtone", color = Color.White, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }

                    }
                }

                // ── Links (Support / Privacy / Share) ─────────
                Box(
                    modifier = Modifier.fillMaxWidth()
                        .shadow(20.dp, RoundedCornerShape(28.dp), spotColor = Color.Black.copy(alpha = 0.07f))
                        .border(1.dp, Color.White.copy(alpha = 0.6f), RoundedCornerShape(28.dp))
                        .background(Color.White.copy(alpha = 0.90f), RoundedCornerShape(28.dp))
                        .clip(RoundedCornerShape(28.dp)),
                ) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        LinkRow(icon = Icons.Default.SupportAgent, label = "Support") {
                            val intent = android.content.Intent(android.content.Intent.ACTION_SENDTO).apply {
                                data = android.net.Uri.parse("mailto:nexustec.official@gmail.com")
                                putExtra(android.content.Intent.EXTRA_SUBJECT, "Emergency Ringer Support")
                            }
                            try { context.startActivity(intent) } catch (_: Exception) {}
                        }
                        Divider(color = Color(0xFFF5F5F5), thickness = 1.dp, modifier = Modifier.padding(horizontal = 16.dp))
                        LinkRow(icon = Icons.Default.Policy, label = "Privacy Policy") {
                            val intent = android.content.Intent(android.content.Intent.ACTION_VIEW,
                                android.net.Uri.parse("https://emergencyringer.com/privacy"))
                            try { context.startActivity(intent) } catch (_: Exception) {}
                        }
                        Divider(color = Color(0xFFF5F5F5), thickness = 1.dp, modifier = Modifier.padding(horizontal = 16.dp))
                        LinkRow(icon = Icons.Default.Share, label = "Share App") {
                            val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(android.content.Intent.EXTRA_TEXT, "Check out Emergency Ringer - always reachable when it matters! https://play.google.com/store/apps/details?id=${context.packageName}")
                            }
                            context.startActivity(android.content.Intent.createChooser(intent, "Share via"))
                        }
                    }
                }

                // ── TEST: Preview Intro Screen (temporary) ───
                OutlinedButton(
                    onClick = { showIntroPreview = true },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    border = BorderStroke(1.dp, NeoPrimary.copy(alpha = 0.3f)),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = NeoPrimary)
                ) {
                    Icon(Icons.Default.Visibility, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Preview Intro Screen", fontWeight = FontWeight.Bold, fontSize = 14.sp)
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
    Box(
        modifier = modifier
            .shadow(20.dp, RoundedCornerShape(28.dp), spotColor = Color.Black.copy(alpha = 0.07f))
            .border(1.dp, Color.White.copy(alpha = 0.6f), RoundedCornerShape(28.dp))
            .background(Color.White.copy(alpha = 0.90f), RoundedCornerShape(28.dp))
            .clip(RoundedCornerShape(28.dp))
    ) {
        Column(modifier = Modifier.padding(28.dp), content = content)
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
                    .background(if (checked) NeoPrimary.copy(alpha = 0.1f) else Color(0xFFF9FAFB), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = if (checked) NeoPrimary else NeoMutedC, modifier = Modifier.size(22.dp))
            }
            Text(label, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = NeoTextC.copy(alpha = 0.8f))
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
private fun LinkRow(icon: ImageVector, label: String, onClick: () -> Unit = {}) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(NeoPrimary.copy(alpha = 0.1f), RoundedCornerShape(14.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = NeoPrimary, modifier = Modifier.size(22.dp))
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
    isBatteryOptDisabled: Boolean,
    onRequestNotification: () -> Unit,
    onRequestBattery: () -> Unit
) {
    val readyCount = listOf(hasNotificationAccess, isBatteryOptDisabled).count { it }
    var expanded by remember { mutableStateOf(readyCount < 2) }

    Box(
        modifier = Modifier.fillMaxWidth()
            .shadow(20.dp, RoundedCornerShape(28.dp), spotColor = Color.Black.copy(alpha = 0.07f))
            .border(1.dp, Color.White.copy(alpha = 0.6f), RoundedCornerShape(28.dp))
            .background(Color.White.copy(alpha = 0.90f), RoundedCornerShape(28.dp))
            .clip(RoundedCornerShape(28.dp))
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
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.weight(1f)) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .background(NeoPrimary.copy(alpha = 0.1f), RoundedCornerShape(14.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Tune, contentDescription = null,
                            tint = NeoPrimary, modifier = Modifier.size(22.dp))
                    }
                    Column {
                        Text("System Control", fontWeight = FontWeight.Bold,
                            fontSize = 16.sp, color = NeoTextC, maxLines = 1)
                        Text("Permissions", fontSize = 12.sp,
                            color = NeoMutedC, fontWeight = FontWeight.Medium)
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (readyCount == 2) {
                        Text("✓ Ready", fontSize = 11.sp, fontWeight = FontWeight.Bold,
                            color = NeoPrimary, maxLines = 1,
                            modifier = Modifier
                                .background(NeoPrimary.copy(alpha = 0.1f), RoundedCornerShape(6.dp))
                                .padding(horizontal = 8.dp, vertical = 4.dp))
                    } else {
                        Text("$readyCount/2", fontSize = 10.sp, fontWeight = FontWeight.Bold,
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
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onRequest() }
            .padding(vertical = 4.dp),
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
