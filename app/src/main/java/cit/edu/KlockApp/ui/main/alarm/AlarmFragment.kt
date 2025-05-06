package cit.edu.KlockApp.ui.main.alarm

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import cit.edu.KlockApp.R
import cit.edu.KlockApp.databinding.FragmentAlarmBinding

class AlarmFragment : Fragment() {
    private var _binding: FragmentAlarmBinding? = null
    private val b get() = _binding!!
    private lateinit var vm: AlarmViewModel
    private lateinit var adapter: AlarmAdapter

    // 1️⃣ Register for create/edit Alarm results
    private val alarmLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val alarm = result.data?.getParcelableExtra<Alarm>("updatedAlarm")
            if (alarm != null) {
                if (vm.alarms.value?.any { it.id == alarm.id } == true) {
                    vm.updateAlarm(alarm)
                    Toast.makeText(requireContext(),
                        "${alarm.label} updated to ${alarm.time}", Toast.LENGTH_SHORT).show()
                } else {
                    vm.addAlarm(alarm)
                    Toast.makeText(requireContext(),
                        "${alarm.label} set to ${alarm.time}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAlarmBinding.inflate(inflater, container, false)
        return b.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        vm = ViewModelProvider(
            this, ViewModelProvider.AndroidViewModelFactory(requireActivity().application)
        ).get(AlarmViewModel::class.java)

        adapter = AlarmAdapter(
            // Inside your AlarmFragment
            onToggleEnabled = { updated ->
                // When toggling 'isEnabled', make sure only that property is updated
                val currentList = vm.alarms.value?.map {
                    if (it.id == updated.id) updated else it
                }
                if (currentList != null) {
                    vm._alarms.value = currentList
                    Toast.makeText(requireContext(), "Alarm ${if (updated.isEnabled) "enabled" else "disabled"}", Toast.LENGTH_SHORT).show()
                }
            },
            onExpandToggled = { alarm ->
                val newList = vm.alarms.value!!.map {
                    if (it.id == alarm.id) {
                        it.copy(isExpanded = !alarm.isExpanded) // Toggle only the selected alarm's expanded state
                    } else {
                        it // Leave others unchanged
                    }
                }
                vm._alarms.value = newList // Update the list without affecting isExpanded of other alarms
            },
            onLabelChanged = { updated ->
                val newList = vm.alarms.value?.map {
                    if (it.id == updated.id) updated else it
                }
                if (newList != null) {
                    vm._alarms.value = newList
                    Toast.makeText(requireContext(),
                        "Label changed to “${updated.label}”", Toast.LENGTH_SHORT).show()
                }
            },
            onAlarmTimeAdjust = { updated ->
                vm.updateAlarm(updated.copy(isExpanded = true))
            },
            onVibrateToggle = { updated ->
                val updatedAlarm = updated.copy(isExpanded = updated.isExpanded) // Retain the current 'isExpanded' state
                vm.updateAlarm(updatedAlarm)
            }

        )

        b.alarmRecycler.layoutManager = LinearLayoutManager(requireContext())
        b.alarmRecycler.adapter        = adapter

        // Swipe to delete
        ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT) {
            override fun onMove(rv: RecyclerView, vh: RecyclerView.ViewHolder,
                                target: RecyclerView.ViewHolder) = false

            override fun onSwiped(vh: RecyclerView.ViewHolder, direction: Int) {
                val alarm = adapter.currentList[vh.adapterPosition]
                vm.deleteAlarm(alarm)
                Toast.makeText(requireContext(),
                    "${alarm.label} deleted", Toast.LENGTH_SHORT).show()
            }
        }).attachToRecyclerView(b.alarmRecycler)

        // Observe LiveData
        vm.alarms.observe(viewLifecycleOwner) { list ->
            adapter.submitList(list.toList())
        }
    }

    // Public function for Activity to call
    fun launchAddAlarm() {
         val intent = Intent(requireContext(), AlarmActivity::class.java)
         alarmLauncher.launch(intent)
    }

    override fun onDestroyView() {
        // Collapse all alarms before fragment is destroyed
        vm.alarms.value?.let { alarmList ->
            val collapsedList = alarmList.map { it.copy(isExpanded = false) }
            collapsedList.forEach { vm.updateAlarm(it) }
        }

        _binding = null
        super.onDestroyView()
    }
}
