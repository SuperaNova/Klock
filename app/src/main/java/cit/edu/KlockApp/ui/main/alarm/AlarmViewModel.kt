package cit.edu.KlockApp.ui.main.alarm

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.contextual
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

class AlarmViewModel(application: Application) : AndroidViewModel(application) {
    private val prefs: SharedPreferences =
        application.applicationContext.getSharedPreferences("alarms_prefs", Context.MODE_PRIVATE)
    private val json = Json {
        serializersModule = SerializersModule {
            contextual(LocalTimeSerializer)
        }
        prettyPrint = true
        ignoreUnknownKeys = true
    }
    private val ALARMS_KEY = "alarms_key"

    private val _alarms = MutableLiveData<List<Alarm>>().apply {
        value = loadAlarms()
    }
    val alarms: LiveData<List<Alarm>> = _alarms

    private fun loadAlarms(): List<Alarm> {
        val defaultAlarm = createDefaultAlarm()
        val stored = prefs.getString(ALARMS_KEY, null)

        // Only add the default alarm if no alarms are stored, and if the stored list is empty
        return if (stored.isNullOrEmpty()) {
            // If no alarms are stored, return an empty list instead of the default alarm
            emptyList()
        } else {
            try {
                val list = json.decodeFromString<List<Alarm>>(stored)
                if (list.isEmpty()) {
                    // Return an empty list if the stored list is empty, so no default alarm is added
                    emptyList()
                } else {
                    list
                }
            } catch (e: Exception) {
                Log.e("AlarmViewModel", "Parse error, using default: ${e.message}", e)
                // If there's a parsing error, return an empty list (no default alarm added)
                emptyList()
            }
        }
    }

    private fun saveAlarms(alarms: List<Alarm>) {
        try {
            val serialized = json.encodeToString(alarms)
            prefs.edit().putString(ALARMS_KEY, serialized).apply()
            Log.d("AlarmViewModel", "Saved alarms: $serialized")
        } catch (e: Exception) {
            Log.e("AlarmViewModel", "Save error: ${e.message}", e)
        }
    }

    private fun createDefaultAlarm(): Alarm {
        val formatter = DateTimeFormatter.ofPattern("hh:mm a", Locale.getDefault())
        val time = LocalTime.parse("07:00 AM", formatter)
        return Alarm(
            id = 1,
            time = time,
            isEnabled = true,
            repeatDays = emptyList(),
            label = "Wake Up"
        )
    }

    fun updateAlarm(updated: Alarm) {
        val current = _alarms.value.orEmpty().toMutableList()
        val idx = current.indexOfFirst { it.id == updated.id }

        // Only update if the alarm is actually modified (not identical)
        if (idx >= 0) {
            val oldAlarm = current[idx]
            if (oldAlarm != updated) {  // Only update if the alarm actually changed
                current[idx] = updated
                _alarms.value = current  // Update LiveData
                saveAlarms(current)      // Save to preferences
                Log.d("AlarmViewModel", "Updated alarm: ${updated.id}")
            }
        } else {
            current.add(updated)
            _alarms.value = current
            saveAlarms(current)
            Log.d("AlarmViewModel", "Added new alarm: ${updated.id}")
        }
    }

    fun deleteAlarm(alarm: Alarm) {
        val current = _alarms.value.orEmpty().toMutableList()
        val removed = current.removeAll { it.id == alarm.id }

        // Only save if an alarm was removed
        if (removed) {
            _alarms.value = current
            saveAlarms(current)
            Log.d("AlarmViewModel", "Deleted alarm: ${alarm.id}")
        }
    }

    fun addAlarm(newAlarm: Alarm) {
        val current = _alarms.value.orEmpty().toMutableList()

        // Determine the next ID by getting the highest existing ID and adding 1
        val nextId = current.maxOfOrNull { it.id }?.plus(1) ?: 1

        // Create the alarm with the new ID
        val alarmWithId = newAlarm.copy(id = nextId)
        current.add(alarmWithId)

        // Only save if the alarm was actually added
        if (_alarms.value != current) {
            _alarms.value = current
            saveAlarms(current)
            Log.d("AlarmViewModel", "Added new alarm: ${alarmWithId.id}")
        }
    }
}


