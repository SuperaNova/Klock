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
        
        // Get all timezone IDs and format them for display
        val timeZones = TimeZone.getAvailableIDs()
            .map { it.replace("_", " ") }
            .sorted()
            
        // Create adapter with the timezone list and a click handler
        val adapter = TimeZoneAdapter(timeZones) { selectedTimeZone ->
            // Find the parent WorldClockFragment to add the timezone
            val fragmentManager = parentFragmentManager
            val worldClockFragment = fragmentManager.findFragmentByTag("WorldClockFragment") as? WorldClockFragment
                ?: fragmentManager.primaryNavigationFragment?.childFragmentManager?.primaryNavigationFragment as? WorldClockFragment
                
            if (worldClockFragment != null) {
                // Add the world clock using the fragment's ViewModel (using the correct name 'viewModel')
                worldClockFragment.viewModel.addWorldClock(selectedTimeZone.replace(" ", "_"))
                Toast.makeText(context, "Added $selectedTimeZone", Toast.LENGTH_SHORT).show()
            } else {
                // Fallback: Create a new ViewModel instance scoped to the activity
                val activityViewModel = ViewModelProvider(requireActivity())[WorldClockViewModel::class.java]
                activityViewModel.addWorldClock(selectedTimeZone.replace(" ", "_"))
                Toast.makeText(context, "Added $selectedTimeZone (Activity Scope)", Toast.LENGTH_SHORT).show()
            }
            
            // Dismiss the dialog
            dismiss()
        }
        
        binding.list.adapter = adapter
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}