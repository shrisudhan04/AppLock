package com.strictfocus

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

data class InstalledApp(val label: String, val packageName: String)

private val Night = Color(0xFF18181B)
private val Panel = Color(0xFF27272A)
private val PanelRaised = Color(0xFF3F3F46)
private val Accent = Color(0xFF8B5CF6)
private val Mint = Color(0xFF8CE6C3)
private val Muted = Color(0xFFA1A1AA)
private val MainText = Color(0xFFFAFAFA)

// Needs res/font/poppins_regular.ttf and res/font/poppins_semibold.ttf.
// If those files are missing, change this to: private val Poppins = FontFamily.Default
private val Poppins = FontFamily(
    Font(R.font.poppins_regular, FontWeight.Normal),
    Font(R.font.poppins_semibold, FontWeight.SemiBold)
)

private fun backgroundColor(isDark: Boolean) = if (isDark) Night else Color(0xFFF3F4F6)
private fun surfaceColor(isDark: Boolean) = if (isDark) Panel else Color.White
private fun raisedColor(isDark: Boolean) = if (isDark) PanelRaised else Color(0xFFE5E7EB)
private fun accentColor(isDark: Boolean) = if (isDark) Accent else Color(0xFF7C3AED)
private fun mintColor(isDark: Boolean) = if (isDark) Mint else Color(0xFF16865D)
private fun mutedColor(isDark: Boolean) = if (isDark) Muted else Color(0xFF666678)
private fun mainTextColor(isDark: Boolean) = if (isDark) MainText else Color(0xFF1B1B27)

// Popup colors are fixed light values so every popup has a white background.
private val PopupBackground = Color.White
private val PopupText = Color(0xFF1B1B27)
private val PopupMuted = Color(0xFF666678)
private val PopupAccent = Color(0xFF7C3AED)
private val PopupChip = Color(0xFFF3F4F6)
private val PopupField = Color(0xFFF3F4F6)
private val PopupError = Color(0xFFDC2626)

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val themePrefs = getSharedPreferences("strictfocus_preferences", MODE_PRIVATE)

        setContent {
            var isDarkTheme by remember {
                mutableStateOf(themePrefs.getBoolean("dark_theme", true))
            }

            StrictFocusTheme(isDark = isDarkTheme) {
                Dashboard(
                    activity = this,
                    isDark = isDarkTheme,
                    onThemeToggle = {
                        isDarkTheme = !isDarkTheme
                        themePrefs.edit().putBoolean("dark_theme", isDarkTheme).apply()
                    }
                )
            }
        }
    }
}

@Composable
private fun StrictFocusTheme(isDark: Boolean, content: @Composable () -> Unit) {
    val colors = if (isDark) {
        darkColorScheme(
            primary = Accent,
            onPrimary = Color(0xFF171326),
            secondary = Mint,
            background = Night,
            surface = Panel,
            onSurface = MainText,
            onSurfaceVariant = Muted
        )
    } else {
        lightColorScheme(
            primary = Color(0xFFE11D48),
            onPrimary = Color.White,
            secondary = Color(0xFF16865D),
            background = Color(0xFFFFF4F5),
            surface = Color.White,
            onSurface = Color(0xFF1B1B27),
            onSurfaceVariant = Color(0xFF666678)
        )
    }

    MaterialTheme(
        colorScheme = colors,
        typography = Typography(
            displayLarge = MaterialTheme.typography.displayLarge.copy(fontFamily = Poppins, fontWeight = FontWeight.Bold),
            headlineLarge = MaterialTheme.typography.headlineLarge.copy(fontFamily = Poppins, fontWeight = FontWeight.Bold),
            headlineMedium = MaterialTheme.typography.headlineMedium.copy(fontFamily = Poppins, fontWeight = FontWeight.SemiBold),
            titleLarge = MaterialTheme.typography.titleLarge.copy(fontFamily = Poppins, fontWeight = FontWeight.SemiBold),
            titleMedium = MaterialTheme.typography.titleMedium.copy(fontFamily = Poppins, fontWeight = FontWeight.Medium),
            bodyLarge = MaterialTheme.typography.bodyLarge.copy(fontFamily = Poppins),
            bodyMedium = MaterialTheme.typography.bodyMedium.copy(fontFamily = Poppins),
            bodySmall = MaterialTheme.typography.bodySmall.copy(fontFamily = Poppins)
        ),
        content = content
    )
}

