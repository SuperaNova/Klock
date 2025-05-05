package cit.edu.KlockApp.ui.main.alarm

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import android.media.RingtoneManager
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
    private val FIRST_LAUNCH_KEY = "first_launch_key"  // Flag to track first launch

    private val _alarms = MutableLiveData<List<Alarm>>().apply {
        value = loadAlarms()
    }
    val alarms: LiveData<List<Alarm>> = _alarms

    private fun loadAlarms(): List<Alarm> {
        val stored = prefs.getString(ALARMS_KEY, null)
        val firstLaunch = prefs.getBoolean(FIRST_LAUNCH_KEY, true)

        // If it's the first launch and there are no alarms, create a default alarm
        if (firstLaunch) {
            // Mark that the app has been opened before
            prefs.edit().putBoolean(FIRST_LAUNCH_KEY, false).apply()

            // If no alarms are stored, create and return default alarm
            if (stored.isNullOrEmpty() || stored == "[]") {
                return listOf(createDefaultAlarm())  // Add default alarm on first launch
            }
        }

        // If it's not the first launch, return the stored alarms or an empty list if none exist
        return try {
            val list = if (stored.isNullOrEmpty()) emptyList() else json.decodeFromString<List<Alarm>>(stored)
            list
        } catch (e: Exception) {
            Log.e("AlarmViewModel", "Error parsing alarms: ${e.message}", e)
            emptyList()  // Return empty list in case of error
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
            label = "Wake Up",
            snoozeMinutes = 5,
            vibrateOnAlarm = true,
            alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM).toString()
        )
    }

    fun updateAlarm(updated: Alarm) {
        val current = _alarms.value.orEmpty().toMutableList()
        val idx = current.indexOfFirst { it.id == updated.id }

        if (idx >= 0) {
            val oldAlarm = current[idx]
            if (oldAlarm != updated) {  // Only update if the alarm actually changed
                current[idx] = updated
                _alarms.value = current
                saveAlarms(current)
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

        if (removed) {
            _alarms.value = current
            saveAlarms(current)
            Log.d("AlarmViewModel", "Deleted alarm: ${alarm.id}")

            // No action needed here because we don't want to re-trigger default alarm
        }
    }

    fun addAlarm(newAlarm: Alarm) {
        val current = _alarms.value.orEmpty().toMutableList()

        val nextId = current.maxOfOrNull { it.id }?.plus(1) ?: 1
        val alarmWithId = newAlarm.copy(id = nextId)
        current.add(alarmWithId)

        if (_alarms.value != current) {
            _alarms.value = current
            saveAlarms(current)
            Log.d("AlarmViewModel", "Added new alarm: ${alarmWithId.id}")
        }
    }
}