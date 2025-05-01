package cit.edu.KlockApp.ui.main.worldClock

import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import cit.edu.KlockApp.databinding.FragmentWorldclockItemBinding
import java.text.SimpleDateFormat
import java.util.*

class WorldClockAdapter : ListAdapter<WorldClockItem, WorldClockAdapter.ViewHolder>(WorldClockDiffCallback()) {
    
    private val handler = Handler(Looper.getMainLooper())
    private val expandedItems = mutableSetOf<String>()

    private val updateTimeRunnable = object : Runnable {
        override fun run() {
            if (itemCount > 0) { 
                notifyItemRangeChanged(0, itemCount, PAYLOAD_TIME_UPDATE)
            }
            handler.postDelayed(this, 60000) // Update every minute
        }
    }

    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        super.onAttachedToRecyclerView(recyclerView)
        handler.post(updateTimeRunnable)
    }

    override fun onDetachedFromRecyclerView(recyclerView: RecyclerView) {
        super.onDetachedFromRecyclerView(recyclerView)
        handler.removeCallbacks(updateTimeRunnable)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = FragmentWorldclockItemBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding, expandedItems) { position -> notifyItemChanged(position) }
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    // Handle partial time updates to avoid full rebind
    override fun onBindViewHolder(holder: ViewHolder, position: Int, payloads: List<Any>) {
        if (payloads.contains(PAYLOAD_TIME_UPDATE)) {
            holder.updateTime(getItem(position))
        } else {
            holder.bind(getItem(position))
        }
    }

    class ViewHolder(
        private val binding: FragmentWorldclockItemBinding,
        private val expandedItems: MutableSet<String>,
        private val notifyChanged: (Int) -> Unit 
    ) : RecyclerView.ViewHolder(binding.root) {
        
        private val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
        private val calendar = Calendar.getInstance()
        
        fun bind(item: WorldClockItem) {
            binding.itemAnalogClock.setTimeZone(item.timeZoneId)
            binding.cityName.text = formatCityName(item.timeZoneId)
            updateTime(item)
            updateExpansionState(item)

            binding.root.setOnClickListener { 
                toggleExpansion(item)
            }
        }

        private fun toggleExpansion(item: WorldClockItem) {
            if (expandedItems.contains(item.timeZoneId)) {
                expandedItems.remove(item.timeZoneId)
            } else {
                expandedItems.add(item.timeZoneId)
            }
            notifyChanged(bindingAdapterPosition) // Use bindingAdapterPosition for safety
        }

        private fun updateExpansionState(item: WorldClockItem) {
            val isExpanded = expandedItems.contains(item.timeZoneId)
            binding.itemAnalogClock.visibility = if (isExpanded) View.VISIBLE else View.GONE
        }
        
        fun updateTime(item: WorldClockItem) {
            val timeZone = TimeZone.getTimeZone(item.timeZoneId)
            timeFormat.timeZone = timeZone
            calendar.timeZone = timeZone
            calendar.timeInMillis = System.currentTimeMillis()

            binding.currentTime.text = timeFormat.format(calendar.time)
            updateTimeDifference(timeZone)
        }

        private fun updateTimeDifference(timeZone: TimeZone) {
            val localOffset = TimeZone.getDefault().rawOffset
            val targetOffset = timeZone.rawOffset
            val diffHours = (targetOffset - localOffset) / (60 * 60 * 1000)
            binding.timeZoneDiff.text = when {
                diffHours == 0 -> "Same time"
                diffHours > 0 -> "+$diffHours hours"
                else -> "$diffHours hours"
            }
        }
        
        private fun formatCityName(timeZoneId: String): String {
            return timeZoneId.substringAfterLast('/').replace('_', ' ')
        }
    }

    companion object {
        private const val PAYLOAD_TIME_UPDATE = "time_update"
    }
}

class WorldClockDiffCallback : DiffUtil.ItemCallback<WorldClockItem>() {
    override fun areItemsTheSame(oldItem: WorldClockItem, newItem: WorldClockItem): Boolean {
        return oldItem.timeZoneId == newItem.timeZoneId
    }

    override fun areContentsTheSame(oldItem: WorldClockItem, newItem: WorldClockItem): Boolean {
        return oldItem == newItem
    }
} 