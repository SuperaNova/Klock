package cit.edu.KlockApp.ui.main.alarm

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.TextView
import android.widget.BaseAdapter
import cit.edu.KlockApp.R

class AlarmAdapter(private val context: Context, private val alarms: List<Alarm>) : BaseAdapter() {

    override fun getCount(): Int = alarms.size

    override fun getItem(position: Int): Any = alarms[position]

    override fun getItemId(position: Int): Long = position.toLong()

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        val view: View = convertView ?: LayoutInflater.from(context).inflate(R.layout.alarm_item, parent, false)

        val alarm = alarms[position]

        val timeTextView: TextView = view.findViewById(R.id.alarm_time)
        val dateTextView: TextView = view.findViewById(R.id.alarm_date)
        val enabledCheckBox: CheckBox = view.findViewById(R.id.alarm_enabled)

        timeTextView.text = alarm.time.toString()
        dateTextView.text = alarm.date.toString()
        enabledCheckBox.isChecked = alarm.isEnabled

        // Handle checkbox toggle (if you need to enable/disable alarms)
        enabledCheckBox.setOnCheckedChangeListener { _, isChecked ->
            alarm.isEnabled = isChecked
            //TODO Add logic to update alarm
        }

        return view
    }
}