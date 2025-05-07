package cit.edu.KlockApp.ui.main.alarm.notificationManager

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import cit.edu.KlockApp.ui.main.alarm.Alarm
import java.util.Calendar
import java.util.concurrent.TimeUnit

object AlarmScheduler {
    fun schedule(context: Context, alarm: Alarm) {
        if (!alarm.isEnabled) return
        val triggerTime = calculateNextTriggerTime(alarm) ?: return

        val intent = Intent(context, AlarmReceiver::class.java).putExtra("alarm", alarm)

        val pi = PendingIntent.getBroadcast(
            context,
            alarm.id.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        // Cancel any existing one before we set the new one:
        am.cancel(pi)

        am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pi)
    }



    fun cancel(context: Context, alarm: Alarm) {
        val intent = Intent(context, AlarmReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            alarm.id.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        (context.getSystemService(Context.ALARM_SERVICE) as AlarmManager).cancel(pendingIntent)
    }

    fun snooze(context: Context, alarm: Alarm) {
        val snoozeTimeMillis = System.currentTimeMillis() + TimeUnit.MINUTES.toMillis(alarm.snoozeMinutes.toLong())
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            putExtra("ALARM_ID", alarm.id)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            ("snooze_${alarm.id}").hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, snoozeTimeMillis, pendingIntent)
    }

    private fun calculateNextTriggerTime(alarm: Alarm): Long? {
        val triggerCal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, alarm.time.hour)
            set(Calendar.MINUTE, alarm.time.minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        if (alarm.repeatDays.isEmpty()) {
            if (triggerCal.timeInMillis <= System.currentTimeMillis()) {
                triggerCal.add(Calendar.DAY_OF_YEAR, 1)
            }
            return triggerCal.timeInMillis
        }

        val orderedDays = listOf("Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday")
        val todayIndex = Calendar.getInstance().get(Calendar.DAY_OF_WEEK) - 1

        for (i in 0..6) {
            val dayToCheck = (todayIndex + i) % 7
            if (alarm.repeatDays.contains(orderedDays[dayToCheck])) {
                val checkCal = Calendar.getInstance().apply {
                    add(Calendar.DAY_OF_YEAR, i)
                    set(Calendar.HOUR_OF_DAY, alarm.time.hour)
                    set(Calendar.MINUTE, alarm.time.minute)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                if (checkCal.timeInMillis > System.currentTimeMillis()) return checkCal.timeInMillis
            }
        }

        return null
    }

}