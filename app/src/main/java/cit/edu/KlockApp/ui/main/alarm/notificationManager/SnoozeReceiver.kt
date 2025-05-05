package cit.edu.KlockApp.ui.main.alarm.notificationManager

import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.widget.Toast
import androidx.core.app.NotificationCompat
import cit.edu.KlockApp.R

class SnoozeReceiver : BroadcastReceiver() {
    @SuppressLint("ScheduleExactAlarm")
    override fun onReceive(context: Context, intent: Intent) {
        val alarmId = intent.getIntExtra("ALARM_ID", -1)
        val snoozeMinutes = intent.getIntExtra("SNOOZE_MINUTES", 5)
        val alarmLabel = intent.getStringExtra("ALARM_LABEL") ?: "Alarm"
        val snoozeTime = System.currentTimeMillis() + snoozeMinutes * 60 * 1000

        if (alarmId == -1) return

        // Stop current alarm service (stop ringing and vibration immediately)
        context.stopService(Intent(context, AlarmService::class.java))

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        // Cancel the current scheduled alarm
        val originalIntent = Intent(context, AlarmReceiver::class.java).apply {
            putExtra("ALARM_ID", alarmId)
            putExtra("ALARM_LABEL", alarmLabel)
        }

        val originalPendingIntent = PendingIntent.getBroadcast(
            context,
            alarmId,
            originalIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        alarmManager.cancel(originalPendingIntent)

        // Reschedule snoozed alarm
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                snoozeTime,
                originalPendingIntent
            )
        } else {
            alarmManager.setExact(
                AlarmManager.RTC_WAKEUP,
                snoozeTime,
                originalPendingIntent
            )
        }

        // Show snooze toast feedback
        Toast.makeText(context, "Snoozed for $snoozeMinutes minutes", Toast.LENGTH_SHORT).show()

        // Optional: Show silent notification indicating alarm is snoozed
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val notification = NotificationCompat.Builder(context, "alarm_channel_id")
            .setContentTitle("$alarmLabel Snoozed")
            .setContentText("Will ring in $snoozeMinutes minutes")
            .setSmallIcon(R.drawable.alarm_icon_24dp)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setSilent(true)
            .build()

        notificationManager.notify(alarmId, notification)
    }
}
