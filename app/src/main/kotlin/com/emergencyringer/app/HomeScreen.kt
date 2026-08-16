package com.emergencyringer.app

import androidx.compose.animation.core.*
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
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.combinedClickable
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.draw.blur
import com.emergencyringer.app.magneticAffinity
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.foundation.Image
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp


// ── Design tokens matching Stitch HTML ──────────────────────
val NeoYellow       = Color(0xFFFFB703)
val NeoYellowDark   = Color(0xFFE6A200)
val NeoBackground   = Color(0xFFFAFAFA)
val NeoCard         = Color(0xFFFFFFFF)
val NeoText         = Color(0xFF1A1A1A)
val NeoMuted        = Color(0xFF888888)
val NeoBorder       = Color(0xFFF2F0EA)
val AccentPeach     = Color(0xFFFFF0EA)
val AccentLavender  = Color(0xFFF2F2FF)
val AccentSage      = Color(0xFFEEF7ED)

@Composable
fun HomeScreen(
    hasNotificationAccess: Boolean,
    hasDndAccess: Boolean,
    hasContactsPermission: Boolean,
    isBatteryOptDisabled: Boolean,
    contacts: List<EmergencyContactRepository.Contact>,
    monitoringEnabled: Boolean,
    onMonitoringToggle: (Boolean) -> Unit,
    onAddContact: () -> Unit,
    onRemoveContact: (String, String) -> Unit,
    isPremium: Boolean = false,
) {
    val allPermsReady = hasNotificationAccess && hasDndAccess && hasContactsPermission && isBatteryOptDisabled

    Box(
        modifier = Modifier
            .fillMaxSize()
    ) {
        // ── Background image ──
        Image(
            painter = painterResource(id = R.drawable.home_bg),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )
        // Subtle 10% overlay
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
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 100.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // ── Header (scrolls with content) ─────────────
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 48.dp, bottom = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            "KinLink",
                            fontSize = 32.sp,
                            fontWeight = FontWeight.ExtraLight,
                            color = NeoText
                        )
                        Text(
                            "Stay connected with your loved ones",
                            fontSize = 12.sp,
                            color = NeoText.copy(alpha = 0.7f),
                            fontWeight = FontWeight.Medium,
                            letterSpacing = 0.5.sp
                        )
                    }
                }

                // ── Hero Status Card ──────────────────────────
                HeroStatusCard(
                    isActive = allPermsReady,
                    monitoringEnabled = monitoringEnabled,
                    onToggle = onMonitoringToggle
                )

                // ── Quick Settings Tip ────────────────────────
                val context = LocalContext.current
                var isTileAdded by remember { mutableStateOf(EmergencyContactRepository.isTileAdded(context)) }
                
                // Continuous background check & resume check
                LaunchedEffect(Unit) {
                    while (true) {
                        isTileAdded = EmergencyContactRepository.isTileAdded(context)
                        kotlinx.coroutines.delay(800)
                    }
                }
                val lifecycleOwner = androidx.compose.ui.platform.LocalLifecycleOwner.current
                DisposableEffect(lifecycleOwner) {
                    val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
                        if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                            isTileAdded = EmergencyContactRepository.isTileAdded(context)
                        }
                    }
                    lifecycleOwner.lifecycle.addObserver(observer)
                    onDispose {
                        lifecycleOwner.lifecycle.removeObserver(observer)
                    }
                }

                if (!isTileAdded) {
                    var showTileInfo by remember { mutableStateOf(false) }
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .shadow(4.dp, RoundedCornerShape(16.dp), spotColor = Color.Black.copy(0.04f))
                            .background(Color.White, RoundedCornerShape(16.dp))
                            .clickable { showTileInfo = true }
                            .padding(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .background(NeoYellow.copy(alpha = 0.15f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(painter = painterResource(id = R.drawable.ic_clock), contentDescription = null, tint = NeoYellowDark, modifier = Modifier.size(22.dp))
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Quick Settings Toggle", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = NeoText)
                                Text("Turn on/off instantly without opening the app", fontSize = 12.sp, color = NeoMuted)
                            }
                            IconButton(
                                onClick = {
                                    isTileAdded = true
                                    EmergencyContactRepository.setTileAdded(context, true)
                                },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(Icons.Default.Close, contentDescription = "Dismiss", tint = NeoMuted.copy(alpha = 0.6f), modifier = Modifier.size(16.dp))
                            }
                        }
                    }

                    if (showTileInfo) {
                        AlertDialog(
                            onDismissRequest = { showTileInfo = false },
                            title = { Text("Quick Settings Tile", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = NeoText) },
                            text = {
                                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                    Text("You can add Alert App to your Android drop-down menu for instant access!", fontSize = 14.sp, color = NeoText)
                                    Text("1. Swipe down twice from the top of your screen.", fontSize = 14.sp, color = NeoMuted)
                                    Text("2. Tap the 'Edit' (pencil) icon.", fontSize = 14.sp, color = NeoMuted)
                                    Text("3. Find 'Alert App' and drag it up to your active tiles.", fontSize = 14.sp, color = NeoMuted)
                                }
                            },
                            confirmButton = {
                                TextButton(onClick = {
                                    showTileInfo = false
                                    isTileAdded = true
                                    EmergencyContactRepository.setTileAdded(context, true)
                                }) {
                                    Text("Got it!", fontWeight = FontWeight.Bold, color = NeoYellowDark)
                                }
                            },
                            dismissButton = {
                                TextButton(onClick = { showTileInfo = false }) {
                                    Text("Close", color = NeoMuted)
                                }
                            },
                            containerColor = NeoCard,
                            shape = RoundedCornerShape(24.dp)
                        )
                    }
                }

                // ── Priority Contacts ─────────────────────────
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "My Inner Circle",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = NeoText,
                        letterSpacing = (-0.5).sp
                    )
                }

                // 2-column grid
                val accentColors = listOf(AccentPeach, AccentLavender, AccentSage)
                val iconTints = listOf(Color(0xFFE53E3E), Color(0xFF5A67D8), Color(0xFF38A169))

                if (contacts.isEmpty()) {
                    EmptyContactsCard(onAddContact = onAddContact)
                } else {
                    val rows = (contacts.size + 1 + 1) / 2  // +1 for the add button
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        val items: List<EmergencyContactRepository.Contact?> = contacts + listOf(null)
                        items.chunked(2).forEach { row ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                row.forEachIndexed { idx, contact ->
                                    val globalIdx = items.indexOf(contact)
                                    Box(modifier = Modifier.weight(1f)) {
                                        if (contact == null) {
                                            AddContactCard(
                                                onAddContact = onAddContact,
                                                isLocked = !isPremium && contacts.size >= 4
                                            )
                                        } else {
                                            ContactCard(
                                                contact = contact,
                                                accentColor = accentColors[globalIdx % accentColors.size],
                                                iconTint = iconTints[globalIdx % iconTints.size],
                                                onRemove = { onRemoveContact(contact.name, contact.number) }
                                            )
                                        }
                                    }
                                }
                                // Fill last row if odd count
                                if (row.size == 1) { Spacer(Modifier.weight(1f)) }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HeroStatusCard(
    isActive: Boolean,
    monitoringEnabled: Boolean,
    onToggle: (Boolean) -> Unit
) {
    val pulse = rememberInfiniteTransition(label = "pulse")
    val pulseAlpha by pulse.animateFloat(
        initialValue = 0.5f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1000), RepeatMode.Reverse),
        label = "alpha"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = 32.dp,
                shape = RoundedCornerShape(32.dp),
                ambientColor = Color.Black.copy(alpha = 0.1f),
                spotColor = Color.Black.copy(alpha = 0.15f)
            ),
        shape = RoundedCornerShape(32.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Color(0xFFFAF9F6).copy(alpha = 0.85f),
                    RoundedCornerShape(32.dp)
                )
                .border(1.dp, Color.White, RoundedCornerShape(32.dp))
                .padding(24.dp)
        ) {
            // Status UI logic
            val statusColor = when {
                !isActive -> Color(0xFFE53E3E) // Red
                monitoringEnabled -> NeoYellow
                else -> Color.Gray
            }
            val statusBg = when {
                !isActive -> Color(0xFFE53E3E)
                monitoringEnabled -> NeoYellow
                else -> Color.Gray.copy(alpha = 0.3f)
            }

            // Status dot at top-left
            Box(modifier = Modifier.size(10.dp).align(Alignment.TopStart)) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            alpha = if (isActive && monitoringEnabled) pulseAlpha else 1f
                        }
                        .background(statusBg, CircleShape)
                )
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .align(Alignment.Center)
                        .background(statusColor, CircleShape)
                )
            }

            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(Modifier.height(8.dp))
                
                val titleText = when {
                    !isActive -> "Action Required"
                    monitoringEnabled -> "Active"
                    else -> "Paused"
                }
                val titleColor = when {
                    !isActive -> Color(0xFFE53E3E)
                    monitoringEnabled -> NeoText
                    else -> Color.Black.copy(alpha = 0.38f)
                }

                Text(
                    titleText,
                    fontSize = 32.sp,
                    fontWeight = if (isActive && monitoringEnabled) FontWeight.Bold else FontWeight.Thin,
                    color = titleColor,
                    lineHeight = 34.sp
                )
                
                if (!isActive) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Please grant permissions in Settings",
                        fontSize = 12.sp,
                        color = Color(0xFFE53E3E).copy(alpha = 0.8f),
                        fontWeight = FontWeight.Medium
                    )
                }
                Spacer(Modifier.height(16.dp))

                // Master toggle row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.White.copy(alpha = 0.6f))
                        .border(1.dp, Color.White.copy(alpha = 0.7f), RoundedCornerShape(16.dp))
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        if (monitoringEnabled) "Monitoring Alerts" else "Monitoring Off",
                        fontSize = 11.sp,
                        fontWeight = if (monitoringEnabled) FontWeight.SemiBold else FontWeight.Thin,
                        letterSpacing = 1.sp,
                        color = if (monitoringEnabled) NeoText else Color.Black.copy(alpha = 0.38f)
                    )
                    Switch(
                        checked = monitoringEnabled,
                        onCheckedChange = onToggle,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = NeoYellow,
                            uncheckedTrackColor = Color.Gray.copy(alpha = 0.3f)
                        )
                    )
                }
            }
        }
    }
}


