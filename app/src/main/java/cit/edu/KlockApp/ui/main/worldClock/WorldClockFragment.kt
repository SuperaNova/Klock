package cit.edu.KlockApp.ui.main.worldClock

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import cit.edu.KlockApp.databinding.FragmentWorldclockBinding

class WorldClockFragment : Fragment() {

    private var _binding: FragmentWorldclockBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val worldClockViewModel =
            ViewModelProvider(this)[WorldClockViewModel::class.java]

        _binding = FragmentWorldclockBinding.inflate(inflater, container, false)
        val root: View = binding.root

        val textView: TextView = binding.textWorldClock
        worldClockViewModel.text.observe(viewLifecycleOwner) {
            textView.text = it
        }
        return root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}