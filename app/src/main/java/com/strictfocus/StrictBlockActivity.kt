
    package com.strictfocus

    import androidx.activity.ComponentActivity
    import androidx.activity.compose.setContent
    import androidx.activity.compose.BackHandler
    import android.os.Bundle
    import androidx.compose.foundation.Canvas
    import androidx.compose.foundation.layout.*
    import androidx.compose.foundation.shape.RoundedCornerShape
    import androidx.compose.material3.*
    import androidx.compose.runtime.*
    import androidx.compose.ui.Alignment
    import androidx.compose.ui.Modifier
    import androidx.compose.ui.graphics.Color
    import androidx.compose.ui.graphics.StrokeCap
    import androidx.compose.ui.text.font.Font
    import androidx.compose.ui.text.font.FontFamily
    import androidx.compose.ui.text.font.FontWeight
    import androidx.compose.ui.unit.dp
    import androidx.compose.ui.unit.sp
    import kotlinx.coroutines.delay
    import kotlinx.coroutines.flow.first

    // Dark theme colors — matching MainActivity
    private val DarkBg = Color(0xFF18181B)
    private val DarkAccent = Color(0xFF8B5CF6)
    private val DarkMint = Color(0xFF8CE6C3)
    private val DarkMuted = Color(0xFFA1A1AA)
    private val DarkTrack = Color(0xFF3F3F46)
    private val DarkText = Color(0xFFFAFAFA)

    // Light theme colors — matching MainActivity
    private val LightBg = Color(0xFFF3F4F6)
    private val LightAccent = Color(0xFF7C3AED)
    private val LightMint = Color(0xFF16865D)
    private val LightMuted = Color(0xFF666678)
    private val LightTrack = Color(0xFFE5E7EB)
    private val LightText = Color(0xFF1B1B27)

    private val LockPoppins = FontFamily(
        Font(R.font.poppins_regular, FontWeight.Normal),
        Font(R.font.poppins_semibold, FontWeight.SemiBold)
    )

    class StrictBlockActivity : ComponentActivity() {

        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)

            // Read the same theme preference used by MainActivity.
            val themePrefs = getSharedPreferences(
                "strictfocus_preferences",
                MODE_PRIVATE
            )

            val isDarkTheme = themePrefs.getBoolean("dark_theme", true)

            val backgroundColor = if (isDarkTheme) DarkBg else LightBg
            val accentColor = if (isDarkTheme) DarkAccent else LightAccent
            val mintColor = if (isDarkTheme) DarkMint else LightMint
            val mutedColor = if (isDarkTheme) DarkMuted else LightMuted
            val trackColor = if (isDarkTheme) DarkTrack else LightTrack
            val textColor = if (isDarkTheme) DarkText else LightText

            setContent {

                BackHandler(enabled = true) {
                    // Back navigation is intentionally disabled.
                }

                MaterialTheme(
                    colorScheme = if (isDarkTheme) {
                        darkColorScheme(
                            primary = accentColor,
                            secondary = mintColor,
                            background = backgroundColor,
                            surface = backgroundColor,
                            onSurface = textColor
                        )
                    } else {
                        lightColorScheme(
                            primary = accentColor,
                            secondary = mintColor,
                            background = backgroundColor,
                            surface = Color.White,
                            onSurface = textColor
                        )
                    },
                    typography = Typography(
                        displayLarge = MaterialTheme.typography.displayLarge.copy(
                            fontFamily = LockPoppins
                        ),
                        headlineLarge = MaterialTheme.typography.headlineLarge.copy(
                            fontFamily = LockPoppins
                        ),
                        titleLarge = MaterialTheme.typography.titleLarge.copy(
                            fontFamily = LockPoppins
                        ),
                        bodyLarge = MaterialTheme.typography.bodyLarge.copy(
                            fontFamily = LockPoppins
                        ),
                        bodyMedium = MaterialTheme.typography.bodyMedium.copy(
                            fontFamily = LockPoppins
                        )
                    )
                ) {

                    var remaining by remember {
                        mutableLongStateOf(0L)
                    }

                    var initial by remember {
                        mutableLongStateOf(1L)
                    }

                    LaunchedEffect(Unit) {

                        while (true) {

                            val session = StrictLockManager
                                .sessionFlow(this@StrictBlockActivity)
                                .first()

                            remaining = session.remainingMillis()

                            if (initial == 1L && remaining > 0L) {
                                initial = remaining
                            }

                            if (remaining <= 0L) {

                                StrictLockManager.clear(
                                    this@StrictBlockActivity
                                )

                                finish()
                                break
                            }

                            delay(1000)
                        }
                    }

                    val totalSeconds = remaining / 1000L

                    val hours = totalSeconds / 3600L
                    val minutes = (totalSeconds % 3600L) / 60L
                    val seconds = totalSeconds % 60L

                    val progress = (
                            remaining.toFloat() /
                                    initial.coerceAtLeast(1L)
                            ).coerceIn(0f, 1f)

                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = backgroundColor
                    ) {

                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(
                                    horizontal = 26.dp,
                                    vertical = 34.dp
                                ),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {

                            // Header
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {

                                Text(
                                    text = "STRICTFOCUS",
                                    color = accentColor,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    letterSpacing = 2.sp,
                                    fontFamily = LockPoppins
                                )

                                Spacer(Modifier.height(10.dp))

                                Text(
                                    text = "Stay in your zone.",
                                    color = textColor,
                                    fontSize = 27.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = LockPoppins
                                )

                                Text(
                                    text = "The distractions can wait.",
                                    color = mutedColor,
                                    fontSize = 13.sp,
                                    fontFamily = LockPoppins
                                )
                            }

                            // Circular countdown timer
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier.size(270.dp)
                            ) {

                                Canvas(
                                    modifier = Modifier.fillMaxSize()
                                ) {

                                    // Background circle
                                    drawArc(
                                        color = trackColor,
                                        startAngle = -90f,
                                        sweepAngle = 360f,
                                        useCenter = false,
                                        style = androidx.compose.ui.graphics.drawscope.Stroke(
                                            width = 15.dp.toPx(),
                                            cap = StrokeCap.Round
                                        )
                                    )

                                    // Progress circle
                                    drawArc(
                                        color = accentColor,
                                        startAngle = -90f,
                                        sweepAngle = 360f * progress,
                                        useCenter = false,
                                        style = androidx.compose.ui.graphics.drawscope.Stroke(
                                            width = 15.dp.toPx(),
                                            cap = StrokeCap.Round
                                        )
                                    )
                                }

                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {

                                    Text(
                                        text = "TIME REMAINING",
                                        color = mutedColor,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 1.4.sp,
                                        fontFamily = LockPoppins
                                    )

                                    Spacer(Modifier.height(12.dp))

                                    Text(
                                        text = "%02d:%02d:%02d".format(
                                            hours,
                                            minutes,
                                            seconds
                                        ),
                                        color = textColor,
                                        fontSize = 31.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = LockPoppins
                                    )

                                    Spacer(Modifier.height(8.dp))

                                    Text(
                                        text = "Focus mode active",
                                        color = mintColor,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        fontFamily = LockPoppins
                                    )
                                }
                            }

                            // Bottom section
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {

                                Text(
                                    text = "You chose to protect this time.",
                                    color = textColor,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    fontFamily = LockPoppins
                                )

                                Spacer(Modifier.height(7.dp))

                                Text(
                                    text = "Take a breath. One task at a time.",
                                    color = mutedColor,
                                    fontSize = 12.sp,
                                    fontFamily = LockPoppins
                                )

                                Spacer(Modifier.height(22.dp))

                                Button(
                                    onClick = {
                                        // Return to the existing MainActivity.
                                        // Focus session and app blocks remain active.
                                        finish()
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(54.dp),
                                    shape = RoundedCornerShape(17.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = accentColor,
                                        contentColor = Color.White
                                    )
                                ) {

                                    Text(
                                        text = "RETURN TO HOME",
                                        fontWeight = FontWeight.ExtraBold,
                                        letterSpacing = 0.7.sp,
                                        fontFamily = LockPoppins
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }