package cit.edu.KlockApp.ui.main.alarm

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import cit.edu.KlockApp.R
import java.time.format.DateTimeFormatter
import java.util.Locale

class AlarmRecyclerAdapter(
    private val alarms: MutableList<Alarm>,
    private val onItemClick: (Alarm) -> Unit,
    private val onEnabledChange: (Alarm) -> Unit
) : RecyclerView.Adapter<AlarmRecyclerAdapter.ViewHolder>() {

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val timeTv: TextView = view.findViewById(R.id.alarm_time)
        val amPmTv: TextView = view.findViewById(R.id.alarm_am_pm)
        val labelTv: TextView = view.findViewById(R.id.alarm_label)
        val repeatTv: TextView = view.findViewById(R.id.alarm_date)
        val enableCb: CheckBox = view.findViewById(R.id.alarm_enabled)

        init {
            view.setOnClickListener { onItemClick(alarms[adapterPosition]) }
            enableCb.setOnCheckedChangeListener { _, isChecked ->
                val alarm = alarms[adapterPosition]
                alarm.isEnabled = isChecked
                onEnabledChange(alarm)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.alarm_item, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val alarm = alarms[position]
        val timeFmt = DateTimeFormatter.ofPattern("hh:mm", Locale.getDefault())
        val amPmFmt = DateTimeFormatter.ofPattern("a", Locale.getDefault())

        holder.timeTv.text = alarm.time.format(timeFmt)
        holder.amPmTv.text = alarm.time.format(amPmFmt).uppercase(Locale.getDefault())
        holder.labelTv.text = alarm.label
        holder.repeatTv.text = formatRepeatDays(alarm.repeatDays)
        holder.enableCb.isChecked = alarm.isEnabled
    }

    override fun getItemCount(): Int = alarms.size

    /**
     * Remove and return the alarm at [position].
     */
    fun removeAt(position: Int): Alarm {
        val removed = alarms.removeAt(position)
        notifyItemRemoved(position)
        return removed
    }

    /**
     * Replace the current list with [newList] and refresh using DiffUtil.
     */
    fun updateList(newList: List<Alarm>) {
        val diffCallback = AlarmDiffCallback(alarms, newList)
        val diffResult = DiffUtil.calculateDiff(diffCallback)

        alarms.clear()
        alarms.addAll(newList.map { it.copy() })  // Ensure you use copies
        diffResult.dispatchUpdatesTo(this)
    }

    private fun formatRepeatDays(days: List<String>): String {
        val fullDays = listOf("Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday")
        val shortDays = listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")

        if (days.isEmpty()) return "Once"
        if (days.size == 7) return "Everyday"

        val sorted = days.sortedBy { fullDays.indexOf(it) }
        val parts = mutableListOf<String>()
        var i = 0

        while (i < sorted.size) {
            var j = i
            while (
                j + 1 < sorted.size &&
                fullDays.indexOf(sorted[j + 1]) == fullDays.indexOf(sorted[j]) + 1
            ) {
                j++
            }

            val startIdx = fullDays.indexOf(sorted[i])
            val endIdx = fullDays.indexOf(sorted[j])
            val startShort = shortDays[startIdx]
            val endShort = shortDays[endIdx]

            if (i == j) {
                parts.add(startShort)
            } else if (j - i == 1) {
                parts.add("$startShort, $endShort")
            } else {
                parts.add("$startShort to $endShort")
            }

            i = j + 1
        }

        return parts.joinToString(", ")
    }

    // DiffUtil Callback for Alarm
    class AlarmDiffCallback(
        private val oldList: List<Alarm>,
        private val newList: List<Alarm>
    ) : DiffUtil.Callback() {

        override fun getOldListSize(): Int = oldList.size

        override fun getNewListSize(): Int = newList.size

        override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            return oldList[oldItemPosition].id == newList[newItemPosition].id
        }

        override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            return oldList[oldItemPosition] == newList[newItemPosition]
        }
    }
}