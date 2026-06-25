package com.example

import android.app.Application
import androidx.room.Room
import com.example.data.db.AppDatabase
import com.example.data.repository.FitnessRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class OmniFitApplication : Application() {

    val database: AppDatabase by lazy {
        Room.databaseBuilder(this, AppDatabase::class.java, "omnifit_db")
            .fallbackToDestructiveMigration()
            .build()
    }

    val repository: FitnessRepository by lazy {
        FitnessRepository(database)
    }

    override fun onCreate() {
        super.onCreate()
        val appScope = CoroutineScope(SupervisorJob())
        appScope.launch {
            repository.seedInitialExercisesIfEmpty()
        }
    }
}
