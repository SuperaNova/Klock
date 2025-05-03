package cit.edu.KlockApp.ui.main.worldClock

import android.os.Bundle
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import androidx.recyclerview.widget.LinearLayoutManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.lifecycle.ViewModelProvider
import cit.edu.KlockApp.databinding.FragmentAddclockListBinding
import java.util.TimeZone

/**
 * A fragment that shows a list of timezones as a modal bottom sheet.
 *
 * You can show this modal bottom sheet from your activity like this:
 * <pre>
 *    AddNewWorldClock().show(supportFragmentManager, "dialog")
 * </pre>
 */
class AddNewWorldClock : BottomSheetDialogFragment() {

    private var _binding: FragmentAddclockListBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAddclockListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.list.layoutManager = LinearLayoutManager(context)
        
        // 1. Get original IDs
        val timeZoneIds = TimeZone.getAvailableIDs()

        // 2. Create TimeZoneDisplay objects and format names
        val timeZoneDisplayList = timeZoneIds.mapNotNull { id ->
            val nameParts = id.split('/')
            if (nameParts.size > 1) {
                val displayName = nameParts.last().replace('_', ' ')
                TimeZoneDisplay(id = id, displayName = displayName)
            } else {
                TimeZoneDisplay(id = id, displayName = id)
            }
        }.sortedBy { it.displayName } // 3. Sort by display name
            
        // 4. Create adapter with List<TimeZoneDisplay>
        val adapter = TimeZoneAdapter(timeZoneDisplayList) { selectedTimeZoneId ->
            val fragmentManager = parentFragmentManager
            // Simplify fragment lookup (assuming standard NavHost usage)
            val navHostFragment = fragmentManager.primaryNavigationFragment 
            val worldClockFragment = navHostFragment?.childFragmentManager?.primaryNavigationFragment as? WorldClockFragment
                
            val displayName = timeZoneDisplayList.firstOrNull { it.id == selectedTimeZoneId }?.displayName ?: selectedTimeZoneId
            
            if (worldClockFragment != null) {
                // Pass the original ID
                worldClockFragment.viewModel.addWorldClock(selectedTimeZoneId)
                Toast.makeText(context, "Added $displayName", Toast.LENGTH_SHORT).show()
            } else {
                // Fallback might not be ideal, consider communication via ViewModel/shared state
                val activityViewModel = ViewModelProvider(requireActivity())[WorldClockViewModel::class.java]
                activityViewModel.addWorldClock(selectedTimeZoneId)
                Toast.makeText(context, "Added $displayName (Activity Scope)", Toast.LENGTH_SHORT).show()
            }
            
            dismiss()
        }
        
        binding.list.adapter = adapter
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}