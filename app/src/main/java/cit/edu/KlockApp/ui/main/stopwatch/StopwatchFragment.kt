package cit.edu.KlockApp.ui.main.stopwatch

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import cit.edu.KlockApp.databinding.FragmentStopwatchBinding
import cit.edu.KlockApp.ui.notifications.StopwatchViewModel

class StopwatchFragment : Fragment() {

    private var _binding: FragmentStopwatchBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val notificationsViewModel =
            ViewModelProvider(this)[StopwatchViewModel::class.java]

        _binding = FragmentStopwatchBinding.inflate(inflater, container, false)
        val root: View = binding.root

        val textView: TextView = binding.textTimer
        notificationsViewModel.text.observe(viewLifecycleOwner) {
            textView.text = it
        }
        return root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}