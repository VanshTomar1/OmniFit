package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.ui.text.style.TextAlign
import com.example.data.model.Exercise
import com.example.data.model.WorkoutSchedule
import com.example.ui.viewmodel.FitnessViewModel
import com.example.ui.theme.VoltLime

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun WorkoutScreen(
    viewModel: FitnessViewModel,
    onStartWorkout: (WorkoutSchedule) -> Unit,
    modifier: Modifier = Modifier
) {
    val schedules by viewModel.schedules.collectAsState(initial = emptyList())
    val selectedDay by viewModel.currentDaySelected.collectAsState()
    val allExercises by viewModel.allExercises.collectAsState(initial = emptyList())
    val profile by viewModel.userProfile.collectAsState(initial = null)
    val isOnPeriod by viewModel.isOnPeriod.collectAsState()

    val p = profile
    val isBeginnerPeriod = p != null && p.experienceLevel.equals("Beginner", ignoreCase = true)

    // Currently selected daily schedule
    val todaySchedule = schedules.find { it.dayOfWeek == selectedDay }

    var exerciseToSwap by remember { mutableStateOf<String?>(null) } // exercise ID
    var alternativesList by remember { mutableStateOf<List<Exercise>>(emptyList()) }

    val allSessions by viewModel.allSessions.collectAsState(initial = emptyList())

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 1. Day Selector Row
        item {
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val days = listOf("M", "T", "W", "T", "F", "S", "S")
                days.forEachIndexed { index, name ->
                    val dayNum = index + 1
                    val isSelected = selectedDay == dayNum
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(
                                if (isSelected) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.surfaceVariant
                            )
                            .clickable { viewModel.selectDay(dayNum) },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = name,
                            fontWeight = FontWeight.Bold,
                            color = if (isSelected) MaterialTheme.colorScheme.onPrimary
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 13.sp
                        )
                    }
                }
            }
        }

        // 2. Scheduled content branch
        if (todaySchedule == null || todaySchedule.isRestDay || todaySchedule.exerciseIds.isEmpty()) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(2.dp))
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.SelfImprovement,
                            contentDescription = "Meditation REST",
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Active Recovery Stretch Day",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Lifting heavy triggers microtears. True muscle growth takes place when resting and synthesis spikes. Do some dynamic posture alignments or water hydration checks!",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 8.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Suggested Stretch: Hollow body holds, deep squat posture alignments, lateral arm swings, chest foam rollings.",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.secondary,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            // --- TRAIN INSTEAD BLOCK FOR MAXIMUM CORE FLEXIBILITY ---
            item {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "🔥 Override Schedule & Train Anyway",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Select any of your personalized active training sessions scheduled for other days of the week to initiate coaching mechanics immediately.",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            val activeScheds = schedules.filter { !it.isRestDay && it.exerciseIds.isNotEmpty() }
            if (activeScheds.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f))
                    ) {
                        Text(
                            text = "No schedules generated yet. Tweak the Smart Tuner on the Dashboard to instantly optimize your training splits!",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(16.dp),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                items(activeScheds) { sched ->
                    val dayName = when (sched.dayOfWeek) {
                        1 -> "Monday"
                        2 -> "Tuesday"
                        3 -> "Wednesday"
                        4 -> "Thursday"
                        5 -> "Friday"
                        6 -> "Saturday"
                        7 -> "Sunday"
                        else -> "Training Day"
                    }
                    val exIds = sched.exerciseIds.split(",").filter { it.isNotEmpty() }
                    val exercisesForSched = exIds.mapNotNull { id -> allExercises.find { it.id == id } }

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp))
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(text = dayName, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = VoltLime)
                                    Text(text = sched.workoutName, fontSize = 14.sp, fontWeight = FontWeight.Black)
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Button(
                                    onClick = { onStartWorkout(sched) },
                                    colors = ButtonDefaults.buttonColors(containerColor = VoltLime, contentColor = Color.Black),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text("Start Work", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Exercises (${exercisesForSched.size}): " + exercisesForSched.joinToString { it.name },
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        } else {
            item {
                Column(modifier = Modifier.fillMaxWidth().padding(top = 4.dp)) {
                    Text(
                        text = todaySchedule.workoutName,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.primary
                    )

                    if (isBeginnerPeriod) {
                        Text(
                            text = "🔰 Beginner Exercises Active (Safe Form Priority)",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.tertiary,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }

                    if (profile?.gender.equals("Female", ignoreCase = true) && isOnPeriod) {
                        Text(
                            text = "🌸 Gentle Period Mode Active (Comfort-First Exercises Selected)",
                            fontSize = 11.sp,
                            color = Color(0xFFE53935),
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            }

            val exerciseIds = todaySchedule.exerciseIds.split(",").filter { it.isNotEmpty() }
            val exercisesToday = exerciseIds.mapNotNull { id ->
                val rawEx = allExercises.find { it.id == id }
                if (rawEx != null) viewModel.getPeriodSafeExercise(rawEx) else null
            }

            items(exercisesToday) { ex ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp))
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(ex.name, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                SuggestionChip(
                                    onClick = {},
                                    label = { Text(ex.targetMuscleGroup, fontSize = 9.sp, fontWeight = FontWeight.Bold) }
                                )
                                SuggestionChip(
                                    onClick = {},
                                    label = { Text(ex.requiredEquipment, fontSize = 9.sp) }
                                )
                                val reqSetsText = if (isBeginnerPeriod || ex.difficultyLevel.equals("Beginner", ignoreCase = true)) {
                                    "Min 3 Sets"
                                } else {
                                    "Min 4 Sets"
                                }
                                SuggestionChip(
                                    onClick = {},
                                    label = {
                                        Text(
                                            text = reqSetsText,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                )
                            }
                        }

                        // Swap action trigger
                        IconButton(
                            onClick = {
                                exerciseToSwap = ex.id
                                alternativesList = allExercises.filter { other ->
                                    other.targetMuscleGroup == ex.targetMuscleGroup && other.id != ex.id
                                }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.SwapHoriz,
                                contentDescription = "Swap Exercise alternatives",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(4.dp))
                // Start active workout button
                Button(
                    onClick = { onStartWorkout(todaySchedule) },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                ) {
                    Icon(imageVector = Icons.Default.PlayArrow, contentDescription = "Start Play icon")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Start Active Workout Session", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        // 3. Divider and Real-time saved history list
        item {
            Spacer(modifier = Modifier.height(8.dp))
            Divider(
                modifier = Modifier.fillMaxWidth(),
                thickness = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
            )
            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "📜 Personal Training Logging History",
                fontSize = 16.sp,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Behold your raw volume progression metrics, exact set records, reps completed, and custom rests retrieved from persistence.",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        if (allSessions.isEmpty()) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                ) {
                    Box(
                        modifier = Modifier.padding(18.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No real-time training sessions completed yet.\nRecord and complete your first session above to kick off logging!",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        } else {
            items(allSessions) { session ->
                HistoricSessionCard(session = session, viewModel = viewModel)
            }
        }
    }

    // Swapping dialog modal
    if (exerciseToSwap != null) {
        val oldExId = exerciseToSwap!!
        AlertDialog(
            onDismissRequest = { exerciseToSwap = null },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { exerciseToSwap = null }) {
                    Text("Cancel")
                }
            },
            title = { Text("Swap with Muscle-Group Alternative") },
            text = {
                if (alternativesList.isEmpty()) {
                    Text("No alternatives available for this muscle group right now!")
                } else {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(alternativesList) { otherEx ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        // Trigger action swap in backend repository
                                        viewModel.swapExercise(oldExId, otherEx)
                                        exerciseToSwap = null
                                    },
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(otherEx.name, fontWeight = FontWeight.Bold)
                                        Text("Level: ${otherEx.difficultyLevel} ${otherEx.requiredEquipment}", fontSize = 11.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        )
    }
}

@Composable
fun PeriodSlidingButton(
    isOnPeriod: Boolean,
    onValueChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    var dragOffsetX by remember { mutableStateOf(0f) }
    val trackWidthDp = 280.dp
    val buttonSizeDp = 48.dp
    val density = androidx.compose.ui.platform.LocalDensity.current
    val maxDragPx = with(density) { (trackWidthDp - buttonSizeDp - 8.dp).toPx() }

    // Synchronize initial offset if already true
    LaunchedEffect(isOnPeriod, maxDragPx) {
        dragOffsetX = if (isOnPeriod) maxDragPx else 0f
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = if (isOnPeriod) "🛑 Period Mode Active (Gentle Workouts)" else "🌸 Off Period (Tap or Slide right to activate)",
            color = if (isOnPeriod) Color(0xFFE53935) else MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Box(
            modifier = Modifier
                .width(trackWidthDp)
                .height(56.dp)
                .clip(RoundedCornerShape(28.dp))
                .background(
                    if (isOnPeriod) Color(0xFFE53935).copy(alpha = 0.15f)
                    else Color.Gray.copy(alpha = 0.12f)
                )
                .border(
                    1.5.dp, 
                    if (isOnPeriod) Color(0xFFE53935) else Color.Gray.copy(alpha = 0.3f), 
                    RoundedCornerShape(28.dp)
                )
                .padding(4.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            // Background label
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (isOnPeriod) "Slide Left to Turn Off" else "Slide Right: On Period",
                    color = (if (isOnPeriod) Color(0xFFE53935) else MaterialTheme.colorScheme.onSurfaceVariant).copy(alpha = 0.8f),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Black
                )
            }

            // Draggable Thumb Button
            val buttonOffsetDp = with(density) { dragOffsetX.toDp() }
            Box(
                modifier = Modifier
                    .offset(x = buttonOffsetDp)
                    .size(buttonSizeDp)
                    .clip(CircleShape)
                    .background(Color(0xFFE53935))
                    .clickable {
                        // Support tapping as a graceful fallback/accessibility options
                        val target = !isOnPeriod
                        onValueChange(target)
                    }
                    .pointerInput(isOnPeriod, maxDragPx) {
                        detectHorizontalDragGestures(
                            onDragEnd = {
                                if (dragOffsetX > maxDragPx * 0.5f) {
                                    dragOffsetX = maxDragPx
                                    onValueChange(true)
                                } else {
                                    dragOffsetX = 0f
                                    onValueChange(false)
                                }
                            },
                            onHorizontalDrag = { change, dragAmount ->
                                change.consume()
                                dragOffsetX = (dragOffsetX + dragAmount).coerceIn(0f, maxDragPx)
                            }
                        )
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isOnPeriod) Icons.Default.Check else Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = "Slide confirm control",
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

@Composable
fun HistoricSessionCard(
    session: com.example.data.model.WorkoutSession,
    viewModel: com.example.ui.viewmodel.FitnessViewModel
) {
    val logs by viewModel.getLogsForSession(session.id).collectAsState(initial = emptyList())
    var isExpanded by remember { mutableStateOf(false) }

    val sdf = remember { java.text.SimpleDateFormat("MMM dd, yyyy • hh:mm a", java.util.Locale.getDefault()) }
    val formattedDate = sdf.format(java.util.Date(session.dateTimestamp))

    val durationMinutes = session.durationSeconds / 60
    val durationSeconds = session.durationSeconds % 60
    val durationStr = if (durationMinutes > 0) "${durationMinutes}m ${durationSeconds}s" else "${durationSeconds}s"

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable { isExpanded = !isExpanded },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        border = androidx.compose.foundation.BorderStroke(
            1.dp, 
            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
        )
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = session.workoutName,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = formattedDate,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                    )
                }

                Icon(
                    imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = "Expand logs icon",
                    tint = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Timer, 
                        contentDescription = "Duration", 
                        modifier = Modifier.size(14.dp), 
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(durationStr, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Whatshot, 
                        contentDescription = "Calories", 
                        modifier = Modifier.size(14.dp), 
                        tint = Color(0xFFFF5722)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("${String.format(java.util.Locale.US, "%.1f", session.caloriesBurned)} kcal", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }

            if (isExpanded) {
                Spacer(modifier = Modifier.height(12.dp))
                Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                Spacer(modifier = Modifier.height(10.dp))

                if (logs.isEmpty()) {
                    Text("No exercises captured in this session.", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    val grouped = logs.groupBy { log -> log.exerciseId to log.exerciseName }
                    for (entry in grouped) {
                        val key = entry.key
                        val setList = entry.value
                        val exerciseName = key.second
                        Column(modifier = Modifier.padding(vertical = 4.dp)) {
                            Text(
                                text = exerciseName,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            for (log in setList) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(start = 8.dp, top = 2.dp, end = 0.dp, bottom = 2.dp),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Set ${log.setNumber}:",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        modifier = Modifier.width(45.dp),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    val weightStr = if (log.weightKg > 0.0) "${log.weightKg} kg" else "Bodyweight"
                                    Text(
                                        text = "$weightStr  ×  ${log.repsCompleted} reps",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Black,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    if (log.estimated1RM > 0.0) {
                                        Spacer(modifier = Modifier.weight(1f))
                                        Text(
                                            text = "Est. 1RM: ${String.format(java.util.Locale.US, "%.1f", log.estimated1RM)} kg",
                                            fontSize = 10.sp,
                                            color = com.example.ui.theme.VoltLime,
                                            fontWeight = FontWeight.Bold
                                        )
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