@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
private fun ContactCard(
    contact: EmergencyContactRepository.Contact,
    accentColor: Color,
    iconTint: Color,
    onRemove: () -> Unit
) {
    val initial = (contact.name.firstOrNull()?.uppercaseChar() ?: '?').toString()
    var showDeleteConfirm by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(210.dp)
            .magneticAffinity(strength = 0.12f)
            .combinedClickable(
                onLongClick = { showDeleteConfirm = true },
                onClick = { if (showDeleteConfirm) showDeleteConfirm = false }
            ),
        shape = RoundedCornerShape(32.dp),
        colors = CardDefaults.cardColors(containerColor = accentColor),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Top: call icon
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(Color.White.copy(alpha = 0.6f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Call, contentDescription = null, tint = iconTint, modifier = Modifier.size(20.dp))
                }
                Spacer(Modifier.height(16.dp))
                // Bottom: avatar + name
                Column {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .background(Color.White.copy(alpha = 0.8f), RoundedCornerShape(16.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(initial, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = iconTint)
                    }
                    Spacer(Modifier.height(10.dp))
                    Text(
                        contact.name,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = NeoText,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (contact.number.isNotBlank()) {
                        Text(
                            contact.number,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = iconTint.copy(alpha = 0.7f),
                            letterSpacing = 0.5.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            // Overlay for Delete
            androidx.compose.animation.AnimatedVisibility(
                visible = showDeleteConfirm,
                enter = androidx.compose.animation.fadeIn(),
                exit = androidx.compose.animation.fadeOut()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.7f)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .background(Color(0xFFE53E3E), CircleShape)
                                .clickable {
                                    showDeleteConfirm = false
                                    onRemove()
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.White)
                        }
                        Spacer(Modifier.height(12.dp))
                        Text("Tap to remove", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddContactCard(onAddContact: () -> Unit, isLocked: Boolean = false) {
    val borderColor = Color(0xFFD6CBC2)
    val textColor = Color(0xFF6B5E55)
    
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(210.dp)
            .magneticAffinity(strength = 0.15f)
            .drawBehind {
                val strokeWidth = 2.dp.toPx()
                val dashWidth = 8.dp.toPx()
                val dashGap = 6.dp.toPx()
                val cornerRadius = 32.dp.toPx()
                
                drawRoundRect(
                    color = borderColor,
                    style = Stroke(
                        width = strokeWidth,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(dashWidth, dashGap), 0f)
                    ),
                    cornerRadius = CornerRadius(cornerRadius)
                )
            }
            .clip(RoundedCornerShape(32.dp))
            .clickable { onAddContact() },
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.then(if (isLocked) Modifier.blur(2.5.dp) else Modifier)
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .background(Color(0xFFF5F2EF), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Add, 
                    contentDescription = null, 
                    tint = textColor.copy(alpha = 0.7f), 
                    modifier = Modifier.size(32.dp)
                )
            }
            Spacer(Modifier.height(16.dp))
            Text("Add Person", color = textColor, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }

        if (isLocked) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier
                    .matchParentSize()
                    .background(Color.White.copy(alpha = 0.35f))
            ) {
                Icon(
                    Icons.Default.Lock,
                    contentDescription = "Premium Feature",
                    tint = NeoYellow,
                    modifier = Modifier.size(42.dp)
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "Upgrade to\npremium",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = NeoText,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EmptyContactsCard(onAddContact: () -> Unit) {
    val borderColor = Color(0xFFD6CBC2)
    val textColor = Color(0xFF6B5E55)
    
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .magneticAffinity(strength = 0.15f)
            .drawBehind {
                val strokeWidth = 2.dp.toPx()
                val dashWidth = 8.dp.toPx()
                val dashGap = 6.dp.toPx()
                val cornerRadius = 28.dp.toPx()
                
                drawRoundRect(
                    color = borderColor,
                    style = Stroke(
                        width = strokeWidth,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(dashWidth, dashGap), 0f)
                    ),
                    cornerRadius = CornerRadius(cornerRadius)
                )
            }
            .clip(RoundedCornerShape(28.dp))
            .clickable { onAddContact() },
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(40.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .background(Color(0xFFF5F2EF), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.PersonAdd, contentDescription = null, tint = textColor, modifier = Modifier.size(34.dp))
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("No emergency contacts yet", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = textColor)
                Text("Tap to add someone who can bypass silent mode", fontSize = 13.sp, color = textColor.copy(alpha = 0.7f),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center, lineHeight = 18.sp)
            }
        }
    }
}
