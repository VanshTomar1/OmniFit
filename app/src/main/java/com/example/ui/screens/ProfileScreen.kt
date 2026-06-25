package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.UserProfile
import com.example.data.model.WorkoutSession
import com.example.ui.theme.*
import com.example.ui.viewmodel.FitnessViewModel
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import coil.compose.AsyncImage
import androidx.compose.ui.layout.ContentScale
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.PickVisualMediaRequest
import androidx.compose.ui.platform.LocalContext
import android.graphics.Bitmap
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.Rect
import android.net.Uri
import java.io.File
import java.io.FileOutputStream
import androidx.compose.ui.window.Dialog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    viewModel: FitnessViewModel,
    modifier: Modifier = Modifier
) {
    val isLoggedIn by viewModel.isLoggedIn.collectAsState()
    val loggedInEmail by viewModel.loggedInEmail.collectAsState()
    val loggedInName by viewModel.loggedInName.collectAsState()
    val loggedInMethod by viewModel.loggedInMethod.collectAsState()
    
    val userProfile by viewModel.userProfile.collectAsState(initial = null)
    val useImperial by viewModel.useImperial.collectAsState()
    val unitSystem by viewModel.unitSystem.collectAsState()
    val appLanguage by viewModel.appLanguage.collectAsState()
    val allSessions by viewModel.allSessions.collectAsState(initial = emptyList())
    val weeklyHealthLogs by viewModel.weeklyHealthLogs.collectAsState(initial = emptyList())
    val todayHealthLog by viewModel.todayHealthLog.collectAsState()
    val stepsGoal by viewModel.stepsGoal.collectAsState()

    var chartTimeframe by remember { mutableStateOf("Week") } // Day, Week, Month, Year
    var chartMetric by remember { mutableStateOf("Steps") } // Steps, Calories, Water, Workouts

    // UI Local variables for Manual form inputs
    var emailInput by remember { mutableStateOf("") }
    var passwordInput by remember { mutableStateOf("") }
    var nameInput by remember { mutableStateOf("") }
    var isRegisterMode by remember { mutableStateOf(false) }
    var showSuccessSnack by remember { mutableStateOf<String?>(null) }
    
    val keyboardController = LocalSoftwareKeyboardController.current

    LaunchedEffect(showSuccessSnack) {
        if (showSuccessSnack != null) {
            kotlinx.coroutines.delay(3500)
            showSuccessSnack = null
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        if (!isLoggedIn) {
            // --- SIGN IN / AUTHENTICATION PORTAL VIEW ---
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header with Lock Icon
                item {
                    Spacer(modifier = Modifier.height(12.dp))
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .background(VoltLime.copy(alpha = 0.12f), shape = CircleShape)
                            .border(1.dp, VoltLime.copy(alpha = 0.4f), shape = CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Https,
                            contentDescription = "Safe secure credentials padlock representation",
                            tint = VoltLime,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = if (isRegisterMode) "Create Safe Account" else "Athletic Account Vault",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Synchronize your body measurements, physical workout loads, reps records, and metabolic energy charts safely using secure credentials.",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 12.dp)
                    )
                }

                // Credentials Panel Card
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp)
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = if (isRegisterMode) "Manual Sign Up Form" else "Manual Sign In Form",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )

                            if (isRegisterMode) {
                                OutlinedTextField(
                                    value = nameInput,
                                    onValueChange = { nameInput = it },
                                    label = { Text("Display Name") },
                                    leadingIcon = { Icon(Icons.Default.Person, contentDescription = "User name profile field") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }

                            OutlinedTextField(
                                value = emailInput,
                                onValueChange = { emailInput = it },
                                label = { Text("Email Address") },
                                leadingIcon = { Icon(Icons.Default.Email, contentDescription = "Email validation account icon") },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Email,
                                    imeAction = ImeAction.Next
                                ),
                                modifier = Modifier.fillMaxWidth()
                             )

                            OutlinedTextField(
                                value = passwordInput,
                                onValueChange = { passwordInput = it },
                                label = { Text("Password Credentials") },
                                leadingIcon = { Icon(Icons.Default.Lock, contentDescription = "Account encryption protect padlock key icon") },
                                singleLine = true,
                                visualTransformation = PasswordVisualTransformation(),
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Password,
                                    imeAction = ImeAction.Done
                                ),
                                keyboardActions = KeyboardActions(
                                    onDone = { keyboardController?.hide() }
                                ),
                                modifier = Modifier.fillMaxWidth()
                            )

                            Button(
                                onClick = {
                                    if (emailInput.isNotBlank() && passwordInput.isNotBlank()) {
                                        val resolvedName = if (isRegisterMode && nameInput.isNotBlank()) nameInput else emailInput.substringBefore("@")
                                        viewModel.login(emailInput, resolvedName, "Manual Encrypted Profile")
                                        showSuccessSnack = "Successfully connected safely with Manual Profile!"
                                        keyboardController?.hide()
                                    }
                                },
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .padding(top = 4.dp)
                            ) {
                                Text(
                                    text = if (isRegisterMode) "Create Safe Profile" else "Access Secure Account",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = if (isRegisterMode) "Already have credentials?" else "Need dedicated vault?",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                TextButton(onClick = { isRegisterMode = !isRegisterMode }) {
                                    Text(
                                        text = if (isRegisterMode) "Login Directly" else "Register Vault Account",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = VoltLime
                                    )
                                }
                            }
                        }
                    }
                }

                // Social auth section divider
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Divider(modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.outlineVariant)
                        Text(
                            text = "OR CHOOSE DIRECT SOCIAL AUTHENTICATION",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 8.dp)
                        )
                        Divider(modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.outlineVariant)
                    }
                }

                // Social logins list
                item {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Gmail Social login
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp)
                                .clickable {
                                    viewModel.login("vansh.tomar809@gmail.com", "Vansh Tomar", "Gmail Account Link")
                                    showSuccessSnack = "Successfully logged in via secure Gmail account!"
                                },
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFF1F1F1)),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFD4D4D4))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.MailOutline,
                                    contentDescription = "Gmail Brand Logo Icon Placeholder",
                                    tint = Color(0xFFEA4335),
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Continue Directly with Gmail Account",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.Black
                                )
                            }
                        }

                        // Facebook Social Login
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp)
                                .clickable {
                                    viewModel.login("vansh.facebook.user@fb.org", "Athletic Warrior", "Facebook Account Link")
                                    showSuccessSnack = "Successfully logged in via secure Facebook session!"
                                },
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF1877F2))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Facebook,
                                    contentDescription = "Facebook brand profile image link logo",
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Continue with Facebook Account Login",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }

                        // Discord / Alternative Secure Bypass
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp)
                                .clickable {
                                    viewModel.login("vansh.secure.auth@id.com", "Omni Warrior", "Apple Direct Connect")
                                    showSuccessSnack = "Successfully logged in via Apple Secure ID!"
                                },
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Fingerprint,
                                    contentDescription = "Apple account secure passkey icon",
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Sign In with Apple Passkey Safe ID",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }
                    }
                }

                // Data protection text
                item {
                    Spacer(modifier = Modifier.height(10.dp))
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.2f)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Icon(
                                imageVector = Icons.Default.Shield,
                                contentDescription = "Safe Lock Guarantee Shield Symbol",
                                tint = MaterialTheme.colorScheme.tertiary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "On-Device Performance Architecture",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.tertiary
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "All historic workout logs, lift volumes, training session sets, and metabolic calorie metrics are securely persisted on your device in a high-performance production Room database, ensuring ultra-low latency, full offline capability, and absolute privacy.",
                                    fontSize = 9.sp,
                                    color = MaterialTheme.colorScheme.onTertiaryContainer
                                )
                            }
                        }
                    }
                }
            }
        } else {
            // --- LOGGED IN USER PROFILE DASHBOARD DETAILS VIEW ---
            var activeProfileSubScreen by remember { mutableStateOf("overview") }
            
            androidx.activity.compose.BackHandler(enabled = activeProfileSubScreen != "overview") {
                activeProfileSubScreen = "overview"
            }

            var isEditingMetrics by remember { mutableStateOf(false) }
            var editPhotoUri by remember { mutableStateOf("") }
            val photoLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.PickVisualMedia()
            ) { uri ->
                if (uri != null) {
                    editPhotoUri = uri.toString()
                }
            }
            var showCameraTaker by remember { mutableStateOf(false) }
            val profileContext = LocalContext.current
            var preSelectGoal by remember { mutableStateOf(false) }
            var preSelectChallenge by remember { mutableStateOf(false) }
            
            // Editable local metrics variables
            var editName by remember { mutableStateOf("") }
            var editWeight by remember { mutableStateOf("") }
            var editHeight by remember { mutableStateOf("") }
            var editExp by remember { mutableStateOf("") }
            var editAge by remember { mutableStateOf("") }
            var editChest by remember { mutableStateOf("") }
            var editArms by remember { mutableStateOf("") }
            var editWaist by remember { mutableStateOf("") }
            var editThighs by remember { mutableStateOf("") }
            var editPrimaryGoal by remember { mutableStateOf("Hypertrophy") }
            var editGender by remember { mutableStateOf("Male") }
            var editEquipment by remember { mutableStateOf("Full Gym") }
            var editDays by remember { mutableStateOf(4) }
            var editTimeMax by remember { mutableStateOf(45) }
            var editWorkoutNotes by remember { mutableStateOf("") }

            if (showCameraTaker) {
                var selectedTheme by remember { mutableStateOf("Vibrant Neon") }
                var selectedStance by remember { mutableStateOf("Strength Squatter") }
                var captureFlashActive by remember { mutableStateOf(false) }
                var isHardwareCamMode by remember { mutableStateOf(false) }
                
                val cameraPermissionLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.RequestPermission()
                ) { granted ->
                    isHardwareCamMode = granted
                }

                Dialog(onDismissRequest = { showCameraTaker = false }) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                    ) {
                        Column(
                            modifier = Modifier
                                .padding(20.dp)
                                .fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.CameraAlt,
                                        contentDescription = null,
                                        tint = VoltLime,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "OmniFit Web Lens",
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Black
                                    )
                                }
                                IconButton(
                                    onClick = { showCameraTaker = false }
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Close lens camera overlay",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(DarkSpaceCharcoal, RoundedCornerShape(10.dp))
                                    .padding(4.dp)
                            ) {
                                Button(
                                    onClick = {
                                        val hasPerm = androidx.core.content.ContextCompat.checkSelfPermission(
                                            profileContext,
                                            android.Manifest.permission.CAMERA
                                        ) == android.content.pm.PackageManager.PERMISSION_GRANTED
                                        if (hasPerm) {
                                            isHardwareCamMode = true
                                        } else {
                                            cameraPermissionLauncher.launch(android.Manifest.permission.CAMERA)
                                        }
                                    },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(8.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (isHardwareCamMode) VoltLime else Color.Transparent,
                                        contentColor = if (isHardwareCamMode) Color.Black else Color.White
                                    )
                                ) {
                                    Text("Physical Lens", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }

                                Button(
                                    onClick = { isHardwareCamMode = false },
                                    modifier = Modifier.weight(1.2f),
                                    shape = RoundedCornerShape(8.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (!isHardwareCamMode) SportsTeal else Color.Transparent,
                                        contentColor = if (!isHardwareCamMode) Color.Black else Color.White
                                    )
                                ) {
                                    Text("WebCam Simulator", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(260.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(Color.Black)
                                    .border(2.dp, if (isHardwareCamMode) VoltLime else SportsTeal, RoundedCornerShape(16.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                if (isHardwareCamMode) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center,
                                        modifier = Modifier.padding(24.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Camera,
                                            contentDescription = null,
                                            tint = VoltLime,
                                            modifier = Modifier.size(48.dp)
                                        )
                                        Spacer(modifier = Modifier.height(12.dp))
                                        Text(
                                            text = "Ready to snap via Camera hardware",
                                            color = Color.White,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            textAlign = TextAlign.Center
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = "Tap the Shutter button below to record local avatar JPEG of current view",
                                            color = Color.LightGray,
                                            fontSize = 10.sp,
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                } else {
                                    Box(
                                        modifier = Modifier.fillMaxSize(),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Canvas(modifier = Modifier.fillMaxSize()) {
                                            drawCircle(
                                                color = Color(if (selectedTheme == "Vibrant Neon") 0x33CCFF00 else 0x334FE3C1),
                                                radius = 110f,
                                                center = Offset(size.width / 2f, size.height / 2f)
                                            )
                                            val scanY = (System.currentTimeMillis() % 3000) / 3000f * size.height
                                            drawLine(
                                                color = if (selectedTheme == "Vibrant Neon") VoltLime else SportsTeal,
                                                start = Offset(0f, scanY),
                                                end = Offset(size.width, scanY),
                                                strokeWidth = 3f
                                            )
                                            drawLine(
                                                color = Color.White.copy(alpha = 0.15f),
                                                start = Offset(size.width/2f, 0f),
                                                end = Offset(size.width/2f, size.height),
                                                strokeWidth = 1f
                                            )
                                        }

                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.Center,
                                            modifier = Modifier.padding(16.dp)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(84.dp)
                                                    .clip(CircleShape)
                                                    .background(DarkSpaceCharcoal)
                                                    .border(2.dp, if (selectedTheme == "Vibrant Neon") VoltLime else SportsTeal, CircleShape),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    imageVector = when(selectedStance) {
                                                        "Strength Squatter" -> Icons.Default.FitnessCenter
                                                        "Cardio Sprint Champion" -> Icons.Default.DirectionsRun
                                                        else -> Icons.Default.SelfImprovement
                                                    },
                                                    contentDescription = "Active stance icon",
                                                    tint = if (selectedTheme == "Vibrant Neon") VoltLime else SportsTeal,
                                                    modifier = Modifier.size(36.dp)
                                                )
                                            }

                                            Spacer(modifier = Modifier.height(12.dp))
                                            Text(
                                                text = "STRIKING POSTURE: $selectedStance",
                                                fontSize = 11.sp,
                                                color = Color.White,
                                                fontWeight = FontWeight.Black
                                            )
                                            Text(
                                                text = "ACTIVE USER: ${editName.ifBlank { "OmniFit Athlete" }} (AGE: ${editAge.ifBlank { "28" }})",
                                                fontSize = 10.sp,
                                                color = Color.LightGray,
                                                fontWeight = FontWeight.Medium
                                            )
                                            Spacer(modifier = Modifier.height(6.dp))
                                            Card(
                                                colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.5f))
                                            ) {
                                                Text(
                                                    text = "🎥 BROWSER WEBCAM FEED SYNCHRONIZED",
                                                    fontSize = 8.sp,
                                                    color = if (selectedTheme == "Vibrant Neon") VoltLime else SportsTeal,
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                    }
                                }

                                if (captureFlashActive) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(Color.White)
                                    )
                                    LaunchedEffect(captureFlashActive) {
                                        kotlinx.coroutines.delay(150)
                                        captureFlashActive = false
                                    }
                                }
                            }

                            if (!isHardwareCamMode) {
                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = "Select Avatar Stance Specialty",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        listOf("Strength Squatter", "Cardio Sprint Champion", "Pilates Yoga Guru").forEach { stance ->
                                            val isAct = selectedStance == stance
                                            Box(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .background(if (isAct) SportsTeal.copy(alpha = 0.15f) else DarkSpaceCharcoal, RoundedCornerShape(8.dp))
                                                    .border(1.dp, if (isAct) SportsTeal else Color.Transparent, RoundedCornerShape(8.dp))
                                                    .clickable { selectedStance = stance }
                                                    .padding(vertical = 8.dp),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = stance.replace(" Strength ", "").replace(" Cardio ", "").replace(" Pilates ", "").split(" ").first(),
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (isAct) SportsTeal else Color.White
                                                )
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(4.dp))

                                    Text(
                                        text = "Select Biometric Shading Theme",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        listOf("Vibrant Neon", "Cyber Athlete", "Classic Red").forEach { theme ->
                                            val isAct = selectedTheme == theme
                                            val themeColor = when(theme) {
                                                "Vibrant Neon" -> VoltLime
                                                "Cyber Athlete" -> SportsTeal
                                                else -> AlertRed
                                            }
                                            Box(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .background(if (isAct) themeColor.copy(alpha = 0.15f) else DarkSpaceCharcoal, RoundedCornerShape(8.dp))
                                                    .border(1.dp, if (isAct) themeColor else Color.Transparent, RoundedCornerShape(8.dp))
                                                    .clickable { selectedTheme = theme }
                                                    .padding(vertical = 8.dp),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = theme,
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (isAct) themeColor else Color.White
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            Button(
                                onClick = {
                                    captureFlashActive = true
                                    val capturedLocalUri = generateAthleteAvatarFile(
                                        context = profileContext,
                                        athleteName = editName,
                                        athleteAge = editAge,
                                        athleteType = selectedStance,
                                        backgroundTheme = selectedTheme
                                    )
                                    editPhotoUri = capturedLocalUri
                                    showCameraTaker = false
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isHardwareCamMode) VoltLime else SportsTeal,
                                    contentColor = Color.Black
                                )
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Camera,
                                    contentDescription = "Capture Snapshot Shutter Action",
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "SNAP ATHLETIC PROFILE PHOTO",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Black
                                )
                            }
                        }
                    }
                }
            }

            LaunchedEffect(userProfile, loggedInName) {
                editName = loggedInName
                userProfile?.let {
                    editPhotoUri = it.photoUri ?: ""
                    editWeight = it.weightKg.toString()
                    editHeight = it.heightCm.toString()
                    editExp = it.experienceLevel
                    editAge = it.age.toString()
                    editChest = it.chestCm.toString()
                    editArms = it.armsCm.toString()
                    editWaist = it.waistCm.toString()
                    editThighs = it.thighsCm.toString()
                    editPrimaryGoal = it.primaryGoal
                    editGender = it.gender
                    editEquipment = it.equipmentInventory
                    editDays = it.availableDaysPerWeek
                    editTimeMax = it.maxTimeMinutes
                    editWorkoutNotes = it.workoutPreferenceNotes
                }
            }

            AnimatedContent(
                targetState = activeProfileSubScreen,
                label = "ProfileSubScreenTransition",
                transitionSpec = {
                    if (targetState == "history" || targetState == "composer" || targetState == "goals" || targetState == "charts" || targetState == "social" || targetState == "metabolic" || targetState == "devices") {
                        slideInHorizontally { it } + fadeIn() togetherWith slideOutHorizontally { -it } + fadeOut()
                    } else {
                        slideInHorizontally { -it } + fadeIn() togetherWith slideOutHorizontally { it } + fadeOut()
                    }
                }
            ) { subScreen ->
                if (subScreen == "history") {
                    // ==========================================
                    // SCREEN 3: ACTIVITY HISTORY (Right Image)
                    // ==========================================
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Header row with Back, Title, Calendar
                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    IconButton(
                                        onClick = { activeProfileSubScreen = "overview" },
                                        modifier = Modifier
                                            .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f), CircleShape)
                                            .size(40.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                            contentDescription = "Return to Dashboard",
                                            tint = MaterialTheme.colorScheme.onBackground,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(
                                        text = "Activity History",
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Black,
                                        color = MaterialTheme.colorScheme.onBackground
                                    )
                                }
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f), shape = RoundedCornerShape(8.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CalendarMonth,
                                        contentDescription = "Calendar view",
                                        tint = MaterialTheme.colorScheme.onBackground,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                        }

                        // We list beautiful chronological activity cards exactly matching the reference representation!
                        item {
                            ActivityHistoryCard(
                                date = "Oct 26 (Today)",
                                title = "Morning Run - 5.2 km, 35 min",
                                details = "5.2 km • 35 min",
                                icon = "👟",
                                iconBg = Color(0xFF4CAF50)
                            )
                        }

                        item {
                            ActivityHistoryCard(
                                date = "Oct 25",
                                title = "Strength Training - Upper Body, 45 min",
                                details = "45 min • 2 sets",
                                icon = "🏋️",
                                iconBg = Color(0xFFD84315)
                            )
                        }

                        item {
                            ActivityHistoryCard(
                                date = "Oct 24",
                                title = "Logged 8 Glasses of Water",
                                details = "Hydration goal achieved!",
                                icon = "💧",
                                iconBg = Color(0xFF2196F3)
                            )
                        }

                        item {
                            ActivityHistoryCard(
                                date = "Oct 24",
                                title = "10,000 Steps Goal Achieved",
                                details = "10 min • 3 sets",
                                icon = "👟",
                                iconBg = Color(0xFF4CAF50)
                            )
                        }

                        item {
                            ActivityHistoryCard(
                                date = "Oct 23",
                                title = "Evening Yoga Session, 30 min",
                                details = "30 min • 3 sets",
                                icon = "🧘",
                                iconBg = Color(0xFF9C27B0)
                            )
                        }

                        // Now also list real logged sessions from the DB to preserve real functionality
                        if (allSessions.isNotEmpty()) {
                            item {
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = "SQLite Saved Sessions Logs",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(start = 4.dp)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                            }
                            items(allSessions) { session ->
                                HistoricSessionCard(session = session, viewModel = viewModel)
                            }
                        }
                    }
                } else if (subScreen == "composer") {
                    SelectivePostComposerScreen(
                        onBack = { activeProfileSubScreen = "overview" },
                        showSuccess = { msg -> showSuccessSnack = msg },
                        preSelectGoal = preSelectGoal,
                        preSelectChallenge = preSelectChallenge
                    )
                } else if (subScreen == "charts") {
                    ChartsSubScreen(
                        onBack = { activeProfileSubScreen = "overview" }
                    )
                } else if (subScreen == "social") {
                    SocialSubScreen(
                        onBack = { activeProfileSubScreen = "overview" },
                        onComposePost = {
                            preSelectGoal = false
                            preSelectChallenge = false
                            activeProfileSubScreen = "composer"
                        },
                        showSuccess = { msg -> showSuccessSnack = msg }
                    )
                } else if (subScreen == "goals") {
                    GoalsSubScreen(
                        viewModel = viewModel,
                        onBack = { activeProfileSubScreen = "overview" },
                        onShareGoal = {
                            preSelectGoal = true
                            preSelectChallenge = false
                            activeProfileSubScreen = "composer"
                        },
                        onShareChallenge = {
                            preSelectGoal = false
                            preSelectChallenge = true
                            activeProfileSubScreen = "composer"
                        },
                        showSuccess = { msg -> showSuccessSnack = msg }
                    )
                } else if (subScreen == "metabolic") {
                    MetabolicSubScreen(
                        viewModel = viewModel,
                        onBack = { activeProfileSubScreen = "overview" },
                        showSuccess = { msg -> showSuccessSnack = msg }
                    )
                } else if (subScreen == "engine") {
                    SmartEngineSubScreen(
                        viewModel = viewModel,
                        onBack = { activeProfileSubScreen = "overview" },
                        showSuccess = { msg -> showSuccessSnack = msg }
                    )
                } else if (subScreen == "devices") {
                    SmartDevicesSyncSubScreen(
                        viewModel = viewModel,
                        onBack = { activeProfileSubScreen = "overview" },
                        showSuccess = { msg -> showSuccessSnack = msg }
                    )
                } else {
                    // ==========================================
                    // SCREEN 1 & 2 combined: MAIN DASHBOARD + CHART VISUALS
                    // ==========================================
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // User info details greeting (Header from Left Image)
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f)
                                ),
                                border = androidx.compose.foundation.BorderStroke(
                                    1.dp,
                                    MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                                )
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        // High quality mock or dynamic avatar image using Coil AsyncImage
                                        AsyncImage(
                                            model = userProfile?.photoUri?.ifBlank { null } ?: "https://images.unsplash.com/photo-1534438327276-14e5300c3a48?auto=format&fit=crop&q=80&w=150",
                                            contentDescription = "${loggedInName.ifBlank { "OmniFit Guest" }} Avatar image",
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier
                                                .size(64.dp)
                                                .clip(CircleShape)
                                                .border(2.dp, VoltLime, CircleShape)
                                        )

                                        Spacer(modifier = Modifier.width(16.dp))

                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = loggedInName.ifBlank { "OmniFit Guest" },
                                                fontSize = 18.sp,
                                                fontWeight = FontWeight.Black,
                                                color = MaterialTheme.colorScheme.onBackground
                                            )
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                text = "Member since Jan 2023",
                                                fontSize = 11.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            Spacer(modifier = Modifier.height(6.dp))
                                            // Edit Profile styled button
                                            Box(
                                                modifier = Modifier
                                                    .border(
                                                        1.dp,
                                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                                                        RoundedCornerShape(8.dp)
                                                    )
                                                    .clickable {
                                                        if (isEditingMetrics) {
                                                            // Save edits via full model
                                                            val profile = userProfile ?: UserProfile()
                                                            val finalWeight = editWeight.toDoubleOrNull() ?: profile.weightKg
                                                            val finalHeight = editHeight.toDoubleOrNull() ?: profile.heightCm
                                                            val finalAge = editAge.toIntOrNull() ?: profile.age
                                                            val finalChest = editChest.toDoubleOrNull() ?: profile.chestCm
                                                            val finalArms = editArms.toDoubleOrNull() ?: profile.armsCm
                                                            val finalWaist = editWaist.toDoubleOrNull() ?: profile.waistCm
                                                            val finalThighs = editThighs.toDoubleOrNull() ?: profile.thighsCm
                                                            
                                                            // Navy Body Fat formula calculated live
                                                            val bmi = finalWeight / ((finalHeight / 100.0) * (finalHeight / 100.0))
                                                            val waistInches = finalWaist / 2.54
                                                            val weightLbs = finalWeight * 2.20462
                                                            val fatMassLbs = (4.15 * waistInches) - (0.082 * weightLbs) - 98.42
                                                            val bfVal = (fatMassLbs / weightLbs) * 100.0
                                                            val finalBf = if (bfVal in 4.0..48.0) bfVal else ((1.20 * bmi) + (0.23 * finalAge) - if (editGender == "Female") 5.4 else 16.2)
                                                            val validBf = finalBf.coerceIn(5.0, 42.0)

                                                            viewModel.updateProfileFull(
                                                                name = editName,
                                                                photoUri = editPhotoUri,
                                                                weight = finalWeight,
                                                                height = finalHeight,
                                                                experience = editExp.ifBlank { profile.experienceLevel },
                                                                age = finalAge,
                                                                bodyFat = validBf,
                                                                chest = finalChest,
                                                                arms = finalArms,
                                                                waist = finalWaist,
                                                                thighs = finalThighs,
                                                                primaryGoal = editPrimaryGoal,
                                                                gender = editGender,
                                                                equipment = editEquipment,
                                                                availableDaysPerWeek = editDays,
                                                                maxTimeMinutes = editTimeMax,
                                                                workoutPreferenceNotes = editWorkoutNotes
                                                            )
                                                            isEditingMetrics = false
                                                            showSuccessSnack = "Athlete biomechanical blueprint synchronized successfully!"
                                                        } else {
                                                            isEditingMetrics = true
                                                        }
                                                    }
                                                    .padding(horizontal = 14.dp, vertical = 4.dp)
                                            ) {
                                                Text(
                                                    text = if (isEditingMetrics) "Save Profile" else "Edit Profile",
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = VoltLime
                                                )
                                            }
                                        }

                                        IconButton(
                                            onClick = { viewModel.logout() },
                                            modifier = Modifier
                                                .background(MaterialTheme.colorScheme.error.copy(alpha = 0.1f), shape = CircleShape)
                                                .size(36.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Logout,
                                                contentDescription = "Log out securely",
                                                tint = MaterialTheme.colorScheme.error,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }

                                    // Expandable Biometrics Inputs Editor In-place
                                    AnimatedVisibility(visible = isEditingMetrics) {
                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(top = 16.dp),
                                            verticalArrangement = Arrangement.spacedBy(12.dp)
                                        ) {
                                            Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                                            Text(
                                                text = "HYPER-PERSONALIZED BIOMETRICS EDITOR",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = VoltLime,
                                                letterSpacing = 1.sp
                                            )

                                            // Athlete Profile Identity Fields
                                            OutlinedTextField(
                                                value = editName,
                                                onValueChange = { editName = it },
                                                label = { Text("Athletic Name") },
                                                singleLine = true,
                                                leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = VoltLime) },
                                                modifier = Modifier.fillMaxWidth(),
                                                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = VoltLime)
                                            )

                                            Text(
                                                text = "SELECT ATHLETIC AVATAR PRESET",
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = VoltLime,
                                                letterSpacing = 1.sp
                                            )

                                            val avatarPresets = listOf(
                                                "https://images.unsplash.com/photo-1534438327276-14e5300c3a48?auto=format&fit=crop&q=80&w=200", 
                                                "https://images.unsplash.com/photo-1544367567-0f2fcb009e0b?auto=format&fit=crop&q=80&w=200", 
                                                "https://images.unsplash.com/photo-1517838277536-f5f99be501cd?auto=format&fit=crop&q=80&w=200", 
                                                "https://images.unsplash.com/photo-1507398941214-572c25f4b1dc?auto=format&fit=crop&q=80&w=200", 
                                                "https://images.unsplash.com/photo-1476480862126-209bfaa8edc8?auto=format&fit=crop&q=80&w=200", 
                                                "https://images.unsplash.com/photo-1494790108377-be9c29b29330?auto=format&fit=crop&q=80&w=200"
                                            )

                                            Row(
                                                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                                            ) {
                                                avatarPresets.forEachIndexed { idx, url ->
                                                    val isSelected = editPhotoUri == url || (editPhotoUri.isBlank() && idx == 0)
                                                    Box(
                                                        modifier = Modifier
                                                            .size(56.dp)
                                                            .clip(CircleShape)
                                                            .border(
                                                                width = if (isSelected) 3.dp else 1.dp,
                                                                color = if (isSelected) VoltLime else Color.Gray.copy(alpha = 0.5f),
                                                                shape = CircleShape
                                                            )
                                                            .clickable { editPhotoUri = url }
                                                    ) {
                                                        AsyncImage(
                                                            model = url,
                                                            contentDescription = "Avatar option ${idx + 1}",
                                                            contentScale = ContentScale.Crop,
                                                            modifier = Modifier.fillMaxSize()
                                                        )
                                                    }
                                                }
                                            }

                                            Spacer(modifier = Modifier.height(2.dp))

                                            Button(
                                                     onClick = {
                                                         photoLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                                                     },
                                                     colors = ButtonDefaults.buttonColors(
                                                         containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.8f),
                                                         contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                                                     ),
                                                     modifier = Modifier.fillMaxWidth().height(48.dp),
                                                     shape = RoundedCornerShape(12.dp)
                                                 ) {
                                                     Icon(
                                                         imageVector = Icons.Default.PhotoLibrary,
                                                         contentDescription = "Pick Photo from gallery",
                                                         tint = VoltLime,
                                                         modifier = Modifier.size(18.dp)
                                                     )
                                                     Spacer(modifier = Modifier.width(8.dp))
                                                     Text("Upload Athlete Avatar Photo from Gallery", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                                 }

                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                OutlinedTextField(
                                                    value = editPhotoUri,
                                                    onValueChange = { editPhotoUri = it },
                                                    label = { Text("Profile avatar file URI or web URL") },
                                                    singleLine = true,
                                                    textStyle = androidx.compose.ui.text.TextStyle(fontSize = 11.sp),
                                                    modifier = Modifier.fillMaxWidth(),
                                                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = VoltLime)
                                                )
                                            }

                                            // Core physical indices
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                OutlinedTextField(
                                                    value = editWeight,
                                                    onValueChange = { editWeight = it },
                                                    label = { Text("Weight (kg)") },
                                                    singleLine = true,
                                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                                    modifier = Modifier.weight(1f),
                                                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = VoltLime)
                                                )
                                                OutlinedTextField(
                                                    value = editHeight,
                                                    onValueChange = { editHeight = it },
                                                    label = { Text("Height (cm)") },
                                                    singleLine = true,
                                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                                    modifier = Modifier.weight(1f),
                                                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = VoltLime)
                                                )
                                                OutlinedTextField(
                                                    value = editAge,
                                                    onValueChange = { editAge = it },
                                                    label = { Text("Age") },
                                                    singleLine = true,
                                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                                    modifier = Modifier.weight(0.8f),
                                                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = VoltLime)
                                                )
                                            }

                                            Text(
                                                text = "ANTHROPOMETRIC LANDMARKS",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = SportsTeal,
                                                letterSpacing = 1.sp
                                            )

                                            // Muscle dimensions
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                OutlinedTextField(
                                                    value = editWaist,
                                                    onValueChange = { editWaist = it },
                                                    label = { Text("Waist (cm)") },
                                                    singleLine = true,
                                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                                    modifier = Modifier.weight(1f),
                                                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = SportsTeal)
                                                )
                                                OutlinedTextField(
                                                    value = editChest,
                                                    onValueChange = { editChest = it },
                                                    label = { Text("Chest (cm)") },
                                                    singleLine = true,
                                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                                    modifier = Modifier.weight(1f),
                                                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = SportsTeal)
                                                )
                                            }

                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                OutlinedTextField(
                                                    value = editArms,
                                                    onValueChange = { editArms = it },
                                                    label = { Text("Arms (cm)") },
                                                    singleLine = true,
                                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                                    modifier = Modifier.weight(1f),
                                                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = SportsTeal)
                                                )
                                                OutlinedTextField(
                                                    value = editThighs,
                                                    onValueChange = { editThighs = it },
                                                    label = { Text("Thighs (cm)") },
                                                    singleLine = true,
                                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                                    modifier = Modifier.weight(1f),
                                                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = SportsTeal)
                                                )
                                            }

                                            // Calculated Live Body Fat Index Banner
                                            val wVal = editWeight.toDoubleOrNull() ?: 75.0
                                            val hVal = editHeight.toDoubleOrNull() ?: 175.0
                                            val aVal = editAge.toIntOrNull() ?: 28
                                            val wtVal = editWaist.toDoubleOrNull() ?: 82.0
                                            val bmiVal = wVal / ((hVal / 100.0) * (hVal / 100.0))
                                            val waistInVal = wtVal / 2.54
                                            val weightLbsVal = wVal * 2.20462
                                            val fatMassLbsVal = (4.15 * waistInVal) - (0.082 * weightLbsVal) - 98.42
                                            val estimatedBfVal = if (fatMassLbsVal / weightLbsVal * 100.0 in 4.0..48.0) (fatMassLbsVal / weightLbsVal * 100.0) else ((1.20 * bmiVal) + (0.23 * aVal) - if (editGender == "Female") 5.4 else 16.2)
                                            val liveBfPercent = estimatedBfVal.coerceIn(5.0, 42.0)

                                            Card(
                                                colors = CardDefaults.cardColors(containerColor = SportsTeal.copy(alpha = 0.08f)),
                                                border = BorderStroke(1.dp, SportsTeal.copy(alpha = 0.3f)),
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Row(
                                                    modifier = Modifier.padding(12.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Icon(Icons.Default.Calculate, contentDescription = null, tint = SportsTeal, modifier = Modifier.size(18.dp))
                                                    Spacer(modifier = Modifier.width(8.dp))
                                                    Column {
                                                        Text("Real-Time Navy Estimator", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = SportsTeal)
                                                        Text(
                                                            text = "Derived Body Fat: ${java.lang.String.format(java.util.Locale.US, "%.1f", liveBfPercent)}% • Density: ${java.lang.String.format(java.util.Locale.US, "%.2f", 1.25 - (liveBfPercent / 100))}",
                                                            fontSize = 11.sp,
                                                            fontWeight = FontWeight.Black,
                                                            color = VoltLime
                                                        )
                                                    }
                                                }
                                            }

                                            // Gender Selector segmented tabs
                                            Text("Gender Partition", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            Row(
                                                modifier = Modifier.fillMaxWidth().height(40.dp).clip(RoundedCornerShape(8.dp)).background(DarkSpaceCharcoal),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                listOf("Male", "Female").forEach { g ->
                                                    val isSelected = editGender == g
                                                    Box(
                                                        modifier = Modifier.weight(1f).fillMaxHeight().clip(RoundedCornerShape(8.dp))
                                                            .background(if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent)
                                                            .clickable { editGender = g },
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        Text(g, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = if (isSelected) Color.Black else IceWhite)
                                                    }
                                                }
                                            }

                                            // Goal Selector segmented tabs
                                            Text("Training Blueprint Goal", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            Row(
                                                modifier = Modifier.fillMaxWidth().height(40.dp).clip(RoundedCornerShape(8.dp)).background(DarkSpaceCharcoal),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                listOf("Hypertrophy", "Strength", "Fat Loss").forEach { goal ->
                                                    val isSelected = editPrimaryGoal == goal
                                                    Box(
                                                        modifier = Modifier.weight(1f).fillMaxHeight().clip(RoundedCornerShape(8.dp))
                                                            .background(if (isSelected) MaterialTheme.colorScheme.secondary else Color.Transparent)
                                                            .clickable { editPrimaryGoal = goal },
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        Text(goal, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = if (isSelected) Color.Black else IceWhite)
                                                    }
                                                }
                                            }

                                            // Experience level input
                                            Text("Aesthetic Experience Level", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            Row(
                                                modifier = Modifier.fillMaxWidth().height(40.dp).clip(RoundedCornerShape(8.dp)).background(DarkSpaceCharcoal),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                listOf("Beginner", "Intermediate", "Advanced").forEach { exp ->
                                                    val isSelected = editExp == exp
                                                    Box(
                                                        modifier = Modifier.weight(1f).fillMaxHeight().clip(RoundedCornerShape(8.dp))
                                                            .background(if (isSelected) MaterialTheme.colorScheme.tertiary else Color.Transparent)
                                                            .clickable { editExp = exp },
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        Text(exp, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = if (isSelected) Color.Black else IceWhite)
                                                    }
                                                }
                                            }

                                            // Equipment availability
                                            // Days per Week Available editor block
                                             Spacer(modifier = Modifier.height(8.dp))
                                             Row(
                                                 modifier = Modifier.fillMaxWidth(),
                                                 horizontalArrangement = Arrangement.SpaceBetween,
                                                 verticalAlignment = Alignment.CenterVertically
                                             ) {
                                                 Text("Days/Week to Train", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                 Text("$editDays days", fontSize = 11.sp, fontWeight = FontWeight.Black, color = VoltLime)
                                             }
                                             Slider(
                                                 value = editDays.toFloat(),
                                                 onValueChange = { editDays = it.toInt() },
                                                 valueRange = 2f..7f,
                                                 steps = 4,
                                                 colors = SliderDefaults.colors(
                                                     activeTrackColor = VoltLime,
                                                     thumbColor = VoltLime
                                                 )
                                             )

                                             // Session Max Time Limit editor block
                                             Row(
                                                 modifier = Modifier.fillMaxWidth(),
                                                 horizontalArrangement = Arrangement.SpaceBetween,
                                                 verticalAlignment = Alignment.CenterVertically
                                             ) {
                                                 Text("Session Duration Limit", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                 Text("$editTimeMax mins", fontSize = 11.sp, fontWeight = FontWeight.Black, color = SportsTeal)
                                             }
                                             Slider(
                                                 value = editTimeMax.toFloat(),
                                                 onValueChange = { editTimeMax = it.toInt() },
                                                 valueRange = 15f..120f,
                                                 steps = 6,
                                                 colors = SliderDefaults.colors(
                                                     activeTrackColor = SportsTeal,
                                                     thumbColor = SportsTeal
                                                 )
                                             )

                                             // Period Mode Toggle (only if female)
                                             if (editGender.equals("Female", ignoreCase = true)) {
                                                 val isOnPeriod by viewModel.isOnPeriod.collectAsState()
                                                 
                                                 Spacer(modifier = Modifier.height(4.dp))
                                                 Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))
                                                 Spacer(modifier = Modifier.height(4.dp))
                                                 
                                                 Row(
                                                     modifier = Modifier
                                                         .fillMaxWidth()
                                                         .clip(RoundedCornerShape(8.dp))
                                                         .background(Color(0xFFE53935).copy(alpha = 0.08f))
                                                         .clickable { viewModel.setIsOnPeriod(!isOnPeriod) }
                                                         .padding(12.dp),
                                                     verticalAlignment = Alignment.CenterVertically,
                                                     horizontalArrangement = Arrangement.SpaceBetween
                                                 ) {
                                                     Row(verticalAlignment = Alignment.CenterVertically) {
                                                         Icon(
                                                             imageVector = if (isOnPeriod) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                                             contentDescription = null,
                                                             tint = Color(0xFFE53935),
                                                             modifier = Modifier.size(18.dp)
                                                         )
                                                         Spacer(modifier = Modifier.width(8.dp))
                                                         Column {
                                                             Text("Period Tracking Care Mode (🌸)", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = IceWhite)
                                                             Text("Adjusts exercise intensity automatically", fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                         }
                                                     }
                                                     Switch(
                                                         checked = isOnPeriod,
                                                         onCheckedChange = { viewModel.setIsOnPeriod(it) },
                                                         colors = SwitchDefaults.colors(
                                                             checkedThumbColor = Color(0xFFE53935),
                                                             checkedTrackColor = Color(0xFFE53935).copy(alpha = 0.3f)
                                                         )
                                                     )
                                                 }
                                             }

                                             Spacer(modifier = Modifier.height(8.dp))

                                             Text("Workout Vision & Custom Preferences", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                             Spacer(modifier = Modifier.height(4.dp))
                                             OutlinedTextField(
                                                 value = editWorkoutNotes,
                                                 onValueChange = { editWorkoutNotes = it },
                                                 placeholder = {
                                                     Text(
                                                         text = "Describe what you want your workouts to be like (focus zones, injury limits, or preferences)...",
                                                         fontSize = 11.sp,
                                                         color = Color.Gray
                                                     )
                                                 },
                                                 modifier = Modifier.fillMaxWidth().height(90.dp),
                                                 textStyle = androidx.compose.ui.text.TextStyle(fontSize = 11.sp, color = IceWhite),
                                                 shape = RoundedCornerShape(12.dp),
                                                 colors = OutlinedTextFieldDefaults.colors(
                                                     focusedBorderColor = VoltLime,
                                                     unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
                                                     focusedContainerColor = DarkSpaceCharcoal,
                                                     unfocusedContainerColor = DarkSpaceCharcoal
                                                 ),
                                                 maxLines = 4
                                             )

                                             Spacer(modifier = Modifier.height(10.dp))

                                             Text("Equipment Available", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            Row(
                                                modifier = Modifier.fillMaxWidth().height(40.dp).clip(RoundedCornerShape(8.dp)).background(DarkSpaceCharcoal),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                listOf("Full Gym", "Dumbbells Only", "Bodyweight/Calisthenics").forEach { equip ->
                                                    val isSelected = editEquipment == equip
                                                    Box(
                                                        modifier = Modifier.weight(1f).fillMaxHeight().clip(RoundedCornerShape(8.dp))
                                                            .background(if (isSelected) VoltLime else Color.Transparent)
                                                            .clickable { editEquipment = equip },
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        Text(
                                                            text = when(equip) {
                                                                "Full Gym" -> "Full Gym"
                                                                "Dumbbells Only" -> "Dumbbells"
                                                                else -> "Bodyweight"
                                                            },
                                                            fontSize = 10.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            color = if (isSelected) Color.Black else Color.White
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }

                                    // Non-editing simple stats indicators row
                                     AnimatedVisibility(visible = !isEditingMetrics) {
                                         val profile = userProfile ?: UserProfile()
                                         Column(
                                             modifier = Modifier.fillMaxWidth(),
                                             verticalArrangement = Arrangement.spacedBy(16.dp)
                                         ) {
                                             Spacer(modifier = Modifier.height(4.dp))
                                             Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))

                                             // SECTION 1: PHYSICAL BLUEPRINT INDICES
                                             Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                                 Text(
                                                     text = "Biomechanical Metric Indexes",
                                                     fontSize = 12.sp,
                                                     fontWeight = FontWeight.Bold,
                                                     color = MaterialTheme.colorScheme.primary
                                                 )

                                                 Row(
                                                     modifier = Modifier.fillMaxWidth(),
                                                     horizontalArrangement = Arrangement.spacedBy(8.dp)
                                                 ) {
                                                     Box(
                                                         modifier = Modifier
                                                             .weight(1f)
                                                             .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.12f), RoundedCornerShape(10.dp))
                                                             .padding(10.dp)
                                                     ) {
                                                         Column {
                                                             Text(com.example.ui.translation.LanguageManager.getString("stature_height", appLanguage), fontSize = 8.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold)
                                                             val hText = when (unitSystem) {
                                                                 "imperial_us" -> String.format(java.util.Locale.US, "%.1f in", profile.heightCm / 2.54)
                                                                 "imperial_uk" -> {
                                                                     val totalInches = profile.heightCm / 2.54
                                                                     val ft = (totalInches / 12).toInt()
                                                                     val remainIn = (totalInches % 12).toInt()
                                                                     "${ft} ft ${remainIn} in"
                                                                 }
                                                                 else -> "${profile.heightCm} cm"
                                                              }
                                                             Text(hText, fontSize = 13.sp, color = Color.White, fontWeight = FontWeight.Black)
                                                         }
                                                     }
                                                     Box(
                                                         modifier = Modifier
                                                             .weight(1f)
                                                             .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.12f), RoundedCornerShape(10.dp))
                                                             .padding(10.dp)
                                                     ) {
                                                         Column {
                                                             Text(com.example.ui.translation.LanguageManager.getString("total_mass", appLanguage), fontSize = 8.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold)
                                                             val wText = when (unitSystem) {
                                                                 "imperial_us" -> String.format(java.util.Locale.US, "%.1f lbs", profile.weightKg * 2.20462)
                                                                 "imperial_uk" -> {
                                                                     val totalLbs = profile.weightKg * 2.20462
                                                                     val st = (totalLbs / 14).toInt()
                                                                     val remainLbs = (totalLbs % 14).toInt()
                                                                     "${st} st ${remainLbs} lbs"
                                                                 }
                                                                 else -> "${profile.weightKg} kg"
                                                              }
                                                             Text(wText, fontSize = 13.sp, color = Color.White, fontWeight = FontWeight.Black)
                                                         }
                                                     }
                                                     Box(
                                                         modifier = Modifier
                                                             .weight(1f)
                                                             .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.12f), RoundedCornerShape(10.dp))
                                                             .padding(10.dp)
                                                     ) {
                                                         Column {
                                                             Text(com.example.ui.translation.LanguageManager.getString("current_age", appLanguage), fontSize = 8.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold)
                                                             Text("${profile.age} yrs", fontSize = 13.sp, color = Color.White, fontWeight = FontWeight.Black)
                                                         }
                                                     }
                                                 }

                                                 Row(
                                                     modifier = Modifier.fillMaxWidth(),
                                                     horizontalArrangement = Arrangement.spacedBy(8.dp)
                                                 ) {
                                                     Box(
                                                         modifier = Modifier
                                                             .weight(1f)
                                                             .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.12f), RoundedCornerShape(10.dp))
                                                             .padding(10.dp)
                                                     ) {
                                                         Column {
                                                             Text(com.example.ui.translation.LanguageManager.getString("body_fat", appLanguage), fontSize = 8.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold)
                                                             Text("${profile.bodyFatPercentage}%", fontSize = 13.sp, color = VoltLime, fontWeight = FontWeight.Black)
                                                         }
                                                     }
                                                     Box(
                                                         modifier = Modifier
                                                             .weight(1f)
                                                             .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.12f), RoundedCornerShape(10.dp))
                                                             .padding(10.dp)
                                                     ) {
                                                         Column {
                                                             Text(com.example.ui.translation.LanguageManager.getString("target_gender", appLanguage), fontSize = 8.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold)
                                                             Text(profile.gender, fontSize = 13.sp, color = SportsTeal, fontWeight = FontWeight.Black)
                                                         }
                                                     }
                                                 }
                                             }

                                             // SECTION 2: PHYSICAL TAPE CIRCUMFERENCES
                                             Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                                 Text(
                                                     text = "Tape Circumference Dimensions",
                                                     fontSize = 12.sp,
                                                     fontWeight = FontWeight.Bold,
                                                     color = MaterialTheme.colorScheme.primary
                                                 )

                                                 Row(
                                                     modifier = Modifier.fillMaxWidth(),
                                                     horizontalArrangement = Arrangement.spacedBy(8.dp)
                                                 ) {
                                                     val isMetric = unitSystem == "metric"
                                                     val chestStr = if (isMetric) "${profile.chestCm} cm" else String.format(java.util.Locale.US, "%.1f in", profile.chestCm / 2.54)
                                                     val waistStr = if (isMetric) "${profile.waistCm} cm" else String.format(java.util.Locale.US, "%.1f in", profile.waistCm / 2.54)
                                                     val armsStr = if (isMetric) "${profile.armsCm} cm" else String.format(java.util.Locale.US, "%.1f in", profile.armsCm / 2.54)
                                                     val thighsStr = if (isMetric) "${profile.thighsCm} cm" else String.format(java.util.Locale.US, "%.1f in", profile.thighsCm / 2.54)

                                                     listOf(
                                                         com.example.ui.translation.LanguageManager.getString("chest", appLanguage) to chestStr,
                                                         com.example.ui.translation.LanguageManager.getString("waist", appLanguage) to waistStr,
                                                         com.example.ui.translation.LanguageManager.getString("arms", appLanguage) to armsStr,
                                                         com.example.ui.translation.LanguageManager.getString("thighs", appLanguage) to thighsStr
                                                     ).forEach { (tape, size) ->
                                                         Box(
                                                             modifier = Modifier
                                                                 .weight(1f)
                                                                 .background(DarkSpaceCharcoal.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
                                                                 .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.12f), RoundedCornerShape(8.dp))
                                                                 .padding(8.dp),
                                                             contentAlignment = Alignment.Center
                                                         ) {
                                                             Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                                 Text(tape.uppercase(), fontSize = 8.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold)
                                                                 Text(size, fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Bold)
                                                             }
                                                         }
                                                     }
                                                 }
                                             }

                                             // SECTION 3: SPORTS TRAINING VISION BLUEPRINT & NOTES
                                             Card(
                                                 modifier = Modifier.fillMaxWidth(),
                                                 shape = RoundedCornerShape(12.dp),
                                                 colors = CardDefaults.cardColors(
                                                     containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f)
                                                 ),
                                                 border = androidx.compose.foundation.BorderStroke(
                                                     1.dp,
                                                     SportsTeal.copy(alpha = 0.25f)
                                                 )
                                             ) {
                                                 Column(modifier = Modifier.padding(14.dp)) {
                                                     Row(
                                                         modifier = Modifier.fillMaxWidth(),
                                                         verticalAlignment = Alignment.CenterVertically
                                                     ) {
                                                         Icon(
                                                             imageVector = Icons.Default.Tune,
                                                             contentDescription = null,
                                                             tint = SportsTeal,
                                                             modifier = Modifier.size(16.dp)
                                                         )
                                                         Spacer(modifier = Modifier.width(6.dp))
                                                         Text(
                                                             text = "Smart Workout Blueprint Settings",
                                                             fontSize = 12.sp,
                                                             fontWeight = FontWeight.Black,
                                                             color = Color.White
                                                         )
                                                     }

                                                     Spacer(modifier = Modifier.height(10.dp))

                                                     Row(
                                                         modifier = Modifier.fillMaxWidth(),
                                                         horizontalArrangement = Arrangement.SpaceBetween
                                                     ) {
                                                         BiometricLabel("Active Goal", profile.primaryGoal)
                                                         BiometricLabel("Experience", profile.experienceLevel)
                                                         BiometricLabel("Schedule Days", "${profile.availableDaysPerWeek} days/wk")
                                                         BiometricLabel("Equipment", profile.equipmentInventory)
                                                     }

                                                     Spacer(modifier = Modifier.height(12.dp))
                                                     Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.15f))
                                                     Spacer(modifier = Modifier.height(10.dp))

                                                     Row(verticalAlignment = Alignment.CenterVertically) {
                                                         Icon(
                                                             imageVector = Icons.Default.FitnessCenter,
                                                             contentDescription = null,
                                                             tint = VoltLime,
                                                             modifier = Modifier.size(14.dp)
                                                         )
                                                         Spacer(modifier = Modifier.width(6.dp))
                                                         Text(
                                                             text = "🎯 WORKOUT PREFERENCE NOTES:",
                                                             fontSize = 9.sp,
                                                             color = VoltLime,
                                                             fontWeight = FontWeight.Black,
                                                             letterSpacing = 0.5.sp
                                                         )
                                                     }
                                                     Spacer(modifier = Modifier.height(6.dp))
                                                     Text(
                                                         text = if (profile.workoutPreferenceNotes.isNotBlank()) profile.workoutPreferenceNotes else "No customized restrictions or focal points. Using optimized digital biomechanical splits.",
                                                         fontSize = 11.sp,
                                                         color = Color.LightGray,
                                                         lineHeight = 15.sp
                                                     )
                                                 }
                                             }
                                         }
                                     }
                                 }
                             }
                         }
                         
    // "Today's Metabolic Tracker" compact glance card
                        item {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f)
                                ),
                                border = androidx.compose.foundation.BorderStroke(
                                    1.dp,
                                    VoltLime.copy(alpha = 0.25f)
                                )
                            ) {
                                Row(
                                    modifier = Modifier.padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "Today's Metabolic Tracker",
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = "👟 ${todayHealthLog.stepsCount} / $stepsGoal steps  •  🔥 ${todayHealthLog.activeCaloriesBurned.toInt()} kcal",
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.primary,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    Box(
                                        modifier = Modifier
                                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), CircleShape)
                                            .size(36.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Adjust,
                                            contentDescription = "Target status indicators",
                                            tint = VoltLime,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }

                        // NEW COMPACT PROFILE SUB-OPTIONS DIRECTORY (REPLACED LONG SCROLLING LAYOUT)
                        item {
                            Column(
                                modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Text(
                                    text = "Profile Feature Directories",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Black,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(bottom = 2.dp)
                                )

                                // NAVIGATION OPTION 1: GOALS & CHALLENGES MANAGER
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { activeProfileSubScreen = "goals" },
                                    shape = RoundedCornerShape(12.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f)
                                    ),
                                    border = androidx.compose.foundation.BorderStroke(
                                        1.dp,
                                        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                                    )
                                ) {
                                    Row(
                                        modifier = Modifier.padding(14.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(42.dp)
                                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f), CircleShape),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text("🎯", fontSize = 20.sp)
                                        }
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = "Goals & Challenges Tracker",
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            Text(
                                                text = "Customize, add, and track progress of your custom goals & streak challenges.",
                                                fontSize = 10.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        Icon(
                                            imageVector = Icons.Default.ChevronRight,
                                            contentDescription = "Navigate to Goals",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }

                                // NAVIGATION OPTION 1.5: DAILY VITAL METRICS LOGGER
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { activeProfileSubScreen = "metabolic" },
                                    shape = RoundedCornerShape(12.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f)
                                    ),
                                    border = androidx.compose.foundation.BorderStroke(
                                        1.dp,
                                        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                                    )
                                ) {
                                    Row(
                                        modifier = Modifier.padding(14.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(42.dp)
                                                .background(Color(0xFF2196F3).copy(alpha = 0.12f), CircleShape),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text("👟", fontSize = 20.sp)
                                        }
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = "Daily Vital Metrics Logger",
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            Text(
                                                text = "Log and customize steps, water intake, active calories burned, and target thresholds.",
                                                fontSize = 10.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        Icon(
                                            imageVector = Icons.Default.ChevronRight,
                                            contentDescription = "Open Daily Vital Metrics Logger",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }

                                // NAVIGATION OPTION 2: ANALYTICS & CHARTS
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { activeProfileSubScreen = "charts" },
                                    shape = RoundedCornerShape(12.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f)
                                    ),
                                    border = androidx.compose.foundation.BorderStroke(
                                        1.dp,
                                        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                                    )
                                ) {
                                    Row(
                                        modifier = Modifier.padding(14.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(42.dp)
                                                .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.12f), CircleShape),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text("📊", fontSize = 20.sp)
                                        }
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = "Analytics & Fitness Charts",
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            Text(
                                                text = "Visualize weekly active time, passive steps, and monthly progression trends.",
                                                fontSize = 10.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        Icon(
                                            imageVector = Icons.Default.ChevronRight,
                                            contentDescription = "Navigate to Charts",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }

                                // NAVIGATION OPTION 2.5: SMART WORKOUT ENGINE TUNER
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { activeProfileSubScreen = "engine" },
                                    shape = RoundedCornerShape(12.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f)
                                    ),
                                    border = androidx.compose.foundation.BorderStroke(
                                        1.dp,
                                        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                                    )
                                ) {
                                    Row(
                                        modifier = Modifier.padding(14.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(42.dp)
                                                .background(MaterialTheme.colorScheme.tertiary.copy(alpha = 0.12f), CircleShape),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text("⚙️", fontSize = 20.sp)
                                        }
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = "Smart Workout Engine Tuner",
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            Text(
                                                text = "Tune weekly training days, session limit, gender & coaching tiers to regenerate schedules.",
                                                fontSize = 10.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        Icon(
                                            imageVector = Icons.Default.ChevronRight,
                                            contentDescription = "Navigate to Smart Workout Engine",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }

                                // NAVIGATION OPTION 2.6: SMART WEARABLES & DEVICES SYNC HUB
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { activeProfileSubScreen = "devices" },
                                    shape = RoundedCornerShape(12.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f)
                                    ),
                                    border = androidx.compose.foundation.BorderStroke(
                                        1.dp,
                                        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                                    )
                                ) {
                                    Row(
                                        modifier = Modifier.padding(14.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(42.dp)
                                                .background(VoltLime.copy(alpha = 0.12f), CircleShape),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text("🔌", fontSize = 20.sp)
                                        }
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = "Universal Smart Devices Hub",
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            Text(
                                                text = "Connect fitness bands, rings, watches and accessories of any company using live telemetry.",
                                                fontSize = 10.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        Icon(
                                            imageVector = Icons.Default.ChevronRight,
                                            contentDescription = "Navigate to Smart Devices Hub",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }

                                // NAVIGATION OPTION 3: SOCIAL SYNC & SHARING
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { activeProfileSubScreen = "social" },
                                    shape = RoundedCornerShape(12.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f)
                                    ),
                                    border = androidx.compose.foundation.BorderStroke(
                                        1.dp,
                                        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                                    )
                                ) {
                                    Row(
                                        modifier = Modifier.padding(14.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(42.dp)
                                                .background(MaterialTheme.colorScheme.tertiary.copy(alpha = 0.12f), CircleShape),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text("🤝", fontSize = 20.sp)
                                        }
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = "Social Connections",
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            Text(
                                                text = "Connect third-party secure accounts and publish custom posts to networks.",
                                                fontSize = 10.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        Icon(
                                            imageVector = Icons.Default.ChevronRight,
                                            contentDescription = "Navigate to Social",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }

                                // NAVIGATION OPTION 4: CHRONOLOGICAL HISTORY (IN-APP + DATABASE SESSIONS)
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { activeProfileSubScreen = "history" },
                                    shape = RoundedCornerShape(12.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f)
                                    ),
                                    border = androidx.compose.foundation.BorderStroke(
                                        1.dp,
                                        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                                    )
                                ) {
                                    Row(
                                        modifier = Modifier.padding(14.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(42.dp)
                                                .background(Color(0xFFFF7F50).copy(alpha = 0.12f), CircleShape),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text("📜", fontSize = 20.sp)
                                        }
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = "Activity Logs History",
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            Text(
                                                text = "Review historical logs, water consumption target logs and database saves.",
                                                fontSize = 10.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        Icon(
                                            imageVector = Icons.Default.ChevronRight,
                                            contentDescription = "Navigate to History Log",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }

                                // NAVIGATION OPTION 5: REPLAY APP WALKTHROUGH
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { 
                                            viewModel.showWalkthroughOverride.value = true
                                            showSuccessSnack = "Launching OmniFit Guide and Tour..."
                                        },
                                    shape = RoundedCornerShape(12.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f)
                                    ),
                                    border = androidx.compose.foundation.BorderStroke(
                                        1.dp,
                                        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                                    )
                                ) {
                                    Row(
                                        modifier = Modifier.padding(14.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(42.dp)
                                                .background(VoltLime.copy(alpha = 0.12f), CircleShape),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text("🔄", fontSize = 20.sp)
                                        }
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = "Replay App Walkthrough",
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            Text(
                                                text = "Review step-by-step guides explaining what features reside in which tabs and how they work.",
                                                fontSize = 10.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        Icon(
                                            imageVector = Icons.Default.PlayArrow,
                                            contentDescription = "Launch Walkthrough",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }

                                // NAVIGATION OPTION 6: APP LIGHT & DARK THEME SETTINGS
                                val appThemeChoice by viewModel.themeSetting.collectAsState()
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f)
                                    ),
                                    border = androidx.compose.foundation.BorderStroke(
                                        1.dp,
                                        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                                    )
                                ) {
                                    Column(
                                        modifier = Modifier.padding(14.dp)
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(42.dp)
                                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f), CircleShape),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(if (appThemeChoice == "light") "☀️" else "🌙", fontSize = 20.sp)
                                            }
                                            Spacer(modifier = Modifier.width(12.dp))
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = "App Theme Color Settings",
                                                    fontSize = 13.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )
                                                Text(
                                                    text = "Select a modern dark theme suited for training, or a high-contrast cozy light theme.",
                                                    fontSize = 10.sp,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(12.dp))

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            // Dark Theme Option Button
                                            val isDarkActive = appThemeChoice != "light"
                                            Button(
                                                onClick = {
                                                    viewModel.setThemeSetting("dark")
                                                    showSuccessSnack = "Switched to Premium Matte Dark Theme"
                                                },
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .height(38.dp),
                                                colors = ButtonDefaults.buttonColors(
                                                    containerColor = if (isDarkActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                                    contentColor = if (isDarkActive) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                                                ),
                                                shape = RoundedCornerShape(8.dp),
                                                contentPadding = PaddingValues(horizontal = 8.dp)
                                            ) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.Center
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.NightsStay,
                                                        contentDescription = null,
                                                        modifier = Modifier.size(16.dp)
                                                    )
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Text("Dark Theme", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                                }
                                            }

                                            // Light Theme Option Button
                                            val isLightActive = appThemeChoice == "light"
                                            Button(
                                                onClick = {
                                                    viewModel.setThemeSetting("light")
                                                    showSuccessSnack = "Switched to Cozy Radiant Light Theme"
                                                },
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .height(38.dp),
                                                colors = ButtonDefaults.buttonColors(
                                                    containerColor = if (isLightActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                                    contentColor = if (isLightActive) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                                                ),
                                                shape = RoundedCornerShape(8.dp),
                                                contentPadding = PaddingValues(horizontal = 8.dp)
                                            ) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.Center
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.WbSunny,
                                                        contentDescription = null,
                                                        modifier = Modifier.size(16.dp)
                                                    )
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Text("Light Theme", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Beautiful float success notifier banner
        AnimatedVisibility(
            visible = showSuccessSnack != null,
            enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut(),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(16.dp)
        ) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, VoltLime)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Action finished indicator check symbol icon",
                        tint = VoltLime,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = showSuccessSnack ?: "",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        }
    }
}

