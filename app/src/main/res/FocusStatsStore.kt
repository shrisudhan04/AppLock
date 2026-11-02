package com.strictfocus

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/** Tracks scheduled focus duration locally. Values represent planned minutes, not verified foreground focus. */
data class FocusStats(
    val todayMinutes: Int,
    val streakDays: Int,
    val weeklyMinutes: List<Int>,
    val weeklyLabels: List<String>,
    val totalSessions: Int
)

object FocusStatsStore {
    private const val PREFS = "strictfocus_stats"
    private const val PREFIX = "day_"

    private fun dayKey(millis: Long = System.currentTimeMillis()): String =
        SimpleDateFormat("yyyyMMdd", Locale.US).format(Date(millis))

    fun recordPlannedSession(context: Context, minutes: Int, appCount: Int) {
        if (minutes <= 0 || appCount <= 0) return
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val key = PREFIX + dayKey()
        prefs.edit()
            .putInt(key, prefs.getInt(key, 0) + minutes)
            .putInt("total_sessions", prefs.getInt("total_sessions", 0) + 1)
            .apply()
    }

    fun load(context: Context): FocusStats {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val calendar = Calendar.getInstance()
        val today = dayKey()
        val todayMinutes = prefs.getInt(PREFIX + today, 0)
        val week = mutableListOf<Int>()
        val labels = mutableListOf<String>()
        for (i in 0..6) {
            calendar.timeInMillis = System.currentTimeMillis()
            calendar.add(Calendar.DAY_OF_YEAR, -i)
            val key = dayKey(calendar.timeInMillis)
            val minutes = prefs.getInt(PREFIX + key, 0)
            week.add(minutes)
            labels.add(SimpleDateFormat("EE", Locale.getDefault()).format(Date(calendar.timeInMillis)).take(1))
        }
        var streak = 0
        calendar.timeInMillis = System.currentTimeMillis()
        if (todayMinutes == 0) calendar.add(Calendar.DAY_OF_YEAR, -1)
        while (true) {
            val key = dayKey(calendar.timeInMillis)
            if (prefs.getInt(PREFIX + key, 0) <= 0) break
            streak++
            calendar.add(Calendar.DAY_OF_YEAR, -1)
            if (streak >= 3650) break
        }
        return FocusStats(todayMinutes, streak, week.reversed(), labels.reversed(), prefs.getInt("total_sessions", 0))
    }
}

@Composable
fun FocusAnalyticsCard(
    stats: FocusStats,
    goalMinutes: Int,
    isDark: Boolean,
    onGoalChange: (Int) -> Unit
) {
    val surface = if (isDark) Color(0xFF27272A) else Color.White
    val text = if (isDark) Color(0xFFFAFAFA) else Color(0xFF1B1B27)
    val muted = if (isDark) Color(0xFFA1A1AA) else Color(0xFF666678)
    val accent = if (isDark) Color(0xFF8B5CF6) else Color(0xFF7C3AED)
    val mint = if (isDark) Color(0xFF8CE6C3) else Color(0xFF16865D)
    val progress = (stats.todayMinutes.toFloat() / goalMinutes.coerceAtLeast(1)).coerceIn(0f, 1f)
    Card(colors = CardDefaults.cardColors(containerColor = surface), shape = RoundedCornerShape(24.dp)) {
        Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Insights, contentDescription = null, tint = accent)
                Spacer(Modifier.width(8.dp))
                Text("FOCUS INSIGHTS", color = accent, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.3.sp)
                Spacer(Modifier.weight(1f))
                Icon(Icons.Filled.LocalFireDepartment, contentDescription = null, tint = Color(0xFFF59E0B), modifier = Modifier.size(18.dp))
                Text("${stats.streakDays} day streak", color = text, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            }
            Row(verticalAlignment = Alignment.Bottom) {
                Text("${stats.todayMinutes / 60}h ${stats.todayMinutes % 60}m", color = text, fontSize = 28.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.width(8.dp))
                Text("planned today", color = muted, fontSize = 12.sp, modifier = Modifier.padding(bottom = 5.dp))
                Spacer(Modifier.weight(1f))
                Icon(Icons.Filled.EmojiEvents, contentDescription = null, tint = mint, modifier = Modifier.size(22.dp))
            }
            LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(50)), color = accent, trackColor = if (isDark) Color(0xFF3F3F46) else Color(0xFFE5E7EB))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("${(progress * 100).toInt()}% of daily goal", color = muted, fontSize = 11.sp)
                Spacer(Modifier.weight(1f))
                Text("Goal: ${goalMinutes / 60}h ${goalMinutes % 60}m", color = text, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.Bottom, modifier = Modifier.fillMaxWidth().height(76.dp)) {
                val max = (stats.weeklyMinutes.maxOrNull() ?: 0).coerceAtLeast(1)
                stats.weeklyMinutes.forEachIndexed { index, minutes ->
                    Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.BottomCenter) {
                            Box(Modifier.fillMaxWidth(0.68f).height((minutes.toFloat() / max * 52f).coerceAtLeast(4f).dp).clip(RoundedCornerShape(topStart = 5.dp, topEnd = 5.dp)).background(if (minutes > 0) accent else if (isDark) Color(0xFF3F3F46) else Color(0xFFE5E7EB)))
                        }
                        Text(stats.weeklyLabels.getOrElse(index) { "·" }, color = muted, fontSize = 10.sp)
                    }
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("${stats.totalSessions} sessions planned", color = muted, fontSize = 11.sp)
                Spacer(Modifier.weight(1f))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(60, 120, 180, 240).forEach { goal ->
                        FilterChip(selected = goalMinutes == goal, onClick = { onGoalChange(goal) }, label = { Text("${goal / 60}h", fontSize = 10.sp) }, modifier = Modifier.height(30.dp))
                    }
                }
            }
            Text("Statistics count planned block duration; Android cannot reliably verify actual focus time.", color = muted, fontSize = 10.sp, lineHeight = 14.sp)
        }
    }
}
