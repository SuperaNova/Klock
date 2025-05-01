package cit.edu.KlockApp.ui.main.timer

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import androidx.core.content.ContextCompat
import cit.edu.KlockApp.R
import cit.edu.KlockApp.databinding.TimerPresetAddItemBinding
import cit.edu.KlockApp.databinding.TimerPresetItemBinding
import java.util.concurrent.TimeUnit

class TimerPresetAdapter(
    private val onPresetClick: (TimerPreset) -> Unit,
    private val onAddClick: () -> Unit,
    private val onPresetLongClick: (TimerPreset) -> Unit // Add long click listener
) : ListAdapter<TimerPreset, RecyclerView.ViewHolder>(PresetDiffCallback()) {

    companion object {
        private const val VIEW_TYPE_PRESET = 0
        private const val VIEW_TYPE_ADD = 1
    }

    override fun getItemViewType(position: Int): Int {
        // The last item is always the Add button
        return if (position == itemCount - 1) VIEW_TYPE_ADD else VIEW_TYPE_PRESET
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            VIEW_TYPE_PRESET -> {
                val binding = TimerPresetItemBinding.inflate(inflater, parent, false)
                PresetViewHolder(binding, onPresetClick, onPresetLongClick) // Pass long click listener
            }
            VIEW_TYPE_ADD -> {
                val binding = TimerPresetAddItemBinding.inflate(inflater, parent, false)
                AddViewHolder(binding, onAddClick)
            }
            else -> throw IllegalArgumentException("Invalid view type")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is PresetViewHolder -> {
                val preset = getItem(position) // Get preset for this position
                holder.bind(preset)
            }
            is AddViewHolder -> {
                // No data binding needed for Add button, listener set in ViewHolder constructor
            }
        }
    }

    // getItemCount needs to be explicitly overridden because ListAdapter's default implementation
    // uses currentList.size, but we need one extra item for the Add button.
    override fun getItemCount(): Int {
        // Size of the preset list + 1 for the Add button
        return super.getItemCount() + 1
    }

    // ViewHolder for displaying a TimerPreset
    inner class PresetViewHolder(
        private val binding: TimerPresetItemBinding,
        private val onClick: (TimerPreset) -> Unit,
        private val onLongClick: (TimerPreset) -> Unit // Add long click listener
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(preset: TimerPreset) {
            Log.d("PresetViewHolder", "Binding preset: ID=${preset.id}, Emoji='${preset.emojiIcon}'") // Log preset being bound

            val hours = TimeUnit.MILLISECONDS.toHours(preset.durationMillis)
            val minutes = TimeUnit.MILLISECONDS.toMinutes(preset.durationMillis) % 60
            val seconds = TimeUnit.MILLISECONDS.toSeconds(preset.durationMillis) % 60
            val timeString = when {
                hours > 0 -> String.format("%dh %dm %ds", hours, minutes, seconds)
                minutes > 0 -> String.format("%dm %ds", minutes, seconds)
                else -> String.format("%ds", seconds)
            }

            binding.buttonPreset.icon = null // Ensure icon is always null here, text handles the visual

            if (preset.emojiIcon.isBlank()) {
                // No emoji: Show default clock emoji on first line, time on second
                val defaultEmoji = "⏰" // Default clock emoji
                binding.buttonPreset.text = "$defaultEmoji\n$timeString"
                binding.buttonPreset.contentDescription = itemView.context.getString(
                    R.string.preset_button_content_description,
                    "Default Clock", // Updated description
                    timeString
                )
            } else {
                // Has emoji: Show emoji on first line, time on second
                binding.buttonPreset.text = "${preset.emojiIcon}\n$timeString"
                binding.buttonPreset.contentDescription = itemView.context.getString(
                    R.string.preset_button_content_description,
                    preset.emojiIcon,
                    timeString
                )
            }

            binding.buttonPreset.setOnClickListener {
                onClick(preset)
            }
            binding.buttonPreset.setOnLongClickListener {
                onLongClick(preset) // Trigger long click callback
                true // Consume the long click
            }
        }
    }

    // ViewHolder for the Add Preset button
    inner class AddViewHolder(
        binding: TimerPresetAddItemBinding,
        onClick: () -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {
        init {
            binding.buttonAddPreset.setOnClickListener {
                onClick()
            }
        }
    }
}

// DiffUtil Callback for efficient list updates
class PresetDiffCallback : DiffUtil.ItemCallback<TimerPreset>() {
    override fun areItemsTheSame(oldItem: TimerPreset, newItem: TimerPreset): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(oldItem: TimerPreset, newItem: TimerPreset): Boolean {
        // Consider adding emojiIcon comparison if it can change during updates
        return oldItem.durationMillis == newItem.durationMillis && oldItem.emojiIcon == newItem.emojiIcon
    }
} 