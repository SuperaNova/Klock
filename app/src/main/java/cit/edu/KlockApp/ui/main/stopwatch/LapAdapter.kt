package cit.edu.KlockApp.ui.main.stopwatch

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import cit.edu.KlockApp.R
import cit.edu.KlockApp.databinding.StopwatchLapItemBinding
import android.util.TypedValue // Import for theme attribute resolution
import androidx.annotation.ColorInt // Import for type hint
import java.util.concurrent.TimeUnit

class LapAdapter : ListAdapter<LapData, LapAdapter.LapViewHolder>(LapDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LapViewHolder {
        val binding = StopwatchLapItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return LapViewHolder(binding)
    }

    override fun onBindViewHolder(holder: LapViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class LapViewHolder(private val binding: StopwatchLapItemBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(lapData: LapData) {
            binding.lapNumberText.text = "Lap ${lapData.lapNumber}"
            binding.lapTimeText.text = formatTimeInternal(lapData.lapTimeMillis)
            binding.totalTimeText.text = formatTimeInternal(lapData.totalTimeMillis)

            binding.lapTimeText.setTextColor(resolveThemeColor(binding.root.context, android.R.attr.textColorPrimary))
        }

        private fun formatTimeInternal(millis: Long): String {
            val minutes = TimeUnit.MILLISECONDS.toMinutes(millis)
            val seconds = TimeUnit.MILLISECONDS.toSeconds(millis) % 60
            val hundredths = (TimeUnit.MILLISECONDS.toMillis(millis) % 1000) / 10
            return String.format("%02d:%02d.%02d", minutes, seconds, hundredths)
        }

        @ColorInt
        private fun resolveThemeColor(context: Context, attr: Int): Int {
            val typedValue = TypedValue()
            val theme = context.theme
            theme.resolveAttribute(attr, typedValue, true)
            return typedValue.data
        }
    }

    class LapDiffCallback : DiffUtil.ItemCallback<LapData>() {
        override fun areItemsTheSame(oldItem: LapData, newItem: LapData):
                Boolean = oldItem.lapNumber == newItem.lapNumber

        override fun areContentsTheSame(oldItem: LapData, newItem: LapData):
                Boolean = oldItem == newItem
    }
} 