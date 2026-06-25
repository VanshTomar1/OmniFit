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
import com.example.ui.theme.DarkSpaceCharcoal
import com.example.ui.theme.CarbonObsidian
import com.example.ui.theme.SlateStroke
import com.example.ui.theme.IceWhite
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
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        OmniFitLogo(modifier = Modifier.size(50.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text("OMNIFIT", color = Color.White, fontWeight = FontWeight.Black, fontSize = 15.sp)
                            Text("Next-Gen Wearable Intelligence", color = VoltLime, fontWeight = FontWeight.Bold, fontSize = 10.sp)
                        }
                    }
                    TextButton(onClick = { 
                        onIntroComplete()
                        activeSlideIndex = 3 
                    }) {
                        Text("Skip", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Black, fontSize = 12.sp)
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Premium Technology Mode Indicator Badge
                Box(
                    modifier = Modifier
                        .background(
                            Brush.linearGradient(
                                listOf(VoltLime.copy(alpha = 0.15f), SportsTeal.copy(alpha = 0.15f))
                            ),
                            RoundedCornerShape(8.dp)
                        )
                        .border(
                            width = 1.dp,
                            brush = Brush.linearGradient(listOf(VoltLime, SportsTeal)),
                            shape = RoundedCornerShape(8.dp)
                        )
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                        .align(Alignment.Start)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .background(VoltLime, CircleShape)
                        )
                        Text(
                            text = "LIVE SYSTEM MODULE PREVIEW : SLIDE 0${activeSlideIndex + 1} / 03",
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            color = VoltLime,
                            letterSpacing = 1.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .wrapContentHeight()
                        .border(
                            width = 1.dp,
                            brush = Brush.linearGradient(listOf(VoltLime.copy(alpha = 0.5f), SportsTeal.copy(alpha = 0.2f))),
                            shape = RoundedCornerShape(20.dp)
                        ),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(2.dp)
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
                                    MockDashboardScreenshot()
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        "1. Dashboard & Smart Metrics Hub",
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Black,
                                        color = VoltLime,
                                        textAlign = TextAlign.Center
                                    )
                                    Text(
                                        "Where: Located on the first navigation tab (Dashboard Screen).",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = VoltLime,
                                        textAlign = TextAlign.Center
                                    )
                                    Text(
                                        "OmniFit converts real-time hardware signals and BLE broadcasts into dynamic metabolic scores, updating continuously as you train.",
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
                                            Text("Passive Acceleration Core", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface)
                                        }
                                        Text("Integrates low-pass digital filters. Only physical acceleration triggers with specific metabolic amplitudes are counted as actual physical steps.", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, lineHeight = 13.sp)

                                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 4.dp)) {
                                            Icon(Icons.Filled.LocalFireDepartment, contentDescription = null, tint = VoltLime, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text("Live Smartwear Telemetry Streams", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface)
                                        }
                                        Text("Couples with smartwatches, rings, and chest bands. Displays a pulsing real-time heart rate and automatically propagates athletic calorie registers.", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, lineHeight = 13.sp)
                                    }
                                }
                            }
                            1 -> {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    MockWorkoutScreenshot()
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        "2. Workout & Muscle Heatmaps",
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Black,
                                        color = SportsTeal,
                                        textAlign = TextAlign.Center
                                    )
                                    Text(
                                        "Where: Located on the second navigation tab (Workouts Screen).",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = SportsTeal,
                                        textAlign = TextAlign.Center
                                    )
                                    Text(
                                        "Auto-curates weekly schedules, custom split cards, and monitors your active training intensity over time.",
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
                                            Text("48-Hour Accumulation Decay", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface)
                                        }
                                        Text("Calculates muscular load decay exponentially. Warns you before overstraining specific regions so you avoid injuries.", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, lineHeight = 13.sp)

                                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 4.dp)) {
                                            Icon(Icons.Filled.FitnessCenter, contentDescription = null, tint = SportsTeal, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text("Synergy Split Calculations", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface)
                                        }
                                        Text("Distributes workloads proportionally based on compound barbell geometries (e.g. chest press counts chest 100%, triceps 20% load).", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, lineHeight = 13.sp)
                                    }
                                }
                            }
                            2 -> {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    MockCoachScreenshot()
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        "3. AI Coach & Connected Hub",
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Black,
                                        color = MaterialTheme.colorScheme.error,
                                        textAlign = TextAlign.Center
                                    )
                                    Text(
                                        "Where: Located on the third (Coach) and fourth (Profile) tabs.",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.error,
                                        textAlign = TextAlign.Center
                                    )
                                    Text(
                                        "Integrates professional coaching bots alongside deep-profile personalization settings and biometric hardware connections.",
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
                                            Icon(Icons.Filled.Face, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text("Personalized AI AI Trainer", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface)
                                        }
                                        Text("Get precise answers from professional Gemini models about recovery techniques, macro nutrition splits, and workout modifications.", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, lineHeight = 13.sp)

                                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 4.dp)) {
                                            Icon(Icons.Filled.Settings, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text("Universal Smart Devices Terminal", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface)
                                        }
                                        Text("Configure active Bluetooth scanners, register custom wearable products, grant biometric permissions, and view active power diagnostics.", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, lineHeight = 13.sp)
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
                            val label = when(lvl) {
                                "Beginner" -> "🔰 Beginner"
                                "Intermediate" -> "⚡ Intermed"
                                else -> "🔥 Advanced"
                            }
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
                                    text = label,
                                    color = if (isSelected) MaterialTheme.colorScheme.onTertiary else MaterialTheme.colorScheme.onSurface,
                                    fontSize = 11.sp,
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

@Composable
fun SmartphoneMockupContainer(
    title: String,
    systemBarLinkedText: String,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(310.dp)
            .border(
                width = 3.dp,
                brush = Brush.verticalGradient(listOf(Color(0xFF3A3D46), Color(0xFF1E2025))),
                shape = RoundedCornerShape(24.dp)
            ),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0C0D0E))
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Status bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.Black)
                    .padding(horizontal = 14.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Time
                Text(
                    text = "09:41 AM",
                    color = Color.LightGray,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold
                )
                // Notch
                Box(
                    modifier = Modifier
                        .width(45.dp)
                        .height(10.dp)
                        .background(Color(0xFF16181C), RoundedCornerShape(bottomStart = 6.dp, bottomEnd = 6.dp))
                )
                // System stats
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = systemBarLinkedText,
                        color = if (systemBarLinkedText.contains("●")) VoltLime else Color.LightGray,
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text("🔋 98%", color = Color.LightGray, fontSize = 8.sp)
                }
            }
            // App Navigation Bar simulation
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF16181C))
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    color = Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(
                        modifier = Modifier
                            .size(14.dp)
                            .background(VoltLime.copy(alpha = 0.15f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Circle,
                            contentDescription = null,
                            tint = VoltLime,
                            modifier = Modifier.size(6.dp)
                        )
                    }
                }
            }
            // Screen content
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .background(Color(0xFF0C0D0E))
                    .padding(10.dp)
            ) {
                content()
            }
        }
    }
}

