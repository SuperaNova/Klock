package cit.edu.KlockApp.ui.main.alarm

import android.content.Context
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.contextual

object AlarmStorage {
    private const val ALARMS_KEY = "alarms_key"

    private val json = Json {
        serializersModule = SerializersModule {
            contextual(LocalTimeSerializer)
        }
        ignoreUnknownKeys = true
    }

    fun getAlarmById(context: Context, id: Int): Alarm? {
        val prefs = context.getSharedPreferences("alarms_prefs", Context.MODE_PRIVATE)
        val stored = prefs.getString(ALARMS_KEY, null) ?: return null
        return try {
            json.decodeFromString<List<Alarm>>(stored).firstOrNull { it.id == id }
        } catch (e: Exception) {
            null
        }
    }
}
