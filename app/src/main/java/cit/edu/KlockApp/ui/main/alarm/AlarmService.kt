package cit.edu.KlockApp.ui.main.alarm

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.Ringtone
import android.media.RingtoneManager
import android.os.Build
import android.os.IBinder
import android.os.Vibrator
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import cit.edu.KlockApp.R

class AlarmService : Service() {

    private var ringtone: Ringtone? = null
    private var vibrator: Vibrator? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val label = intent?.getStringExtra("ALARM_LABEL") ?: "Alarm"
        Log.d("AlarmService", "Alarm started with label: $label")

        createNotificationChannel()
        val notification = buildNotification(label)

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

        // Play ringtone if not already playing
        val alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
        if (ringtone == null || !ringtone!!.isPlaying) {
            ringtone = RingtoneManager.getRingtone(this, alarmUri)
            ringtone?.play()
        }

        // Vibrate only once
        if (vibrator == null) {
            vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            val pattern = longArrayOf(0, 500, 500)
            vibrator?.vibrate(pattern, 0)
        }

        return START_STICKY
    }

    private fun buildNotification(label: String): Notification {
        val stopIntent = Intent(this, AlarmDismissReceiver::class.java).apply {
            putExtra("ALARM_LABEL", label)
        }
        val stopPending = PendingIntent.getBroadcast(
            this,
            label.hashCode(),
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, "alarm_channel_id")
            .setContentTitle(label)
            .setContentText("Alarm Ringing")
            .setSmallIcon(R.drawable.alarm_icon_24dp)
            .addAction(R.drawable.alarm_icon_24dp, "Stop", stopPending)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build().apply {
                flags = flags or Notification.FLAG_NO_CLEAR
            }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Alarm Channel"
            val descriptionText = "Channel for alarm notifications"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel("alarm_channel_id", name, importance).apply {
                description = descriptionText
                setSound(
                    RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM),
                    android.media.AudioAttributes.Builder()
                        .setUsage(android.media.AudioAttributes.USAGE_ALARM)
                        .setContentType(android.media.AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                setBypassDnd(true)
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
