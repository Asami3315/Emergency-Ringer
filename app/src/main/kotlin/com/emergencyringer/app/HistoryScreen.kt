package com.emergencyringer.app

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context as AndroidContext
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
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
import androidx.compose.foundation.border
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// ── Color tokens ─────────────────────────────────────────────
private val WBg       = Color(0xFFF2F0EB)
private val WText     = Color(0xFF3E3A36)
private val WMuted    = Color(0xFF8C867D)
private val WAccent   = Color(0xFFD4A373)
private val WLine     = Color(0xFFE5E2DC)
private val WFloat    = Color(0xCCFFFFFF)

private val BadgeCBg  = Color(0x80E9D8FD)
private val BadgeCTxt = Color(0xFF6B46C1)
private val BadgeRBg  = Color(0x80FED7D7)
private val BadgeRTxt = Color(0xFFC53030)
private val BadgeKBg  = Color(0xFFF3F4F6)
private val BadgeKTxt = Color(0xFF6B7280)
private val BadgeMBg  = Color(0x80D1FAE5)
private val BadgeMTxt = Color(0xFF065F46)

@Composable
fun HistoryScreen() {
    val context = LocalContext.current
    var history by remember { mutableStateOf(EmergencyContactRepository.getTriggerHistory(context)) }
    var showClearDialog by remember { mutableStateOf(false) }
    var showDebugLog by remember { mutableStateOf(false) }
    val logMessages by AppLog.messages.collectAsState()

    // Refresh AppLog from file (in case service wrote logs while app was closed)
    LaunchedEffect(Unit) {
        AppLog.init(context)
        AppLog.refreshFromFile()
    }

    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = { Text("Clear History", fontWeight = FontWeight.Bold) },
            text = { Text("Remove all trigger records? This cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        EmergencyContactRepository.clearTriggerHistory(context)
                        history = emptyList()
                        showClearDialog = false
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = BadgeRTxt)
                ) { Text("Clear", fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) { Text("Cancel") }
            },
            shape = RoundedCornerShape(20.dp)
        )
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

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {

            // ── Header ────────────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(top = 52.dp, bottom = 16.dp)
            ) {
                Text(
                    "History Log",
                    modifier = Modifier.align(Alignment.Center),
                    fontSize = 18.sp, fontWeight = FontWeight.SemiBold,
                    color = WText, letterSpacing = (-0.3).sp
                )
            }

            if (history.isEmpty()) {
                // Empty state
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(320.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(80.dp)
                                .background(WAccent.copy(alpha = 0.15f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.History, null, tint = WAccent, modifier = Modifier.size(40.dp))
                        }
                        Text("No triggers yet", fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = WText)
                        Text(
                            "When an emergency alarm fires,\nit will appear here.",
                            fontSize = 14.sp, color = WMuted,
                            textAlign = TextAlign.Center, lineHeight = 20.sp
                        )
                    }
                }
            } else {
                // Scrollable list
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 20.dp, end = 16.dp)
                ) {
                    val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                    val today = sdf.format(Date())
                    val yesterday = sdf.format(Date(System.currentTimeMillis() - 86_400_000L))

                    val grouped = history
                        .groupBy { sdf.format(Date(it.timestampMs)) }
                        .entries
                        .sortedByDescending { it.key }

                    grouped.forEachIndexed { groupIdx, (dateKey, records) ->
                        val dateLabel = when (dateKey) {
                            today     -> "Today"
                            yesterday -> "Yesterday"
                            else      -> SimpleDateFormat("d MMM yyyy", Locale.getDefault())
                                            .format(sdf.parse(dateKey) ?: Date())
                        }

                        // ── Date separator ─────────────────────────
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(IntrinsicSize.Min),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(
                                modifier = Modifier
                                    .width(38.dp)
                                    .fillMaxHeight(),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                val topSpace = if (groupIdx == 0) 4.dp else 20.dp
                                if (groupIdx > 0) {
                                    Box(
                                        modifier = Modifier
                                            .height(topSpace)
                                            .width(2.dp)
                                            .background(WLine)
                                    )
                                } else {
                                    Spacer(Modifier.height(topSpace))
                                }

                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .background(
                                            if (groupIdx == 0) WAccent else WLine,
                                            CircleShape
                                        )
                                )

                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .width(2.dp)
                                        .background(WLine)
                                )
                            }
                            Spacer(Modifier.width(12.dp))
                            Text(
                                dateLabel.uppercase(Locale.getDefault()),
                                modifier = Modifier.padding(top = if (groupIdx == 0) 4.dp else 22.dp, bottom = 14.dp),
                                fontSize = 11.sp, fontWeight = FontWeight.Bold,
                                letterSpacing = 1.5.sp, color = WMuted
                            )
                        }

                        // ── Event rows ─────────────────────────────
                        records.forEachIndexed { recIdx, record ->
                            val isLast = groupIdx == grouped.lastIndex &&
                                         recIdx == records.lastIndex
                            // KEY FIX: IntrinsicSize.Min makes the Row height = card height,
                            // so the icon column can use fillMaxHeight() to draw the line perfectly
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(IntrinsicSize.Min),   // <-- makes line fill card height
                                horizontalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                // ── Icon + connecting line column ───
                                 Column(
                                     modifier = Modifier
                                         .width(38.dp)
                                         .fillMaxHeight(),
                                     horizontalAlignment = Alignment.CenterHorizontally
                                 ) {
                                     val style = resolveStyle(record.reason)
                                     // Top bridge line: connects from bottom of previous card's gap
                                     // down to this icon (only for non-first items)
                                     if (recIdx > 0 || groupIdx > 0) {
                                         Box(
                                             modifier = Modifier
                                                 .height(20.dp)   // matches card bottom padding
                                                 .width(2.dp)
                                                 .background(
                                                     Brush.verticalGradient(
                                                         listOf(WLine, WAccent.copy(0.4f))
                                                     )
                                                 )
                                         )
                                     } else {
                                         Spacer(Modifier.height(8.dp))
                                     }
                                     // White ring + icon circle
                                     Box(
                                         modifier = Modifier
                                             .size(38.dp)
                                             .background(Color(0xFFF7F4EF), CircleShape),
                                         contentAlignment = Alignment.Center
                                     ) {
                                         Box(
                                             modifier = Modifier
                                                 .size(32.dp)
                                                 .background(style.iconBg, CircleShape),
                                             contentAlignment = Alignment.Center
                                         ) {
                                             Icon(style.icon, null, tint = style.iconTint,
                                                 modifier = Modifier.size(16.dp))
                                         }
                                     }
                                     // Main connecting line below icon
                                     if (!isLast) {
                                         Box(
                                             modifier = Modifier
                                                 .weight(1f)
                                                 .width(2.dp)
                                                 .background(
                                                     Brush.verticalGradient(
                                                         listOf(WAccent.copy(0.5f), WLine)
                                                     )
                                                 )
                                         )
                                     }
                                 }

                                // ── Card ────────────────────────────
                                val style = resolveStyle(record.reason)
                                val timeStr = SimpleDateFormat("hh:mm a", Locale.getDefault())
                                    .format(Date(record.timestampMs))

                                 Card(
                                     modifier = Modifier
                                         .weight(1f)
                                         .weightedSpring()
                                         .padding(bottom = 20.dp),
                                     shape = RoundedCornerShape(20.dp),
                                     colors = CardDefaults.cardColors(containerColor = Color(0xA6FFFFFF)),
                                     elevation = CardDefaults.cardElevation(0.dp)
                                 ) {
                                      Column(modifier = Modifier.padding(horizontal = 18.dp).padding(top = 14.dp, bottom = 14.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                record.callerName, fontSize = 17.sp,
                                                fontWeight = FontWeight.SemiBold, color = WText,
                                                modifier = Modifier.weight(1f)
                                            )
                                            Text(timeStr, fontSize = 11.sp, color = WMuted,
                                                fontWeight = FontWeight.Medium)
                                        }
                                        Spacer(Modifier.height(8.dp))
                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .background(style.badgeBg, RoundedCornerShape(50.dp))
                                                    .padding(horizontal = 10.dp, vertical = 3.dp)
                                            ) {
                                                Text(
                                                    style.badgeLabel, fontSize = 10.sp,
                                                    fontWeight = FontWeight.ExtraBold,
                                                    letterSpacing = 0.8.sp, color = style.badgeTxt
                                                )
                                            }
                                            Text(
                                                record.reason, fontSize = 12.sp, color = WMuted,
                                                maxLines = 1,
                                                fontStyle = if (style.italic) FontStyle.Italic
                                                            else FontStyle.Normal
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // ── Clear History at bottom of scroll ──────────
                    Spacer(Modifier.height(8.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .magneticAffinity(strength = 0.2f)
                            .shadow(12.dp, RoundedCornerShape(28.dp), spotColor = Color.Black.copy(alpha = 0.15f))
                            .border(1.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(28.dp))
                            .background(Color.White.copy(alpha = 0.85f), RoundedCornerShape(28.dp))
                            .clip(RoundedCornerShape(28.dp))
                            .clickable { showClearDialog = true },
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Clear History", fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold, color = BadgeRTxt)
                    }
                }
            }

            // ── Debug Log Section ──────────────────────────────────────────
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 96.dp, top = 8.dp)
            ) {
                // Toggle header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF1A1A1A))
                        .clickable { showDebugLog = !showDebugLog
                            if (showDebugLog) AppLog.refreshFromFile() }
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.BugReport, null,
                            tint = Color(0xFFFFB703), modifier = Modifier.size(18.dp))
                        Text("Debug Log", color = Color.White,
                            fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                        if (logMessages.isNotEmpty()) {
                            Text("(${logMessages.size})", color = Color(0xFFFFB703), fontSize = 12.sp)
                        }
                    }
                    Icon(
                        if (showDebugLog) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        null, tint = Color.White.copy(0.6f), modifier = Modifier.size(20.dp)
                    )
                }

                if (showDebugLog) {
                    Spacer(Modifier.height(4.dp))
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 300.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF111111))
                            .verticalScroll(rememberScrollState())
                            .padding(12.dp)
                    ) {
                        if (logMessages.isEmpty()) {
                            Text("No logs yet. Trigger a call or toggle notification access.",
                                color = Color(0xFF888888), fontSize = 11.sp)
                        } else {
                            logMessages.reversed().forEach { msg ->
                                Text(msg, color = when {
                                    msg.contains("🚨") || msg.contains("EMERGENCY") -> Color(0xFFFF6B6B)
                                    msg.contains("✅") -> Color(0xFF6BFF9E)
                                    msg.contains("⚠️") || msg.contains("❌") -> Color(0xFFFFD166)
                                    msg.contains("═══") -> Color(0xFFFFB703)
                                    msg.contains("🔍") -> Color(0xFF74C0FC)
                                    else -> Color(0xFFCCCCCC)
                                }, fontSize = 10.sp, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                lineHeight = 16.sp)
                                Spacer(Modifier.height(1.dp))
                            }
                        }
                    }
                    Spacer(Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // ── Copy All button ──
                        OutlinedButton(
                            onClick = {
                                val all = logMessages.joinToString("\n")
                                val clipboard = context.getSystemService(AndroidContext.CLIPBOARD_SERVICE) as ClipboardManager
                                clipboard.setPrimaryClip(ClipData.newPlainText("Emergency Ringer Log", all))
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(50.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF74C0FC))
                        ) {
                            Icon(Icons.Default.ContentCopy, null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Copy All", fontSize = 13.sp)
                        }
                        // ── Clear button ──
                        OutlinedButton(
                            onClick = { AppLog.clear() },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(50.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFFF6B6B))
                        ) {
                            Icon(Icons.Default.DeleteForever, null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Clear Log", fontSize = 13.sp)
                        }
                    }
                }
            }
        }
    }
}
// ── Style per event type ──────────────────────────────────────
private data class RowStyle(
    val iconBg: Color, val iconTint: Color, val icon: ImageVector,
    val badgeBg: Color, val badgeTxt: Color, val badgeLabel: String,
    val italic: Boolean = false
)

private fun resolveStyle(reason: String): RowStyle = when {
    reason.contains("Emergency Contact", ignoreCase = true) ->
        RowStyle(WAccent, Color.White, Icons.Default.Favorite, BadgeCBg, BadgeCTxt, "CONTACT")
    reason.contains("Repeated", ignoreCase = true) ->
        RowStyle(Color(0xFFFEF2F2), Color(0xFFF9A8A8), Icons.Default.CallMissed,
            BadgeRBg, BadgeRTxt, "REPEATED")
    reason.contains("Message", ignoreCase = true) ->
        RowStyle(Color(0xFFF0FDF4), Color(0xFF6EE7B7), Icons.Default.Message,
            BadgeMBg, BadgeMTxt, "MESSAGE")
    else ->
        RowStyle(Color.White, Color(0xFF94A3B8), Icons.Default.MedicalServices,
            BadgeKBg, BadgeKTxt, "KEYWORD", italic = true)
}