@Composable
private fun Dashboard(
    activity: MainActivity,
    isDark: Boolean,
    onThemeToggle: () -> Unit
) {
    val context = activity
    val scope = rememberCoroutineScope()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)

    fun toast(message: String) {
        android.widget.Toast.makeText(context, message, android.widget.Toast.LENGTH_LONG).show()
    }

    val appSelectionPrefs = remember {
        context.getSharedPreferences("strictfocus_app_selection", Context.MODE_PRIVATE)
    }
    // Multi-select: every checked app is blocked when Start is pressed.
    var selected by remember {
        mutableStateOf(appSelectionPrefs.getStringSet("selected_packages", null)?.toSet() ?: emptySet())
    }
    LaunchedEffect(selected) {
        appSelectionPrefs.edit().putStringSet("selected_packages", selected.toSet()).apply()
    }

    val favoritePrefs = remember {
        context.getSharedPreferences("strictfocus_favorites", Context.MODE_PRIVATE)
    }
    var favorites by remember {
        mutableStateOf(favoritePrefs.getStringSet("favorite_packages", emptySet())?.toSet() ?: emptySet())
    }
    LaunchedEffect(favorites) {
        favoritePrefs.edit().putStringSet("favorite_packages", favorites.toSet()).apply()
    }

    var appFilter by remember { mutableIntStateOf(0) } // 0 = all, 1 = favorites
    var durationMinutes by remember { mutableIntStateOf(120) }
    var selectedTab by remember {
        mutableIntStateOf(
            if (activity.intent.getBooleanExtra("open_apps_list", false)) 1 else 0
        )
    }
    var apps by remember { mutableStateOf(emptyList<InstalledApp>()) }
    var rules by remember { mutableStateOf(emptyList<BlockRule>()) }
    var search by remember { mutableStateOf("") }
    var accessibilityEnabled by remember { mutableStateOf(false) }
    var overlayEnabled by remember { mutableStateOf(false) }
    var adminEnabled by remember { mutableStateOf(false) }
    var deviceOwner by remember { mutableStateOf(false) }

    // Schedule popup state
    var pendingApp by remember { mutableStateOf<InstalledApp?>(null) }
    var scheduleStart by remember { mutableLongStateOf(0L) }
    var scheduleEnd by remember { mutableLongStateOf(0L) }

    // Ticks every second so blocks switch on and off on time.
    var nowMillis by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            nowMillis = System.currentTimeMillis()
            delay(1000)
        }
    }
    val liveRules = rules.filter { !it.isExpiredAt(nowMillis) }
    val activeRules = liveRules.filter { it.isActiveAt(nowMillis) }
    val blockedPackages = liveRules.map { it.packageName }.toSet()

    fun refreshPermissions() {
        accessibilityEnabled = isAccessibilityEnabled(context)
        overlayEnabled = Settings.canDrawOverlays(context)
        val dpm = context.getSystemService(DevicePolicyManager::class.java)
        adminEnabled = dpm.isAdminActive(ComponentName(context, FocusDeviceAdminReceiver::class.java))
        deviceOwner = dpm.isDeviceOwnerApp(context.packageName)
    }

    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) refreshPermissions()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(Unit) {
        refreshPermissions()

        apps = withContext(Dispatchers.IO) {
            context.packageManager
                .getInstalledApplications(0)
                .filter { appInfo ->
                    appInfo.packageName != context.packageName &&
                            (appInfo.flags and ApplicationInfo.FLAG_SYSTEM == 0)
                }
                .map { appInfo ->
                    InstalledApp(
                        context.packageManager.getApplicationLabel(appInfo).toString(),
                        appInfo.packageName
                    )
                }
                .sortedBy { it.label.lowercase() }
        }

        StrictLockManager.rulesFlow(context).collect { rules = it }
    }

    fun openSchedule(app: InstalledApp) {
        val now = System.currentTimeMillis()
        scheduleStart = now
        scheduleEnd = now + 60 * 60_000L // default: 1 hour
        pendingApp = app
    }

    val filteredApps = apps.filter { app ->
        (appFilter == 0 || app.packageName in favorites) &&
                (app.label.contains(search, ignoreCase = true) ||
                        app.packageName.contains(search, ignoreCase = true))
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        scrimColor = Color.Black.copy(alpha = 0.42f),
        drawerContent = {
            ModalDrawerSheet(
                drawerContainerColor = surfaceColor(isDark),
                drawerShape = RoundedCornerShape(topEnd = 26.dp, bottomEnd = 26.dp),
                modifier = Modifier.fillMaxWidth(0.84f).widthIn(max = 340.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxHeight().padding(horizontal = 20.dp, vertical = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(18.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                "StrictFocus", color = mainTextColor(isDark),
                                fontSize = 20.sp, fontWeight = FontWeight.Bold, fontFamily = Poppins
                            )
                            Text(
                                "Focus settings", color = mutedColor(isDark),
                                fontSize = 12.sp, fontFamily = Poppins
                            )
                        }
                        Surface(
                            onClick = onThemeToggle,
                            shape = RoundedCornerShape(50),
                            color = raisedColor(isDark),
                            border = BorderStroke(1.dp, accentColor(isDark).copy(alpha = .35f))
                        ) {
                            Icon(
                                imageVector = if (isDark) Icons.Filled.LightMode else Icons.Filled.DarkMode,
                                contentDescription = "Switch theme",
                                tint = accentColor(isDark),
                                modifier = Modifier.padding(horizontal = 15.dp, vertical = 11.dp).size(22.dp)
                            )
                        }
                    }
                    HorizontalDivider(color = accentColor(isDark).copy(alpha = .22f))
                    SectionHeading(isDark, "Permissions", "Manage access needed for focus mode")
                    PermissionRow(isDark, "Accessibility service", accessibilityEnabled,
                        "Needed to detect blocked apps") {
                        context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                    }
                    PermissionRow(isDark, "Display over other apps", overlayEnabled,
                        "Allow StrictFocus to draw on top") {
                        context.startActivity(
                            Intent(
                                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                Uri.parse("package:${context.packageName}")
                            )
                        )
                    }
                    PermissionRow(isDark, "Device admin", adminEnabled,
                        "Optional additional device protection") {
                        context.startActivity(
                            Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN)
                                .putExtra(
                                    DevicePolicyManager.EXTRA_DEVICE_ADMIN,
                                    ComponentName(context, FocusDeviceAdminReceiver::class.java)
                                )
                        )
                    }
                    Spacer(Modifier.weight(1f))
                    Text(
                        if (isDark) "Dark • Red theme" else "Pale red • Red theme",
                        color = mutedColor(isDark), fontSize = 11.sp, fontFamily = Poppins
                    )
                }
            }
        }
    ) {
        Scaffold(containerColor = backgroundColor(isDark)) { padding ->

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 18.dp),
                contentPadding = PaddingValues(top = 22.dp, bottom = 28.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {

                // Header
                item {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        TextButton(
                            onClick = { scope.launch { drawerState.open() } },
                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp)
                        ) {
                            Icon(
                                Icons.Filled.Menu,
                                contentDescription = "Open menu",
                                tint = accentColor(isDark),
                                modifier = Modifier.size(26.dp)
                            )
                        }

                        Spacer(Modifier.width(8.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "StrictFocus", color = mainTextColor(isDark),
                                fontSize = 25.sp, fontWeight = FontWeight.Bold, fontFamily = Poppins
                            )
                            Text(
                                "Make time for what matters.", color = mutedColor(isDark),
                                fontSize = 13.sp, fontFamily = Poppins
                            )
                        }

                        val isActive = activeRules.isNotEmpty()
                        Box(
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(
                                    if (isActive) mintColor(isDark).copy(alpha = 0.16f)
                                    else raisedColor(isDark)
                                )
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Text(
                                if (isActive) "● ACTIVE" else "○ READY",
                                color = if (isActive) mintColor(isDark) else mutedColor(isDark),
                                fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = Poppins
                            )
                        }
                    }
                }

                // Accessibility warning
                if (!accessibilityEnabled) {
                    item {
                        Surface(
                            onClick = { context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) },
                            shape = RoundedCornerShape(18.dp),
                            color = Color(0xFFFFC27A).copy(alpha = 0.18f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Filled.Warning,
                                    contentDescription = null,
                                    tint = Color(0xFFB45309),
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(Modifier.width(10.dp))
                                Text(
                                    "Accessibility service is off, so blocks won't work. Tap to turn it on.",
                                    color = mainTextColor(isDark), fontSize = 13.sp, fontFamily = Poppins
                                )
                            }
                        }
                    }
                }

                // Overview card
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = surfaceColor(isDark)),
                        shape = RoundedCornerShape(24.dp)
                    ) {
                        Column(modifier = Modifier.fillMaxWidth().padding(20.dp)) {
                            Text(
                                "YOUR FOCUS", color = accentColor(isDark),
                                fontSize = 11.sp, fontWeight = FontWeight.Bold,
                                letterSpacing = 1.5.sp, fontFamily = Poppins
                            )
                            Spacer(Modifier.height(10.dp))
                            Text(
                                if (activeRules.isNotEmpty()) "Stay in your zone." else "Ready to focus?",
                                color = mainTextColor(isDark),
                                fontSize = 23.sp, fontWeight = FontWeight.Bold, fontFamily = Poppins
                            )
                            Spacer(Modifier.height(6.dp))
                            Text(
                                if (activeRules.isNotEmpty())
                                    "${activeRules.map { it.packageName }.distinct().size} apps blocked · " +
                                            "${formatDuration(activeRules.maxOf { it.endMillis } - nowMillis)} left. " +
                                            "Blocks can't be removed early."
                                else
                                    "Check apps and press Start, or double-tap an app to schedule a block.",
                                color = mutedColor(isDark),
                                fontSize = 13.sp, lineHeight = 19.sp, fontFamily = Poppins
                            )
                        }
                    }
                }

                // Tabs
                item {
                    FocusTabSwitcher(
                        isDark = isDark,
                        selectedTab = selectedTab,
                        onSelectTab = { selectedTab = it }
                    )
                }

                if (selectedTab == 0) {
                    item {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            SectionHeading(isDark, "Choose apps", "${selected.size} selected")
                            Spacer(Modifier.weight(1f))
                            Text(
                                if (deviceOwner) "Device Owner" else "Standard mode",
                                color = mutedColor(isDark), fontSize = 11.sp, fontFamily = Poppins
                            )
                        }
                    }

                    // Quick selection chips
                    item {
                        val visiblePackages = filteredApps.map { it.packageName }.toSet()
                        val favoritePackages = apps
                            .filter { it.packageName in favorites }
                            .map { it.packageName }
                            .toSet()
                        val allVisibleSelected = visiblePackages.isNotEmpty() && visiblePackages.all { it in selected }
                        val allFavoritesSelected = favoritePackages.isNotEmpty() && favoritePackages.all { it in selected }

                        Row(
                            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            SelectionActionChip(
                                label = if (allVisibleSelected) "Deselect visible" else "Select visible",
                                selected = allVisibleSelected,
                                enabled = visiblePackages.isNotEmpty(),
                                isDark = isDark,
                                onClick = {
                                    selected = if (allVisibleSelected) selected - visiblePackages
                                    else selected + visiblePackages
                                }
                            )
                            SelectionActionChip(
                                label = if (allFavoritesSelected) "Deselect favorites" else "Select favorites",
                                selected = allFavoritesSelected,
                                enabled = favoritePackages.isNotEmpty(),
                                isDark = isDark,
                                onClick = {
                                    selected = if (allFavoritesSelected) selected - favoritePackages
                                    else selected + favoritePackages
                                }
                            )
                            SelectionActionChip(
                                label = "Clear selection",
                                selected = false,
                                enabled = selected.isNotEmpty(),
                                isDark = isDark,
                                onClick = { selected = emptySet() },
                                secondary = true
                            )
                            Surface(shape = RoundedCornerShape(50), color = raisedColor(isDark)) {
                                Text(
                                    "${filteredApps.size} apps",
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
                                    color = mutedColor(isDark), fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium, fontFamily = Poppins
                                )
                            }
                        }
                    }

                    // App filters
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                onClick = { appFilter = 0 },
                                shape = RoundedCornerShape(50),
                                color = if (appFilter == 0) accentColor(isDark) else surfaceColor(isDark),
                                border = if (appFilter == 0) null else BorderStroke(1.dp, raisedColor(isDark))
                            ) {
                                Text(
                                    "All apps  ${apps.size}",
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                                    color = if (appFilter == 0) Color.White else mainTextColor(isDark),
                                    fontSize = 12.sp, fontWeight = FontWeight.SemiBold, fontFamily = Poppins
                                )
                            }
                            Surface(
                                onClick = { appFilter = 1 },
                                shape = RoundedCornerShape(50),
                                color = if (appFilter == 1) accentColor(isDark) else surfaceColor(isDark),
                                border = if (appFilter == 1) null else BorderStroke(1.dp, raisedColor(isDark))
                            ) {
                                Text(
                                    "★ Favorites  ${favorites.size}",
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                                    color = if (appFilter == 1) Color.White else mainTextColor(isDark),
                                    fontSize = 12.sp, fontWeight = FontWeight.SemiBold, fontFamily = Poppins
                                )
                            }
                            Spacer(Modifier.weight(1f))
                        }
                    }

                    // Search
                    item {
                        OutlinedTextField(
                            value = search,
                            onValueChange = { search = it },
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = {
                                Text("Search installed apps…", color = mutedColor(isDark), fontFamily = Poppins)
                            },
                            leadingIcon = {
                                Icon(Icons.Filled.Search, contentDescription = null, tint = accentColor(isDark))
                            },
                            trailingIcon = {
                                if (search.isNotBlank()) {
                                    TextButton(onClick = { search = "" }) {
                                        Icon(Icons.Filled.Close, contentDescription = "Clear search", tint = mutedColor(isDark))
                                    }
                                }
                            },
                            singleLine = true,
                            shape = RoundedCornerShape(16.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = accentColor(isDark),
                                unfocusedBorderColor = raisedColor(isDark),
                                focusedTextColor = mainTextColor(isDark),
                                unfocusedTextColor = mainTextColor(isDark),
                                cursorColor = accentColor(isDark)
                            )
                        )
                    }

                    // App list
                    if (filteredApps.isEmpty()) {
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = surfaceColor(isDark)),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Column(
                                    modifier = Modifier.fillMaxWidth().padding(20.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        if (appFilter == 1) "No favorite apps yet" else "No apps found",
                                        color = mainTextColor(isDark),
                                        fontWeight = FontWeight.SemiBold, fontFamily = Poppins
                                    )
                                    Spacer(Modifier.height(4.dp))
                                    Text(
                                        if (appFilter == 1 && search.isBlank())
                                            "Tap the ☆ beside an app to add it to Favorites."
                                        else "Try another app name or package name.",
                                        color = mutedColor(isDark), fontSize = 12.sp, fontFamily = Poppins
                                    )
                                    TextButton(onClick = { search = ""; appFilter = 0 }) {
                                        Text("Show all apps", color = accentColor(isDark), fontFamily = Poppins)
                                    }
                                }
                            }
                        }
                    } else {
                        items(filteredApps, key = { it.packageName }) { app ->
                            val isSelected = app.packageName in selected
                            val appRules = liveRules.filter { it.packageName == app.packageName }
                            val activeEnd = appRules.filter { it.isActiveAt(nowMillis) }
                                .maxOfOrNull { it.endMillis }
                            val nextStart = appRules.filter { it.startMillis > nowMillis }
                                .minOfOrNull { it.startMillis }
                            val isBlocked = appRules.isNotEmpty()

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(
                                        if (isSelected)
                                            accentColor(isDark).copy(alpha = if (isDark) 0.13f else 0.08f)
                                        else surfaceColor(isDark)
                                    )
                                    .border(
                                        width = if (isSelected) 1.dp else 0.dp,
                                        color = if (isSelected) accentColor(isDark).copy(alpha = 0.65f)
                                        else Color.Transparent,
                                        shape = RoundedCornerShape(16.dp)
                                    )
                                    .alpha(if (isBlocked) 0.6f else 1f)
                                    .combinedClickable(
                                        // Blocked apps can't be selected or rescheduled from here.
                                        enabled = !isBlocked,
                                        // Single tap toggles the selection. Double tap opens the schedule popup.
                                        onClick = {
                                            selected = if (isSelected) selected - app.packageName
                                            else selected + app.packageName
                                        },
                                        onDoubleClick = { openSchedule(app) }
                                    )
                                    .padding(horizontal = 14.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(raisedColor(isDark)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    val appIcon by produceState<ImageBitmap?>(
                                        initialValue = null,
                                        key1 = app.packageName
                                    ) {
                                        value = withContext(Dispatchers.IO) {
                                            try {
                                                context.packageManager
                                                    .getApplicationIcon(app.packageName)
                                                    .toBitmap(width = 72, height = 72)
                                                    .asImageBitmap()
                                            } catch (_: Exception) {
                                                null
                                            }
                                        }
                                    }
                                    if (appIcon != null) {
                                        Image(
                                            painter = BitmapPainter(appIcon!!),
                                            contentDescription = "${app.label} icon",
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .padding(3.dp)
                                                .clip(RoundedCornerShape(8.dp))
                                        )
                                    } else {
                                        Text(
                                            app.label.take(1).uppercase(),
                                            color = accentColor(isDark),
                                            fontWeight = FontWeight.Bold, fontFamily = Poppins
                                        )
                                    }
                                }

                                Spacer(Modifier.width(12.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        app.label, color = mainTextColor(isDark),
                                        fontWeight = FontWeight.SemiBold, fontSize = 14.sp, fontFamily = Poppins
                                    )
                                    Text(
                                        app.packageName, color = mutedColor(isDark),
                                        fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis,
                                        fontFamily = Poppins
                                    )
                                    when {
                                        activeEnd != null -> Text(
                                            "Blocked now · until ${formatTime(activeEnd)}",
                                            color = mintColor(isDark), fontSize = 11.sp, fontFamily = Poppins
                                        )
                                        nextStart != null -> Text(
                                            "Scheduled · starts ${formatTime(nextStart)}",
                                            color = accentColor(isDark), fontSize = 11.sp, fontFamily = Poppins
                                        )
                                        isSelected -> Text(
                                            "Selected. Blocks when you press Start.",
                                            color = accentColor(isDark), fontSize = 11.sp, fontFamily = Poppins
                                        )
                                    }
                                }

                                TextButton(
                                    onClick = {
                                        favorites = if (app.packageName in favorites)
                                            favorites - app.packageName
                                        else favorites + app.packageName
                                    },
                                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 4.dp)
                                ) {
                                    Icon(
                                        imageVector = if (app.packageName in favorites) Icons.Filled.Star
                                        else Icons.Outlined.StarBorder,
                                        contentDescription = "Favorite",
                                        tint = if (app.packageName in favorites) Color(0xFFFFC857)
                                        else mutedColor(isDark),
                                        modifier = Modifier.size(24.dp)
                                    )
                                }

                                Checkbox(
                                    checked = isSelected,
                                    enabled = !isBlocked,
                                    onCheckedChange = { checked ->
                                        selected = if (checked) selected + app.packageName
                                        else selected - app.packageName
                                    },
                                    colors = CheckboxDefaults.colors(
                                        checkedColor = accentColor(isDark),
                                        uncheckedColor = mutedColor(isDark),
                                        checkmarkColor = Color.White
                                    )
                                )
                            }
                        }
                    }

                    // Duration
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = surfaceColor(isDark)),
                            shape = RoundedCornerShape(22.dp)
                        ) {
                            Column(modifier = Modifier.fillMaxWidth().padding(18.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        "Focus duration", color = mainTextColor(isDark),
                                        fontWeight = FontWeight.SemiBold, fontSize = 16.sp, fontFamily = Poppins
                                    )
                                    Spacer(Modifier.weight(1f))
                                    Text(
                                        formatDuration(durationMinutes * 60_000L), color = accentColor(isDark),
                                        fontWeight = FontWeight.Bold, fontSize = 16.sp, fontFamily = Poppins
                                    )
                                }

                                Slider(
                                    value = durationMinutes.toFloat(),
                                    onValueChange = { durationMinutes = it.toInt().coerceAtLeast(1) },
                                    valueRange = 1f..240f,
                                    steps = 238,
                                    colors = SliderDefaults.colors(
                                        thumbColor = accentColor(isDark),
                                        activeTrackColor = accentColor(isDark),
                                        inactiveTrackColor = raisedColor(isDark)
                                    )
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    listOf(25, 50, 90, 120, 180).forEach { preset ->
                                        val isPresetSelected = durationMinutes == preset
                                        Surface(
                                            onClick = { durationMinutes = preset },
                                            shape = RoundedCornerShape(50),
                                            color = if (isPresetSelected) accentColor(isDark) else raisedColor(isDark)
                                        ) {
                                            Text(
                                                text = when (preset) {
                                                    25 -> "25 min"
                                                    50 -> "50 min"
                                                    90 -> "90 min"
                                                    120 -> "2 hours"
                                                    else -> "3 hours"
                                                },
                                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp),
                                                color = if (isPresetSelected) Color.White else mainTextColor(isDark),
                                                fontSize = 12.sp, fontWeight = FontWeight.SemiBold, fontFamily = Poppins
                                            )
                                        }
                                    }
                                }

                                Spacer(Modifier.height(8.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("1 min", color = mutedColor(isDark), fontSize = 11.sp, fontFamily = Poppins)
                                    Text("4 hours", color = mutedColor(isDark), fontSize = 11.sp, fontFamily = Poppins)
                                }
                            }
                        }
                    }

                    // Start button
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                enabled = selected.isNotEmpty(),
                                onClick = {
                                    if (!accessibilityEnabled) {
                                        context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                                        toast("Enable StrictFocus Accessibility Service, then return here.")
                                    } else {
                                        scope.launch {
                                            val start = System.currentTimeMillis()
                                            val end = start + durationMinutes * 60_000L
                                            runCatching {
                                                selected.forEach { pkg ->
                                                    StrictLockManager.addRule(context, pkg, start, end)
                                                }
                                            }.onSuccess {
                                                context.startActivity(Intent(context, StrictBlockActivity::class.java))
                                            }.onFailure {
                                                toast(it.message ?: "Could not start the focus session.")
                                            }
                                        }
                                    }
                                },
                                modifier = Modifier.fillMaxWidth().height(58.dp),
                                shape = RoundedCornerShape(18.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = accentColor(isDark),
                                    contentColor = Color.White,
                                    disabledContainerColor = raisedColor(isDark),
                                    disabledContentColor = mutedColor(isDark)
                                )
                            ) {
                                Text(
                                    when {
                                        selected.isEmpty() -> "SELECT APPS TO FOCUS"
                                        activeRules.isNotEmpty() -> "ADD TO ACTIVE FOCUS LOCK"
                                        else -> "START FOCUS LOCK"
                                    },
                                    color = Color.White,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 14.sp,
                                    letterSpacing = 0.8.sp,
                                    fontFamily = Poppins
                                )
                            }

                            if (!accessibilityEnabled) {
                                Text(
                                    "Tap the button to open Accessibility Settings and turn on StrictFocus Service.",
                                    color = mutedColor(isDark), fontSize = 12.sp, fontFamily = Poppins
                                )
                            }
                        }
                    }

                    item {
                        Text(
                            "Focus restrictions are best-effort on standard Android devices. System settings may still stop or disable the service.",
                            color = mutedColor(isDark), fontSize = 11.sp,
                            lineHeight = 16.sp, fontFamily = Poppins
                        )
                    }

                } else {
                    // Blocked apps tab (original layout)
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            SectionHeading(
                                isDark,
                                "Blocked apps",
                                if (activeRules.isNotEmpty()) "These apps are restricted during your focus session"
                                else "Your blocked apps appear here when a focus session is active"
                            )

                            if (activeRules.isEmpty()) {
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = surfaceColor(isDark)),
                                    shape = RoundedCornerShape(22.dp)
                                ) {
                                    Column(
                                        modifier = Modifier.fillMaxWidth().padding(22.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Icon(
                                            Icons.Filled.Block,
                                            contentDescription = null,
                                            tint = accentColor(isDark),
                                            modifier = Modifier.size(40.dp)
                                        )
                                        Spacer(Modifier.height(8.dp))
                                        Text(
                                            "No active blocked apps",
                                            color = mainTextColor(isDark),
                                            fontWeight = FontWeight.SemiBold, fontSize = 16.sp, fontFamily = Poppins
                                        )
                                        Text(
                                            "Start a focus session to see the apps and remaining time here.",
                                            color = mutedColor(isDark), fontSize = 12.sp, fontFamily = Poppins
                                        )
                                    }
                                }
                            } else {
                                val blockedNow = activeRules
                                    .groupBy { it.packageName }
                                    .toList()
                                    .sortedBy { (packageName, _) ->
                                        apps.firstOrNull { it.packageName == packageName }?.label ?: packageName
                                    }

                                blockedNow.forEach { (packageName, packageRules) ->
                                    val appLabel = apps.firstOrNull { it.packageName == packageName }?.label
                                        ?: packageName
                                    val endMillis = packageRules.maxOf { it.endMillis }
                                    val remaining = (endMillis - nowMillis).coerceAtLeast(0L)
                                    var showClockDialog by remember(packageName) { mutableStateOf(false) }

                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(16.dp))
                                            .background(surfaceColor(isDark))
                                            .padding(horizontal = 14.dp, vertical = 12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(38.dp)
                                                .clip(RoundedCornerShape(12.dp))
                                                .background(raisedColor(isDark)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            val appIcon = remember(packageName) {
                                                try {
                                                    context.packageManager.getApplicationIcon(packageName)
                                                } catch (e: Exception) {
                                                    null
                                                }
                                            }
                                            if (appIcon != null) {
                                                Image(
                                                    painter = BitmapPainter(appIcon.toBitmap(72, 72).asImageBitmap()),
                                                    contentDescription = "$appLabel icon",
                                                    modifier = Modifier.fillMaxSize().padding(3.dp)
                                                        .clip(RoundedCornerShape(8.dp))
                                                )
                                            } else {
                                                Text(
                                                    appLabel.take(1).uppercase(),
                                                    color = accentColor(isDark), fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }

                                        Spacer(Modifier.width(12.dp))

                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                appLabel, color = mainTextColor(isDark),
                                                fontWeight = FontWeight.SemiBold, fontSize = 14.sp, fontFamily = Poppins
                                            )
                                            Text(
                                                packageName, color = mutedColor(isDark),
                                                fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis,
                                                fontFamily = Poppins
                                            )
                                            Text(
                                                "Blocked now · until ${formatTime(endMillis)}",
                                                color = mintColor(isDark), fontSize = 11.sp, fontFamily = Poppins
                                            )
                                        }

                                        // Timer: tap to open the clock popup
                                        Surface(
                                            onClick = { showClockDialog = true },
                                            shape = RoundedCornerShape(50),
                                            color = raisedColor(isDark)
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                Icon(
                                                    Icons.Filled.Timer,
                                                    contentDescription = null,
                                                    tint = accentColor(isDark),
                                                    modifier = Modifier.size(16.dp)
                                                )
                                                Text(
                                                    formatDuration(remaining), color = accentColor(isDark),
                                                    fontSize = 12.sp, fontWeight = FontWeight.Bold, fontFamily = Poppins
                                                )
                                            }
                                        }
                                    }

                                    if (showClockDialog) {
                                        SessionClockDialog(
                                            appLabel = appLabel,
                                            endMillis = endMillis,
                                            isDark = isDark,
                                            onDismiss = { showClockDialog = false }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Schedule popup (white background)
            pendingApp?.let { app ->
                ScheduleDialog(
                    app = app,
                    startMillis = scheduleStart,
                    endMillis = scheduleEnd,
                    onStartChange = { scheduleStart = it },
                    onEndChange = { scheduleEnd = it },
                    onDismiss = { pendingApp = null },
                    onConfirm = {
                        scope.launch {
                            runCatching {
                                StrictLockManager.addRule(context, app.packageName, scheduleStart, scheduleEnd)
                            }.onSuccess {
                                toast("${app.label} is blocked")
                                selected = selected - app.packageName
                                pendingApp = null
                                selectedTab = 1
                            }.onFailure {
                                toast(it.message ?: "Could not save the block.")
                            }
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun ScheduleDialog(
    app: InstalledApp,
    startMillis: Long,
    endMillis: Long,
    onStartChange: (Long) -> Unit,
    onEndChange: (Long) -> Unit,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    val context = LocalContext.current
    val durationMs = endMillis - startMillis

    val error: String? = when {
        durationMs < 60_000L -> "The end must be after the start."
        endMillis <= System.currentTimeMillis() -> "The end time has already passed."
        durationMs > 86_400_000L -> "A block can be up to 24 hours."
        else -> null
    }

    fun pickStartDate() {
        val c = Calendar.getInstance().apply { timeInMillis = startMillis }
        DatePickerDialog(context, { _, y, m, d ->
            onStartChange(withDate(startMillis, y, m, d))
        }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).show()
    }

    fun pickStartTime() {
        val c = Calendar.getInstance().apply { timeInMillis = startMillis }
        TimePickerDialog(context, { _, h, min ->
            onStartChange(withTime(startMillis, h, min))
        }, c.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE), false).show()
    }

    fun pickEndDate() {
        val c = Calendar.getInstance().apply { timeInMillis = endMillis }
        DatePickerDialog(context, { _, y, m, d ->
            onEndChange(withDate(endMillis, y, m, d))
        }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).show()
    }

    fun pickEndTime() {
        val c = Calendar.getInstance().apply { timeInMillis = endMillis }
        TimePickerDialog(context, { _, h, min ->
            onEndChange(withTime(endMillis, h, min))
        }, c.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE), false).show()
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = PopupBackground,
            tonalElevation = 0.dp
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    "SCHEDULE A BLOCK", color = PopupAccent,
                    fontSize = 11.sp, fontWeight = FontWeight.Bold,
                    letterSpacing = 1.5.sp, fontFamily = Poppins
                )
                Text(
                    app.label, color = PopupText,
                    fontSize = 22.sp, fontWeight = FontWeight.Bold, fontFamily = Poppins,
                    maxLines = 2, overflow = TextOverflow.Ellipsis
                )

                // Quick durations
                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(30 to "30 min", 60 to "1 hour", 120 to "2 hours", 240 to "4 hours").forEach { (mins, label) ->
                        val chosen = durationMs == mins * 60_000L
                        Surface(
                            onClick = { onEndChange(startMillis + mins * 60_000L) },
                            shape = RoundedCornerShape(50),
                            color = if (chosen) PopupAccent else PopupChip
                        ) {
                            Text(
                                label,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp),
                                color = if (chosen) Color.White else PopupText,
                                fontSize = 12.sp, fontWeight = FontWeight.SemiBold, fontFamily = Poppins
                            )
                        }
                    }
                }

                TimeCard(
                    label = "FROM", millis = startMillis,
                    onDate = { pickStartDate() }, onTime = { pickStartTime() }
                )
                TimeCard(
                    label = "TO", millis = endMillis,
                    onDate = { pickEndDate() }, onTime = { pickEndTime() }
                )

                Text(
                    error ?: "Blocks ${app.label} for ${formatDuration(durationMs)}.",
                    color = if (error == null) PopupMuted else PopupError,
                    fontSize = 13.sp, fontFamily = Poppins
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f).height(52.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = PopupText)
                    ) {
                        Text("Cancel", fontFamily = Poppins)
                    }
                    Button(
                        onClick = onConfirm,
                        enabled = error == null,
                        modifier = Modifier.weight(1f).height(52.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = PopupAccent,
                            contentColor = Color.White,
                            disabledContainerColor = PopupChip,
                            disabledContentColor = PopupMuted
                        )
                    ) {
                        Text("Done", fontWeight = FontWeight.ExtraBold, fontFamily = Poppins)
                    }
                }
            }
        }
    }
}

