package cit.edu.KlockApp.ui.main.alarm

import android.app.Activity
import android.app.AlertDialog
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
            onToggleEnabled  = { updated -> vm.updateAlarm(updated) },
            onExpandToggled  = { alarm ->
                // Toggle expansion and persist
                vm.updateAlarm(alarm.copy(isExpanded = !alarm.isExpanded))
            },
            onLabelChanged   = { updated -> vm.updateAlarm(updated) },
            onAlarmTimeAdjust= { updated -> vm.updateAlarm(updated) },
            onVibrateToggle  = { updated -> vm.updateAlarm(updated) }

        )

        b.alarmRecycler.layoutManager = LinearLayoutManager(requireContext())
        b.alarmRecycler.adapter        = adapter

        // Swipe to delete
        ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(
            0,
            ItemTouchHelper.LEFT
        ) {
            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean {
                return false
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val position     = viewHolder.adapterPosition
                val originalList = adapter.currentList.toList()
                val alarm        = originalList[position]

                val without      = originalList.toMutableList().apply { removeAt(position) }
                adapter.submitList(without)

                var confirmed = false

                AlertDialog.Builder(requireContext())
                    .setTitle("Delete Alarm")
                    .setMessage("Are you sure you want to delete “${alarm.label}”?")
                    .setPositiveButton("Delete") { _, _ ->
                        confirmed = true
                        vm.deleteAlarm(alarm)
                        Toast.makeText(requireContext(),
                            "${alarm.label} deleted", Toast.LENGTH_SHORT).show()
                    }
                    .setNegativeButton("Cancel") { dialog, _ ->
                        dialog.dismiss()
                    }
                    .setOnDismissListener {
                        if (!confirmed) {
                            // Only restore if they did NOT confirm
                            adapter.submitList(originalList)
                        }
                    }
                    .show()
            }

        }).attachToRecyclerView(b.alarmRecycler)



        // Observe LiveData
        vm.alarms.observe(viewLifecycleOwner) { list ->
            adapter.submitList(list.toList())
        }

        // Hook up your "+" menu item via fragment's host activity's onOptionsItemSelected
        // setHasOptionsMenu(true) // REMOVED
    }

    override fun onResume() {
        super.onResume()
        // Refresh the adapter to re-evaluate time formats when the fragment resumes
        if (::adapter.isInitialized) {
            adapter.notifyDataSetChanged()
        }
    }

    // Public method to be called from KlockActivity
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