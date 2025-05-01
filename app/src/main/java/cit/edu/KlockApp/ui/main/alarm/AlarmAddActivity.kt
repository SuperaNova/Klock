package cit.edu.KlockApp.ui.main.alarm

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.lifecycle.ViewModelProvider
import cit.edu.KlockApp.R
import java.time.LocalTime

class AlarmAddActivity : BaseAlarmActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.title = "Create Alarm"

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

    // Handle the confirmAddAlarm button to finalize the alarm
    private fun handleConfirmAddAlarm() {
        updateAlarmFromUI()

        // If the alarm is disabled, we cancel it
        if (!alarm.isEnabled) {
            cancelAlarm()
            Toast.makeText(this, "Alarm is disabled and won't be scheduled", Toast.LENGTH_SHORT).show()
            returnWithResult()
            return
        }

        // Calculate the trigger time
        val triggerTime = calculateNextTriggerTime() ?: run {
            Toast.makeText(this, "No valid future repeat day found. Alarm not scheduled.", Toast.LENGTH_SHORT).show()
            return
        }

        // Schedule the alarm
        scheduleAlarm(triggerTime)

        // Finalize
        handleFinalization()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.action_bar_menu, menu)

        val settingsItem = menu?.findItem(R.id.action_settings)
        settingsItem?.isVisible = false  // Hide settings item

        val confirmAddAlarm = menu?.findItem(R.id.action_add)
        confirmAddAlarm?.setIcon(R.drawable.check_24px)  // Set your custom icon

        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_add -> {
                // Call the method when the "Add" button is clicked
                handleConfirmAddAlarm()
                true
            }
            android.R.id.home -> {
                finish()  // Handle back action
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

}
