package cit.edu.KlockApp.ui.main.stopwatch

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import cit.edu.KlockApp.databinding.StopwatchLapItemBinding
import java.util.concurrent.TimeUnit

class LapAdapter : ListAdapter<LapData, LapAdapter.LapViewHolder>(LapDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LapViewHolder {
        val binding = StopwatchLapItemBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return LapViewHolder(binding)
    }

    override fun onBindViewHolder(holder: LapViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class LapViewHolder(private val binding: StopwatchLapItemBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(lapData: LapData) {
            binding.lapNumberText.text = "Lap ${lapData.lapNumber}"
            binding.lapTimeText.text = formatMillis(lapData.lapTimeMillis)
            binding.totalTimeText.text = formatMillis(lapData.totalTimeMillis)
        }

        private fun formatMillis(millis: Long): String {
            val minutes = TimeUnit.MILLISECONDS.toMinutes(millis)
            val seconds = TimeUnit.MILLISECONDS.toSeconds(millis) % 60
            val milliseconds = (millis % 1000) / 10 // Show hundredths
            return String.format("%02d:%02d.%02d", minutes, seconds, milliseconds)
        }
    }
}

class LapDiffCallback : DiffUtil.ItemCallback<LapData>() {
    override fun areItemsTheSame(oldItem: LapData, newItem: LapData): Boolean {
        return oldItem.lapNumber == newItem.lapNumber
    }

    override fun areContentsTheSame(oldItem: LapData, newItem: LapData): Boolean {
        return oldItem == newItem
    }
} 