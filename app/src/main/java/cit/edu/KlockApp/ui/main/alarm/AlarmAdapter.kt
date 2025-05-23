package cit.edu.KlockApp.ui.main.alarm

import android.app.AlertDialog
import android.content.Context
import android.media.Ringtone
import android.media.RingtoneManager
import android.net.Uri
import android.view.ContextThemeWrapper
import android.view.LayoutInflater
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.TimePicker
import androidx.core.view.isVisible
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import cit.edu.KlockApp.R
import cit.edu.KlockApp.databinding.AlarmItemBinding
import cit.edu.KlockApp.ui.settings.SettingsActivity
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java.time.format.DateTimeFormatter
import java.util.Locale

class AlarmAdapter(
    private val onToggleEnabled: (Alarm) -> Unit,
    private val onExpandToggled: (Alarm) -> Unit,
    private val onLabelChanged: (Alarm) -> Unit,
    private val onAlarmTimeAdjust: (Alarm) -> Unit,
    private val onAlarmSoundChange: (Alarm) -> Unit,
    private val onVibrateToggle: (Alarm) -> Unit
) : ListAdapter<Alarm, AlarmAdapter.ViewHolder>(AlarmDiff()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        ViewHolder(AlarmItemBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(private val b: AlarmItemBinding) : RecyclerView.ViewHolder(b.root) {
        private var previewRingtone: Ringtone? = null

        private val dayMap = mapOf(
            R.id.button_sunday    to "Sunday",
            R.id.button_monday    to "Monday",
            R.id.button_tuesday   to "Tuesday",
            R.id.button_wednesday to "Wednesday",
            R.id.button_thursday  to "Thursday",
            R.id.button_friday    to "Friday",
            R.id.button_saturday  to "Saturday"
        )

        fun bind(a: Alarm) {
            val context = b.root.context
            val prefs = PreferenceManager.getDefaultSharedPreferences(context)
            val use24HourFormat = prefs.getBoolean(SettingsActivity.PREF_KEY_24_HOUR, false)

            val timePattern = if (use24HourFormat) "HH:mm" else "hh:mm"
            val timeFmt = DateTimeFormatter.ofPattern(timePattern, Locale.getDefault())
            val ampmFmt = DateTimeFormatter.ofPattern("a", Locale.getDefault())

            // repeat-days toggles
            b.toggleButtonGroup.clearOnButtonCheckedListeners()
            dayMap.forEach { (id, day) ->
                b.toggleButtonGroup.findViewById<MaterialButton>(id).isChecked =
                    a.repeatDays.contains(day)
            }
            b.toggleButtonGroup.addOnButtonCheckedListener { _, _, _ ->
                val p = bindingAdapterPosition.takeIf { it != RecyclerView.NO_POSITION } ?: return@addOnButtonCheckedListener
                val fresh = getItem(p)
                val sel = dayMap.filter { (id, _) ->
                    b.toggleButtonGroup.findViewById<MaterialButton>(id).isChecked
                }.values.toList()
                if (sel != fresh.repeatDays) {
                    onAlarmTimeAdjust(fresh.copy(repeatDays = sel))
                }
            }

            // header
            b.alarmTime.text       = a.time.format(timeFmt)
            if (use24HourFormat) {
                b.alarmAmPm.isVisible = false
            } else {
                b.alarmAmPm.text       = a.time.format(ampmFmt).uppercase(Locale.getDefault())
                b.alarmAmPm.isVisible  = b.alarmAmPm.text.isNotBlank()
            }
            b.alarmLabel.text      = a.label
            b.alarmRepeatInfo.text = when {
                a.repeatDays.isEmpty() -> "Once"
                a.repeatDays.size == 7 -> "Everyday"
                else                   -> a.repeatDays.joinToString(", ") { it.take(3) }
            }

            // enabled toggle
            b.alarmEnabledSwitch.setOnCheckedChangeListener(null)
            b.alarmEnabledSwitch.isChecked = a.isEnabled
            b.alarmEnabledSwitch.setOnCheckedChangeListener { _, on ->
                val p = bindingAdapterPosition.takeIf { it != RecyclerView.NO_POSITION } ?: return@setOnCheckedChangeListener
                val fresh = getItem(p)
                onToggleEnabled(fresh.copy(isEnabled = on))
            }

            // vibrate toggle
            b.vibrateCheckbox.setOnCheckedChangeListener(null)
            b.vibrateCheckbox.isChecked = a.vibrateOnAlarm
            b.vibrateCheckbox.setOnCheckedChangeListener { _, on ->
                val p = bindingAdapterPosition.takeIf { it != RecyclerView.NO_POSITION } ?: return@setOnCheckedChangeListener
                val fresh = getItem(p)
                onVibrateToggle(fresh.copy(vibrateOnAlarm = on))
            }

            // expand/collapse
            b.expandedContentGroup.isVisible = a.isExpanded
            b.divider.isVisible = a.isExpanded
            b.expandIcon.rotation = if (a.isExpanded) 180f else 0f
            b.collapsedContentGroup.setOnClickListener {
                onExpandToggled(a)
            }

            // edit label
            b.alarmChangeLabel.setOnClickListener {
                val p = bindingAdapterPosition.takeIf { it != RecyclerView.NO_POSITION } ?: return@setOnClickListener
                val fresh = getItem(p)
                val ctx = it.context
                val input = EditText(ctx).apply {
                    setText(fresh.label)
                    setSelection(text.length)
                    isSingleLine = true; maxLines = 1
                    layoutParams = FrameLayout.LayoutParams(
                        FrameLayout.LayoutParams.MATCH_PARENT,
                        FrameLayout.LayoutParams.WRAP_CONTENT
                    )
                }
                FrameLayout(ctx).apply {
                    setPadding(48, 16, 48, 0)
                    addView(input)
                }.let { container ->
                    MaterialAlertDialogBuilder(ctx)
                        .setTitle("Edit Alarm Label")
                        .setView(container)
                        .setPositiveButton("Done") { _, _ ->
                            val newLabel = input.text.toString().trim()
                            if (newLabel.isNotEmpty() && newLabel != fresh.label) {
                                onLabelChanged(fresh.copy(label = newLabel))
                            }
                        }
                        .setNegativeButton("Cancel", null)
                        .create().apply {
                            show()
                            window?.setBackgroundDrawableResource(R.drawable.rounded_dialog_bg)
                            input.requestFocus()
                            window?.setSoftInputMode(
                                WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE
                            )
                        }
                }
            }

            // edit time
            b.alarmChangeTime.setOnClickListener {
                val p = bindingAdapterPosition.takeIf { it != RecyclerView.NO_POSITION } ?: return@setOnClickListener
                val fresh = getItem(p)
                val currentPrefs = PreferenceManager.getDefaultSharedPreferences(context)
                val is24Hour = currentPrefs.getBoolean(SettingsActivity.PREF_KEY_24_HOUR, false)

                val timePicker = TimePicker(context).apply {
                    setIs24HourView(is24Hour)
                    hour = fresh.time.hour
                    minute = fresh.time.minute
                    layoutParams = FrameLayout.LayoutParams(
                        FrameLayout.LayoutParams.WRAP_CONTENT,
                        FrameLayout.LayoutParams.WRAP_CONTENT
                    )
                }
                val container = FrameLayout(context).apply {
                    setPadding(48, 16, 48, 0)
                    addView(timePicker, FrameLayout.LayoutParams(FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT, android.view.Gravity.CENTER))
                }

                MaterialAlertDialogBuilder(context)
                    .setTitle("Edit Alarm Time")
                    .setView(container)
                    .setPositiveButton("Done") { _, _ ->
                        val newTime = fresh.time
                            .withHour(timePicker.hour)
                            .withMinute(timePicker.minute)
                        if (newTime != fresh.time)
                            onAlarmTimeAdjust(fresh.copy(time = newTime))
                    }
                    .setNegativeButton("Cancel", null)
                    .create()
                    .apply {
                        show()
                    }
            }

            // edit sound
            b.alarmChangeSound.text = getTitleForUri(a.alarmSound)
            b.alarmChangeSound.setOnClickListener {
                val p = bindingAdapterPosition.takeIf { it != RecyclerView.NO_POSITION } ?: return@setOnClickListener
                val fresh = getItem(p)
                val ctx = it.context
                val rm = RingtoneManager(ctx).apply { setType(RingtoneManager.TYPE_ALARM) }
                val cursor = rm.cursor
                val titles = mutableListOf<String>()
                val uris   = mutableListOf<Uri>()
                while (cursor.moveToNext()) {
                    titles += cursor.getString(RingtoneManager.TITLE_COLUMN_INDEX)
                    uris   += rm.getRingtoneUri(cursor.position)
                }
                cursor.close()
                var selIdx = uris.indexOfFirst { it.toString() == fresh.alarmSound }
                    .takeIf { it >= 0 } ?: 0

                MaterialAlertDialogBuilder(ctx)
                    .setTitle("Select Alarm Sound")
                    .setSingleChoiceItems(titles.toTypedArray(), selIdx) { _, which ->
                        previewRingtone?.stop()
                        previewRingtone = RingtoneManager.getRingtone(ctx, uris[which]).apply { play() }
                        selIdx = which
                    }
                    .setPositiveButton("Done") { _, _ ->
                        previewRingtone?.stop()
                        onAlarmSoundChange(fresh.copy(alarmSound = uris[selIdx].toString()))
                    }
                    .setNegativeButton("Cancel") { _, _ -> previewRingtone?.stop() }
                    .setOnCancelListener { previewRingtone?.stop() }
                    .show()
            }
        }

        private fun getTitleForUri(uriString: String): String = try {
            RingtoneManager.getRingtone(b.root.context, Uri.parse(uriString))
                ?.getTitle(b.root.context) ?: "Default"
        } catch (e: Exception) {
            "Default"
        }
    }

    private class AlarmDiff : DiffUtil.ItemCallback<Alarm>() {
        override fun areItemsTheSame(oldItem: Alarm, newItem: Alarm) = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: Alarm, newItem: Alarm) = oldItem == newItem
    }
}

