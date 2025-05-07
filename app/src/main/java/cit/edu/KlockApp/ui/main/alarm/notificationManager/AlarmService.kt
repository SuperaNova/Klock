package cit.edu.KlockApp.ui.main.alarm.notificationManager

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.Ringtone
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.IBinder
import android.os.Vibrator
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import cit.edu.KlockApp.R
import cit.edu.KlockApp.ui.main.alarm.Alarm
import kotlinx.serialization.modules.contextual

class AlarmService : Service() {

    private var ringtone: Ringtone? = null
    private var vibrator: Vibrator? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val alarm = intent?.getParcelableExtra<Alarm>("alarm")
            ?: return START_NOT_STICKY

        Log.d("AlarmService", "Alarm started with label: ${alarm.label} and ID: ${alarm.id}, vibrateOnAlarm: ${alarm.vibrateOnAlarm}")

        createNotificationChannel()
        val notification = buildNotification(alarm.id, alarm.label) // Pass both alarmId and label

        // Start foreground with explicit alarm type for Android 14+
        if (Build.VERSION.SDK_INT >= 34) {
            ServiceCompat.startForeground(
                this,
                1,
                notification,
                0x40000000.toInt()  // specialUse ALARM bit
            )
        } else {
            startForeground(1, notification)
        }


        // Play only the exact URI on the Alarm object:
        val uri = Uri.parse(alarm.alarmSound)
        if (ringtone == null || !ringtone!!.isPlaying) {
            ringtone = RingtoneManager.getRingtone(this, uri)
            ringtone?.play()
        }

        // Vibrate only once if the vibrateOnAlarm flag is true
        // Vibrate if requested
        if (alarm.vibrateOnAlarm) {
            vibrator = vibrator ?: getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            vibrator?.vibrate(longArrayOf(0, 500, 500), 0)
        } else {
            Log.d("AlarmService", "Vibration is disabled based on the setting.")
        }

        return START_STICKY
    }

    private fun buildNotification(alarmId: Int, label: String): Notification {
        val prefs = getSharedPreferences("alarms_prefs", Context.MODE_PRIVATE)
        val alarmsJson = prefs.getString("alarms_key", null)

        val snoozeMinutes = alarmsJson?.let {
            try {
                val alarmsList = kotlinx.serialization.json.Json {
                    serializersModule = kotlinx.serialization.modules.SerializersModule {
                        contextual(cit.edu.KlockApp.ui.main.alarm.LocalTimeSerializer)
                    }
                    ignoreUnknownKeys = true
                }.decodeFromString<List<cit.edu.KlockApp.ui.main.alarm.Alarm>>(it)

                alarmsList.find { it.id == alarmId }?.snoozeMinutes ?: 5
            } catch (e: Exception) {
                Log.e("AlarmService", "Failed to parse alarms JSON: ${e.message}")
                5
            }
        } ?: 5


        // Creating snooze intent
        val snoozeIntent = Intent(this, SnoozeReceiver::class.java).apply {
            putExtra("ALARM_ID", alarmId)
            putExtra("SNOOZE_MINUTES", snoozeMinutes)
            putExtra("ALARM_LABEL", label)
        }

        // Creating PendingIntent for snooze action
        val snoozePending = PendingIntent.getBroadcast(
            this,
            alarmId + 1000,  // Ensure unique request code for snooze
            snoozeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Creating stop intent
        val stopIntent = Intent(this, AlarmDismissReceiver::class.java).apply {
            putExtra("ALARM_LABEL", label)
        }

        // Creating PendingIntent for stop action
        val stopPending = PendingIntent.getBroadcast(
            this,
            label.hashCode(),
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Build the notification
        return NotificationCompat.Builder(this, "alarm_channel_id")
            .setContentTitle(label)
            .setContentText("Alarm Ringing")
            .setSmallIcon(R.drawable.alarm_icon_24dp)
            .addAction(R.drawable.alarm_icon_24dp, "Stop", stopPending)
            .addAction(R.drawable.snooze_24px, "Snooze", snoozePending)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build().apply {
                flags = flags or Notification.FLAG_NO_CLEAR  // Prevent clearing the notification
            }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Alarm Channel"
            val descriptionText = "Channel for alarm notifications"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel("alarm_channel_id", name, importance).apply {
                description = descriptionText
            }
            val notificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        ringtone?.stop()
        vibrator?.cancel()
        Log.d("AlarmService", "Alarm stopped")
    }

    override fun onBind(intent: Intent?): IBinder? = null
}