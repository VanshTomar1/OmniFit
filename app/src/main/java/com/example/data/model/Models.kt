package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_profile")
data class UserProfile(
    @PrimaryKey val id: Int = 1, // Singleton profile
    val weightKg: Double = 75.0,
    val heightCm: Double = 175.0,
    val age: Int = 28,
    val bodyFatPercentage: Double = 15.0,
    val chestCm: Double = 100.0,
    val armsCm: Double = 35.0,
    val waistCm: Double = 82.0,
    val thighsCm: Double = 55.0,
    val primaryGoal: String = "hypertrophy", // hypertrophy, strength, endurance, fat_loss, mobility
    val secondaryGoal: String = "mobility",
    val equipmentInventory: String = "Full Gym", // Comma-separated or full gym
    val availableDaysPerWeek: Int = 4,
    val maxTimeMinutes: Int = 45,
    val photoUri: String? = null, // For body fat estimation
    val experienceLevel: String = "Beginner", // Beginner, Intermediate, Advanced
    val onboardingTimestamp: Long = System.currentTimeMillis(),
    val gender: String = "Male" // Male, Female
)

@Entity(tableName = "workout_schedule")
data class WorkoutSchedule(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val dayOfWeek: Int, // 1 = Monday, 7 = Sunday
    val isRestDay: Boolean = false,
    val workoutName: String = "", // e.g., "Push Day"
    val exerciseIds: String = "" // Comma-separated exercise IDs
)

@Entity(tableName = "exercises")
data class Exercise(
    @PrimaryKey val id: String,
    val name: String,
    val targetMuscleGroup: String, // Chest, Back, Quads, Hamstrings, Shoulders, Arms, Core
    val difficultyLevel: String, // Beginner, Intermediate, Advanced
    val requiredEquipment: String, // Full Gym, Dumbbells, Resistance Bands, Bodyweight
    val alternativeIds: String = "", // Comma-separated alternative Exercise IDs
    val instructions: String = "",
    val genderTarget: String = "Universal" // Female, Male, Universal
)

@Entity(tableName = "workout_sessions")
data class WorkoutSession(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val dateTimestamp: Long = System.currentTimeMillis(),
    val workoutName: String,
    val durationSeconds: Long = 0,
    val caloriesBurned: Double = 0.0
)

@Entity(tableName = "exercise_logs")
data class ExerciseLog(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sessionId: Long,
    val exerciseId: String,
    val exerciseName: String,
    val setNumber: Int,
    val weightKg: Double,
    val repsCompleted: Int,
    val estimated1RM: Double = 0.0
)

@Entity(tableName = "health_tracker_logs")
data class HealthTrackerLog(
    @PrimaryKey val dateKey: String, // "yyyy-MM-dd"
    val stepsCount: Int = 0,
    val activeCaloriesBurned: Double = 0.0,
    val waterIntakeMl: Int = 0,
    val sleepMinutes: Int = 0
)
