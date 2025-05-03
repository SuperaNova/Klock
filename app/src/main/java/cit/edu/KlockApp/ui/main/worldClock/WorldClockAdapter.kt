package cit.edu.KlockApp.ui.main.worldClock

import android.annotation.SuppressLint
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.text.format.DateFormat
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.ItemTouchHelper
import cit.edu.KlockApp.databinding.FragmentWorldclockItemBinding
import java.text.SimpleDateFormat
import java.util.*

class WorldClockAdapter(
    private val itemTouchHelper: ItemTouchHelper,
    private val onDeleteRequested: (WorldClockItem) -> Unit
) : ListAdapter<WorldClockItem, WorldClockAdapter.ViewHolder>(WorldClockDiffCallback()) {
    
    private val handler = Handler(Looper.getMainLooper())
    private val expandedItems = mutableSetOf<String>()
    private var isEditMode = false

    private val updateTimeRunnable = object : Runnable {
        override fun run() {
            if (itemCount > 0) { 
                notifyItemRangeChanged(0, itemCount, PAYLOAD_TIME_UPDATE)
            }
            // Update every second for analog clock sync
            handler.postDelayed(this, 1000) 
        }
    }

    fun setEditMode(editMode: Boolean) {
        if (isEditMode != editMode) {
            isEditMode = editMode
            notifyItemRangeChanged(0, itemCount, PAYLOAD_EDIT_MODE_CHANGED)
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
        return ViewHolder(
            binding,
            expandedItems,
            parent.context,
            itemTouchHelper,
            onDeleteRequested,
            { position -> notifyItemChanged(position) }
        )
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position), isEditMode)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int, payloads: List<Any>) {
        if (payloads.isEmpty()) {
            onBindViewHolder(holder, position)
        } else {
            payloads.forEach { payload ->
                when (payload) {
                    PAYLOAD_TIME_UPDATE -> holder.updateTime(getItem(position))
                    PAYLOAD_EDIT_MODE_CHANGED -> holder.updateEditMode(isEditMode)
                }
            }
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    class ViewHolder(
        private val binding: FragmentWorldclockItemBinding,
        private val expandedItems: MutableSet<String>,
        private val context: Context,
        private val itemTouchHelper: ItemTouchHelper,
        private val onDeleteRequested: (WorldClockItem) -> Unit,
        private val onItemChanged: (Int) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {
        
        private val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
        private val calendar = Calendar.getInstance()
        private var is24HourFormat: Boolean = false

        init {
            updateFormat()
        }

        fun updateFormat() {
            val currentSetting = DateFormat.is24HourFormat(context)
            if (currentSetting != is24HourFormat) {
                is24HourFormat = currentSetting
                val pattern = if (is24HourFormat) "HH:mm" else "h:mm a"
                timeFormat.applyPattern(pattern)
            }
        }

        fun bind(item: WorldClockItem, editMode: Boolean) {
            binding.itemAnalogClock.setTimeZone(item.timeZoneId)
            binding.cityName.text = formatCityName(item.timeZoneId)
            updateTime(item)
            updateExpansionState(item, editMode)
            updateEditMode(editMode)

            binding.root.setOnClickListener {
                if (!editMode) {
                    toggleExpansion(item)
                }
            }
            
            binding.root.setOnLongClickListener {
                if (!editMode) {
                    onDeleteRequested(item)
                    true
                } else {
                    false
                }
            }
            
            binding.dragHandle.setOnTouchListener { _, event ->
                if (editMode && event.actionMasked == MotionEvent.ACTION_DOWN) {
                    itemTouchHelper.startDrag(this)
                }
                true
            }
        }

        private fun toggleExpansion(item: WorldClockItem) {
            if (expandedItems.contains(item.timeZoneId)) {
                expandedItems.remove(item.timeZoneId)
            } else {
                expandedItems.add(item.timeZoneId)
            }
            updateExpansionState(item, false)
            val position = bindingAdapterPosition
            if (position != RecyclerView.NO_POSITION) {
                onItemChanged(position)
            }
        }

        private fun updateExpansionState(item: WorldClockItem, editMode: Boolean) {
            val isExpanded = expandedItems.contains(item.timeZoneId) && !editMode
            binding.itemAnalogClock.visibility = if (isExpanded) View.VISIBLE else View.GONE
        }
        
        fun updateTime(item: WorldClockItem) {
            updateFormat()
            
            val timeZone = TimeZone.getTimeZone(item.timeZoneId)
            timeFormat.timeZone = timeZone
            calendar.timeZone = timeZone
            
            // Get current time ONCE
            val currentDeviceTimeMillis = System.currentTimeMillis()
            calendar.timeInMillis = currentDeviceTimeMillis 

            // Update digital time
            binding.currentTime.text = timeFormat.format(calendar.time)
            updateTimeDifference(timeZone)
            
            // Update analog clock with the same timestamp
            binding.itemAnalogClock.setTimeMillis(currentDeviceTimeMillis)
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

        fun updateEditMode(editMode: Boolean) {
            binding.dragHandle.isVisible = editMode
            binding.root.isClickable = !editMode
            binding.root.isLongClickable = !editMode
            if (editMode && binding.itemAnalogClock.isVisible) {
                binding.itemAnalogClock.visibility = View.GONE 
            }
        }
    }

    companion object {
        private const val PAYLOAD_TIME_UPDATE = "time_update"
        private const val PAYLOAD_EDIT_MODE_CHANGED = "edit_mode_changed"
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