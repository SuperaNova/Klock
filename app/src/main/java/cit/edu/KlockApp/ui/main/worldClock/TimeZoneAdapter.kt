package cit.edu.KlockApp.ui.main.worldClock

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import cit.edu.KlockApp.databinding.ListItemTimezoneBinding
import java.util.Locale

class TimeZoneAdapter(
    private val allTimeZones: List<TimeZoneDisplay>,
    private val onItemClick: (String) -> Unit
) : RecyclerView.Adapter<TimeZoneAdapter.ViewHolder>() {

    private var filteredTimeZones: List<TimeZoneDisplay> = allTimeZones

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ListItemTimezoneBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val timeZoneDisplay = filteredTimeZones[position]
        holder.bind(timeZoneDisplay, onItemClick)
    }

    override fun getItemCount(): Int = filteredTimeZones.size

    fun filter(query: String) {
        filteredTimeZones = if (query.isEmpty()) {
            allTimeZones
        } else {
            val lowerCaseQuery = query.lowercase(Locale.getDefault())
            allTimeZones.filter {
                it.displayName.lowercase(Locale.getDefault()).contains(lowerCaseQuery)
            }
        }
        notifyDataSetChanged()
    }

    class ViewHolder(private val binding: ListItemTimezoneBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(timeZoneDisplay: TimeZoneDisplay, onItemClick: (String) -> Unit) {
            binding.timezoneName.text = timeZoneDisplay.displayName
            itemView.setOnClickListener { onItemClick(timeZoneDisplay.id) }
        }
    }
}
