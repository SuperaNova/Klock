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
    }

    private fun setupRecyclerView() {
        layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerViewTimezones.layoutManager = layoutManager

        // Fetch the list and create the adapter here, once.
        val allTimeZoneDisplays = getTimeZoneDisplayList()
        timeZoneAdapter = TimeZoneAdapter(allTimeZoneDisplays) { timeZoneId ->
            // Set result and dismiss when an item is clicked
            val result = Bundle().apply {
                putString(SELECTED_TIMEZONE_ID_KEY, timeZoneId)
            }
            android.util.Log.d(TAG, "Setting fragment result: Key=$REQUEST_KEY, ID=$timeZoneId")
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

    // Function to get timezone list - Filtered for better user experience
    private fun getTimeZoneDisplayList(): List<TimeZoneDisplay> {
        val cityToTimeZoneIdMap = mutableMapOf<String, String>()

        TimeZone.getAvailableIDs().forEach { id ->
            // Explicitly skip GMT and Etc/GMT timezones
            if (id.startsWith("GMT", ignoreCase = true) || id.startsWith("Etc/GMT", ignoreCase = true)) {
                return@forEach // Skip this ID
            }

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