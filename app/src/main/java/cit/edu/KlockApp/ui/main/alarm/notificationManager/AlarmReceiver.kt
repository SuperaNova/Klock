package cit.edu.KlockApp.ui.main.alarm.notificationManager

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import cit.edu.KlockApp.ui.main.alarm.AlarmStorage

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val alarmId = intent.getIntExtra("ALARM_ID", -1)
        if (alarmId == -1) return

        val alarm = AlarmStorage.getAlarmById(context, alarmId)
        if (alarm == null || !alarm.isEnabled) {
            // Alarm was deleted or disabled
            return
        }

        // Cancel the "Snoozed" notification
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(alarmId)

        val alarmLabel = alarm.label
        val vibrateOnAlarm = alarm.vibrateOnAlarm  // Get the vibrate setting
        val alarmSoundUri = alarm.alarmSound

        val serviceIntent = Intent(context, AlarmService::class.java).apply {
            putExtra("ALARM_LABEL", alarmLabel)
            putExtra("ALARM_ID", alarmId)
            putExtra("VIBRATE_ON_ALARM", vibrateOnAlarm)  // Pass vibrateOnAlarm flag
            putExtra("ALARM_SOUND_URI", alarmSoundUri)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(serviceIntent)
        } else {
            context.startService(serviceIntent)
        }
    }
}