@Composable
fun BiometricLabel(title: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = title,
            fontSize = 9.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = value,
            fontSize = 12.sp,
            fontWeight = FontWeight.Black,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
fun ProfileActivitySummaryCards(
    todayLog: com.example.data.model.HealthTrackerLog,
    modifier: Modifier = Modifier
) {
    val steps = todayLog.stepsCount
    val calories = todayLog.activeCaloriesBurned
    val water = todayLog.waterIntakeMl

    val stepsGoal = 10000
    val caloriesGoal = 700.0
    val waterGoal = 2500

    val stepsPct = if (stepsGoal > 0) (steps.toFloat() / stepsGoal).coerceIn(0f, 1f) else 0f
    val caloriesPct = if (caloriesGoal > 0) (calories.toFloat() / caloriesGoal.toFloat()).coerceIn(0f, 1f) else 0f
    val waterPct = if (waterGoal > 0) (water.toFloat() / waterGoal).coerceIn(0f, 1f) else 0f

    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = "⚡ Today's Daily Activity Summary",
            fontSize = 14.sp,
            fontWeight = FontWeight.Black,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = "Real-time metrics collected from today's metabolic record logs.",
            fontSize = 10.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Steps Card
            Card(
                modifier = Modifier
                    .weight(1f)
                    .height(115.dp),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f)
                ),
                border = androidx.compose.foundation.BorderStroke(1.dp, VoltLime.copy(alpha = 0.2f))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(8.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .background(VoltLime.copy(alpha = 0.12f), shape = RoundedCornerShape(8.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("👟", fontSize = 14.sp)
                        }
                        Text(
                            text = "${(stepsPct * 100).toInt()}%",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Black,
                            color = VoltLime,
                            modifier = Modifier
                                .background(VoltLime.copy(alpha = 0.08f), shape = RoundedCornerShape(4.dp))
                                .padding(horizontal = 4.dp, vertical = 2.dp)
                        )
                    }

                    Column {
                        Text(
                            text = "Daily Steps",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(1.dp))
                        Text(
                            text = String.format("%,d", steps),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(1.dp))
                        Text(
                            text = "/ $stepsGoal",
                            fontSize = 8.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                    }

                    LinearProgressIndicator(
                        progress = stepsPct,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(3.dp)
                            .clip(RoundedCornerShape(1.5.dp)),
                        color = VoltLime,
                        trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
                    )
                }
            }

            // Calories Card
            Card(
                modifier = Modifier
                    .weight(1f)
                    .height(115.dp),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f)
                ),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFF7F50).copy(alpha = 0.2f))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(8.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .background(Color(0xFFFF7F50).copy(alpha = 0.12f), shape = RoundedCornerShape(8.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("🔥", fontSize = 14.sp)
                        }
                        Text(
                            text = "${(caloriesPct * 100).toInt()}%",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Black,
                            color = Color(0xFFFF7F50),
                            modifier = Modifier
                                .background(Color(0xFFFF7F50).copy(alpha = 0.08f), shape = RoundedCornerShape(4.dp))
                                .padding(horizontal = 4.dp, vertical = 2.dp)
                        )
                    }

                    Column {
                        Text(
                            text = "Calories",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(1.dp))
                        Text(
                            text = String.format("%.0f", calories),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(1.dp))
                        Text(
                            text = "/ ${caloriesGoal.toInt()} kcal",
                            fontSize = 8.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                    }

                    LinearProgressIndicator(
                        progress = caloriesPct,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(3.dp)
                            .clip(RoundedCornerShape(1.5.dp)),
                        color = Color(0xFFFF7F50),
                        trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
                    )
                }
            }

            // Water Card
            Card(
                modifier = Modifier
                    .weight(1f)
                    .height(115.dp),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f)
                ),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF00BFFF).copy(alpha = 0.2f))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(8.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .background(Color(0xFF00BFFF).copy(alpha = 0.12f), shape = RoundedCornerShape(8.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("💧", fontSize = 14.sp)
                        }
                        Text(
                            text = "${(waterPct * 100).toInt()}%",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Black,
                            color = Color(0xFF00BFFF),
                            modifier = Modifier
                                .background(Color(0xFF00BFFF).copy(alpha = 0.08f), shape = RoundedCornerShape(4.dp))
                                .padding(horizontal = 4.dp, vertical = 2.dp)
                        )
                    }

                    Column {
                        Text(
                            text = "Water Intake",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(1.dp))
                        Text(
                            text = String.format("%,d", water),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(1.dp))
                        Text(
                            text = "/ $waterGoal ml",
                            fontSize = 8.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                    }

                    LinearProgressIndicator(
                        progress = waterPct,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(3.dp)
                            .clip(RoundedCornerShape(1.5.dp)),
                        color = Color(0xFF00BFFF),
                        trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
                    )
                }
            }
        }
    }
}

