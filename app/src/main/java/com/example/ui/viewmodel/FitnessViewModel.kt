package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.OmniFitApplication
import com.example.data.api.GeminiContent
import com.example.data.api.GeminiPart
import com.example.data.api.GeminiRequest
import com.example.data.api.GeminiRetrofitClient
import com.example.data.model.Exercise
import com.example.data.model.ExerciseLog
import com.example.data.model.HealthTrackerLog
import com.example.data.model.UserProfile
import com.example.data.model.WorkoutSchedule
import com.example.data.model.WorkoutSession
import com.example.data.repository.FitnessRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class FitnessViewModel(
    application: Application,
    private val repository: FitnessRepository
) : AndroidViewModel(application) {

    // --- Flows from Room DB ---
    val userProfile = repository.userProfile
    val schedules = repository.schedules
    val allExercises = repository.allExercises
    val allSessions = repository.allSessions
    val weeklyHealthLogs = repository.weeklyHealthLogs

    // --- UI/UX States ---
    private val _uiState = MutableStateFlow<FitnessUiState>(FitnessUiState.Success)
    val uiState: StateFlow<FitnessUiState> = _uiState.asStateFlow()

    private val _isProfileLoaded = MutableStateFlow(false)
    val isProfileLoaded = _isProfileLoaded.asStateFlow()

    // Period Tracking for Female Version
    private val prefs = getApplication<Application>().getSharedPreferences("com.aistudio.omnifit.PREFS", android.content.Context.MODE_PRIVATE)
    private val _isOnPeriod = MutableStateFlow(prefs.getBoolean("is_on_period", false))
    val isOnPeriod: StateFlow<Boolean> = _isOnPeriod.asStateFlow()

    fun setIsOnPeriod(active: Boolean) {
        _isOnPeriod.value = active
        prefs.edit().putBoolean("is_on_period", active).apply()
    }

    fun getPeriodSafeExercise(original: Exercise): Exercise {
        if (!_isOnPeriod.value) return original
        
        // If it's already a safe beginner/stretch exercise, keep it
        if (original.id in listOf("ex_glute_bridge", "ex_fire_hydrants", "ex_arm_circles", "ex_lateral_raise", "ex_doorframe_rows", "ex_floor_pulls", "ex_calf_raises")) {
            return original
        }
        
        // Map heavy/abdominal-pressure/pelvic-heavy movements to period-safe variations:
        return when (original.targetMuscleGroup) {
            "Quads" -> Exercise("ex_air_squat", "Gentle Air Squat (Period-Safe)", "Quads", "Beginner", "Bodyweight", instructions = "Keep it extremely light. Focus on comfortable breathing and joint mobility.", genderTarget = "Female")
            "Hamstrings" -> Exercise("ex_glute_bridge", "Gentle Single-Leg Glute Bridge (Period-Safe)", "Hamstrings", "Beginner", "Bodyweight", instructions = "Lying flat helps alleviate lower back/pelvic pressure. Raise slowly.", genderTarget = "Female")
            "Chest" -> Exercise("ex_knee_pushup", "Light Knee Push-up (Period-Safe)", "Chest", "Beginner", "Bodyweight", instructions = "Reduces vertical chest press stress. Maintain a gentle, non-straining pose.", genderTarget = "Female")
            "Back" -> Exercise("ex_doorframe_rows", "Light Doorframe Bodyweight Row (Period-Safe)", "Back", "Beginner", "Bodyweight", instructions = "Pull gently. Enhances upper thoracic posture and relieves stress.", genderTarget = "Female")
            "Shoulders" -> Exercise("ex_arm_circles", "Tension Arm Circles (Period-Safe)", "Shoulders", "Beginner", "Bodyweight", instructions = "No extra weights. Ideal to keep shoulder joints warm and active.", genderTarget = "Female")
            "Core" -> Exercise("ex_fire_hydrants", "Gentle Fire Hydrants (Period-Safe)", "Core", "Beginner", "Bodyweight", instructions = "Promotes open hips, relieves lower pelvic tension and cramps.", genderTarget = "Female")
            "Arms" -> Exercise("ex_doorframe_bicep_curls", "Gentle Doorframe Bicep Curl (Period-Safe)", "Arms", "Beginner", "Bodyweight", instructions = "Gentle pulling on doorframe. Easy control on muscles.", genderTarget = "Female")
            else -> original.copy(
                name = "${original.name} (Low-Intensity Safe)",
                instructions = "Adjusted for gentle period-safe execution. Avoid strain."
            )
        }
    }

    // Active Onboarding stats (in-memory during form fill)
    val onboardingWeight = MutableStateFlow("75.0")
    val onboardingHeight = MutableStateFlow("175.0")
    val onboardingAge = MutableStateFlow("28")
    val onboardingBodyFat = MutableStateFlow("15.0")
    val onboardingChest = MutableStateFlow("100.0")
    val onboardingArms = MutableStateFlow("35.0")
    val onboardingWaist = MutableStateFlow("82.0")
    val onboardingThighs = MutableStateFlow("55.0")
    val onboardingPrimaryGoal = MutableStateFlow("Hypertrophy")
    val onboardingSecondaryGoal = MutableStateFlow("Fat Loss")
    val onboardingEquipment = MutableStateFlow("Full Gym")
    val onboardingDays = MutableStateFlow(4)
    val onboardingTimeMax = MutableStateFlow(45)
    val onboardingExperienceLevel = MutableStateFlow("Beginner")
    val onboardingGender = MutableStateFlow("Male") // Male, Female

    // Current daily workout screen state
    private val _currentDaySelected = MutableStateFlow(1) // Monday = 1
    val currentDaySelected = _currentDaySelected.asStateFlow()

    // Live active workout state
    private val _activeSessionId = MutableStateFlow<Long?>(null)
    val activeSessionId = _activeSessionId.asStateFlow()

    private val _currentActiveWorkoutName = MutableStateFlow<String?>(null)
    val currentActiveWorkoutName = _currentActiveWorkoutName.asStateFlow()

    private val _activeWorkoutExercises = MutableStateFlow<List<Exercise>>(emptyList())
    val activeWorkoutExercises = _activeWorkoutExercises.asStateFlow()

    private val _exerciseLogsInProgress = MutableStateFlow<Map<String, List<ExerciseLog>>>(emptyMap()) // Keyed by exerciseId
    val exerciseLogsInProgress = _exerciseLogsInProgress.asStateFlow()

    // Timer state
    private val _restTimerSeconds = MutableStateFlow(0)
    val restTimerSeconds = _restTimerSeconds.asStateFlow()
    private var timerJob: Job? = null

    // Gemini API coach thread
    private val _chatHistory = MutableStateFlow<List<ChatMessage>>(listOf(
        ChatMessage("assistant", "Greetings gym warrior! I am OmniFit Advisor. Tell me about your fitness goals or log some statistics so we can dial in your custom macro goals!")
    ))
    val chatHistory = _chatHistory.asStateFlow()

    private val _isChatLoading = MutableStateFlow(false)
    val isChatLoading = _isChatLoading.asStateFlow()

    // Dynamic stats trackers
    private val _todayHealthLog = MutableStateFlow<HealthTrackerLog>(HealthTrackerLog(repository.getTodayDateKey()))
    val todayHealthLog = _todayHealthLog.asStateFlow()

    // Fatigue status
    private val _fatigueMap = MutableStateFlow<Map<String, Float>>(emptyMap())
    val fatigueMap = _fatigueMap.asStateFlow()

    // Plateau Breaker status
    private val _detectedPlateaus = MutableStateFlow<List<DetectedPlateau>>(emptyList())
    val detectedPlateaus = _detectedPlateaus.asStateFlow()

    // --- Active Running Mode State ---
    private val _isRunActive = MutableStateFlow(false)
    val isRunActive = _isRunActive.asStateFlow()

    private val _runSeconds = MutableStateFlow(0)
    val runSeconds = _runSeconds.asStateFlow()

    private val _runSteps = MutableStateFlow(0)
    val runSteps = _runSteps.asStateFlow()

    private val _runDistanceMeters = MutableStateFlow(0.0)
    val runDistanceMeters = _runDistanceMeters.asStateFlow()

    private val _runCurrentSpeed = MutableStateFlow(0.0) // m/s
    val runCurrentSpeed = _runCurrentSpeed.asStateFlow()

    private val _runLatitude = MutableStateFlow<Double?>(null)
    val runLatitude = _runLatitude.asStateFlow()

    private val _runLongitude = MutableStateFlow<Double?>(null)
    val runLongitude = _runLongitude.asStateFlow()

    private var runTimerJob: Job? = null
    private var lastLocation: Location? = null
    private var locationListener: LocationListener? = null

    init {
        viewModelScope.launch {
            repository.userProfile.collect {
                _isProfileLoaded.value = true
            }
        }
        viewModelScope.launch {
            // Monitor today's health logs
            repository.getHealthLogForDate(repository.getTodayDateKey()).collect { log ->
                if (log != null) {
                    _todayHealthLog.value = log
                }
            }
        }
        viewModelScope.launch {
            // Recalculate fatigue based on exercise sessions on load
            calculateMuscleFatigue()
            // Analyze strength plateaus on load
            analyzePlateaus()
        }
    }

    fun loadExistingProfileToOnboarding(profile: UserProfile) {
        onboardingWeight.value = profile.weightKg.toString()
        onboardingHeight.value = profile.heightCm.toString()
        onboardingAge.value = profile.age.toString()
        onboardingBodyFat.value = profile.bodyFatPercentage.toString()
        onboardingChest.value = profile.chestCm.toString()
        onboardingArms.value = profile.armsCm.toString()
        onboardingWaist.value = profile.waistCm.toString()
        onboardingThighs.value = profile.thighsCm.toString()
        onboardingPrimaryGoal.value = profile.primaryGoal
        onboardingSecondaryGoal.value = profile.secondaryGoal
        onboardingEquipment.value = profile.equipmentInventory
        onboardingDays.value = profile.availableDaysPerWeek
        onboardingTimeMax.value = profile.maxTimeMinutes
        onboardingExperienceLevel.value = profile.experienceLevel
        onboardingGender.value = profile.gender
    }

    fun addManualMuscleLoad(muscle: String) {
        val current = _fatigueMap.value.toMutableMap()
        val currentVal = current[muscle] ?: 0.1f
        current[muscle] = (currentVal + 0.25f).coerceAtMost(1.0f)
        _fatigueMap.value = current
    }

    fun clearMuscleLoad(muscle: String) {
        val current = _fatigueMap.value.toMutableMap()
        current[muscle] = 0.1f
        _fatigueMap.value = current
    }

    fun analyzePlateaus() {
        viewModelScope.launch {
            val allExs = repository.allExercises.first()
            val plateaus = mutableListOf<DetectedPlateau>()

            for (ex in allExs) {
                // Fetch historical log entries for this exercise
                val logs = repository.getLogsForExercise(ex.id).first()
                val logsBySession = logs.groupBy { it.sessionId }
                if (logsBySession.size >= 2) {
                    // Extract maximum weights lifted in chronological session order
                    val maxWeightsBySession = logsBySession.entries
                        .sortedBy { it.key }
                        .map { entry ->
                            entry.value.maxOfOrNull { it.weightKg } ?: 0.0
                        }

                    if (maxWeightsBySession.size >= 2) {
                        val lastWeight = maxWeightsBySession[maxWeightsBySession.size - 1]
                        val prevWeight = maxWeightsBySession[maxWeightsBySession.size - 2]
                        
                        // Plateau is true if weight has hit a ceiling or declined
                        if (lastWeight > 0.0 && lastWeight <= prevWeight) {
                            val altId = ex.alternativeIds.split(",").firstOrNull { it.isNotBlank() } ?: ""
                            val altEx = allExs.find { it.id == altId }
                            if (altEx != null) {
                                plateaus.add(
                                    DetectedPlateau(
                                        exerciseId = ex.id,
                                        exerciseName = ex.name,
                                        targetMuscleGroup = ex.targetMuscleGroup,
                                        currentMax1RM = lastWeight,
                                        streakOfStagnation = 2,
                                        recommendedAlternativeId = altEx.id,
                                        recommendedAlternativeName = altEx.name
                                    )
                                )
                            }
                        }
                    }
                }
            }
            _detectedPlateaus.value = plateaus
        }
    }

    fun simulatePlateauHistory() {
        viewModelScope.launch {
            _uiState.value = FitnessUiState.Loading
            
            // Create session 1 for Barbell Bench Press (ex_bench_press)
            val session1Id = repository.saveWorkoutSession(
                WorkoutSession(
                    workoutName = "Push Day Alpha",
                    durationSeconds = 1800,
                    caloriesBurned = 250.0
                )
            )
            repository.saveExerciseLog(
                ExerciseLog(sessionId = session1Id, exerciseId = "ex_bench_press", exerciseName = "Barbell Bench Press", setNumber = 1, weightKg = 80.0, repsCompleted = 8, estimated1RM = 101.3)
            )

            // Create session 2 stall for Barbell Bench Press (ex_bench_press)
            val session2Id = repository.saveWorkoutSession(
                WorkoutSession(
                    workoutName = "Push Day Beta",
                    durationSeconds = 1850,
                    caloriesBurned = 240.0
                )
            )
            repository.saveExerciseLog(
                ExerciseLog(sessionId = session2Id, exerciseId = "ex_bench_press", exerciseName = "Barbell Bench Press", setNumber = 1, weightKg = 80.0, repsCompleted = 6, estimated1RM = 96.0)
            )

            // Inject fatigue into the heatmap for chest and shoulders
            val currentFatigue = _fatigueMap.value.toMutableMap()
            currentFatigue["Chest"] = 0.85f
            currentFatigue["Shoulders"] = 0.70f
            _fatigueMap.value = currentFatigue

            delay(100)
            analyzePlateaus()
            _uiState.value = FitnessUiState.Success
        }
    }

    fun resolveActivePlateau(plateau: DetectedPlateau) {
        viewModelScope.launch {
            _uiState.value = FitnessUiState.Loading

            // Substitute the plateaued exercise permanently in schedules
            val allSchedules = repository.schedules.first()
            allSchedules.forEach { sched ->
                if (sched.exerciseIds.contains(plateau.exerciseId)) {
                    val currentIds = sched.exerciseIds.split(",").filter { it.isNotBlank() }.toMutableList()
                    val index = currentIds.indexOf(plateau.exerciseId)
                    if (index != -1) {
                        currentIds[index] = plateau.recommendedAlternativeId
                        val updatedSchedule = sched.copy(exerciseIds = currentIds.joinToString(","))
                        repository.updateSchedule(updatedSchedule)
                    }
                }
            }

            delay(150)
            analyzePlateaus()
            _uiState.value = FitnessUiState.Success
        }
    }

    fun selectDay(day: Int) {
        _currentDaySelected.value = day
    }

    // --- Onboarding & Profile Saving ---
    fun saveProfileFromOnboarding() {
        viewModelScope.launch {
            _uiState.value = FitnessUiState.Loading
            val existing = repository.getUserProfileDirect()
            val profile = UserProfile(
                weightKg = onboardingWeight.value.toDoubleOrNull() ?: 75.0,
                heightCm = onboardingHeight.value.toDoubleOrNull() ?: 175.0,
                age = onboardingAge.value.toIntOrNull() ?: 28,
                bodyFatPercentage = onboardingBodyFat.value.toDoubleOrNull() ?: 15.0,
                chestCm = onboardingChest.value.toDoubleOrNull() ?: 100.0,
                armsCm = onboardingArms.value.toDoubleOrNull() ?: 35.0,
                waistCm = onboardingWaist.value.toDoubleOrNull() ?: 82.0,
                thighsCm = onboardingThighs.value.toDoubleOrNull() ?: 55.0,
                primaryGoal = onboardingPrimaryGoal.value,
                secondaryGoal = onboardingSecondaryGoal.value,
                equipmentInventory = onboardingEquipment.value,
                availableDaysPerWeek = onboardingDays.value,
                maxTimeMinutes = onboardingTimeMax.value,
                experienceLevel = onboardingExperienceLevel.value,
                onboardingTimestamp = existing?.onboardingTimestamp ?: System.currentTimeMillis(),
                gender = onboardingGender.value
            )
            repository.saveUserProfile(profile)
            repository.generateWorkoutSchedule(profile)
            _uiState.value = FitnessUiState.Success
        }
    }

    // --- Live active workout ---
    fun startActiveWorkout(schedule: WorkoutSchedule) {
        viewModelScope.launch {
            _uiState.value = FitnessUiState.Loading
            val exIds = schedule.exerciseIds.split(",").filter { it.isNotEmpty() }
            val exercisesList = mutableListOf<Exercise>()
            val initialLogs = mutableMapOf<String, List<ExerciseLog>>()

            for (id in exIds) {
                repository.getExerciseById(id)?.let { rawEx ->
                    val ex = getPeriodSafeExercise(rawEx)
                    exercisesList.add(ex)
                    initialLogs[ex.id] = listOf(
                        ExerciseLog(sessionId = 0, exerciseId = ex.id, exerciseName = ex.name, setNumber = 1, weightKg = 0.0, repsCompleted = 0)
                    )
                }
            }

            _activeSessionId.value = System.currentTimeMillis()
            _currentActiveWorkoutName.value = schedule.workoutName
            _activeWorkoutExercises.value = exercisesList
            _exerciseLogsInProgress.value = initialLogs
            _uiState.value = FitnessUiState.Success
        }
    }

    fun addSetToExercise(exerciseId: String) {
        val currentList = _exerciseLogsInProgress.value[exerciseId] ?: emptyList()
        val nextSetNumber = currentList.size + 1
        val newLog = ExerciseLog(
            sessionId = 0,
            exerciseId = exerciseId,
            exerciseName = currentList.firstOrNull()?.exerciseName ?: "Exercise",
            setNumber = nextSetNumber,
            weightKg = currentList.lastOrNull()?.weightKg ?: 0.0,
            repsCompleted = currentList.lastOrNull()?.repsCompleted ?: 0
        )
        val updatedMap = _exerciseLogsInProgress.value.toMutableMap()
        updatedMap[exerciseId] = currentList + newLog
        _exerciseLogsInProgress.value = updatedMap
    }

    fun updateSetValues(exerciseId: String, setIndex: Int, weight: Double, reps: Int) {
        val currentList = _exerciseLogsInProgress.value[exerciseId]?.toMutableList() ?: return
        if (setIndex in currentList.indices) {
            val updatedLog = currentList[setIndex].copy(
                weightKg = weight,
                repsCompleted = reps,
                // Epley 1RM: w * (1 + r/30)
                estimated1RM = if (reps > 0) weight * (1.0 + reps / 30.0) else 0.0
            )
            currentList[setIndex] = updatedLog
            val updatedMap = _exerciseLogsInProgress.value.toMutableMap()
            updatedMap[exerciseId] = currentList
            _exerciseLogsInProgress.value = updatedMap
        }
    }

    fun logFinishedSet() {
        // Trigger rest countdown (custom countdown of e.g. 90 secs)
        timerJob?.cancel()
        _restTimerSeconds.value = 90
        timerJob = viewModelScope.launch {
            while (_restTimerSeconds.value > 0) {
                delay(1000)
                _restTimerSeconds.value -= 1
            }
        }
    }

    fun stopTimer() {
        _restTimerSeconds.value = 0
        timerJob?.cancel()
    }

    fun finishCurrentWorkout() {
        val sessionId = _activeSessionId.value ?: return
        val workoutName = _currentActiveWorkoutName.value ?: "Workout Session"
        viewModelScope.launch {
            _uiState.value = FitnessUiState.Loading

            // Save central session
            val sessionKey = repository.saveWorkoutSession(
                WorkoutSession(
                    workoutName = workoutName,
                    durationSeconds = 2400, // 40 minutes mock duration
                    caloriesBurned = 320.0
                )
            )

            // Save detailed logs
            _exerciseLogsInProgress.value.values.flatten().forEach { log ->
                if (log.repsCompleted > 0) {
                    repository.saveExerciseLog(log.copy(sessionId = sessionKey))
                }
            }

            // Save daily active calories burned
            repository.addActiveCalories(320.0)

            // Clear states
            _activeSessionId.value = null
            _currentActiveWorkoutName.value = null
            _activeWorkoutExercises.value = emptyList()
            _exerciseLogsInProgress.value = emptyMap()
            stopTimer()

            // Re-calculate muscle recovery fatigue ratings
            calculateMuscleFatigue()

            _uiState.value = FitnessUiState.Success
        }
    }

    fun swapExercise(oldExerciseId: String, newExercise: Exercise) {
        viewModelScope.launch {
            val updatedExs = _activeWorkoutExercises.value.toMutableList()
            val index = updatedExs.indexOfFirst { it.id == oldExerciseId }
            if (index != -1) {
                updatedExs[index] = newExercise
                _activeWorkoutExercises.value = updatedExs

                // Reset logs for this specific item
                val updatedLogsMap = _exerciseLogsInProgress.value.toMutableMap()
                updatedLogsMap.remove(oldExerciseId)
                updatedLogsMap[newExercise.id] = listOf(
                    ExerciseLog(sessionId = 0, exerciseId = newExercise.id, exerciseName = newExercise.name, setNumber = 1, weightKg = 0.0, repsCompleted = 0)
                )
                _exerciseLogsInProgress.value = updatedLogsMap
            }
        }
    }

    // --- Muscle recovery calculations (Phase 4 Heatmap) ---
    private suspend fun calculateMuscleFatigue() {
        val sessionsInLast48 = repository.allSessions.first()
        val fatigue = mutableMapOf(
            "Chest" to 0.1f,
            "Back" to 0.1f,
            "Quads" to 0.1f,
            "Hamstrings" to 0.1f,
            "Shoulders" to 0.1f,
            "Arms" to 0.1f,
            "Core" to 0.1f
        )
        // Dynamically compute fatigue scoring based on recorded sets
        sessionsInLast48.take(5).forEach { session ->
            val logs = repository.getLogsForSession(session.id).first()
            logs.forEach { log ->
                // Map exercise target
                val ex = repository.getExerciseById(log.exerciseId)
                if (ex != null) {
                    val group = ex.targetMuscleGroup
                    val currentVal = fatigue[group] ?: 0.0f
                    // Accumulate workload: each set adds fatigue
                    val newVal = (currentVal + 0.15f).coerceAtMost(1.0f)
                    fatigue[group] = newVal
                }
            }
        }
        _fatigueMap.value = fatigue
    }

    // --- Active tracking utilities ---
    private val pendingSteps = java.util.concurrent.atomic.AtomicInteger(0)
    private var flushJob: Job? = null

    private fun triggerThrottledFlush() {
        if (flushJob != null && flushJob?.isActive == true) {
            return
        }
        flushJob = viewModelScope.launch(Dispatchers.IO) {
            delay(2500)
            val stepsToCommit = pendingSteps.getAndSet(0)
            if (stepsToCommit > 0) {
                try {
                    repository.incrementSteps(stepsToCommit)
                } catch (e: Exception) {
                    e.printStackTrace()
                    // Restore in case of failure
                    pendingSteps.addAndGet(stepsToCommit)
                }
            }
        }
    }

    fun trackPassiveSteps(amount: Int) {
        val currentLog = _todayHealthLog.value
        val updatedSteps = currentLog.stepsCount + amount
        val addedCalories = amount * 0.04
        _todayHealthLog.value = currentLog.copy(
            stepsCount = updatedSteps,
            activeCaloriesBurned = currentLog.activeCaloriesBurned + addedCalories
        )

        pendingSteps.addAndGet(amount)
        triggerThrottledFlush()

        if (_isRunActive.value) {
            _runSteps.value += amount
            if (lastLocation == null) {
                // Approximate 1.25 meters of distance per step when GPS is calibrating or not active
                _runDistanceMeters.value += amount * 1.25
            }
        }
    }

    fun startRunning() {
        if (_isRunActive.value) return
        _isRunActive.value = true
        _runSeconds.value = 0
        _runSteps.value = 0
        _runDistanceMeters.value = 0.0
        _runCurrentSpeed.value = 0.0
        _runLatitude.value = null
        _runLongitude.value = null
        lastLocation = null

        runTimerJob = viewModelScope.launch {
            while (_isRunActive.value) {
                delay(1000)
                _runSeconds.value += 1
                if (_runCurrentSpeed.value == 0.0 && _runSteps.value > 0) {
                    _runCurrentSpeed.value = (_runDistanceMeters.value / _runSeconds.value).coerceIn(1.0, 5.5)
                }
            }
        }

        startLocationTracking()
    }

    fun stopRunning() {
        if (!_isRunActive.value) return
        _isRunActive.value = false
        runTimerJob?.cancel()
        runTimerJob = null
        stopLocationTracking()

        val distKm = _runDistanceMeters.value / 1000.0
        val runningCalories = (distKm * 68.0) + (_runSteps.value * 0.045)
        if (runningCalories > 0.5) {
            viewModelScope.launch {
                repository.addActiveCalories(runningCalories)
                repository.incrementSteps(_runSteps.value)
            }
        }
    }

    private fun startLocationTracking() {
        val ctx = getApplication<Application>()
        val lm = ctx.getSystemService(Context.LOCATION_SERVICE) as? LocationManager ?: return

        val hasFine = androidx.core.content.ContextCompat.checkSelfPermission(
            ctx,
            android.Manifest.permission.ACCESS_FINE_LOCATION
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED

        val hasCoarse = androidx.core.content.ContextCompat.checkSelfPermission(
            ctx,
            android.Manifest.permission.ACCESS_COARSE_LOCATION
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED

        if (!hasFine && !hasCoarse) return

        try {
            val listener = object : LocationListener {
                override fun onLocationChanged(location: Location) {
                    val prev = lastLocation
                    _runLatitude.value = location.latitude
                    _runLongitude.value = location.longitude

                    if (prev != null) {
                        val distanceSegment = prev.distanceTo(location).toDouble()
                        if (distanceSegment > 1.2 && distanceSegment < 120.0) {
                            _runDistanceMeters.value += distanceSegment
                            if (location.hasSpeed()) {
                                _runCurrentSpeed.value = location.speed.toDouble()
                            } else {
                                val sec = (location.time - prev.time) / 1000.0
                                if (sec > 0) {
                                    _runCurrentSpeed.value = distanceSegment / sec
                                }
                            }
                        }
                    } else {
                        if (location.hasSpeed()) {
                            _runCurrentSpeed.value = location.speed.toDouble()
                        }
                    }
                    lastLocation = location
                }
                override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
                override fun onProviderEnabled(provider: String) {}
                override fun onProviderDisabled(provider: String) {}
            }
            locationListener = listener

            if (lm.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                lm.requestLocationUpdates(
                    LocationManager.GPS_PROVIDER,
                    2000L,
                    1f,
                    listener,
                    android.os.Looper.getMainLooper()
                )
            }
            if (lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                lm.requestLocationUpdates(
                    LocationManager.NETWORK_PROVIDER,
                    3000L,
                    2f,
                    listener,
                    android.os.Looper.getMainLooper()
                )
            }
        } catch (e: SecurityException) {
            e.printStackTrace()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun stopLocationTracking() {
        val ctx = getApplication<Application>()
        try {
            locationListener?.let {
                val lm = ctx.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
                lm?.removeUpdates(it)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        locationListener = null
    }

    override fun onCleared() {
        super.onCleared()
        val stepsToCommit = pendingSteps.getAndSet(0)
        if (stepsToCommit > 0) {
            @OptIn(kotlinx.coroutines.DelicateCoroutinesApi::class)
            kotlinx.coroutines.GlobalScope.launch(Dispatchers.IO) {
                try {
                    repository.incrementSteps(stepsToCommit)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    fun recordWaterIntake(ml: Int) {
        viewModelScope.launch {
            try {
                repository.addWaterIntake(ml)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // --- AI Nutrition / Gemini Integration (Phase 3) ---
    fun askGeminiCoach(question: String) {
        if (question.isBlank()) return
        val currentHistory = _chatHistory.value.toMutableList()
        currentHistory.add(ChatMessage("user", question))
        _chatHistory.value = currentHistory

        _isChatLoading.value = true

        viewModelScope.launch {
            val userProf = repository.getUserProfileDirect()
            val textAns = queryGeminiApi(userProf, currentHistory)
            _chatHistory.value = _chatHistory.value + ChatMessage("assistant", textAns)
            _isChatLoading.value = false
        }
    }

    private suspend fun queryGeminiApi(profile: UserProfile?, history: List<ChatMessage>): String = withContext(Dispatchers.IO) {
        val apiKey = com.example.BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext "API Configuration Notice: Please register your actual GEMINI_API_KEY in the Secrets panel in Google AI Studio to fetch a dynamic, live response from Gemini."
        }

        // Build robust fitness instructions based on profile stats and historical trackers
        val p = profile ?: UserProfile()
        val log = _todayHealthLog.value
        val fatigue = _fatigueMap.value.entries.joinToString { "${it.key}: ${(it.value * 100).toInt()}% fatigue" }

        val isBeginnerPeriod = if (p.experienceLevel.equals("Beginner", ignoreCase = true)) {
            val daysPassed = (System.currentTimeMillis() - p.onboardingTimestamp) / (24 * 60 * 60 * 1000)
            daysPassed < 30
        } else {
            false
        }

        // Precise target calorie and macronutrient calculation for sync
        val targetWeight = p.weightKg
        val targetGoal = p.primaryGoal
        val computedCalories = when (targetGoal.lowercase()) {
            "hypertrophy" -> (targetWeight * 33).toInt() + 300
            "strength" -> (targetWeight * 31).toInt() + 200
            "fat_loss", "fat loss" -> (targetWeight * 28).toInt() - 400
            else -> (targetWeight * 30).toInt() // maintenance
        }
        val computedProtein = (targetWeight * 2.1).toInt()
        val computedFats = (targetWeight * 1.0).toInt()
        val computedCarbs = (computedCalories - (computedProtein * 4) - (computedFats * 9)) / 4

        val periodAddendum = if (p.gender.equals("Female", ignoreCase = true) && _isOnPeriod.value) {
            "\nCRITICAL NOTE: The user is currently on her period and Period Mode is enabled. Provide gentle, comfort-focused advice, warm, supportive language, and suggest only light exercises (like walking, static stretches, gentle yoga, child's pose, and light pelvic mobility) to avoid cramp-inducing abdominal strain or pelvic aggravation. Avoid high-impact or heavy lifting suggestions."
        } else {
            ""
        }

        val promptText = "User Profiling:\n" +
                "Weight: ${p.weightKg} kg, Height: ${p.heightCm} cm, Age: ${p.age}\n" +
                "Estimated fat %: ${p.bodyFatPercentage}%\n" +
                "Primary Goal: ${p.primaryGoal}, Equipment list: ${p.equipmentInventory}\n" +
                "Gender: ${p.gender}\n" +
                "Experience Level: ${p.experienceLevel} (Beginner phase active: $isBeginnerPeriod)\n" +
                "Measurements - Chest: ${p.chestCm}cm, Arms: ${p.armsCm}cm, Waist: ${p.waistCm}cm, Thighs: ${p.thighsCm}cm.\n\n" +
                "Target Nutrition Calculated in the App (MUST align perfectly with this in your answers):\n" +
                "Calories Target: $computedCalories kcal\n" +
                "Protein Target: ${computedProtein}g\n" +
                "Carbs Target: ${computedCarbs}g\n" +
                "Fats Target: ${computedFats}g\n\n" +
                "Today's Active Health Tracking stats (use this dynamic data to give custom comments):\n" +
                "Steps completed: ${log.stepsCount}, Sleep tracked: ${log.sleepMinutes / 60.0} hours, Water Intake: ${log.waterIntakeMl} ml, Active Calories Burned: ${log.activeCaloriesBurned} kcal.\n\n" +
                "Muscle Fatigue Levels:\n" +
                "$fatigue\n\n" +
                "Conversation History:\n" +
                history.joinToString("\n") { "${it.role}: ${it.message}" } +
                periodAddendum +
                "\n\nOmniFit AI, please provide laser-focused macro calculations, diet strategy modifications, and physical habit corrections without generic fluff. " +
                "Always refer to the user's actual target macro numbers ($computedCalories kcal, ${computedProtein}g Protein, ${computedCarbs}g Carbs, ${computedFats}g Fats) and " +
                "explain the physiological importance of matching recommended reps & weights (e.g. why beginners need higher reps like 12-15 to build clean joint motor patterns, in contrast to heavy load low rep work)."

        try {
            val req = GeminiRequest(
                contents = listOf(
                    GeminiContent(
                        parts = listOf(
                            GeminiPart(text = promptText)
                        )
                    )
                ),
                systemInstruction = GeminiContent(
                    parts = listOf(
                        GeminiPart("You are the supreme omniscient Gym personal trainer and medical sports scientist. Respond with bold, authoritative, scientifically-accurate fitness tips, macro target adjustments that keep up-to-date with progressive stats, and tactical meal generator advice. CRITICAL FORMATTING RULE: You MUST format your entire response using ONLY clean, concise bullet points or numbered lists. Every single point must be punchy, highly structured, and direct. Do NOT write blocks of paragraphs.")
                    )
                )
            )
            val res = GeminiRetrofitClient.service.generateContent(apiKey, req)
            res.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                ?: "I was unable to retrieve a response. Let's make sure we try again!"
        } catch (e: Exception) {
            "Diagnostic Message: Error occurred while communicating with Gemini API: ${e.message}"
        }
    }
}

sealed interface FitnessUiState {
    object Success : FitnessUiState
    object Loading : FitnessUiState
    data class Error(val message: String) : FitnessUiState
}

data class ChatMessage(
    val role: String, // "user" or "assistant"
    val message: String
)

data class DetectedPlateau(
    val exerciseId: String,
    val exerciseName: String,
    val targetMuscleGroup: String,
    val currentMax1RM: Double,
    val streakOfStagnation: Int,
    val recommendedAlternativeId: String,
    val recommendedAlternativeName: String
)

class FitnessViewModelFactory(
    private val app: Application,
    private val repository: FitnessRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(classType: Class<T>): T {
        if (classType.isAssignableFrom(FitnessViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return FitnessViewModel(app, repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
