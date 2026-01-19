package com.example.digit

import android.app.AppOpsManager
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Paint
import android.graphics.drawable.Drawable
import android.graphics.pdf.PdfDocument
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.digit.ui.theme.DIGITTheme
import com.example.digit.ui.theme.NeonBlue
import com.example.digit.ui.theme.NeonPurple
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

// --- THEME CONSTANTS ---
val DeepSpace = Color(0xFF050511)
val CardBgDark = Color(0xFF13132B)
val CardBorderDark = Color(0xFF2D2D55)

val CleanWhite = Color(0xFFF8FAFC)
val CardBgLight = Color(0xFFFFFFFF)
val CardBorderLight = Color(0xFFE2E8F0)

// Colors
val GlowCyan = Color(0xFF00E5FF)
val GlowPurple = Color(0xFF9D00FF)
val GlowEmerald = Color(0xFF00FF94)
val GlowAmber = Color(0xFFFFB300)
val GlowPink = Color(0xFFFF005C)
val GlowOrange = Color(0xFFFF5722)
val GlowTeal = Color(0xFF1DE9B6)

// --- DATA MODELS ---
enum class AppCategory { Productive, Unproductive, Game, Study, Neutral }
enum class TimeRange(val label: String) { Daily("Today"), Weekly("Week"), Monthly("Month") }
enum class Screen(val label: String, val icon: ImageVector) {
    Dashboard("Hub", Icons.Default.Home),
    Apps("Apps", Icons.Default.List),
    Goals("Limit", Icons.Default.Lock),
    Reports("Audit", Icons.Default.Share)
}

data class AppUsageInfo(
    val packageName: String,
    val label: String,
    val timeInForeground: Long,
    val lastTimeUsed: Long,
    val openCount: Int,
    val icon: ImageBitmap?,
    val category: AppCategory
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            DIGITTheme {
                MainAppStructure()
            }
        }
    }
}

@Composable
fun MainAppStructure() {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("digit_prefs", Context.MODE_PRIVATE) }
    var currentScreen by remember { mutableStateOf(Screen.Dashboard) }
    var usageList by remember { mutableStateOf<List<AppUsageInfo>>(emptyList()) }
    var timeRange by remember { mutableStateOf(TimeRange.Daily) }

    var isDarkTheme by remember { mutableStateOf(prefs.getBoolean("is_dark_theme", true)) }

    val bgColor by animateColorAsState(if (isDarkTheme) DeepSpace else CleanWhite, label = "bg")
    val cardBg = if (isDarkTheme) CardBgDark else CardBgLight
    val textColor = if (isDarkTheme) Color.White else Color.Black
    val subTextColor = if (isDarkTheme) Color.Gray else Color.DarkGray
    val borderColor = if (isDarkTheme) CardBorderDark else CardBorderLight

    val hasUsagePermission = checkUsagePermission(context)

    LaunchedEffect(timeRange, hasUsagePermission, currentScreen) {
        if (hasUsagePermission) {
            usageList = withContext(Dispatchers.IO) {
                fetchDetailedUsage(context, timeRange)
            }
        }
    }

    Scaffold(
        containerColor = bgColor,
        bottomBar = {
            GlassDock(currentScreen, isDarkTheme) { currentScreen = it }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    if (isDarkTheme) Brush.radialGradient(colors = listOf(Color(0xFF101025), DeepSpace), radius = 1200f)
                    else Brush.verticalGradient(listOf(Color(0xFFF1F5F9), Color(0xFFE2E8F0)))
                )
                .padding(padding)
        ) {
            if (!hasUsagePermission) {
                PermissionRequestView(context, isDarkTheme)
            } else {
                Column {
                    DashboardHeader(isDarkTheme, {
                        isDarkTheme = !isDarkTheme
                        prefs.edit().putBoolean("is_dark_theme", isDarkTheme).apply()
                    }, cardBg, borderColor, textColor)

                    androidx.compose.animation.Crossfade(targetState = currentScreen, label = "PageNav") { screen ->
                        Box(Modifier.padding(horizontal = 16.dp)) {
                            when (screen) {
                                Screen.Dashboard -> DashboardView(usageList, prefs, isDarkTheme, textColor, subTextColor, cardBg, borderColor)
                                Screen.Apps -> {
                                    Column {
                                        ModernRangeSelector(timeRange, isDarkTheme) { timeRange = it }
                                        AppListView(usageList, prefs, isDarkTheme, textColor, cardBg, borderColor)
                                    }
                                }
                                Screen.Goals -> GoalsView(prefs, isDarkTheme, textColor, subTextColor, cardBg, borderColor)
                                Screen.Reports -> ReportsView(usageList, context, isDarkTheme, textColor, cardBg, borderColor)
                            }
                        }
                    }
                }
            }
        }
    }
}

