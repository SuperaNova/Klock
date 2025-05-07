package cit.edu.KlockApp.ui.main.alarm

import android.app.AlarmManager
import android.app.AlertDialog
import android.app.PendingIntent
import android.content.Intent
import android.media.Ringtone
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
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
import com.google.android.material.button.MaterialButton
import com.google.android.material.switchmaterial.SwitchMaterial
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Calendar
import com.google.android.material.appbar.MaterialToolbar
import android.content.SharedPreferences
import androidx.preference.PreferenceManager
import cit.edu.KlockApp.ui.settings.ProfileActivity

class AlarmActivity : AppCompatActivity() {

    protected lateinit var timePicker: TimePicker
    private lateinit var repeatButton: MaterialButton
    private lateinit var alarmSoundButton: MaterialButton
    protected lateinit var labelEditText: EditText
    private lateinit var snoozeSpinner: Spinner
    private lateinit var vibrateSwitch: SwitchMaterial
    protected lateinit var alarm: Alarm
    private var selectedAlarmSoundUri: Uri? = null
    private var currentPreviewRingtone: Ringtone? = null
    protected var selectedDays = mutableListOf<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        // Apply FULL theme from prefs BEFORE super.onCreate()
        applyAppTheme()

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_alarm)

        // Setup the toolbar from the layout
        val toolbar: MaterialToolbar = findViewById(R.id.toolbar_alarm)
        setSupportActionBar(toolbar)

        supportActionBar?.title = "Create Alarm"

        // Enable the "up button" (back button) in the ActionBar
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)

        alarm = initAlarm()

        // Initialize UI components after alarm is initialized
        initializeViews()
        setupSnoozeSpinner()
        setupRepeatDialog()
    }

    // Function to apply FULL theme based on SharedPreferences
    private fun applyAppTheme() {
        val sharedPreferences: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)
        // Read the saved theme *Resource ID*
        val themeResId = sharedPreferences.getInt(ProfileActivity.PREF_KEY_THEME_ID, ProfileActivity.THEME_DEFAULT_ID)
        setTheme(themeResId) // Apply the chosen FULL theme
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
                isExpanded = false
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

            val cursor = ringtoneManager.cursor
            val soundTitles = mutableListOf<String>()
            val soundUris = mutableListOf<Uri>()

            try {
                while (cursor.moveToNext()) {
                    val title = cursor.getString(RingtoneManager.TITLE_COLUMN_INDEX) ?: "Unknown Sound"
                    val uri = ringtoneManager.getRingtoneUri(cursor.position)
                    soundTitles.add(title)
                    soundUris.add(uri)
                }
            } finally {
                cursor.close()
            }

            if (soundTitles.isEmpty()) {
                Toast.makeText(this, "No alarm sounds found.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val initialSelectedUriString = alarm.alarmSound ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM).toString()
            var currentSelectedSoundIndex = soundUris.indexOfFirst { it.toString() == initialSelectedUriString }.takeIf { it >= 0 } ?: 0
            // Ensure currentSelectedSoundIndex is valid, default to 0 if not found or list is small
            if (currentSelectedSoundIndex >= soundUris.size) currentSelectedSoundIndex = 0

            // Store the URI that was selected when the dialog opened, for reversion on cancel.
            val originalSoundUriForDialog = if(soundUris.isNotEmpty()) soundUris[currentSelectedSoundIndex] else null

            val dialog = AlertDialog.Builder(this)
                .setTitle("Select Alarm Sound")
                .setSingleChoiceItems(soundTitles.toTypedArray(), currentSelectedSoundIndex) { _, which ->
                    currentSelectedSoundIndex = which // Update the persistently tracked index
                    // Play preview
                    currentPreviewRingtone?.stop()
                    currentPreviewRingtone = null
                    if (which >= 0 && which < soundUris.size) {
                        try {
                            currentPreviewRingtone = RingtoneManager.getRingtone(this, soundUris[which])
                            currentPreviewRingtone?.play()
                        } catch (e: Exception) {
                            // Log error or show toast if preview fails
                        }
                    }
                }
                .setPositiveButton("Done") { _, _ ->
                    if (currentSelectedSoundIndex >= 0 && currentSelectedSoundIndex < soundUris.size) {
                        selectedAlarmSoundUri = soundUris[currentSelectedSoundIndex]
                        alarm = alarm.copy(alarmSound = selectedAlarmSoundUri.toString())
                        alarmSoundButton.text = soundTitles[currentSelectedSoundIndex]
                    } else if (soundUris.isNotEmpty()) { // Fallback if somehow index is bad but list not empty
                        selectedAlarmSoundUri = soundUris[0]
                        alarm = alarm.copy(alarmSound = selectedAlarmSoundUri.toString())
                        alarmSoundButton.text = soundTitles[0]
                    }
                }
                .setNegativeButton("Cancel") { _, _ ->
                    // Revert to the sound that was selected when the dialog was opened
                    selectedAlarmSoundUri = originalSoundUriForDialog
                    alarm = alarm.copy(alarmSound = originalSoundUriForDialog.toString())
                    val originalIndex = soundUris.indexOf(originalSoundUriForDialog)
                    if (originalIndex != -1 && originalIndex < soundTitles.size) {
                        alarmSoundButton.text = soundTitles[originalIndex]
                    } else if (soundTitles.isNotEmpty()){
                        alarmSoundButton.text = soundTitles[0] // Fallback to first sound title
                    }
                }
                .setOnDismissListener { // Handles cancel, back press, and button clicks
                    currentPreviewRingtone?.stop()
                    currentPreviewRingtone = null
                }
                .create()
            dialog.show()
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

        val editItem = menu?.findItem(R.id.action_edit)
        editItem?.isVisible = false

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