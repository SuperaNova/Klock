package cit.edu.KlockApp.ui.main.alarm

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import java.time.LocalTime

class AlarmEditActivity : BaseAlarmActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        tvTitle.text = "Edit Alarm"

        // Retrieve passed alarm object
        alarm = intent.getParcelableExtra("alarm")
            ?: throw IllegalStateException("No alarm provided")

        // Initialize UI fields with saved alarm data
        labelEditText.setText(alarm.label)
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
        setResult(Activity.RESULT_OK, resultIntent)
        finish()
    }

    override fun handleFinalization() {
        returnWithResult()
    }
}
