package cit.edu.KlockApp.ui.main.worldClock

import android.os.Bundle
import android.view.*
import android.widget.SearchView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import cit.edu.KlockApp.R
import cit.edu.KlockApp.databinding.FragmentWorldclockBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import java.util.TimeZone
import cit.edu.KlockApp.ui.util.OnItemMoveListener
import cit.edu.KlockApp.ui.util.SimpleItemTouchHelperCallback

class WorldClockFragment : Fragment(), OnItemMoveListener {

    private var _binding: FragmentWorldclockBinding? = null
    private val binding get() = _binding!!
    internal lateinit var viewModel: WorldClockViewModel
    private lateinit var worldClockListAdapter: WorldClockAdapter
    private var itemTouchHelper: ItemTouchHelper? = null
    private var editMenuItem: MenuItem? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentWorldclockBinding.inflate(inflater, container, false)
        viewModel = ViewModelProvider(this)[WorldClockViewModel::class.java]
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        observeViewModel()
        setupFab()
    }

    private fun setupRecyclerView() {
        val callback = SimpleItemTouchHelperCallback(this)
        itemTouchHelper = ItemTouchHelper(callback)
        
        worldClockListAdapter = WorldClockAdapter(itemTouchHelper!!) { clockItem ->
            showDeleteConfirmation(clockItem)
        }

        binding.worldClockRecyclerView.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = worldClockListAdapter
        }
        itemTouchHelper?.attachToRecyclerView(binding.worldClockRecyclerView)
    }

    private fun observeViewModel() {
        viewModel.worldClocks.observe(viewLifecycleOwner) { worldClocks ->
            worldClockListAdapter.submitList(worldClocks)
        }

        viewModel.isEditMode.observe(viewLifecycleOwner) { isEditing ->
            worldClockListAdapter.setEditMode(isEditing)
            updateEditMenuIcon(isEditing)
            binding.fabAddWorldClock.isEnabled = !isEditing
        }
    }

    private fun setupFab() {
        binding.fabAddWorldClock.setOnClickListener {
            showTimeZoneSelectionDialog()
        }
    }

    fun showTimeZoneSelectionDialog() {
        // 1. Get original IDs
        val timeZoneIds = TimeZone.getAvailableIDs()

        // 2. Create TimeZoneDisplay objects and format names
        val timeZoneDisplayList = timeZoneIds.mapNotNull { id ->
            // Simple formatting: Take part after last '/' and replace '_' with space
            // More robust formatting might involve checking for edge cases or using libraries
            val nameParts = id.split('/')
            if (nameParts.size > 1) { // Ensure there's a '/' to split by
                val displayName = nameParts.last().replace('_', ' ')
                TimeZoneDisplay(id = id, displayName = displayName)
            } else {
                // Handle cases without '/' (e.g., "UTC", "GMT") - use the ID itself
                TimeZoneDisplay(id = id, displayName = id)
            }
        }.sortedBy { it.displayName } // 3. Sort by display name

        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_timezone_search, null)
        val searchView = dialogView.findViewById<SearchView>(R.id.searchView)
        val timezoneRecyclerView = dialogView.findViewById<RecyclerView>(R.id.timezoneRecyclerView)

        val dialog = MaterialAlertDialogBuilder(requireContext(), R.style.ThemeOverlay_App_MaterialAlertDialog)
            .setTitle("Select Time Zone")
            .setView(dialogView)
            .setNegativeButton(android.R.string.cancel, null)
            .create()

        // 4. Pass the List<TimeZoneDisplay> to the adapter
        val timezoneAdapter = TimeZoneAdapter(timeZoneDisplayList) { selectedTimeZoneId ->
            // Callback still receives the original ID
            viewModel.addWorldClock(selectedTimeZoneId)
            dialog.dismiss()
        }

        timezoneRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        timezoneRecyclerView.adapter = timezoneAdapter

        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean = false

            override fun onQueryTextChange(newText: String?): Boolean {
                // 5. Adapter's filter method now works on TimeZoneDisplay list
                timezoneAdapter.filter(newText ?: "")
                return true
            }
        })

        dialog.show()
    }

    override fun onItemMove(fromPosition: Int, toPosition: Int) {
        viewModel.moveClock(fromPosition, toPosition)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.menu_main, menu)
        editMenuItem = menu.findItem(R.id.action_edit_reorder)
        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onPrepareOptionsMenu(menu: Menu) {
        val worldClockItem = menu.findItem(R.id.action_add)
        val editItem = menu.findItem(R.id.action_edit_reorder)
        val settingsItem = menu.findItem(R.id.action_settings)
        
        worldClockItem?.isVisible = true
        editItem?.isVisible = true
        settingsItem?.isVisible = true
        
        viewModel.isEditMode.value?.let { updateEditMenuIcon(it) }
        
        super.onPrepareOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_add -> {
                showTimeZoneSelectionDialog()
                true
            }
            R.id.action_edit_reorder -> {
                viewModel.toggleEditMode()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
    
    private fun updateEditMenuIcon(isEditing: Boolean) {
        if (isEditing) {
            editMenuItem?.setIcon(R.drawable.ic_done_24)
            editMenuItem?.title = "Done"
        } else {
            editMenuItem?.setIcon(R.drawable.ic_edit_24)
            editMenuItem?.title = "Edit"
        }
    }

    private fun showDeleteConfirmation(clockItem: WorldClockItem) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(getString(R.string.delete_world_clock))
            .setMessage("Remove ${clockItem.timeZoneId.substringAfterLast('/').replace('_', ' ')}?")
            .setNegativeButton(R.string.cancel, null)
            .setPositiveButton("Delete") { _, _ ->
                viewModel.removeWorldClock(clockItem.timeZoneId)
                Snackbar.make(binding.root, "Clock removed", Snackbar.LENGTH_SHORT).show()
            }
            .show()
    }

    override fun onDestroyView() {
        viewModel.exitEditMode()
        itemTouchHelper?.attachToRecyclerView(null)
        itemTouchHelper = null
        editMenuItem = null
        super.onDestroyView()
        _binding = null
    }
}