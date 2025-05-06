package cit.edu.KlockApp.ui.main.alarm

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
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
            onToggleEnabled = { alarm -> vm.updateAlarm(alarm) },
            onDelete        = { alarm ->
                vm.deleteAlarm(alarm)
                Toast.makeText(requireContext(),
                    "${alarm.label} deleted", Toast.LENGTH_SHORT).show()
            },
            onExpandToggled = { alarm ->
                // toggle expanded flag on the one clicked
                val newList = vm.alarms.value!!.map {
                    it.copy(isExpanded = it.id == alarm.id && !it.isExpanded)
                }
                vm._alarms.value = newList
            },
            onLabelChanged     = { alarm ->
                vm.updateAlarm(alarm)
                Toast.makeText(requireContext(),
                    "Label changed to “${alarm.label}”",
                    Toast.LENGTH_SHORT
                ).show()
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

        // Hook up your “+” menu item via fragment’s host activity’s onOptionsItemSelected
        setHasOptionsMenu(true)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.action_bar_menu, menu)
        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onOptionsItemSelected(item: MenuItem) = when (item.itemId) {
        R.id.action_add -> {
            val intent = Intent(requireContext(), AlarmActivity::class.java)
            alarmLauncher.launch(intent)
            true
        }
        else -> super.onOptionsItemSelected(item)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
