package cit.edu.KlockApp.ui.main.worldClock

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.util.Locale

class TimeZoneAdapter(
    private val allTimeZones: List<String>,
    private val onItemClick: (String) -> Unit
) : RecyclerView.Adapter<TimeZoneAdapter.ViewHolder>() {

    private var filteredTimeZones: List<String> = allTimeZones

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(android.R.layout.simple_list_item_1, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val timeZone = filteredTimeZones[position]
        holder.bind(timeZone, onItemClick)
    }

    override fun getItemCount(): Int = filteredTimeZones.size

    fun filter(query: String) {
        filteredTimeZones = if (query.isEmpty()) {
            allTimeZones
        } else {
            val lowerCaseQuery = query.lowercase(Locale.getDefault())
            allTimeZones.filter {
                it.lowercase(Locale.getDefault()).contains(lowerCaseQuery)
            }
        }
        // Consider DiffUtil for better performance with large lists
        notifyDataSetChanged()
        }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val textView: TextView = itemView.findViewById(android.R.id.text1)

        fun bind(timeZone: String, onItemClick: (String) -> Unit) {
            textView.text = timeZone
            itemView.setOnClickListener { onItemClick(timeZone) }
        }
    }
}
