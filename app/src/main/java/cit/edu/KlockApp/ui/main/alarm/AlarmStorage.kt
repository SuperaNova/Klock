package cit.edu.KlockApp.ui.main.alarm

import android.content.Context
import android.util.Log
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.contextual

object AlarmStorage {
    private const val PREFS_NAME = "alarms_prefs"
    private const val ALARMS_KEY = "alarms_key"
    private val json = Json {
        serializersModule = SerializersModule {
            contextual(LocalTimeSerializer)
        }
        ignoreUnknownKeys = true
    }

    fun getAlarmById(context: Context, id: Int): Alarm? {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val stored = prefs.getString(ALARMS_KEY, null) ?: return null

        return try {
            val alarms = json.decodeFromString<List<Alarm>>(stored)
            alarms.find { it.id == id }
        } catch (e: Exception) {
            Log.e("AlarmStorage", "Error reading alarm: ${e.message}", e)
            null
        }
    }
}