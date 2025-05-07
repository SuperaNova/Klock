package cit.edu.KlockApp.ui.main.alarm

import android.app.AlarmManager
import android.app.AlertDialog
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.util.TypedValue
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.TimePicker
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import cit.edu.KlockApp.R
import cit.edu.KlockApp.databinding.ActivityAlarmBinding
import cit.edu.KlockApp.ui.main.alarm.notificationManager.AlarmScheduler
import cit.edu.KlockApp.ui.main.alarm.notificationManager.AlarmReceiver
import cit.edu.KlockApp.ui.settings.ProfileActivity
import com.google.android.material.switchmaterial.SwitchMaterial
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Calendar

class AddNewAlarmActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAlarmBinding

    protected lateinit var alarm: Alarm
    private var selectedAlarmSoundUri: Uri? = null
    protected var selectedDays = mutableListOf<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        applyAppTheme()
        super.onCreate(savedInstanceState)
        binding = ActivityAlarmBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbarAddAlarm)
        supportActionBar?.title = if (intent.hasExtra("alarm")) "Edit Alarm" else "Create Alarm"
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)

        alarm = initAlarm()

        initializeViews()
        setupSnoozeSpinner()
        setupRepeatDialog()
    }

    private fun applyAppTheme() {
        val sharedPreferences = getSharedPreferences("theme_prefs", Context.MODE_PRIVATE)
        val themeResId = sharedPreferences.getInt(ProfileActivity.PREF_KEY_THEME_ID, ProfileActivity.THEME_DEFAULT_ID)
        setTheme(themeResId)
        Log.d("AddNewAlarmActivity", "Theme applied: $themeResId")
    }

    private fun initAlarm(): Alarm {
        return if (intent.hasExtra("alarm")) {
            supportActionBar?.title = "Edit Alarm"
            intent.getParcelableExtra<Alarm>("alarm") ?: throw IllegalStateException("No alarm data found")
        } else {
            supportActionBar?.title = "Create Alarm"
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
        binding.labelEditText.setText(alarm.label)
        binding.timePicker.hour = alarm.time.hour
        binding.timePicker.minute = alarm.time.minute
        binding.vibrateSwitch.isChecked = alarm.vibrateOnAlarm

        binding.vibrateSwitch.setOnCheckedChangeListener { _, isChecked ->
            alarm = alarm.copy(vibrateOnAlarm = isChecked)
        }

        selectedAlarmSoundUri = Uri.parse(alarm.alarmSound)
        updateAlarmSoundButtonText()

        binding.alarmSoundButton.setOnClickListener {
            val ringtoneManager = RingtoneManager(this)
            ringtoneManager.setType(RingtoneManager.TYPE_ALARM)

            val cursor = ringtoneManager.cursor
            val soundTitles = mutableListOf<String>()
            val soundUris = mutableListOf<Uri>()

            while (cursor.moveToNext()) {
                val title = cursor.getString(RingtoneManager.TITLE_COLUMN_INDEX)
                val uri = ringtoneManager.getRingtoneUri(cursor.position)
                soundTitles.add(title)
                soundUris.add(uri)
            }
            cursor.close()

            var selectedSoundIndex = soundUris.indexOfFirst { it.toString() == selectedAlarmSoundUri.toString() }.takeIf { it >= 0 } ?: 0
            val currentSelectedIndex = selectedSoundIndex
            val currentSoundUri = selectedAlarmSoundUri

            MaterialAlertDialogBuilder(this)
                .setTitle("Select Alarm Sound")
                .setSingleChoiceItems(soundTitles.toTypedArray(), selectedSoundIndex) { _, which ->
                    selectedSoundIndex = which
                    selectedAlarmSoundUri = soundUris[which]
                }
                .setPositiveButton("Done") { _, _ ->
                    updateAlarmSoundButtonText(soundTitles.getOrNull(selectedSoundIndex))
                }
                .setNegativeButton("Cancel") { _, _ ->
                    selectedAlarmSoundUri = currentSoundUri
                    updateAlarmSoundButtonText(soundTitles.getOrNull(currentSelectedIndex))
                }
                .setOnCancelListener {
                    selectedAlarmSoundUri = currentSoundUri
                    updateAlarmSoundButtonText(soundTitles.getOrNull(currentSelectedIndex))
                }
                .show()
        }
        updateRepeatButtonText()
    }

    private fun updateAlarmSoundButtonText(soundTitle: String? = null) {
        if (soundTitle != null) {
            binding.alarmSoundButton.text = soundTitle
        } else {
            val ringtone = RingtoneManager.getRingtone(this, selectedAlarmSoundUri)
            binding.alarmSoundButton.text = ringtone?.getTitle(this) ?: "Select Sound"
        }
    }

    private fun setupSnoozeSpinner() {
        val options = arrayOf("5 minutes", "10 minutes", "15 minutes")
        binding.snoozeSpinner.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, options)
        val defaultSnooze = when (alarm.snoozeMinutes) {
            10 -> "10 minutes"
            15 -> "15 minutes"
            else -> "5 minutes"
        }
        binding.snoozeSpinner.setSelection(options.indexOf(defaultSnooze))

        binding.snoozeSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
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
        selectedDays.clear()
        selectedDays.addAll(alarm.repeatDays)
        binding.repeatButton.setOnClickListener {
            val days = arrayOf("Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday")
            val checked = BooleanArray(days.size) { selectedDays.contains(days[it]) }
            MaterialAlertDialogBuilder(this)
                .setTitle("Repeat")
                .setMultiChoiceItems(days, checked) { _, which, isChecked ->
                    if (isChecked) selectedDays.add(days[which]) else selectedDays.remove(days[which])
                }
                .setPositiveButton("Done") { _, _ -> updateRepeatButtonText() }
                .setNegativeButton("Cancel", null)
                .show()
        }
    }

    private fun updateRepeatButtonText() {
        binding.repeatButton.text = formatSelectedDays(selectedDays)
    }

    private fun formatSelectedDays(days: List<String>): String {
        if (days.isEmpty()) return "Once"
        if (days.size == 7) return "Everyday"
        if (days.size == 2 && days.containsAll(listOf("Saturday", "Sunday"))) return "Weekends"
        if (days.size == 5 && days.containsAll(listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday"))) return "Weekdays"

        val orderedFull = listOf("Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday")
        val orderedShort = listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")
        val sortedDays = days.sortedBy { orderedFull.indexOf(it) }

        return sortedDays.joinToString(", ") { day ->
            orderedShort[orderedFull.indexOf(day)]
        }
    }

    private fun updateAlarmFromUI() {
        alarm = alarm.copy(
            time = LocalTime.of(binding.timePicker.hour, binding.timePicker.minute),
            label = binding.labelEditText.text.toString(),
            repeatDays = selectedDays.toList(),
            vibrateOnAlarm = binding.vibrateSwitch.isChecked,
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
            AlarmScheduler.cancel(this, alarm)
            returnWithResult()
            return
        }

        val alarmManager = getSystemService(ALARM_SERVICE) as AlarmManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
            startActivity(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                data = Uri.parse("package:$packageName")
            })
            Toast.makeText(this, "Please grant exact alarm permission and try again.", Toast.LENGTH_LONG).show()
            return
        }

        AlarmScheduler.schedule(this, alarm)
        Log.d("AlarmScheduler", "Alarm is Scheduled via AddNewAlarmActivity")
        
        handleFinalization()

        val action = if (intent.hasExtra("alarm")) "updated to" else "set to"
        Toast.makeText(
            this,
            "${alarm.label} $action ${alarm.time.format(DateTimeFormatter.ofPattern("h:mm a"))}",
            Toast.LENGTH_SHORT
        ).show()
    }

    private fun cancelAlarm() {
        AlarmScheduler.cancel(this, alarm)
        Log.d("AlarmScheduler", "Alarm explicitly cancelled via AddNewAlarmActivity")
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.action_bar_menu, menu)

        val settingsItem = menu?.findItem(R.id.action_settings)
        settingsItem?.isVisible = false

        val confirmAddEditAlarmItem = menu?.findItem(R.id.action_add)
        confirmAddEditAlarmItem?.setIcon(R.drawable.check_24px)
        confirmAddEditAlarmItem?.title = if (intent.hasExtra("alarm")) "Save" else "Add"

        val typedValue = TypedValue()
        theme.resolveAttribute(com.google.android.material.R.attr.colorOnPrimary, typedValue, true)
        val colorOnPrimary = ContextCompat.getColor(this, typedValue.resourceId)
        confirmAddEditAlarmItem?.icon?.colorFilter = PorterDuffColorFilter(colorOnPrimary, PorterDuff.Mode.SRC_IN)

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