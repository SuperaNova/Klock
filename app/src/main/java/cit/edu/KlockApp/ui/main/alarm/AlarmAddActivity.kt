package cit.edu.KlockApp.ui.main.alarm

import android.content.Intent
import android.os.Bundle
import androidx.lifecycle.ViewModelProvider
import java.time.LocalTime

class AlarmAddActivity : BaseAlarmActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Access views via binding
        binding.tvTitle.text = "Create Alarm"

        // Load or create default alarm
        // Ensure default alarm has a unique ID. Relying on subclasses/ViewModel to manage IDs.
        alarm = intent.getParcelableExtra<Alarm>("ALARM_DATA") ?: Alarm(
            id = System.currentTimeMillis().toInt(), // Use timestamp for potentially unique ID
            label = "Alarm", // Changed default label slightly
            time = LocalTime.now(), // Default to current time
            repeatDays = emptyList(),
            vibrate = true,
            isEnabled = true,
        )

        // Set the default values in the UI using binding
        binding.labelEditText.setText(alarm.label)
        binding.timePicker.hour = alarm.time.hour
        binding.timePicker.minute = alarm.time.minute
        binding.vibrateSwitch.isChecked = alarm.vibrate
        selectedDays = alarm.repeatDays.toMutableList() // Initialize selectedDays from potentially passed alarm

        // Update repeat button text initially
        binding.repeatButton.text = formatSelectedDays(selectedDays)
    }

    override fun updateAlarmFromUI() {
        alarm = alarm.copy(
            // Access views via binding
            time = LocalTime.of(binding.timePicker.hour, binding.timePicker.minute),
            label = binding.labelEditText.text.toString().ifBlank { "Alarm" }, // Use default if blank
            repeatDays = selectedDays.toList(),
            vibrate = binding.vibrateSwitch.isChecked,
            isEnabled = true // New alarms are always enabled when created
        )
    }

    override fun returnWithResult() {
        val resultIntent = Intent().putExtra("updatedAlarm", alarm)
        setResult(RESULT_OK, resultIntent)
        finish()
    }

    override fun handleFinalization() {
        // This assumes AlarmViewModel handles ID assignment if needed, or the timestamp ID is sufficient
        val alarmViewModel = ViewModelProvider(this).get(AlarmViewModel::class.java)
        alarmViewModel.addAlarm(alarm)
        returnWithResult()
    }
}
