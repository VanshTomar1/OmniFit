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
import com.example.ui.screens.*
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.VoltLime
import com.example.ui.viewmodel.FitnessViewModel
import com.example.ui.viewmodel.FitnessViewModelFactory

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
                val notification = androidx.core.app.NotificationCompat.Builder(this@MainActivity, "omnifit_steps_channel")
                    .setSmallIcon(android.R.drawable.ic_dialog_info)
                    .setContentTitle("🏃 OmniFit Motion Tracker Active")
                    .setContentText("Live Steps: $steps | Metabolic Burn: $calorieStr kcal")
                    .setOngoing(true) // Keep in notification panel
                    .setOnlyAlertOnce(true)
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

            val notification = androidx.core.app.NotificationCompat.Builder(this, "omnifit_steps_channel")
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle("🏃 Live Running Mode Active!")
                .setContentText("Time: $minStr | Dist: $distStr | Speed: $paceStr")
                .setStyle(androidx.core.app.NotificationCompat.BigTextStyle()
                    .bigText("Time: $minStr\nDistance: $distStr\nSteps: $steps\nLive Speed: $paceStr"))
                .setOngoing(true)
                .setOnlyAlertOnce(true)
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

        // Request POST_NOTIFICATIONS, ACTIVITY_RECOGNITION, location, and CAMERA permissions dynamically
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
        if (checkSelfPermission(android.Manifest.permission.CAMERA) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
            permissionsNeeded.add(android.Manifest.permission.CAMERA)
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
            MyApplicationTheme {
                val isProfileLoaded by viewModel.isProfileLoaded.collectAsState()
                val profile by viewModel.userProfile.collectAsState(initial = null)
                val activeSessionId by viewModel.activeSessionId.collectAsState()
                val lastCompletedSummary by viewModel.lastCompletedWorkoutSummary.collectAsState()

                // Determine first-time versus second-time status using SharedPreferences
                val sharedPrefs = remember { getSharedPreferences("com.aistudio.omnifit.PREFS", android.content.Context.MODE_PRIVATE) }
                var hasOpenedBefore by remember { mutableStateOf(sharedPrefs.getBoolean("has_opened_before", false)) }

                // State to control active tab
                var selectedTab by remember { mutableStateOf(0) }

                // 2-second elegant loading state specifically for returning users (second open onwards)
                var splashTimerActive by remember { mutableStateOf(hasOpenedBefore) }

                LaunchedEffect(hasOpenedBefore, isProfileLoaded) {
                    if (isProfileLoaded) {
                        if (hasOpenedBefore) {
                            splashTimerActive = true
                            kotlinx.coroutines.delay(2000)
                            splashTimerActive = false
                        } else {
                            splashTimerActive = false
                        }
                    }
                }

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    bottomBar = {
                        // Display bottom bar only if fully onboarded, not actively executing a workout, and splash is not active
                        if (isProfileLoaded && profile != null && activeSessionId == null && !splashTimerActive) {
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
                                    icon = { Icon(Icons.Default.Videocam, contentDescription = "AI Form") },
                                    label = { Text("Form AI", fontWeight = FontWeight.Bold) }
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

                            // Splash loading for returning user
                            profile != null && splashTimerActive -> {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(MaterialTheme.colorScheme.background),
                                    contentAlignment = androidx.compose.ui.Alignment.Center
                                ) {
                                    Column(
                                        horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center,
                                        modifier = Modifier.padding(24.dp)
                                    ) {
                                        OmniFitLogo(modifier = Modifier.size(90.dp))
                                        Spacer(modifier = Modifier.height(20.dp))
                                        Text(
                                            text = "OMNIFIT ADVISOR",
                                            fontWeight = FontWeight.Black,
                                            fontSize = 22.sp,
                                            letterSpacing = 2.sp,
                                            color = VoltLime
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            text = "Loading personalized AI metrics & athletic logs...",
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                        )
                                        Spacer(modifier = Modifier.height(32.dp))
                                        CircularProgressIndicator(
                                            color = VoltLime,
                                            strokeWidth = 3.dp,
                                            modifier = Modifier.size(40.dp)
                                        )
                                    }
                                }
                            }

                            // Stage 0: If profile is not set up, load user profiling onboard
                            profile == null -> {
                                OnboardingScreen(
                                    viewModel = viewModel,
                                    initialHasOpenedBefore = hasOpenedBefore,
                                    onIntroComplete = {
                                        sharedPrefs.edit().putBoolean("has_opened_before", true).apply()
                                        hasOpenedBefore = true
                                    },
                                    onComplete = {
                                        // Also guarantee we mark opened before once profile onboarding finishes
                                        sharedPrefs.edit().putBoolean("has_opened_before", true).apply()
                                        hasOpenedBefore = true
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
                                    3 -> CameraFrameScreen()
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
