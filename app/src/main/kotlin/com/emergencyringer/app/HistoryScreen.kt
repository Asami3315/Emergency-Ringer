package com.emergencyringer.app

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context as AndroidContext
import androidx.compose.foundation.clickable
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.ExperimentalFoundationApi
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

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HistoryScreen() {
    val context = LocalContext.current
    var history by remember { mutableStateOf(EmergencyContactRepository.getTriggerHistory(context)) }
    var showClearDialog by remember { mutableStateOf(false) }
    var selectedFilter by remember { mutableStateOf("All") }
    var expandedRecordTs by remember { mutableStateOf<Long?>(null) }

    // Compute filtered history based on selected chip
    val filteredHistory = remember(history, selectedFilter) {
        when (selectedFilter) {
            "Emergency" -> history.filter { it.reason.contains("Emergency Contact", ignoreCase = true) }
            "Messages" -> history.filter { it.reason.contains("Message", ignoreCase = true) }
            "Alerts" -> history.filter { it.reason.contains("Repeated", ignoreCase = true) }
            else -> history
        }
    }

    val sdfDate = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()) }
    val displayDateFormat = remember { SimpleDateFormat("d MMM yyyy", Locale.getDefault()) }
    val timeFormat = remember { SimpleDateFormat("hh:mm a", Locale.getDefault()) }
    val fullFormat = remember { SimpleDateFormat("hh:mm:ss a, dd MMM yyyy", Locale.getDefault()) }

    val grouped = remember(filteredHistory) {
        val today = sdfDate.format(Date())
        val yesterday = sdfDate.format(Date(System.currentTimeMillis() - 86_400_000L))

        filteredHistory
            .groupBy { sdfDate.format(Date(it.timestampMs)) }
            .entries
            .sortedByDescending { it.key }
            .map { entry ->
                val label = when (entry.key) {
                    today -> "Today"
                    yesterday -> "Yesterday"
                    else -> displayDateFormat.format(sdfDate.parse(entry.key) ?: Date())
                }
                Pair(label, entry.value)
            }
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

        LazyColumn(
            modifier = Modifier.fillMaxSize()
        ) {
            item {
                // ── Header ────────────────────────────────────────
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                        .padding(top = 52.dp, bottom = 16.dp)
                ) {
                    Text(
                        "Activity",
                        fontSize = 32.sp,
                        fontWeight = FontWeight.ExtraLight,
                        color = WText
                    )
                }
            }

            // ── Filter Chips ─────────────────────────────────────
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                        .padding(bottom = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("All", "Alerts", "Messages", "Emergency").forEach { filter ->
                        val isSelected = selectedFilter == filter
                        Box(
                            modifier = Modifier
                                .height(34.dp)
                                .clip(RoundedCornerShape(17.dp))
                                .background(
                                    if (isSelected) WAccent
                                    else Color.White.copy(alpha = 0.65f)
                                )
                                .border(
                                    1.dp,
                                    if (isSelected) WAccent else WLine,
                                    RoundedCornerShape(17.dp)
                                )
                                .clickable { selectedFilter = filter }
                                .padding(horizontal = 14.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                filter,
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold
                                             else FontWeight.Medium,
                                color = if (isSelected) Color.White else WMuted
                            )
                        }
                    }
                }
            }

            // ── Stats Summary ────────────────────────────────────
            if (history.isNotEmpty()) {
                item {
                    val emergencyCount = history.count { it.reason.contains("Emergency Contact", ignoreCase = true) }
                    val messageCount = history.count { it.reason.contains("Message", ignoreCase = true) }
                    val alertCount = history.count { it.reason.contains("Repeated", ignoreCase = true) }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp)
                            .padding(bottom = 16.dp)
                            .background(Color.White.copy(alpha = 0.55f), RoundedCornerShape(16.dp))
                            .border(1.dp, WLine, RoundedCornerShape(16.dp))
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        StatItem("${history.size}", "Total")
                        StatItem("$alertCount", "Alerts")
                        StatItem("$messageCount", "Messages")
                        StatItem("$emergencyCount", "Emergency")
                    }
                }
            }

            if (filteredHistory.isEmpty()) {
                // Empty state
                item {
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
                            Text(
                                when (selectedFilter) {
                                    "Emergency" -> "No emergency alerts"
                                    "Messages" -> "No message alerts"
                                    "Alerts" -> "No repeated caller alerts"
                                    else -> "No triggers yet"
                                },
                                fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = WText
                            )
                            Text(
                                when (selectedFilter) {
                                    "All" -> "When an emergency alarm fires,\nit will appear here."
                                    else -> "Try selecting a different filter."
                                },
                                fontSize = 14.sp, color = WMuted,
                                textAlign = TextAlign.Center, lineHeight = 20.sp
                            )
                        }
                    }
                }
            } else {

                grouped.forEachIndexed { groupIdx, (dateLabel, records) ->

                    stickyHeader {
                        // ── Date separator ─────────────────────────
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(IntrinsicSize.Min)
                                .background(Color.White.copy(alpha = 0.0f)) // transparent background to allow stickiness to glide visually
                                .padding(start = 20.dp, end = 16.dp),
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
                                fontSize = 16.sp, fontWeight = FontWeight.ExtraBold,
                                letterSpacing = 0.5.sp, color = WText
                            )
                        }
                    }

                    // ── Event rows ─────────────────────────────
                    itemsIndexed(records) { recIdx, record ->
                        val isLast = groupIdx == grouped.lastIndex && recIdx == records.lastIndex
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(IntrinsicSize.Min)
                                .padding(start = 20.dp, end = 16.dp),
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
                                 
                                 // Top bridge line
                                 if (recIdx > 0 || groupIdx > 0) {
                                     Box(
                                         modifier = Modifier
                                             .height(20.dp)
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
                            val timeStr = timeFormat.format(Date(record.timestampMs))

                             Card(
                                 modifier = Modifier
                                     .weight(1f)
                                     .weightedSpring()
                                     .padding(bottom = 20.dp)
                                     .clickable {
                                         expandedRecordTs = if (expandedRecordTs == record.timestampMs) null
                                                            else record.timestampMs
                                     },
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
                                             fontWeight = FontWeight.SemiBold, color = WText
                                         )
                                         Text(
                                             timeStr, fontSize = 11.sp, 
                                             color = WMuted.copy(alpha = 0.6f),
                                             fontWeight = FontWeight.Medium
                                         )
                                     }
                                    Spacer(Modifier.height(8.dp))
                                     Row(
                                         modifier = Modifier.fillMaxWidth(),
                                         horizontalArrangement = Arrangement.SpaceBetween,
                                         verticalAlignment = Alignment.CenterVertically
                                     ) {
                                         val cleanReason = record.reason
                                             .replace("Message from", "", ignoreCase = true)
                                             .replace("Emergency", "", ignoreCase = true)
                                             .replace("Contact", "", ignoreCase = true)
                                             .replace("()", "", ignoreCase = true)
                                             .trim()

                                         Row(
                                             horizontalArrangement = Arrangement.spacedBy(8.dp),
                                             verticalAlignment = Alignment.CenterVertically,
                                             modifier = Modifier.weight(1f)
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
                                             if (cleanReason.isNotEmpty()) {
                                                 Text(
                                                     cleanReason, 
                                                     fontSize = 12.sp, color = WMuted,
                                                     maxLines = 1,
                                                     fontStyle = if (style.italic) FontStyle.Italic
                                                                 else FontStyle.Normal
                                                 )
                                             }
                                         }
                                     }
                                     // ── Expanded details ────────────
                                     AnimatedVisibility(visible = expandedRecordTs == record.timestampMs) {
                                         Column {
                                             Spacer(Modifier.height(10.dp))
                                             @Suppress("DEPRECATION") Divider(color = WLine, thickness = 1.dp)
                                             Spacer(Modifier.height(10.dp))
                                             Row(
                                                 horizontalArrangement = Arrangement.spacedBy(6.dp),
                                                 verticalAlignment = Alignment.CenterVertically
                                             ) {
                                                 Icon(Icons.Default.Info, null, tint = WMuted, modifier = Modifier.size(14.dp))
                                                 Text("Trigger: ${record.reason}", fontSize = 12.sp, color = WMuted)
                                             }
                                             Spacer(Modifier.height(4.dp))
                                             Row(
                                                 horizontalArrangement = Arrangement.spacedBy(6.dp),
                                                 verticalAlignment = Alignment.CenterVertically
                                             ) {
                                                 Icon(Icons.Default.Schedule, null, tint = WMuted.copy(alpha = 0.6f), modifier = Modifier.size(14.dp))
                                                 Text(
                                                     fullFormat.format(Date(record.timestampMs)),
                                                     fontSize = 12.sp,
                                                     color = WMuted.copy(alpha = 0.6f)
                                                 )
                                             }
                                             
                                             if (record.priorTimestamps.isNotEmpty()) {
                                                 Spacer(Modifier.height(12.dp))
                                                 Text("Call Timeline", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = WText.copy(alpha = 0.8f))
                                                 Spacer(Modifier.height(4.dp))
                                                 record.priorTimestamps.forEachIndexed { index, ts ->
                                                     Row(
                                                         modifier = Modifier.padding(vertical = 2.dp),
                                                         horizontalArrangement = Arrangement.spacedBy(6.dp),
                                                         verticalAlignment = Alignment.CenterVertically
                                                     ) {
                                                         Icon(Icons.Default.Phone, null, tint = WMuted.copy(alpha = 0.5f), modifier = Modifier.size(12.dp))
                                                         val isTrigger = index == record.priorTimestamps.lastIndex
                                                         Text(
                                                             "Call ${index + 1}${if (isTrigger) " (Trigger)" else ""}:",
                                                             fontSize = 12.sp,
                                                             fontWeight = if (isTrigger) FontWeight.Bold else FontWeight.Medium,
                                                             color = if (isTrigger) Color(0xFFF16558) else WMuted.copy(alpha = 0.8f)
                                                         )
                                                         Text(
                                                             timeFormat.format(Date(ts)),
                                                             fontSize = 12.sp,
                                                             color = WMuted
                                                         )
                                                     }
                                                 }
                                             }
                                         }
                                     }
                                 }
                             }
                        }
                    }
                }

                // ── Clear History at bottom of scroll ──────────
                item {
                    Spacer(Modifier.height(8.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp)
                            .height(56.dp)
                            .magneticAffinity(strength = 0.2f)
                            .border(1.dp, Color.White.copy(alpha = 0.5f), RoundedCornerShape(28.dp))
                            .background(Color.White.copy(alpha = 0.6f), RoundedCornerShape(28.dp))
                            .clip(RoundedCornerShape(28.dp))
                            .clickable { showClearDialog = true },
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Clear History", fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold, color = Color.Black)
                    }
                    Spacer(Modifier.height(120.dp))
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
        RowStyle(WAccent, Color.White, Icons.Default.Favorite, BadgeCBg, BadgeCTxt, "CALL")
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

@Composable
private fun StatItem(count: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(count, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = WAccent)
        Text(label, fontSize = 11.sp, color = WMuted, fontWeight = FontWeight.Medium)
    }
}
