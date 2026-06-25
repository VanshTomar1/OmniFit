package com.example

import android.os.Bundle
import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlin.math.sqrt
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.background
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontWeight
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.ui.text.style.TextAlign
import com.example.ui.viewmodel.WorkoutSummary
import com.example.ui.viewmodel.InAppNotification
import com.example.ui.screens.*
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.VoltLime
import com.example.ui.theme.SportsTeal
import com.example.ui.viewmodel.FitnessViewModel
import com.example.ui.viewmodel.FitnessViewModelFactory
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.platform.LocalDensity
import kotlin.math.roundToInt

class MainActivity : ComponentActivity(), SensorEventListener {

    private val viewModel: FitnessViewModel by viewModels {
        val app = application as OmniFitApplication
        FitnessViewModelFactory(app, app.repository)
    }

    private lateinit var sensorManager: SensorManager
    private var accelerometer: Sensor? = null
    private var lastStepTime = 0L
    private val stepThreshold = 12.2f // Highly calibrated magnitude peak (normal gravity is ~9.8m/s^2)
    private var isBelowThreshold = true

    private fun createNotificationChannel() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            val name = "OmniFit Live Tracker"
            val descriptionText = "Real-time step count and metabolic active calorie notifications."
            val importance = android.app.NotificationManager.IMPORTANCE_LOW
            val channel = android.app.NotificationChannel("omnifit_steps_channel", name, importance).apply {
                description = descriptionText
            }
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun postStepsNotification(steps: Int, calories: Double) {
        lifecycleScope.launch(kotlinx.coroutines.Dispatchers.Default) {
            try {
                val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
                val calorieStr = String.format(java.util.Locale.US, "%.1f", calories)
                
                // ContentIntent to launch the MainActivity when tapped or slid
                val intent = android.content.Intent(this@MainActivity, MainActivity::class.java).apply {
                    flags = android.content.Intent.FLAG_ACTIVITY_SINGLE_TOP or android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP
                }
                val pendingIntent = android.app.PendingIntent.getActivity(
                    this@MainActivity,
                    0,
                    intent,
                    android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
                )

                val notification = androidx.core.app.NotificationCompat.Builder(this@MainActivity, "omnifit_steps_channel")
                    .setSmallIcon(android.R.drawable.ic_dialog_info)
                    .setContentTitle("🏃 OmniFit Activity Tracker")
                    .setContentText("Step Count: $steps   •   Active Energy: $calorieStr kcal")
                    .setSubText("Real-time Performance")
                    .setColor(0xFFD4FF00.toInt()) // High-contrast Volt Lime brand tint
                    .setOngoing(true) // Keep in notification panel
                    .setOnlyAlertOnce(true)
                    .setContentIntent(pendingIntent) // Critical: opening the app on swipe/slide/tap actions!
                    .setPriority(androidx.core.app.NotificationCompat.PRIORITY_LOW)
                    .build()
                notificationManager.notify(4242, notification)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun postRunNotification(seconds: Int, distanceMeters: Double, steps: Int, currentSpeed: Double) {
        try {
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
            val speedKmh = currentSpeed * 3.6
            val distStr = if (distanceMeters >= 1000.0) {
                String.format(java.util.Locale.US, "%.2f km", distanceMeters / 1000.0)
            } else {
                String.format(java.util.Locale.US, "%.0f m", distanceMeters)
            }
            val paceStr = String.format(java.util.Locale.US, "%.1f km/h", speedKmh)
            val minStr = String.format(java.util.Locale.US, "%02d:%02d", seconds / 60, seconds % 60)

            val intent = android.content.Intent(this, MainActivity::class.java).apply {
                flags = android.content.Intent.FLAG_ACTIVITY_SINGLE_TOP or android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val pendingIntent = android.app.PendingIntent.getActivity(
                this,
                0,
                intent,
                android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
            )

            val notification = androidx.core.app.NotificationCompat.Builder(this, "omnifit_steps_channel")
                .setSmallIcon(android.R.drawable.ic_media_play)
                .setContentTitle("⚽ Cardio Ride Mode Active")
                .setContentText("Duration: $minStr   •   Distance: $distStr   •   Speed: $paceStr")
                .setSubText("Running Metrics")
                .setColor(0xFFD4FF00.toInt()) // High-contrast Volt Lime brand tint
                .setStyle(androidx.core.app.NotificationCompat.BigTextStyle()
                    .setBigContentTitle("🏃 Live Run Completed Performance Logs")
                    .bigText("Active Run Tracking Overview:\n⏱️ Elapsed Duration: $minStr\n🎯 Logged Distance: $distStr\n🚶 Step Frequency: $steps steps\n⚡ Dynamic Speed Velocity: $paceStr"))
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .setContentIntent(pendingIntent) // Critical: opening the app on swipe/slide/tap actions!
                .setPriority(androidx.core.app.NotificationCompat.PRIORITY_LOW)
                .build()
            notificationManager.notify(4243, notification)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun cancelRunNotification() {
        try {
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
            notificationManager.cancel(4243)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    @OptIn(ExperimentalLayoutApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Setup sensory step counting and notification channel
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        createNotificationChannel()

        // Request POST_NOTIFICATIONS, ACTIVITY_RECOGNITION, and location permissions dynamically
        val permissionsNeeded = mutableListOf<String>()
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                permissionsNeeded.add(android.Manifest.permission.POST_NOTIFICATIONS)
            }
        }
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            if (checkSelfPermission(android.Manifest.permission.ACTIVITY_RECOGNITION) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                permissionsNeeded.add(android.Manifest.permission.ACTIVITY_RECOGNITION)
            }
        }
        if (checkSelfPermission(android.Manifest.permission.ACCESS_FINE_LOCATION) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
            permissionsNeeded.add(android.Manifest.permission.ACCESS_FINE_LOCATION)
        }
        if (checkSelfPermission(android.Manifest.permission.ACCESS_COARSE_LOCATION) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
            permissionsNeeded.add(android.Manifest.permission.ACCESS_COARSE_LOCATION)
        }
        if (permissionsNeeded.isNotEmpty()) {
            requestPermissions(permissionsNeeded.toTypedArray(), 101)
        }

        // Auto-update notification panel with conflated throttling to prevent binder IPC overload
        lifecycleScope.launch {
            var lastSteps = -1
            viewModel.todayHealthLog.collect { log ->
                if (log.stepsCount != lastSteps) {
                    lastSteps = log.stepsCount
                    postStepsNotification(log.stepsCount, log.activeCaloriesBurned)
                    // Conflated suspension: sleeps the collector so intermediate updates are skipped
                    kotlinx.coroutines.delay(4000)
                }
            }
        }

        // Live running mode updates
        lifecycleScope.launch {
            viewModel.runSeconds.collect { sec ->
                if (viewModel.isRunActive.value) {
                    postRunNotification(
                        seconds = sec,
                        distanceMeters = viewModel.runDistanceMeters.value,
                        steps = viewModel.runSteps.value,
                        currentSpeed = viewModel.runCurrentSpeed.value
                    )
                }
            }
        }

        lifecycleScope.launch {
            viewModel.isRunActive.collect { active ->
                if (!active) {
                    cancelRunNotification()
                }
            }
        }

        setContent {
            val themeSetting by viewModel.themeSetting.collectAsState()
            MyApplicationTheme(darkTheme = themeSetting != "light") {
                val isProfileLoaded by viewModel.isProfileLoaded.collectAsState()
                val isLoggedIn by viewModel.isLoggedIn.collectAsState()
                val profile by viewModel.userProfile.collectAsState(initial = null)
                val activeSessionId by viewModel.activeSessionId.collectAsState()
                val lastCompletedSummary by viewModel.lastCompletedWorkoutSummary.collectAsState()
                val showWalkthroughOverride by viewModel.showWalkthroughOverride.collectAsState()

                // Determine first-time versus second-time status using SharedPreferences
                val sharedPrefs = remember { getSharedPreferences("com.aistudio.omnifit.PREFS", android.content.Context.MODE_PRIVATE) }
                var hasOpenedBefore by remember { mutableStateOf(sharedPrefs.getBoolean("has_opened_before", false)) }

                // State to control active tab
                var selectedTab by remember { mutableStateOf(0) }

                // Modern Cinematic Video-style Startup Transition (runs on every launch)
                var cinematicSplashActive by remember { mutableStateOf(true) }

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    bottomBar = {
                        // Display bottom bar only if fully onboarded, not actively executing a workout, and splash is not active
                        if (isProfileLoaded && profile != null && activeSessionId == null && !cinematicSplashActive) {
                            NavigationBar(
                                modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars)
                            ) {
                                NavigationBarItem(
                                    selected = selectedTab == 0,
                                    onClick = { selectedTab = 0 },
                                    icon = { Icon(Icons.Default.Dashboard, contentDescription = "Dashboard") },
                                    label = { Text("Tracker", fontWeight = FontWeight.Bold) }
                                )
                                NavigationBarItem(
                                    selected = selectedTab == 1,
                                    onClick = { selectedTab = 1 },
                                    icon = { Icon(Icons.Default.FitnessCenter, contentDescription = "Train") },
                                    label = { Text("Workout", fontWeight = FontWeight.Bold) }
                                )
                                NavigationBarItem(
                                    selected = selectedTab == 2,
                                    onClick = { selectedTab = 2 },
                                    icon = { Icon(Icons.Default.Chat, contentDescription = "AI Coach") },
                                    label = { Text("Coach", fontWeight = FontWeight.Bold) }
                                )
                                NavigationBarItem(
                                    selected = selectedTab == 3,
                                    onClick = { selectedTab = 3 },
                                    icon = { Icon(Icons.Default.Person, contentDescription = "Profile") },
                                    label = { Text("Profile", fontWeight = FontWeight.Bold) }
                                )
                            }
                        }
                    }
                ) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    ) {
                        when {

                            // Stage -1: Initial loading window to prevent startup flicker glitch
                            !isProfileLoaded -> {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(MaterialTheme.colorScheme.background),
                                    contentAlignment = androidx.compose.ui.Alignment.Center
                                ) {
                                    CircularProgressIndicator(
                                        color = MaterialTheme.colorScheme.primary,
                                        strokeWidth = 3.dp
                                    )
                                }
                            }

                            // Stage 0: If profile is not set up or walkthrough is requested, load onboarding
                            (profile == null || showWalkthroughOverride) -> {
                                OnboardingScreen(
                                    viewModel = viewModel,
                                    initialHasOpenedBefore = if (showWalkthroughOverride) false else hasOpenedBefore,
                                    onIntroComplete = {
                                        sharedPrefs.edit().putBoolean("has_opened_before", true).apply()
                                        hasOpenedBefore = true
                                        if (showWalkthroughOverride) {
                                            viewModel.showWalkthroughOverride.value = false
                                        }
                                    },
                                    onComplete = {
                                        // Also guarantee we mark opened before once profile onboarding finishes
                                        sharedPrefs.edit().putBoolean("has_opened_before", true).apply()
                                        hasOpenedBefore = true
                                        if (showWalkthroughOverride) {
                                            viewModel.showWalkthroughOverride.value = false
                                        }
                                        selectedTab = 0
                                    }
                                )
                            }

                            // Stage 1: Active Workout Session intercepts UI to prevent distracted navigation
                            activeSessionId != null -> {
                                ActiveWorkoutScreen(
                                    viewModel = viewModel,
                                    onFinished = { selectedTab = 1 }
                                )
                            }

                            // Stage 2: Load relevant Navigation Tabs
                            else -> {
                                when (selectedTab) {
                                    0 -> DashboardScreen(viewModel = viewModel)
                                    1 -> WorkoutScreen(
                                        viewModel = viewModel,
                                        onStartWorkout = { schedule ->
                                            viewModel.startActiveWorkout(schedule)
                                        }
                                    )
                                    2 -> CoachScreen(viewModel = viewModel)
                                    3 -> ProfileScreen(viewModel = viewModel)
                                }
                            }
                        }
                    }
                }

                // Show Workout Summary Screen overlay if present
                lastCompletedSummary?.let { summary ->
                    WorkoutSummaryOverlay(
                        summary = summary,
                        onDismiss = { viewModel.dismissWorkoutSummary() }
                    )
                }

                // Show floating In-App notification overlay with tactile slide gesture
                val currentInAppNotification by viewModel.currentInAppNotification.collectAsState()
                currentInAppNotification?.let { notif ->
                    InAppNotificationBannerOverlay(
                        notification = notif,
                        onDismiss = { viewModel.dismissInAppNotification() },
                        onSlideToOpen = {
                            if (notif.iconType == "steps" || notif.iconType == "run") {
                                selectedTab = 0 // Tracker/Dashboard Screen
                            } else if (notif.iconType == "achievement") {
                                selectedTab = 1 // Workouts/Train Screen
                            }
                            viewModel.dismissInAppNotification()
                        }
                    )
                }

                if (cinematicSplashActive) {
                    CinematicSplashTransition(
                        onTransitionFinished = {
                            cinematicSplashActive = false
                        }
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        try {
            val hasActivityPermission = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                checkSelfPermission(android.Manifest.permission.ACTIVITY_RECOGNITION) == android.content.pm.PackageManager.PERMISSION_GRANTED
            } else {
                true
            }
            if (hasActivityPermission) {
                accelerometer?.let {
                    sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL)
                }
            } else {
                sensorManager.unregisterListener(this)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onPause() {
        super.onPause()
        try {
            sensorManager.unregisterListener(this)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event == null || event.values == null || event.values.size < 3 || event.sensor == null) return
        try {
            val hasActivityPermission = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                checkSelfPermission(android.Manifest.permission.ACTIVITY_RECOGNITION) == android.content.pm.PackageManager.PERMISSION_GRANTED
            } else {
                true
            }
            if (!hasActivityPermission) return

            if (event.sensor.type == Sensor.TYPE_ACCELEROMETER) {
                val x = event.values[0]
                val y = event.values[1]
                val z = event.values[2]

                // Calculate overall force vector magnitude
                val gForceMag = sqrt(x * x + y * y + z * z)
                val now = System.currentTimeMillis()

                // Safe threshold peak counting
                if (gForceMag > stepThreshold) {
                    if (isBelowThreshold && (now - lastStepTime > 320)) {
                        lastStepTime = now
                        isBelowThreshold = false
                        viewModel.trackPassiveSteps(1)
                    }
                } else if (gForceMag < 10.2f) {
                    isBelowThreshold = true
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
}

@Composable
fun WorkoutSummaryOverlay(
    summary: WorkoutSummary,
    onDismiss: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background.copy(alpha = 0.98f))
            .clickable(enabled = true, onClick = {}) // block touch interception to back layers
            .windowInsetsPadding(WindowInsets.safeDrawing)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(28.dp))

            // Celebration Icon Header
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .background(VoltLime.copy(alpha = 0.15f), shape = CircleShape)
                    .border(2.dp, VoltLime, shape = CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Star,
                    contentDescription = "Celebration Medal Icon",
                    tint = VoltLime,
                    modifier = Modifier.size(40.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "SESSION ACCOMPLISHED!",
                fontWeight = FontWeight.Black,
                fontSize = 12.sp,
                color = VoltLime,
                letterSpacing = 2.sp
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = summary.name,
                fontWeight = FontWeight.Black,
                fontSize = 24.sp,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Dynamic Stats Grid
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Calories
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.Bolt,
                            contentDescription = "Metabolic fire burn",
                            tint = Color(0xFFFF9800),
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = String.format(java.util.Locale.US, "%.0f kcal", summary.caloriesBurned),
                            fontWeight = FontWeight.Black,
                            fontSize = 16.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Calorie Burn",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Volume
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.FitnessCenter,
                            contentDescription = "Lift weight",
                            tint = VoltLime,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        val volText = if (summary.totalVolumeKg > 0) {
                            String.format(java.util.Locale.US, "%.0f kg", summary.totalVolumeKg)
                        } else {
                            "Bodyweight"
                        }
                        Text(
                            text = volText,
                            fontWeight = FontWeight.Black,
                            fontSize = 16.sp,
                            color = MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = "Total Load",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Time Duration
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.Timer,
                            contentDescription = "Time duration icon",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        val durationMins = summary.durationSeconds / 60
                        val durationSecs = summary.durationSeconds % 60
                        val timeStr = if (durationMins > 0) {
                            "${durationMins}m ${durationSecs}s"
                        } else {
                            "${durationSecs}s"
                        }
                        Text(
                            text = timeStr,
                            fontWeight = FontWeight.Black,
                            fontSize = 16.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Active Time",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Muscle target chip list
            if (summary.muscleGroupsWorked.isNotEmpty()) {
                Text(
                    text = "Loaded Muscle Groups",
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.wrapContentSize(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    summary.muscleGroupsWorked.forEach { group ->
                        Text(
                            text = group,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = VoltLime,
                            modifier = Modifier
                                .background(VoltLime.copy(alpha = 0.1f), shape = RoundedCornerShape(6.dp))
                                .border(1.dp, VoltLime.copy(alpha = 0.3f), shape = RoundedCornerShape(6.dp))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Adaptive Support Induction Badges
            if (summary.isPeriodMode || summary.isBeginnerPeriod) {
                Row(
                    modifier = Modifier.padding(bottom = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (summary.isPeriodMode) {
                        Text(
                            text = "🌸 Active Menstrual Comfort Tuning",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFE53935),
                            modifier = Modifier
                                .background(Color(0xFFE53935).copy(alpha = 0.08f), shape = RoundedCornerShape(8.dp))
                                .border(1.dp, Color(0xFFE53935).copy(alpha = 0.25f), shape = RoundedCornerShape(8.dp))
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                    if (summary.isBeginnerPeriod) {
                        Text(
                            text = "🔰 30-Day Joint Conditioning Active",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier
                                .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.08f), shape = RoundedCornerShape(8.dp))
                                .border(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.25f), shape = RoundedCornerShape(8.dp))
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                }
            }

            // Summary List of Exercises & Sets Completed
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Athletic Performance Logs (${summary.totalSets} completed sets)",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    if (summary.exercisesPerformed.isEmpty()) {
                        Text(
                            text = "No performance sets logged during this session.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        summary.exercisesPerformed.forEach { exName ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(16.dp)
                                        .background(VoltLime.copy(alpha = 0.15f), shape = CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = "Checked completed exercise",
                                        tint = VoltLime,
                                        modifier = Modifier.size(10.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = exName,
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))
            Spacer(modifier = Modifier.height(24.dp))

            // Action CTA button to Dismiss Summary Screen
            Button(
                onClick = onDismiss,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                colors = ButtonDefaults.buttonColors(containerColor = VoltLime),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = "AWESOME, RETURN TO FITNESS APP",
                    fontWeight = FontWeight.Black,
                    fontSize = 13.sp,
                    color = Color.Black,
                    letterSpacing = 1.sp
                )
            }
        }
    }
}

@Composable
fun InAppNotificationBannerOverlay(
    notification: InAppNotification,
    onDismiss: () -> Unit,
    onSlideToOpen: () -> Unit
) {
    var offsetX by remember { mutableStateOf(0f) }
    val density = LocalDensity.current
    val maxSwipeOffset = with(density) { 220.dp.toPx() } // Target offset to slide completely and open target context

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.statusBars)
            .padding(horizontal = 16.dp, vertical = 10.dp)
            .offset { IntOffset(offsetX.roundToInt(), 0) }
            .pointerInput(notification) {
                detectHorizontalDragGestures(
                    onDragEnd = {
                        if (offsetX > maxSwipeOffset) {
                            onSlideToOpen()
                        } else {
                            offsetX = 0f
                        }
                    },
                    onDragCancel = {
                        offsetX = 0f
                    },
                    onHorizontalDrag = { change, dragAmount ->
                        change.consume()
                        offsetX = (offsetX + dragAmount).coerceAtLeast(0f)
                    }
                )
            }
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(2.dp, VoltLime.copy(alpha = 0.6f), RoundedCornerShape(16.dp)),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(12.dp).copy(alpha = 0.98f)
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 10.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val (icon, color) = when (notification.iconType) {
                        "steps" -> Pair(Icons.Default.DirectionsRun, VoltLime)
                        "run" -> Pair(Icons.Default.Speed, MaterialTheme.colorScheme.primary)
                        "achievement" -> Pair(Icons.Default.EmojiEvents, Color(0xFFFFB300))
                        else -> Pair(Icons.Default.NotificationsActive, VoltLime)
                    }

                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .background(color.copy(alpha = 0.15f), shape = CircleShape)
                            .border(1.dp, color.copy(alpha = 0.4f), shape = CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = "Notification Icon Accent",
                            tint = color,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = notification.title,
                            fontWeight = FontWeight.Black,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = notification.message,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 16.sp
                        )
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Dismiss Banner Button",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(34.dp)
                        .background(
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            shape = RoundedCornerShape(10.dp)
                        )
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f), RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.CenterStart
                ) {
                    val progress = (offsetX / maxSwipeOffset).coerceIn(0f, 1f)
                    Text(
                        text = if (progress > 0.8f) "RELEASE TO OPEN APP" else ">>> SLIDE RIGHT TO OPEN >>>",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Black,
                        color = if (progress > 0.8f) VoltLime else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        letterSpacing = 1.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Box(
                        modifier = Modifier
                            .offset { IntOffset(offsetX.coerceAtMost(maxSwipeOffset).roundToInt(), 0) }
                            .fillMaxHeight()
                            .width(50.dp)
                            .background(VoltLime, shape = RoundedCornerShape(10.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowForward,
                            contentDescription = "Swipe Handle Drag Indicator Icon",
                            tint = Color.Black,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}

data class SplashParticle(
    val xPercent: Float,
    val yPercent: Float,
    val speed: Float,
    val size: Float,
    val alpha: Float
)

@Composable
fun CinematicSplashTransition(
    onTransitionFinished: () -> Unit
) {
    // Animation controls
    val assembleProgress = remember { Animatable(0f) }
    val boltScale = remember { Animatable(0f) }
    val flashAlpha = remember { Animatable(0f) }
    val shakeOffset = remember { Animatable(0f) }
    val textReveal = remember { Animatable(0f) }
    val textSpacingPercent = remember { Animatable(0f) }
    val transitionOut = remember { Animatable(0f) }

    var loadStatusText by remember { mutableStateOf("Initializing Core Systems...") }

    // Floating data particles
    val particles = remember {
        List(22) {
            SplashParticle(
                xPercent = (5..95).random() / 100f,
                yPercent = (0..100).random() / 100f,
                speed = (8..18).random() / 10000f,
                size = (3..7).random().toFloat(),
                alpha = (20..70).random() / 100f
            )
        }
    }

    // Tick to drive particle animation
    val infiniteTransition = rememberInfiniteTransition()
    val frameTime by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        )
    )

    // Run cinematic choreography
    LaunchedEffect(Unit) {
        // Step 1: Start converging plates (0 to 900ms)
        loadStatusText = "Connecting secure SQLite database layers..."
        assembleProgress.animateTo(
            targetValue = 1f,
            animationSpec = spring(
                dampingRatio = 0.58f,
                stiffness = Spring.StiffnessLow
            )
        )

        // Step 2: The weights crash and lock! Trigger strobe and camera shake
        loadStatusText = "Calibrating smart metric aggregators..."
        launch {
            shakeOffset.animateTo(12f, animationSpec = keyframes {
                durationMillis = 350
                0f at 0
                12f at 60
                -10f at 120
                8f at 180
                -5f at 240
                0f at 350
            })
        }
        launch {
            flashAlpha.animateTo(0.9f, animationSpec = tween(80, easing = LinearEasing))
            flashAlpha.animateTo(0f, animationSpec = tween(500, easing = EaseOutQuad))
        }

        // Step 3: Energy Ignition. The lighting bolt expands from the center with a spark
        boltScale.animateTo(
            targetValue = 1f,
            animationSpec = spring(
                dampingRatio = 0.55f,
                stiffness = Spring.StiffnessMedium
            )
        )

        // Step 4: Text elements fade in and tracking/letter spacing expands
        loadStatusText = "Warm-launching local AI Coach intelligence..."
        launch {
            textSpacingPercent.animateTo(1f, animationSpec = tween(900, easing = EaseOutCubic))
        }
        textReveal.animateTo(1f, animationSpec = tween(700, easing = EaseOutQuad))

        // Wait in fully-powered glory & load system features completely
        kotlinx.coroutines.delay(800)
        loadStatusText = "Pre-aggregating physical strain biometrics..."
        kotlinx.coroutines.delay(1000)
        loadStatusText = "Warm-booting real-time heart rate GATT sensors..."
        kotlinx.coroutines.delay(1000)
        loadStatusText = "Caching system exercises mapping..."
        kotlinx.coroutines.delay(800)
        loadStatusText = "Sensing active accessory beacons..."
        kotlinx.coroutines.delay(800)
        loadStatusText = "All systems fully synchronized!"
        kotlinx.coroutines.delay(600)

        // Step 5: Transition-Out Morph (seamless coordinate & alpha transition to normal app header)
        transitionOut.animateTo(
            targetValue = 1f,
            animationSpec = tween(800, easing = EaseInOutCubic)
        )

        // Clean hand-off
        onTransitionFinished()
    }

    // Map properties for continuous morphing
    val scale = (1.5f * (1f - transitionOut.value)).coerceAtLeast(0.6f)
    val alpha = 1f - transitionOut.value
    val offsetY = -transitionOut.value * 280f // Smooth drift upwards

    Box(
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer {
                this.alpha = alpha
            }
            .background(Color(0xFF070809)), // Absolute sleek dark metal
        contentAlignment = Alignment.Center
    ) {
        // 1. Digital Mesh & Starfield Grid Canvas
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height

            // Render glowing grid centered at viewport
            val horizontalLines = 14
            val verticalLines = 10
            for (i in 0..horizontalLines) {
                val y = h * (i / horizontalLines.toFloat())
                val centerDist = Math.abs(y - h / 2f) / (h / 2f)
                val lineAlpha = (1f - centerDist) * 0.12f * (1f - transitionOut.value)
                drawLine(
                    color = SportsTeal.copy(alpha = lineAlpha),
                    start = Offset(0f, y),
                    end = Offset(w, y),
                    strokeWidth = 1.dp.toPx()
                )
            }
            for (i in 0..verticalLines) {
                val x = w * (i / verticalLines.toFloat())
                val centerDist = Math.abs(x - w / 2f) / (w / 2f)
                val lineAlpha = (1f - centerDist) * 0.12f * (1f - transitionOut.value)
                drawLine(
                    color = VoltLime.copy(alpha = lineAlpha),
                    start = Offset(x, 0f),
                    end = Offset(x, h),
                    strokeWidth = 1.dp.toPx()
                )
            }

            // 2. Telemetry Particles (bubbling/rising performance bits)
            particles.forEach { p ->
                val currentY = ((p.yPercent - (frameTime * p.speed)) % 1f + 1f) % 1f * h
                drawCircle(
                    color = if (p.xPercent > 0.5f) VoltLime.copy(alpha = p.alpha * (1f - transitionOut.value))
                            else SportsTeal.copy(alpha = p.alpha * (1f - transitionOut.value)),
                    radius = p.size.dp.toPx(),
                    center = Offset(p.xPercent * w, currentY)
                )
            }

            // 3. Shockwave Rings expanding outward from epicenter on impact
            if (assembleProgress.value == 1f && flashAlpha.value > 0f) {
                val ringRadius1 = 80f + (1f - flashAlpha.value) * 600f
                val ringRadius2 = 30f + (1f - flashAlpha.value) * 450f
                drawCircle(
                    color = VoltLime.copy(alpha = flashAlpha.value * 0.4f),
                    radius = ringRadius1,
                    center = Offset(w / 2f, h / 2f),
                    style = Stroke(width = 3.dp.toPx())
                )
                drawCircle(
                    color = SportsTeal.copy(alpha = flashAlpha.value * 0.5f),
                    radius = ringRadius2,
                    center = Offset(w / 2f, h / 2f),
                    style = Stroke(width = 2.dp.toPx())
                )
            }
        }

        // Concentrated Cinematic Core containing Logo and Texts
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .offset(
                    x = (shakeOffset.value * Math.sin(frameTime.toDouble() * 40)).dp,
                    y = (offsetY + (shakeOffset.value * Math.cos(frameTime.toDouble() * 40))).dp
                )
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                }
        ) {
            // High-fidelity assembling custom logo drawing canvas
            Box(
                modifier = Modifier
                    .size(160.dp)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                VoltLime.copy(alpha = 0.25f * (boltScale.value)),
                                Color.Transparent
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.size(120.dp)) {
                    val w = size.width
                    val h = size.height
                    val midY = h / 2f

                    // 1. Dumbbell / Barbell Shaft (emerges gradually)
                    drawRoundRect(
                        color = VoltLime.copy(alpha = 0.85f),
                        topLeft = Offset(w * 0.15f, midY - 6f),
                        size = Size(w * 0.7f * assembleProgress.value, 12f),
                        cornerRadius = CornerRadius(6f)
                    )

                    // Converging Offset Calculation (assembleProgress goes from 0f to 1f)
                    val platesLeftOffset = (1f - assembleProgress.value) * w * 0.35f
                    val platesRightOffset = -(1f - assembleProgress.value) * w * 0.35f

                    // 2. Left Weight Plates (Converg toward center)
                    drawRoundRect(
                        color = SportsTeal,
                        topLeft = Offset((w * 0.22f) + platesLeftOffset, h * 0.3f),
                        size = Size(10f, h * 0.4f),
                        cornerRadius = CornerRadius(4f)
                    )
                    drawRoundRect(
                        color = VoltLime,
                        topLeft = Offset((w * 0.32f) + platesLeftOffset, h * 0.2f),
                        size = Size(18f, h * 0.6f),
                        cornerRadius = CornerRadius(6f)
                    )

                    // 3. Right Weight Plates (Converge toward center)
                    drawRoundRect(
                        color = VoltLime,
                        topLeft = Offset((w * 0.58f) + platesRightOffset, h * 0.2f),
                        size = Size(18f, h * 0.6f),
                        cornerRadius = CornerRadius(6f)
                    )
                    drawRoundRect(
                        color = SportsTeal,
                        topLeft = Offset((w * 0.72f) + platesRightOffset, h * 0.3f),
                        size = Size(10f, h * 0.4f),
                        cornerRadius = CornerRadius(4f)
                    )

                    // 4. Heavy Lightning energy core (zooms in on impact)
                    if (boltScale.value > 0f) {
                        val lightningPath = Path().apply {
                            moveTo(w * 0.58f, h * 0.12f)
                            lineTo(w * 0.38f, h * 0.52f)
                            lineTo(w * 0.50f, h * 0.52f)
                            lineTo(w * 0.44f, h * 0.88f)
                            lineTo(w * 0.66f, h * 0.46f)
                            lineTo(w * 0.52f, h * 0.46f)
                        close()
                        }
                        
                        drawContext.canvas.save()
                        drawContext.transform.scale(boltScale.value, boltScale.value, Offset(w / 2f, h / 2f))
                        drawPath(
                            path = lightningPath,
                            brush = Brush.linearGradient(
                                colors = listOf(VoltLime, SportsTeal),
                                start = Offset(w * 0.4f, h * 0.15f),
                                end = Offset(w * 0.65f, h * 0.85f)
                            )
                        )
                        drawContext.canvas.restore()
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Text Animations using local tracking percentages
            val animatedTrackingValue = (1 + (8 * textSpacingPercent.value)).sp
            Text(
                text = "OMNIFIT",
                fontSize = 32.sp,
                fontWeight = FontWeight.Black,
                color = Color.White,
                letterSpacing = animatedTrackingValue,
                modifier = Modifier.graphicsLayer {
                    this.alpha = textReveal.value
                }
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "NEXT-GEN WEARABLE INTELLIGENCE",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = SportsTeal,
                letterSpacing = 1.5.sp,
                modifier = Modifier.graphicsLayer {
                    this.alpha = textReveal.value * 0.8f
                }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Live status loader line
            Box(
                modifier = Modifier
                    .width(140.dp)
                    .height(2.dp)
                    .background(Color.White.copy(alpha = 0.1f), RoundedCornerShape(1.dp))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(fraction = textReveal.value)
                        .background(
                            Brush.linearGradient(listOf(VoltLime, SportsTeal)),
                            RoundedCornerShape(1.dp)
                        )
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = loadStatusText.uppercase(),
                fontSize = 8.5.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                letterSpacing = 1.2.sp,
                modifier = Modifier.graphicsLayer {
                    this.alpha = textReveal.value * 0.9f
                }
            )
        }

        // Fullscreen strobe flash layer
        if (flashAlpha.value > 0f) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                VoltLime.copy(alpha = flashAlpha.value),
                                Color.Transparent
                            )
                        )
                    )
            )
        }
    }
}