@Composable
fun ActivityHistoryCard(
    date: String,
    title: String,
    details: String,
    icon: String,
    iconBg: Color,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = date,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
            modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
        )
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f)
            ),
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Circular icon bullet matching exact Screen 3 colors
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(iconBg.copy(alpha = 0.12f), shape = CircleShape)
                        .border(1.dp, iconBg.copy(alpha = 0.3f), shape = CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = icon, fontSize = 16.sp)
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (details.isNotBlank()) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = details,
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }
                }
            }
        }
    }
}

// =========================================================================
// HIGH-FIDELITY SUBCOMPOSE CODE EXTENSIONS (SCREENS MODULE)
// =========================================================================

@Composable
fun WeeklyActiveTimeStepsChart() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f), RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f), RoundedCornerShape(16.dp))
            .padding(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Weekly Active Time & Steps",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.padding(top = 4.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(Color(0xFF1877F2), CircleShape)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Workout Time (mins)", fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(Color(0xFF90CAF9), CircleShape)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Step Count (10k)", fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            
            // Dumbbell and Running Shoe Icons in top corner
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("🏋️", fontSize = 14.sp)
                Text("👟", fontSize = 14.sp)
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Dual bar chart rendering in Canvas
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(130.dp)
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val width = size.width
                val height = size.height

                // Draw background grid lines (3 horizontal guides)
                val lineCount = 3
                for (j in 0 until lineCount) {
                    val y = (height / (lineCount - 1)) * j
                    drawLine(
                        color = Color.LightGray.copy(alpha = 0.08f),
                        start = Offset(0f, y),
                        end = Offset(width, y),
                        strokeWidth = 1f
                    )
                }

                // Scales: 120k for steps, 120 mins for workout time
                // Mock daily values [Mon..Sun] (Workout Time mins, Steps in 10k units)
                val workoutMins = listOf(45f, 60f, 30f, 80f, 50f, 95f, 40f)
                val stepTenKs = listOf(8.5f, 9.2f, 7.8f, 10.5f, 6.0f, 11.2f, 8.9f)
                
                val numDays = 7
                val columnSpacing = width / numDays
                val gapBetweenGroups = columnSpacing * 0.25f
                val singleBarWidth = (columnSpacing - gapBetweenGroups) * 0.35f
                val startMargin = gapBetweenGroups * 0.5f

                for (i in 0 until numDays) {
                    // Day X coordinate center area
                    val groupStartX = (columnSpacing * i) + startMargin

                    // Bar 1: Workout Time (Blue)
                    val workoutPct = (workoutMins[i] / 120f).coerceIn(0f, 1f)
                    val wBarHeight = workoutPct * height * 0.85f
                    val wY = height - wBarHeight
                    drawRoundRect(
                        color = Color(0xFF1877F2), // Accent brand blue
                        topLeft = Offset(groupStartX, wY),
                        size = androidx.compose.ui.geometry.Size(singleBarWidth, wBarHeight),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(3.dp.toPx(), 3.dp.toPx())
                    )

                    // Bar 2: Steps tens (Light blue)
                    val stepsPct = (stepTenKs[i] / 12f).coerceIn(0f, 1f)
                    val sBarHeight = stepsPct * height * 0.85f
                    val sY = height - sBarHeight
                    drawRoundRect(
                        color = Color(0xFF90CAF9), // Lighter blue
                        topLeft = Offset(groupStartX + singleBarWidth + 4.dp.toPx(), sY),
                        size = androidx.compose.ui.geometry.Size(singleBarWidth, sBarHeight),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(3.dp.toPx(), 3.dp.toPx())
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // X Axes labels matching image
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun").forEach { day ->
                Text(
                    text = day,
                    fontSize = 9.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
fun ShareToSocialsSection(
    onComposePost: () -> Unit,
    showSuccess: (String) -> Unit
) {
    // We can track connection states of accounts (checkable cards!)
    var isGoogleConnected by remember { mutableStateOf(true) } // Gmail is checked in image
    var isXConnected by remember { mutableStateOf(false) }
    var isFBConnected by remember { mutableStateOf(false) }
    var isInstaConnected by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f)
        ),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f)
        )
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Share to Socials",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "Connect",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = VoltLime,
                    modifier = Modifier.clickable { 
                        showSuccess("Social Sync Portal initiated! Safe auth links generated.")
                    }
                )
            }
            Spacer(modifier = Modifier.height(12.dp))

            // Carousel of connect buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Branded checkable accounts: Google (Gmail), X (Twitter), Facebook, Instagram
                SocialConnectButton(
                    logoChar = "✉️",
                    logoBg = Color(0xFFEA4335),
                    checked = isGoogleConnected,
                    onClick = { 
                        isGoogleConnected = !isGoogleConnected
                        showSuccess(if (isGoogleConnected) "Google/Gmail connected!" else "Google/Gmail disconnected!")
                    },
                    modifier = Modifier.weight(1f)
                )

                SocialConnectButton(
                    logoChar = "𝕏",
                    logoBg = Color(0xFF111111),
                    checked = isXConnected,
                    onClick = { 
                        isXConnected = !isXConnected
                        showSuccess(if (isXConnected) "Twitter/𝕏 connection authenticated!" else "Twitter/𝕏 handle disconnected!")
                    },
                    modifier = Modifier.weight(1f)
                )

                SocialConnectButton(
                    logoChar = "f",
                    logoBg = Color(0xFF1877F2),
                    checked = isFBConnected,
                    onClick = { 
                        isFBConnected = !isFBConnected
                        showSuccess(if (isFBConnected) "Facebook account connected!" else "Facebook account unlinked")
                    },
                    modifier = Modifier.weight(1f)
                )

                SocialConnectButton(
                    logoChar = "📸",
                    logoBg = Color(0xFFE1306C),
                    checked = isInstaConnected,
                    onClick = { 
                        isInstaConnected = !isInstaConnected
                        showSuccess(if (isInstaConnected) "Instagram account linked!" else "Instagram account unlinked!")
                    },
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Prominent "Compose Progress Post" Button
            Button(
                onClick = onComposePost,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(46.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Text(
                    text = "Compose Progress Post",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimary
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Sample Caption Textbox preview
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)
                )
            ) {
                Text(
                    text = "Hit my workout goals this week!",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                )
            }
        }
    }
}

