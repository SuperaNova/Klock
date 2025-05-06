package cit.edu.KlockApp.ui.main.alarm

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.EditText
import android.widget.FrameLayout
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import cit.edu.KlockApp.R
import cit.edu.KlockApp.databinding.AlarmItemBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java.time.format.DateTimeFormatter
import java.util.Locale

class AlarmAdapter(
    private val onToggleEnabled: (Alarm) -> Unit,
    private val onDelete: (Alarm) -> Unit,
    private val onExpandToggled: (Alarm) -> Unit,
    private val onLabelChanged: (Alarm) -> Unit
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

        fun bind(a: Alarm) {
            // Header
            b.alarmTime.text      = a.time.format(timeFmt)
            b.alarmAmPm.text      = a.time.format(ampmFmt).uppercase(Locale.getDefault())
            b.alarmAmPm.isVisible = b.alarmAmPm.text.isNotBlank()
            b.alarmLabel.text      = a.label
            b.alarmRepeatInfo.text = when {
                a.repeatDays.isEmpty() -> "Once"
                a.repeatDays.size == 7 -> "Everyday"
                else                   -> a.repeatDays.joinToString(", ") { it.take(3) }
            }

            // Enabled
            b.alarmEnabledSwitch.isChecked = a.isEnabled
            b.alarmEnabledSwitch.setOnCheckedChangeListener { _, isOn ->
                a.isEnabled = isOn
                onToggleEnabled(a)
            }

            // Expanded details
            b.expandedContentGroup.isVisible = a.isExpanded
            b.divider.isVisible               = a.isExpanded
            b.expandIcon.rotation             = if (a.isExpanded) 180f else 0f
            b.expandIcon.setOnClickListener   { onExpandToggled(a) }

            b.alarmChangeLabel.setOnClickListener {
                val ctx = it.context

                val input = EditText(ctx).apply {
                    setText(a.label)
                    setSelection(text.length)
                    isSingleLine = true  // Ensures only one line
                    maxLines = 1         // Extra precaution
                    layoutParams = FrameLayout.LayoutParams(
                        FrameLayout.LayoutParams.MATCH_PARENT,
                        FrameLayout.LayoutParams.WRAP_CONTENT
                    )
                }

                // Wrap EditText with margin in a FrameLayout
                val container = FrameLayout(ctx).apply {
                    setPadding(48, 16, 48, 0) // left, top, right, bottom padding in pixels
                    addView(input)
                }

                val dialog = MaterialAlertDialogBuilder(ctx)
                    .setTitle("Edit Alarm Label")
                    .setView(container)
                    .setPositiveButton("Done") { _, _ ->
                        val newLabel = input.text.toString().trim()
                        if (newLabel.isNotEmpty() && newLabel != a.label) {
                            onLabelChanged(a.copy(label = newLabel))
                        }
                    }
                    .setNegativeButton("Cancel", null)
                    .show()

                dialog.window?.setBackgroundDrawableResource(R.drawable.rounded_dialog_bg)
            }



            // Delete
            b.deleteButton.setOnClickListener { onDelete(a) }
        }
    }

    private class AlarmDiff : DiffUtil.ItemCallback<Alarm>() {
        override fun areItemsTheSame(oldItem: Alarm, newItem: Alarm) =
            oldItem.id == newItem.id

        override fun areContentsTheSame(oldItem: Alarm, newItem: Alarm) =
            oldItem == newItem
    }
}