@Composable
private fun TimeCard(
    label: String,
    millis: Long,
    onDate: () -> Unit,
    onTime: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = PopupField
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                label, color = PopupAccent,
                fontSize = 11.sp, fontWeight = FontWeight.Bold,
                letterSpacing = 1.2.sp, fontFamily = Poppins
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Surface(
                    onClick = onDate,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    color = Color.White
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            Icons.Filled.DateRange,
                            contentDescription = null,
                            tint = PopupAccent,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            formatDate(millis),
                            color = PopupText, fontSize = 14.sp, fontFamily = Poppins
                        )
                    }
                }
                Surface(
                    onClick = onTime,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    color = Color.White
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            Icons.Filled.Schedule,
                            contentDescription = null,
                            tint = PopupAccent,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            formatTime(millis),
                            color = PopupText, fontSize = 14.sp, fontFamily = Poppins
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SessionClockDialog(
    appLabel: String,
    endMillis: Long,
    isDark: Boolean,
    onDismiss: () -> Unit
) {
    val accent = accentColor(isDark)
    val muted = mutedColor(isDark)

    // Live countdown inside the popup
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            now = System.currentTimeMillis()
            delay(1000)
        }
    }
    val remaining = (endMillis - now).coerceAtLeast(0L)

    val clock = Calendar.getInstance().apply { timeInMillis = endMillis }
    val hour = clock.get(Calendar.HOUR_OF_DAY)
    val minute = clock.get(Calendar.MINUTE)

    // The popup uses a white surface in both themes.
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(
            modifier = Modifier.fillMaxWidth(0.88f),
            shape = RoundedCornerShape(28.dp),
            color = Color.White,
            tonalElevation = 0.dp
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 26.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    "BLOCK ENDS", color = accent, fontSize = 12.sp,
                    fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp, fontFamily = Poppins
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    appLabel, color = Color(0xFF1B1B27), fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold, fontFamily = Poppins,
                    maxLines = 1, overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(10.dp))
                Text(
                    formatTime(endMillis), color = Color(0xFF18181B), fontSize = 32.sp,
                    fontWeight = FontWeight.Bold, fontFamily = Poppins
                )
                Text(
                    formatDate(endMillis), color = Color(0xFF666678), fontSize = 13.sp, fontFamily = Poppins
                )
                Spacer(Modifier.height(18.dp))
                Canvas(modifier = Modifier.size(200.dp)) {
                    val center = Offset(size.width / 2f, size.height / 2f)
                    val radius = size.minDimension * 0.44f
                    drawCircle(color = Color(0xFFFFF1F2), radius = radius, center = center)
                    drawCircle(
                        color = accent.copy(alpha = 0.30f), radius = radius, center = center,
                        style = Stroke(width = 2.dp.toPx())
                    )
                    for (i in 0 until 12) {
                        rotate(i * 30f, pivot = center) {
                            val tickStart = Offset(center.x, center.y - radius * 0.86f)
                            val tickEnd = Offset(center.x, center.y - radius * (if (i % 3 == 0) 0.72f else 0.78f))
                            drawLine(
                                color = if (i % 3 == 0) accent else muted.copy(alpha = 0.65f),
                                start = tickStart,
                                end = tickEnd,
                                strokeWidth = if (i % 3 == 0) 3.dp.toPx() else 1.5.dp.toPx()
                            )
                        }
                    }
                    val hourAngle = ((hour % 12) + minute / 60f) * 30f - 90f
                    val minuteAngle = minute * 6f - 90f
                    val hourRad = Math.toRadians(hourAngle.toDouble())
                    val minuteRad = Math.toRadians(minuteAngle.toDouble())
                    drawLine(
                        color = Color(0xFF18181B),
                        start = center,
                        end = Offset(
                            center.x + kotlin.math.cos(hourRad).toFloat() * radius * 0.48f,
                            center.y + kotlin.math.sin(hourRad).toFloat() * radius * 0.48f
                        ),
                        strokeWidth = 6.dp.toPx(),
                        cap = androidx.compose.ui.graphics.StrokeCap.Round
                    )
                    drawLine(
                        color = accent,
                        start = center,
                        end = Offset(
                            center.x + kotlin.math.cos(minuteRad).toFloat() * radius * 0.70f,
                            center.y + kotlin.math.sin(minuteRad).toFloat() * radius * 0.70f
                        ),
                        strokeWidth = 4.dp.toPx(),
                        cap = androidx.compose.ui.graphics.StrokeCap.Round
                    )
                    drawCircle(color = accent, radius = 6.dp.toPx(), center = center)
                }
                Spacer(Modifier.height(14.dp))
                Text(
                    "${formatDuration(remaining)} left", color = accent,
                    fontSize = 18.sp, fontWeight = FontWeight.Bold, fontFamily = Poppins
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    "This app stays blocked until the time above.",
                    color = Color(0xFF52525B), fontSize = 13.sp, fontFamily = Poppins,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(18.dp))
                Button(
                    onClick = onDismiss,
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = accent)
                ) {
                    Text("Got it", color = Color.White, fontWeight = FontWeight.SemiBold, fontFamily = Poppins)
                }
            }
        }
    }
}

