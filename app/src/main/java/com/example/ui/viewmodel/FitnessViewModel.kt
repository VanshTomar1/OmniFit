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
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class WorkoutSummary(
    val id: Long,
    val name: String,
    val durationSeconds: Long,
    val caloriesBurned: Double,
    val totalVolumeKg: Double,
    val totalSets: Int,
    val totalReps: Int,
    val exercisesPerformed: List<String>,
    val muscleGroupsWorked: List<String>,
    val isPeriodMode: Boolean,
    val isBeginnerPeriod: Boolean
)

data class InAppNotification(
    val title: String,
    val message: String,
    val iconType: String = "info", // "steps", "burn", "run", "achievement", "coach"
    val timestamp: Long = System.currentTimeMillis()
)

data class CustomGoal(
    val id: String = java.util.UUID.randomUUID().toString(),
    val title: String,
    val currentValue: Double,
    val targetValue: Double,
    val unit: String,
    val emoji: String = "🏃"
) {
    val progress: Float get() = if (targetValue > 0) (currentValue / targetValue).toFloat().coerceIn(0f, 1f) else 0f
}

data class CustomChallenge(
    val id: String = java.util.UUID.randomUUID().toString(),
    val title: String,
    val currentDays: Int,
    val totalDays: Int,
    val emoji: String = "🏋️"
) {
    val progress: Float get() = if (totalDays > 0) (currentDays.toFloat() / totalDays).coerceIn(0f, 1f) else 0f
}

data class SmartDevice(
    val id: String = java.util.UUID.randomUUID().toString(),
    val name: String,
    val brand: String,
    val type: String, // "Watch", "Ring", "Band", "Chest Strap", "Ecosystem", "Other"
    val isConnected: Boolean,
    val batteryPercent: Int = 85,
    val syncStatus: String = "Synced 2m ago",
    val syncedMetrics: String = "Steps, Heart Rate, Active Burn"
)

