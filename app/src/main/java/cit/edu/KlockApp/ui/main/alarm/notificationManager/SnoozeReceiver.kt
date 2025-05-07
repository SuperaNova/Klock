package cit.edu.KlockApp.ui.main.alarm.notificationManager

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.widget.Toast
import cit.edu.KlockApp.ui.main.alarm.AlarmStorage

class SnoozeReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val alarmId       = intent.getIntExtra("ALARM_ID", -1)
        val snoozeMinutes = intent.getIntExtra("SNOOZE_MINUTES", 5)
        val alarmLabel    = intent.getStringExtra("ALARM_LABEL") ?: "Alarm"

        if (alarmId == -1) return

        // Stop any existing AlarmService
        context.stopService(Intent(context, AlarmService::class.java))

        // Show a toast
        Toast.makeText(
            context,
            "Snoozed \"$alarmLabel\" for $snoozeMinutes minutes",
            Toast.LENGTH_SHORT
        ).show()

        // Fetch the Alarm object, apply the new snooze length, and reschedule
        val alarm = AlarmStorage.getAlarmById(context, alarmId)?.copy(snoozeMinutes = snoozeMinutes)
        if (alarm != null) {
            AlarmScheduler.snooze(context, alarm)
        }
    }
}
