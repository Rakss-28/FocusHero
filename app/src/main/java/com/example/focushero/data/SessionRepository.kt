package com.example.focushero.data

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class SessionRepository(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("focus_hero_prefs", Context.MODE_PRIVATE)
    private val gson = Gson()

    fun saveSession(session: Session) {
        val sessions = getSessions().toMutableList()
        sessions.add(session)
        val json = gson.toJson(sessions)
        prefs.edit().putString("sessions", json).apply()
        
        // Update points
        val currentPoints = getPoints()
        savePoints(currentPoints + session.pointsEarned)
        
        // Update streak
        updateStreak(session.timestamp)
    }

    fun getSessions(): List<Session> {
        val json = prefs.getString("sessions", null) ?: return emptyList()
        val type = object : TypeToken<List<Session>>() {}.type
        return gson.fromJson(json, type)
    }

    fun getPoints(): Int {
        return prefs.getInt("points", 0)
    }

    fun savePoints(points: Int) {
        prefs.edit().putInt("points", points).apply()
    }
    
    fun getStreak(): Int {
        return prefs.getInt("streak", 0)
    }

    private fun updateStreak(sessionTimestamp: Long) {
        val lastSessionTime = prefs.getLong("last_session_time", 0)
        val currentStreak = getStreak()
        
        // Simple streak logic: if last session was yesterday, increment. If today, do nothing. If older, reset.
        // For simplicity in this version, we just increment if it's a new day.
        // Real implementation would need proper day comparison.
        
        // Placeholder logic:
        prefs.edit().putInt("streak", currentStreak + 1).putLong("last_session_time", sessionTimestamp).apply()
    }
}
