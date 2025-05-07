package cit.edu.KlockApp.ui.main.timer

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import cit.edu.KlockApp.R
import cit.edu.KlockApp.ui.main.KlockActivity

class TimerSoundService : Service() {

    private var mediaPlayer: MediaPlayer? = null

    companion object {
        const val ACTION_START_SOUND = "cit.edu.KlockApp.ACTION_START_SOUND"
        const val ACTION_STOP_SOUND = "cit.edu.KlockApp.ACTION_STOP_SOUND"
        const val EXTRA_SOUND_URI = "extra_sound_uri_service"
        const val EXTRA_TIMER_LABEL_SERVICE = "extra_timer_label_service"
        private const val SERVICE_CHANNEL_ID = "timer_sound_service_channel"
        private const val SERVICE_NOTIFICATION_ID = 3 // Different from TimerFinishedReceiver's notification
        private const val TAG = "TimerSoundService"
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "onStartCommand received action: ${intent?.action}")
        when (intent?.action) {
            ACTION_START_SOUND -> {
                val soundUriString = intent.getStringExtra(EXTRA_SOUND_URI)
                val timerLabel = intent.getStringExtra(EXTRA_TIMER_LABEL_SERVICE) ?: "Timer"
                startForegroundAndPlaySound(soundUriString, timerLabel)
            }
            ACTION_STOP_SOUND -> {
                stopSoundAndService()
            }
            else -> {
                Log.w(TAG, "Unknown action or null intent, stopping service to be safe.")
                stopSoundAndService() // Stop if action is unknown or intent is null
            }
        }
        return START_NOT_STICKY // Don't restart if killed by system
    }

    private fun startForegroundAndPlaySound(soundUriString: String?, timerLabel: String) {
        createServiceNotificationChannel()

        // Create the notification for the foreground service
        val notificationIntent = Intent(this, KlockActivity::class.java) // Opens app
        val pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)

        val serviceNotification = NotificationCompat.Builder(this, SERVICE_CHANNEL_ID)
            .setContentTitle("$timerLabel is sounding")
            .setContentText("Timer sound is playing.")
            .setSmallIcon(R.drawable.ic_timer_24) // Use same timer icon or a generic sound icon
            .setContentIntent(pendingIntent)
            .setSound(null) // Important: Service notification itself should be silent
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

        startForeground(SERVICE_NOTIFICATION_ID, serviceNotification)
        Log.d(TAG, "Service started in foreground.")

        playSound(soundUriString)
    }

    private fun playSound(soundUriString: String?) {
        mediaPlayer?.release() // Release any existing player
        mediaPlayer = null

        val soundUri = if (!soundUriString.isNullOrEmpty()) {
            try { Uri.parse(soundUriString) } catch (e: Exception) {
                Log.e(TAG, "Error parsing sound URI: $soundUriString", e)
                null
            }
        } else {
            RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
        }

        if (soundUri == null) {
            Log.w(TAG, "Sound URI is null, cannot play sound.")
            // Optionally, play a very short fallback beep here if desired, then stopSelf()
            stopSoundAndService() // Nothing to play
            return
        }

        Log.d(TAG, "Attempting to play sound: $soundUri")
        mediaPlayer = MediaPlayer().apply {
            try {
                setDataSource(applicationContext, soundUri)
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .setUsage(AudioAttributes.USAGE_ALARM) // Crucial for playing as an alarm
                        .build()
                )
                isLooping = true // Make the sound loop until stopped
                prepareAsync() // Prepare asynchronously
                setOnPreparedListener {
                    Log.d(TAG, "MediaPlayer prepared, starting playback.")
                    start()
                }
                setOnErrorListener { mp, what, extra ->
                    Log.e(TAG, "MediaPlayer error: what: $what, extra: $extra for URI: $soundUri")
                    stopSoundAndService() // Stop service on error
                    true
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error setting data source or preparing MediaPlayer for URI: $soundUri", e)
                stopSoundAndService()
            }
        }
    }

    private fun stopSoundAndService() {
        Log.d(TAG, "Stopping sound and service.")
        mediaPlayer?.stop()
        mediaPlayer?.release()
        mediaPlayer = null
        stopForeground(true) // True to remove notification
        stopSelf() // Stop the service
    }

    private fun createServiceNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channelName = "Timer Sound Service"
            val importance = NotificationManager.IMPORTANCE_LOW // Low importance for service's own notification
            val channel = NotificationChannel(SERVICE_CHANNEL_ID, channelName, importance).apply {
                description = "Channel for the timer sound foreground service notification."
                setSound(null, null) // No sound for this channel's notifications
            }
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
            Log.d(TAG, "Service notification channel created.")
        }
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null // Not a bound service
    }

    override fun onDestroy() {
        Log.d(TAG, "onDestroy called. Releasing MediaPlayer.")
        mediaPlayer?.release()
        mediaPlayer = null
        super.onDestroy()
    }
}