// --- CORE ENGINE ---
fun fetchDetailedUsage(context: Context, range: TimeRange): List<AppUsageInfo> {
    val usm = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
    val pm = context.packageManager
    val endTime = System.currentTimeMillis()
    val calendar = Calendar.getInstance()

    when (range) {
        TimeRange.Daily -> {
            calendar.set(Calendar.HOUR_OF_DAY, 0)
            calendar.set(Calendar.MINUTE, 0)
            calendar.set(Calendar.SECOND, 0)
            calendar.set(Calendar.MILLISECOND, 0)
        }
        TimeRange.Weekly -> {
            calendar.add(Calendar.DAY_OF_YEAR, -7)
            calendar.set(Calendar.HOUR_OF_DAY, 0)
            calendar.set(Calendar.MINUTE, 0)
            calendar.set(Calendar.SECOND, 0)
            calendar.set(Calendar.MILLISECOND, 0)
        }
        TimeRange.Monthly -> {
            calendar.add(Calendar.MONTH, -1)
            calendar.set(Calendar.HOUR_OF_DAY, 0)
            calendar.set(Calendar.MINUTE, 0)
            calendar.set(Calendar.SECOND, 0)
            calendar.set(Calendar.MILLISECOND, 0)
        }
    }
    val startTime = calendar.timeInMillis

    val statsMap = usm.queryAndAggregateUsageStats(startTime, endTime)
    val openingsMap = mutableMapOf<String, Int>()
    val events = usm.queryEvents(startTime, endTime)
    val event = UsageEvents.Event()
    while (events.hasNextEvent()) {
        events.getNextEvent(event)
        if (event.eventType == UsageEvents.Event.MOVE_TO_FOREGROUND) {
            openingsMap[event.packageName] = (openingsMap[event.packageName] ?: 0) + 1
        }
    }

    val installedApps = pm.getInstalledApplications(0)
    return installedApps.mapNotNull { info ->
        val pkg = info.packageName
        val label = try { info.loadLabel(pm).toString() } catch (e: Exception) { pkg }
        val icon = try { drawableToBitmap(info.loadIcon(pm)).asImageBitmap() } catch (e: Exception) { null }
        val usage = statsMap[pkg]
        val time = usage?.totalTimeInForeground ?: 0L
        val lastUsed = usage?.lastTimeUsed ?: 0L
        val catCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) info.category else -1

        AppUsageInfo(pkg, label, time, lastUsed, openingsMap[pkg] ?: 0, icon, categorizeExact(pkg, catCode))
    }.sortedByDescending { it.timeInForeground }
}

fun categorizeExact(pkg: String, sysCategory: Int): AppCategory {
    val p = pkg.lowercase()
    if (sysCategory == ApplicationInfo.CATEGORY_GAME || p.contains("game")) return AppCategory.Game
    val study = listOf("zoom", "teams", "classroom", "duolingo", "khan", "coursera", "udemy", "linkedin", "docs", "sheets", "drive", "pdf", "wiki")
    if (study.any { p.contains(it) }) return AppCategory.Study
    val unproductive = listOf("instagram", "facebook", "tiktok", "snapchat", "twitter", "x", "youtube", "netflix", "prime", "hotstar", "disney", "spotify", "twitch", "reddit")
    if (unproductive.any { p.contains(it) }) return AppCategory.Unproductive
    val productive = listOf("clock", "settings", "dialer", "calendar", "calculator", "maps", "gmail", "whatsapp", "message", "contact", "bank", "pay")
    if (productive.any { p.contains(it) }) return AppCategory.Productive
    return AppCategory.Neutral
}

// --- DASHBOARD ---