@Composable
private fun SelectionActionChip(
    label: String,
    selected: Boolean,
    enabled: Boolean,
    isDark: Boolean,
    onClick: () -> Unit,
    secondary: Boolean = false
) {
    val chipColor = when {
        !enabled -> surfaceColor(isDark)
        selected -> accentColor(isDark).copy(alpha = if (isDark) 0.22f else 0.12f)
        else -> surfaceColor(isDark)
    }
    val borderColor = if (selected) accentColor(isDark).copy(alpha = 0.75f) else raisedColor(isDark)

    Surface(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(50),
        color = chipColor,
        border = BorderStroke(1.dp, borderColor)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 13.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = when {
                    secondary -> Icons.Filled.ClearAll
                    selected -> Icons.Filled.Check
                    else -> Icons.Filled.Add
                },
                contentDescription = null,
                tint = if (secondary) mutedColor(isDark) else accentColor(isDark),
                modifier = Modifier.size(16.dp)
            )
            Text(
                label,
                color = when {
                    !enabled -> mutedColor(isDark)
                    selected -> accentColor(isDark)
                    else -> mainTextColor(isDark)
                },
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                fontFamily = Poppins
            )
        }
    }
}

@Composable
private fun SectionHeading(isDark: Boolean, title: String, subtitle: String) {
    Column {
        Text(
            title, color = mainTextColor(isDark),
            fontSize = 17.sp, fontWeight = FontWeight.Bold, fontFamily = Poppins
        )
        Text(
            subtitle, color = mutedColor(isDark),
            fontSize = 12.sp, fontFamily = Poppins
        )
    }
}

