package cit.edu.KlockApp.ui.main.worldClock

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.setFragmentResult
import androidx.recyclerview.widget.LinearLayoutManager
import cit.edu.KlockApp.databinding.FragmentTimeZoneSelectorBottomSheetBinding
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import java.util.Locale
import java.util.TimeZone

class TimeZoneSelectorBottomSheetDialogFragment : BottomSheetDialogFragment() {

    private var _binding: FragmentTimeZoneSelectorBottomSheetBinding? = null
    private val binding get() = _binding!!

    private val viewModel: WorldClockViewModel by activityViewModels()
    private lateinit var timeZoneAdapter: TimeZoneAdapter
    private lateinit var layoutManager: LinearLayoutManager

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTimeZoneSelectorBottomSheetBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        setupSearch()
        setupAlphabetIndexer()
        observeTimeZones()
    }

    private fun setupRecyclerView() {
        layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerViewTimezones.layoutManager = layoutManager

        // Initialize adapter with empty list, it will be updated by observer
        timeZoneAdapter = TimeZoneAdapter(emptyList()) { timeZoneId ->
            // Set result and dismiss when an item is clicked
            val result = Bundle().apply {
                putString(SELECTED_TIMEZONE_ID_KEY, timeZoneId)
            }
            setFragmentResult(REQUEST_KEY, result)
            dismiss()
        }
        binding.recyclerViewTimezones.adapter = timeZoneAdapter
    }

     private fun setupSearch() {
        binding.searchEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                 timeZoneAdapter.filter(s.toString())
                 // When searching, scroll to top
                 if (s?.isNotEmpty() == true) {
                     binding.recyclerViewTimezones.scrollToPosition(0)
                 }
            }

            override fun afterTextChanged(s: Editable?) {}
        })
    }


    private fun setupAlphabetIndexer() {
        binding.alphabetIndexer.onLetterSelectedListener = { letter ->
            scrollToLetter(letter)
        }
    }

    private fun observeTimeZones() {
        // Use the correct LiveData property from the ViewModel
        viewModel.worldClocks.observe(viewLifecycleOwner) { worldClockItems ->
            // Adapt to the data structure if needed (assuming TimeZoneDisplay is generated elsewhere or needs adaptation)
            // For now, let's assume the adapter needs List<TimeZoneDisplay> and the ViewModel provides it somehow,
            // or we need to adapt WorldClockItem. This part might need further refinement based on how TimeZoneDisplay is populated.
            
            // TEMP: Placeholder assuming viewModel provides TimeZoneDisplay list directly for the selector
            // This needs verification. If viewModel.worldClocks is List<WorldClockItem>, we need a way to get the full TimeZoneDisplay list.
            // Let's assume WorldClockViewModel needs a method to provide all selectable timezones.
            // For now, commenting out the problematic observer logic until the source of TimeZoneDisplay is clarified.
            /* 
            // Update the adapter's data source and apply filter if needed
            timeZoneAdapter = TimeZoneAdapter(timeZoneDisplays) { timeZoneId -> // timeZoneDisplays is undefined here
                val result = Bundle().apply {
                    putString(SELECTED_TIMEZONE_ID_KEY, timeZoneId)
                }
                setFragmentResult(REQUEST_KEY, result)
                dismiss()
            }
             // Preserve current search filter
             timeZoneAdapter.filter(binding.searchEditText.text.toString())
             binding.recyclerViewTimezones.adapter = timeZoneAdapter
            */
            
            // TODO: Fetch the full list of TimeZoneDisplay objects correctly.
            // Maybe from a utility function or a separate LiveData in the ViewModel?
            // For now, initializing with an empty list in setupRecyclerView() and filter will work on that.
            // The list needs to be populated from a source like TimeZone.getAvailableIDs()
            // Let's move the population logic here temporarily
            
             val allTimeZoneDisplays = getTimeZoneDisplayList() // Get the full list
             timeZoneAdapter = TimeZoneAdapter(allTimeZoneDisplays) { timeZoneId ->
                 val result = Bundle().apply {
                     putString(SELECTED_TIMEZONE_ID_KEY, timeZoneId)
                 }
                 setFragmentResult(REQUEST_KEY, result)
                 dismiss()
             }
             timeZoneAdapter.filter(binding.searchEditText.text.toString()) // Apply filter
             binding.recyclerViewTimezones.adapter = timeZoneAdapter // Set adapter
             
        } // End of incorrect observer
    }

    // Function to get timezone list - Filtered for better user experience
    private fun getTimeZoneDisplayList(): List<TimeZoneDisplay> {
        val cityToTimeZoneIdMap = mutableMapOf<String, String>()

        TimeZone.getAvailableIDs().forEach { id ->
            if (id.contains('/')) { // Keep only Region/City format
                val city = id.substringAfterLast('/').replace('_', ' ')
                // Only add if the city name isn't already mapped, preferring the first encountered ID for a given city name
                if (!cityToTimeZoneIdMap.containsKey(city)) {
                    // Basic sanity check - avoid excessively short names that might be acronyms
                    if (city.length > 3) { 
                       cityToTimeZoneIdMap[city] = id
                    }
                }
            }
        }

        // Convert the map to a list of TimeZoneDisplay objects and sort by city name
        return cityToTimeZoneIdMap.map { (city, id) ->
            TimeZoneDisplay(id = id, displayName = city)
        }.sortedBy { it.displayName }
    }

     private fun scrollToLetter(letter: Char) {
        val position = timeZoneAdapter.getPositionForLetter(letter)
        if (position != -1) {
            // Scrolls smoothly to the position, placing the item at the top of the view.
            layoutManager.scrollToPositionWithOffset(position, 0)
        }
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val TAG = "TimeZoneSelectorBottomSheet"
        const val REQUEST_KEY = "TimeZoneSelectorRequest"
        const val SELECTED_TIMEZONE_ID_KEY = "SelectedTimeZoneId"

        fun newInstance(): TimeZoneSelectorBottomSheetDialogFragment {
            return TimeZoneSelectorBottomSheetDialogFragment()
        }
    }
}

// Extension function needed in TimeZoneAdapter to find the position
// Alternatively, add this logic inside TimeZoneSelectorBottomSheetDialogFragment
fun TimeZoneAdapter.getPositionForLetter(letter: Char): Int {
    val lowerCaseLetter = letter.lowercaseChar()
    // Iterate through the *currently filtered* list
    return this.filteredTimeZones.indexOfFirst { // Use filteredTimeZones here
        it.displayName.firstOrNull()?.lowercaseChar() == lowerCaseLetter
    }
} 