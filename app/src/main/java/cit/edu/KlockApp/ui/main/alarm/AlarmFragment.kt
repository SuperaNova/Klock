package cit.edu.KlockApp.ui.main.alarm

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import cit.edu.KlockApp.databinding.FragmentAlarmBinding

class AlarmFragment : Fragment() {

    private var _binding: FragmentAlarmBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!
    private lateinit var alarmViewModel: AlarmViewModel

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        alarmViewModel = ViewModelProvider(this)[AlarmViewModel::class.java]
        _binding = FragmentAlarmBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        alarmViewModel.alarms.observe(viewLifecycleOwner) { alarms ->
            val adapter = AlarmAdapter(requireContext(), alarms)
            binding.alarmList.adapter = adapter
        }
        }

    // Method to be called from KlockActivity to trigger adding a new alarm
    fun showAddAlarmDialog() {
        // TODO: Implement the UI/logic for adding a new alarm (e.g., show a dialog or navigate to a new screen)
        Toast.makeText(context, "Add Alarm Clicked (Implement Dialog Here)", Toast.LENGTH_SHORT).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}