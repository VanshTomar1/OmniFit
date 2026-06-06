package com.example.ui.screens

import android.content.Context
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
import com.example.ui.theme.VoltLime
import com.example.ui.theme.SportsTeal
import com.example.ui.theme.AlertRed
import kotlinx.coroutines.delay
import java.util.Locale
import kotlin.math.sin

enum class DetectionMode {
    AUTO_AI, SQUAT, BICEP_CURL, PUSH_UP, OVERHEAD_PRESS
}

@Composable
fun CameraFrameScreen(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }
    var cameraProvider by remember { mutableStateOf<ProcessCameraProvider?>(null) }
    var previewViewRef by remember { mutableStateOf<PreviewView?>(null) }

    LaunchedEffect(cameraProviderFuture) {
        val executor = ContextCompat.getMainExecutor(context)
        cameraProviderFuture.addListener({
            try {
                cameraProvider = cameraProviderFuture.get()
            } catch (t: Throwable) {
                t.printStackTrace()
            }
        }, executor)
    }

    DisposableEffect(lifecycleOwner) {
        onDispose {
            try {
                if (cameraProviderFuture.isDone) {
                    val provider = cameraProviderFuture.get()
                    provider.unbindAll()
                }
            } catch (t: Throwable) {
                t.printStackTrace()
            }
        }
    }

    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, android.Manifest.permission.CAMERA) == android.content.pm.PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasPermission = granted
    }

    LaunchedEffect(Unit) {
        if (!hasPermission) {
            permissionLauncher.launch(android.Manifest.permission.CAMERA)
        }
    }

    var isBackCamera by remember { mutableStateOf(true) }

    LaunchedEffect(hasPermission, isBackCamera, cameraProvider, previewViewRef) {
        val provider = cameraProvider ?: return@LaunchedEffect
        val view = previewViewRef ?: return@LaunchedEffect
        if (!hasPermission) return@LaunchedEffect

        try {
            provider.unbindAll()

            val cameraSelector = try {
                if (isBackCamera) {
                    if (provider.hasCamera(CameraSelector.DEFAULT_BACK_CAMERA)) {
                        CameraSelector.DEFAULT_BACK_CAMERA
                    } else if (provider.hasCamera(CameraSelector.DEFAULT_FRONT_CAMERA)) {
                        CameraSelector.DEFAULT_FRONT_CAMERA
                    } else {
                        null
                    }
                } else {
                    if (provider.hasCamera(CameraSelector.DEFAULT_FRONT_CAMERA)) {
                        CameraSelector.DEFAULT_FRONT_CAMERA
                    } else if (provider.hasCamera(CameraSelector.DEFAULT_BACK_CAMERA)) {
                        CameraSelector.DEFAULT_BACK_CAMERA
                    } else {
                        null
                    }
                }
            } catch (t: Throwable) {
                t.printStackTrace()
                null
            }

            if (cameraSelector != null) {
                val preview = Preview.Builder().build().also {
                    it.setSurfaceProvider(view.surfaceProvider)
                }

                provider.bindToLifecycle(
                    lifecycleOwner,
                    cameraSelector,
                    preview
                )
            } else {
                android.util.Log.w("CameraFrameScreen", "No compatible physical camera detected. Running in mock posture coordinate simulator.")
            }
        } catch (t: Throwable) {
            t.printStackTrace()
        }
    }

    // Interactive AI state management
    var rawMode by remember { mutableStateOf(DetectionMode.AUTO_AI) }
    var lastActiveEx by remember { mutableStateOf("Squat") }
    var activeExName by remember { mutableStateOf("Squat") }
    var isScanning by remember { mutableStateOf(false) }
    var confidenceRate by remember { mutableStateOf(96) }
    var repsCount by remember { mutableStateOf(4) }
    var voiceCueEnabled by remember { mutableStateOf(true) }
    var formScore by remember { mutableStateOf(92) }
    
    // Rep cadence time-cycle animation coordinates
    var timeElapsed by remember { mutableStateOf(0.0f) }
    val jointsProgress = sin(timeElapsed) * 0.5f + 0.5f // ranges strictly 0.0f to 1.0f
    
    // Low performance delay ticking to simulate camera feed processing loops
    LaunchedEffect(Unit) {
        while (true) {
            delay(200)
            timeElapsed += 0.4f
        }
    }

    // Auto-Exercise Detection transition loop
    LaunchedEffect(rawMode) {
        if (rawMode == DetectionMode.AUTO_AI) {
            isScanning = false
            activeExName = "Squat"
            confidenceRate = 96
            formScore = 94
            while (rawMode == DetectionMode.AUTO_AI) {
                delay(7000) // Swap simulated activity every 7 seconds
                isScanning = true
                delay(1200) // Landmarking scan interval
                isScanning = false
                
                when (activeExName) {
                    "Squat" -> {
                        activeExName = "Bicep Curl"
                        confidenceRate = 98
                        formScore = 87
                    }
                    "Bicep Curl" -> {
                        activeExName = "Push-Up"
                        confidenceRate = 94
                        formScore = 95
                    }
                    "Push-Up" -> {
                        activeExName = "Overhead Press"
                        confidenceRate = 97
                        formScore = 91
                    }
                    else -> {
                        activeExName = "Squat"
                        confidenceRate = 96
                        formScore = 93
                    }
                }
                repsCount = (1..12).random()
            }
        } else {
            isScanning = false
            activeExName = when (rawMode) {
                DetectionMode.SQUAT -> "Squat"
                DetectionMode.BICEP_CURL -> "Bicep Curl"
                DetectionMode.PUSH_UP -> "Push-Up"
                DetectionMode.OVERHEAD_PRESS -> "Overhead Press"
                else -> "Squat"
            }
            confidenceRate = (92..99).random()
            formScore = (85..98).random()
        }
    }

    // Automated rep counters
    var lastProgress = 0f
    LaunchedEffect(jointsProgress) {
        if (lastProgress < 0.8f && jointsProgress >= 0.82f) {
            repsCount += 1
        }
        lastProgress = jointsProgress
    }

    // Generate dynamic joint angles and corrective coaching instructions on physical feedback loops
    val (jointAngleLabel, postureCue, cueColor) = remember(activeExName, jointsProgress) {
        when (activeExName) {
            "Squat" -> {
                val currentDepthVal = (125 - 45 * jointsProgress).toInt()
                val isFine = currentDepthVal < 100
                val textLabel = "Knee Flexion: $currentDepthVal°"
                val advice = if (isFine) {
                    "✅ Depth sufficient! Drive through your heels."
                } else {
                    "⚠️ Depth insufficient! Drop your hips parallel to the ground."
                }
                val color = if (isFine) VoltLime else Color(0xFFFFB300)
                Triple(textLabel, advice, color)
            }
            "Bicep Curl" -> {
                val currentElbowAngle = (160 - 120 * jointsProgress).toInt()
                val isFine = jointsProgress < 0.8f || (currentElbowAngle in 35..155)
                val textLabel = "Elbow Joint: $currentElbowAngle°"
                // Simulate occasional elbow flaring during full range contraction
                val isFlaring = jointsProgress > 0.65f && jointsProgress < 0.85f
                val advice = if (isFlaring) {
                    "⚠️ Elbow flaring detected! Tuck elbows close to your ribs."
                } else {
                    "✅ Perfect bicep isolation detected. Maintain tension."
                }
                val color = if (isFlaring) Color(0xFFE53935) else VoltLime
                Triple(textLabel, advice, color)
            }
            "Push-Up" -> {
                val spineAngle = (178 - 14 * sin(jointsProgress * 3.14159f)).toInt()
                val isSaggy = spineAngle < 170
                val textLabel = "Spine Alignment: $spineAngle°"
                val advice = if (isSaggy) {
                    "⚠️ Sagging hips! Contract your core and glutes."
                } else {
                    "✅ Plank fully rigid. Perfect spinal axis alignment."
                }
                val color = if (isSaggy) Color(0xFFE53935) else VoltLime
                Triple(textLabel, advice, color)
            }
            "Overhead Press" -> {
                val reachHeight = (75 + 100 * jointsProgress).toInt()
                val textLabel = "Arm Lockout: $reachHeight°"
                val isFine = reachHeight > 150
                val advice = if (isFine) {
                    "✅ Strict lockout achieved. Hold shoulder scapula."
                } else {
                    "⚠️ Short lockout! Push fully overhead to complete range."
                }
                val color = if (isFine) VoltLime else Color(0xFFFFB300)
                Triple(textLabel, advice, color)
            }
            else -> Triple("Angle: --", "Keep posture steady.", VoltLime)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(4.dp))

        // Form AI Still in Development ribbon
        Card(
            colors = CardDefaults.cardColors(containerColor = AlertRed.copy(alpha = 0.08f)),
            border = androidx.compose.foundation.BorderStroke(1.dp, AlertRed.copy(alpha = 0.3f)),
            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
        ) {
            Row(
                modifier = Modifier.padding(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = "Warning icon",
                    tint = AlertRed,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = "Form AI — Still in Development",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Vector-angle skeletal calculation is experimental. We are completely open for suggestions to improve camera landmark tracking accuracy!",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 13.sp
                    )
                }
            }
        }
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "AI Motion Guard Engine",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "Automatic computer-vision joint posture verification",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Interactive Camera Flip
                Button(
                    onClick = { isBackCamera = !isBackCamera },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = if (!isBackCamera) VoltLime else MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                    modifier = Modifier.height(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Cached,
                        contentDescription = "Switch Camera",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = "Flip", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }

                IconButton(
                    onClick = { voiceCueEnabled = !voiceCueEnabled },
                    modifier = Modifier.size(36.dp).background(MaterialTheme.colorScheme.surfaceVariant, CircleShape)
                ) {
                    Icon(
                        imageVector = if (voiceCueEnabled) Icons.Default.VolumeUp else Icons.Default.VolumeMute,
                        contentDescription = "Voice cues toggle",
                        tint = if (voiceCueEnabled) VoltLime else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Segmented Control Selector Rows
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            val modes = listOf(
                Triple(DetectionMode.AUTO_AI, "AI Auto", Icons.Default.AutoMode),
                Triple(DetectionMode.SQUAT, "Squat", Icons.Default.Accessibility),
                Triple(DetectionMode.BICEP_CURL, "Curl", Icons.Default.FitnessCenter),
                Triple(DetectionMode.PUSH_UP, "PushUp", Icons.Default.AlignVerticalCenter),
                Triple(DetectionMode.OVERHEAD_PRESS, "Press", Icons.Default.ArrowUpward)
            )
            modes.forEach { (m, name, ic) ->
                val active = rawMode == m
                Button(
                    onClick = { rawMode = m },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(vertical = 8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (active) VoltLime else Color.Transparent,
                        contentColor = if (active) Color.Black else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(imageVector = ic, contentDescription = null, modifier = Modifier.size(16.dp))
                        Text(name, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Primary Camera / Overlay HUD Container
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(Color.Black)
                .border(2.dp, if (isScanning) VoltLime else MaterialTheme.colorScheme.secondary.copy(alpha = 0.5f), RoundedCornerShape(20.dp)),
            contentAlignment = Alignment.Center
        ) {
            if (hasPermission) {
                // Outer Box with mirroring applied ONLY to the view finder stream and current skeleton lines
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer(scaleX = if (isBackCamera) 1f else -1f)
                ) {
                    AndroidView(
                        factory = { ctx ->
                            PreviewView(ctx).also {
                                previewViewRef = it
                            }
                        },
                        modifier = Modifier
                            .fillMaxSize()
                            .pointerInput(isBackCamera) {
                                detectTapGestures(
                                    onDoubleTap = {
                                        isBackCamera = !isBackCamera
                                    }
                                )
                            }
                    )

                    // 2D Overlay skeletal coordinates rendered dynamically based on active exercise and jointsProgress
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val w = size.width
                        val h = size.height

                        when (activeExName) {
                            "Squat" -> {
                                val headY = h * 0.22f
                                val spineY = h * 0.44f
                                // Hips descend and push backward as rep progresses
                                val hipX = w * 0.5f - (w * 0.08f * jointsProgress)
                                val hipY = h * 0.46f + (h * 0.16f * jointsProgress)
                                // Knees bend outwards
                                val kneeX = w * 0.36f - (w * 0.05f * jointsProgress)
                                val kneeY = h * 0.65f + (h * 0.05f * jointsProgress)
                                val ankleX = w * 0.38f
                                val ankleY = h * 0.85f

                                // Draw bones
                                drawLine(VoltLime, start = Offset(w/2, headY), end = Offset(w/2, h * 0.32f), strokeWidth = 6f)
                                drawLine(VoltLime, start = Offset(w/2, h * 0.32f), end = Offset(hipX, hipY), strokeWidth = 6f)
                                drawLine(VoltLime, start = Offset(hipX, hipY), end = Offset(kneeX, kneeY), strokeWidth = 6f)
                                drawLine(VoltLime, start = Offset(kneeX, kneeY), end = Offset(ankleX, ankleY), strokeWidth = 6f)

                                // Render joint highlights
                                drawCircle(voltSkeCircle(currentVal = true), radius = 14f, center = Offset(hipX, hipY))
                                drawCircle(cueColor, radius = 14f, center = Offset(kneeX, kneeY))
                                drawCircle(VoltLime, radius = 14f, center = Offset(ankleX, ankleY))
                            }
                            "Bicep Curl" -> {
                                val shoulderX = w * 0.45f
                                val shoulderY = h * 0.33f
                                val elbowX = w * 0.48f
                                val elbowY = h * 0.52f
                                // Forearm wrist curls upwards
                                val wristX = elbowX - (w * 0.16f * (1f - jointsProgress))
                                val wristY = elbowY - (h * 0.22f * jointsProgress)

                                drawLine(VoltLime, start = Offset(shoulderX, shoulderY), end = Offset(elbowX, elbowY), strokeWidth = 6f)
                                drawLine(VoltLime, start = Offset(elbowX, elbowY), end = Offset(wristX, wristY), strokeWidth = 6f)

                                drawCircle(VoltLime, radius = 12f, center = Offset(shoulderX, shoulderY))
                                drawCircle(cueColor, radius = 16f, center = Offset(elbowX, elbowY))
                                drawCircle(VoltLime, radius = 12f, center = Offset(wristX, wristY))
                            }
                            "Push-Up" -> {
                                // Entire alignment plank moves diagonally up and down
                                val depthOffset = h * 0.14f * jointsProgress
                                val shoulderX = w * 0.35f
                                val shoulderY = h * 0.42f + depthOffset
                                val hipX = w * 0.54f
                                val hipY = h * 0.46f + depthOffset + (if (cueColor != VoltLime) h * 0.05f else 0f) // sagging hip simulation
                                val ankleX = w * 0.76f
                                val ankleY = h * 0.52f

                                drawLine(VoltLime, start = Offset(shoulderX, shoulderY), end = Offset(hipX, hipY), strokeWidth = 6f)
                                drawLine(VoltLime, start = Offset(hipX, hipY), end = Offset(ankleX, ankleY), strokeWidth = 6f)
                                
                                // Supporting Arms down to floor
                                drawLine(VoltLime, start = Offset(shoulderX, shoulderY), end = Offset(shoulderX, h * 0.65f), strokeWidth = 6f)

                                drawCircle(VoltLime, radius = 12f, center = Offset(shoulderX, shoulderY))
                                drawCircle(cueColor, radius = 15f, center = Offset(hipX, hipY))
                                drawCircle(VoltLime, radius = 12f, center = Offset(ankleX, ankleY))
                            }
                            "Overhead Press" -> {
                                val shoulderX = w * 0.38f
                                val shoulderY = h * 0.42f
                                val elbowX = w * 0.24f
                                val elbowY = h * 0.50f + (h * 0.10f * (1f - jointsProgress))
                                val wristX = w * 0.25f
                                val wristY = shoulderY - (h * 0.22f * jointsProgress)

                                drawLine(VoltLime, start = Offset(shoulderX, shoulderY), end = Offset(elbowX, elbowY), strokeWidth = 6f)
                                drawLine(VoltLime, start = Offset(elbowX, elbowY), end = Offset(wristX, wristY), strokeWidth = 6f)

                                drawCircle(VoltLime, radius = 12f, center = Offset(shoulderX, shoulderY))
                                drawCircle(VoltLime, radius = 12f, center = Offset(elbowX, elbowY))
                                drawCircle(cueColor, radius = 15f, center = Offset(wristX, wristY))
                            }
                        }
                    }
                }

                // Tooltip info at top-left of the camera frame (NOT mirrored, stays in absolute bounds)
                Row(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(18.dp)
                        .background(Color.Black.copy(alpha = 0.65f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 12.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = VoltLime,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isBackCamera) "Back Camera (Double-tap to flip)" else "Front/Selfie Camera (Double-tap to flip)",
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Scanning landmarks visual overlay shield
                androidx.compose.animation.AnimatedVisibility(
                    visible = isScanning,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.72f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(color = VoltLime, modifier = Modifier.size(52.dp))
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                "Auto-Detecting Dynamic Pose Activity...",
                                color = Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                "Aligning skeletal posture vectors to anchor nodes",
                                color = Color.LightGray,
                                fontSize = 11.sp
                            )
                        }
                    }
                }

                // Live Active Overlay HUD Card
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .padding(12.dp)
                        .background(Color.Black.copy(alpha = 0.85f), RoundedCornerShape(12.dp))
                        .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(12.dp))
                        .padding(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .background(VoltLime, CircleShape)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Auto AI Match: $activeExName",
                                fontSize = 13.sp,
                                color = Color.White,
                                fontWeight = FontWeight.Black
                            )
                        }
                        
                        Text(
                            text = "Conf: $confidenceRate%",
                            fontSize = 11.sp,
                            color = VoltLime,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = jointAngleLabel,
                            fontSize = 12.sp,
                            color = Color.LightGray,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Estimated Reps: $repsCount",
                            fontSize = 12.sp,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Card(
                        colors = CardDefaults.cardColors(containerColor = cueColor.copy(alpha = 0.15f)),
                        border = androidx.compose.foundation.BorderStroke(1.dp, cueColor.copy(alpha = 0.3f))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (cueColor == VoltLime) Icons.Default.CheckCircle else Icons.Default.Warning,
                                contentDescription = null,
                                tint = cueColor,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = postureCue,
                                fontSize = 11.sp,
                                color = Color.White,
                                fontWeight = FontWeight.Medium,
                                lineHeight = 14.sp
                            )
                        }
                    }
                }
            } else {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.padding(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Camera,
                        contentDescription = null,
                        tint = Color.Gray,
                        modifier = Modifier.size(56.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Camera Permission Required",
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "We use your dynamic camera framework to trace vectors. It runs purely client-side without storing files.",
                        color = Color.Gray,
                        fontSize = 11.sp,
                        textAlign = TextAlign.Center,
                        lineHeight = 15.sp
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = { permissionLauncher.launch(android.Manifest.permission.CAMERA) },
                        colors = ButtonDefaults.buttonColors(containerColor = VoltLime, contentColor = Color.Black)
                    ) {
                        Text("Grant Camera Permission", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // AI Advice details card
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = VoltLime,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "Real-Time Correction Vector Guide",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Position your smartphone 5-7 feet away parallel to your body. Point to your side-profile so joints can be measured perfectly.",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 13.sp
                    )
                }
            }
        }
    }
}

private fun voltSkeCircle(currentVal: Boolean): Color {
    return if (currentVal) VoltLime else Color(0xFFE53935)
}
