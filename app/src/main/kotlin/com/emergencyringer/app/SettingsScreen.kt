package com.emergencyringer.app

import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.content.Context
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onSelectRingtone: () -> Unit,
    onTestRinger: () -> Unit,
    onStopRinger: () -> Unit,
    vibrantPurple: Color,
    deepPurple: Color
) {
    val context = LocalContext.current
    
    // Settings state
    var autoStopDuration by remember { mutableStateOf(EmergencyContactRepository.getAutoStopDuration(context)) }
    var vibrateEnabled by remember { mutableStateOf(EmergencyContactRepository.isVibrateEnabled(context)) }
    var flashlightEnabled by remember { mutableStateOf(EmergencyContactRepository.isFlashlightEnabled(context)) }
    var volumePercent by remember { mutableStateOf(EmergencyContactRepository.getVolumePercent(context).coerceAtLeast(10)) }
    var alarmSoundType by remember { mutableStateOf(EmergencyContactRepository.getAlarmSoundType(context)) }
    
    // Track ringer playing state
    var isRingerPlaying by remember { mutableStateOf(false) }
    
    LaunchedEffect(Unit) {
        while (true) {
            kotlinx.coroutines.delay(300)
            isRingerPlaying = EmergencyContactRepository.isRingerPlaying
        }
    }
    
    // Get ringtone name - reactive to changes
    var ringtoneRefresh by remember { mutableStateOf(0) }
    val ringtoneUri = remember(ringtoneRefresh) { EmergencyContactRepository.getRingtoneUri(context) }
    val ringtoneName = remember(ringtoneUri) {
        if (ringtoneUri != null) {
            try {
                val ringtone = android.media.RingtoneManager.getRingtone(context, android.net.Uri.parse(ringtoneUri))
                val title = ringtone?.getTitle(context)
                // Filter out raw numeric/URI titles that aren't human-readable
                if (title != null && !title.all { it.isDigit() } && title.length > 1) title
                else "Pick your favorite alarm sound"
            } catch (e: Exception) {
                "Pick your favorite alarm sound"
            }
        } else {
            "Pick your favorite alarm sound"
        }
    }
    
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        ringtoneRefresh++
    }

    // Gradient background
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color.White,
                        Color(0xFFF3F0FF),
                        Color(0xFFE9DFFF),
                        Color(0xFFDDD0FF)
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, "Back", tint = vibrantPurple)
                }
                Text(
                    "Settings",
                    style = MaterialTheme.typography.headlineLarge.copy(
                        fontWeight = FontWeight.ExtraBold,
                        brush = Brush.linearGradient(
                            colors = listOf(
                                Color(0xFF7C3AED),
                                Color(0xFFD946EF)
                            )
                        )
                    )
                )
            }

            Spacer(Modifier.height(24.dp))

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // ═══════════════════════════════════════
                // ALARM BEHAVIOR
                // ═══════════════════════════════════════
                SettingsSectionCard(title = "Alarm Behavior", vibrantPurple = vibrantPurple) {
                    // Auto-stop timer with icon
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_clock),
                            contentDescription = null,
                            tint = vibrantPurple,
                            modifier = Modifier.size(24.dp)
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "Auto-Stop Timer",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = Color(0xFF1A1A1A)
                            )
                            Text(
                                "Prevent the phone from ringing forever if you don't answer.",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF666666)
                            )
                        }
                    }
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(
                            "30s" to 30_000L,
                            "1 min" to 60_000L,
                            "5 min" to 300_000L
                        ).forEach { (label, duration) ->
                            FilterChip(
                                selected = autoStopDuration == duration,
                                onClick = {
                                    autoStopDuration = duration
                                    EmergencyContactRepository.setAutoStopDuration(context, duration)
                                },
                                label = { Text(label) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = vibrantPurple,
                                    selectedLabelColor = Color.White
                                )
                            )
                        }
                    }

                    Spacer(Modifier.height(12.dp))

                    // Vibrate toggle
                    SettingToggleRow(
                        title = "Vibrate on Ring",
                        description = "Vibrate phone when alarm rings",
                        checked = vibrateEnabled,
                        onCheckedChange = {
                            vibrateEnabled = it
                            EmergencyContactRepository.setVibrateEnabled(context, it)
                        },
                        vibrantPurple = vibrantPurple
                    )

                    Spacer(Modifier.height(8.dp))

                    // Flashlight toggle
                    SettingToggleRow(
                        title = "Flashlight Strobe",
                        description = "Blink the camera light to get your attention in the dark.",
                        checked = flashlightEnabled,
                        onCheckedChange = {
                            flashlightEnabled = it
                            EmergencyContactRepository.setFlashlightEnabled(context, it)
                        },
                        vibrantPurple = vibrantPurple
                    )
                }

                // ═══════════════════════════════════════
                // AUDIO CUSTOMIZATION
                // ═══════════════════════════════════════
                SettingsSectionCard(title = "Audio Customization", vibrantPurple = vibrantPurple) {
                    
                    // Ringtone Source Toggle
                    var ringtoneSource by remember { mutableStateOf(EmergencyContactRepository.getRingtoneSource(context)) }
                    val isPhoneRingtone = ringtoneSource == EmergencyContactRepository.RINGTONE_SOURCE_PHONE
                    
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Header
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Surface(
                                modifier = Modifier.size(40.dp),
                                shape = CircleShape,
                                color = vibrantPurple.copy(alpha = 0.1f)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        if (isPhoneRingtone) Icons.Default.PhoneAndroid else Icons.Default.MusicNote,
                                        contentDescription = null,
                                        tint = vibrantPurple,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                            Column {
                                Text(
                                    "Ringtone Source",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                                    color = Color(0xFF1A1A1A)
                                )
                                Text(
                                    if (isPhoneRingtone) "Using system default ringtone" else "Using custom selected sound",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFF666666)
                                )
                            }
                        }

                        // Premium Selection Cards
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Phone Option
                            Surface(
                                onClick = {
                                    ringtoneSource = EmergencyContactRepository.RINGTONE_SOURCE_PHONE
                                    EmergencyContactRepository.setRingtoneSource(context, EmergencyContactRepository.RINGTONE_SOURCE_PHONE)
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp),
                                color = if (isPhoneRingtone) vibrantPurple.copy(alpha = 0.1f) else Color(0xFFFAFAFA),
                                border = BorderStroke(
                                    if (isPhoneRingtone) 1.5.dp else 1.dp,
                                    if (isPhoneRingtone) vibrantPurple else Color(0xFFEEEEEE)
                                )
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        Icons.Default.PhoneAndroid,
                                        contentDescription = null,
                                        tint = if (isPhoneRingtone) vibrantPurple else Color.Gray,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Text(
                                        "Phone Ringtone",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = if (isPhoneRingtone) vibrantPurple else Color.Gray,
                                        fontWeight = FontWeight.Bold,
                                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                    )
                                }
                            }

                            // Custom Option
                            Surface(
                                onClick = {
                                    ringtoneSource = EmergencyContactRepository.RINGTONE_SOURCE_CUSTOM
                                    EmergencyContactRepository.setRingtoneSource(context, EmergencyContactRepository.RINGTONE_SOURCE_CUSTOM)
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp),
                                color = if (!isPhoneRingtone) vibrantPurple.copy(alpha = 0.1f) else Color(0xFFFAFAFA),
                                border = BorderStroke(
                                    if (!isPhoneRingtone) 1.5.dp else 1.dp,
                                    if (!isPhoneRingtone) vibrantPurple else Color(0xFFEEEEEE)
                                )
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        Icons.Default.MusicNote,
                                        contentDescription = null,
                                        tint = if (!isPhoneRingtone) vibrantPurple else Color.Gray,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Text(
                                        "Custom Sound",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = if (!isPhoneRingtone) vibrantPurple else Color.Gray,
                                        fontWeight = FontWeight.Bold,
                                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                    )
                                }
                            }
                        }

                        // Custom Controls (Change / Preview) - Shown in a row below
                        if (!isPhoneRingtone) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = Color(0xFFF8F5FF),
                                border = BorderStroke(1.dp, vibrantPurple.copy(alpha = 0.3f))
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text(
                                        "Selected Sound",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = vibrantPurple
                                    )
                                    Text(
                                        ringtoneName,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Medium,
                                        color = Color.Black
                                    )
                                    Spacer(Modifier.height(12.dp))
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        OutlinedButton(
                                            onClick = onSelectRingtone,
                                            modifier = Modifier.weight(1f),
                                            shape = RoundedCornerShape(8.dp),
                                            border = BorderStroke(1.dp, vibrantPurple),
                                            colors = ButtonDefaults.outlinedButtonColors(contentColor = vibrantPurple)
                                        ) {
                                            Icon(Icons.Default.LibraryMusic, null, Modifier.size(16.dp))
                                            Spacer(Modifier.width(8.dp))
                                            Text("Change")
                                        }
                                        Button(
                                            onClick = if (isRingerPlaying) onStopRinger else onTestRinger,
                                            modifier = Modifier.weight(1f),
                                            shape = RoundedCornerShape(8.dp),
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = if (isRingerPlaying) Color.Red else vibrantPurple
                                            )
                                        ) {
                                            Icon(if (isRingerPlaying) Icons.Default.Stop else Icons.Default.PlayArrow, null, Modifier.size(16.dp))
                                            Spacer(Modifier.width(8.dp))
                                            Text(if (isRingerPlaying) "Stop" else "Preview")
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(20.dp))
                    Divider(color = Color(0xFFEEEEEE), thickness = 1.dp)
                    Spacer(Modifier.height(20.dp))
                    
                    // 2. Modern Volume Control with Visual Indicators
                    Column(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Animated volume icon based on level
                            Surface(
                                modifier = Modifier.size(40.dp),
                                shape = CircleShape,
                                color = vibrantPurple.copy(alpha = 0.1f)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        when {
                                            volumePercent >= 70 -> Icons.Default.VolumeUp
                                            volumePercent >= 30 -> Icons.Default.VolumeDown
                                            else -> Icons.Default.VolumeMute
                                        },
                                        contentDescription = null,
                                        tint = vibrantPurple,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                            
                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        "Alarm Volume",
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                                        color = Color(0xFF1A1A1A)
                                    )
                                    
                                    // Modern percentage badge with gradient
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = vibrantPurple
                                    ) {
                                        Text(
                                            "$volumePercent%",
                                            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                                            color = Color.White,
                                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(Modifier.height(12.dp))

                        // Enhanced Slider with visual markers
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 52.dp, end = 16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Low volume icon
                            Icon(
                                Icons.Default.VolumeMute,
                                contentDescription = "Low",
                                tint = if (volumePercent <= 30) vibrantPurple else Color(0xFFCCCCCC),
                                modifier = Modifier.size(18.dp)
                            )
                            
                            // Modern slider with custom track
                            Slider(
                                value = volumePercent.toFloat(),
                                onValueChange = { 
                                    volumePercent = it.toInt().coerceAtLeast(10)
                                },
                                onValueChangeFinished = {
                                    EmergencyContactRepository.setVolumePercent(context, volumePercent)
                                },
                                valueRange = 10f..100f,
                                modifier = Modifier.weight(1f),
                                colors = SliderDefaults.colors(
                                    thumbColor = Color.White,
                                    activeTrackColor = vibrantPurple,
                                    inactiveTrackColor = vibrantPurple.copy(alpha = 0.15f)
                                )
                            )
                            
                            // High volume icon
                            Icon(
                                Icons.Default.VolumeUp,
                                contentDescription = "High",
                                tint = if (volumePercent >= 70) vibrantPurple else Color(0xFFCCCCCC),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        
                        // Volume level indicator text
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 70.dp, end = 32.dp, top = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                "10%",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF999999),
                                fontSize = 10.sp
                            )
                            Text(
                                when {
                                    volumePercent >= 80 -> "Very Loud"
                                    volumePercent >= 60 -> "Loud"
                                    volumePercent >= 40 -> "Medium"
                                    volumePercent >= 20 -> "Low"
                                    else -> "Quiet"
                                },
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                                color = vibrantPurple,
                                fontSize = 11.sp
                            )
                            Text(
                                "100%",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF999999),
                                fontSize = 10.sp
                            )
                        }
                    }

                    Spacer(Modifier.height(16.dp))
                    Divider(color = Color(0xFFEEEEEE), thickness = 1.dp)
                    Spacer(Modifier.height(16.dp))

                    // 3. Alarm Sound Type with Icons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Surface(
                            modifier = Modifier.size(40.dp),
                            shape = CircleShape,
                            color = vibrantPurple.copy(alpha = 0.1f)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Default.Notifications,
                                    contentDescription = null,
                                    tint = vibrantPurple,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Column {
                            Text(
                                "Alarm Sound Type",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = Color(0xFF1A1A1A)
                            )
                            Text(
                                "Choose the alarm tone style",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF666666)
                            )
                        }
                    }
                    
                    Spacer(Modifier.height(8.dp))
                    
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 52.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(
                            "Ringtone" to EmergencyContactRepository.SOUND_TYPE_RINGTONE,
                            "Siren" to EmergencyContactRepository.SOUND_TYPE_SIREN,
                            "Beep" to EmergencyContactRepository.SOUND_TYPE_BEEP
                        ).forEach { (label, type) ->
                            FilterChip(
                                selected = alarmSoundType == type,
                                onClick = {
                                    // Save the selection
                                    alarmSoundType = type
                                    EmergencyContactRepository.setAlarmSoundType(context, type)
                                    
                                    // Preview the sound for 5 seconds
                                    RingerManager.triggerEmergencyRinger(
                                        context,
                                        durationMs = 5000,
                                        tempSoundType = type
                                    )
                                },
                                label = { Text(label, fontWeight = FontWeight.Medium) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = vibrantPurple,
                                    selectedLabelColor = Color.White,
                                    containerColor = Color(0xFFF5F5F5)
                                ),
                                shape = RoundedCornerShape(10.dp)
                            )
                        }
                    }
                }

                Spacer(Modifier.height(20.dp))
            }
        }
    }
}

@Composable
fun SettingsSectionCard(
    title: String,
    vibrantPurple: Color,
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = Color.White,
        shadowElevation = 2.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                title,
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = vibrantPurple
            )
            content()
        }
    }
}

@Composable
fun SettingToggleRow(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    vibrantPurple: Color
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                title,
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                color = Color(0xFF1A1A1A)
            )
            Text(
                description,
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF666666)
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = vibrantPurple,
                uncheckedThumbColor = Color.White,
                uncheckedTrackColor = Color(0xFFCCCCCC)
            )
        )
    }
}
