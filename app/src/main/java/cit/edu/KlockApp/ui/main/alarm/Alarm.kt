package cit.edu.KlockApp.ui.main.alarm

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import java.time.LocalTime

@Parcelize
@Serializable
data class Alarm(
    val id: Int,
    val label: String,
    @Contextual
    val time: LocalTime,
    val repeatDays: List<String>,
    var isEnabled: Boolean,
    var snoozeMinutes: Int,
    var vibrateOnAlarm: Boolean,
    var alarmSound: String
) : Parcelable
