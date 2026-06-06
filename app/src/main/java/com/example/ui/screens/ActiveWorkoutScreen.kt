package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DirectionsRun
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.viewmodel.FitnessViewModel

@Composable
fun ActiveWorkoutScreen(
    viewModel: FitnessViewModel,
    onFinished: () -> Unit
) {
    val activeWorkoutExs by viewModel.activeWorkoutExercises.collectAsState()
    val logsMap by viewModel.exerciseLogsInProgress.collectAsState()
    val workoutName by viewModel.currentActiveWorkoutName.collectAsState()
    val timerSeconds by viewModel.restTimerSeconds.collectAsState()
    val profile by viewModel.userProfile.collectAsState(initial = null)

    val p = profile
    val isBeginnerPeriod = if (p != null && p.experienceLevel.equals("Beginner", ignoreCase = true)) {
        val daysPassed = (System.currentTimeMillis() - p.onboardingTimestamp) / (24 * 60 * 60 * 1000)
        daysPassed < 30
    } else {
        false
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .padding(16.dp)
    ) {
        // Workout Session Title & Finish Button Control Row
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.DirectionsRun,
                        contentDescription = "Run",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Active Tracking Mode",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Text(
                    text = workoutName ?: "Live Workout",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Black
                )
            }

            Button(
                onClick = {
                    viewModel.finishCurrentWorkout()
                    onFinished()
                },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("Complete", color = MaterialTheme.colorScheme.onTertiary, fontWeight = FontWeight.Bold)
            }
        }

        // Rest Timer horizontal ticker
        if (timerSeconds > 0) {
            Card(
                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                shape = RoundedCornerShape(10.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.Timer, contentDescription = "Hold timer", tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Resting: ${timerSeconds}s remaining",
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            fontSize = 14.sp
                        )
                    }

                    TextButton(onClick = { viewModel.stopTimer() }) {
                        Text("Skip Rest", color = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }

        // List of Active exercises and set boxes
        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            items(activeWorkoutExs) { ex ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = ex.name,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Target: ${ex.targetMuscleGroup} | ${ex.requiredEquipment}",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        val isBodyweight = ex.requiredEquipment.equals("Bodyweight", ignoreCase = true)

                        val recReps = when {
                            isBeginnerPeriod -> "12 - 15 Reps"
                            ex.difficultyLevel.equals("Beginner", ignoreCase = true) -> "12 - 15 Reps"
                            ex.difficultyLevel.equals("Intermediate", ignoreCase = true) -> "8 - 12 Reps"
                            else -> "5 - 8 Reps"
                        }

                        val recSets = when {
                            isBeginnerPeriod -> "At least 3 Sets"
                            ex.difficultyLevel.equals("Beginner", ignoreCase = true) -> "At least 3 Sets"
                            else -> "At least 4 Sets"
                        }

                        val recRepsNum = when {
                            isBeginnerPeriod -> 12
                            ex.difficultyLevel.equals("Beginner", ignoreCase = true) -> 12
                            ex.difficultyLevel.equals("Intermediate", ignoreCase = true) -> 10
                            else -> 8
                        }

                        val repReason = when {
                            isBeginnerPeriod -> "Higher rep ranges (12-15) are crucial for beginners to condition tendons, build bone density, and imprint pristine neuromuscular movement patterns safely before handling heavier loads."
                            isBodyweight -> "Bodyweight movements with 10-15 reps foster joint longevity and muscular endurance. Note: Speed/Rhythm control is prioritized as you grow stronger!"
                            else -> "Targeting $recReps recruits motor units matching your training goal (${ex.difficultyLevel} level) to optimize adaptation."
                        }

                        // Displaying Recommended Reps and Rep Explanation card inside ex column
                        Card(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.05f))
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text(
                                    text = "🎯 Recommended: $recSets | $recReps",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Black,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = repReason,
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    lineHeight = 15.sp
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Headings
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Set", modifier = Modifier.width(36.dp), fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                            Text(if (isBodyweight) "Option" else "Weight (Kg)", modifier = Modifier.weight(1f), fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                            Text("Reps", modifier = Modifier.weight(1f), fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                            Spacer(modifier = Modifier.width(48.dp)) // Padding for checked state
                        }

                        // Set Entries
                        val setLogs = logsMap[ex.id] ?: emptyList()
                        setLogs.forEachIndexed { idx, log ->
                            var weightText by remember { mutableStateOf(if (log.weightKg > 0.0) log.weightKg.toString() else "") }
                            var repsText by remember { mutableStateOf(if (log.repsCompleted > 0) log.repsCompleted.toString() else "") }

                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Set Badge
                                Card(
                                    modifier = Modifier.size(36.dp),
                                    shape = RoundedCornerShape(6.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                                ) {
                                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                        Text("${log.setNumber}", fontWeight = FontWeight.Black, fontSize = 12.sp)
                                    }
                                }

                                if (!isBodyweight) {
                                    // Weight input
                                    OutlinedTextField(
                                        value = weightText,
                                        onValueChange = {
                                            weightText = it
                                            val w = it.toDoubleOrNull() ?: 0.0
                                            val r = repsText.toIntOrNull() ?: 0
                                            viewModel.updateSetValues(ex.id, idx, w, r)
                                        },
                                        modifier = Modifier.weight(1f).height(48.dp),
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        singleLine = true,
                                        textStyle = LocalTextStyle.current.copy(fontSize = 13.sp)
                                    )
                                } else {
                                    Card(
                                        modifier = Modifier.weight(1f).height(48.dp),
                                        shape = RoundedCornerShape(8.dp),
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                    ) {
                                        Box(
                                            modifier = Modifier.fillMaxSize(),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text("Bodyweight", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                    }
                                }

                                // Reps input
                                OutlinedTextField(
                                    value = repsText,
                                    onValueChange = {
                                        repsText = it
                                        val w = weightText.toDoubleOrNull() ?: 0.0
                                        val r = it.toIntOrNull() ?: 0
                                        viewModel.updateSetValues(ex.id, idx, w, r)
                                    },
                                    modifier = Modifier.weight(1f).height(48.dp),
                                    placeholder = { Text("Rec: $recRepsNum", fontSize = 10.sp) },
                                    trailingIcon = {
                                        if (repsText.isEmpty()) {
                                            IconButton(
                                                onClick = {
                                                    repsText = recRepsNum.toString()
                                                    val w = weightText.toDoubleOrNull() ?: 0.0
                                                    viewModel.updateSetValues(ex.id, idx, w, recRepsNum)
                                                },
                                                modifier = Modifier.size(24.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Bolt,
                                                    contentDescription = "Auto-fill recommended reps",
                                                    tint = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.size(14.dp)
                                                )
                                            }
                                        }
                                    },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    singleLine = true,
                                    textStyle = LocalTextStyle.current.copy(fontSize = 13.sp)
                                )

                                // Complete set log trigger action
                                IconButton(
                                    onClick = {
                                        viewModel.logFinishedSet()
                                    },
                                    modifier = Modifier.size(40.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = "Log set complete",
                                        tint = if (log.repsCompleted > 0) MaterialTheme.colorScheme.primary else Color.Gray.copy(alpha = 0.5f)
                                    )
                                }
                            }
                        }

                        // Add Set Button
                        TextButton(
                            onClick = { viewModel.addSetToExercise(ex.id) },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(imageVector = Icons.Default.Add, contentDescription = "Add Set")
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Add Performance Set", fontSize = 13.sp)
                        }
                    }
                }
            }
        }
    }
}
