package cit.edu.KlockApp.timer

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import cit.edu.KlockApp.R
import cit.edu.KlockApp.ui.main.KlockActivity // Target activity when notification is clicked

class TimerFinishedReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_TIMER_FINISHED = "cit.edu.KlockApp.ACTION_TIMER_FINISHED"
        const val EXTRA_SOUND_URI = "extra_sound_uri"
        const val EXTRA_TIMER_LABEL = "extra_timer_label" // Optional: pass a label
        const val CHANNEL_ID = "timer_channel"
        const val NOTIFICATION_ID = 2 // Unique ID for timer notifications
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == ACTION_TIMER_FINISHED) {
            Log.d("TimerFinishedReceiver", "Timer finished action received")
            val soundUriString = intent.getStringExtra(EXTRA_SOUND_URI)
            val timerLabel = intent.getStringExtra(EXTRA_TIMER_LABEL) ?: "Timer"

            createNotificationChannel(context)
            showNotification(context, timerLabel)
            playSound(context, soundUriString)
        }
    }

    private fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Timer Alerts"
            val descriptionText = "Notifications for completed timers"
            val importance = NotificationManager.IMPORTANCE_HIGH // High importance for sound/vibration
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
                // Optional: Customize vibration, lights etc.
                enableVibration(true)
            }
            val notificationManager: NotificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
            Log.d("TimerFinishedReceiver", "Notification channel created")
        }
    }

    private fun showNotification(context: Context, label: String) {
        val intent = Intent(context, KlockActivity::class.java).apply {
            // Optional: Add flags or extras to navigate to TimerFragment
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent: PendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_timer_24) // Replace with your timer icon
            .setContentTitle("$label Finished")
            .setContentText("Your timer has finished.")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM) // Important for heads-up/sound
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            // Optional: Add actions like dismiss or restart?

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID, builder.build())
        Log.d("TimerFinishedReceiver", "Notification shown")
    }

    private fun playSound(context: Context, soundUriString: String?) {
        val soundUri = if (!soundUriString.isNullOrEmpty()) {
            try { Uri.parse(soundUriString) } catch (e: Exception) { null }
        } else {
            RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION) // Default to notification sound
        }

        if (soundUri != null) {
            try {
                val ringtone = RingtoneManager.getRingtone(context, soundUri)
                ringtone?.play()
                Log.d("TimerFinishedReceiver", "Playing sound: $soundUri")
            } catch (e: Exception) {
                Log.e("TimerFinishedReceiver", "Error playing sound", e)
            }
        } else {
             Log.w("TimerFinishedReceiver", "Sound URI was null or invalid, no sound played")
        }
    }
} 