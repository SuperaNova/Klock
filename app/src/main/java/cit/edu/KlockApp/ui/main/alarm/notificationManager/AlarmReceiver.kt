    package cit.edu.KlockApp.ui.main.alarm.notificationManager

    import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import cit.edu.KlockApp.ui.main.alarm.Alarm

    class AlarmReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            // 1) Pull the full Alarm you scheduled
            val alarm = intent.getParcelableExtra<Alarm>("alarm")
            if (alarm == null || !alarm.isEnabled) return

            // 2) Cancel any prior notification
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.cancel(alarm.id)

            // 3) Start your service with that same Alarm
            val serviceIntent = Intent(context, AlarmService::class.java).apply {
                putExtra("alarm", alarm)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(serviceIntent)
            } else {
                context.startService(serviceIntent)
            }
        }
    }
