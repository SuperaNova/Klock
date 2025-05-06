package cit.edu.KlockApp.ui.main.alarm

import androidx.recyclerview.widget.DiffUtil

class AlarmDiffCallback : DiffUtil.ItemCallback<Alarm>() {
    override fun areItemsTheSame(oldItem: Alarm, newItem: Alarm) = oldItem.id == newItem.id
    override fun areContentsTheSame(oldItem: Alarm, newItem: Alarm) = oldItem == newItem
}