@Composable
fun DashboardView(
    list: List<AppUsageInfo>,
    prefs: SharedPreferences,
    isDark: Boolean,
    textColor: Color,
    subTextColor: Color,
    cardBg: Color,
    borderColor: Color
) {
    val today = Calendar.getInstance().get(Calendar.DAY_OF_YEAR)
    val manualStudyTime = prefs.getLong("manual_study_$today", 0L)

    val appProd = list.filter { it.category == AppCategory.Productive }.sumOf { it.timeInForeground }
    val unprod = list.filter { it.category == AppCategory.Unproductive }.sumOf { it.timeInForeground }
    val appStudy = list.filter { it.category == AppCategory.Study }.sumOf { it.timeInForeground }
    val game = list.filter { it.category == AppCategory.Game }.sumOf { it.timeInForeground }

    val studyTotal = appStudy + manualStudyTime
    val grandTotal = list.sumOf { it.timeInForeground } + manualStudyTime
    val safeTotal = grandTotal.toFloat().coerceAtLeast(1f)

    val prodP = appProd / safeTotal
    val unprodP = unprod / safeTotal
    val studyP = studyTotal / safeTotal
    val gameP = game / safeTotal

    val topApp = list.maxByOrNull { it.timeInForeground }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 100.dp)
    ) {
        item {
            // --- CONCENTRIC THICK CHARTS ---
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp)
            ) {
                // Background Glow
                Box(
                    modifier = Modifier
                        .size(240.dp)
                        .background(
                            Brush.radialGradient(colors = listOf(GlowCyan.copy(alpha = 0.15f), Color.Transparent)),
                            CircleShape
                        )
                )

                // The Chart
                ConcentricThickChart(studyP, prodP, unprodP, gameP, isDark)

                // Center Text
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = formatTime(grandTotal),
                        color = textColor,
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Black
                    )
                    Text(
                        text = "TOTAL",
                        color = subTextColor,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                }
            }
        }

        item {
            Text("Overview", color = textColor, fontSize = 18.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 16.dp))

            // Simple Gradient Box Design
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    GradientStatCard("Productive", formatTime(appProd), NeonBlue, Icons.Default.Check, Modifier.weight(1f), textColor)
                    GradientStatCard("Unproductive", formatTime(unprod), GlowPurple, Icons.Default.Warning, Modifier.weight(1f), textColor)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    GradientStatCard("Gaming", formatTime(game), GlowEmerald, Icons.Default.PlayArrow, Modifier.weight(1f), textColor)
                    GradientStatCard("Most Used", topApp?.label ?: "None", GlowPink, Icons.Default.Star, Modifier.weight(1f), textColor)
                }
            }
            Spacer(Modifier.height(24.dp))
        }

        item {
            val studyGoal = prefs.getLong("study_goal", 60) * 60000L
            val mediaLimit = prefs.getLong("daily_goal", 120) * 60000L

            GlassCard(cardBg, borderColor) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Daily Targets", color = textColor, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Icon(Icons.Default.DateRange, null, tint = GlowCyan)
                }
                Spacer(Modifier.height(16.dp))
                NeonGoalBar("Focus Goal", studyTotal, studyGoal, GlowCyan, textColor, Color.Transparent)
                Spacer(Modifier.height(12.dp))
                NeonGoalBar("Social Limit", unprod, mediaLimit, GlowPurple, textColor, Color.Transparent, isLimit = true)
            }
        }
    }
}

// --- THICK CONCENTRIC CHART ---