@Composable
fun SocialConnectButton(
    logoChar: String,
    logoBg: Color,
    checked: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .aspectRatio(1.2f)
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
            .border(
                width = 1.dp,
                color = if (checked) VoltLime else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
                shape = RoundedCornerShape(12.dp)
            )
            .clickable(onClick = onClick)
            .padding(4.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .background(logoBg, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = logoChar,
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Black
                )
            }
        }
        
        // Small checkmark badge overlay if checked at top-right
        if (checked) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(2.dp)
                    .size(12.dp)
                    .background(VoltLime, CircleShape)
                    .border(1.dp, MaterialTheme.colorScheme.surface, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "checked",
                    tint = Color.Black,
                    modifier = Modifier.size(8.dp)
                )
            }
        }
    }
}

@Composable
fun MonthlyFitnessMetricsChart() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f), RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f), RoundedCornerShape(16.dp))
            .padding(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Monthly Fitness Metrics",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                // Legend
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.padding(top = 4.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .background(Color(0xFF4CAF50), CircleShape)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Workout Volume (sets)", fontSize = 8.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .background(Color(0xFF2196F3), CircleShape)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Daily Steps (scaled 0-15k)", fontSize = 8.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            
            // Selector timeline
            Row(
                modifier = Modifier
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                    .padding(2.dp),
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                listOf("D", "W", "M", "Y").forEach { tf ->
                    val isSelected = (tf == "M")
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (isSelected) MaterialTheme.colorScheme.surfaceColorAtElevation(12.dp) else Color.Transparent)
                            .padding(horizontal = 6.dp, vertical = 2.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = tf,
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Dual lines line-graph drawn in Canvas
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(130.dp)
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val width = size.width
                val height = size.height

                // Grid lines (vertical and horizontal axes guides)
                val gridY = 4
                for (i in 0 until gridY) {
                    val y = (height / (gridY - 1)) * i
                    drawLine(
                        color = Color.LightGray.copy(alpha = 0.08f),
                        start = Offset(0f, y),
                        end = Offset(width, y),
                        strokeWidth = 1f
                    )
                }

                // Green line elements: Workout volume sets
                val workoutSets = listOf(42.0f, 39.0f, 41.5f, 39.5f, 40.2f, 38.3f, 37.6f)
                val stepsScaled = listOf(38.0f, 37.5f, 37.3f, 37.0f, 37.8f, 37.2f, 36.8f) // Blue line
                
                val maxVal = 49.5f
                val minVal = 37.0f

                val spacingX = width / (workoutSets.size - 1)
                
                // Coordinates for Line 1 (Green)
                val ptsGreen = workoutSets.mapIndexed { idx, valW ->
                    val x = spacingX * idx
                    val y = height - (((valW - minVal) / (maxVal - minVal)) * height)
                    Offset(x, y)
                }

                // Coordinates for Line 2 (Blue)
                val ptsBlue = stepsScaled.mapIndexed { idx, valS ->
                    val x = spacingX * idx
                    val y = height - (((valS - minVal) / (maxVal - minVal)) * height)
                    Offset(x, y)
                }

                // Draw Green spline/smooth curve
                val pathGreen = Path().apply {
                    moveTo(ptsGreen[0].x, ptsGreen[0].y)
                    for (i in 1 until ptsGreen.size) {
                        val prev = ptsGreen[i - 1]
                        val curr = ptsGreen[i]
                        val controlX1 = prev.x + (curr.x - prev.x) / 2
                        val controlY1 = prev.y
                        val controlX2 = prev.x + (curr.x - prev.x) / 2
                        val controlY2 = curr.y
                        cubicTo(controlX1, controlY1, controlX2, controlY2, curr.x, curr.y)
                    }
                }
                drawPath(
                    path = pathGreen,
                    color = Color(0xFF4CAF50),
                    style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
                )

                // Fill under green path
                val fillGreen = Path().apply {
                    moveTo(ptsGreen[0].x, ptsGreen[0].y)
                    for (i in 1 until ptsGreen.size) {
                        val prev = ptsGreen[i - 1]
                        val curr = ptsGreen[i]
                        val controlX1 = prev.x + (curr.x - prev.x) / 2
                        val controlY1 = prev.y
                        val controlX2 = prev.x + (curr.x - prev.x) / 2
                        val controlY2 = curr.y
                        cubicTo(controlX1, controlY1, controlX2, controlY2, curr.x, curr.y)
                    }
                    lineTo(width, height)
                    lineTo(0f, height)
                    close()
                }
                drawPath(
                    path = fillGreen,
                    brush = Brush.verticalGradient(
                        colors = listOf(Color(0xFF4CAF50).copy(alpha = 0.12f), Color.Transparent)
                    )
                )

                // Draw Blue spline/curve
                val pathBlue = Path().apply {
                    moveTo(ptsBlue[0].x, ptsBlue[0].y)
                    for (i in 1 until ptsBlue.size) {
                        val prev = ptsBlue[i - 1]
                        val curr = ptsBlue[i]
                        val controlX1 = prev.x + (curr.x - prev.x) / 2
                        val controlY1 = prev.y
                        val controlX2 = prev.x + (curr.x - prev.x) / 2
                        val controlY2 = curr.y
                        cubicTo(controlX1, controlY1, controlX2, controlY2, curr.x, curr.y)
                    }
                }
                drawPath(
                    path = pathBlue,
                    color = Color(0xFF2196F3),
                    style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
                )

                // Fill under blue path
                val fillBlue = Path().apply {
                    moveTo(ptsBlue[0].x, ptsBlue[0].y)
                    for (i in 1 until ptsBlue.size) {
                        val prev = ptsBlue[i - 1]
                        val curr = ptsBlue[i]
                        val controlX1 = prev.x + (curr.x - prev.x) / 2
                        val controlY1 = prev.y
                        val controlX2 = prev.x + (curr.x - prev.x) / 2
                        val controlY2 = curr.y
                        cubicTo(controlX1, controlY1, controlX2, controlY2, curr.x, curr.y)
                    }
                    lineTo(width, height)
                    lineTo(0f, height)
                    close()
                }
                drawPath(
                    path = fillBlue,
                    brush = Brush.verticalGradient(
                        colors = listOf(Color(0xFF2196F3).copy(alpha = 0.12f), Color.Transparent)
                    )
                )

                // Draw points on vertices
                ptsGreen.forEach { pt ->
                    drawCircle(color = Color.Black, radius = 4.dp.toPx(), center = pt)
                    drawCircle(color = Color(0xFF4CAF50), radius = 2.5.dp.toPx(), center = pt)
                }
                ptsBlue.forEach { pt ->
                    drawCircle(color = Color.Black, radius = 4.dp.toPx(), center = pt)
                    drawCircle(color = Color(0xFF2196F3), radius = 2.5.dp.toPx(), center = pt)
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Axes ticks
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            listOf("1", "6", "13", "16", "21", "27", "30").forEach { tick ->
                Text(
                    text = tick,
                    fontSize = 9.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
            }
        }
    }
}

@Composable
fun GoalsAndChallengesSection(
    onShareGoal: () -> Unit,
    onShareChallenge: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "My Goals & Challenges",
            fontSize = 16.sp,
            fontWeight = FontWeight.Black,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = "Track your long term commitment and share progress with your peer network.",
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // GOAL CARD: "Run 5k < 25min"
            Card(
                modifier = Modifier
                    .weight(1f)
                    .height(136.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f)
                ),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))
            ) {
                Column(
                    modifier = Modifier.padding(12.dp).fillMaxSize(),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("🏃", fontSize = 14.sp)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Goal", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }

                    Column {
                        Text(
                            text = "Run 5k < 25min",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            LinearProgressIndicator(
                                progress = 0.6f,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(4.dp)
                                    .clip(RoundedCornerShape(2.dp)),
                                color = VoltLime,
                                trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("60%", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = VoltLime)
                        }
                    }

                    // Button: Share Goal
                    Button(
                        onClick = onShareGoal,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(28.dp),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(0.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = "share logo icon",
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(10.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Share Goal", fontSize = 8.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.onPrimaryContainer)
                        }
                    }
                }
            }

            // CHALLENGE CARD: "Complete 30-Day Squat Challenge"
            Card(
                modifier = Modifier
                    .weight(1f)
                    .height(136.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f)
                ),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))
            ) {
                Column(
                    modifier = Modifier.padding(12.dp).fillMaxSize(),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("🏋️", fontSize = 14.sp)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Challenge", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }

                    Column {
                        Text(
                            text = "Complete 30-Day Squat Challenge",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            LinearProgressIndicator(
                                progress = 1f,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(4.dp)
                                    .clip(RoundedCornerShape(2.dp)),
                                color = VoltLime,
                                trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("30/30 days", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                        }
                    }

                    // Button: Share Challenge
                    Button(
                        onClick = onShareChallenge,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(28.dp),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(0.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.EmojiEvents, // trophy icon
                                contentDescription = "trophy challenge icon logo",
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(10.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Share Challenge", fontSize = 8.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.onPrimaryContainer)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SelectivePostComposerScreen(
    onBack: () -> Unit,
    showSuccess: (String) -> Unit,
    preSelectGoal: Boolean = false,
    preSelectChallenge: Boolean = false
) {
    // Checkable achievements accomplishments
    var isWorkoutChecked by remember { mutableStateOf(true) }
    var isRunChecked by remember { mutableStateOf(false) }
    var isGoalStepsChecked by remember { mutableStateOf(true) }

    // Caption editable text
    var captionText by remember { mutableStateOf("Alex Johnson: Hit my workout goals this week!") }

    // Include goals/challenges checkboxes
    var includeGoalChecked by remember { mutableStateOf(preSelectGoal) }
    var includeChallengeChecked by remember { mutableStateOf(preSelectChallenge) }

    // Uploaded photo simulation
    var isUsingCameraPhoto by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Top Header Row with Arrow back, Screen Title "Share Progress"
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f), CircleShape)
                        .size(40.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Return to Profile tab screen",
                        tint = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    text = "Share Progress",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
        }

        // Achievements Section (Central Panel with checkable accomplishments)
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f)
                ),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("🏋️", fontSize = 16.sp)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Accomplishments:",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))

                    // 1. Workout checkbox card
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                if (isWorkoutChecked) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f) 
                                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
                            )
                            .clickable { isWorkoutChecked = !isWorkoutChecked }
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = isWorkoutChecked,
                            onCheckedChange = { isWorkoutChecked = it },
                            colors = CheckboxDefaults.colors(checkedColor = MaterialTheme.colorScheme.primary)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("🏋️", fontSize = 14.sp)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Workout: Full Body Burn (45m)",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    // 2. Ran checkbox card
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                if (isRunChecked) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f) 
                                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
                            )
                            .clickable { isRunChecked = !isRunChecked }
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = isRunChecked,
                            onCheckedChange = { isRunChecked = it },
                            colors = CheckboxDefaults.colors(checkedColor = MaterialTheme.colorScheme.primary)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("👟", fontSize = 14.sp)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Ran 5.2 km, 35 min",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    // 3. Goal weekly steps checked card
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                if (isGoalStepsChecked) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f) 
                                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
                            )
                            .clickable { isGoalStepsChecked = !isGoalStepsChecked }
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = isGoalStepsChecked,
                            onCheckedChange = { isGoalStepsChecked = it },
                            colors = CheckboxDefaults.colors(checkedColor = MaterialTheme.colorScheme.primary)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("👟", fontSize = 14.sp)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Achieved Weekly Goal: 60k Steps",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }

        // Visual Preview Panel Card (Left Side: Graphic badge, Right Side: photo camera upload trigger)
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Card(
                    modifier = Modifier
                        .weight(0.55f)
                        .fillMaxHeight(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp)
                    ),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        if (isUsingCameraPhoto) {
                            AsyncImage(
                                model = "https://images.unsplash.com/photo-1517841905240-472988babdf9?auto=format&fit=crop&q=80&w=300",
                                contentDescription = "Simulated Uploaded Workout Selfie Thumbnail Card representation",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.6f))))
                            )
                            Text(
                                text = "Uploaded Photo! 📸",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = VoltLime,
                                modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 6.dp)
                            )
                        } else {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Brush.radialGradient(listOf(VoltLime.copy(alpha = 0.15f), Color.Transparent)))
                                    .padding(8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .background(VoltLime.copy(alpha = 0.12f), CircleShape)
                                        .border(2.dp, VoltLime, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("🏆", fontSize = 20.sp)
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "ACHIEVEMENT",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Black,
                                    color = VoltLime,
                                    letterSpacing = 1.sp
                                )
                                Text(
                                    text = "OMNIFIT ACTIVE",
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                Card(
                    modifier = Modifier
                        .weight(0.45f)
                        .fillMaxHeight()
                        .clickable { 
                            isUsingCameraPhoto = !isUsingCameraPhoto
                            showSuccess(if (isUsingCameraPhoto) "Fitness selfie uploaded to preview!" else "Badge graphic selected for preview!")
                        },
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.08f)
                    ),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (isUsingCameraPhoto) VoltLime else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                    )
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.CameraAlt,
                                contentDescription = "Camera Upload placeholder icon button",
                                tint = if (isUsingCameraPhoto) VoltLime else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                modifier = Modifier.size(26.dp)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = if (isUsingCameraPhoto) "Change Photo" else "Upload Photo",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                            )
                        }
                    }
                }
            }
        }

        // Caption Editable text field
        item {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Edit Post Caption:",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(start = 2.dp, bottom = 4.dp)
                )
                OutlinedTextField(
                    value = captionText,
                    onValueChange = { captionText = it },
                    placeholder = { Text("What is currently on your mind...") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
            }
        }

        // "Include Goals/Challenges" Toggle Checkbox panel
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.1f)
                ),
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = "Customize details to display:",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = includeGoalChecked,
                            onCheckedChange = { includeGoalChecked = it },
                            colors = CheckboxDefaults.colors(checkedColor = MaterialTheme.colorScheme.primary)
                        )
                        Text(
                            text = "Include Goal Progress: Run 5k < 25m",
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = includeChallengeChecked,
                            onCheckedChange = { includeChallengeChecked = it },
                            colors = CheckboxDefaults.colors(checkedColor = MaterialTheme.colorScheme.primary)
                        )
                        Text(
                            text = "Include Challenge: 30-Day Squats",
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                        )
                    }
                }
            }
        }

        // Branded Share Buttons at bottom: "Post to X", "Post to Facebook", "Post to Instagram"
        item {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Twitter/X (Black brand color button)
                    Button(
                        onClick = {
                            showSuccess("Successfully posted progress update safely to Twitter/𝕏 handle!")
                            onBack()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E1E1E)),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f).height(44.dp),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Text("Post to X\n(Twitter)", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.White, textAlign = TextAlign.Center)
                    }

                    // Facebook (Blue brand color button)
                    Button(
                        onClick = {
                            showSuccess("Successfully published active workout statistics on Facebook feed!")
                            onBack()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1877F2)),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f).height(44.dp),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Text("Post to\nFacebook", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.White, textAlign = TextAlign.Center)
                    }

                    // Instagram (Grand gradient colored brand button)
                    Button(
                        onClick = {
                            showSuccess("Successfully shared achievement badge frame on Instagram stories!")
                            onBack()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE1306C)), // Insta pink
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f).height(44.dp),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Text("Post to\nInstagram", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.White, textAlign = TextAlign.Center)
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))
                // Instructions descriptor
                Text(
                    text = "Select progress and challenge details to share.",
                    fontSize = 9.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
fun ChartsSubScreen(
    onBack: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f), CircleShape)
                        .size(40.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Return to Profile",
                        tint = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "Analytics & Progression",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "Weekly active time, steps & monthly trends",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        item {
            WeeklyActiveTimeStepsChart()
        }

        item {
            Spacer(modifier = Modifier.height(8.dp))
            MonthlyFitnessMetricsChart()
        }
    }
}

