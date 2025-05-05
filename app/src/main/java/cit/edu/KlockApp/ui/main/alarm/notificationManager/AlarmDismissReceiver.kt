package cit.edu.KlockApp.ui.main.alarm.notificationManager

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationManagerCompat

class AlarmDismissReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val alarmLabel = intent.getStringExtra("ALARM_LABEL") ?: "Alarm"
        // Stop the foreground service
        context.stopService(Intent(context, AlarmService::class.java))
        // Cancel the persistent notification
        NotificationManagerCompat.from(context)
            .cancel(alarmLabel.hashCode())
    }
}

