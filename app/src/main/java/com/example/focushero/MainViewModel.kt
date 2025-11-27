package com.example.focushero

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.focushero.data.Mood
import com.example.focushero.data.Session
import com.example.focushero.data.SessionRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = SessionRepository(application)
    
    // Navigation State
    var currentDestination by mutableStateOf(AppDestinations.HOME)

    // User Stats
    var points by mutableIntStateOf(repository.getPoints())
    var streak by mutableIntStateOf(repository.getStreak())

    // Timer & Session State
    var isRunning by mutableStateOf(false)
    var isPaused by mutableStateOf(false)
    var timeLeft by mutableLongStateOf(0L)
    var totalTime by mutableLongStateOf(0L)
    var selectedMood by mutableStateOf(Mood.NEUTRAL)
    var subject by mutableStateOf("")
    var showSummary by mutableStateOf(false)
    var lastSessionPoints by mutableIntStateOf(0)

    // Internal Timer Logic
    private var timerJob: Job? = null
    private var endTime: Long = 0L

    fun startTimer() {
        if (isRunning && !isPaused) return

        if (!isPaused) {
            // New Session
            totalTime = (selectedMood.focusMinutes * 60).toLong()
            timeLeft = totalTime
        }
        
        // Calculate the absolute time when the timer should end
        // This prevents drift and handles backgrounding correctly
        endTime = System.currentTimeMillis() + (timeLeft * 1000)
        
        isRunning = true
        isPaused = false
        
        startTicker()
    }

    fun pauseTimer() {
        isPaused = true
        timerJob?.cancel()
    }

    fun stopTimer() {
        isRunning = false
        isPaused = false
        timeLeft = 0
        timerJob?.cancel()
    }
    
    fun updateSubject(newSubject: String) {
        subject = newSubject
    }
    
    fun updateMood(newMood: Mood) {
        selectedMood = newMood
    }
    
    fun dismissSummary() {
        showSummary = false
    }

    private fun startTicker() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (isRunning && !isPaused) {
                val remainingMillis = endTime - System.currentTimeMillis()
                if (remainingMillis <= 0) {
                    timeLeft = 0
                    finishSession()
                    break
                } else {
                    timeLeft = remainingMillis / 1000
                }
                delay(200) // Check more frequently for smoothness
            }
        }
    }

    private fun finishSession() {
        isRunning = false
        isPaused = false
        
        val durationMinutes = totalTime / 60
        // Formula: 1 point per 5 minutes
        val earned = (durationMinutes / 5).toInt()
        
        val session = Session(
            subject = subject.ifBlank { "Study" },
            mood = selectedMood,
            durationSeconds = totalTime.toInt(),
            pointsEarned = earned
        )
        
        repository.saveSession(session)
        
        // Refresh stats
        points = repository.getPoints()
        streak = repository.getStreak()
        
        lastSessionPoints = earned
        showSummary = true
    }
}
