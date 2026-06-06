package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import kotlin.math.sin
import com.example.ui.theme.VoltLime
import com.example.ui.theme.SportsTeal
import com.example.ui.theme.FlameOrange
import com.example.ui.theme.HealthyGreen
import com.example.ui.theme.AlertRed
import com.example.ui.viewmodel.FitnessViewModel

@Composable
fun DashboardScreen(
    viewModel: FitnessViewModel,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()

    val profile by viewModel.userProfile.collectAsState(initial = null)
    val isOnPeriod by viewModel.isOnPeriod.collectAsState()
    val todayLog by viewModel.todayHealthLog.collectAsState()
    val fatigueMap by viewModel.fatigueMap.collectAsState()
    val plateaus by viewModel.detectedPlateaus.collectAsState()

    // Running Mode Active State Collects
    val isRunActive by viewModel.isRunActive.collectAsState()
    val runSeconds by viewModel.runSeconds.collectAsState()
    val runSteps by viewModel.runSteps.collectAsState()
    val runDistanceMeters by viewModel.runDistanceMeters.collectAsState()
    val runCurrentSpeed by viewModel.runCurrentSpeed.collectAsState()
    val runLatitude by viewModel.runLatitude.collectAsState()
    val runLongitude by viewModel.runLongitude.collectAsState()

    val context = androidx.compose.ui.platform.LocalContext.current
    var showLocationDisabledDialog by remember { mutableStateOf(false) }

    var hasActivityPermission by remember {
        mutableStateOf(
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                androidx.core.content.ContextCompat.checkSelfPermission(
                    context,
                    android.Manifest.permission.ACTIVITY_RECOGNITION
                ) == android.content.pm.PackageManager.PERMISSION_GRANTED
            } else {
                true
            }
        )
    }

    val activityPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasActivityPermission = isGranted
    }

    val permissionsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineGranted = permissions[android.Manifest.permission.ACCESS_FINE_LOCATION] ?: false
        val coarseGranted = permissions[android.Manifest.permission.ACCESS_COARSE_LOCATION] ?: false
        if (fineGranted || coarseGranted) {
            val lm = context.getSystemService(android.content.Context.LOCATION_SERVICE) as? android.location.LocationManager
            val isGpsOn = lm?.isProviderEnabled(android.location.LocationManager.GPS_PROVIDER) == true
            val isNetworkOn = lm?.isProviderEnabled(android.location.LocationManager.NETWORK_PROVIDER) == true
            if (!isGpsOn && !isNetworkOn) {
                showLocationDisabledDialog = true
            } else {
                viewModel.startRunning()
            }
        }
    }

    var showEditProfileDialog by remember { mutableStateOf(false) }

    // Onboarding text states for profile editing
    val onboardingWeight by viewModel.onboardingWeight.collectAsState()
    val onboardingHeight by viewModel.onboardingHeight.collectAsState()
    val onboardingAge by viewModel.onboardingAge.collectAsState()
    val onboardingBodyFat by viewModel.onboardingBodyFat.collectAsState()
    val onboardingChest by viewModel.onboardingChest.collectAsState()
    val onboardingArms by viewModel.onboardingArms.collectAsState()
    val onboardingWaist by viewModel.onboardingWaist.collectAsState()
    val onboardingThighs by viewModel.onboardingThighs.collectAsState()
    val onboardingPrimaryGoal by viewModel.onboardingPrimaryGoal.collectAsState()
    val onboardingSecondaryGoal by viewModel.onboardingSecondaryGoal.collectAsState()
    val onboardingEquipment by viewModel.onboardingEquipment.collectAsState()
    val onboardingDays by viewModel.onboardingDays.collectAsState()
    val onboardingTimeMax by viewModel.onboardingTimeMax.collectAsState()
    val onboardingExperienceLevel by viewModel.onboardingExperienceLevel.collectAsState()
    val onboardingGender by viewModel.onboardingGender.collectAsState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 16.dp)
            .verticalScroll(scrollState),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(20.dp))

        // Check and display the beginner restrictive training phase banner
        val p = profile
        val isBeginnerPeriod = if (p != null && p.experienceLevel.equals("Beginner", ignoreCase = true)) {
            val daysPassed = (System.currentTimeMillis() - p.onboardingTimestamp) / (24 * 60 * 60 * 1000)
            daysPassed < 30
        } else {
            false
        }

        if (isBeginnerPeriod) {
            val daysPassed = ((System.currentTimeMillis() - (profile?.onboardingTimestamp ?: 0)) / (24 * 60 * 60 * 1000)).toInt()
            val daysRemaining = (30 - daysPassed).coerceAtLeast(1)
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp)
                    .border(1.dp, SportsTeal.copy(alpha = 0.4f), RoundedCornerShape(12.dp)),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = SportsTeal.copy(alpha = 0.12f))
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "Beginner Period Info Icon",
                        tint = SportsTeal,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "🔰 Beginner Training Phase Active",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = SportsTeal
                        )
                        Text(
                            text = "$daysRemaining days remaining of safe form induction. Curating Beginner-only exercises to condition joints.",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // Profile brief with Neon Glow Accent and Edit button
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, VoltLime.copy(alpha = 0.3f), RoundedCornerShape(16.dp)),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Interactive Profile avatar with dynamic sport status
                Box(
                    modifier = Modifier
                        .size(54.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                colors = listOf(VoltLime, SportsTeal)
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = "User Profile icon",
                        tint = Color.Black,
                        modifier = Modifier.size(28.dp)
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = if (profile?.gender.equals("Female", ignoreCase = true) && isOnPeriod) "Hello, Warrior! 🌸" else "Hello, Gym Warrior!",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.onSurface,
                            letterSpacing = (-0.3).sp
                        )
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    if (profile?.gender.equals("Female", ignoreCase = true) && isOnPeriod) {
                        Text(
                            text = "💖 Cozy Period Care Active (Stretches & Comfort)",
                            fontSize = 11.sp,
                            color = Color(0xFFE53935),
                            fontWeight = FontWeight.Black
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                    }
                    Text(
                        text = "Goal: ${profile?.primaryGoal ?: "Onboarding"} (${profile?.equipmentInventory ?: "General"})",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                IconButton(
                    onClick = {
                        profile?.let { viewModel.loadExistingProfileToOnboarding(it) }
                        showEditProfileDialog = true
                    }
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Edit Profile and metrics",
                        tint = VoltLime
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Section Title: Passive Wellness trackers
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.DirectionsRun,
                contentDescription = null,
                tint = VoltLime,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "Passive Wellness Trackers",
                fontSize = 16.sp,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Steps Panel Card
            Card(
                modifier = Modifier
                    .weight(1f)
                    .border(1.dp, VoltLime.copy(alpha = 0.15f), RoundedCornerShape(14.dp)),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .background(VoltLime.copy(alpha = 0.15f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.DirectionsWalk,
                                contentDescription = null,
                                tint = VoltLime,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Steps", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Text("${todayLog.stepsCount}", fontSize = 24.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.onSurface)
                    Text("Target: 10,000", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Medium)

                    Spacer(modifier = Modifier.height(18.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                if (hasActivityPermission) VoltLime.copy(alpha = 0.08f) 
                                else MaterialTheme.colorScheme.error.copy(alpha = 0.08f), 
                                RoundedCornerShape(8.dp)
                            )
                            .clickable {
                                if (!hasActivityPermission && android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                                    activityPermissionLauncher.launch(android.Manifest.permission.ACTIVITY_RECOGNITION)
                                }
                            }
                            .padding(vertical = 8.dp, horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .background(if (hasActivityPermission) VoltLime else MaterialTheme.colorScheme.error, CircleShape)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (hasActivityPermission) "Sensory Active" else "Sensory Inactive (Tap to grant)",
                            color = if (hasActivityPermission) VoltLime else MaterialTheme.colorScheme.error,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                }
            }

            // Calories & Water Panel
            Card(
                modifier = Modifier
                    .weight(1f)
                    .border(1.dp, SportsTeal.copy(alpha = 0.15f), RoundedCornerShape(14.dp)),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .background(FlameOrange.copy(alpha = 0.15f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.LocalFireDepartment,
                                contentDescription = null,
                                tint = FlameOrange,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Active Energy", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Text("${todayLog.activeCaloriesBurned.toInt()} kcal", fontSize = 24.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.onSurface)
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Filled.LocalDrink,
                            contentDescription = null,
                            tint = SportsTeal,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Column {
                            Text("Water: ${todayLog.waterIntakeMl}ml", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                            Text("Target: 3k ml", fontSize = 8.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.SemiBold)
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = { viewModel.recordWaterIntake(250) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(34.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = SportsTeal,
                            contentColor = Color.Black
                        ),
                        contentPadding = PaddingValues(0.dp),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("+250 ml", fontSize = 11.sp, fontWeight = FontWeight.Black)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Running Tracker Section
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.DirectionsRun,
                contentDescription = null,
                tint = VoltLime,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "Dynamic Run Tracker (GPS & Motion)",
                fontSize = 16.sp,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(
                    width = 1.dp,
                    color = if (isRunActive) VoltLime.copy(alpha = 0.5f) else MaterialTheme.colorScheme.outlineVariant,
                    shape = RoundedCornerShape(16.dp)
                ),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (isRunActive) MaterialTheme.colorScheme.surfaceColorAtElevation(2.dp)
                else MaterialTheme.colorScheme.surface
            )
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                if (!isRunActive) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Start Running Mode Session",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Uses phone accelerometer dynamic stride counting and live GPS location updates to map and compute exact meters and kilometers.",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                lineHeight = 15.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Button(
                        onClick = {
                            val fineGranted = androidx.core.content.ContextCompat.checkSelfPermission(
                                context,
                                android.Manifest.permission.ACCESS_FINE_LOCATION
                            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
                            val coarseGranted = androidx.core.content.ContextCompat.checkSelfPermission(
                                context,
                                android.Manifest.permission.ACCESS_COARSE_LOCATION
                            ) == android.content.pm.PackageManager.PERMISSION_GRANTED

                            if (fineGranted || coarseGranted) {
                                val lm = context.getSystemService(android.content.Context.LOCATION_SERVICE) as? android.location.LocationManager
                                val isGpsOn = lm?.isProviderEnabled(android.location.LocationManager.GPS_PROVIDER) == true
                                val isNetworkOn = lm?.isProviderEnabled(android.location.LocationManager.NETWORK_PROVIDER) == true
                                if (!isGpsOn && !isNetworkOn) {
                                    showLocationDisabledDialog = true
                                } else {
                                    viewModel.startRunning()
                                }
                            } else {
                                permissionsLauncher.launch(
                                    arrayOf(
                                        android.Manifest.permission.ACCESS_FINE_LOCATION,
                                        android.Manifest.permission.ACCESS_COARSE_LOCATION
                                    )
                                )
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = VoltLime,
                            contentColor = Color.Black
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Connect Sensors & Start Run", fontWeight = FontWeight.Black, fontSize = 13.sp)
                    }
                } else {
                    // Running Mode Active State Panel
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(VoltLime)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "LIVE RUNNING SESSION ACTIVE",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = VoltLime,
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.weight(1f))
                        Text(
                            text = String.format(java.util.Locale.US, "%02d:%02d", runSeconds / 60, runSeconds % 60),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // 3 Grid Stats
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Card 1: Distance
                        Card(
                            modifier = Modifier.weight(1.3f),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text("Distance", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = if (runDistanceMeters >= 1000.0) {
                                        String.format(java.util.Locale.US, "%.2f km", runDistanceMeters / 1000.0)
                                    } else {
                                        String.format(java.util.Locale.US, "%.0f meters", runDistanceMeters)
                                    },
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Black,
                                    color = VoltLime
                                )
                                Text(
                                    text = if (runDistanceMeters >= 1000.0) {
                                        String.format(java.util.Locale.US, "%.0f m", runDistanceMeters)
                                    } else {
                                        String.format(java.util.Locale.US, "%.3f km", runDistanceMeters / 1000.0)
                                    },
                                    fontSize = 9.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        // Card 2: Steps
                        Card(
                            modifier = Modifier.weight(1f),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text("Steps", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "$runSteps",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Black,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text("motion synced", fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }

                        // Card 3: Speed / Pace
                        Card(
                            modifier = Modifier.weight(1f),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text("Speed", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Spacer(modifier = Modifier.height(4.dp))
                                val speedKmh = runCurrentSpeed * 3.6
                                Text(
                                    text = String.format(java.util.Locale.US, "%.1f km/h", speedKmh),
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Black,
                                    color = SportsTeal
                                )
                                Text(
                                    text = String.format(java.util.Locale.US, "%.1f m/s", runCurrentSpeed),
                                    fontSize = 9.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Live Coordinates or Calibration Status Ribbon
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f))
                            .padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = null,
                            tint = if (runLatitude != null) VoltLime else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (runLatitude != null && runLongitude != null) {
                                String.format(java.util.Locale.US, "GPS Active: %.5f° N, %.5f° E", runLatitude, runLongitude)
                            } else {
                                "Calibrating Outdoor GPS sensors... (Step-motion fallback ACTIVE)"
                            },
                            fontSize = 10.sp,
                            color = if (runLatitude != null) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Live Route Canvas Simulation of Route Progress
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(60.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.Black.copy(alpha = 0.4f))
                    ) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val w = size.width
                            val h = size.height

                            val path = Path()
                            path.moveTo(0f, h * 0.7f)

                            // Generate organic-looking running path curve based on runSeconds to simulate actual progress
                            val pointCount = 12
                            for (i in 1..pointCount) {
                                val x = (w / pointCount) * i
                                val sinArg = (i * 1.5f) + (runSeconds * 0.12f)
                                val yAdjustment = sin(sinArg) * (h * 0.25f)
                                val targetY = (h * 0.5f) + yAdjustment
                                path.lineTo(x, targetY)
                            }

                            // Outline the path with glowing gradient
                            drawPath(
                                path = path,
                                brush = Brush.horizontalGradient(
                                    colors = listOf(SportsTeal, VoltLime)
                                ),
                                style = androidx.compose.ui.graphics.drawscope.Stroke(
                                    width = 6f,
                                    cap = androidx.compose.ui.graphics.StrokeCap.Round
                                )
                            )

                            // Pulse indicator at current end position
                            val finalX = w
                            val finalYAdjustment = sin((pointCount * 1.5f) + (runSeconds * 0.12f)) * (h * 0.25f)
                            val finalY = (h * 0.5f) + finalYAdjustment

                            drawCircle(
                                color = VoltLime,
                                radius = 7f + sin(runSeconds * 1.5f) * 3f,
                                center = Offset(finalX - 10f, finalY)
                            )
                        }
                        Box(
                            modifier = Modifier
                                .align(Alignment.Center)
                                .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(4.dp))
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text(
                                "Live Route Profile Trace",
                                fontSize = 8.sp,
                                color = VoltLime,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Button(
                        onClick = { viewModel.stopRunning() },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = AlertRed,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(imageVector = Icons.Default.Stop, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Stop & Record Run Statistics", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Muscle Recovery Heatmap
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.FitnessCenter,
                contentDescription = null,
                tint = SportsTeal,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "Muscle Recovery Heatmap",
                fontSize = 16.sp,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.onBackground
            )
        }
        Text(
            text = "Calculates muscular load from actual logged exercise weights & recovery intervals.",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 2.dp, bottom = 12.dp)
        )

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, SportsTeal.copy(alpha = 0.25f), RoundedCornerShape(16.dp)),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth().height(220.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1.1f)
                            .fillMaxHeight()
                            .background(Color.Black.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                            .border(1.dp, SportsTeal.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
                            .padding(8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val w = size.width
                            val h = size.height
                            val cx = w / 2

                            drawLine(color = SportsTeal.copy(alpha = 0.08f), start = Offset(cx, 0f), end = Offset(cx, h), strokeWidth = 1.0f)
                            drawCircle(color = SportsTeal.copy(alpha = 0.08f), center = Offset(cx, h * 0.4f), radius = w * 0.35f, style = androidx.compose.ui.graphics.drawscope.Stroke(1f))

                            drawCircle(color = Color.DarkGray, center = Offset(cx, h * 0.12f), radius = 10f)

                            val shFat = fatigueMap["Shoulders"] ?: 0.1f
                            val shColor = if (shFat > 0.7f) AlertRed else if (shFat > 0.3f) FlameOrange else HealthyGreen
                            drawCircle(color = shColor.copy(alpha = 0.8f), center = Offset(cx - 20f, h * 0.25f), radius = 9f)
                            drawCircle(color = shColor.copy(alpha = 0.8f), center = Offset(cx + 20f, h * 0.25f), radius = 9f)

                            val chFat = fatigueMap["Chest"] ?: 0.1f
                            val chColor = if (chFat > 0.7f) AlertRed else if (chFat > 0.3f) FlameOrange else HealthyGreen
                            drawRoundRect(color = chColor.copy(alpha = 0.7f), topLeft = Offset(cx - 18f, h * 0.3f), size = androidx.compose.ui.geometry.Size(16f, 18f), cornerRadius = androidx.compose.ui.geometry.CornerRadius(4f))
                            drawRoundRect(color = chColor.copy(alpha = 0.7f), topLeft = Offset(cx + 2f, h * 0.3f), size = androidx.compose.ui.geometry.Size(16f, 18f), cornerRadius = androidx.compose.ui.geometry.CornerRadius(4f))

                            val arFat = fatigueMap["Arms"] ?: 0.1f
                            val arColor = if (arFat > 0.7f) AlertRed else if (arFat > 0.3f) FlameOrange else HealthyGreen
                            drawRoundRect(color = arColor.copy(alpha = 0.6f), topLeft = Offset(cx - 32f, h * 0.32f), size = androidx.compose.ui.geometry.Size(10f, 26f), cornerRadius = androidx.compose.ui.geometry.CornerRadius(5f))
                            drawRoundRect(color = arColor.copy(alpha = 0.6f), topLeft = Offset(cx + 22f, h * 0.32f), size = androidx.compose.ui.geometry.Size(10f, 26f), cornerRadius = androidx.compose.ui.geometry.CornerRadius(5f))

                            val coFat = fatigueMap["Core"] ?: 0.1f
                            val coColor = if (coFat > 0.7f) AlertRed else if (coFat > 0.3f) FlameOrange else HealthyGreen
                            drawRoundRect(color = coColor.copy(alpha = 0.7f), topLeft = Offset(cx - 8f, h * 0.44f), size = androidx.compose.ui.geometry.Size(16f, 30f), cornerRadius = androidx.compose.ui.geometry.CornerRadius(3f))

                            val quFat = fatigueMap["Quads"] ?: 0.1f
                            val quColor = if (quFat > 0.7f) AlertRed else if (quFat > 0.3f) FlameOrange else HealthyGreen
                            drawRoundRect(color = quColor.copy(alpha = 0.7f), topLeft = Offset(cx - 18f, h * 0.65f), size = androidx.compose.ui.geometry.Size(15f, 36f), cornerRadius = androidx.compose.ui.geometry.CornerRadius(7f))
                            drawRoundRect(color = quColor.copy(alpha = 0.7f), topLeft = Offset(cx + 3f, h * 0.65f), size = androidx.compose.ui.geometry.Size(15f, 36f), cornerRadius = androidx.compose.ui.geometry.CornerRadius(7f))
                        }

                        Text("TOGGLE NODES", color = Color.White.copy(alpha = 0.3f), fontSize = 7.sp, fontWeight = FontWeight.Black, modifier = Modifier.align(Alignment.BottomCenter))
                    }

                    Column(
                        modifier = Modifier.weight(1.3f).fillMaxHeight(),
                        verticalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        listOf("Chest", "Back", "Shoulders", "Arms", "Core", "Quads").forEach { group ->
                            val fat = fatigueMap[group] ?: 0.1f
                            val statusStr = if (fat > 0.7f) "Fatigued" else if (fat > 0.3f) "Recovering" else "Ready"
                            val colorScheme = if (fat > 0.7f) AlertRed else if (fat > 0.3f) FlameOrange else HealthyGreen

                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color.White.copy(alpha = 0.03f), RoundedCornerShape(6.dp))
                                    .clickable {
                                        if (fat >= 0.8f) viewModel.clearMuscleLoad(group)
                                        else viewModel.addManualMuscleLoad(group)
                                    }
                                    .padding(vertical = 4.dp, horizontal = 6.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(text = group, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    Text(text = "${(fat * 100).toInt()}% ($statusStr)", fontSize = 8.sp, fontWeight = FontWeight.Black, color = colorScheme)
                                }
                                Spacer(modifier = Modifier.height(3.dp))
                                LinearProgressIndicator(
                                    progress = fat,
                                    modifier = Modifier.fillMaxWidth().height(3.dp),
                                    color = colorScheme,
                                    trackColor = Color.White.copy(alpha = 0.08f)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(6.dp).background(HealthyGreen, CircleShape))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Ready (<30%)", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(6.dp).background(FlameOrange, CircleShape))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Active Rest (30-70%)", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(6.dp).background(AlertRed, CircleShape))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Strained (>70%)", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                Card(
                    colors = CardDefaults.cardColors(containerColor = AlertRed.copy(alpha = 0.08f)),
                    border = androidx.compose.foundation.BorderStroke(1.dp, AlertRed.copy(alpha = 0.3f)),
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = "Caution: Still under development",
                            tint = AlertRed,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "Still in Development",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "We are refining muscle load calculations. This feature is open for suggestions to improve medical/sports metrics!",
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                lineHeight = 13.sp
                            )
                        }
                    }
                }
            }
        }

        if (profile?.gender.equals("Female", ignoreCase = true)) {
            Spacer(modifier = Modifier.height(16.dp))
            PeriodSlidingButton(
                isOnPeriod = isOnPeriod,
                onValueChange = { viewModel.setIsOnPeriod(it) }
            )
        }

        Spacer(modifier = Modifier.height(16.dp))
    }

    if (showLocationDisabledDialog) {
        AlertDialog(
            onDismissRequest = { showLocationDisabledDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.LocationOff,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Turn On Location Services", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                }
            },
            text = {
                Text(
                    text = "To connect sensors and track your live route, distance, and running stats accurately, please enable location services on your phone.",
                    fontSize = 14.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showLocationDisabledDialog = false
                        try {
                            val intent = android.content.Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = VoltLime,
                        contentColor = Color.Black
                    )
                ) {
                    Text("Go to Settings", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showLocationDisabledDialog = false }
                ) {
                    Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        )
    }

    if (showEditProfileDialog) {
        val dialogScrollState = rememberScrollState()

        // Real-time body fat index calculation based on weight, height, age, and waist metrics
        val weightVal = onboardingWeight.toDoubleOrNull() ?: 75.0
        val heightVal = onboardingHeight.toDoubleOrNull() ?: 175.0
        val ageVal = onboardingAge.toIntOrNull() ?: 28
        val waistVal = onboardingWaist.toDoubleOrNull() ?: 82.0

        val autoFat = remember(weightVal, heightVal, ageVal, waistVal) {
            if (weightVal > 15.0 && heightVal > 50.0 && waistVal > 30.0) {
                val bmi = weightVal / ((heightVal / 100.0) * (heightVal / 100.0))
                // Navy/YMCA health mathematical adaptation
                val rawEst = 86.010 * Math.log10(waistVal - 10.0) - 70.041 * Math.log10(heightVal) + 36.76
                val finalEst = if (rawEst > 4.0 && rawEst < 45.0) rawEst else (1.20 * bmi) + (0.23 * ageVal) - 16.2
                String.format(java.util.Locale.US, "%.1f", finalEst.coerceIn(5.0, 50.0))
            } else {
                "15.0"
            }
        }

        LaunchedEffect(autoFat) {
            viewModel.onboardingBodyFat.value = autoFat
        }

        AlertDialog(
            onDismissRequest = { showEditProfileDialog = false },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.saveProfileFromOnboarding()
                        showEditProfileDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = VoltLime, contentColor = Color.Black)
                ) {
                    Text("Save Changes", fontWeight = FontWeight.Black)
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditProfileDialog = false }) {
                    Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Settings, contentDescription = null, tint = VoltLime, modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Edit Profile & Physical Metrics", fontWeight = FontWeight.Black, fontSize = 18.sp)
                }
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 450.dp)
                        .verticalScroll(dialogScrollState),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "Adjusting metrics triggers real-time mechanical modifications and overrides previous workout planning.",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 15.sp
                    )

                    Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.12f))

                    // SECTION 1: Key Profile Metrics
                    Text("1. KEY PROFILE METRICS", fontSize = 11.sp, fontWeight = FontWeight.Black, color = VoltLime)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedTextField(
                            value = onboardingWeight,
                            onValueChange = { viewModel.onboardingWeight.value = it },
                            label = { Text("Weight (kg)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = onboardingHeight,
                            onValueChange = { viewModel.onboardingHeight.value = it },
                            label = { Text("Height (cm)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedTextField(
                            value = onboardingAge,
                            onValueChange = { viewModel.onboardingAge.value = it },
                            label = { Text("Age") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = onboardingBodyFat,
                            onValueChange = { },
                            readOnly = true,
                            label = { Text("Body Fat %") },
                            supportingText = { Text("Auto calculated", fontSize = 9.sp, color = VoltLime) },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text("Gender Alignment:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
                        listOf("Male", "Female").forEach { g ->
                            val isSelected = onboardingGender == g
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(
                                        if (isSelected) VoltLime.copy(alpha = 0.2f)
                                        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                                    )
                                    .border(
                                        width = 1.dp,
                                        color = if (isSelected) VoltLime else Color.Transparent,
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                    .clickable { viewModel.onboardingGender.value = g }
                                    .padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Text(
                                        text = if (g == "Male") "🙋‍♂️ Male" else "🙋‍♀️ Female",
                                        color = if (isSelected) VoltLime else MaterialTheme.colorScheme.onSurface,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }

                    // SECTION 2: Body Tape Dimensions
                    Text("2. BODY TAPE DIMENSIONS", fontSize = 11.sp, fontWeight = FontWeight.Black, color = VoltLime)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedTextField(
                            value = onboardingChest,
                            onValueChange = { viewModel.onboardingChest.value = it },
                            label = { Text("Chest (cm)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = onboardingArms,
                            onValueChange = { viewModel.onboardingArms.value = it },
                            label = { Text("Arms (cm)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedTextField(
                            value = onboardingWaist,
                            onValueChange = { viewModel.onboardingWaist.value = it },
                            label = { Text("Waist (cm)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = onboardingThighs,
                            onValueChange = { viewModel.onboardingThighs.value = it },
                            label = { Text("Thighs (cm)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f)
                        )
                    }

                    // SECTION 3: Workout custom limits
                    Text("3. TRAINING OPTIONS", fontSize = 11.sp, fontWeight = FontWeight.Black, color = VoltLime)

                    // Equipment Selectors
                    Text("Equipment Inventory:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                    val equipmentInventoryOptions = listOf("Full Gym", "Dumbbells Only", "Bodyweight/Calisthenics")
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        equipmentInventoryOptions.forEach { option ->
                            val isSelected = onboardingEquipment == option
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { viewModel.onboardingEquipment.value = option },
                                shape = RoundedCornerShape(8.dp),
                                color = if (isSelected) VoltLime.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                border = if (isSelected) androidx.compose.foundation.BorderStroke(1.dp, VoltLime) else null
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    RadioButton(
                                        selected = isSelected,
                                        onClick = { viewModel.onboardingEquipment.value = option },
                                        colors = RadioButtonDefaults.colors(selectedColor = VoltLime)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(option, fontSize = 13.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal)
                                }
                            }
                        }
                    }

                    // Workout Goals Selectors
                    Text("Primary Target Goal:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                    val goalOptions = listOf("Hypertrophy", "Strength", "Endurance", "Fat Loss", "Mobility")
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        goalOptions.forEach { option ->
                            val isSelected = onboardingPrimaryGoal == option
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { viewModel.onboardingPrimaryGoal.value = option },
                                shape = RoundedCornerShape(8.dp),
                                color = if (isSelected) SportsTeal.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                border = if (isSelected) androidx.compose.foundation.BorderStroke(1.dp, SportsTeal) else null
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    RadioButton(
                                        selected = isSelected,
                                        onClick = { viewModel.onboardingPrimaryGoal.value = option },
                                        colors = RadioButtonDefaults.colors(selectedColor = SportsTeal)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(option, fontSize = 13.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal)
                                }
                            }
                        }
                    }

                    // Experience Level Selectors
                    Text("Experience Level:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                    val levelOptions = listOf("Beginner", "Intermediate", "Advanced")
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        levelOptions.forEach { lvl ->
                            val isSelected = onboardingExperienceLevel == lvl
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { viewModel.onboardingExperienceLevel.value = lvl },
                                shape = RoundedCornerShape(8.dp),
                                color = if (isSelected) MaterialTheme.colorScheme.tertiary.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                border = if (isSelected) androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.tertiary) else null
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    RadioButton(
                                        selected = isSelected,
                                        onClick = { viewModel.onboardingExperienceLevel.value = lvl },
                                        colors = RadioButtonDefaults.colors(selectedColor = MaterialTheme.colorScheme.tertiary)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(lvl, fontSize = 13.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal)
                                }
                            }
                        }
                    }

                    // Days per week limit slider
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Days/Week Available:", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Text("$onboardingDays days", fontSize = 12.sp, fontWeight = FontWeight.Black, color = VoltLime)
                    }
                    Slider(
                        value = onboardingDays.toFloat(),
                        onValueChange = { viewModel.onboardingDays.value = it.toInt() },
                        valueRange = 1f..7f,
                        steps = 5,
                        colors = SliderDefaults.colors(thumbColor = VoltLime, activeTrackColor = VoltLime)
                    )

                    // Session Time duration slider
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Session Max Time Limit:", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Text("$onboardingTimeMax mins", fontSize = 12.sp, fontWeight = FontWeight.Black, color = SportsTeal)
                    }
                    Slider(
                        value = onboardingTimeMax.toFloat(),
                        onValueChange = { viewModel.onboardingTimeMax.value = it.toInt() },
                        valueRange = 15f..120f,
                        steps = 6,
                        colors = SliderDefaults.colors(thumbColor = SportsTeal, activeTrackColor = SportsTeal)
                    )
                }
            }
        )
    }
}

@Composable
fun MuscleRecoveryNode(muscleName: String, fatigue: Float, onClick: () -> Unit) {
    val nodeColor = when {
        fatigue < 0.3f -> HealthyGreen
        fatigue < 0.7f -> FlameOrange
        else -> AlertRed
    }

    val recoveryPercentage = ((1f - fatigue) * 100).toInt()

    Card(
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(4.dp)),
        modifier = Modifier
            .width(96.dp)
            .border(1.dp, nodeColor.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
            .clickable { onClick() }
    ) {
        Column(
            modifier = Modifier.padding(6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = muscleName,
                fontSize = 10.sp,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(2.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(nodeColor.copy(alpha = 0.2f))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(1f - fatigue)
                        .background(nodeColor)
                )
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "$recoveryPercentage% Recov",
                fontSize = 8.sp,
                color = nodeColor,
                fontWeight = FontWeight.Black
            )
        }
    }
}
