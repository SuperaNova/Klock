package cit.edu.KlockApp.ui.main.worldClock

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import cit.edu.KlockApp.databinding.FragmentAddclockListItemBinding

class TimeZoneAdapter(private val itemCount: Int) :
    RecyclerView.Adapter<TimeZoneAdapter.ViewHolder>() {

    private val options = listOf("Notifications", "Dark Mode", "Sync Data", "Log Out")

    inner class ViewHolder(binding: FragmentAddclockListItemBinding) :
        RecyclerView.ViewHolder(binding.root) {
        val text: TextView = binding.text
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(
            FragmentAddclockListItemBinding.inflate(
                LayoutInflater.from(parent.context), parent, false
            )
        )
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.text.text = options.getOrNull(position) ?: "Option $position"
        holder.itemView.setOnClickListener {
            Toast.makeText(it.context, "${holder.text.text} clicked", Toast.LENGTH_SHORT).show()
        }
    }

    override fun getItemCount(): Int = minOf(itemCount, options.size)
}