@Composable
fun ConcentricThickChart(study: Float, prod: Float, unprod: Float, game: Float, isDark: Boolean) {
    val animStudy by animateFloatAsState(targetValue = study, animationSpec = tween(1200))
    val animUnprod by animateFloatAsState(targetValue = unprod, animationSpec = tween(1200))
    val animGame by animateFloatAsState(targetValue = game, animationSpec = tween(1200))

    Canvas(modifier = Modifier.size(240.dp)) {
        val center = Offset(size.width / 2, size.height / 2)
        val strokeWidth = 35f // Thick rings
        val gap = 12f
        val trackColor = if(isDark) Color(0xFF1E293B) else Color(0xFFE2E8F0)

        // Ring 1 (Outer - Study)
        val r1 = size.minDimension / 2 - strokeWidth / 2
        drawCircle(color = trackColor, radius = r1, style = Stroke(strokeWidth))
        drawArc(
            brush = Brush.sweepGradient(listOf(GlowCyan, NeonBlue)),
            startAngle = -90f,
            sweepAngle = animStudy * 360f,
            useCenter = false,
            style = Stroke(strokeWidth, cap = StrokeCap.Round),
            topLeft = Offset((size.width - r1 * 2) / 2, (size.height - r1 * 2) / 2),
            size = Size(r1 * 2, r1 * 2)
        )

        // Ring 2 (Middle - Social)
        val r2 = r1 - strokeWidth - gap
        drawCircle(color = trackColor, radius = r2, style = Stroke(strokeWidth))
        drawArc(
            brush = Brush.sweepGradient(listOf(GlowPurple, NeonPurple)),
            startAngle = -90f,
            sweepAngle = animUnprod * 360f,
            useCenter = false,
            style = Stroke(strokeWidth, cap = StrokeCap.Round),
            topLeft = Offset((size.width - r2 * 2) / 2, (size.height - r2 * 2) / 2),
            size = Size(r2 * 2, r2 * 2)
        )

        // Ring 3 (Inner - Game)
        val r3 = r2 - strokeWidth - gap
        drawCircle(color = trackColor, radius = r3, style = Stroke(strokeWidth))
        drawArc(
            brush = Brush.sweepGradient(listOf(GlowEmerald, Color.Green)),
            startAngle = -90f,
            sweepAngle = animGame * 360f,
            useCenter = false,
            style = Stroke(strokeWidth, cap = StrokeCap.Round),
            topLeft = Offset((size.width - r3 * 2) / 2, (size.height - r3 * 2) / 2),
            size = Size(r3 * 2, r3 * 2)
        )
    }
}

// --- SIMPLE GRADIENT STAT CARD (No Box/Border Design) ---

