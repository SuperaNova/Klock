package cit.edu.KlockApp.ui.main.alarm

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import cit.edu.KlockApp.R
import java.time.LocalTime

class AlarmEditActivity : BaseAlarmActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.title = "Edit Alarm"

        // Retrieve passed alarm object
        alarm = intent.getParcelableExtra("alarm")
            ?: throw IllegalStateException("No alarm provided")

        // Initialize UI fields with saved alarm data
        labelEditText.setText(alarm.label)
        timePicker.hour = alarm.time.hour
        timePicker.minute = alarm.time.minute
        selectedDays = alarm.repeatDays.toMutableList()
    }

    // Update the alarm object with the data from UI components
    override fun updateAlarmFromUI() {
        alarm = alarm.copy(
            time = LocalTime.of(timePicker.hour, timePicker.minute),
            label = labelEditText.text.toString(),
            repeatDays = selectedDays.toList()
        )
    }

    // Return the updated alarm back to the calling activity
    override fun returnWithResult() {
        val resultIntent = Intent().putExtra("updatedAlarm", alarm)
        setResult(RESULT_OK, resultIntent)
        finish()
    }

    // Handle the confirm button click to update the alarm
    private fun handleConfirmEditAlarm() {
        updateAlarmFromUI()

        if (!alarm.isEnabled) {
            cancelAlarm()
            Toast.makeText(this, "Alarm is disabled and won't be scheduled", Toast.LENGTH_SHORT).show()
            returnWithResult()
            return
        }

        val triggerTime = calculateNextTriggerTime()
        if (triggerTime == null) {
            Toast.makeText(this, "No valid future repeat day found. Alarm not scheduled.", Toast.LENGTH_SHORT).show()
            return
        }

        scheduleAlarm(triggerTime)
        handleFinalization()
    }

    // Finalize the action by returning the updated alarm
    override fun handleFinalization() {
        returnWithResult()  // Just return the updated alarm
    }

    // Create the options menu, where we set the confirm button
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.action_bar_menu, menu)

        val settingsItem = menu?.findItem(R.id.action_settings)
        settingsItem?.isVisible = false  // Hide settings item

        val confirmEditAlarm = menu?.findItem(R.id.action_add)
        confirmEditAlarm?.setIcon(R.drawable.check_24px)  // Set your custom icon

        return true
    }

    // Handle options item selection, specifically the edit button
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_add -> {
                handleConfirmEditAlarm()  // Handle confirm edit alarm
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