@Composable
private fun PermissionRow(
    isDark: Boolean,
    title: String,
    enabled: Boolean,
    description: String,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        color = raisedColor(isDark),
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, accentColor(isDark).copy(alpha = if (isDark) .12f else .10f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = when (title) {
                    "Accessibility service" -> Icons.Filled.Accessibility
                    "Display over other apps" -> Icons.Filled.Layers
                    else -> Icons.Filled.AdminPanelSettings
                },
                contentDescription = null,
                tint = if (enabled) mintColor(isDark) else Color(0xFFFFC27A),
                modifier = Modifier.size(22.dp)
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    title, color = mainTextColor(isDark),
                    fontWeight = FontWeight.SemiBold, fontSize = 14.sp, fontFamily = Poppins
                )
                Text(
                    description, color = mutedColor(isDark),
                    fontSize = 11.sp, fontFamily = Poppins
                )
            }
            Text(
                if (enabled) "ON" else "SET UP",
                color = if (enabled) mintColor(isDark) else accentColor(isDark),
                fontSize = 10.sp, fontWeight = FontWeight.Bold, fontFamily = Poppins
            )
        }
    }
}

@Composable
private fun FocusTabSwitcher(
    isDark: Boolean,
    selectedTab: Int,
    onSelectTab: (Int) -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = surfaceColor(isDark),
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, if (isDark) Color(0xFF303447) else Color(0xFFE5E1EF))
    ) {
        Row(
            modifier = Modifier.padding(5.dp),
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            listOf("Focus", "Blocked apps").forEachIndexed { index, label ->
                val active = selectedTab == index
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(13.dp))
                        .background(if (active) accentColor(isDark) else Color.Transparent)
                        .clickable { onSelectTab(index) }
                        .padding(horizontal = 10.dp, vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(7.dp)
                    ) {
                        Icon(
                            imageVector = if (index == 0) Icons.Filled.CenterFocusStrong else Icons.Filled.Block,
                            contentDescription = null,
                            tint = if (active) Color.White else accentColor(isDark),
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = label,
                            color = if (active) Color.White else mainTextColor(isDark),
                            fontFamily = Poppins,
                            fontWeight = if (active) FontWeight.SemiBold else FontWeight.Normal,
                            fontSize = 13.sp
                        )
                    }
                }
            }
        }
    }
}

