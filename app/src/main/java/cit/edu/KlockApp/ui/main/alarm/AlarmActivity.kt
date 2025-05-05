package cit.edu.KlockApp.ui.main.alarm

import android.app.AlarmManager
import android.app.AlertDialog
import android.app.PendingIntent
import android.content.Intent
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.provider.Settings
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.Switch
import android.widget.TimePicker
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import cit.edu.KlockApp.R
import cit.edu.KlockApp.ui.main.alarm.notificationManager.AlarmReceiver
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Calendar

class AlarmActivity : AppCompatActivity() {

    protected lateinit var timePicker: TimePicker
    private lateinit var repeatButton: Button
    private lateinit var alarmSoundButton: Button
    protected lateinit var labelEditText: EditText
    private lateinit var snoozeSpinner: Spinner
    private lateinit var vibrateSwitch: Switch
    protected lateinit var alarm: Alarm
    private var selectedAlarmSoundUri: Uri? = null
    protected var selectedDays = mutableListOf<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_alarm)
        // Choose the layout based on whether you're creating or editing the alarm
        if (intent.hasExtra("alarm")) {
            supportActionBar?.title = "Edit Alarm"
        } else {
            supportActionBar?.title = "Create Alarm"
        }

        // Enable the "up button" (back button) in the ActionBar
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)

        alarm = initAlarm()

        // Initialize UI components after alarm is initialized
        initializeViews()
        setupSnoozeSpinner()
        setupRepeatDialog()


    }

    // Abstract function for initializing alarm
    private fun initAlarm(): Alarm {
        return if (intent.hasExtra("alarm")) {
            intent.getParcelableExtra<Alarm>("alarm") ?: throw IllegalStateException("No alarm data found")
        } else {
            Alarm(
                id = System.currentTimeMillis().toInt(),
                label = "New Alarm",
                time = LocalTime.of(9, 0),
                repeatDays = emptyList(),
                isEnabled = true,
                snoozeMinutes = 5,
                vibrateOnAlarm = true,
                alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM).toString(),
            )
        }
    }

    private fun initializeViews() {

        labelEditText = findViewById(R.id.labelEditText)
        timePicker = findViewById(R.id.timePicker)
        repeatButton = findViewById(R.id.repeatButton)
        alarmSoundButton = findViewById(R.id.alarmSoundButton)
        snoozeSpinner = findViewById(R.id.snoozeSpinner)
        vibrateSwitch = findViewById(R.id.vibrateSwitch)

        labelEditText.setText(alarm.label)
        timePicker.hour = alarm.time.hour
        timePicker.minute = alarm.time.minute
        vibrateSwitch.isChecked = alarm.vibrateOnAlarm

        vibrateSwitch.setOnCheckedChangeListener { _, isChecked ->
            alarm = alarm.copy(vibrateOnAlarm = isChecked)  // Update the alarm object when toggled
        }

        alarmSoundButton.setOnClickListener {
            val ringtoneManager = RingtoneManager(this)
            ringtoneManager.setType(RingtoneManager.TYPE_ALARM) // Set the type to alarms

            // Get the cursor for the ringtones
            val cursor = ringtoneManager.cursor

            // Prepare lists to hold the sound titles and URIs
            val soundTitles = mutableListOf<String>()
            val soundUris = mutableListOf<Uri>()

            // Loop through the cursor and collect the ringtone titles and URIs
            while (cursor.moveToNext()) {
                val title = cursor.getString(RingtoneManager.TITLE_COLUMN_INDEX)
                val uri = ringtoneManager.getRingtoneUri(cursor.position)
                soundTitles.add(title)
                soundUris.add(uri)
            }
            cursor.close()

            // Find the currently selected alarm sound index (if any)
            var selectedSoundIndex = soundUris.indexOfFirst { it.toString() == selectedAlarmSoundUri.toString() }.takeIf { it >= 0 } ?: 0

            // Remember the current sound in case the user cancels
            val currentSelectedIndex = selectedSoundIndex
            val currentSoundUri = selectedAlarmSoundUri

            AlertDialog.Builder(this)
                .setTitle("Select Alarm Sound")
                .setSingleChoiceItems(soundTitles.toTypedArray(), selectedSoundIndex) { _, which ->
                    // Temporarily update the selected index and URI
                    selectedSoundIndex = which
                    selectedAlarmSoundUri = soundUris[which]
                }
                .setPositiveButton("Done") { _, _ ->
                    // Update the button text to the newly selected sound
                    alarmSoundButton.text = soundTitles[selectedSoundIndex]
                }
                .setNegativeButton("Cancel") { _, _ ->
                    // Revert to the original values
                    selectedSoundIndex = currentSelectedIndex
                    selectedAlarmSoundUri = currentSoundUri
                    // Show the current/default alarm sound name
                    alarmSoundButton.text = soundTitles[currentSelectedIndex]
                }
                .setOnCancelListener {
                    // Also handle when user presses the back button
                    selectedSoundIndex = currentSelectedIndex
                    selectedAlarmSoundUri = currentSoundUri
                    alarmSoundButton.text = soundTitles[currentSelectedIndex]
                }
                .show()
        }
    }

    private fun setupSnoozeSpinner() {
        val options = arrayOf("5 minutes", "10 minutes", "15 minutes")
        snoozeSpinner.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, options)
        val defaultSnooze = when (alarm.snoozeMinutes) {
            10 -> "10 minutes"
            15 -> "15 minutes"
            else -> "5 minutes"
        }
        snoozeSpinner.setSelection(options.indexOf(defaultSnooze))

        snoozeSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                val selectedOption = parent.getItemAtPosition(position).toString()
                alarm.snoozeMinutes = when {
                    selectedOption.contains("10") -> 10
                    selectedOption.contains("15") -> 15
                    else -> 5
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>) {}
        }
    }

    private fun setupRepeatDialog() {
        repeatButton.setOnClickListener {
            val days = arrayOf("Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday")
            val checked = BooleanArray(days.size) { selectedDays.contains(days[it]) }
            AlertDialog.Builder(this)
                .setTitle("Repeat")
                .setMultiChoiceItems(days, checked) { _, which, isChecked ->
                    if (isChecked) selectedDays.add(days[which]) else selectedDays.remove(days[which])
                }
                .setPositiveButton("Done") { _, _ -> repeatButton.text = formatSelectedDays(selectedDays) }
                .setNegativeButton("Cancel", null)
                .show()
        }
    }

    private fun formatSelectedDays(days: List<String>): String {
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

    private fun updateAlarmFromUI() {
        alarm = alarm.copy(
            time = LocalTime.of(timePicker.hour, timePicker.minute),
            label = labelEditText.text.toString(),
            repeatDays = selectedDays.toList(),
            vibrateOnAlarm = vibrateSwitch.isChecked,
            alarmSound = selectedAlarmSoundUri.toString()
        )
    }

    private fun handleFinalization() {
        val alarmViewModel = ViewModelProvider(this).get(AlarmViewModel::class.java)
        if (intent.hasExtra("alarm")) {
            alarmViewModel.updateAlarm(alarm)
        } else {
            alarmViewModel.addAlarm(alarm)
        }
        returnWithResult()
    }

    private fun returnWithResult() {
        val resultIntent = Intent().putExtra("updatedAlarm", alarm)
        setResult(RESULT_OK, resultIntent)
        finish()
    }

    private fun handleConfirmAddOrEditAlarm() {
        updateAlarmFromUI()

        if (!alarm.isEnabled) {
            cancelAlarm()
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

        // Display appropriate Toast message
        val action = if (intent.hasExtra("alarm")) "updated to" else "set to"
        Toast.makeText(
            this,
            "${alarm.label} $action ${alarm.time.format(DateTimeFormatter.ofPattern("h:mm a"))}",
            Toast.LENGTH_SHORT
        ).show()
    }


    private fun scheduleAlarm(triggerAt: Long) {
        val alarmManager = getSystemService(ALARM_SERVICE) as AlarmManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
            startActivity(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                data = Uri.parse("package:$packageName")
            })
            Toast.makeText(this, "Grant exact-alarm permission in settings.", Toast.LENGTH_LONG).show()
            return
        }

        val pi = PendingIntent.getBroadcast(
            this,
            alarm.id.hashCode(),
            Intent(this, AlarmReceiver::class.java).apply {
                putExtra("ALARM_LABEL", alarm.label)
                putExtra("ALARM_ID", alarm.id)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pi)
        } else {
            alarmManager.setExact(AlarmManager.RTC_WAKEUP, triggerAt, pi)
        }
    }

    private fun cancelAlarm() {
        val alarmManager = getSystemService(ALARM_SERVICE) as AlarmManager
        val pi = PendingIntent.getBroadcast(
            this,
            alarm.id,
            Intent(this, AlarmReceiver::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pi)
    }

    private fun calculateNextTriggerTime(): Long? {
        val triggerCal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, timePicker.hour)
            set(Calendar.MINUTE, timePicker.minute)
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
                    set(Calendar.HOUR_OF_DAY, timePicker.hour)
                    set(Calendar.MINUTE, timePicker.minute)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                if (checkCal.timeInMillis > System.currentTimeMillis()) return checkCal.timeInMillis
            }
        }

        return null
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.action_bar_menu, menu)

        val settingsItem = menu?.findItem(R.id.action_settings)
        settingsItem?.isVisible = false

        val confirmAddEditAlarm = menu?.findItem(R.id.action_add)
        confirmAddEditAlarm?.setIcon(R.drawable.check_24px)

        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_add -> {
                handleConfirmAddOrEditAlarm()
                true
            }
            android.R.id.home -> {
                finish()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}