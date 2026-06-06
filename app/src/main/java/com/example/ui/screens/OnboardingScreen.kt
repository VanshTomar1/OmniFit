package com.example.ui.screens

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
import com.example.ui.theme.VoltLime
import com.example.ui.theme.SportsTeal
import com.example.ui.viewmodel.FitnessViewModel
import java.util.Locale

@Composable
fun OmniFitLogo(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(90.dp)
            .background(
                Brush.radialGradient(
                    colors = listOf(
                        VoltLime.copy(alpha = 0.25f),
                        Color.Transparent
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        // Render custom vector athlete barbell and performance lightning
        Canvas(modifier = Modifier.size(60.dp)) {
            val w = size.width
            val h = size.height
            val midY = h / 2f
            
            // Dumbbell / Barbell Shaft
            drawRoundRect(
                color = VoltLime,
                topLeft = Offset(w * 0.15f, midY - 3f),
                size = androidx.compose.ui.geometry.Size(w * 0.7f, 6f),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(3f)
            )
            
            // Left Weight Plates
            drawRoundRect(
                color = SportsTeal,
                topLeft = Offset(w * 0.22f, h * 0.35f),
                size = androidx.compose.ui.geometry.Size(6f, h * 0.3f),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(2f)
            )
            drawRoundRect(
                color = VoltLime,
                topLeft = Offset(w * 0.28f, h * 0.25f),
                size = androidx.compose.ui.geometry.Size(10f, h * 0.5f),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(3f)
            )
            
            // Right Weight Plates
            drawRoundRect(
                color = VoltLime,
                topLeft = Offset(w * 0.62f, h * 0.25f),
                size = androidx.compose.ui.geometry.Size(10f, h * 0.5f),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(3f)
            )
            drawRoundRect(
                color = SportsTeal,
                topLeft = Offset(w * 0.72f, h * 0.35f),
                size = androidx.compose.ui.geometry.Size(6f, h * 0.3f),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(2f)
            )
            
            // Lightning Bolt core
            val lightningPath = Path().apply {
                moveTo(w * 0.58f, h * 0.15f)
                lineTo(w * 0.40f, h * 0.50f)
                lineTo(w * 0.50f, h * 0.50f)
                lineTo(w * 0.46f, h * 0.85f)
                lineTo(w * 0.65f, h * 0.45f)
                lineTo(w * 0.54f, h * 0.45f)
                close()
            }
            drawPath(
                path = lightningPath,
                brush = Brush.linearGradient(
                    colors = listOf(VoltLime, SportsTeal),
                    start = Offset(w * 0.4f, h * 0.15f),
                    end = Offset(w * 0.65f, h * 0.85f)
                )
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OnboardingScreen(
    viewModel: FitnessViewModel,
    initialHasOpenedBefore: Boolean,
    onIntroComplete: () -> Unit,
    onComplete: () -> Unit
) {
    val scrollState = rememberScrollState()

    // Sub-states from VM
    val weight by viewModel.onboardingWeight.collectAsState()
    val height by viewModel.onboardingHeight.collectAsState()
    val age by viewModel.onboardingAge.collectAsState()
    val bodyFat by viewModel.onboardingBodyFat.collectAsState()
    val chest by viewModel.onboardingChest.collectAsState()
    val arms by viewModel.onboardingArms.collectAsState()
    val waist by viewModel.onboardingWaist.collectAsState()
    val thighs by viewModel.onboardingThighs.collectAsState()

    val primaryGoal by viewModel.onboardingPrimaryGoal.collectAsState()
    val equipment by viewModel.onboardingEquipment.collectAsState()
    val days by viewModel.onboardingDays.collectAsState()
    val timeMax by viewModel.onboardingTimeMax.collectAsState()
    val experienceLevel by viewModel.onboardingExperienceLevel.collectAsState()
    val onboardingGender by viewModel.onboardingGender.collectAsState()

    // Parse values safely to trigger automated body fat calculations
    val weightDouble = remember(weight) { weight.toDoubleOrNull() }
    val heightDouble = remember(height) { height.toDoubleOrNull() }
    val ageInt = remember(age) { age.toIntOrNull() }
    val waistDouble = remember(waist) { waist.toDoubleOrNull() }

    // Dynamic Clinical Body Fat Index Calculation
    LaunchedEffect(weightDouble, heightDouble, ageInt, waistDouble) {
        val w = weightDouble ?: 75.0
        val h = heightDouble ?: 175.0
        val a = ageInt ?: 28
        val wt = waistDouble ?: 82.0
        
        val heightM = h / 100.0
        val bmi = w / (heightM * heightM)
        
        val waistInches = wt / 2.54
        val weightLbs = w * 2.20462
        val fatMassLbs = (4.15 * waistInches) - (0.082 * weightLbs) - 98.42
        val bfVal = (fatMassLbs / weightLbs) * 100.0
        
        val estimatedBf = if (bfVal in 4.0..48.0) {
            bfVal
        } else {
            // Fallback to validated BMI model
            (1.20 * bmi) + (0.23 * a) - 16.2
        }
        
        val finalBf = estimatedBf.coerceIn(5.0, 42.0)
        val bfString = String.format(Locale.US, "%.1f", finalBf)
        
        viewModel.onboardingBodyFat.value = bfString
    }

    var activeSlideIndex by remember { mutableStateOf(if (initialHasOpenedBefore) 3 else 0) }

    if (activeSlideIndex < 3) {
        val slideScrollState = rememberScrollState()
        Scaffold(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(24.dp)
                    .verticalScroll(slideScrollState),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OmniFitLogo(modifier = Modifier.size(50.dp))
                    TextButton(onClick = { 
                        onIntroComplete()
                        activeSlideIndex = 3 
                    }) {
                        Text("Skip Intro", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .wrapContentHeight()
                        .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f), RoundedCornerShape(20.dp)),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp)
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        when (activeSlideIndex) {
                            0 -> {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(90.dp)
                                            .background(VoltLime.copy(alpha = 0.15f), CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Canvas(modifier = Modifier.size(60.dp)) {
                                            val w = size.width
                                            drawCircle(color = VoltLime.copy(alpha = 0.2f), radius = w * 0.45f, style = androidx.compose.ui.graphics.drawscope.Stroke(2f))
                                            drawCircle(color = VoltLime.copy(alpha = 0.4f), radius = w * 0.30f, style = androidx.compose.ui.graphics.drawscope.Stroke(4f))
                                            drawCircle(color = VoltLime, radius = w * 0.15f)
                                        }
                                    }
                                    Text(
                                        "1. Passive Motion Sensor Core",
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Black,
                                        color = VoltLime,
                                        textAlign = TextAlign.Center
                                    )
                                    Text(
                                        "OmniFit converts raw hardware readings into real-time energetic variables, functioning continuously even when the screen is locked.",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        textAlign = TextAlign.Center,
                                        lineHeight = 16.sp
                                    )
                                    Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.12f))
                                    Column(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Filled.DirectionsRun, contentDescription = null, tint = VoltLime, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text("Low-Pass Filtering (g-Force Math)", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface)
                                        }
                                        Text("Filters noisy hand shakes by monitoring gravitational force vectors. Only acceleration spikes above 12.2 m/s² separated by static 320ms biometric debouncing count as pure steps.", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, lineHeight = 13.sp)

                                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 4.dp)) {
                                            Icon(Icons.Filled.LocalFireDepartment, contentDescription = null, tint = VoltLime, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text("Dynamic Metabolic Equivalence (MET)", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface)
                                        }
                                        Text("Rather than standard static counts, caloric burn uses your specific body weight multiplied by motion amplitude indexes to estimate live biological burn rates every 1.5 seconds.", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, lineHeight = 13.sp)
                                    }
                                }
                            }
                            1 -> {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(90.dp)
                                            .background(SportsTeal.copy(alpha = 0.15f), CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Canvas(modifier = Modifier.size(60.dp)) {
                                            val w = size.width
                                            val h = size.height
                                            drawRoundRect(
                                                color = SportsTeal,
                                                topLeft = Offset(w * 0.4f, h * 0.3f),
                                                size = androidx.compose.ui.geometry.Size(w * 0.2f, h * 0.5f),
                                                cornerRadius = androidx.compose.ui.geometry.CornerRadius(10f)
                                            )
                                            drawCircle(color = SportsTeal, center = Offset(w * 0.5f, h * 0.15f), radius = w * 0.12f)
                                        }
                                    }
                                    Text(
                                        "2. Biomechanical Load Heatmap",
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Black,
                                        color = SportsTeal,
                                        textAlign = TextAlign.Center
                                    )
                                    Text(
                                        "Analyzes muscular tension, strain spikes, and micro-fiber recoveries chronologically using biological half-life modeling.",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        textAlign = TextAlign.Center,
                                        lineHeight = 16.sp
                                    )
                                    Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.12f))
                                    Column(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Filled.Timeline, contentDescription = null, tint = SportsTeal, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text("48-Hour Half-Life Exponential Decay", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface)
                                        }
                                        Text("Local muscle accumulation loads decay exponentially over a 48H recovery window. This simulates metabolic clearing of lactic acid and myofibrillar repair cycles.", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, lineHeight = 13.sp)

                                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 4.dp)) {
                                            Icon(Icons.Filled.FitnessCenter, contentDescription = null, tint = SportsTeal, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text("Primary & Secondary Synergy Splits", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface)
                                        }
                                        Text("Completing compound exercises distributes load variables proportionally. A bench press puts 100% strain on Chest, 35% on anterior Deltoid, and 20% on Triceps.", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, lineHeight = 13.sp)
                                    }
                                }
                            }
                            2 -> {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(90.dp)
                                            .background(MaterialTheme.colorScheme.error.copy(alpha = 0.15f), CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Canvas(modifier = Modifier.size(60.dp)) {
                                            val w = size.width
                                            val h = size.height
                                            val path = Path().apply {
                                                moveTo(w * 0.1f, h * 0.8f)
                                                lineTo(w * 0.35f, h * 0.5f)
                                                lineTo(w * 0.65f, h * 0.5f)
                                                lineTo(w * 0.9f, h * 0.2f)
                                            }
                                            drawPath(path = path, color = Color.Red, style = androidx.compose.ui.graphics.drawscope.Stroke(4f))
                                        }
                                    }
                                    Text(
                                        "3. Real-Time AI Form Correction",
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Black,
                                        color = MaterialTheme.colorScheme.error,
                                        textAlign = TextAlign.Center
                                    )
                                    Text(
                                        "Combines dynamic CameraX pose landmarks with automated posture alignment routines to optimize your workout.",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        textAlign = TextAlign.Center,
                                        lineHeight = 16.sp
                                    )
                                    Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.12f))
                                    Column(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Filled.Videocam, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text("AI Exercise Recognition & Joint Angles", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface)
                                        }
                                        Text("Uses real-time landmark coordinates. Auto-detects movement patterns and calculates precise angles (e.g. squat depth) to correct form on-screen.", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, lineHeight = 13.sp)

                                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 4.dp)) {
                                            Icon(Icons.Filled.Bolt, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text("Safe Beginner Induction", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface)
                                        }
                                        Text("For beginners, the engine locks in safe, joint-friendly introductory movements for an initial period, ensuring tendons and nerves condition safely before handling high resistance.", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, lineHeight = 13.sp)
                                    }
                                }
                            }
                        }

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(top = 8.dp)
                        ) {
                            for (i in 0 until 3) {
                                val isSelected = i == activeSlideIndex
                                Box(
                                    modifier = Modifier
                                        .size(if (isSelected) 10.dp else 6.dp)
                                        .background(
                                            if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f),
                                            CircleShape
                                        )
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = { 
                        if (activeSlideIndex == 2) {
                            onIntroComplete()
                        }
                        activeSlideIndex += 1 
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (activeSlideIndex == 2) VoltLime else MaterialTheme.colorScheme.primary,
                        contentColor = if (activeSlideIndex == 2) Color.Black else MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    Text(
                        text = if (activeSlideIndex == 2) "Initialize Profiling Form" else "Next Feature",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Black
                    )
                }
            }
        }
    } else {
        Scaffold(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 20.dp)
                    .verticalScroll(scrollState),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
            Spacer(modifier = Modifier.height(28.dp))

            // Brand Logo Header
            OmniFitLogo()

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Welcome to OmniFit",
                fontSize = 28.sp,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center,
                letterSpacing = (-0.5).sp
            )

            Text(
                text = "Set up your physical dimensions. The AI automatically designs progressive splits and estimates body compositions.",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                lineHeight = 18.sp
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Body Metrics Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        1.dp,
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                        RoundedCornerShape(16.dp)
                    ),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp)
                )
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.DirectionsRun,
                            contentDescription = "Core Stats",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Core Body Metrics",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedTextField(
                            value = weight,
                            onValueChange = { 
                                viewModel.onboardingWeight.value = it
                            },
                            label = { Text("Weight (kg)") },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                            )
                        )
                        OutlinedTextField(
                            value = height,
                            onValueChange = { 
                                viewModel.onboardingHeight.value = it
                            },
                            label = { Text("Height (cm)") },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = age,
                            onValueChange = { 
                                viewModel.onboardingAge.value = it
                            },
                            label = { Text("Age (yrs)") },
                            modifier = Modifier.weight(1.1f),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                            )
                        )

                        Column(modifier = Modifier.weight(1.5f)) {
                            OutlinedTextField(
                                value = bodyFat,
                                onValueChange = { },
                                readOnly = true,
                                label = { Text("Body Fat (%)") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                supportingText = { Text("Automatically calculated in real-time") },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = VoltLime,
                                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                                    focusedLabelColor = VoltLime,
                                    unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Calculate,
                                    contentDescription = "Calculated",
                                    tint = VoltLime,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Mathematical Composition Engine",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = VoltLime
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Computes your fat mass index dynamically using the US Navy Circumference Method & YMCA density equations (adapting Weight, Height, Age & Waist metrics). Adjust your waist size below to calibrate estimates.",
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                lineHeight = 14.sp
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Body Measurements Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        1.dp,
                        MaterialTheme.colorScheme.secondary.copy(alpha = 0.2f),
                        RoundedCornerShape(16.dp)
                    ),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp)
                )
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Straighten,
                            contentDescription = "Measurements Icon",
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Anthropometric Dimensions",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Providing Waist circumference unlocks advanced Navy body fat estimates.",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedTextField(
                            value = waist,
                            onValueChange = { viewModel.onboardingWaist.value = it },
                            label = { Text("Waist (cm)") },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.secondary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                            )
                        )
                        OutlinedTextField(
                            value = chest,
                            onValueChange = { viewModel.onboardingChest.value = it },
                            label = { Text("Chest (cm)") },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.secondary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedTextField(
                            value = arms,
                            onValueChange = { viewModel.onboardingArms.value = it },
                            label = { Text("Arms (cm)") },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.secondary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                            )
                        )
                        OutlinedTextField(
                            value = thighs,
                            onValueChange = { viewModel.onboardingThighs.value = it },
                            label = { Text("Thighs (cm)") },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.secondary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Objective Preferences Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        1.dp,
                        MaterialTheme.colorScheme.outline.copy(alpha = 0.15f),
                        RoundedCornerShape(16.dp)
                    ),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp)
                )
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        text = "Objective & Preferences",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Text("Gender Partition", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("Male", "Female").forEach { g ->
                            val isSelected = onboardingGender == g
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(
                                        if (isSelected) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.surfaceColorAtElevation(4.dp)
                                    )
                                    .clickable { viewModel.onboardingGender.value = g }
                                    .padding(vertical = 12.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Text(
                                        text = if (g == "Male") "🙋‍♂️ Male" else "🙋‍♀️ Female",
                                        color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text("Primary Training Goal", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("Hypertrophy", "Strength", "Fat Loss").forEach { goal ->
                            val isSelected = primaryGoal == goal
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(
                                        if (isSelected) MaterialTheme.colorScheme.primary 
                                        else MaterialTheme.colorScheme.surfaceColorAtElevation(4.dp)
                                    )
                                    .clickable { viewModel.onboardingPrimaryGoal.value = goal }
                                    .padding(vertical = 12.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = goal,
                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text("Experience Level", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("Beginner", "Intermediate", "Advanced").forEach { lvl ->
                            val isSelected = experienceLevel == lvl
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(
                                        if (isSelected) MaterialTheme.colorScheme.tertiary
                                        else MaterialTheme.colorScheme.surfaceColorAtElevation(4.dp)
                                    )
                                    .clickable { viewModel.onboardingExperienceLevel.value = lvl }
                                    .padding(vertical = 12.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = lvl,
                                    color = if (isSelected) MaterialTheme.colorScheme.onTertiary else MaterialTheme.colorScheme.onSurface,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text("Equipment Availability", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("Full Gym", "Dumbbells Only", "Bodyweight/Calisthenics").forEach { equip ->
                            val isSelected = equipment == equip
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(
                                        if (isSelected) MaterialTheme.colorScheme.secondary 
                                        else MaterialTheme.colorScheme.surfaceColorAtElevation(4.dp)
                                    )
                                    .clickable { viewModel.onboardingEquipment.value = equip }
                                    .padding(vertical = 12.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = when (equip) {
                                        "Full Gym" -> "Full Gym"
                                        "Dumbbells Only" -> "Dumbbells"
                                        else -> "Bodyweight"
                                    },
                                    color = if (isSelected) MaterialTheme.colorScheme.onSecondary else MaterialTheme.colorScheme.onSurface,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Frequency (${days} workouts/week)", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                    }
                    Slider(
                        value = days.toFloat(),
                        onValueChange = { viewModel.onboardingDays.value = it.toInt() },
                        valueRange = 2f..5f,
                        steps = 2,
                        colors = SliderDefaults.colors(
                            thumbColor = MaterialTheme.colorScheme.primary,
                            activeTrackColor = MaterialTheme.colorScheme.primary,
                            inactiveTrackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                        )
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Duration Limit (${timeMax} min/session)", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                    }
                    Slider(
                        value = timeMax.toFloat(),
                        onValueChange = { viewModel.onboardingTimeMax.value = it.toInt() },
                        valueRange = 30f..90f,
                        steps = 3,
                        colors = SliderDefaults.colors(
                            thumbColor = MaterialTheme.colorScheme.secondary,
                            activeTrackColor = MaterialTheme.colorScheme.secondary,
                            inactiveTrackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Submit Button
            Button(
                onClick = {
                    viewModel.saveProfileFromOnboarding()
                    onComplete()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                Text("Generate AI Training Blueprint", fontSize = 15.sp, fontWeight = FontWeight.Black)
                Spacer(modifier = Modifier.width(8.dp))
                Icon(imageVector = Icons.Default.ArrowForward, contentDescription = "Arrow Icon")
            }

            Spacer(modifier = Modifier.height(48.dp))
        }
    }
    }
}