private fun formatTime(millis: Long): String =
    SimpleDateFormat("h:mm a", Locale.getDefault()).format(millis)

private fun formatDate(millis: Long): String =
    SimpleDateFormat("d MMM yyyy", Locale.getDefault()).format(millis)

private fun formatWindow(start: Long, end: Long): String =
    "${formatDate(start)} ${formatTime(start)} → ${formatDate(end)} ${formatTime(end)}"

private fun formatDuration(millis: Long): String {
    val totalMinutes = (millis / 60_000L).coerceAtLeast(1L)
    val hours = totalMinutes / 60
    val minutes = totalMinutes % 60
    return if (hours > 0) "${hours}h ${minutes}m" else "$minutes min"
}

private fun withDate(base: Long, year: Int, month: Int, day: Int): Long =
    Calendar.getInstance().apply {
        timeInMillis = base
        set(Calendar.YEAR, year)
        set(Calendar.MONTH, month)
        set(Calendar.DAY_OF_MONTH, day)
    }.timeInMillis

private fun withTime(base: Long, hour: Int, minute: Int): Long =
    Calendar.getInstance().apply {
        timeInMillis = base
        set(Calendar.HOUR_OF_DAY, hour)
        set(Calendar.MINUTE, minute)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis

private fun isAccessibilityEnabled(context: Context): Boolean {
    val expected = ComponentName(context, StrictFocusAccessibilityService::class.java).flattenToString()
    val enabled = Settings.Secure.getString(
        context.contentResolver,
        Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
    ) ?: return false
    return enabled.split(':').any { it.equals(expected, ignoreCase = true) }
}