class FitnessViewModel(
    application: Application,
    private val repository: FitnessRepository
) : AndroidViewModel(application) {

    // --- Customizable Goals & Challenges States ---
    private val _goalsList = MutableStateFlow<List<CustomGoal>>(
        listOf(
            CustomGoal(title = "Run 5k < 25min", currentValue = 3.0, targetValue = 5.0, unit = "km", emoji = "🏃"),
            CustomGoal(title = "Lift 100kg Bench Press", currentValue = 85.0, targetValue = 100.0, unit = "kg", emoji = "🏋️"),
            CustomGoal(title = "Drink 3L of Water Daily", currentValue = 1500.0, targetValue = 3000.0, unit = "ml", emoji = "💧")
        )
    )
    val goalsList: StateFlow<List<CustomGoal>> = _goalsList.asStateFlow()

    private val _challengesList = MutableStateFlow<List<CustomChallenge>>(
        listOf(
            CustomChallenge(title = "Complete 30-Day Squat Challenge", currentDays = 18, totalDays = 30, emoji = "🏋️"),
            CustomChallenge(title = "Morning Yoga Streak", currentDays = 6, totalDays = 7, emoji = "🧘"),
            CustomChallenge(title = "Daily Steps consistency", currentDays = 12, totalDays = 15, emoji = "👟")
        )
    )
    val challengesList: StateFlow<List<CustomChallenge>> = _challengesList.asStateFlow()

    fun addGoal(title: String, target: Double, unit: String, emoji: String) {
        val newList = _goalsList.value + CustomGoal(title = title, currentValue = 0.0, targetValue = target, unit = unit, emoji = emoji)
        _goalsList.value = newList
    }

    fun updateGoalProgress(id: String, increment: Double) {
        val newList = _goalsList.value.map {
            if (it.id == id) {
                val newVal = (it.currentValue + increment).coerceAtMost(it.targetValue)
                it.copy(currentValue = newVal)
            } else {
                it
            }
        }
        _goalsList.value = newList
    }

    fun deleteGoal(id: String) {
        _goalsList.value = _goalsList.value.filter { it.id != id }
    }

    fun addChallenge(title: String, totalDays: Int, emoji: String) {
        val newList = _challengesList.value + CustomChallenge(title = title, currentDays = 0, totalDays = totalDays, emoji = emoji)
        _challengesList.value = newList
    }

    fun incrementChallengeDay(id: String) {
        val newList = _challengesList.value.map {
            if (it.id == id) {
                val newVal = (it.currentDays + 1).coerceAtMost(it.totalDays)
                it.copy(currentDays = newVal)
            } else {
                it
            }
        }
        _challengesList.value = newList
    }

    fun deleteChallenge(id: String) {
        _challengesList.value = _challengesList.value.filter { it.id != id }
    }

    // --- Customizable Daily Vital Targets ---
    private val _stepsGoal = MutableStateFlow(10000)
    val stepsGoal: StateFlow<Int> = _stepsGoal.asStateFlow()

    private val _caloriesGoal = MutableStateFlow(700.0)
    val caloriesGoal: StateFlow<Double> = _caloriesGoal.asStateFlow()

    private val _waterGoal = MutableStateFlow(2500)
    val waterGoal: StateFlow<Int> = _waterGoal.asStateFlow()

    fun updateMetricGoals(steps: Int, calories: Double, water: Int) {
        _stepsGoal.value = steps
        _caloriesGoal.value = calories
        _waterGoal.value = water
    }

    fun modifyDirectDailyVitals(steps: Int?, calories: Double?, water: Int?) {
        val currentLog = _todayHealthLog.value
        val updatedSteps = steps ?: currentLog.stepsCount
        val updatedCalories = calories ?: currentLog.activeCaloriesBurned
        val updatedWater = water ?: currentLog.waterIntakeMl
        
        _todayHealthLog.value = currentLog.copy(
            stepsCount = updatedSteps,
            activeCaloriesBurned = updatedCalories,
            waterIntakeMl = updatedWater
        )
        
        // Trigger save schedule
        triggerThrottledFlush()
    }

    // --- SharedPreferences & Authentication Core States ---
    private val prefs = getApplication<Application>().getSharedPreferences("com.aistudio.omnifit.PREFS", android.content.Context.MODE_PRIVATE)

    private val _isLoggedIn = MutableStateFlow(true)
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn.asStateFlow()

    private val _loggedInEmail = MutableStateFlow(prefs.getString("logged_in_email", "guest@omnifit.com") ?: "guest@omnifit.com")
    val loggedInEmail: StateFlow<String> = _loggedInEmail.asStateFlow()

    private val _useImperial = MutableStateFlow(prefs.getBoolean("use_imperial", false))
    val useImperial: StateFlow<Boolean> = _useImperial.asStateFlow()

    private val _unitSystem = MutableStateFlow(prefs.getString("unit_system", "metric") ?: "metric")
    val unitSystem: StateFlow<String> = _unitSystem.asStateFlow()

    fun toggleUnits(useImperial: Boolean) {
        _useImperial.value = useImperial
        prefs.edit().putBoolean("use_imperial", useImperial).apply()
        if (useImperial) {
            _unitSystem.value = "imperial_us"
            prefs.edit().putString("unit_system", "imperial_us").apply()
        } else {
            _unitSystem.value = "metric"
            prefs.edit().putString("unit_system", "metric").apply()
        }
    }

    fun setUnitSystem(system: String) {
        _unitSystem.value = system
        prefs.edit().putString("unit_system", system).apply()
        _useImperial.value = (system != "metric")
        prefs.edit().putBoolean("use_imperial", system != "metric").apply()
    }

    private val _appLanguage = MutableStateFlow(prefs.getString("app_language", "en") ?: "en")
    val appLanguage: StateFlow<String> = _appLanguage.asStateFlow()

    fun setAppLanguage(lang: String) {
        _appLanguage.value = lang
        prefs.edit().putString("app_language", lang).apply()
    }

    // App Theme Setting (defaults to "dark")
    private val _themeSetting = MutableStateFlow(prefs.getString("app_theme_setting", "dark") ?: "dark")
    val themeSetting: StateFlow<String> = _themeSetting.asStateFlow()

    fun setThemeSetting(theme: String) {
        _themeSetting.value = theme
        prefs.edit().putString("app_theme_setting", theme).apply()
    }

    private val _loggedInName = MutableStateFlow(prefs.getString("logged_in_name", "Athlete") ?: "Athlete")
    val loggedInName: StateFlow<String> = _loggedInName.asStateFlow()

    private val _loggedInMethod = MutableStateFlow("Local")
    val loggedInMethod: StateFlow<String> = _loggedInMethod.asStateFlow()

    // --- Universal Smart Health Tracker Devices Integration ---
    private val _smartDevices = MutableStateFlow<List<SmartDevice>>(emptyList())
    val smartDevices: StateFlow<List<SmartDevice>> = _smartDevices.asStateFlow()

    private val _deviceSyncOverride = MutableStateFlow(prefs.getBoolean("device_sync_override", true))
    val deviceSyncOverride: StateFlow<Boolean> = _deviceSyncOverride.asStateFlow()

    // Dynamic Walkthrough / Onboarding replay state
    val showWalkthroughOverride = MutableStateFlow(false)

    // Live Heart Rate telemetry streaming from Bluetooth wearable
    private val _liveHeartRate = MutableStateFlow(0)
    val liveHeartRate: StateFlow<Int> = _liveHeartRate.asStateFlow()

    fun setDeviceSyncOverride(enabled: Boolean) {
        _deviceSyncOverride.value = enabled
        prefs.edit().putBoolean("device_sync_override", enabled).apply()
        triggerDeviceMockDataSync()
    }

    fun triggerDeviceMockDataSync() {
        if (!_deviceSyncOverride.value) return
        val activeDevices = _smartDevices.value.filter { it.isConnected }
        if (activeDevices.isEmpty()) return

        viewModelScope.launch {
            val email = loggedInEmail.value.ifBlank { "guest@omnifit.com" }
            val currentLog = _todayHealthLog.value
            
            var targetSteps = 0
            var targetCalories = 0.0
            var targetWater = 0
            var targetSleep = 0

            activeDevices.forEach { dev ->
                when (dev.type) {
                    "Watch" -> {
                        targetSteps += 7800
                        targetCalories += 450.0
                        targetSleep += 420
                    }
                    "Ring" -> {
                        targetSteps += 5100
                        targetCalories += 220.0
                        targetSleep += 460
                    }
                    "Band" -> {
                        targetSteps += 6200
                        targetCalories += 310.0
                    }
                    "Chest Strap" -> {
                        targetCalories += 500.0 // Extra cardiac exercise burn
                    }
                    else -> {
                        targetSteps += 2000
                        targetCalories += 120.0
                    }
                }
                if (dev.syncedMetrics.contains("Water", ignoreCase = true) || dev.syncedMetrics.contains("Hydration", ignoreCase = true) || dev.syncedMetrics.contains("Oxygen", ignoreCase = true)) {
                    targetWater += 1000
                }
            }

            // Merge values
            val finalSteps = maxOf(currentLog.stepsCount, targetSteps)
            val finalCalories = maxOf(currentLog.activeCaloriesBurned, targetCalories)
            val finalWater = maxOf(currentLog.waterIntakeMl, targetWater)
            val finalSleep = maxOf(currentLog.sleepMinutes, targetSleep)

            val updatedLog = currentLog.copy(
                stepsCount = finalSteps,
                activeCaloriesBurned = finalCalories,
                waterIntakeMl = finalWater,
                sleepMinutes = finalSleep
            )
            _todayHealthLog.value = updatedLog
            try {
                repository.saveHealthTrackerLog(updatedLog)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun loadSmartDevices() {
        val presets = listOf(
            SmartDevice("dev_apple", "Apple Watch Ultra 2", "Apple", "Watch", prefs.getBoolean("dev_apple_conn", false), 92, "Sync done", "Heart Rate, Steps, Blood Oxygen"),
            SmartDevice("dev_garmin", "Garmin Fenix 7 Pro", "Garmin", "Watch", prefs.getBoolean("dev_garmin_conn", false), 88, "Sync done", "Sleep, HR, GPS Track"),
            SmartDevice("dev_oura", "Oura Ring Gen 3", "Oura", "Ring", prefs.getBoolean("dev_oura_conn", false), 79, "Sync done", "Heart Rate Variability, Sleep, Temp"),
            SmartDevice("dev_fitbit", "Fitbit Charge 6", "Fitbit", "Band", prefs.getBoolean("dev_fitbit_conn", false), 64, "Sync done", "Active Zone Mins, Calories"),
            SmartDevice("dev_samsung", "Samsung Galaxy Ring", "Samsung", "Ring", prefs.getBoolean("dev_samsung_conn", false), 85, "Sync done", "Sleep Score, Stress Log"),
            SmartDevice("dev_polar", "Polar H10 Chest Strap", "Polar", "Chest Strap", prefs.getBoolean("dev_polar_conn", false), 95, "Live Stream", "Real-Time EKG Heart Rate")
        )

        val customDevicesList = mutableListOf<SmartDevice>()
        val customDeviceIds = prefs.getStringSet("custom_device_ids", emptySet()) ?: emptySet()
        for (id in customDeviceIds) {
            val name = prefs.getString("custom_${id}_name", "") ?: ""
            val brand = prefs.getString("custom_${id}_brand", "") ?: ""
            val type = prefs.getString("custom_${id}_type", "Other") ?: "Other"
            val connected = prefs.getBoolean("custom_${id}_conn", true)
            val batt = prefs.getInt("custom_${id}_batt", 85)
            val metrics = prefs.getString("custom_${id}_metrics", "Steps, Vitals") ?: "Steps, Vitals"
            if (name.isNotEmpty()) {
                customDevicesList.add(SmartDevice(id, name, brand, type, connected, batt, "Online Dynamic Sync", metrics))
            }
        }

        _smartDevices.value = presets + customDevicesList
        triggerDeviceMockDataSync()
    }

    fun toggleDeviceConnection(id: String) {
        val list = _smartDevices.value.map { device ->
            if (device.id == id) {
                val newStatus = !device.isConnected
                if (id.startsWith("dev_")) {
                    prefs.edit().putBoolean("${id}_conn", newStatus).apply()
                } else {
                    prefs.edit().putBoolean("custom_${id}_conn", newStatus).apply()
                }
                device.copy(isConnected = newStatus, syncStatus = if (newStatus) "Synced just now" else "Disconnected")
            } else {
                device
            }
        }
        _smartDevices.value = list
        triggerDeviceMockDataSync()
    }

    fun registerCustomDevice(name: String, brand: String, type: String, metrics: String) {
        val newId = java.util.UUID.randomUUID().toString().take(6)
        prefs.edit().apply {
            putString("custom_${newId}_name", name)
            putString("custom_${newId}_brand", brand)
            putString("custom_${newId}_type", type)
            putBoolean("custom_${newId}_conn", true)
            putInt("custom_${newId}_batt", (75..100).random())
            putString("custom_${newId}_metrics", metrics)
            
            val set = HashSet(prefs.getStringSet("custom_device_ids", emptySet()) ?: emptySet())
            set.add(newId)
            putStringSet("custom_device_ids", set)
            apply()
        }
        loadSmartDevices()
    }

    fun removeCustomDevice(id: String) {
        prefs.edit().apply {
            remove("custom_${id}_name")
            remove("custom_${id}_brand")
            remove("custom_${id}_type")
            remove("custom_${id}_conn")
            remove("custom_${id}_batt")
            remove("custom_${id}_metrics")
            
            val set = HashSet(prefs.getStringSet("custom_device_ids", emptySet()) ?: emptySet())
            set.remove(id)
            putStringSet("custom_device_ids", set)
            apply()
        }
        loadSmartDevices()
    }

    // --- Flows from Room DB ---
    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val userProfile = _loggedInEmail.flatMapLatest { email ->
        repository.getUserProfile(email.ifBlank { "guest@omnifit.com" })
    }
    val schedules = repository.schedules
    val allExercises = repository.allExercises
    
    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val allSessions = _loggedInEmail.flatMapLatest { email ->
        repository.getSessionsForUser(email.ifBlank { "guest@omnifit.com" })
    }
    
    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val weeklyHealthLogs = _loggedInEmail.flatMapLatest { email ->
        repository.getWeeklyLogsForUser(email.ifBlank { "guest@omnifit.com" })
    }

    // --- UI/UX States ---
    private val _uiState = MutableStateFlow<FitnessUiState>(FitnessUiState.Success)
    val uiState: StateFlow<FitnessUiState> = _uiState.asStateFlow()

    private val _isProfileLoaded = MutableStateFlow(false)
    val isProfileLoaded = _isProfileLoaded.asStateFlow()

    // Period Tracking for Female Version
    private val _isOnPeriod = MutableStateFlow(prefs.getBoolean("is_on_period", false))
    val isOnPeriod: StateFlow<Boolean> = _isOnPeriod.asStateFlow()

    fun login(email: String, name: String, method: String) {
        _isLoggedIn.value = true
        _loggedInEmail.value = email
        _loggedInName.value = name
        _loggedInMethod.value = method
        prefs.edit()
            .putBoolean("is_logged_in", true)
            .putString("logged_in_email", email)
            .putString("logged_in_name", name)
            .putString("logged_in_method", method)
            .apply()
        
        generateMockHistoryForUser(email)
    }

    fun generateMockHistoryForUser(email: String) {
        viewModelScope.launch {
            val existing = repository.getUserProfileDirect(email)
            if (existing == null) {
                val weightVal = if (email == "vansh.tomar809@gmail.com") 78.5 else 74.0
                repository.saveUserProfile(
                    UserProfile(
                        email = email,
                        weightKg = weightVal,
                        heightCm = 178.0,
                        experienceLevel = "Intermediate",
                        primaryGoal = "strength",
                        photoUri = null
                    )
                )
            }

            // Check if user already has logged sessions
            val sessionsList = repository.getSessionsForUser(email).first()
            if (sessionsList.isEmpty()) {
                val todayMs = System.currentTimeMillis()
                val oneDayMs = 24 * 60 * 60 * 1000L
                val random = java.util.Random()

                // Generate 8 completed workout sessions
                val workoutNames = listOf("Push Power Lifts", "Pull Hypertrophy Day", "Leg Growth routine", "Dynamic Body Pump", "Interval HIIT Cardio")
                for (i in 1..8) {
                    val daysAgo = i * 2L
                    val calBurn = 220.0 + random.nextInt(320)
                    val durSecs = 1800L + random.nextInt(1800)
                    repository.saveWorkoutSession(
                        WorkoutSession(
                            userEmail = email,
                            dateTimestamp = todayMs - (daysAgo * oneDayMs),
                            workoutName = workoutNames[i % workoutNames.size],
                            durationSeconds = durSecs,
                            caloriesBurned = calBurn
                        )
                    )
                }

                // Generate 30 days of steps and tracker logs to enable day/week/month/year toggle analytics
                for (i in 0..29) {
                    val daysAgo = i.toLong()
                    val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                    val dateKey = sdf.format(Date(todayMs - (daysAgo * oneDayMs)))
                    
                    val stepMultiplier = if (daysAgo % 7 == 0L || daysAgo % 7 == 6L) 4500 else 8200
                    val randSteps = stepMultiplier + random.nextInt(3500)
                    val randWater = 1600 + random.nextInt(1400)
                    val randCal = randSteps * 0.04 + random.nextInt(120)

                    repository.saveHealthTrackerLog(
                        HealthTrackerLog(
                            id = "$dateKey:$email",
                            dateKey = dateKey,
                            userEmail = email,
                            stepsCount = randSteps,
                            waterIntakeMl = randWater,
                            activeCaloriesBurned = randCal
                        )
                    )
                }
            }
        }
    }

    fun logout() {
        val currentEmail = _loggedInEmail.value
        viewModelScope.launch {
            repository.deleteUserProfile(currentEmail)
            if (currentEmail != "guest@omnifit.com") {
                repository.deleteUserProfile("guest@omnifit.com")
            }
            
            _isLoggedIn.value = true
            _loggedInEmail.value = "guest@omnifit.com"
            _loggedInName.value = "Athlete"
            _loggedInMethod.value = "Local"
            prefs.edit()
                .putBoolean("is_logged_in", true)
                .putString("logged_in_email", "guest@omnifit.com")
                .putString("logged_in_name", "Athlete")
                .putString("logged_in_method", "Local")
                .putBoolean("has_opened_before", false) // Reset this so they get the intro too!
                .apply()
        }
    }

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
    private val _currentDaySelected = MutableStateFlow(
        when (java.util.Calendar.getInstance().get(java.util.Calendar.DAY_OF_WEEK)) {
            java.util.Calendar.MONDAY -> 1
            java.util.Calendar.TUESDAY -> 2
            java.util.Calendar.WEDNESDAY -> 3
            java.util.Calendar.THURSDAY -> 4
            java.util.Calendar.FRIDAY -> 5
            java.util.Calendar.SATURDAY -> 6
            java.util.Calendar.SUNDAY -> 7
            else -> 1
        }
    )
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

    private val _lastCompletedWorkoutSummary = MutableStateFlow<WorkoutSummary?>(null)
    val lastCompletedWorkoutSummary = _lastCompletedWorkoutSummary.asStateFlow()

    fun dismissWorkoutSummary() {
        _lastCompletedWorkoutSummary.value = null
    }

    private val _currentInAppNotification = MutableStateFlow<InAppNotification?>(null)
    val currentInAppNotification = _currentInAppNotification.asStateFlow()

    fun postInAppNotification(title: String, message: String, iconType: String = "info") {
        _currentInAppNotification.value = InAppNotification(title, message, iconType)
    }

    fun dismissInAppNotification() {
        _currentInAppNotification.value = null
    }

    // Timer state
    private val _restTimerSeconds = MutableStateFlow(0)
    val restTimerSeconds = _restTimerSeconds.asStateFlow()
    private var timerJob: Job? = null

    private val _completedSets = MutableStateFlow<Set<String>>(emptySet())
    val completedSets = _completedSets.asStateFlow()

    private val _customRestInterval = MutableStateFlow(90)
    val customRestInterval = _customRestInterval.asStateFlow()

    fun setCustomRestInterval(seconds: Int) {
        _customRestInterval.value = seconds
    }

    fun toggleSetCompleted(exerciseId: String, setNumber: Int) {
        val key = "${exerciseId}_$setNumber"
        val current = _completedSets.value.toMutableSet()
        if (current.contains(key)) {
            current.remove(key)
        } else {
            current.add(key)
            logFinishedSet()
        }
        _completedSets.value = current
    }

    fun getLogsForSession(sessionId: Long): kotlinx.coroutines.flow.Flow<List<com.example.data.model.ExerciseLog>> {
        return repository.getLogsForSession(sessionId)
    }

    // Gemini API coach thread
    private val _chatHistory = MutableStateFlow<List<ChatMessage>>(listOf(
        ChatMessage("assistant", "Greetings gym warrior! I am OmniFit Advisor. Tell me about your fitness goals or log some statistics so we can dial in your custom macro goals!")
    ))
    val chatHistory = _chatHistory.asStateFlow()

    private val _isChatLoading = MutableStateFlow(false)
    val isChatLoading = _isChatLoading.asStateFlow()

    // Dynamic stats trackers
    private val _todayHealthLog = MutableStateFlow<HealthTrackerLog>(
        HealthTrackerLog(
            id = "${repository.getTodayDateKey()}:guest@omnifit.com",
            dateKey = repository.getTodayDateKey(),
            userEmail = "guest@omnifit.com"
        )
    )
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
            _loggedInEmail.collect { email ->
                repository.getUserProfile(email.ifBlank { "guest@omnifit.com" }).collect { profile ->
                    _isProfileLoaded.value = true
                    if (profile != null) {
                        viewModelScope.launch {
                            val scheds = repository.schedules.first()
                            val allEmpty = scheds.isEmpty() || scheds.all { it.isRestDay || it.exerciseIds.isEmpty() }
                            if (allEmpty) {
                                repository.generateWorkoutSchedule(profile)
                            }
                        }
                    }
                }
            }
        }
        viewModelScope.launch {
            _loggedInEmail.collect { email ->
                // Monitor today's health logs for current user
                repository.getHealthLogForDate(repository.getTodayDateKey(), email.ifBlank { "guest@omnifit.com" }).collect { log ->
                    if (log != null) {
                        _todayHealthLog.value = log
                    } else {
                        val currentEmail = email.ifBlank { "guest@omnifit.com" }
                        _todayHealthLog.value = HealthTrackerLog(
                            id = "${repository.getTodayDateKey()}:$currentEmail",
                            dateKey = repository.getTodayDateKey(),
                            userEmail = currentEmail
                        )
                    }
                }
            }
        }
        viewModelScope.launch {
            // Recalculate fatigue based on exercise sessions on load
            calculateMuscleFatigue()
            // Analyze strength plateaus on load
            analyzePlateaus()
        }
        loadSmartDevices()

        // Periodic Bluetooth real-time telemetry streaming simulation loop (Active Wearable Data Gathering)
        viewModelScope.launch {
            while (true) {
                delay(3500) // update every 3.5 seconds to represent an active stream!
                if (_deviceSyncOverride.value) {
                    val activeDevices = _smartDevices.value.filter { it.isConnected }
                    if (activeDevices.isNotEmpty()) {
                        val currentLog = _todayHealthLog.value
                        
                        // Increment metrics slightly over time to show active wearable sensor / GATT push notifications
                        var stepDelta = 0
                        var calorieDelta = 0.0
                        
                        activeDevices.forEach { dev ->
                            when (dev.type) {
                                "Watch" -> {
                                    stepDelta += (3..9).random()
                                    calorieDelta += (15..45).random() / 100.0
                                    _liveHeartRate.value = (70..130).random()
                                }
                                "Ring" -> {
                                    stepDelta += (1..4).random()
                                    _liveHeartRate.value = (64..105).random()
                                }
                                "Band" -> {
                                    stepDelta += (2..7).random()
                                    calorieDelta += (10..35).random() / 100.0
                                    _liveHeartRate.value = (68..122).random()
                                }
                                "Chest Strap" -> {
                                    calorieDelta += (4..9).random() / 10.0
                                    _liveHeartRate.value = (100..160).random()
                                }
                                else -> {
                                    stepDelta += (1..3).random()
                                    calorieDelta += (5..20).random() / 100.0
                                    _liveHeartRate.value = (72..115).random()
                                }
                            }
                        }
                        
                        if (stepDelta > 0 || calorieDelta > 0.0) {
                            val updatedLog = currentLog.copy(
                                stepsCount = currentLog.stepsCount + stepDelta,
                                activeCaloriesBurned = currentLog.activeCaloriesBurned + calorieDelta
                            )
                            _todayHealthLog.value = updatedLog
                            try {
                                repository.saveHealthTrackerLog(updatedLog)
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }
                    } else {
                        _liveHeartRate.value = 0
                    }
                } else {
                    _liveHeartRate.value = 0
                }
            }
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
                    userEmail = loggedInEmail.value.ifBlank { "guest@omnifit.com" },
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
                    userEmail = loggedInEmail.value.ifBlank { "guest@omnifit.com" },
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
            val email = loggedInEmail.value.ifBlank { "guest@omnifit.com" }
            val existing = repository.getUserProfileDirect(email)
            val profile = UserProfile(
                email = email,
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

    fun updateProfileBiometrics(weight: Double, height: Double, experience: String) {
        viewModelScope.launch {
            val email = loggedInEmail.value.ifBlank { "guest@omnifit.com" }
            val existing = repository.getUserProfileDirect(email) ?: UserProfile(email = email)
            val updated = existing.copy(
                weightKg = weight,
                heightCm = height,
                experienceLevel = experience
            )
            repository.saveUserProfile(updated)
        }
    }

    fun updateProfileFull(
        name: String,
        photoUri: String?,
        weight: Double,
        height: Double,
        experience: String,
        age: Int,
        bodyFat: Double,
        chest: Double,
        arms: Double,
        waist: Double,
        thighs: Double,
        primaryGoal: String,
        gender: String,
        equipment: String,
        availableDaysPerWeek: Int,
        maxTimeMinutes: Int,
        workoutPreferenceNotes: String
    ) {
        viewModelScope.launch {
            if (name.isNotBlank()) {
                _loggedInName.value = name
                prefs.edit().putString("logged_in_name", name).apply()
            }
            val email = loggedInEmail.value.ifBlank { "guest@omnifit.com" }
            val existing = repository.getUserProfileDirect(email) ?: UserProfile(email = email)
            val updated = existing.copy(
                weightKg = weight,
                heightCm = height,
                experienceLevel = experience,
                age = age,
                bodyFatPercentage = bodyFat,
                chestCm = chest,
                armsCm = arms,
                waistCm = waist,
                thighsCm = thighs,
                primaryGoal = primaryGoal,
                gender = gender,
                equipmentInventory = equipment,
                photoUri = photoUri,
                availableDaysPerWeek = availableDaysPerWeek,
                maxTimeMinutes = maxTimeMinutes,
                workoutPreferenceNotes = workoutPreferenceNotes
            )
            repository.saveUserProfile(updated)
            
            // Sync local onboarding states in viewmodel
            onboardingDays.value = availableDaysPerWeek
            onboardingTimeMax.value = maxTimeMinutes

            // If training variables change, regenerate AI workout schedule to align with new objectives!
            if (existing.primaryGoal != primaryGoal || 
                existing.experienceLevel != experience || 
                existing.equipmentInventory != equipment ||
                existing.availableDaysPerWeek != availableDaysPerWeek ||
                existing.maxTimeMinutes != maxTimeMinutes
            ) {
                repository.generateWorkoutSchedule(updated)
            }
        }
    }

    fun updateTrainingTuner(days: Int, duration: Int, experience: String, gender: String) {
        viewModelScope.launch {
            val email = loggedInEmail.value.ifBlank { "guest@omnifit.com" }
            val existing = repository.getUserProfileDirect(email) ?: UserProfile(email = email)
            val finalTimestamp = if (experience.equals("Beginner", ignoreCase = true)) {
                System.currentTimeMillis()
            } else {
                System.currentTimeMillis() - (40L * 24 * 60 * 60 * 1000)
            }
            
            val updated = existing.copy(
                availableDaysPerWeek = days,
                maxTimeMinutes = duration,
                experienceLevel = experience,
                onboardingTimestamp = finalTimestamp,
                gender = gender
            )
            repository.saveUserProfile(updated)
            repository.generateWorkoutSchedule(updated)
            
            onboardingDays.value = days
            onboardingTimeMax.value = duration
            onboardingExperienceLevel.value = experience
            onboardingGender.value = gender
        }
    }

    // --- Live active workout ---
    fun startActiveWorkout(schedule: WorkoutSchedule) {
        viewModelScope.launch {
            _uiState.value = FitnessUiState.Loading
            _completedSets.value = emptySet() // Reset completed sets
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
        // Trigger rest countdown using custom selected interval seconds
        timerJob?.cancel()
        _restTimerSeconds.value = _customRestInterval.value
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

            val durationMs = System.currentTimeMillis() - sessionId
            val durationSecs = if (durationMs > 1000) durationMs / 1000 else 2400L // 40 minutes fallback if instantaneous

            val completedSets = _exerciseLogsInProgress.value.values.flatten().filter { it.repsCompleted > 0 }
            val totalSets = completedSets.size
            val totalReps = completedSets.sumOf { it.repsCompleted }
            val totalVolume = completedSets.sumOf { it.weightKg * it.repsCompleted }

            // Calculate dynamic, high-fidelity calories
            val minutes = durationSecs / 60.0
            val calculatedCalories = (minutes * 6.5) + (totalSets * 8.5)
            val finalCalories = if (calculatedCalories > 5.0) calculatedCalories else 150.0 // guarantee reasonable burn for visual sanity

            // Save central session
            val sessionKey = repository.saveWorkoutSession(
                WorkoutSession(
                    userEmail = loggedInEmail.value.ifBlank { "guest@omnifit.com" },
                    workoutName = workoutName,
                    durationSeconds = durationSecs,
                    caloriesBurned = finalCalories
                )
            )

            // Save detailed logs
            completedSets.forEach { log ->
                repository.saveExerciseLog(log.copy(sessionId = sessionKey))
            }

            // Save daily active calories burned
            repository.addActiveCalories(loggedInEmail.value.ifBlank { "guest@omnifit.com" }, finalCalories)

            // Find completed exercise details and muscle groups
            val exercisesPerformed = completedSets.map { it.exerciseName }.distinct()
            val muscleGroups = _activeWorkoutExercises.value
                .filter { ex -> _exerciseLogsInProgress.value[ex.id]?.any { it.repsCompleted > 0 } == true }
                .map { it.targetMuscleGroup }
                .distinct()

            val p = repository.getUserProfileDirect(loggedInEmail.value.ifBlank { "guest@omnifit.com" })
            val isBegin = p != null && p.experienceLevel.equals("Beginner", ignoreCase = true)

            // Capture summary state
            _lastCompletedWorkoutSummary.value = WorkoutSummary(
                id = sessionKey,
                name = workoutName,
                durationSeconds = durationSecs,
                caloriesBurned = finalCalories,
                totalVolumeKg = totalVolume,
                totalSets = totalSets,
                totalReps = totalReps,
                exercisesPerformed = exercisesPerformed,
                muscleGroupsWorked = muscleGroups,
                isPeriodMode = _isOnPeriod.value,
                isBeginnerPeriod = isBegin
            )

            // Trigger beautiful in-app completion notification
            postInAppNotification(
                title = "💪 Session Accomplished!",
                message = "Successfully logged session '${workoutName}'. Burned ${String.format(java.util.Locale.US, "%.0f", finalCalories)} kcal across ${totalSets} sets! Slide right to see stats.",
                iconType = "achievement"
            )

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

    fun discardCurrentWorkout() {
        _activeSessionId.value = null
        _currentActiveWorkoutName.value = null
        _activeWorkoutExercises.value = emptyList()
        _exerciseLogsInProgress.value = emptyMap()
        stopTimer()
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
        val sessionsInLast48 = allSessions.first()
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
        for (session in sessionsInLast48.take(5)) {
            val logs = repository.getLogsForSession(session.id).first()
            for (log in logs) {
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
                    repository.incrementSteps(loggedInEmail.value.ifBlank { "guest@omnifit.com" }, stepsToCommit)
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

        val milestoneMatched = (updatedSteps > 0) && (
            (currentLog.stepsCount < 50 && updatedSteps >= 50) ||
            (currentLog.stepsCount < 500 && updatedSteps >= 500) ||
            (currentLog.stepsCount < 2500 && updatedSteps >= 2500) ||
            (currentLog.stepsCount < 5000 && updatedSteps >= 5000) ||
            ((currentLog.stepsCount / 1000) < (updatedSteps / 1000))
        )

        if (milestoneMatched) {
            postInAppNotification(
                title = "🏃 Step Milestone Unlocked!",
                message = "Awesome progress! Today you have reached ${updatedSteps} steps, burning ${String.format(java.util.Locale.US, "%.1f", currentLog.activeCaloriesBurned + addedCalories)} kcal. Slide right to view details!",
                iconType = "steps"
            )
        }

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

        postInAppNotification(
            title = "⚡ Cardio Run Engaged!",
            message = "Your high-freq run sensor metrics are now active. Slide right to monitor real-time tracking stats!",
            iconType = "run"
        )

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
        
        val distStr = if (_runDistanceMeters.value >= 1000.0) {
            String.format(java.util.Locale.US, "%.2f km", distKm)
        } else {
            String.format(java.util.Locale.US, "%.0f m", _runDistanceMeters.value)
        }

        postInAppNotification(
            title = "🏁 Live Run Completed!",
            message = "Brilliant session! You logged ${distStr} and burned ${String.format(java.util.Locale.US, "%.1f", runningCalories)} kcal. Slide right to view dashboard logs!",
            iconType = "achievement"
        )

        if (runningCalories > 0.5) {
            viewModelScope.launch {
                repository.addActiveCalories(loggedInEmail.value.ifBlank { "guest@omnifit.com" }, runningCalories)
                repository.incrementSteps(loggedInEmail.value.ifBlank { "guest@omnifit.com" }, _runSteps.value)
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
                    repository.incrementSteps(loggedInEmail.value.ifBlank { "guest@omnifit.com" }, stepsToCommit)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    fun recordWaterIntake(ml: Int) {
        val currentLog = _todayHealthLog.value
        _todayHealthLog.value = currentLog.copy(
            waterIntakeMl = currentLog.waterIntakeMl + ml
        )
        viewModelScope.launch {
            try {
                repository.addWaterIntake(loggedInEmail.value.ifBlank { "guest@omnifit.com" }, ml)
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
            val userProf = repository.getUserProfileDirect(loggedInEmail.value.ifBlank { "guest@omnifit.com" })
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

        val isBeginnerPeriod = p.experienceLevel.equals("Beginner", ignoreCase = true)

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

        val languageName = when (_appLanguage.value) {
            "es" -> "Spanish (Español)"
            "fr" -> "French (Français)"
            "de" -> "German (Deutsch)"
            else -> "English"
        }

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
                        GeminiPart("You are the OmniFit AI Coach, specialized exclusively in diet, food, nutrition, health, fitness, workout training, and physical tracking. You have deep and perfect understanding of the user's input/metrics and stats.\n\nCRITICAL SCOPE RULE: You must ONLY answer questions directly related to diet, food, health, fitness, macros, training, or physical vitals. If the user asks about anything else outside of health/fitness/diet (e.g., coding, general science, math, history, non-fitness trivia, politics, generic non-fitness chit-chat), you must politely but firmly decline to answer and guide them back, stating that as your OmniFit AI Coach, you can only assist with health, fitness, exercise, diet, and nutrition questions.\n\nCRITICAL FORMATTING RULE: You MUST format your entire response using ONLY clean, concise bullet points or numbered lists. Every single point must be punchy, highly structured, and direct. Do NOT write blocks of paragraphs. Customize your response specifically based on the user's metrics (e.g., weight, goals, active logs) provided in the prompt.\n\nCRITICAL LANGUAGE RULE: You MUST speak and write your ENTIRE response natively in " + languageName + ". Explanations, bullet points, guidance, lists, numbers, and motivational tips must be completely written in this language.")
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
