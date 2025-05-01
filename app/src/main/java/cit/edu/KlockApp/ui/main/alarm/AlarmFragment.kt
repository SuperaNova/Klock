package cit.edu.KlockApp.ui.main.alarm

import android.app.Activity
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import cit.edu.KlockApp.R
import cit.edu.KlockApp.databinding.FragmentAlarmBinding
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.time.DayOfWeek

// Implement the listener interface
class AlarmFragment : Fragment(), AlarmInteractionListener {
    private var _binding: FragmentAlarmBinding? = null
    private val binding get() = _binding!!
    private lateinit var alarmViewModel: AlarmViewModel
    private lateinit var adapter: AlarmRecyclerAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        alarmViewModel = ViewModelProvider(
            this,
            ViewModelProvider.AndroidViewModelFactory(requireActivity().application)
        )[AlarmViewModel::class.java] // Corrected ViewModel getter

        _binding = FragmentAlarmBinding.inflate(inflater, container, false)
        val view = binding.root

        setupRecyclerView()
        observeViewModel()

        return view
    }

    private fun setupRecyclerView() {
        val recycler = binding.alarmRecycler // Use binding
        recycler.layoutManager = LinearLayoutManager(requireContext())
        // Pass 'this' fragment as the listener
        adapter = AlarmRecyclerAdapter(this)
        recycler.adapter = adapter
    }

    private fun observeViewModel() {
        // Observe LiveData
        alarmViewModel.alarms.observe(viewLifecycleOwner) { alarms ->
            adapter.submitList(alarms) // Use submitList with ListAdapter
        }
    }

    // --- AlarmInteractionListener Implementation --- 

    override fun onToggle(alarm: Alarm, isEnabled: Boolean) {
        Log.d("AlarmFragment", "Toggling alarm ${alarm.id} to $isEnabled")
        // Create a copy with the updated enabled state and pass to ViewModel
        alarmViewModel.updateAlarm(alarm.copy(isEnabled = isEnabled))
        // TODO: Schedule/cancel system alarm based on isEnabled state
    }

    override fun onDelete(alarm: Alarm) {
        Log.d("AlarmFragment", "Deleting alarm ${alarm.id}")
        // TODO: Cancel the scheduled system alarm using AlarmManager and PendingIntent
        alarmViewModel.deleteAlarm(alarm)
        Toast.makeText(requireContext(), "Alarm deleted", Toast.LENGTH_SHORT).show()
    }

    override fun onEdit(alarm: Alarm) {
        Log.d("AlarmFragment", "Edit requested for alarm ${alarm.id}")
        // Launch AlarmEditActivity, passing the alarm data
        val intent = Intent(requireContext(), AlarmEditActivity::class.java).apply {
            putExtra("alarm", alarm.copy()) // Pass a copy
        }
        updateAlarmLauncher.launch(intent)
    }

    // Implement the new listener method
    override fun onVibrateChanged(alarm: Alarm, vibrate: Boolean) {
        Log.d("AlarmFragment", "Vibrate changed for alarm ${alarm.id} to $vibrate")
        alarmViewModel.updateAlarm(alarm.copy(vibrate = vibrate))
        // Vibrate setting usually doesn't require rescheduling the alarm itself
    }

    // --- ActivityResultLaunchers --- 

    private val updateAlarmLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.getParcelableExtra<Alarm>("updatedAlarm")?.let { updated ->
                alarmViewModel.updateAlarm(updated.copy()) 
                val fmt = DateTimeFormatter.ofPattern("h:mm a", Locale.getDefault())
                Toast.makeText(
                    requireContext(),
                    "Alarm updated: ${updated.time.format(fmt)}",
                    Toast.LENGTH_SHORT
                ).show()
                // TODO: Reschedule alarm if time/enabled changed
            }
        }
    }

    private val addAlarmLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.getParcelableExtra<Alarm>("updatedAlarm")?.let { newAlarm ->
                alarmViewModel.addAlarm(newAlarm.copy())
                 // TODO: Schedule the newly added alarm
            }
        }
    }

    // Called from KlockActivity via menu item
    fun launchAddAlarm() {
        val intent = Intent(requireContext(), AlarmAddActivity::class.java)
        addAlarmLauncher.launch(intent)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