@Composable
fun GradientStatCard(label: String, value: String, color: Color, icon: ImageVector, modifier: Modifier, textColor: Color) {
    Box(
        modifier = modifier
            .height(90.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(
                Brush.verticalGradient(
                    colors = listOf(color.copy(alpha = 0.15f), Color.Transparent)
                )
            )
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(14.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, null, tint = color, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(label, color = textColor.copy(alpha = 0.7f), fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
            }
            Text(value, color = textColor, fontSize = 16.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

// --- EXPORT PAGE ---

@Composable
fun ReportsView(
    list: List<AppUsageInfo>,
    context: Context,
    isDark: Boolean,
    textColor: Color,
    cardBg: Color,
    border: Color
) {
    val totalTime = list.sumOf { it.timeInForeground }
    val tint = if(isDark) GlowCyan else NeonBlue
    var reportRange by remember { mutableStateOf("Today") }
    var includeSystem by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.Top, horizontalAlignment = Alignment.CenterHorizontally) {
        Spacer(Modifier.height(20.dp))
        Text("Export Data", color = textColor, fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(30.dp))

        // Range Selector
        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            listOf("Today", "Week", "Month").forEach { r ->
                val selected = reportRange == r
                Box(Modifier
                    .weight(1f)
                    .padding(4.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(if(selected) tint.copy(alpha=0.2f) else Color.Transparent)
                    .border(1.dp, if(selected) tint else border, RoundedCornerShape(8.dp))
                    .clickable { reportRange = r }
                    .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(r, color = if(selected) tint else Color.Gray, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }
        }

        Spacer(Modifier.height(20.dp))

        // Preview Card
        GlassCard(cardBg, border) {
            Text("Preview Summary", color = textColor, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Spacer(Modifier.height(16.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text("Total Duration", color = Color.Gray, fontSize = 12.sp)
                    Text(formatTime(totalTime), color = textColor, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("Apps Count", color = Color.Gray, fontSize = 12.sp)
                    Text("${list.size}", color = textColor, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }
            }
            Spacer(Modifier.height(16.dp))

            // Toggle
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Detailed Log", color = textColor)
                Switch(checked = includeSystem, onCheckedChange = { includeSystem = it }, colors = SwitchDefaults.colors(checkedThumbColor = tint))
            }
        }

        Spacer(Modifier.weight(1f))

        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(
                onClick = { /* Share */ },
                colors = ButtonDefaults.buttonColors(containerColor = cardBg),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.weight(1f).height(56.dp).border(1.dp, border, RoundedCornerShape(16.dp))
            ) {
                Icon(Icons.Default.Share, null, tint = textColor)
            }
            Button(
                onClick = { generateProPDF(context, list) },
                colors = ButtonDefaults.buttonColors(containerColor = tint),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.weight(3f).height(56.dp).shadow(12.dp, RoundedCornerShape(16.dp), ambientColor = tint, spotColor = tint)
            ) {
                Text("DOWNLOAD PDF", color = Color.Black, fontWeight = FontWeight.Black, letterSpacing = 1.sp)
            }
        }
        Spacer(Modifier.height(30.dp))
    }
}

// --- SHARED COMPONENTS ---

@Composable
fun DashboardHeader(isDark: Boolean, onToggle: () -> Unit, bg: Color, border: Color, textCol: Color) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            Text("Digital Pulse", color = textCol, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold)
            Text(SimpleDateFormat("EEEE, d MMMM", Locale.getDefault()).format(Date()), color = Color.Gray, fontSize = 12.sp)
        }

        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(50))
                .background(bg)
                .border(1.dp, border, RoundedCornerShape(50))
                .clickable { onToggle() }
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Text(
                if (isDark) "LIGHT MODE" else "DARK MODE",
                color = textCol,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
        }
    }
}

@Composable
fun GlassCard(bg: Color, border: Color, content: @Composable ColumnScope.() -> Unit) {
    Card(
        colors = CardDefaults.cardColors(bg),
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.dp, border),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(20.dp), content = content)
    }
}

@Composable
fun NeonGoalBar(label: String, current: Long, max: Long, color: Color, textColor: Color, bg: Color, isLimit: Boolean = false) {
    val progress = (current.toFloat() / max).coerceIn(0f, 1f)
    val animProgress by animateFloatAsState(targetValue = progress, animationSpec = tween(1000))
    val statusColor = if (isLimit && progress >= 1f) Color.Red else color

    Column {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, color = textColor, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            Text(
                "${TimeUnit.MILLISECONDS.toMinutes(current)} / ${TimeUnit.MILLISECONDS.toMinutes(max)}m",
                color = statusColor,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp
            )
        }
        Spacer(Modifier.height(8.dp))
        Box(
            Modifier.height(10.dp).fillMaxWidth().clip(RoundedCornerShape(50)).background(if(bg == Color.Transparent) Color.Black.copy(alpha = 0.1f) else bg)
        ) {
            Box(
                Modifier.fillMaxHeight().fillMaxWidth(animProgress).clip(RoundedCornerShape(50))
                    .background(Brush.horizontalGradient(listOf(statusColor.copy(alpha = 0.5f), statusColor)))
            )
        }
    }
}

@Composable
fun GlassDock(current: Screen, isDark: Boolean = true, onSelect: (Screen) -> Unit) {
    val bg = if(isDark) Color(0xFF1E293B).copy(alpha = 0.95f) else Color(0xFFFFFFFF).copy(alpha = 0.95f)
    val border = if(isDark) Color(0xFF334155) else Color(0xFFE2E8F0)

    Box(
        modifier = Modifier.padding(20.dp).fillMaxWidth().height(70.dp)
            .shadow(10.dp, RoundedCornerShape(50))
            .clip(RoundedCornerShape(50))
            .background(bg)
            .border(1.dp, border, RoundedCornerShape(50))
    ) {
        Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
            Screen.values().forEach { screen ->
                val selected = current == screen
                val color = if (selected) NeonBlue else Color.Gray
                val scale by animateFloatAsState(if(selected) 1.15f else 1f)

                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.scale(scale).clickable { onSelect(screen) }) {
                    Icon(screen.icon, null, tint = color, modifier = Modifier.size(26.dp))
                    if(selected) Box(Modifier.padding(top = 4.dp).size(4.dp).background(color, CircleShape))
                }
            }
        }
    }
}

