package cit.edu.KlockApp.ui.main.alarm

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import cit.edu.KlockApp.R
import cit.edu.KlockApp.databinding.AlarmItemBinding
import com.google.android.material.button.MaterialButtonToggleGroup
import java.time.DayOfWeek
import java.time.format.DateTimeFormatter
import java.util.Locale

// Callbacks for interaction
interface AlarmInteractionListener {
    fun onToggle(alarm: Alarm, isEnabled: Boolean)
    fun onDelete(alarm: Alarm)
    fun onEdit(alarm: Alarm) // Maybe needed later
    fun onVibrateChanged(alarm: Alarm, vibrate: Boolean) // Add vibrate callback
    // Add other callbacks if needed (e.g., changing label, ringtone)
}

// Changed to ListAdapter for easier updates
class AlarmRecyclerAdapter(
    private val listener: AlarmInteractionListener
) : ListAdapter<Alarm, AlarmRecyclerAdapter.ViewHolder>(AlarmDiffCallback()) {

    private val expandedItems = mutableSetOf<Int>() // Use alarm ID for tracking

    inner class ViewHolder(val binding: AlarmItemBinding) : RecyclerView.ViewHolder(binding.root) {

        private val timeFormatter = DateTimeFormatter.ofPattern("h:mm", Locale.getDefault())
        private val amPmFormatter = DateTimeFormatter.ofPattern("a", Locale.getDefault())
        private val dayButtonMap: Map<DayOfWeek, Button>

        init {
            // Map DayOfWeek to Button IDs
            dayButtonMap = mapOf(
                DayOfWeek.SUNDAY to binding.buttonSunday,
                DayOfWeek.MONDAY to binding.buttonMonday,
                DayOfWeek.TUESDAY to binding.buttonTuesday,
                DayOfWeek.WEDNESDAY to binding.buttonWednesday,
                DayOfWeek.THURSDAY to binding.buttonThursday,
                DayOfWeek.FRIDAY to binding.buttonFriday,
                DayOfWeek.SATURDAY to binding.buttonSaturday
            )

            // Root click listener for expansion
            binding.root.setOnClickListener {
                val position = bindingAdapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    toggleExpansion(getItem(position), position)
                }
            }

            // Switch listener
            binding.alarmEnabledSwitch.setOnCheckedChangeListener { _, isChecked ->
                 val position = bindingAdapterPosition
                 if (position != RecyclerView.NO_POSITION) {
                     listener.onToggle(getItem(position), isChecked)
                 }
            }

            // Delete button listener
             binding.deleteButton.setOnClickListener {
                 val position = bindingAdapterPosition
                 if (position != RecyclerView.NO_POSITION) {
                     listener.onDelete(getItem(position))
                     // Optionally collapse after delete?
                     if (expandedItems.contains(getItem(position).id)) {
                         expandedItems.remove(getItem(position).id)
                         // No need to notify here, list update handles it
                     }
                 }
             }

             // TODO: Add listeners for other elements (Repeat checkbox, Day toggles, Ringtone, Label EditText)
             // These listeners should likely call methods on the listener interface to update the ViewModel/Storage
             binding.toggleButtonGroup.addOnButtonCheckedListener { group, checkedId, isChecked ->
                // Handle day toggle changes
                // This requires mapping checkedId back to DayOfWeek and updating alarm.repeatDays
                // Then call a listener method e.g., listener.onRepeatDaysChanged(alarm, updatedDays)
             }
             binding.repeatCheckbox.setOnCheckedChangeListener { _, isChecked ->
                 // Show/Hide toggle group, potentially update alarm state if needed
                 binding.toggleButtonGroup.visibility = if (isChecked) View.VISIBLE else View.GONE
                 // Maybe update alarm.repeatDays based on checkbox state?
             }
             binding.vibrateCheckbox.setOnCheckedChangeListener { _, isChecked ->
                 val position = bindingAdapterPosition
                 if (position != RecyclerView.NO_POSITION) {
                     listener.onVibrateChanged(getItem(position), isChecked)
                 }
             }
             // Example: binding.alarmLabelEditText.doAfterTextChanged { ... listener.onLabelChanged(...) }
             // Example: binding.ringtoneText.setOnClickListener { ... listener.onRingtoneClicked(...) }
        }


        fun bind(alarm: Alarm) {
            // Bind collapsed view data
            binding.alarmTime.text = alarm.time.format(timeFormatter)
            binding.alarmAmPm.text = alarm.time.format(amPmFormatter).uppercase(Locale.getDefault())
            binding.alarmLabelRepeatInfo.text = formatLabelRepeatInfo(alarm)
            // Set switch state WITHOUT triggering listener during bind
            binding.alarmEnabledSwitch.setOnCheckedChangeListener(null)
            binding.alarmEnabledSwitch.isChecked = alarm.isEnabled
            binding.alarmEnabledSwitch.setOnCheckedChangeListener { _, isChecked ->
                listener.onToggle(alarm, isChecked)
            }

            // Bind expanded view data
            binding.repeatCheckbox.isChecked = alarm.repeatDays.isNotEmpty()
            binding.toggleButtonGroup.visibility = if (alarm.repeatDays.isNotEmpty()) View.VISIBLE else View.GONE
            updateDayToggleGroup(alarm.repeatDays)
            // Set vibrate state WITHOUT triggering listener during bind
            binding.vibrateCheckbox.setOnCheckedChangeListener(null)
            binding.vibrateCheckbox.isChecked = alarm.vibrate
            binding.vibrateCheckbox.setOnCheckedChangeListener { _, isChecked ->
                val position = bindingAdapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    listener.onVibrateChanged(getItem(position), isChecked)
                }
            }
            binding.alarmLabelEditText.setText(alarm.label)
            // TODO: Bind ringtone text (e.g., get ringtone name from URI)
            // binding.ringtoneText.text = getRingtoneDisplayName(itemView.context, alarm.ringtoneUri)

            // Handle expansion state
            updateExpansionState(alarm, false) // Don't animate on initial bind
        }

         private fun updateDayToggleGroup(repeatDaysList: List<String>) {
             // Convert List<String> to Set<DayOfWeek> inside the function
             val repeatDaysSet = repeatDaysList.mapNotNull { dayString ->
                 try {
                     // Assuming strings match DayOfWeek enum names (e.g., "MONDAY", "TUESDAY")
                     DayOfWeek.valueOf(dayString.uppercase(Locale.ROOT))
                 } catch (e: IllegalArgumentException) {
                     null // Ignore invalid strings
                 }
             }.toSet()

             binding.toggleButtonGroup.clearChecked() // Clear previous state
             repeatDaysSet.forEach { day -> // Use the converted Set
                 dayButtonMap[day]?.let { button ->
                     binding.toggleButtonGroup.check(button.id)
                 }
             }
         }

        private fun formatLabelRepeatInfo(alarm: Alarm): String {
            val labelPart = if (alarm.label.isNotBlank()) alarm.label else "Alarm"
            val repeatPart = formatRepeatDaysShort(alarm.repeatDays)
            return "$labelPart, $repeatPart"
        }

        private fun formatRepeatDaysShort(daysList: List<String>): String {
             if (daysList.isEmpty()) return "Once"
             if (daysList.size == 7) return "Every day"

             // Convert to DayOfWeek for sorting and formatting
             val daysOfWeek = daysList.mapNotNull {
                 try {
                      // Assuming strings are full uppercase names like "MONDAY"
                      DayOfWeek.valueOf(it.uppercase(Locale.ROOT))
                 } catch (e: IllegalArgumentException) { null }
             }.toSet()

             // Simple comma-separated list for collapsed view
             return daysOfWeek.sorted() // Sort for consistent order
                 .joinToString(", ") { it.name.substring(0, 3) } // "Mon, Tue, Wed"
        }

        private fun updateExpansionState(alarm: Alarm, animate: Boolean) {
            val isExpanded = expandedItems.contains(alarm.id)
            binding.expandedContentGroup.visibility = if (isExpanded) View.VISIBLE else View.GONE
            binding.divider.visibility = if (isExpanded) View.VISIBLE else View.GONE
            // Rotate expand icon (optional animation)
            binding.expandIcon.rotation = if (isExpanded) 180f else 0f
             // TODO: Add animation if 'animate' is true
        }

         private fun toggleExpansion(alarm: Alarm, position: Int) {
             val isExpanding = !expandedItems.contains(alarm.id)
             if (isExpanding) {
                 expandedItems.add(alarm.id)
             } else {
                 expandedItems.remove(alarm.id)
             }
             updateExpansionState(alarm, true) // Animate the change
             // Consider notifying only this item if animation is smooth
             // notifyItemChanged(position) // May cause flicker with animations
         }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = AlarmItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
}

// Updated DiffUtil Callback for ListAdapter
class AlarmDiffCallback : DiffUtil.ItemCallback<Alarm>() {
    override fun areItemsTheSame(oldItem: Alarm, newItem: Alarm): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(oldItem: Alarm, newItem: Alarm): Boolean {
        return oldItem == newItem // Assumes Alarm is a data class
    }
}
