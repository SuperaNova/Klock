package cit.edu.KlockApp.ui.main.alarm

import android.app.AlarmManager
import android.app.AlertDialog
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Spinner
import android.widget.Switch
import android.widget.TextView
import android.widget.TimePicker
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import cit.edu.KlockApp.R
import cit.edu.KlockApp.databinding.ActivityEditAlarmBinding
import java.util.Calendar

abstract class BaseAlarmActivity : AppCompatActivity() {

    protected lateinit var binding: ActivityEditAlarmBinding

    protected lateinit var alarm: Alarm
    protected var selectedDays = mutableListOf<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEditAlarmBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupSnoozeSpinner()
        setupRepeatDialog()

        binding.ivClose.setOnClickListener { finish() }

        binding.ivCheck.setOnClickListener {
            updateAlarmFromUI()
            if (!alarm.isEnabled) {
                cancelAlarm()
                Toast.makeText(this, "Alarm is disabled and won't be scheduled", Toast.LENGTH_SHORT).show()
                returnWithResult()
                return@setOnClickListener
            }

            val triggerTime = calculateNextTriggerTime() ?: run {
                Toast.makeText(this, "No valid future repeat day found. Alarm not scheduled.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            scheduleAlarm(triggerTime)
            handleFinalization()
        }
    }

    abstract fun handleFinalization()

    abstract fun updateAlarmFromUI()

    abstract fun returnWithResult()

    private fun setupSnoozeSpinner() {
        val options = arrayOf("5 minutes", "10 minutes", "15 minutes")
        binding.snoozeSpinner.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, options)
    }

    private fun setupRepeatDialog() {
        binding.repeatButton.setOnClickListener {
            val days = arrayOf("Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday")
            val checked = BooleanArray(days.size) { selectedDays.contains(days[it]) }
            AlertDialog.Builder(this)
                .setTitle("Repeat")
                .setMultiChoiceItems(days, checked) { _, which, isChecked ->
                    if (isChecked) selectedDays.add(days[which]) else selectedDays.remove(days[which])
                }
                .setPositiveButton("Done") { _, _ -> binding.repeatButton.text = formatSelectedDays(selectedDays) }
                .setNegativeButton("Cancel", null)
                .show()
        }
    }

    private fun scheduleAlarm(triggerAt: Long) {
        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
            startActivity(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                data = Uri.parse("package:$packageName")
            })
            Toast.makeText(this, "Grant exact-alarm permission in settings.", Toast.LENGTH_LONG).show()
            return
        }

        val pi = PendingIntent.getBroadcast(
            this,
            alarm.id,
            Intent(this, AlarmReceiver::class.java).apply {
                putExtra("ALARM_LABEL", alarm.label)
                putExtra("ALARM_ID", alarm.id)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        alarmManager.setExact(AlarmManager.RTC_WAKEUP, triggerAt, pi)
        Toast.makeText(this, "Alarm set for ${binding.timePicker.hour.toString().padStart(2, '0')}:${binding.timePicker.minute.toString().padStart(2, '0')}", Toast.LENGTH_SHORT).show()
    }

    private fun calculateNextTriggerTime(): Long? {
        val triggerCal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, binding.timePicker.hour)
            set(Calendar.MINUTE, binding.timePicker.minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        if (selectedDays.isEmpty()) {
            if (triggerCal.timeInMillis <= System.currentTimeMillis()) {
                triggerCal.add(Calendar.DAY_OF_YEAR, 1)
            }
            return triggerCal.timeInMillis
        }

        val orderedDays = listOf("Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday")
        val todayIndex = Calendar.getInstance().get(Calendar.DAY_OF_WEEK) - 1

        for (i in 0..6) {
            val dayToCheck = (todayIndex + i) % 7
            if (selectedDays.contains(orderedDays[dayToCheck])) {
                val checkCal = Calendar.getInstance().apply {
                    add(Calendar.DAY_OF_YEAR, i)
                    set(Calendar.HOUR_OF_DAY, binding.timePicker.hour)
                    set(Calendar.MINUTE, binding.timePicker.minute)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                if (checkCal.timeInMillis > System.currentTimeMillis()) return checkCal.timeInMillis
            }
        }

        return null
    }

    private fun cancelAlarm() {
        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val pi = PendingIntent.getBroadcast(
            this,
            alarm.id,
            Intent(this, AlarmReceiver::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pi)
    }

    protected fun formatSelectedDays(days: List<String>): String {
        if (days.isEmpty()) return "Once"
        if (days.size == 7) return "Everyday"

        val orderedFull = listOf("Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday")
        val orderedShort = listOf("sun", "mon", "tue", "wed", "thu", "fri", "sat")
        val sortedDays = days.sortedBy { orderedFull.indexOf(it) }
        val spans = mutableListOf<String>()
        var i = 0

        while (i < sortedDays.size) {
            var j = i
            while (
                j + 1 < sortedDays.size &&
                orderedFull.indexOf(sortedDays[j + 1]) == orderedFull.indexOf(sortedDays[j]) + 1
            ) {
                j++
            }

            val startIndex = orderedFull.indexOf(sortedDays[i])
            val endIndex = orderedFull.indexOf(sortedDays[j])
            val startShort = orderedShort[startIndex]
            val endShort = orderedShort[endIndex]

            if (i == j) spans.add(startShort)
            else if (j - i == 1) spans.add("$startShort, $endShort")
            else spans.add("$startShort to $endShort")

            i = j + 1
        }

        return spans.joinToString(", ")
    }
}
