package cit.edu.KlockApp.ui.main.alarm

import android.app.AlertDialog
import android.view.ContextThemeWrapper
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
import com.google.android.material.button.MaterialButton
import java.time.format.DateTimeFormatter
import java.util.Locale

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

        private val timeFmt  = DateTimeFormatter.ofPattern("hh:mm", Locale.getDefault())
        private val ampmFmt  = DateTimeFormatter.ofPattern("a",    Locale.getDefault())

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
            // Set toggle state based on Alarm.repeatDays
            dayMap.forEach { (buttonId, day) ->
                val button = b.toggleButtonGroup.findViewById<MaterialButton>(buttonId)
                button.isChecked = a.repeatDays.contains(day)
            }

            // Header
            b.alarmTime.text = a.time.format(timeFmt)
            b.alarmAmPm.text = a.time.format(ampmFmt).uppercase(Locale.getDefault())
            b.alarmAmPm.isVisible = b.alarmAmPm.text.isNotBlank()
            b.alarmLabel.text = a.label

            b.alarmRepeatInfo.text = when {
                a.repeatDays.isEmpty() -> "Once"
                a.repeatDays.size == 7 -> "Everyday"
                else -> a.repeatDays.joinToString(", ") { it.take(3) }
            }

            // Handle ToggleButtonGroup changes for repeatDays
            b.toggleButtonGroup.addOnButtonCheckedListener { _, _, _ ->
                // Get updated repeatDays from UI
                val selectedDays = dayMap.filter { (id, _) ->
                    b.toggleButtonGroup.findViewById<MaterialButton>(id).isChecked
                }.values.toList()

                // Only update if changed to avoid redundant saves
                if (selectedDays != a.repeatDays) {
                    val updatedAlarm = a.copy(repeatDays = selectedDays)
                    onAlarmTimeAdjust(updatedAlarm)
                }
            }

            // In your AlarmAdapter, bind isEnabled to the checkbox
            b.alarmEnabledSwitch.isChecked = a.isEnabled
            b.alarmEnabledSwitch.setOnCheckedChangeListener { _, isOn ->
                val updatedAlarm = a.copy(isEnabled = isOn)
                onToggleEnabled(updatedAlarm)
            }

            // Set the checkbox state when binding the view
            b.vibrateCheckbox.isChecked = a.vibrateOnAlarm
            b.vibrateCheckbox.setOnCheckedChangeListener { _, isOn ->
                // Only update the vibrate property and keep other properties unchanged
                val updated = a.copy(vibrateOnAlarm = isOn)
                onVibrateToggle(updated)  // This updates only the vibrateOnAlarm property
            }




            // Set expanded/collapsed content visibility based on `isExpanded`
            b.expandedContentGroup.isVisible = a.isExpanded
            b.divider.isVisible = a.isExpanded
            b.expandIcon.rotation = if (a.isExpanded) 180f else 0f

            // Expand icon click listener to toggle the `isExpanded` state
            b.expandIcon.setOnClickListener {
                onExpandToggled(a)  // This will toggle the expansion explicitly
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
                val timePicker = TimePicker(ContextThemeWrapper(ctx, android.R.style.Theme_Holo_Light_Dialog)).apply {
                    setIs24HourView(false)
                    hour = a.time.hour
                    minute = a.time.minute
                    layoutParams = FrameLayout.LayoutParams(
                        FrameLayout.LayoutParams.MATCH_PARENT,
                        FrameLayout.LayoutParams.WRAP_CONTENT
                    )
                }
                val container = FrameLayout(ctx).apply {
                    setPadding(48, 16, 48, 0)
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