@Composable
fun MockDashboardScreenshot() {
    SmartphoneMockupContainer(
        title = "OMNIFIT DASHBOARD",
        systemBarLinkedText = "● ACTIVE FEED"
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            // First row: Circular Rings and primary stats
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(95.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Multi-Ring Box with Glassmorphism
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .background(Color(0xFF16181C), RoundedCornerShape(12.dp))
                        .border(1.dp, Brush.linearGradient(listOf(VoltLime.copy(alpha = 0.4f), Color.Transparent)), RoundedCornerShape(12.dp))
                        .padding(8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(modifier = Modifier.size(58.dp)) {
                        val strokeWidth = 4.dp.toPx()
                        val centerX = size.width / 2f
                        val centerY = size.height / 2f

                        // Outer Ring: Activity (VoltLime)
                        val r1 = 24.dp.toPx()
                        drawCircle(color = Color(0xFF2C2F36), radius = r1, style = androidx.compose.ui.graphics.drawscope.Stroke(strokeWidth))
                        drawArc(
                            color = VoltLime,
                            startAngle = -90f,
                            sweepAngle = 295f,
                            useCenter = false,
                            topLeft = Offset(centerX - r1, centerY - r1),
                            size = androidx.compose.ui.geometry.Size(r1 * 2, r1 * 2),
                            style = androidx.compose.ui.graphics.drawscope.Stroke(strokeWidth, cap = androidx.compose.ui.graphics.StrokeCap.Round)
                        )

                        // Middle Ring: Cardio (SportsTeal)
                        val r2 = 17.dp.toPx()
                        drawCircle(color = Color(0xFF2C2F36), radius = r2, style = androidx.compose.ui.graphics.drawscope.Stroke(strokeWidth))
                        drawArc(
                            color = SportsTeal,
                            startAngle = -120f,
                            sweepAngle = 230f,
                            useCenter = false,
                            topLeft = Offset(centerX - r2, centerY - r2),
                            size = androidx.compose.ui.geometry.Size(r2 * 2, r2 * 2),
                            style = androidx.compose.ui.graphics.drawscope.Stroke(strokeWidth, cap = androidx.compose.ui.graphics.StrokeCap.Round)
                        )

                        // Inner Ring: Rest & Hypertrophy (FlameOrange)
                        val r3 = 11.dp.toPx()
                        drawCircle(color = Color(0xFF2C2F36), radius = r3, style = androidx.compose.ui.graphics.drawscope.Stroke(strokeWidth))
                        drawArc(
                            color = Color(0xFFFF7043),
                            startAngle = -45f,
                            sweepAngle = 180f,
                            useCenter = false,
                            topLeft = Offset(centerX - r3, centerY - r3),
                            size = androidx.compose.ui.geometry.Size(r3 * 2, r3 * 2),
                            style = androidx.compose.ui.graphics.drawscope.Stroke(strokeWidth, cap = androidx.compose.ui.graphics.StrokeCap.Round)
                        )
                    }
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.align(Alignment.Center)
                    ) {
                        Text("🔥", fontSize = 10.sp)
                    }
                }

                // Primary numbers
                Column(
                    modifier = Modifier
                        .weight(1.3f)
                        .fillMaxHeight()
                        .background(Color(0xFF16181C), RoundedCornerShape(12.dp))
                        .border(1.dp, Brush.linearGradient(listOf(SportsTeal.copy(alpha = 0.4f), Color.Transparent)), RoundedCornerShape(12.dp))
                        .padding(8.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("STAMINA BAR", color = Color.White, fontSize = 7.sp, fontWeight = FontWeight.Black)
                        Box(
                            modifier = Modifier
                                .background(VoltLime.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                                .padding(horizontal = 4.dp, vertical = 1.dp)
                        ) {
                            Text("OPTIMAL", color = VoltLime, fontSize = 6.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    Text("92.4 %", color = VoltLime, fontSize = 16.sp, fontWeight = FontWeight.Black)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("🏃 12.4 km", color = Color.LightGray, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                        Text("⚡ 620 kcal", color = Color(0xFFFF1744), fontSize = 8.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Second row: Pulse Live telemetry card with simulated ECG wave
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF16181C), RoundedCornerShape(12.dp))
                    .border(1.dp, Color(0xFF2C2F36), RoundedCornerShape(12.dp))
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1.2f)) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .background(Color.Red.copy(alpha = 0.15f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("❤️", fontSize = 11.sp)
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Column {
                        Text("GARMIN WATCH ANT+", color = Color.LightGray, fontSize = 7.sp, fontWeight = FontWeight.SemiBold)
                        Text("Pulsing Telemetry Sync", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    }
                }

                // Drawing dynamic heartbeat line in canvas!
                Canvas(modifier = Modifier
                    .weight(1f)
                    .height(18.dp)
                    .padding(horizontal = 4.dp)) {
                    val w = size.width
                    val h = size.height
                    val path = Path().apply {
                        moveTo(0f, h * 0.5f)
                        lineTo(w * 0.2f, h * 0.5f)
                        lineTo(w * 0.3f, h * 0.2f)
                        lineTo(w * 0.4f, h * 0.8f)
                        lineTo(w * 0.5f, h * 0.1f)
                        lineTo(w * 0.6f, h * 0.9f)
                        lineTo(w * 0.7f, h * 0.5f)
                        lineTo(w * 0.8f, h * 0.5f)
                        lineTo(w * 0.85f, h * 0.4f)
                        lineTo(w * 0.9f, h * 0.5f)
                        lineTo(w, h * 0.5f)
                    }
                    drawPath(
                        path = path,
                        color = Color.Red,
                        style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.5.dp.toPx(), cap = androidx.compose.ui.graphics.StrokeCap.Round)
                    )
                }

                Text("132 bpm", color = Color.Red, fontSize = 11.sp, fontWeight = FontWeight.Black)
            }

            // Third row: Simple weekly performance bar layout with glowing highlights
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .background(Color(0xFF16181C), RoundedCornerShape(12.dp))
                    .padding(6.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("ACTIVE CALORIC EXPONENTIAL INTENSITY", color = Color.Gray, fontSize = 7.sp, fontWeight = FontWeight.Bold)
                    Text("WEEKLY AVERAGE", color = VoltLime, fontSize = 6.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    val heights = listOf(0.35f, 0.65f, 0.25f, 0.8f, 0.45f, 0.95f, 0.15f)
                    val days = listOf("MON", "TUE", "WED", "THU", "FRI", "SAT", "SUN")
                    heights.forEachIndexed { idx, hFactor ->
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .width(7.dp)
                                    .fillMaxHeight(hFactor)
                                    .background(
                                        brush = Brush.verticalGradient(
                                            if (idx == 5) listOf(VoltLime, VoltLime.copy(alpha = 0.4f))
                                            else listOf(SportsTeal, SportsTeal.copy(alpha = 0.3f))
                                        ),
                                        shape = RoundedCornerShape(topStart = 3.dp, topEnd = 3.dp)
                                    )
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(days[idx], color = Color.LightGray, fontSize = 5.5.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MockWorkoutScreenshot() {
    SmartphoneMockupContainer(
        title = "WORKOUT SPLITS & HEATMAP",
        systemBarLinkedText = "● ACTIVE TIMER"
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            // First section: Interactive Muscle Strain & Synergy Splits
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1.3f),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Muscle heatmap widget (Simulating human form highlights)
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .background(Color(0xFF16181C), RoundedCornerShape(12.dp))
                        .border(1.dp, VoltLime.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
                        .padding(6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("MUSCLE FATIGUE MAP", color = Color.Gray, fontSize = 7.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(3.dp))
                        // Draw a stylized vector muscle form
                        Canvas(modifier = Modifier.size(56.dp)) {
                            val w = size.width
                            val h = size.height
                            val cx = w / 2f
                            // Head
                            drawCircle(color = Color(0xFF2C2F36), center = Offset(cx, h * 0.12f), radius = h * 0.07f)
                            // Torso (highlighted pectorals/chest)
                            val torsoPath = Path().apply {
                                moveTo(cx - 11f, h * 0.22f)
                                lineTo(cx + 11f, h * 0.22f)
                                lineTo(cx + 9f, h * 0.62f)
                                lineTo(cx - 9f, h * 0.62f)
                                close()
                            }
                            drawPath(torsoPath, color = Color(0xFF2C2F36))
                            // Left Chest half (hypertrophy load VoltLime)
                            drawRoundRect(
                                color = VoltLime,
                                topLeft = Offset(cx - 10f, h * 0.28f),
                                size = androidx.compose.ui.geometry.Size(9f, h * 0.11f),
                                cornerRadius = androidx.compose.ui.geometry.CornerRadius(2.dp.toPx())
                            )
                            // Right Chest half (hypertrophy load VoltLime)
                            drawRoundRect(
                                color = VoltLime,
                                topLeft = Offset(cx + 1f, h * 0.28f),
                                size = androidx.compose.ui.geometry.Size(9f, h * 0.11f),
                                cornerRadius = androidx.compose.ui.geometry.CornerRadius(2.dp.toPx())
                            )
                            // Left arm with active high-strain biceps glow
                            drawLine(color = SportsTeal, start = Offset(cx - 11f, h * 0.25f), end = Offset(cx - 20f, h * 0.52f), strokeWidth = 6f)
                            drawCircle(color = VoltLime, center = Offset(cx - 17.5f, h * 0.38f), radius = 4f)

                            // Right arm
                            drawLine(color = SportsTeal, start = Offset(cx + 11f, h * 0.25f), end = Offset(cx + 20f, h * 0.52f), strokeWidth = 6f)
                            drawCircle(color = SportsTeal, center = Offset(cx + 17.5f, h * 0.38f), radius = 4f)

                            // Lower body hips
                            drawLine(color = Color(0xFF2C2F36), start = Offset(cx - 9f, h * 0.62f), end = Offset(cx - 14f, h * 0.95f), strokeWidth = 5f)
                            drawLine(color = Color(0xFF2C2F36), start = Offset(cx + 9f, h * 0.62f), end = Offset(cx + 14f, h * 0.95f), strokeWidth = 5f)
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(4.dp).background(VoltLime, CircleShape))
                            Text("CHEST SYNERGY 94%", color = VoltLime, fontSize = 6.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                // Active Split Card list with nice status pill
                Column(
                    modifier = Modifier
                        .weight(1.2f)
                        .fillMaxHeight()
                        .background(Color(0xFF16181C), RoundedCornerShape(12.dp))
                        .border(1.dp, SportsTeal.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
                        .padding(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text("ACTIVE SPLIT FORMULA", color = Color.Gray, fontSize = 7.sp, fontWeight = FontWeight.Black)
                    Text("Push Day 1 Focus", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)

                    Spacer(modifier = Modifier.height(2.dp))
                    listOf(
                        "💥 Bench Press ~ 100% Chest",
                        "⚡ Incline Dumbbell ~ 85% Delt",
                        "🧪 Overhead Press ~ 40% Tri",
                        "💎 Tricep Dip ~ 80% Triceps"
                    ).forEach { item ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(item, color = Color.LightGray, fontSize = 6.sp, maxLines = 1)
                        }
                    }
                }
            }

            // Second section: 48H Exponential load decay countdown widget with live progress bar
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF16181C), RoundedCornerShape(12.dp))
                    .border(1.dp, Color(0xFF2C2F36), RoundedCornerShape(12.dp))
                    .padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.NetworkCheck, contentDescription = null, tint = SportsTeal, modifier = Modifier.size(12.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Column {
                            Text("SYSTEM MUSCULAR DECAY WINDOW", color = Color.LightGray, fontSize = 7.sp, fontWeight = FontWeight.SemiBold)
                            Text("Chest recovery decaying exponentially", color = Color.White, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    Box(
                        modifier = Modifier
                            .background(SportsTeal.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                            .padding(horizontal = 5.dp, vertical = 2.dp)
                    ) {
                        Text("18H 34M Left", color = SportsTeal, fontSize = 7.sp, fontWeight = FontWeight.Black)
                    }
                }

                // Recovery Progress indicator
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp)
                        .background(Color(0xFF2C2F36), RoundedCornerShape(1.5.dp))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.62f)
                            .fillMaxHeight()
                            .background(SportsTeal, RoundedCornerShape(1.5.dp))
                    )
                }
            }
        }
    }
}

@Composable
fun MockCoachScreenshot() {
    SmartphoneMockupContainer(
        title = "OMNIFIT AI TRAINER (GEMINI)",
        systemBarLinkedText = "● ACTIVE BOT"
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            // Chat bubbles with sparkles indicator
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1.3f)
                    .background(Color(0xFF16181C), RoundedCornerShape(12.dp))
                    .border(1.dp, VoltLime.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
                    .padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(16.dp)
                                .background(VoltLime.copy(alpha = 0.15f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("✨", fontSize = 8.sp)
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("GEMINI HEALTH ASSISTANT v1.5", color = VoltLime, fontSize = 7.sp, fontWeight = FontWeight.Black)
                    }
                    Box(
                        modifier = Modifier
                            .background(Color(0xFF2C2F36), RoundedCornerShape(4.dp))
                            .padding(horizontal = 4.dp, vertical = 1.dp)
                    ) {
                        Text("ONLINE", color = Color.White, fontSize = 5.5.sp, fontWeight = FontWeight.Bold)
                    }
                }

                // Chat bubble user
                Box(
                    modifier = Modifier
                        .align(Alignment.End)
                        .background(Color(0xFF2C2F36), RoundedCornerShape(topStart = 8.dp, bottomStart = 8.dp, bottomEnd = 8.dp))
                        .padding(6.dp)
                ) {
                    Text("My shoulder feels tight during dips.", color = Color.White, fontSize = 7.sp)
                }

                // Chat bubble system with beautiful recommendation styling
                Box(
                    modifier = Modifier
                        .align(Alignment.Start)
                        .background(Color(0xFF0C0D0E), RoundedCornerShape(topEnd = 8.dp, bottomStart = 8.dp, bottomEnd = 8.dp))
                        .border(1.dp, VoltLime.copy(alpha = 0.25f), RoundedCornerShape(topEnd = 8.dp, bottomStart = 8.dp, bottomEnd = 8.dp))
                        .padding(6.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            "OmniFit Recommendation:",
                            color = VoltLime,
                            fontSize = 7.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "Swap heavy dips with seated cable flyes. This eliminates deltoid synergy load dynamically while preserving pectoral progress.",
                            color = Color.LightGray,
                            fontSize = 6.5.sp,
                            lineHeight = 9.sp
                        )
                    }
                }
            }

            // Wearable synchronizer stream live status with BLE signals
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF16181C), RoundedCornerShape(12.dp))
                    .border(1.dp, Color(0xFF2C2F36), RoundedCornerShape(12.dp))
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("⌚", fontSize = 13.sp)
                    Spacer(modifier = Modifier.width(6.dp))
                    Column {
                        Text("ACTIVE BLUETOOTH SCANNERS", color = Color.LightGray, fontSize = 7.sp, fontWeight = FontWeight.SemiBold)
                        Text("Whoop Strain Band BLE (A4:8F)", color = Color.White, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .background(VoltLime, CircleShape)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("STREAMING", color = VoltLime, fontSize = 7.sp, fontWeight = FontWeight.Black)
                }
            }
        }
    }
}