@Composable
fun ModernRangeSelector(current: TimeRange, isDark: Boolean, onSelect: (TimeRange) -> Unit) {
    val bg = if(isDark) CardBgDark else CardBgLight
    val border = if(isDark) CardBorderDark else CardBorderLight

    Row(
        Modifier
            .fillMaxWidth()
            .height(45.dp)
            .background(bg, RoundedCornerShape(50))
            .border(1.dp, border, RoundedCornerShape(50))
            .padding(4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        TimeRange.values().forEach { range ->
            val selected = range == current
            val bgSel = if (selected) NeonBlue.copy(alpha = 0.2f) else Color.Transparent
            val textCol = if (selected) NeonBlue else Color.Gray
            Box(contentAlignment = Alignment.Center, modifier = Modifier.weight(1f).fillMaxHeight().clip(RoundedCornerShape(50)).background(bgSel).clickable { onSelect(range) }) {
                Text(range.label, color = textCol, fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
        }
    }
}

// --- APP LIST & GOALS ---
@Composable
fun AppListView(list: List<AppUsageInfo>, prefs: SharedPreferences, isDark: Boolean, textColor: Color, cardBg: Color, border: Color) {
    val max = list.firstOrNull()?.timeInForeground?.toFloat() ?: 1f
    LazyColumn(contentPadding = PaddingValues(bottom = 100.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { Text("All Applications", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = textColor, modifier = Modifier.padding(vertical = 10.dp)) }
        items(list) { app ->
            val color = getCatColor(app.category)
            val notifs = getNotifCount(prefs, app.packageName)
            val ratio = (app.timeInForeground.toFloat() / max).coerceIn(0f, 1f)

            Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(cardBg).border(1.dp, border, RoundedCornerShape(16.dp)).padding(14.dp)) {
                Box(Modifier.matchParentSize().clip(RoundedCornerShape(16.dp))) {
                    Box(Modifier.fillMaxHeight().fillMaxWidth(ratio).background(color.copy(alpha = 0.08f)))
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (app.icon != null) Image(app.icon, null, Modifier.size(46.dp).clip(RoundedCornerShape(12.dp)))
                    else Box(Modifier.size(46.dp).background(Color.Gray, RoundedCornerShape(12.dp)))
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text(app.label, color = textColor, fontWeight = FontWeight.Bold, fontSize = 14.sp, maxLines = 1)
                        Text(formatTime(app.timeInForeground), color = color, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("$notifs", color = textColor, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Text("NOTIFS", color = Color.Gray, fontSize = 8.sp)
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(horizontalAlignment = Alignment.End) {
                        Text("${app.openCount}", color = textColor, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Text("OPENS", color = Color.Gray, fontSize = 8.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun GoalsView(prefs: SharedPreferences, isDark: Boolean, textColor: Color, subTextColor: Color, cardBg: Color, border: Color) {
    var study by remember { mutableStateOf(prefs.getLong("study_goal", 60).toFloat()) }
    var media by remember { mutableStateOf(prefs.getLong("daily_goal", 120).toFloat()) }
    val today = Calendar.getInstance().get(Calendar.DAY_OF_YEAR)
    var manualStudy by remember { mutableStateOf(prefs.getLong("manual_study_$today", 0L)) }

    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 100.dp)) {
        item {
            Text("Settings", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = textColor)
            Spacer(Modifier.height(20.dp))
            GlassCard(cardBg, border) {
                GoalSlider("Study Target", study, GlowCyan, textColor) { study = it; prefs.edit().putLong("study_goal", it.toLong()).apply() }
                Spacer(Modifier.height(24.dp))
                GoalSlider("Media Limit", media, GlowPurple, textColor) { media = it; prefs.edit().putLong("daily_goal", it.toLong()).apply() }
            }
            Spacer(Modifier.height(24.dp))
            Text("Offline Mode", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = textColor)
            Spacer(Modifier.height(10.dp))
            GlassCard(cardBg, border) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column {
                        Text("Manual Log", color = subTextColor, fontSize = 12.sp)
                        Text(formatTime(manualStudy), color = textColor, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                    }
                    IconButton(onClick = { manualStudy = 0L; prefs.edit().putLong("manual_study_$today", 0L).apply() }) { Icon(Icons.Default.Refresh, null, tint = Color.Gray) }
                }
                Spacer(Modifier.height(16.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { manualStudy += 900000L; prefs.edit().putLong("manual_study_$today", manualStudy).apply() }, colors = ButtonDefaults.buttonColors(containerColor = GlowCyan), modifier = Modifier.weight(1f)) { Text("+15m", color = Color.Black) }
                    Button(onClick = { manualStudy = (manualStudy - 900000L).coerceAtLeast(0L); prefs.edit().putLong("manual_study_$today", manualStudy).apply() }, colors = ButtonDefaults.buttonColors(containerColor = if(isDark) Color(0xFF334155) else Color(0xFFD1D5DB)), modifier = Modifier.weight(1f)) { Text("-15m", color = textColor) }
                }
            }
        }
    }
}

@Composable
fun GoalSlider(label: String, value: Float, color: Color, textColor: Color, onChange: (Float) -> Unit) {
    Column {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, color = textColor, fontSize = 12.sp)
            Text("${value.toInt()} min", color = color, fontWeight = FontWeight.Bold)
        }
        Slider(value = value, onValueChange = onChange, valueRange = 5f..480f, colors = SliderDefaults.colors(thumbColor = color, activeTrackColor = color, inactiveTrackColor = Color(0xFF0F172A)))
    }
}

// --- UTILS ---
fun generateProPDF(context: Context, list: List<AppUsageInfo>) {
    val doc = PdfDocument()
    val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
    val page = doc.startPage(pageInfo)
    val canvas = page.canvas
    val paint = Paint().apply { isAntiAlias = true }
    paint.textSize = 24f; paint.isFakeBoldText = true
    canvas.drawText("DIGIT: Professional Audit", 50f, 60f, paint)
    paint.textSize = 12f; paint.isFakeBoldText = false
    canvas.drawText("Generated: ${Date()}", 50f, 80f, paint)
    var y = 120f
    list.take(45).forEach {
        canvas.drawText("${it.label} [${it.category}]", 50f, y, paint)
        canvas.drawText("${formatTime(it.timeInForeground)} | ${it.openCount} opens", 350f, y, paint)
        y += 20f
    }
    doc.finishPage(page)
    val file = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "DIGIT_Audit_Pro.pdf")
    try { doc.writeTo(FileOutputStream(file)); Toast.makeText(context, "Saved to Downloads", Toast.LENGTH_LONG).show() }
    catch (e: Exception) { Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show() }
    doc.close()
}

fun getCatColor(cat: AppCategory) = when (cat) {
    AppCategory.Productive -> NeonBlue
    AppCategory.Study -> GlowCyan
    AppCategory.Unproductive -> GlowPurple
    AppCategory.Game -> GlowEmerald
    AppCategory.Neutral -> Color.Gray
}

fun getNotifCount(prefs: SharedPreferences, pkg: String): Int {
    val today = Calendar.getInstance().get(Calendar.DAY_OF_YEAR)
    return prefs.getInt("notif_${pkg}_$today", 0)
}

fun formatTime(ms: Long): String {
    val h = TimeUnit.MILLISECONDS.toHours(ms)
    val m = TimeUnit.MILLISECONDS.toMinutes(ms) % 60
    return if (h > 0) "${h}h ${m}m" else "${m}m"
}

fun drawableToBitmap(drawable: Drawable): Bitmap {
    val bitmap = Bitmap.createBitmap(drawable.intrinsicWidth.coerceAtLeast(1), drawable.intrinsicHeight.coerceAtLeast(1), Bitmap.Config.ARGB_8888)
    val canvas = android.graphics.Canvas(bitmap)
    drawable.setBounds(0, 0, canvas.width, canvas.height)
    drawable.draw(canvas)
    return bitmap
}

fun checkUsagePermission(context: Context): Boolean {
    val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
    val mode = appOps.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, android.os.Process.myUid(), context.packageName)
    return mode == AppOpsManager.MODE_ALLOWED
}

@Composable
fun PermissionRequestView(context: Context, isDark: Boolean) {
    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
        Text("Usage Permission Required", color = if(isDark) Color.White else Color.Black, fontWeight = FontWeight.Bold)
        Button(onClick = { context.startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)) }, colors = ButtonDefaults.buttonColors(containerColor = NeonBlue)) { Text("Grant Access", color = Color.Black) }
    }
}