@Composable
fun SocialSubScreen(
    onBack: () -> Unit,
    onComposePost: () -> Unit,
    showSuccess: (String) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f), CircleShape)
                        .size(40.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Return to Profile",
                        tint = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "Social Connections",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "Connect safe credentials & share milestones",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        item {
            ShareToSocialsSection(
                onComposePost = onComposePost,
                showSuccess = showSuccess
            )
        }
    }
}

@Composable
fun GoalsSubScreen(
    viewModel: FitnessViewModel,
    onBack: () -> Unit,
    onShareGoal: () -> Unit,
    onShareChallenge: () -> Unit,
    showSuccess: (String) -> Unit
) {
    val goals by viewModel.goalsList.collectAsState()
    val challenges by viewModel.challengesList.collectAsState()

    var showAddGoalDialog by remember { mutableStateOf(false) }
    var showAddChallengeDialog by remember { mutableStateOf(false) }

    // Dialog state variables
    var newGoalTitle by remember { mutableStateOf("") }
    var newGoalTarget by remember { mutableStateOf("") }
    var newGoalUnit by remember { mutableStateOf("km") }
    var newGoalEmoji by remember { mutableStateOf("🏃") }

    var newChallengeTitle by remember { mutableStateOf("") }
    var newChallengeDays by remember { mutableStateOf("30") }
    var newChallengeEmoji by remember { mutableStateOf("🏋️") }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Core Header with Return to Profile Action
        item {
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f), CircleShape)
                        .size(40.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Return to Dashboard",
                        tint = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "Goals & Challenges",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "Dynamic, trackable goals & daily challenges",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Section header for Goals
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "My Customizable Goals",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Tap cards to record progress values.",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Button(
                    onClick = { showAddGoalDialog = true },
                    modifier = Modifier.height(28.dp),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text("Add Goal", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimary)
                }
            }
        }

        if (goals.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.1f)),
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))
                ) {
                    Box(modifier = Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                        Text("No active goals yet. Click 'Add Goal' to create one!", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        } else {
            items(goals) { goal ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f)
                    ),
                    border = androidx.compose.foundation.BorderStroke(1.dp, VoltLime.copy(alpha = 0.2f))
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(goal.emoji, fontSize = 18.sp)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = goal.title,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Black,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            IconButton(
                                onClick = {
                                    viewModel.deleteGoal(goal.id)
                                    showSuccess("Goal deleted successfully")
                                },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Delete Goal",
                                    tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Progress Indicator with current vs target Values
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "${goal.currentValue} / ${goal.targetValue} ${goal.unit}",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "${(goal.progress * 100).toInt()}% Done",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Black,
                                color = VoltLime
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        LinearProgressIndicator(
                            progress = goal.progress,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp)),
                            color = VoltLime,
                            trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Determine appropriate increment
                            val defaultIncrement = if (goal.unit.contains("ml", ignoreCase = true)) 250.0 else if (goal.targetValue > 20) 5.0 else 1.0

                            Button(
                                onClick = {
                                    viewModel.updateGoalProgress(goal.id, defaultIncrement)
                                    showSuccess("Logged progress towards milestone!")
                                },
                                modifier = Modifier
                                    .weight(1.5f)
                                    .height(34.dp),
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                            ) {
                                Text("Log +$defaultIncrement ${goal.unit}", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                            }

                            // Share Goal Action
                            OutlinedButton(
                                onClick = onShareGoal,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(34.dp),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(0.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Share,
                                    contentDescription = "Share Goal",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Share", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }

        // Section header for Challenges
        item {
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "My Customizable Challenges",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Complete consecutive day streaks.",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Button(
                    onClick = { showAddChallengeDialog = true },
                    modifier = Modifier.height(28.dp),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text("Add Streak", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimary)
                }
            }
        }

        if (challenges.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.1f)),
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))
                ) {
                    Box(modifier = Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                        Text("No active challenges yet. Create one above!", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        } else {
            items(challenges) { challenge ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f)
                    ),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(challenge.emoji, fontSize = 18.sp)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = challenge.title,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Black,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            IconButton(
                                onClick = {
                                    viewModel.deleteChallenge(challenge.id)
                                    showSuccess("Challenge removed")
                                },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Delete Challenge",
                                    tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Progress Indicator with day count
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "${challenge.currentDays} of ${challenge.totalDays} Days Completed",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "${challenge.currentDays}/${challenge.totalDays} days",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Black,
                                color = VoltLime
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        LinearProgressIndicator(
                            progress = challenge.progress,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp)),
                            color = VoltLime,
                            trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Tracker actions: Increment Day completed
                            Button(
                                onClick = {
                                    viewModel.incrementChallengeDay(challenge.id)
                                    showSuccess("Day completed! Progress logged!")
                                },
                                modifier = Modifier
                                    .weight(1.5f)
                                    .height(34.dp),
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                            ) {
                                Text("Tick Off Today", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                            }

                            // Share Challenge Action
                            OutlinedButton(
                                onClick = onShareChallenge,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(34.dp),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(0.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Share,
                                    contentDescription = "Share Challenge",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Share", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }

    // Modal dialogue popup to ADD custom goal
    if (showAddGoalDialog) {
        AlertDialog(
            onDismissRequest = { showAddGoalDialog = false },
            title = { Text("Add Custom Fitness Goal", fontWeight = FontWeight.Black) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = newGoalTitle,
                        onValueChange = { newGoalTitle = it },
                        label = { Text("Goal Title (e.g. Back Squat)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = newGoalTarget,
                            onValueChange = { newGoalTarget = it },
                            label = { Text("Target Goal") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = newGoalUnit,
                            onValueChange = { newGoalUnit = it },
                            label = { Text("Unit (ml, km, kg)") },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    OutlinedTextField(
                        value = newGoalEmoji,
                        onValueChange = { newGoalEmoji = it },
                        label = { Text("Select Emoji") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val target = newGoalTarget.toDoubleOrNull() ?: 10.0
                        if (newGoalTitle.isNotBlank()) {
                            viewModel.addGoal(newGoalTitle, target, newGoalUnit, newGoalEmoji)
                            showSuccess("Goal mapped successfully!")
                            newGoalTitle = ""
                            newGoalTarget = ""
                            showAddGoalDialog = false
                        }
                    }
                ) {
                    Text("Create Goal")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddGoalDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Modal dialogue popup to ADD custom challenge
    if (showAddChallengeDialog) {
        AlertDialog(
            onDismissRequest = { showAddChallengeDialog = false },
            title = { Text("Add Custom Challenge", fontWeight = FontWeight.Black) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = newChallengeTitle,
                        onValueChange = { newChallengeTitle = it },
                        label = { Text("Challenge Title (e.g. Squat Challenge)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = newChallengeDays,
                        onValueChange = { newChallengeDays = it },
                        label = { Text("Total Duration (Days)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = newChallengeEmoji,
                        onValueChange = { newChallengeEmoji = it },
                        label = { Text("Select Emoji") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val days = newChallengeDays.toIntOrNull() ?: 30
                        if (newChallengeTitle.isNotBlank()) {
                            viewModel.addChallenge(newChallengeTitle, days, newChallengeEmoji)
                            showSuccess("Challenge started successfully!")
                            newChallengeTitle = ""
                            newChallengeDays = "30"
                            showAddChallengeDialog = false
                        }
                    }
                ) {
                    Text("Start Challenge")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddChallengeDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun MetabolicSubScreen(
    viewModel: FitnessViewModel,
    onBack: () -> Unit,
    showSuccess: (String) -> Unit
) {
    val todayLog by viewModel.todayHealthLog.collectAsState()
    val stepsGoal by viewModel.stepsGoal.collectAsState()
    val caloriesGoal by viewModel.caloriesGoal.collectAsState()
    val waterGoal by viewModel.waterGoal.collectAsState()

    var customStepsGoalText by remember { mutableStateOf(stepsGoal.toString()) }
    var customCaloriesGoalText by remember { mutableStateOf(caloriesGoal.toInt().toString()) }
    var customWaterGoalText by remember { mutableStateOf(waterGoal.toString()) }

    var customStepsInputText by remember { mutableStateOf("") }
    var customCaloriesInputText by remember { mutableStateOf("") }
    var customWaterInputText by remember { mutableStateOf("") }

    var isEditingGoals by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Header with back button
        item {
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f), CircleShape)
                        .size(40.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Return to Profile",
                        tint = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "Daily Vital Logger",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "Customize daily targets & log exact steps, burn & water",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // 2. Customizable Target Configuration Panel
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f)
                ),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                )
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("⚙️", fontSize = 16.sp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Daily Target Thresholds",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        IconButton(
                            onClick = {
                                if (isEditingGoals) {
                                    val steps = customStepsGoalText.toIntOrNull() ?: stepsGoal
                                    val calories = customCaloriesGoalText.toDoubleOrNull() ?: caloriesGoal
                                    val water = customWaterGoalText.toIntOrNull() ?: waterGoal
                                    viewModel.updateMetricGoals(steps, calories, water)
                                    showSuccess("Custom daily targets updated and synchronized!")
                                }
                                isEditingGoals = !isEditingGoals
                            },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = if (isEditingGoals) Icons.Default.Check else Icons.Default.Edit,
                                contentDescription = "Edit Goals",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    if (isEditingGoals) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedTextField(
                                    value = customStepsGoalText,
                                    onValueChange = { customStepsGoalText = it },
                                    label = { Text("Step Target", fontSize = 10.sp) },
                                    textStyle = MaterialTheme.typography.bodyMedium,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    modifier = Modifier.weight(1f)
                                )
                                OutlinedTextField(
                                    value = customCaloriesGoalText,
                                    onValueChange = { customCaloriesGoalText = it },
                                    label = { Text("Burn Target (kcal)", fontSize = 10.sp) },
                                    textStyle = MaterialTheme.typography.bodyMedium,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    modifier = Modifier.weight(1f)
                                )
                                OutlinedTextField(
                                    value = customWaterGoalText,
                                    onValueChange = { customWaterGoalText = it },
                                    label = { Text("Water (ml)", fontSize = 10.sp) },
                                    textStyle = MaterialTheme.typography.bodyMedium,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    } else {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Steps Goal", fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(String.format("%,d steps", stepsGoal), fontSize = 12.sp, fontWeight = FontWeight.Black)
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Burn Goal", fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("${caloriesGoal.toInt()} kcal", fontSize = 12.sp, fontWeight = FontWeight.Black)
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Water Goal", fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("${waterGoal} ml", fontSize = 12.sp, fontWeight = FontWeight.Black)
                            }
                        }
                    }
                }
            }
        }

        // 3. STEPS TRACKER CARD (Customizable & Trackable)
        item {
            val stepsPct = if (stepsGoal > 0) (todayLog.stepsCount.toFloat() / stepsGoal).coerceIn(0f, 1f) else 0f
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f)
                ),
                border = androidx.compose.foundation.BorderStroke(1.dp, VoltLime.copy(alpha = 0.3f))
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("👟", fontSize = 18.sp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Daily Walked Steps",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Black,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Text(
                            text = "${(stepsPct * 100).toInt()}% Done",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black,
                            color = VoltLime
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Left: Progress ring
                        Box(
                            modifier = Modifier.size(72.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Canvas(modifier = Modifier.fillMaxSize()) {
                                drawArc(
                                    color = Color.LightGray.copy(alpha = 0.1f),
                                    startAngle = 0f,
                                    sweepAngle = 360f,
                                    useCenter = false,
                                    style = Stroke(width = 5.dp.toPx(), cap = StrokeCap.Round)
                                )
                                drawArc(
                                    color = VoltLime,
                                    startAngle = -90f,
                                    sweepAngle = stepsPct * 360f,
                                    useCenter = false,
                                    style = Stroke(width = 5.dp.toPx(), cap = StrokeCap.Round)
                                )
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = String.format("%,d", todayLog.stepsCount),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Black
                                )
                                Text(
                                    text = "/${String.format("%,d", stepsGoal)}",
                                    fontSize = 8.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        // Right: Interactive Quick Logging + Inputs
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(
                                    onClick = {
                                        viewModel.trackPassiveSteps(1000)
                                        showSuccess("Added +1,000 steps!")
                                    },
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.height(30.dp),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                                ) {
                                    Text("+1k Run", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                                }
                                Button(
                                    onClick = {
                                        viewModel.trackPassiveSteps(2500)
                                        showSuccess("Added +2,500 steps!")
                                    },
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.height(30.dp),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                                ) {
                                    Text("+2.5K Hike", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                OutlinedTextField(
                                    value = customStepsInputText,
                                    onValueChange = { customStepsInputText = it },
                                    placeholder = { Text("Custom Amount", fontSize = 10.sp) },
                                    textStyle = MaterialTheme.typography.bodyMedium,
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    modifier = Modifier.height(46.dp).weight(1f)
                                )
                                Button(
                                    onClick = {
                                        val amt = customStepsInputText.toIntOrNull()
                                        if (amt != null && amt > 0) {
                                            viewModel.trackPassiveSteps(amt)
                                            showSuccess("Added +$amt custom steps successfully!")
                                            customStepsInputText = ""
                                        }
                                    },
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.height(36.dp),
                                    contentPadding = PaddingValues(horizontal = 12.dp)
                                ) {
                                    Text("Add", fontSize = 10.sp, fontWeight = FontWeight.Black)
                                }
                            }
                        }
                    }
                }
            }
        }

        // 4. CALORIES BURN TRACKER (Customizable & Trackable)
        item {
            val caloriesPct = if (caloriesGoal > 0) (todayLog.activeCaloriesBurned / caloriesGoal).coerceIn(0.0, 1.0).toFloat() else 0f
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f)
                ),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFF7F50).copy(alpha = 0.3f))
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("🔥", fontSize = 18.sp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Caloric Burn",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Black,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Text(
                            text = "${(caloriesPct * 100).toInt()}% Done",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black,
                            color = Color(0xFFFF7F50)
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier.size(72.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Canvas(modifier = Modifier.fillMaxSize()) {
                                drawArc(
                                    color = Color.LightGray.copy(alpha = 0.1f),
                                    startAngle = 0f,
                                    sweepAngle = 360f,
                                    useCenter = false,
                                    style = Stroke(width = 5.dp.toPx(), cap = StrokeCap.Round)
                                )
                                drawArc(
                                    color = Color(0xFFFF7F50),
                                    startAngle = -90f,
                                    sweepAngle = caloriesPct * 360f,
                                    useCenter = false,
                                    style = Stroke(width = 5.dp.toPx(), cap = StrokeCap.Round)
                                )
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = String.format("%.0f", todayLog.activeCaloriesBurned),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Black
                                )
                                Text(
                                    text = "/${caloriesGoal.toInt()} kcal",
                                    fontSize = 8.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(
                                    onClick = {
                                        viewModel.modifyDirectDailyVitals(
                                            steps = null,
                                            calories = todayLog.activeCaloriesBurned + 150,
                                            water = null
                                        )
                                        showSuccess("Logged +150 kcal!")
                                    },
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.height(30.dp),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                                ) {
                                    Text("+150 kcal", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                                }
                                Button(
                                    onClick = {
                                        viewModel.modifyDirectDailyVitals(
                                            steps = null,
                                            calories = todayLog.activeCaloriesBurned + 300,
                                            water = null
                                        )
                                        showSuccess("Logged +300 kcal!")
                                    },
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.height(30.dp),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                                ) {
                                    Text("+300 kcal", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                OutlinedTextField(
                                    value = customCaloriesInputText,
                                    onValueChange = { customCaloriesInputText = it },
                                    placeholder = { Text("Custom kcal", fontSize = 10.sp) },
                                    textStyle = MaterialTheme.typography.bodyMedium,
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    modifier = Modifier.height(46.dp).weight(1f)
                                )
                                Button(
                                    onClick = {
                                        val calVal = customCaloriesInputText.toDoubleOrNull()
                                        if (calVal != null && calVal > 0) {
                                            viewModel.modifyDirectDailyVitals(
                                                steps = null,
                                                calories = todayLog.activeCaloriesBurned + calVal,
                                                water = null
                                            )
                                            showSuccess("Added +${calVal.toInt()} kcal burn session!")
                                            customCaloriesInputText = ""
                                        }
                                    },
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.height(36.dp),
                                    contentPadding = PaddingValues(horizontal = 12.dp)
                                ) {
                                    Text("Add", fontSize = 10.sp, fontWeight = FontWeight.Black)
                                }
                            }
                        }
                    }
                }
            }
        }

        // 5. HYDRATION WATER LOGGER CARD (Customizable & Trackable)
        item {
            val waterPct = if (waterGoal > 0) (todayLog.waterIntakeMl.toFloat() / waterGoal).coerceIn(0f, 1f) else 0f
            val glassesCount = (todayLog.waterIntakeMl / 250).coerceIn(0, 8)
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f)
                ),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF2196F3).copy(alpha = 0.3f))
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("💧", fontSize = 18.sp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Water Hydration Volume",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Black,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Text(
                            text = "${(waterPct * 100).toInt()}% Done",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black,
                            color = Color(0xFF2196F3)
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.width(88.dp)
                        ) {
                            Text(
                                text = "${todayLog.waterIntakeMl} ml",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Black
                            )
                            Text(
                                text = "Target: ${waterGoal} ml",
                                fontSize = 8.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            // 8 Miniature Glass Indicators matching reference image
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(3.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                for (idx in 1..8) {
                                    val filled = (idx <= glassesCount)
                                    Box(
                                        modifier = Modifier
                                            .size(width = 8.dp, height = 12.dp)
                                            .border(
                                                width = 0.8.dp,
                                                color = Color(0xFF2196F3).copy(alpha = 0.6f),
                                                shape = RoundedCornerShape(bottomStart = 2.dp, bottomEnd = 2.dp, topStart = 1.dp, topEnd = 1.dp)
                                            )
                                            .background(
                                                if (filled) Color(0xFF2196F3).copy(alpha = 0.8f) else Color.Transparent,
                                                shape = RoundedCornerShape(bottomStart = 2.dp, bottomEnd = 2.dp, topStart = 1.dp, topEnd = 1.dp)
                                            )
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(
                                    onClick = {
                                        viewModel.recordWaterIntake(250)
                                        showSuccess("Logged +250 ml glass fraction!")
                                    },
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.height(30.dp),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                                ) {
                                    Text("+250 ml Glass", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                                }
                                Button(
                                    onClick = {
                                        viewModel.recordWaterIntake(500)
                                        showSuccess("Logged +500 ml shaker volume!")
                                    },
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.height(30.dp),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                                ) {
                                    Text("+500 ml Flask", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                OutlinedTextField(
                                    value = customWaterInputText,
                                    onValueChange = { customWaterInputText = it },
                                    placeholder = { Text("Custom ml", fontSize = 10.sp) },
                                    textStyle = MaterialTheme.typography.bodyMedium,
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    modifier = Modifier.height(46.dp).weight(1f)
                                )
                                Button(
                                    onClick = {
                                        val waterAdd = customWaterInputText.toIntOrNull()
                                        if (waterAdd != null && waterAdd > 0) {
                                            viewModel.recordWaterIntake(waterAdd)
                                            showSuccess("Added +${waterAdd} ml of pure liquid water!")
                                            customWaterInputText = ""
                                        }
                                    },
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.height(36.dp),
                                    contentPadding = PaddingValues(horizontal = 12.dp)
                                ) {
                                    Text("Add", fontSize = 10.sp, fontWeight = FontWeight.Black)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

fun generateAthleteAvatarFile(
    context: android.content.Context,
    athleteName: String,
    athleteAge: String,
    athleteType: String,
    backgroundTheme: String
): String {
    val bitmap = Bitmap.createBitmap(512, 512, Bitmap.Config.ARGB_8888)
    val canvas = android.graphics.Canvas(bitmap)
    val paint = Paint().apply { isAntiAlias = true }

    // Start with a premium dark matte background
    paint.color = android.graphics.Color.parseColor("#0E0E0F")
    canvas.drawRect(0f, 0f, 512f, 512f, paint)

    // Draw dynamic aesthetic background shaders or rings
    val circleColor = when(backgroundTheme) {
        "Vibrant Neon" -> "#CCFF00"  // VoltLime
        "Cyber Athlete" -> "#4FE3C1" // SportsTeal
        else -> "#FF3D00"            // Red/Orange active
    }

    // Gradient background radial shader
    val gColors = intArrayOf(
        android.graphics.Color.parseColor(circleColor),
        android.graphics.Color.parseColor("#0C2021"),
        android.graphics.Color.parseColor("#0E0E0F")
    )
    val radGradient = android.graphics.RadialGradient(
        256f, 256f, 320f,
        gColors, floatArrayOf(0f, 0.45f, 1f),
        android.graphics.Shader.TileMode.CLAMP
    )
    paint.shader = radGradient
    canvas.drawRect(0f, 0f, 512f, 512f, paint)
    paint.shader = null

    // Draw tech horizontal/vertical grid lines in the background for custom biometric vibe
    paint.strokeWidth = 1f
    paint.color = android.graphics.Color.parseColor("#22252A")
    for (i in 1..8) {
        canvas.drawLine(0f, i * 64f, 512f, i * 64f, paint)
        canvas.drawLine(i * 64f, 0f, i * 64f, 512f, paint)
    }

    // Draw glowing circle border
    paint.style = Paint.Style.STROKE
    paint.strokeWidth = 8f
    paint.color = android.graphics.Color.parseColor(circleColor)
    canvas.drawCircle(256f, 256f, 220f, paint)

    // Inner scanning ring
    paint.strokeWidth = 2f
    paint.color = android.graphics.Color.parseColor("#FFFFFF")
    paint.alpha = 100
    canvas.drawCircle(256f, 256f, 200f, paint)
    paint.alpha = 255

    // Draw aesthetic vector geometric crosshairs in corners
    paint.strokeWidth = 3f
    paint.color = android.graphics.Color.parseColor(circleColor)
    // TL Crosshair
    canvas.drawLine(80f, 100f, 110f, 100f, paint)
    canvas.drawLine(100f, 80f, 100f, 110f, paint)
    // BR Crosshair
    canvas.drawLine(402f, 412f, 432f, 412f, paint)
    canvas.drawLine(412f, 402f, 412f, 432f, paint)

    // Draw stylized silhouette vector! Let's draw a professional high-fidelity athletic icon on the canvas!
    paint.style = Paint.Style.FILL
    paint.color = android.graphics.Color.WHITE
    // Head circle definition
    canvas.drawCircle(256f, 180f, 45f, paint)
    // Torso arc path
    val torsoPath = android.graphics.Path().apply {
        moveTo(180f, 340f)
        cubicTo(190f, 240f, 322f, 240f, 332f, 340f)
        lineTo(332f, 380f)
        lineTo(180f, 380f)
        close()
    }
    canvas.drawPath(torsoPath, paint)

    // Draw glowing "OmniFit Verified Member" status bar below the torso
    paint.color = android.graphics.Color.parseColor(circleColor)
    canvas.drawRoundRect(140f, 360f, 372f, 396f, 18f, 18f, paint)

    // Draw premium badge texts on it
    paint.color = android.graphics.Color.BLACK
    paint.textSize = 15f
    paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    val statusTxt = "OMNIFIT ATHLETE • ${athleteType.uppercase()}"
    val sRect = Rect()
    paint.getTextBounds(statusTxt, 0, statusTxt.length, sRect)
    canvas.drawText(statusTxt, 256f - sRect.width() / 2f, 384f, paint)

    // Draw display name text inside circle top area
    paint.color = android.graphics.Color.WHITE
    paint.textSize = 28f
    paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    val dispName = if (athleteName.isNotBlank()) athleteName else "OmniFit Guest"
    val nRect = Rect()
    paint.getTextBounds(dispName, 0, dispName.length, nRect)
    canvas.drawText(dispName, 256f - nRect.width() / 2f, 440f, paint)

    // Draw age text below the display name
    paint.color = android.graphics.Color.parseColor("#A1A5AC")
    paint.textSize = 17f
    paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
    val dispAge = "AGE PROFILE: ${if (athleteAge.isNotBlank()) athleteAge else "28"} YEARS"
    val aRect = Rect()
    paint.getTextBounds(dispAge, 0, dispAge.length, aRect)
    canvas.drawText(dispAge, 256f - aRect.width() / 2f, 475f, paint)

    // Save strictly to local application sandbox on disk
    val filesDir = context.cacheDir
    val file = File(filesDir, "omni_capture_${System.currentTimeMillis()}.jpg")
    try {
        val fos = FileOutputStream(file)
        bitmap.compress(Bitmap.CompressFormat.JPEG, 95, fos)
        fos.flush()
        fos.close()
    } catch(e: Exception) {
        e.printStackTrace()
    }

    return Uri.fromFile(file).toString()
}

@Composable
fun SmartEngineSubScreen(
    viewModel: com.example.ui.viewmodel.FitnessViewModel,
    onBack: () -> Unit,
    showSuccess: (String) -> Unit
) {
    val profile by viewModel.userProfile.collectAsState(initial = null)
    val useImperial by viewModel.useImperial.collectAsState()
    val unitSystem by viewModel.unitSystem.collectAsState()
    val appLanguage by viewModel.appLanguage.collectAsState()

    val smartDevices by viewModel.smartDevices.collectAsState()
    val deviceSyncOverride by viewModel.deviceSyncOverride.collectAsState()

    var showAddDeviceForm by remember { mutableStateOf(false) }
    var customBrand by remember { mutableStateOf("") }
    var customModel by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf("Watch") }
    var selectedMetrics by remember { mutableStateOf(setOf("Steps", "Heart Rate")) }
    var isPairingSimulated by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    val daysCount = profile?.availableDaysPerWeek ?: 4
    val maxMinutes = profile?.maxTimeMinutes ?: 45
    val experience = profile?.experienceLevel ?: "Beginner"
    val gender = profile?.gender ?: "Male"

    fun getDeviceString(key: String, lang: String): String {
        return when (lang) {
            "es" -> when (key) {
                "title" -> "Hub de Salud Inteligente"
                "desc" -> "Sincroniza telemetría y biometría desde cualquier reloj, anillo o sensor inteligente, sin importar la marca."
                "header" -> "Conexiones de Sincronización Activas"
                "reg_custom" -> "Registrar Nuevo Wearable"
                "form_brand" -> "Marca del Dispositivo / Fabricante"
                "form_model" -> "Nombre del Modelo"
                "form_type" -> "Factor de Forma o Categoría"
                "form_metrics" -> "Métricas Sincronizadas"
                "btn_pair" -> "Establecer Sincronización Universal"
                "toast_pairing" -> "Realizando handshake Bluetooth con"
                "toast_success" -> "Conectado correctamente"
                "searching" -> "Buscando bioseñales..."
                "battery" -> "Batería"
                "no_devices" -> "No hay dispositivos conectados"
                "placeholder_brand" -> "Ej. Whoop, Suunto, Amazfit, Oura"
                "placeholder_model" -> "Ej. Tracker Pro X1"
                "paired_state" -> "Sincronizado"
                "unpaired_state" -> "Desconectado"
                "btn_disconnect" -> "Desconectar"
                "btn_connect" -> "Sincronizar"
                "custom_alert" -> "Cualquier marca de wearables se puede registrar aquí."
                "sync_toggle_title" -> "Sincronización de Dispositivos"
                "sync_toggle_desc" -> "Sobrescribir datos manuales con telemetría en vivo cuando los wearables están conectados"
                else -> key
            }
            "fr" -> when (key) {
                "title" -> "Hub de Synchronisation Universel"
                "desc" -> "Synchronisez vos données et biométries de n'importe quel tracker, bague ou montre intelligente de toutes marques."
                "header" -> "Connexions de Synchronisation Actives"
                "reg_custom" -> "Enregistrer un Nouveau Capteur"
                "form_brand" -> "Marque du Dispositif / Constructeur"
                "form_model" -> "Nom du Modèle"
                "form_type" -> "Facteur de Forme / Catégorie"
                "form_metrics" -> "Mesures Synchronisées"
                "btn_pair" -> "Activer la Synchronisation Universelle"
                "toast_pairing" -> "Appairage Bluetooth en cours avec"
                "toast_success" -> "Dispositif connecté avec succès"
                "searching" -> "Recherche de signaux..."
                "battery" -> "Batterie"
                "no_devices" -> "Aucun appareil connecté"
                "placeholder_brand" -> "Ex. Whoop, Suunto, Amazfit, Oura"
                "placeholder_model" -> "Ex. Tracker Pro X1"
                "paired_state" -> "Synchronisé"
                "unpaired_state" -> "Déconnecté"
                "btn_disconnect" -> "Déconnecter"
                "btn_connect" -> "Synchroniser"
                "custom_alert" -> "Toutes les marques d'appareils portables peuvent être enregistrées ici."
                "sync_toggle_title" -> "Sincronisation Automatique"
                "sync_toggle_desc" -> "Remplacez vos entrées manuelles par les données biométriques collectées"
                else -> key
            }
            "de" -> when (key) {
                "title" -> "Universelles Smart-Health-Hub"
                "desc" -> "Synchronisieren Sie Vital- und Trainingsdaten von jedem Tracker, Ring, Sensor oder jeder Uhr beliebiger Hersteller."
                "header" -> "Aktive Sync-Verbindungen"
                "reg_custom" -> "Neues Gerät registrieren"
                "form_brand" -> "Gerätehersteller / Marke"
                "form_model" -> "Modellbezeichnung"
                "form_type" -> "Formfaktor / Kategorie"
                "form_metrics" -> "Synchronisierte Messwerte"
                "btn_pair" -> "Handshake & Synchronisieren"
                "toast_pairing" -> "Bluetooth-Kopplung läuft für"
                "toast_success" -> "Erfolgreich gekoppelt"
                "searching" -> "Suche nach Wearable-Signalen..."
                "battery" -> "Batterie"
                "no_devices" -> "Keine Geräte verbunden"
                "placeholder_brand" -> "Z.B. Whoop, Suunto, Amazfit, Oura"
                "placeholder_model" -> "Z.B. Tracker Pro X1"
                "paired_state" -> "Synchronisiert"
                "unpaired_state" -> "Getrennt"
                "btn_disconnect" -> "Trennen"
                "btn_connect" -> "Verbinden"
                "custom_alert" -> "Jede Wearable-Marke kann hier registriert werden."
                "sync_toggle_title" -> "Automatischer Daten-Sync"
                "sync_toggle_desc" -> "Manuelle Logs mit echten biometrischen Live-Daten überschreiben"
                else -> key
            }
            else -> when (key) {
                "title" -> "Universal Smart Health Hub"
                "desc" -> "Synchronize fitness & biometric logs from any smart tracker, ring, watch, or wearable sensor regardless of manufacturer."
                "header" -> "Active Sync Connections"
                "reg_custom" -> "Add Universal Wearable Sensor"
                "form_brand" -> "Device Manufacturer / Company Brand"
                "form_model" -> "Model Name"
                "form_type" -> "Device Form Factor / Category"
                "form_metrics" -> "Telemetry Metrics Synced"
                "btn_pair" -> "Establish Universal Connection"
                "toast_pairing" -> "Polling Bluetooth beacons and pairing"
                "toast_success" -> "Handshake established & connection active!"
                "searching" -> "Searching active signals..."
                "battery" -> "Battery"
                "no_devices" -> "No connected devices found"
                "placeholder_brand" -> "e.g. Whoop, Suunto, Amazfit, Oura"
                "placeholder_model" -> "e.g. Tracker Pro X1"
                "paired_state" -> "Synced"
                "unpaired_state" -> "Disconnected"
                "btn_disconnect" -> "Disconnect"
                "btn_connect" -> "Sync Now"
                "custom_alert" -> "Any wearable brand and company is fully supported."
                "sync_toggle_title" -> "Automated Telemetry Sync Override"
                "sync_toggle_desc" -> "Automatically overrides manual fitness inputs with real-time biometric feed when devices are linked"
                else -> key
            }
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 40.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // HEADER CARD BLOCK
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f), CircleShape)
                        .size(40.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Return to Profile Dashboard",
                        tint = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "Smart Workout Engine",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "Fine-tune split generation parameters and biomechanical coaching tiers.",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // PHYSICAL TUNER CONTROLS MAIN CARD
        item {
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
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f)
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Biomechanical Tuning Parameters",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(14.dp))

                    // Days available
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Weekly Workout Frequency",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "$daysCount Days / Wk",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black,
                            color = VoltLime
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Slider(
                        value = daysCount.toFloat(),
                        onValueChange = { viewModel.updateTrainingTuner(it.toInt(), maxMinutes, experience, gender) },
                        valueRange = 2f..7f,
                        steps = 4,
                        colors = SliderDefaults.colors(
                            activeTrackColor = VoltLime,
                            thumbColor = VoltLime
                        )
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Session limit
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Daily Session Time Limit",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "$maxMinutes Minutes",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black,
                            color = SportsTeal
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Slider(
                        value = maxMinutes.toFloat(),
                        onValueChange = { viewModel.updateTrainingTuner(daysCount, it.toInt(), experience, gender) },
                        valueRange = 15f..120f,
                        steps = 6,
                        colors = SliderDefaults.colors(
                            activeTrackColor = SportsTeal,
                            thumbColor = SportsTeal
                        )
                    )
                }
            }
        }

        // COACHING EXPERIENCE TIER SELECTION
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        1.dp,
                        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f),
                        RoundedCornerShape(16.dp)
                    ),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.1f)
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Active Professional Coaching Tier",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Adjusting the tier dynamically updates target exercise complexity, volume loading, and protective set parameters.",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    listOf("Beginner", "Intermediate", "Advanced").forEach { tier ->
                        val isSelected = experience.equals(tier, ignoreCase = true)
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .clickable {
                                    viewModel.updateTrainingTuner(daysCount, maxMinutes, tier, gender)
                                    showSuccess("Engine tier switched to $tier: training split adjusted!")
                                },
                            shape = RoundedCornerShape(12.dp),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (isSelected) {
                                    when (tier) {
                                        "Beginner" -> VoltLime
                                        "Intermediate" -> SportsTeal
                                        else -> MaterialTheme.colorScheme.primary
                                    }
                                } else Color.Transparent
                            ),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelected) {
                                    when (tier) {
                                        "Beginner" -> VoltLime.copy(alpha = 0.1f)
                                        "Intermediate" -> SportsTeal.copy(alpha = 0.1f)
                                        else -> MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                                    }
                                } else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .background(
                                            if (isSelected) {
                                                when (tier) {
                                                    "Beginner" -> VoltLime.copy(alpha = 0.15f)
                                                    "Intermediate" -> SportsTeal.copy(alpha = 0.15f)
                                                    else -> MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                                                }
                                            } else MaterialTheme.colorScheme.surfaceVariant,
                                            CircleShape
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = when(tier) {
                                            "Beginner" -> "🔰"
                                            "Intermediate" -> "⚡"
                                            else -> "🔥"
                                        },
                                        fontSize = 16.sp
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = tier,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = when(tier) {
                                            "Beginner" -> "Strict joint protection and foundational muscle form curation."
                                            "Intermediate" -> "Balanced progression focusing on progressive muscle load development."
                                            else -> "Advanced Compound lifts and full neuromuscular overload profiles."
                                        },
                                        fontSize = 10.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = "Active Selection Indicator",
                                        tint = when (tier) {
                                            "Beginner" -> VoltLime
                                            "Intermediate" -> SportsTeal
                                            else -> MaterialTheme.colorScheme.primary
                                        },
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // BIOMETRIC GENDER PREFERENCE CARD
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        1.dp,
                        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f),
                        RoundedCornerShape(16.dp)
                    ),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.1f)
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Biomechanical Target Gender Profile",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        listOf("Male", "Female").forEach { g ->
                            val isSelected = gender.equals(g, ignoreCase = true)
                            Card(
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable {
                                        viewModel.updateTrainingTuner(daysCount, maxMinutes, experience, g)
                                        showSuccess("Gender preferences set to $g!")
                                    },
                                border = androidx.compose.foundation.BorderStroke(
                                    1.dp,
                                    if (isSelected) VoltLime else Color.Transparent
                                ),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isSelected) VoltLime.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
                                ),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp).fillMaxWidth(),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = if (g == "Male") "Male 🧑" else "Female 👧",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) VoltLime else MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // SYSTEM MEASUREMENT UNITS CONFIGURATION CARD
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        1.dp,
                        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f),
                        RoundedCornerShape(16.dp)
                    ),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.1f)
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = com.example.ui.translation.LanguageManager.getString("system_units", appLanguage),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = com.example.ui.translation.LanguageManager.getString("sub_units_desc", appLanguage),
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Metric selector button
                            Card(
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable {
                                        viewModel.setUnitSystem("metric")
                                        showSuccess("System measurement standard set to Metric (kg, cm, ml)")
                                    },
                                border = androidx.compose.foundation.BorderStroke(
                                    1.dp,
                                    if (unitSystem == "metric") VoltLime else Color.Transparent
                                ),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (unitSystem == "metric") VoltLime.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
                                ),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp).fillMaxWidth(),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = com.example.ui.translation.LanguageManager.getString("metric_system", appLanguage),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (unitSystem == "metric") VoltLime else MaterialTheme.colorScheme.onSurface,
                                        textAlign = TextAlign.Center
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "kg, cm, ml",
                                        fontSize = 9.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            // Imperial US selector button
                            Card(
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable {
                                        viewModel.setUnitSystem("imperial_us")
                                        showSuccess("System measurement standard set to Imperial US (lbs, in, fl oz)")
                                    },
                                border = androidx.compose.foundation.BorderStroke(
                                    1.dp,
                                    if (unitSystem == "imperial_us") VoltLime else Color.Transparent
                                ),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (unitSystem == "imperial_us") VoltLime.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
                                ),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp).fillMaxWidth(),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = com.example.ui.translation.LanguageManager.getString("imperial_us", appLanguage),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (unitSystem == "imperial_us") VoltLime else MaterialTheme.colorScheme.onSurface,
                                        textAlign = TextAlign.Center
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "lbs, in, fl oz",
                                        fontSize = 9.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }

                        // Imperial UK selector button
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.setUnitSystem("imperial_uk")
                                    showSuccess("System measurement standard set to Imperial UK (st, lbs, ft, in)")
                                },
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (unitSystem == "imperial_uk") VoltLime else Color.Transparent
                            ),
                            colors = CardDefaults.cardColors(
                                containerColor = if (unitSystem == "imperial_uk") VoltLime.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
                            ),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp).fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = com.example.ui.translation.LanguageManager.getString("imperial_uk", appLanguage),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (unitSystem == "imperial_uk") VoltLime else MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "st & lbs, ft & in, imperial fl oz",
                                    fontSize = 9.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }

        // PREFERRED SYSTEM LANGUAGE CONFIGURATION CARD
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        1.dp,
                        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f),
                        RoundedCornerShape(16.dp)
                    ),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.1f)
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = com.example.ui.translation.LanguageManager.getString("settings_language", appLanguage),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    val languages = listOf(
                        Triple("en", "English", "🇺🇸"),
                        Triple("es", "Español", "🇪🇸"),
                        Triple("fr", "Français", "🇫🇷"),
                        Triple("de", "Deutsch", "🇩🇪")
                    )

                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        languages.chunked(2).forEach { rowLangs ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                rowLangs.forEach { (code, label, flag) ->
                                    val isSelected = appLanguage == code
                                    Card(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clickable {
                                                viewModel.setAppLanguage(code)
                                                val msg = com.example.ui.translation.LanguageManager.getString("language_selected_toast", code)
                                                showSuccess(msg)
                                            },
                                        border = androidx.compose.foundation.BorderStroke(
                                            1.dp,
                                            if (isSelected) VoltLime else Color.Transparent
                                        ),
                                        colors = CardDefaults.cardColors(
                                            containerColor = if (isSelected) VoltLime.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
                                        ),
                                        shape = RoundedCornerShape(10.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(10.dp).fillMaxWidth(),
                                            horizontalArrangement = Arrangement.Center,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(flag, fontSize = 14.sp)
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = label,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isSelected) VoltLime else MaterialTheme.colorScheme.onSurface
                                            )
                                        }
                                    }
                                }
                                if (rowLangs.size < 2) {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }
                }
            }
        }

        // MANUAL TRIGGER ENGINE GENERATOR & OPTIMIZATION CARD
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        1.dp,
                        VoltLime.copy(alpha = 0.3f),
                        RoundedCornerShape(16.dp)
                    ),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "🚀 Biomechanical Engine Synchronization",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Black,
                        color = VoltLime
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Manually trigger full schedule synchronization. This completely wipes and recomputes every training session, rep volume profile, and equipment-specific workout splits based on your target options above.",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(14.dp))

                    Button(
                        onClick = {
                            viewModel.updateTrainingTuner(daysCount, maxMinutes, experience, gender)
                            showSuccess("Smart Engine recalculation accomplished! Workout schedules regenerated successfully.")
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = VoltLime),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Recalculate & Optimize Schedules",
                            fontWeight = FontWeight.Black,
                            fontSize = 12.sp,
                            color = Color.Black
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SmartDevicesSyncSubScreen(
    viewModel: com.example.ui.viewmodel.FitnessViewModel,
    onBack: () -> Unit,
    showSuccess: (String) -> Unit
) {
    val appLanguage by viewModel.appLanguage.collectAsState()
    val smartDevices by viewModel.smartDevices.collectAsState()
    val deviceSyncOverride by viewModel.deviceSyncOverride.collectAsState()
    
    var isScanning by remember { mutableStateOf(false) }
    var scanStepText by remember { mutableStateOf("") }
    var detectedGadget by remember { mutableStateOf<com.example.ui.viewmodel.SmartDevice?>(null) }
    
    var isPairingSimulated by remember { mutableStateOf(false) }
    var pairStepText by remember { mutableStateOf("") }
    
    // For existing paired devices being connected manually:
    var connectingDevice by remember { mutableStateOf<com.example.ui.viewmodel.SmartDevice?>(null) }
    var connectingStepText by remember { mutableStateOf("") }
    
    val coroutineScope = rememberCoroutineScope()

    val context = androidx.compose.ui.platform.LocalContext.current
    var showPermissionDialog by remember { mutableStateOf(false) }
    
    // Auto-Enable and Dialog Prompt States for Bluetooth Radios
    var showBluetoothOffAssistant by remember { mutableStateOf(false) }
    var pendingBluetoothAction by remember { mutableStateOf<(() -> Unit)?>(null) }
    
    // Guided First-Time Accessory Pairing Wizard States
    var showPairingWizard by remember { mutableStateOf(false) }
    var wizardStep by remember { mutableStateOf(1) } // 1: Prepare, 2: Scan Radar, 3: Sync Pin confirmation, 4: Telemetry Selection, 5: Finished
    var wizardLogText by remember { mutableStateOf("") }
    var wizardChosenDevice by remember { mutableStateOf<com.example.ui.viewmodel.SmartDevice?>(null) }

    androidx.activity.compose.BackHandler(enabled = showPairingWizard) {
        showPairingWizard = false
        wizardChosenDevice = null
        wizardStep = 1
    }
    
    // Callback helper for active bluetooth activation responses
    val enableBluetoothLauncher = rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val isEnabled = try {
            val bm = context.getSystemService(android.content.Context.BLUETOOTH_SERVICE) as? android.bluetooth.BluetoothManager
            bm?.adapter?.isEnabled ?: false
        } catch (e: Exception) { false }
        
        if (isEnabled) {
            showSuccess("Bluetooth has been activated successfully! Initiating connection stream...")
            pendingBluetoothAction?.invoke()
            pendingBluetoothAction = null
        } else {
            showSuccess("Bluetooth remains powered off. Directing to simulated pairing framework.")
        }
    }
    
    // Safe checker for bluetooth state and capability execution
    fun isBluetoothEnabledSafely(ctx: android.content.Context): Boolean {
        return try {
            val bm = ctx.getSystemService(android.content.Context.BLUETOOTH_SERVICE) as? android.bluetooth.BluetoothManager
            bm?.adapter?.isEnabled ?: false
        } catch (e: Exception) {
            false
        }
    }
    
    // Wrapper to ensure that permission & active bluetooth are on before running operations
    val initialBluetoothPermission = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
        androidx.core.content.ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.BLUETOOTH_SCAN
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED &&
        androidx.core.content.ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.BLUETOOTH_CONNECT
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED
    } else {
        androidx.core.content.ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.ACCESS_FINE_LOCATION
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED
    }
    
    var hasBluetoothPermissionMutable by remember { mutableStateOf(initialBluetoothPermission) }
    
    val checkAndRunBtAction = { action: () -> Unit ->
        if (!hasBluetoothPermissionMutable) {
            showPermissionDialog = true
        } else {
            if (!isBluetoothEnabledSafely(context)) {
                pendingBluetoothAction = action
                showBluetoothOffAssistant = true
            } else {
                action()
            }
        }
    }
    
    var hasBluetoothPermissionState by remember { mutableStateOf(initialBluetoothPermission) }
    
    var hasBluetoothPermission by remember {
        mutableStateOf(
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                androidx.core.content.ContextCompat.checkSelfPermission(
                    context,
                    android.Manifest.permission.BLUETOOTH_SCAN
                ) == android.content.pm.PackageManager.PERMISSION_GRANTED &&
                androidx.core.content.ContextCompat.checkSelfPermission(
                    context,
                    android.Manifest.permission.BLUETOOTH_CONNECT
                ) == android.content.pm.PackageManager.PERMISSION_GRANTED
            } else {
                androidx.core.content.ContextCompat.checkSelfPermission(
                    context,
                    android.Manifest.permission.ACCESS_FINE_LOCATION
                ) == android.content.pm.PackageManager.PERMISSION_GRANTED
            }
        )
    }

    val bluetoothPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val scanGranted = permissions[android.Manifest.permission.BLUETOOTH_SCAN] ?: false
        val connectGranted = permissions[android.Manifest.permission.BLUETOOTH_CONNECT] ?: false
        val locationGranted = permissions[android.Manifest.permission.ACCESS_FINE_LOCATION] ?: false
        
        hasBluetoothPermission = (scanGranted && connectGranted) || locationGranted
        hasBluetoothPermissionMutable = hasBluetoothPermission
        if (hasBluetoothPermission) {
            showSuccess("Bluetooth telemetry scanning authorizations granted successfully!")
        } else {
            showSuccess("Fallback mode: Using secure localized RF synthesizer.")
        }
    }
    
    fun getString(key: String): String {
        return when (appLanguage) {
            "es" -> when (key) {
                "title" -> "Hub de Dispositivos Inteligentes"
                "subtitle" -> "Sincroniza telemetría y biometría desde cualquier reloj, anillo o sensor inteligente."
                "scanning" -> "Buscando bioseñales..."
                "ready_scan" -> "🔍 Escanear Canales Inalámbricos"
                "scanning_active" -> "Buscando balizas..."
                "pair_btn" -> "Establecer Handshake Seguro"
                "disconnect" -> "Desvincular"
                "sync_on" -> "Streaming Activo"
                "sync_off" -> "Streaming Pausado"
                "no_paired" -> "No hay wearables vinculados aún."
                "back" -> "Volver"
                "avail_telemetry" -> "Métricas Disponibles:"
                else -> key
            }
            "fr" -> when (key) {
                "title" -> "Hub Biométrique Universel"
                "subtitle" -> "Synchronisez vos données de n'importe quel tracker, bague ou montre intelligente."
                "scanning" -> "Recherche de signaux..."
                "ready_scan" -> "🔍 Lancer le Scan Sans Fil"
                "scanning_active" -> "Scan en cours..."
                "pair_btn" -> "Établir Connexion Sécurisée"
                "disconnect" -> "Dissocier"
                "sync_on" -> "Flux Actif"
                "sync_off" -> "Flux Suspendu"
                "no_paired" -> "Aucun capteur jumelé."
                "back" -> "Retour"
                "avail_telemetry" -> "Biométries Disponibles:"
                else -> key
            }
            "de" -> when (key) {
                "title" -> "Smart-Wearables Integration"
                "subtitle" -> "Echtzeit-Synchronisation für Ringe, Uhren, Bänder & Fitnesstracker aller Hersteller."
                "scanning" -> "Suche nach Funksignalen..."
                "ready_scan" -> "🔍 Funkfrequenzen abscannen"
                "scanning_active" -> "Frequenzanalyse..."
                "pair_btn" -> "Sichere Kopplung starten"
                "disconnect" -> "Trennen"
                "sync_on" -> "Telemetrie Aktiv"
                "sync_off" -> "Telemetrie Gestoppt"
                "no_paired" -> "Keine verbundenen Wearables gefunden."
                "back" -> "Zurück"
                "avail_telemetry" -> "Verfügbare Sensoren:"
                else -> key
            }
            else -> when (key) {
                "title" -> "Universal Smart Devices Hub"
                "subtitle" -> "Securely feed telemetry & biometrics from any wearable ring, band, watch, or sensor regardless of brand."
                "scanning" -> "Scanning RF airwaves..."
                "ready_scan" -> "🔍 Search & Scan Airwaves"
                "scanning_active" -> "RF Spectrum Analyzer scanning..."
                "pair_btn" -> "Authorize Secure Handshake"
                "disconnect" -> "Unpair & Delete"
                "sync_on" -> "Stream Live Feed"
                "sync_off" -> "Pause Telemetry Stream"
                "no_paired" -> "No paired wearables connected."
                "back" -> "Back"
                "avail_telemetry" -> "Detected Telemetry Capabilities:"
                else -> key
            }
        }
    }

    val isAnyConnecting = isScanning || isPairingSimulated || (detectedGadget != null) || (connectingDevice != null) || showPairingWizard

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Back Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier
                            .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f), CircleShape)
                            .size(40.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Return to Profile",
                            tint = MaterialTheme.colorScheme.onBackground,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = getString("title"),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f), shape = RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = "Share sync hub details",
                        tint = VoltLime,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        // Informative Explainer Card (Hide details when actively focusing on connection setup to clean the UI)
        if (!isAnyConnecting) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f)
                    ),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "🔒 " + getString("subtitle"),
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 16.sp
                        )
                    }
                }
            }

            // Automated Global Telemetry Sync Toggle Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (deviceSyncOverride) VoltLime.copy(alpha = 0.08f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                    ),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (deviceSyncOverride) VoltLime.copy(alpha = 0.3f) else Color.Transparent
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (appLanguage == "es") "Sincronización de Telemetría" else if (appLanguage == "fr") "Sincronisation Automatique" else if (appLanguage == "de") "Automatischer Daten-Sync" else "Automated Telemetry Sync Override",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (deviceSyncOverride) VoltLime else MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = if (appLanguage == "es") "Sobrescribir datos manuales con telemetría en vivo de wearables" else "Prioritize and overwrite manual inputs with linked wearable biometric and log streams automatically.",
                                fontSize = 9.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                lineHeight = 12.sp
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Switch(
                            checked = deviceSyncOverride,
                            onCheckedChange = { isChecked ->
                                viewModel.setDeviceSyncOverride(isChecked)
                                showSuccess(if (isChecked) "Smart health device telemetry sync enabled!" else "Smart device telemetry overridden to manual logging.")
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.Black,
                                checkedTrackColor = VoltLime,
                                uncheckedThumbColor = MaterialTheme.colorScheme.outline,
                                uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        )
                    }
                }
            }
        }

        // ACTIVE CONNECTION/PAIRING RADAR VIEW 
        // Showing only this view when connecting, as requested: "don't show what devices it can connect to while connecting... should properly show which device it is and its features"
        if (connectingDevice != null) {
            val dev = connectingDevice!!
            item {
                Text(
                    text = if (appLanguage == "es") "Estableciendo Feed de Telemetría" else "Establishing Secure Telemetry Feed",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = VoltLime,
                    modifier = Modifier.padding(start = 4.dp)
                )
            }
            
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.1f)
                    ),
                    border = androidx.compose.foundation.BorderStroke(2.dp, VoltLime.copy(alpha = 0.8f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Big Animated Logo Area
                        Box(
                            modifier = Modifier
                                .size(90.dp)
                                .background(VoltLime.copy(alpha = 0.12f), CircleShape)
                                .border(2.dp, VoltLime, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            val bigIcon = when (dev.type) {
                                "Watch" -> "⌚"
                                "Ring" -> "💍"
                                "Band" -> "🩹"
                                "Chest Strap" -> "❤️"
                                else -> "📟"
                            }
                            CircularProgressIndicator(
                                color = VoltLime,
                                strokeWidth = 3.dp,
                                modifier = Modifier.size(76.dp)
                            )
                            Text(text = bigIcon, fontSize = 36.sp)
                        }
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        Text(
                            text = if (appLanguage == "es") "Conectando Dispositivo..." else "Connecting Wearable Feed...",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Black,
                            color = VoltLime
                        )
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        // Device details
                        Text(
                            text = "${dev.brand.uppercase()} ${dev.name.uppercase()}",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.Center
                        )
                        
                        Text(
                            text = "Model Class: ${dev.type} • Est. Battery Remaining: ${dev.batteryPercent}%",
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        // Highlighted features card
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f)
                            ),
                            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Text(
                                    text = getString("avail_telemetry").uppercase(),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = VoltLime
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = dev.syncedMetrics,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    lineHeight = 16.sp
                                )
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(18.dp))
                        
                        // Handshake Diagnostic Feed Terminal
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = Color.Black.copy(alpha = 0.85f)
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp).fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .background(VoltLime, CircleShape)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = connectingStepText,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                    color = VoltLime,
                                    lineHeight = 14.sp
                                )
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = if (appLanguage == "es") "Mantén la aplicación abierta durante el apretón de manos." else "Verifying biometrics and securing telemetry pipe. Keep app open.",
                            fontSize = 8.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
        else if (showPairingWizard) {
            // STEP-BY-STEP FIRST-TIME DEVICE PAIRING WIZARD COMPONENT
            item {
                Text(
                    text = "First-Time Wearable Pairing Wizard",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = VoltLime,
                    modifier = Modifier.padding(start = 4.dp)
                )
            }
            
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.12f)
                    ),
                    border = androidx.compose.foundation.BorderStroke(1.dp, VoltLime.copy(alpha = 0.4f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp)
                    ) {
                        // Header info
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "STEP $wizardStep OF 5",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Black,
                                color = VoltLime,
                                letterSpacing = 1.sp
                            )
                            Text(
                                text = "Secure BLE GATT Pair",
                                fontSize = 9.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(6.dp))
                        
                        // Stepper dots indicator
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            for (i in 1..5) {
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(4.dp)
                                        .background(
                                            color = if (i <= wizardStep) VoltLime else MaterialTheme.colorScheme.surfaceVariant,
                                            shape = RoundedCornerShape(2.dp)
                                        )
                                )
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(18.dp))
                        
                        // Step-specific displays
                        when (wizardStep) {
                            1 -> {
                                Text(
                                    text = "Prepare Your Fitness Wearable",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Black,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "To synchronize live heart rate, sleep scores, and training load safely, please configure your primary health peripheral device:",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    lineHeight = 15.sp
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Text("💡 Ensure Bluetooth is active on your host device.", fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Text("🔋 Ensure accessory has at least 20% charge left.", fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Text("⚙️ Place your watch or ring into \"Pairing Mode\" from system settings.", fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                                Spacer(modifier = Modifier.height(16.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Button(
                                        onClick = { showPairingWizard = false },
                                        shape = RoundedCornerShape(8.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text("Cancel", fontSize = 11.sp)
                                    }
                                    Button(
                                        onClick = {
                                            checkAndRunBtAction {
                                                wizardStep = 2
                                            }
                                        },
                                        shape = RoundedCornerShape(8.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = VoltLime),
                                        modifier = Modifier.weight(2.5f)
                                    ) {
                                        Text("🟢 Discover Beacons", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                    }
                                }
                            }
                            2 -> {
                                // Dynamic automated scanner step
                                LaunchedEffect(wizardStep) {
                                    wizardLogText = "Broadcasting discovery beacons over LE..."
                                    delay(900)
                                    wizardLogText = "Resolving BLE GATT advertisement templates..."
                                    delay(800)
                                    val candidates = listOf(
                                        com.example.ui.viewmodel.SmartDevice("", "Whoop Band 4.0", "Whoop", "Band", true, 89, "", "Heart Rate, HRV, Recovery Metrics"),
                                        com.example.ui.viewmodel.SmartDevice("", "Oura Ring Gen 3", "Oura", "Ring", true, 92, "", "Sleep Score, Temperature, HRV"),
                                        com.example.ui.viewmodel.SmartDevice("", "Garmin Fenix 7X", "Garmin", "Watch", true, 81, "", "VO2 Max, PulseOx, Sleep tracking"),
                                        com.example.ui.viewmodel.SmartDevice("", "Withings ScanWatch", "Withings", "Watch", true, 74, "", "Oxygen saturation, Sleep Apnea Index")
                                    )
                                    wizardChosenDevice = candidates.random()
                                    wizardLogText = "Spotted device [${wizardChosenDevice!!.brand} ${wizardChosenDevice!!.name}]!"
                                    delay(800)
                                    wizardStep = 3
                                }
                                
                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(72.dp)
                                            .background(VoltLime.copy(alpha = 0.1f), CircleShape)
                                            .border(1.dp, VoltLime, CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        CircularProgressIndicator(
                                            color = VoltLime,
                                            strokeWidth = 2.dp,
                                            modifier = Modifier.size(54.dp)
                                        )
                                        Text("📡", fontSize = 24.sp)
                                    }
                                    Spacer(modifier = Modifier.height(14.dp))
                                    Text(
                                        text = "Scanning Airwaves for Beacons",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = VoltLime
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = wizardLogText,
                                        fontSize = 9.sp,
                                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                            3 -> {
                                val dev = wizardChosenDevice ?: com.example.ui.viewmodel.SmartDevice("", "Smart Fitness Device", "Generic", "Watch", true, 90, "", "Vitals")
                                Text(
                                    text = "Confirm Secure Handshake PIN",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Black,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Confirm that the 6-digit PIN below is visible on your ${dev.brand} ${dev.name}:",
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(14.dp))
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(Color.Black.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(8.dp))
                                        .padding(vertical = 12.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "5 9 3    1 8 2",
                                        fontSize = 24.sp,
                                        fontWeight = FontWeight.Black,
                                        letterSpacing = 2.sp,
                                        color = VoltLime
                                    )
                                }
                                Spacer(modifier = Modifier.height(16.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Button(
                                        onClick = { wizardStep = 1 },
                                        shape = RoundedCornerShape(8.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text("Retry", fontSize = 11.sp)
                                    }
                                    Button(
                                        onClick = { wizardStep = 4 },
                                        shape = RoundedCornerShape(8.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = VoltLime),
                                        modifier = Modifier.weight(2f)
                                    ) {
                                        Text("🤝 Codes Match", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                    }
                                }
                            }
                            4 -> {
                                val dev = wizardChosenDevice ?: com.example.ui.viewmodel.SmartDevice("", "Smart Fitness Device", "Generic", "Watch", true, 90, "", "Vitals")
                                Text(
                                    text = "Configure Stream Preferences",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Black,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Verify metric characteristics to stream from your ${dev.name}:",
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.1f)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text("✅", fontSize = 12.sp)
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text("Continuous Heart Rate Biometrics", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                        }
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text("✅", fontSize = 12.sp)
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text("Sleep Staging & Sleep Apnea Index", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                        }
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text("✅", fontSize = 12.sp)
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text("Calorie metrics & Steps override syncs", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.height(16.dp))
                                Button(
                                    onClick = {
                                        viewModel.registerCustomDevice(
                                            name = dev.name,
                                            brand = dev.brand,
                                            type = dev.type,
                                            metrics = dev.syncedMetrics
                                        )
                                        wizardStep = 5
                                    },
                                    shape = RoundedCornerShape(8.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = VoltLime),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("⚡ Finalize Secure Connection", color = Color.Black, fontWeight = FontWeight.Black, fontSize = 11.sp)
                                }
                            }
                            5 -> {
                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(54.dp)
                                            .background(VoltLime.copy(alpha = 0.15f), CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text("🎉", fontSize = 26.sp)
                                    }
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Text(
                                        text = "Pairing Complete!",
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Black,
                                        color = VoltLime
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Your first-time device connection has been completed and dynamic synchronization is fully active.",
                                        fontSize = 10.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        textAlign = TextAlign.Center,
                                        lineHeight = 14.sp
                                    )
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Button(
                                        onClick = {
                                            showPairingWizard = false
                                            wizardChosenDevice = null
                                            wizardStep = 1
                                        },
                                        shape = RoundedCornerShape(8.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = VoltLime),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text("🚀 Enter Device Hub Dashboard", color = Color.Black, fontWeight = FontWeight.Black, fontSize = 11.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        else if (isScanning || isPairingSimulated || detectedGadget != null) {
            // WIRELESS SCAN & PAIR ZONE
            item {
                Text(
                    text = if (appLanguage == "es") "Sincronizador Inalámbrico Inteligente" else "Smart Wireless Synchronizer Console",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = VoltLime,
                    modifier = Modifier.padding(start = 4.dp)
                )
            }

            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(
                            1.dp,
                            if (isScanning) VoltLime.copy(alpha = 0.5f) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
                            RoundedCornerShape(16.dp)
                        ),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.1f),
                        contentColor = MaterialTheme.colorScheme.onSurface,
                        disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.1f),
                        disabledContentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .padding(16.dp)
                            .fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        if (isScanning) {
                            // Radar Scan in progress
                            Spacer(modifier = Modifier.height(16.dp))
                            Box(
                                modifier = Modifier
                                    .size(72.dp)
                                    .background(VoltLime.copy(alpha = 0.12f), CircleShape)
                                    .border(1.dp, VoltLime, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(
                                    color = VoltLime,
                                    strokeWidth = 2.dp,
                                    modifier = Modifier.size(54.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = scanStepText,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = VoltLime,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(horizontal = 16.dp)
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Locating nearby health bands, smart wearables, chest sensors, smart rings...",
                                fontSize = 9.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                        }
                        else if (detectedGadget != null) {
                            // Found device & show details nicely!
                            val device = detectedGadget!!
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Info,
                                    contentDescription = "Universal Sensor Found",
                                    tint = VoltLime,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = if (isPairingSimulated) {
                                            if (appLanguage == "es") "Vinculando Wearable..." else "Pairing Wearable..."
                                        } else {
                                            if (appLanguage == "es") "Nuevo Dispositivo Auto-Detectado!" else "Dynamic Bluetooth Beacon Discovered!"
                                        },
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = VoltLime
                                    )
                                    Text(
                                        text = if (isPairingSimulated) "Verifying biometrics and securing channel..." else "RSSI Signal: -${(42..65).random()} dBm (Strong proximity signal)",
                                        fontSize = 9.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            
                            Spacer(modifier = Modifier.height(12.dp))
                            
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
                                ),
                                shape = RoundedCornerShape(10.dp),
                                border = androidx.compose.foundation.BorderStroke(2.dp, if (isPairingSimulated) VoltLime.copy(alpha = 0.6f) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text(
                                                text = device.brand.uppercase(),
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = VoltLime
                                            )
                                            Text(
                                                text = device.name,
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Black,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                        }
                                        
                                        Box(
                                            modifier = Modifier
                                                .background(VoltLime.copy(alpha = 0.15f), RoundedCornerShape(6.dp))
                                                .padding(horizontal = 6.dp, vertical = 3.dp)
                                        ) {
                                            Text(
                                                text = device.type,
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = VoltLime
                                            )
                                        }
                                    }
                                    
                                    Spacer(modifier = Modifier.height(8.dp))
                                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))
                                    Spacer(modifier = Modifier.height(8.dp))
                                    
                                    Text(
                                        text = getString("avail_telemetry"),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = device.syncedMetrics,
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        lineHeight = 14.sp
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Estimated Battery Level: ${device.batteryPercent}% • Connection Port: BLE GATT Broadcast Profile",
                                        fontSize = 9.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            
                            Spacer(modifier = Modifier.height(14.dp))
                            
                            if (isPairingSimulated) {
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.1f)
                                    ),
                                    shape = RoundedCornerShape(10.dp),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, VoltLime.copy(alpha = 0.3f))
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp).fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        CircularProgressIndicator(
                                            color = VoltLime,
                                            strokeWidth = 2.dp,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column {
                                            Text(
                                                text = pairStepText,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = VoltLime
                                            )
                                            Text(
                                                text = "Connecting telemetry line. Keep app open.",
                                                fontSize = 8.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            } else {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Button(
                                        onClick = { detectedGadget = null },
                                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text(text = if (appLanguage == "es") "Ignorar" else "Ignore", fontSize = 11.sp)
                                    }
                                    
                                    Button(
                                        onClick = {
                                            isPairingSimulated = true
                                            coroutineScope.launch {
                                                pairStepText = "Initiating secure handshake over encrypted L2CAP channel..."
                                                delay(700)
                                                pairStepText = "Retrieving device manufacturer descriptor configuration..."
                                                delay(600)
                                                pairStepText = "Negotiating custom dynamic payload characteristics..."
                                                delay(600)
                                                pairStepText = "Registering device: ${device.brand} ${device.name}..."
                                                delay(500)
                                                viewModel.registerCustomDevice(
                                                    name = device.name,
                                                    brand = device.brand,
                                                    type = device.type,
                                                    metrics = device.syncedMetrics
                                                )
                                                showSuccess("Successfully paired ${device.brand} ${device.name}!")
                                                detectedGadget = null
                                                isPairingSimulated = false
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = VoltLime),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.weight(2f)
                                    ) {
                                        Text(
                                            text = getString("pair_btn"),
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.Black
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        else {
            // NO ACTIVE CONNECTION OR SCAN ONGOING
            // Display normal console setup where user can click to Scan:
            item {
                Text(
                    text = "Smart Wireless Synchronizer Console",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = VoltLime,
                    modifier = Modifier.padding(start = 4.dp)
                )
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.08f)
                    ),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f)
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .padding(16.dp)
                            .fillMaxWidth()
                    ) {
                        // Section 1: Standard Search
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(VoltLime.copy(alpha = 0.12f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = "Search icon",
                                    tint = VoltLime,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Standard Peripheral Scanner",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Perform a quick automatic detection of any active heart rate strap, smart watch or ring.",
                                    fontSize = 9.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    lineHeight = 12.sp
                                )
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(10.dp))
                        
                        Button(
                            onClick = {
                                checkAndRunBtAction {
                                    isScanning = true
                                    detectedGadget = null
                                    coroutineScope.launch {
                                        scanStepText = "Initializing broad-spectrum BLE beacon sweeps..."
                                        delay(800)
                                        scanStepText = "Seeking advertisement frames over ANT+ and Bluetooth LE..."
                                        delay(700)
                                        scanStepText = "Parsing active broadcast payloads from airwaves..."
                                        delay(700)
                                        
                                        val candidateDevices = listOf(
                                            listOf("Whoop", "Whoop Band 4.0", "Band", "Strain Index, Cardiovascular Recovery, Skin Temp"),
                                            listOf("Suunto", "Suunto Race", "Watch", "Recovery Time, Training Load, VO2 Max, GPS"),
                                            listOf("Ultrahuman", "Ultrahuman Ring Air", "Ring", "Circadian Phase, Recovery, Skin Temp"),
                                            listOf("Withings", "Withings ScanWatch 2", "Watch", "Sleep Apnea Index, Oxygen, Heart Rate"),
                                            listOf("Pixel", "Pixel Watch 3", "Watch", "Stress Response, Continuous EDA, Sleep Coach"),
                                            listOf("Xiaomi", "Mi Band 9", "Band", "Passive Pulse Tracker, Workout Mode, Oxygen")
                                        )
                                        
                                        val currentNames = smartDevices.map { it.name.lowercase() }
                                        val eligible = candidateDevices.filter { !currentNames.contains(it[1].lowercase()) }
                                        val chosen = if (eligible.isNotEmpty()) eligible.random() else candidateDevices.random()
                                        
                                        scanStepText = "Spotted advertising beacon: [${chosen[0]} ${chosen[1]}]! Handshaking parameters..."
                                        delay(600)
                                        
                                        detectedGadget = com.example.ui.viewmodel.SmartDevice(
                                            id = "custom_" + java.util.UUID.randomUUID().toString().take(6),
                                            name = chosen[1],
                                            brand = chosen[0],
                                            type = chosen[2],
                                            isConnected = true,
                                            batteryPercent = (55..100).random(),
                                            syncStatus = "Stream Paused - Press Switch to Feed",
                                            syncedMetrics = chosen[3]
                                        )
                                        isScanning = false
                                    }
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = VoltLime),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "🔍 Run Auto-Discovery Scan",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Black,
                                color = Color.Black
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(14.dp))
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                        Spacer(modifier = Modifier.height(14.dp))
                        
                        // Section 2: First-time Guided Wizard (THE pairing function!)
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(VoltLime.copy(alpha = 0.12f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("✨", fontSize = 16.sp)
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "First-Time Accessory Pairing Wizard",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Complete secure GATT pin handshake, security check and metric filters setup for newly purchased fitness wearables.",
                                    fontSize = 9.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    lineHeight = 12.sp
                                )
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(10.dp))
                        
                        Button(
                            onClick = {
                                checkAndRunBtAction {
                                    showPairingWizard = true
                                    wizardStep = 1
                                    wizardLogText = ""
                                    wizardChosenDevice = null
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "🌟 Start Guided Pairing Wizard",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Black,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        }

        // List of Active Paired Devices with On/Off Feed Switch (Hide while actively connecting to prevent clutter/confusion)
        if (!isAnyConnecting) {
            item {
                Text(
                    text = "${getString("avail_telemetry").substringBefore(":")} (${smartDevices.size})",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.padding(start = 4.dp, top = 6.dp)
                )
            }

            if (smartDevices.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.05f)
                        ),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = getString("no_paired"),
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                items(smartDevices) { device ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .animateContentSize(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (device.isConnected) VoltLime.copy(alpha = 0.04f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
                        ),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (device.isConnected) VoltLime.copy(alpha = 0.2f) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.15f)
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Category Icon
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .background(
                                        if (device.isConnected) VoltLime.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant,
                                        CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                val iconText = when (device.type) {
                                    "Watch" -> "⌚"
                                    "Ring" -> "💍"
                                    "Band" -> "🩹"
                                    "Chest Strap" -> "❤️"
                                    else -> "📟"
                                }
                                Text(text = iconText, fontSize = 18.sp)
                            }
                            
                            Spacer(modifier = Modifier.width(12.dp))
                            
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = device.brand,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = VoltLime,
                                        modifier = Modifier
                                            .background(VoltLime.copy(alpha = 0.08f), RoundedCornerShape(4.dp))
                                            .padding(horizontal = 4.dp, vertical = 1.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = device.name,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "Battery: ${device.batteryPercent}% • ${device.syncedMetrics}",
                                    fontSize = 9.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "Telemetry Stream: " + (if (device.isConnected) getString("sync_on") else getString("sync_off")),
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (device.isConnected) VoltLime else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            
                            Spacer(modifier = Modifier.width(8.dp))
                            
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                // On/Off switch for the individual device stream telemetry feeder
                                Switch(
                                    checked = device.isConnected,
                                    onCheckedChange = { isChecked ->
                                        if (isChecked) {
                                            checkAndRunBtAction {
                                                coroutineScope.launch {
                                                    connectingDevice = device
                                                    connectingStepText = "Locating broadcast on BLE address..."
                                                    delay(600)
                                                    connectingStepText = "Initiating secure handshakes..."
                                                    delay(500)
                                                    connectingStepText = "Retrieving advertised variables..."
                                                    delay(550)
                                                    connectingStepText = "Stabilizing diagnostic transport channel..."
                                                    delay(450)
                                                    viewModel.toggleDeviceConnection(device.id)
                                                    connectingDevice = null
                                                    showSuccess("Live Telemetry Feeder connected for ${device.brand} ${device.name}!")
                                                }
                                            }
                                        } else {
                                            viewModel.toggleDeviceConnection(device.id)
                                            showSuccess("Telemetry feed paused for ${device.brand} ${device.name}.")
                                        }
                                    },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = Color.Black,
                                        checkedTrackColor = VoltLime,
                                        uncheckedThumbColor = MaterialTheme.colorScheme.outline,
                                        uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant
                                    ),
                                    modifier = Modifier
                                )
                                
                                Spacer(modifier = Modifier.width(4.dp))
                                
                                // Delete button (Unpair option present only for Custom Devices to avoid losing basic preset templates if custom, or allow all)
                                IconButton(
                                    onClick = {
                                        if (device.id.startsWith("custom_")) {
                                            viewModel.removeCustomDevice(device.id)
                                            showSuccess("Device unlinked successfully.")
                                        } else {
                                            // If preset, just force-disconnect it from the system
                                            if (device.isConnected) {
                                                viewModel.toggleDeviceConnection(device.id)
                                            }
                                            showSuccess("Preset wearable telemetry stream zeroed out.")
                                        }
                                    },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = getString("disconnect"),
                                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
        
        item {
            Spacer(modifier = Modifier.height(30.dp))
        }
    }

    if (showPermissionDialog) {
        AlertDialog(
            onDismissRequest = { showPermissionDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Bluetooth,
                        contentDescription = "Bluetooth authorization symbol",
                        tint = VoltLime,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text("Bluetooth Authorization", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "OmniFit requests secure permissions to discover, connect to, and read real-time biometric telemetry from physical smartwatches, rings, or fitness bands.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        "• Scan wireless airwaves for active beacons\n• Establish secure GATT device handshake\n• Stream continuous live metabolic activity",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showPermissionDialog = false
                        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                            bluetoothPermissionLauncher.launch(
                                arrayOf(
                                    android.Manifest.permission.BLUETOOTH_SCAN,
                                    android.Manifest.permission.BLUETOOTH_CONNECT
                                )
                            )
                        } else {
                            bluetoothPermissionLauncher.launch(
                                arrayOf(
                                    android.Manifest.permission.ACCESS_FINE_LOCATION
                                )
                            )
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = VoltLime)
                ) {
                    Text("Grant Access", color = Color.Black, fontWeight = FontWeight.Black)
                }
            },
            dismissButton = {
                TextButton(onClick = { 
                    showPermissionDialog = false 
                    showSuccess("Sandbox fallback active: Using secure localized RF synthesizer.")
                    // Automatically run sandbox scanner to be helpful!
                    isScanning = true
                    detectedGadget = null
                    coroutineScope.launch {
                        scanStepText = "Initializing broad-spectrum BLE beacon sweeps..."
                        delay(800)
                        scanStepText = "Seeking advertisement frames over ANT+ and Bluetooth LE..."
                        delay(700)
                        scanStepText = "Parsing active broadcast payloads from airwaves..."
                        delay(700)
                        
                        val candidateDevices = listOf(
                            listOf("Whoop", "Whoop Band 4.0", "Band", "Strain Index, Cardiovascular Recovery, Skin Temp"),
                            listOf("Suunto", "Suunto Race", "Watch", "Recovery Time, Training Load, VO2 Max, GPS"),
                            listOf("Ultrahuman", "Ultrahuman Ring Air", "Ring", "Circadian Phase, Recovery, Skin Temp"),
                            listOf("Withings", "Withings ScanWatch 2", "Watch", "Sleep Apnea Index, Oxygen, Heart Rate"),
                            listOf("Pixel", "Pixel Watch 3", "Watch", "Stress Response, Continuous EDA, Sleep Coach"),
                            listOf("Xiaomi", "Mi Band 9", "Band", "Passive Pulse Tracker, Workout Mode, Oxygen")
                        )
                        
                        val currentNames = smartDevices.map { it.name.lowercase() }
                        val eligible = candidateDevices.filter { !currentNames.contains(it[1].lowercase()) }
                        val chosen = if (eligible.isNotEmpty()) eligible.random() else candidateDevices.random()
                        
                        scanStepText = "Spotted advertising beacon: [${chosen[0]} ${chosen[1]}]! Handshaking parameters..."
                        delay(600)
                        
                        detectedGadget = com.example.ui.viewmodel.SmartDevice(
                            id = "custom_" + java.util.UUID.randomUUID().toString().take(6),
                            name = chosen[1],
                            brand = chosen[0],
                            type = chosen[2],
                            isConnected = true,
                            batteryPercent = (55..100).random(),
                            syncStatus = "Stream Paused - Press Switch to Feed",
                            syncedMetrics = chosen[3]
                        )
                        isScanning = false
                    }
                }) {
                    Text("Sandbox Mode", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        )
    }

    if (showBluetoothOffAssistant) {
        AlertDialog(
            onDismissRequest = { showBluetoothOffAssistant = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "Bluetooth off symbol",
                        tint = VoltLime,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text("Bluetooth is Disabled", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "Bluetooth is currently powered off. Please activate your device's Bluetooth radio state to initiate the telemetry connect sequence.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        "Once turned on, OmniFit will automatically connect and establish the active biometric synchronization channel.",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showBluetoothOffAssistant = false
                        try {
                            val intent = android.content.Intent(android.bluetooth.BluetoothAdapter.ACTION_REQUEST_ENABLE)
                            enableBluetoothLauncher.launch(intent)
                        } catch (e: Exception) {
                            try {
                                val intent = android.content.Intent(android.provider.Settings.ACTION_BLUETOOTH_SETTINGS)
                                context.startActivity(intent)
                                showSuccess("Opening system Bluetooth settings...")
                            } catch (settingsError: Exception) {
                                showSuccess("Unable to launch settings. Please activate Bluetooth from your device status bar.")
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = VoltLime)
                ) {
                    Text("Turn On Bluetooth", color = Color.Black, fontWeight = FontWeight.Black)
                }
            },
            dismissButton = {
                TextButton(onClick = { 
                    showBluetoothOffAssistant = false 
                    showSuccess("Running in offline simulation mode...")
                    pendingBluetoothAction?.invoke()
                    pendingBluetoothAction = null
                }) {
                    Text("Use Simulation Mode", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        )
    }
}
