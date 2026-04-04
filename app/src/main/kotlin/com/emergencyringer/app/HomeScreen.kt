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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import com.emergencyringer.app.magneticAffinity
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
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
    onRequestNotification: () -> Unit,
    onRequestDnd: () -> Unit,
    onRequestBattery: () -> Unit,
) {
    val allPermsReady = hasNotificationAccess && hasDndAccess

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
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 48.dp, bottom = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            "KinLink",
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold,
                            color = NeoText,
                            letterSpacing = (-0.5).sp
                        )
                        Text(
                            if (allPermsReady) "All systems guarded" else "Setup required",
                            fontSize = 13.sp,
                            color = NeoMuted,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                // ── Hero Status Card ──────────────────────────
                HeroStatusCard(
                    isActive = allPermsReady,
                    monitoringEnabled = monitoringEnabled,
                    onToggle = onMonitoringToggle
                )



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
                                modifier = Modifier.height(IntrinsicSize.Max),
                                horizontalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                row.forEachIndexed { idx, contact ->
                                    val globalIdx = items.indexOf(contact)
                                    Box(modifier = Modifier.weight(1f)) {
                                        if (contact == null) {
                                            AddContactCard(onAddContact = onAddContact)
                                        } else {
                                            ContactCard(
                                                contact = contact,
                                                accentColor = accentColors[globalIdx % accentColors.size],
                                                iconTint = iconTints[globalIdx % iconTints.size]
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
                elevation = 20.dp,
                shape = RoundedCornerShape(32.dp),
                ambientColor = Color.Black.copy(alpha = 0.08f),
                spotColor = Color.Black.copy(alpha = 0.05f)
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
            Column {
                // Active pill
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50.dp))
                        .background(Color.White.copy(alpha = 0.7f))
                        .border(1.dp, Color.White.copy(alpha = 0.5f), RoundedCornerShape(50.dp))
                        .padding(horizontal = 12.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(modifier = Modifier.size(8.dp)) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    if (isActive && monitoringEnabled) NeoYellow.copy(alpha = pulseAlpha)
                                    else Color.Gray.copy(alpha = 0.3f),
                                    CircleShape
                                )
                        )
                        Box(
                            modifier = Modifier
                                .size(5.dp)
                                .align(Alignment.Center)
                                .background(
                                    if (isActive && monitoringEnabled) NeoYellow else Color.Gray,
                                    CircleShape
                                )
                        )
                    }
                }
                Spacer(Modifier.height(12.dp))
                Text(
                    if (isActive && monitoringEnabled) "Active" else "Sleeping",
                    fontSize = 30.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = NeoText,
                    lineHeight = 32.sp
                )



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
                        "Live Shield",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 1.sp,
                        color = NeoText
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


@Composable
private fun ContactCard(contact: EmergencyContactRepository.Contact, accentColor: Color, iconTint: Color) {
    val initial = (contact.name.firstOrNull()?.uppercaseChar() ?: '?').toString()
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .defaultMinSize(minHeight = 180.dp)
            .magneticAffinity(strength = 0.12f),
        shape = RoundedCornerShape(32.dp),
        colors = CardDefaults.cardColors(containerColor = accentColor),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
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
                if (!contact.number.isNullOrBlank()) {
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
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddContactCard(onAddContact: () -> Unit) {
    val borderColor = Color(0xFFD6CBC2)
    val textColor = Color(0xFF6B5E55)
    
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight()
            .defaultMinSize(minHeight = 180.dp)
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
            verticalArrangement = Arrangement.Center
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
                    tint = NeoYellow, 
                    modifier = Modifier.size(28.dp)
                )
            }
            Spacer(Modifier.height(20.dp))
            Text(
                "Add New", 
                fontSize = 18.sp, 
                fontWeight = FontWeight.Bold, 
                color = NeoText
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "Invite members to stay connected", 
                fontSize = 12.sp, 
                fontWeight = FontWeight.Medium, 
                color = textColor,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
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
