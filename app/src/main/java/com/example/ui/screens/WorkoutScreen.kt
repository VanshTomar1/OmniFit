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
import com.example.data.model.Exercise
import com.example.data.model.WorkoutSchedule
import com.example.ui.viewmodel.FitnessViewModel

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
    val isBeginnerPeriod = if (p != null && p.experienceLevel.equals("Beginner", ignoreCase = true)) {
        val daysPassed = (System.currentTimeMillis() - p.onboardingTimestamp) / (24 * 60 * 60 * 1000)
        daysPassed < 30
    } else {
        false
    }

    // Currently selected daily schedule
    val todaySchedule = schedules.find { it.dayOfWeek == selectedDay }

    var exerciseToSwap by remember { mutableStateOf<String?>(null) } // exercise ID
    var alternativesList by remember { mutableStateOf<List<Exercise>>(emptyList()) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        // 7-day selector row with circular pills
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            val days = listOf("M", "T", "W", "T", "F", "S", "S")
            days.forEachIndexed { index, name ->
                val dayNum = index + 1
                val isSelected = selectedDay == dayNum
                Box(
                    modifier = Modifier
                        .size(40.dp)
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
                        fontSize = 14.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        if (todaySchedule == null || todaySchedule.isRestDay || todaySchedule.exerciseIds.isEmpty()) {
            // Rest Day display
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp))
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.SelfImprovement,
                        contentDescription = "Meditation REST",
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(72.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Active Recovery & Stretching Day",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Lifting heavy triggers microtears. True muscle growth takes place when resting and synthesis spikes. Do some dynamic posture alignments or water hydration checks!",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 12.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Suggested Stretch: Hollow body holds, deep squat posture alignments, lateral arm swings, chest foam rollings.",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
            }
        } else {
            // Workout Schedule
            Text(
                text = todaySchedule.workoutName,
                fontSize = 22.sp,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 4.dp)
            )

            if (isBeginnerPeriod) {
                Text(
                    text = "🔰 Enforcing Beginner Exercises Only (Safe Induction Active)",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.tertiary,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 6.dp)
                )
            }

            if (profile?.gender.equals("Female", ignoreCase = true) && isOnPeriod) {
                Text(
                    text = "🌸 Gentle Period Mode Active (Comfort-First Exercises Selected)",
                    fontSize = 11.sp,
                    color = Color(0xFFE53935),
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 6.dp)
                )
            }

            val exerciseIds = todaySchedule.exerciseIds.split(",").filter { it.isNotEmpty() }
            val exercisesToday = exerciseIds.mapNotNull { id ->
                val rawEx = allExercises.find { it.id == id }
                if (rawEx != null) viewModel.getPeriodSafeExercise(rawEx) else null
            }

            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(vertical = 10.dp)
            ) {
                items(exercisesToday) { ex ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp))
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(ex.name, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    SuggestionChip(
                                        onClick = {},
                                        label = { Text(ex.targetMuscleGroup, fontSize = 10.sp, fontWeight = FontWeight.Bold) }
                                    )
                                    SuggestionChip(
                                        onClick = {},
                                        label = { Text(ex.requiredEquipment, fontSize = 10.sp) }
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
                                                fontSize = 10.sp,
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
                                    // Filter alternative exercises targeting correct muscle group
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
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Start active workout button
            Button(
                onClick = { onStartWorkout(todaySchedule) },
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .padding(bottom = 8.dp)
            ) {
                Icon(imageVector = Icons.Default.PlayArrow, contentDescription = "Start Play icon")
                Spacer(modifier = Modifier.width(8.dp))
                Text("Start Active Workout Session", fontSize = 15.sp, fontWeight = FontWeight.Bold)
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

