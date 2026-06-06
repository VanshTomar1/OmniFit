package com.example.data.repository

import com.example.data.db.AppDatabase
import com.example.data.model.UserProfile
import com.example.data.model.WorkoutSchedule
import com.example.data.model.Exercise
import com.example.data.model.WorkoutSession
import com.example.data.model.ExerciseLog
import com.example.data.model.HealthTrackerLog
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class FitnessRepository(private val db: AppDatabase) {

    val userProfile: Flow<UserProfile?> = db.userProfileDao().getUserProfile()
    val schedules: Flow<List<WorkoutSchedule>> = db.workoutScheduleDao().getSchedules()
    val allExercises: Flow<List<Exercise>> = db.exerciseDao().getAllExercises()
    val allSessions: Flow<List<WorkoutSession>> = db.workoutSessionDao().getAllSessions()
    val weeklyHealthLogs: Flow<List<HealthTrackerLog>> = db.healthTrackerLogDao().getWeeklyLogs()

    suspend fun getUserProfileDirect(): UserProfile? = db.userProfileDao().getUserProfileDirect()

    suspend fun saveUserProfile(profile: UserProfile) {
        db.userProfileDao().insertUserProfile(profile)
    }

    suspend fun getExerciseById(id: String): Exercise? {
        return db.exerciseDao().getExerciseById(id)
    }

    suspend fun saveExercise(exercise: Exercise) {
        db.exerciseDao().insertExercises(listOf(exercise))
    }

    fun getLogsForSession(sessionId: Long): Flow<List<ExerciseLog>> {
        return db.exerciseLogDao().getLogsForSession(sessionId)
    }

    fun getLogsForExercise(exerciseId: String): Flow<List<ExerciseLog>> {
        return db.exerciseLogDao().getLogsForExercise(exerciseId)
    }

    suspend fun saveWorkoutSession(session: WorkoutSession): Long {
        return db.workoutSessionDao().insertSession(session)
    }

    suspend fun saveExerciseLog(log: ExerciseLog) {
        db.exerciseLogDao().insertExerciseLog(log)
    }

    fun getHealthLogForDate(dateKey: String): Flow<HealthTrackerLog?> {
        return db.healthTrackerLogDao().getLogForDate(dateKey)
    }

    suspend fun saveHealthTrackerLog(log: HealthTrackerLog) {
        db.healthTrackerLogDao().insertHealthTrackerLog(log)
    }

    fun getTodayDateKey(): String {
        return SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
    }

    private val stepMutex = Mutex()

    suspend fun incrementSteps(amount: Int) {
        stepMutex.withLock {
            withContext(Dispatchers.IO) {
                val today = getTodayDateKey()
                val current = db.healthTrackerLogDao().getLogForDateDirect(today) ?: HealthTrackerLog(today)
                val newSteps = current.stepsCount + amount
                val addedCalories = amount * 0.04
                db.healthTrackerLogDao().insertHealthTrackerLog(
                    current.copy(
                        stepsCount = newSteps,
                        activeCaloriesBurned = current.activeCaloriesBurned + addedCalories
                    )
                )
            }
        }
    }

    suspend fun addWaterIntake(amountMl: Int) {
        val today = getTodayDateKey()
        val current = db.healthTrackerLogDao().getLogForDateDirect(today) ?: HealthTrackerLog(today)
        db.healthTrackerLogDao().insertHealthTrackerLog(
            current.copy(waterIntakeMl = current.waterIntakeMl + amountMl)
        )
    }

    suspend fun addActiveCalories(calories: Double) {
        val today = getTodayDateKey()
        val current = db.healthTrackerLogDao().getLogForDateDirect(today) ?: HealthTrackerLog(today)
        db.healthTrackerLogDao().insertHealthTrackerLog(
            current.copy(activeCaloriesBurned = current.activeCaloriesBurned + calories)
        )
    }

    // --- SEEDING INITIAL EXERCISES ---
    suspend fun seedInitialExercisesIfEmpty() {
        val existing = db.exerciseDao().getAllExercises().firstOrNull() ?: emptyList()
        if (existing.isNotEmpty()) return

        val seedList = listOf(
            // Chest
            Exercise("ex_pushup", "Regular Push-up", "Chest", "Beginner", "Bodyweight", "ex_knee_pushup,ex_bench_press", "Keep elbows at 45 degrees, lower chest to the floor."),
            Exercise("ex_knee_pushup", "Knee Push-up", "Chest", "Beginner", "Bodyweight", "ex_pushup", "Perfect for building core and chest strength before advanced moves."),
            Exercise("ex_bench_press", "Barbell Bench Press", "Chest", "Intermediate", "Full Gym", "ex_pushup,ex_dumbbell_press", "Lower slow, touch mid-chest, drive weight up."),
            Exercise("ex_dumbbell_press", "Dumbbell Bench Press", "Chest", "Intermediate", "Dumbbells", "ex_bench_press,ex_pushup", "Lie flat on a bench, press dumbbells up steadily."),
            Exercise("ex_decline_pushup", "Decline Push-up", "Chest", "Advanced", "Bodyweight", "ex_pushup", "Elevate feet on chair/bench to load upper clavicular chest fibers."),
            Exercise("ex_chest_fly", "Dumbbell Chest Fly", "Chest", "Intermediate", "Dumbbells", "ex_dumbbell_press", "Stretch wide, hug a large tree on the way back."),
            Exercise("ex_diamond_pushup", "Diamond Push-up", "Chest", "Intermediate", "Bodyweight", "ex_pushup,ex_chair_dips", "Place hands close together in a diamond shape to target inner chest and triceps."),

            // Back
            Exercise("ex_pullup", "Standard Pull-up", "Back", "Advanced", "Bodyweight", "ex_chin_up,ex_lat_pulldown", "Engage lats, pull chest up, avoid excessive swinging."),
            Exercise("ex_chin_up", "Chin-up", "Back", "Intermediate", "Bodyweight", "ex_pullup", "Underhand grip pulls. Uses more bicep."),
            Exercise("ex_lat_pulldown", "Lat Pulldown Machine", "Back", "Beginner", "Full Gym", "ex_pullup,ex_db_row", "Pull bar cleanly towards upper chest."),
            Exercise("ex_db_row", "Dumbbell Row", "Back", "Beginner", "Dumbbells", "ex_lat_pulldown,ex_doorframe_rows", "Pull elbow past ribs, keep back flat."),
            Exercise("ex_doorframe_rows", "Doorframe Bodyweight Row", "Back", "Beginner", "Bodyweight", "ex_db_row", "Stand in a doorframe, hold the frame, lean back, and pull your upper body forward using lats."),
            Exercise("ex_floor_pulls", "Floor Belly Pulldown", "Back", "Beginner", "Bodyweight", "ex_doorframe_rows", "Lie on your belly, lift chest slightly, and guide elbows back to squeeze shoulder blades."),

            // Legs
            Exercise("ex_goblet_squat", "Dumbbell Goblet Squat", "Quads", "Beginner", "Dumbbells", "ex_air_squat,ex_barbell_squat", "Hold db upright at chest height, break parallel with thighs."),
            Exercise("ex_air_squat", "Bodyweight Air Squat", "Quads", "Beginner", "Bodyweight", "ex_goblet_squat", "Perfect form builder. Deep knee bend, feet flat."),
            Exercise("ex_barbell_squat", "Barbell Back Squat", "Quads", "Intermediate", "Full Gym", "ex_leg_press", "Full muscle loading squat with safety racks."),
            Exercise("ex_leg_press", "Leg Press Machine", "Quads", "Beginner", "Full Gym", "ex_barbell_squat", "Excellent quad loading without spine compression."),
            Exercise("ex_hamstring_curl", "Hamstring Curl (Machine/Bands)", "Hamstrings", "Beginner", "Full Gym", "ex_deadlift", "Flex knees, squeeze hammies tight."),
            Exercise("ex_deadlift", "Barbell Deadlift", "Hamstrings", "Intermediate", "Full Gym", "ex_db_deadlift", "Maintain neutral spine, hinge from hips, pull from heels."),
            Exercise("ex_db_deadlift", "Dumbbell Romanian Deadlift", "Hamstrings", "Intermediate", "Dumbbells", "ex_deadlift", "Feel stretch in hamstrings, drive hips forward."),
            Exercise("ex_glute_bridge", "Single-Leg Glute Bridge", "Hamstrings", "Beginner", "Bodyweight", "ex_db_deadlift", "Lie flat on back, elevate one leg, and raise hips high driving through the heel."),
            Exercise("ex_calf_raises", "Bodyweight Standing Calf Raise", "Hamstrings", "Beginner", "Bodyweight", "ex_glute_bridge", "Piston upwards on flat feet or steps to target gastrocnemius muscles."),

            // Shoulders & Arms
            Exercise("ex_overhead_press", "Dumbbell Shoulder Press", "Shoulders", "Intermediate", "Dumbbells", "ex_lateral_raise,ex_pike_pushup", "Press from shoulders straight overhead."),
            Exercise("ex_lateral_raise", "Dumbbell Lateral Raise", "Shoulders", "Beginner", "Dumbbells", "ex_overhead_press,ex_arm_circles", "Raise straight out to sides to form a 'T' pose."),
            Exercise("ex_pike_pushup", "Pike Push-up", "Shoulders", "Intermediate", "Bodyweight", "ex_overhead_press", "Bend at hips to form inverted V-shape, lower head strictly to ground to load anterior delts."),
            Exercise("ex_arm_circles", "Tension Arm Circles", "Shoulders", "Beginner", "Bodyweight", "ex_lateral_raise", "Hold arms horizontal, create high-torque circles under flexed isometric muscle tension."),
            
            Exercise("ex_bicep_curl", "Dumbbell Bicep Curl", "Arms", "Beginner", "Dumbbells", "ex_pullup,ex_doorframe_bicep_curls", "Isolate elbow, contract biceps upwards."),
            Exercise("ex_tricep_extension", "Overhead Tricep Extension", "Arms", "Beginner", "Dumbbells", "ex_pushup,ex_chair_dips", "Keep upper arms close to head, extend elbows fully."),
            Exercise("ex_chair_dips", "Couch Tricep Dips", "Arms", "Beginner", "Bodyweight", "ex_tricep_extension", "Place hands behind back on chair or bed, dip hips, and drive up through extensions."),
            Exercise("ex_doorframe_bicep_curls", "Doorframe Isometric Curl", "Arms", "Beginner", "Bodyweight", "ex_bicep_curl", "Hold doorframe underhand, lean back, and use bicep flexion torque to pull your weight in."),

            // Core
            Exercise("ex_plank", "Forearm Plank", "Core", "Beginner", "Bodyweight", "ex_hollow_body", "Hold straight horizontal pose. Squeeze glutes and abs."),
            Exercise("ex_hollow_body", "Hollow Body Hold", "Core", "Advanced", "Bodyweight", "ex_plank", "Keep lower back glued to the floor, legs & arms hovered."),

            // Female Targeted Seeding
            Exercise("ex_hip_thrust", "Dumbbell Hip Thrust", "Hamstrings", "Beginner", "Dumbbells", "ex_glute_bridge", "Lie on back with upper back on couch, place dumbbell on hips, squeeze glutes upwards.", "Female"),
            Exercise("ex_glute_kickback", "Band Glute Kickbacks", "Hamstrings", "Beginner", "Resistance Bands", "ex_glute_bridge", "Kick back against resistance, squeezing glutes tightly.", "Female"),
            Exercise("ex_fire_hydrants", "Bodyweight Fire Hydrant", "Core", "Beginner", "Bodyweight", "ex_plank", "On all fours, lift leg sideways to contract lateral glutes/hip openers.", "Female"),
            
            // Male Targeted Seeding
            Exercise("ex_barbell_shrugs", "Barbell Shrugs", "Shoulders", "Intermediate", "Full Gym", "ex_overhead_press", "Stand straight, elevate scapular trapezius squeezing upper back.", "Male"),
            Exercise("ex_military_press", "Barbell Military Press", "Shoulders", "Advanced", "Full Gym", "ex_overhead_press", "Strict standing overhead drive with heavy barbell compound power.", "Male"),
            Exercise("ex_incline_press", "Incline Barbell Bench Press", "Chest", "Intermediate", "Full Gym", "ex_bench_press", "Lie on incline bench, lower to upper chest clavicular fibers, press up.", "Male")
        )
        db.exerciseDao().insertExercises(seedList)
    }

    // --- WORKOUT SPLIT GENERATOR ENGINE ---
    suspend fun generateWorkoutSchedule(profile: UserProfile) {
        db.workoutScheduleDao().clearAllSchedules()

        val equip = profile.equipmentInventory
        val userGender = profile.gender
        val isBeginnerPeriod = if (profile.experienceLevel.equals("Beginner", ignoreCase = true)) {
            val daysPassed = (System.currentTimeMillis() - profile.onboardingTimestamp) / (24 * 60 * 60 * 1000)
            daysPassed < 30
        } else {
            false
        }

        val level = if (isBeginnerPeriod) "Beginner" else profile.experienceLevel

        // Get matching exercises
        val allAvailable = db.exerciseDao().getAllExercises().firstOrNull() ?: emptyList()
        val filtered = allAvailable.filter { ex ->
            val matchEquip = when (equip) {
                "Bodyweight/Calisthenics" -> ex.requiredEquipment == "Bodyweight"
                "Resistance Bands" -> ex.requiredEquipment == "Bodyweight" || ex.requiredEquipment == "Resistance Bands"
                "Dumbbells Only" -> ex.requiredEquipment == "Dumbbells" || ex.requiredEquipment == "Bodyweight"
                else -> true // Full gym gets everything
            }
            val matchGender = ex.genderTarget.equals("Universal", ignoreCase = true) || ex.genderTarget.equals(userGender, ignoreCase = true)
            matchEquip && matchGender
        }

        fun getExsByMuscleAndLevel(muscleGroup: String, count: Int): List<String> {
            val muscleExs = filtered.filter { it.targetMuscleGroup.equals(muscleGroup, ignoreCase = true) }
            val preferred = if (isBeginnerPeriod) {
                muscleExs.filter { it.difficultyLevel.equals("Beginner", ignoreCase = true) }
            } else {
                muscleExs.filter { it.difficultyLevel.equals(level, ignoreCase = true) }
            }
            val finalPool = if (preferred.isNotEmpty()) preferred else muscleExs
            return finalPool.take(count).map { it.id }
        }

        val daysCount = profile.availableDaysPerWeek
        val splits = when (daysCount) {
            2 -> listOf(
                WorkoutSchedule(dayOfWeek = 2, isRestDay = false, workoutName = "Full Body Focus (A)", exerciseIds = (getExsByMuscleAndLevel("Chest", 1) + getExsByMuscleAndLevel("Back", 1) + getExsByMuscleAndLevel("Quads", 1) + getExsByMuscleAndLevel("Core", 1)).joinToString(",")),
                WorkoutSchedule(dayOfWeek = 4, isRestDay = true, workoutName = "Active Rest", exerciseIds = ""),
                WorkoutSchedule(dayOfWeek = 5, isRestDay = false, workoutName = "Full Body Focus (B)", exerciseIds = (getExsByMuscleAndLevel("Chest", 1) + getExsByMuscleAndLevel("Back", 2) + getExsByMuscleAndLevel("Hamstrings", 1) + getExsByMuscleAndLevel("Shoulders", 1)).joinToString(","))
            )
            3 -> listOf(
                WorkoutSchedule(dayOfWeek = 1, isRestDay = false, workoutName = "Push Day (Chest, Shoulders)", exerciseIds = (getExsByMuscleAndLevel("Chest", 2) + getExsByMuscleAndLevel("Shoulders", 2) + getExsByMuscleAndLevel("Arms", 1)).joinToString(",")),
                WorkoutSchedule(dayOfWeek = 3, isRestDay = false, workoutName = "Pull Day (Back, Arms)", exerciseIds = (getExsByMuscleAndLevel("Back", 2) + getExsByMuscleAndLevel("Arms", 2) + getExsByMuscleAndLevel("Core", 1)).joinToString(",")),
                WorkoutSchedule(dayOfWeek = 5, isRestDay = false, workoutName = "Leg Day (Quads, Hamstrings)", exerciseIds = (getExsByMuscleAndLevel("Quads", 2) + getExsByMuscleAndLevel("Hamstrings", 2)).joinToString(","))
            )
            4 -> listOf(
                WorkoutSchedule(dayOfWeek = 1, isRestDay = false, workoutName = "Upper Body Strength", exerciseIds = (getExsByMuscleAndLevel("Chest", 2) + getExsByMuscleAndLevel("Back", 2) + getExsByMuscleAndLevel("Shoulders", 1)).joinToString(",")),
                WorkoutSchedule(dayOfWeek = 2, isRestDay = false, workoutName = "Lower Body Strength", exerciseIds = (getExsByMuscleAndLevel("Quads", 2) + getExsByMuscleAndLevel("Hamstrings", 2)).joinToString(",")),
                WorkoutSchedule(dayOfWeek = 4, isRestDay = false, workoutName = "Upper Focus (Pump & Core)", exerciseIds = (getExsByMuscleAndLevel("Chest", 1) + getExsByMuscleAndLevel("Back", 1) + getExsByMuscleAndLevel("Arms", 2) + getExsByMuscleAndLevel("Core", 1)).joinToString(",")),
                WorkoutSchedule(dayOfWeek = 5, isRestDay = false, workoutName = "Lower Focus (Endurance & Stretch)", exerciseIds = (getExsByMuscleAndLevel("Quads", 1) + getExsByMuscleAndLevel("Hamstrings", 1) + getExsByMuscleAndLevel("Core", 2)).joinToString(","))
            )
            else -> listOf( // 5+ days default PPL-Upper-Lower layout
                WorkoutSchedule(dayOfWeek = 1, isRestDay = false, workoutName = "Push Max", exerciseIds = (getExsByMuscleAndLevel("Chest", 2) + getExsByMuscleAndLevel("Shoulders", 2) + getExsByMuscleAndLevel("Arms", 1)).joinToString(",")),
                WorkoutSchedule(dayOfWeek = 2, isRestDay = false, workoutName = "Pull Max", exerciseIds = (getExsByMuscleAndLevel("Back", 2) + getExsByMuscleAndLevel("Arms", 2) + getExsByMuscleAndLevel("Core", 1)).joinToString(",")),
                WorkoutSchedule(dayOfWeek = 3, isRestDay = false, workoutName = "Legs Hypertrophy", exerciseIds = (getExsByMuscleAndLevel("Quads", 2) + getExsByMuscleAndLevel("Hamstrings", 2)).joinToString(",")),
                WorkoutSchedule(dayOfWeek = 5, isRestDay = false, workoutName = "Upper Burnout", exerciseIds = (getExsByMuscleAndLevel("Chest", 2) + getExsByMuscleAndLevel("Back", 2) + getExsByMuscleAndLevel("Arms", 1)).joinToString(",")),
                WorkoutSchedule(dayOfWeek = 6, isRestDay = false, workoutName = "Core & HIIT Conditioning", exerciseIds = (getExsByMuscleAndLevel("Core", 2) + getExsByMuscleAndLevel("Quads", 1) + getExsByMuscleAndLevel("Shoulders", 1)).joinToString(","))
            )
        }

        // Fill remaining 7 days as rest days if necessary
        val finalScheduleList = mutableListOf<WorkoutSchedule>()
        for (day in 1..7) {
            val assigned = splits.find { it.dayOfWeek == day }
            if (assigned != null) {
                finalScheduleList.add(assigned)
            } else {
                finalScheduleList.add(
                    WorkoutSchedule(
                        dayOfWeek = day,
                        isRestDay = true,
                        workoutName = "Active Muscle Recovery",
                        exerciseIds = ""
                    )
                )
            }
        }
        db.workoutScheduleDao().insertSchedules(finalScheduleList)
    }

    suspend fun updateSchedule(schedule: WorkoutSchedule) {
        db.workoutScheduleDao().insertSchedules(listOf(schedule))
    }
}
