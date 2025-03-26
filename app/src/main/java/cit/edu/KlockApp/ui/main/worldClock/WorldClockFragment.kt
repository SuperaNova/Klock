package cit.edu.KlockApp.ui.main.worldClock

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import cit.edu.KlockApp.R
import cit.edu.KlockApp.databinding.FragmentWorldclockBinding

class WorldClockFragment : Fragment() {

    private var _binding: FragmentWorldclockBinding? = null
    private val binding get() = _binding!!
    private lateinit var worldClockViewModel: WorldClockViewModel

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentWorldclockBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize ViewModel
        worldClockViewModel = ViewModelProvider(this)[WorldClockViewModel::class.java]

        worldClockViewModel.textList.observe(viewLifecycleOwner) { list ->
            updateWorldClockList(list)
        }
    }

    private fun updateWorldClockList(textList: List<String>) {
        binding.worldClockContainer.removeAllViews() // Clear previous items

        val inflater = LayoutInflater.from(requireContext())
        for (text in textList) {
            val childView = inflater.inflate(R.layout.fragment_worldclock_item, binding.worldClockContainer, false)
            val textView = childView.findViewById<TextView>(R.id.text_worldClock)
            textView.text = text
            binding.worldClockContainer.addView(childView)
        }
    }

    fun addNewWorldClockItem(newText: String) {
        worldClockViewModel.addNewWorldClock(newText) // test add text
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}