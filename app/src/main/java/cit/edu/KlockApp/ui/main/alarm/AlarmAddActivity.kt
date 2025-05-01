package cit.edu.KlockApp.ui.main.alarm

import android.content.Intent
import android.os.Bundle
import androidx.lifecycle.ViewModelProvider
import java.time.LocalTime

class AlarmAddActivity : BaseAlarmActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        tvTitle.text = "Create Alarm"

        // Load or create default alarm
        alarm = intent.getSerializableExtra("ALARM_DATA") as? Alarm ?: Alarm(
            id = 1,
            label = "New Alarm",
            time = LocalTime.of(9, 0),
            repeatDays = emptyList(),
            isEnabled = true,
        )

        // Set the default label in the UI
        labelEditText.setText(alarm.label)  // ← This ensures "New Alarm" shows up

        timePicker.hour = alarm.time.hour
        timePicker.minute = alarm.time.minute

        selectedDays = alarm.repeatDays.toMutableList()
    }

    override fun updateAlarmFromUI() {
        alarm = alarm.copy(
            time = LocalTime.of(timePicker.hour, timePicker.minute),
            label = labelEditText.text.toString(),
            repeatDays = selectedDays.toList()
        )
    }

    override fun returnWithResult() {
        val resultIntent = Intent().putExtra("updatedAlarm", alarm)
        setResult(RESULT_OK, resultIntent)
        finish()
    }

    override fun handleFinalization() {
        val alarmViewModel = ViewModelProvider(this).get(AlarmViewModel::class.java)
        alarmViewModel.addAlarm(alarm)
        returnWithResult()
    }
}
