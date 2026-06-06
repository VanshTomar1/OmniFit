package com.example.data.db

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.RoomDatabase
import com.example.data.model.UserProfile
import com.example.data.model.WorkoutSchedule
import com.example.data.model.Exercise
import com.example.data.model.WorkoutSession
import com.example.data.model.ExerciseLog
import com.example.data.model.HealthTrackerLog
import kotlinx.coroutines.flow.Flow

@Dao
interface UserProfileDao {
    @Query("SELECT * FROM user_profile WHERE id = 1 LIMIT 1")
    fun getUserProfile(): Flow<UserProfile?>

    @Query("SELECT * FROM user_profile WHERE id = 1 LIMIT 1")
    suspend fun getUserProfileDirect(): UserProfile?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUserProfile(userProfile: UserProfile)
}

@Dao
interface WorkoutScheduleDao {
    @Query("SELECT * FROM workout_schedule ORDER BY dayOfWeek ASC")
    fun getSchedules(): Flow<List<WorkoutSchedule>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSchedules(schedules: List<WorkoutSchedule>)

    @Query("DELETE FROM workout_schedule")
    suspend fun clearAllSchedules()
}

@Dao
interface ExerciseDao {
    @Query("SELECT * FROM exercises")
    fun getAllExercises(): Flow<List<Exercise>>

    @Query("SELECT * FROM exercises WHERE id = :id LIMIT 1")
    suspend fun getExerciseById(id: String): Exercise?

    @Query("SELECT * FROM exercises WHERE targetMuscleGroup = :muscleGroup")
    fun getExercisesForMuscle(muscleGroup: String): Flow<List<Exercise>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExercises(exercises: List<Exercise>)
}

@Dao
interface WorkoutSessionDao {
    @Query("SELECT * FROM workout_sessions ORDER BY dateTimestamp DESC")
    fun getAllSessions(): Flow<List<WorkoutSession>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: WorkoutSession): Long

    @Query("DELETE FROM workout_sessions WHERE id = :id")
    suspend fun deleteSessionById(id: Long)
}

@Dao
interface ExerciseLogDao {
    @Query("SELECT * FROM exercise_logs WHERE sessionId = :sessionId ORDER BY id ASC")
    fun getLogsForSession(sessionId: Long): Flow<List<ExerciseLog>>

    @Query("SELECT * FROM exercise_logs WHERE exerciseId = :exerciseId ORDER BY id DESC")
    fun getLogsForExercise(exerciseId: String): Flow<List<ExerciseLog>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExerciseLog(exerciseLog: ExerciseLog)
}

@Dao
interface HealthTrackerLogDao {
    @Query("SELECT * FROM health_tracker_logs WHERE dateKey = :dateKey LIMIT 1")
    fun getLogForDate(dateKey: String): Flow<HealthTrackerLog?>

    @Query("SELECT * FROM health_tracker_logs WHERE dateKey = :dateKey LIMIT 1")
    suspend fun getLogForDateDirect(dateKey: String): HealthTrackerLog?

    @Query("SELECT * FROM health_tracker_logs ORDER BY dateKey DESC LIMIT 7")
    fun getWeeklyLogs(): Flow<List<HealthTrackerLog>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHealthTrackerLog(log: HealthTrackerLog)
}

@Database(
    entities = [
        UserProfile::class,
        WorkoutSchedule::class,
        Exercise::class,
        WorkoutSession::class,
        ExerciseLog::class,
        HealthTrackerLog::class
    ],
    version = 3,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userProfileDao(): UserProfileDao
    abstract fun workoutScheduleDao(): WorkoutScheduleDao
    abstract fun exerciseDao(): ExerciseDao
    abstract fun workoutSessionDao(): WorkoutSessionDao
    abstract fun exerciseLogDao(): ExerciseLogDao
    abstract fun healthTrackerLogDao(): HealthTrackerLogDao
}
