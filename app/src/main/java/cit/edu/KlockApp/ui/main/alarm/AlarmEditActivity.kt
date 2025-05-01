package cit.edu.KlockApp.ui.main.alarm

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import java.time.LocalTime

class AlarmEditActivity : BaseAlarmActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Access views via binding
        binding.tvTitle.text = "Edit Alarm"

        // Retrieve passed alarm object
        alarm = intent.getParcelableExtra("alarm")
            ?: throw IllegalStateException("No alarm provided")

        // Initialize UI fields with saved alarm data using binding
        binding.labelEditText.setText(alarm.label)
        binding.timePicker.hour = alarm.time.hour
        binding.timePicker.minute = alarm.time.minute
        selectedDays = alarm.repeatDays.toMutableList()
        binding.vibrateSwitch.isChecked = alarm.vibrate

        // Update repeat button text initially
        binding.repeatButton.text = formatSelectedDays(selectedDays)
    }


    override fun updateAlarmFromUI() {
        alarm = alarm.copy(
            // Access views via binding
            time = LocalTime.of(binding.timePicker.hour, binding.timePicker.minute),
            label = binding.labelEditText.text.toString(),
            repeatDays = selectedDays.toList(),
            vibrate = binding.vibrateSwitch.isChecked,
            // Preserve isEnabled state when updating
            isEnabled = alarm.isEnabled
        )
    }

    override fun returnWithResult() {
        val resultIntent = Intent().putExtra("updatedAlarm", alarm)
        setResult(Activity.RESULT_OK, resultIntent)
        finish()
    }

    override fun handleFinalization() {
        returnWithResult()
    }
}
