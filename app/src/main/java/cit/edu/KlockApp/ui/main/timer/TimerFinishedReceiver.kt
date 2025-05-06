package cit.edu.KlockApp.ui.main.timer

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat // For starting foreground service
import cit.edu.KlockApp.R
import cit.edu.KlockApp.ui.main.KlockActivity

class TimerFinishedReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_TIMER_FINISHED = "cit.edu.KlockApp.ACTION_TIMER_FINISHED"
        const val EXTRA_SOUND_URI = "extra_sound_uri"
        const val EXTRA_TIMER_LABEL = "extra_timer_label"
        const val USER_NOTIFICATION_CHANNEL_ID = "timer_user_notifications_channel" // Renamed for clarity
        const val USER_NOTIFICATION_ID = 2 // User-visible notification ID
        private const val TAG = "TimerFinishedReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        Log.d(TAG, "onReceive called. Action: ${intent.action}")
        if (intent.action == ACTION_TIMER_FINISHED) {
            Log.d(TAG, "Timer finished action received.")
            intent.extras?.let {
                for (key in it.keySet()) {
                    Log.d(TAG, "Intent Extra: Key=$key, Value=${it.get(key)}")
                }
            }

            val soundUriString = intent.getStringExtra(EXTRA_SOUND_URI)
            val timerLabel = intent.getStringExtra(EXTRA_TIMER_LABEL) ?: "Timer"
            Log.d(TAG, "Received soundUriString: $soundUriString, Label: $timerLabel")

            // Start the sound service
            val serviceIntent = Intent(context, TimerSoundService::class.java).apply {
                action = TimerSoundService.ACTION_START_SOUND
                putExtra(TimerSoundService.EXTRA_SOUND_URI, soundUriString)
                putExtra(TimerSoundService.EXTRA_TIMER_LABEL_SERVICE, timerLabel)
            }
            ContextCompat.startForegroundService(context, serviceIntent)
            Log.d(TAG, "Sent intent to start TimerSoundService.")

            // Show the user-interactive notification (silent, sound handled by service)
            createUserNotificationChannel(context) // Ensure channel exists
            showUserNotification(context, timerLabel)
        }
    }

    // Renamed to createUserNotificationChannel for clarity
    private fun createUserNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = context.getString(R.string.timer_notification_channel_name)
            val descriptionText = context.getString(R.string.timer_notification_channel_description)
            // Importance HIGH so it shows as a heads-up, but sound is handled by service
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(USER_NOTIFICATION_CHANNEL_ID, name, importance).apply {
                description = descriptionText
                enableVibration(true) // Channel can still manage vibration pattern
                setSound(null, null) // Explicitly no sound on this channel
            }
            val notificationManager: NotificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    // Renamed to showUserNotification, soundUri parameter removed
    private fun showUserNotification(context: Context, label: String) {
        val openAppIntent = Intent(context, KlockActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val openAppPendingIntent: PendingIntent = PendingIntent.getActivity(
            context, 0, openAppIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Intent to stop the sound service
        val stopSoundIntent = Intent(context, TimerSoundService::class.java).apply {
            action = TimerSoundService.ACTION_STOP_SOUND
        }
        val stopSoundPendingIntent: PendingIntent = PendingIntent.getService(
            context, 1, stopSoundIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notificationTitle = "$label Finished"
        val notificationText = context.getString(R.string.timer_notification_content_text)

        val builder = NotificationCompat.Builder(context, USER_NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_timer_24)
            .setContentTitle(notificationTitle)
            .setContentText(notificationText)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setContentIntent(openAppPendingIntent)
            .setDeleteIntent(stopSoundPendingIntent) // Stop sound service if notification is dismissed by swipe
            .setAutoCancel(true)
            .setSound(null) // Notification itself is silent
            .addAction(R.drawable.ic_stop_sound, "Stop Sound", stopSoundPendingIntent) // Action button
            // Vibration is handled by the channel (enableVibration(true))

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(USER_NOTIFICATION_ID, builder.build())
        Log.d(TAG, "User notification shown for timer: '$label' (ID: $USER_NOTIFICATION_ID)")
    }
} 