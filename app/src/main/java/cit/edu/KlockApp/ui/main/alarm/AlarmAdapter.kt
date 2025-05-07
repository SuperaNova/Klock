package cit.edu.KlockApp.ui.main.alarm

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.view.LayoutInflater
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.TimePicker
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import cit.edu.KlockApp.R
import cit.edu.KlockApp.databinding.AlarmItemBinding
import cit.edu.KlockApp.ui.settings.SettingsActivity
import com.google.android.material.button.MaterialButton
import java.time.format.DateTimeFormatter
import java.util.Locale
import androidx.preference.PreferenceManager

class AlarmAdapter(
    private val onToggleEnabled: (Alarm) -> Unit,
    private val onExpandToggled: (Alarm) -> Unit,
    private val onLabelChanged: (Alarm) -> Unit,
    private val onAlarmTimeAdjust: (Alarm) -> Unit,
    private val onVibrateToggle: (Alarm) -> Unit
) : ListAdapter<Alarm, AlarmAdapter.ViewHolder>(AlarmDiff()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = ViewHolder(
        AlarmItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
    )

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(private val b: AlarmItemBinding) :
        RecyclerView.ViewHolder(b.root) {

        private val dayMap = mapOf(
            R.id.button_sunday to "Sunday",
            R.id.button_monday to "Monday",
            R.id.button_tuesday to "Tuesday",
            R.id.button_wednesday to "Wednesday",
            R.id.button_thursday to "Thursday",
            R.id.button_friday to "Friday",
            R.id.button_saturday to "Saturday"
        )

        fun bind(a: Alarm) {
            val context = b.root.context
            val prefs = PreferenceManager.getDefaultSharedPreferences(context)
            val use24HourFormat = prefs.getBoolean(SettingsActivity.PREF_KEY_24_HOUR, false)

            val timeFmtPattern = if (use24HourFormat) "HH:mm" else "hh:mm"
            val timeFmt = DateTimeFormatter.ofPattern(timeFmtPattern, Locale.getDefault())
            val ampmFmt = DateTimeFormatter.ofPattern("a", Locale.getDefault())

            b.toggleButtonGroup.clearOnButtonCheckedListeners()

            // Set toggle state based on Alarm.repeatDays
            dayMap.forEach { (buttonId, day) ->
                val button = b.toggleButtonGroup.findViewById<MaterialButton>(buttonId)
                button.isChecked = a.repeatDays.contains(day)
            }

            // Header
            b.alarmTime.text = a.time.format(timeFmt)
            if (use24HourFormat) {
                b.alarmAmPm.isVisible = false
            } else {
                b.alarmAmPm.text = a.time.format(ampmFmt).uppercase(Locale.getDefault())
                b.alarmAmPm.isVisible = b.alarmAmPm.text.isNotBlank()
            }
            b.alarmLabel.text = a.label

            b.alarmRepeatInfo.text = when {
                a.repeatDays.isEmpty() -> "Once"
                a.repeatDays.size == 7 -> "Everyday"
                else -> a.repeatDays.joinToString(", ") { it.take(3) }
            }

            // Handle ToggleButtonGroup changes for repeatDays
            b.toggleButtonGroup.addOnButtonCheckedListener { _, _, _ ->
                val selectedDays = dayMap
                    .filter { (id, _) -> b.toggleButtonGroup.findViewById<MaterialButton>(id).isChecked }
                    .values
                    .toList()

                if (selectedDays != a.repeatDays) {
                    onAlarmTimeAdjust(a.copy(repeatDays = selectedDays))
                }
            }

            b.alarmEnabledSwitch.apply {
                setOnCheckedChangeListener(null)
                isChecked = a.isEnabled
                setOnCheckedChangeListener { _, isOn ->
                    onToggleEnabled(a.copy(isEnabled = isOn))
                }
            }

            // Set the checkbox state when binding the view
            b.vibrateCheckbox.apply {
                setOnCheckedChangeListener(null)
                isChecked = a.vibrateOnAlarm
                setOnCheckedChangeListener { _, isOn ->
                    onToggleEnabled(a.copy(vibrateOnAlarm = isOn))
                }
            }

            // Set expanded/collapsed content visibility based on `isExpanded`
            b.expandedContentGroup.isVisible = a.isExpanded
            b.divider.isVisible = a.isExpanded
            b.expandIcon.rotation = if (a.isExpanded) 180f else 0f

            // Make the whole header clickable for expand/collapse
            b.collapsedContentGroup.setOnClickListener {
                onExpandToggled(a)
            }

            // Handle label change
            b.alarmChangeLabel.setOnClickListener {
                val ctx = it.context
                val input = EditText(ctx).apply {
                    setText(a.label)
                    setSelection(text.length)
                    isSingleLine = true
                    maxLines = 1
                    layoutParams = FrameLayout.LayoutParams(
                        FrameLayout.LayoutParams.MATCH_PARENT,
                        FrameLayout.LayoutParams.WRAP_CONTENT
                    )
                }
                val container = FrameLayout(ctx).apply {
                    setPadding(48, 16, 48, 0)
                    addView(input)
                }
                val dialog = AlertDialog.Builder(ctx)
                    .setTitle("Edit Alarm Label")
                    .setView(container)
                    .setPositiveButton("Done") { _, _ ->
                        val newLabel = input.text.toString().trim()
                        if (newLabel.isNotEmpty() && newLabel != a.label) {
                            onLabelChanged(a.copy(label = newLabel))
                        }
                    }
                    .setNegativeButton("Cancel", null)
                    .create()

                dialog.show()
                dialog.window?.setBackgroundDrawableResource(R.drawable.rounded_dialog_bg)
                input.requestFocus()
                dialog.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE)
            }

            // Handle time change
            b.alarmChangeTime.setOnClickListener {
                val ctx = it.context
                val currentPrefs = PreferenceManager.getDefaultSharedPreferences(ctx)
                val is24HourDialog = currentPrefs.getBoolean(SettingsActivity.PREF_KEY_24_HOUR, false)

                val timePicker = TimePicker(ctx).apply {
                    setIs24HourView(is24HourDialog)
                    hour = a.time.hour
                    minute = a.time.minute
                    layoutParams = FrameLayout.LayoutParams(
                        FrameLayout.LayoutParams.MATCH_PARENT,
                        FrameLayout.LayoutParams.WRAP_CONTENT
                    )
                }
                val container = FrameLayout(ctx).apply {
                    setPadding(24, 16, 24, 16)
                    addView(timePicker)
                }
                val dialog = AlertDialog.Builder(ctx)
                    .setTitle("Edit Alarm Time")
                    .setView(container)
                    .setPositiveButton("Done") { _, _ ->
                        val newTime = a.time.withHour(timePicker.hour).withMinute(timePicker.minute)
                        if (newTime != a.time) {
                            onAlarmTimeAdjust(a.copy(time = newTime))
                        }
                    }
                    .setNegativeButton("Cancel", null)
                    .create()

                dialog.show()
                dialog.window?.setBackgroundDrawableResource(R.drawable.rounded_dialog_bg)
            }
        }
    }

    private class AlarmDiff : DiffUtil.ItemCallback<Alarm>() {
        override fun areItemsTheSame(oldItem: Alarm, newItem: Alarm) =
            oldItem.id == newItem.id

        @SuppressLint("DiffUtilEquals")
        override fun areContentsTheSame(oldItem: Alarm, newItem: Alarm): Boolean {
            return oldItem.id == newItem.id &&
                    oldItem.label == newItem.label &&
                    oldItem.time == newItem.time &&
                    oldItem.isEnabled == newItem.isEnabled &&
                    oldItem.repeatDays == newItem.repeatDays &&
                    oldItem.vibrateOnAlarm == newItem.vibrateOnAlarm &&
                    oldItem.isExpanded == newItem.isExpanded
        }
    }
}