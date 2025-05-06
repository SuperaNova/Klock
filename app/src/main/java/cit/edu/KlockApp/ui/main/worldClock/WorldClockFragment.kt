package cit.edu.KlockApp.ui.main.worldClock

import android.os.Bundle
import android.view.*
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.setFragmentResultListener
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import cit.edu.KlockApp.databinding.FragmentWorldclockBinding
import cit.edu.KlockApp.util.OnItemMoveListener
import cit.edu.KlockApp.util.SimpleItemTouchHelperCallback

class WorldClockFragment : Fragment(), OnItemMoveListener {

    private var _binding: FragmentWorldclockBinding? = null
    private val binding get() = _binding!!
    private val viewModel: WorldClockViewModel by activityViewModels()
    private lateinit var worldClockAdapter: WorldClockAdapter
    private var itemTouchHelper: ItemTouchHelper? = null
    private var previousClockListSize = 0 // Variable to store previous list size

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setFragmentResultListener(TimeZoneSelectorBottomSheetDialogFragment.REQUEST_KEY) { requestKey, bundle ->
            android.util.Log.d("WorldClockFragment", "Fragment result received: Key=$requestKey")
            val selectedTimeZoneId = bundle.getString(TimeZoneSelectorBottomSheetDialogFragment.SELECTED_TIMEZONE_ID_KEY)
            android.util.Log.d("WorldClockFragment", "Received TimeZone ID: $selectedTimeZoneId")
            selectedTimeZoneId?.let {
                viewModel.addWorldClock(it)
            }
        }
    }

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
        setupRecyclerView()
        observeViewModel()
    }

    private fun setupRecyclerView() {
        val callback = SimpleItemTouchHelperCallback(this)
        itemTouchHelper = ItemTouchHelper(callback)

        worldClockAdapter = WorldClockAdapter(itemTouchHelper!!) { clockItem ->
            viewModel.removeWorldClock(clockItem.timeZoneId)
        }

        binding.worldClockRecyclerView.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = worldClockAdapter
        }
        itemTouchHelper?.attachToRecyclerView(binding.worldClockRecyclerView)
    }

    private fun observeViewModel() {
        viewModel.worldClocks.observe(viewLifecycleOwner) { clocks ->
            android.util.Log.d(
                "WorldClockFragment",
                "WorldClocks observer triggered. Received list size: ${clocks.size}, List: $clocks"
            )

            // Submit the list with a completion callback
            worldClockAdapter.submitList(clocks) {
                // This runnable executes after the diff calculation completes
                val newSize = worldClockAdapter.currentList.size
                android.util.Log.d(
                    "WorldClockFragment",
                    "submitList completed. Adapter list size: $newSize. Comparing to previous size: $previousClockListSize"
                )

                // Check if an item was added
                if (previousClockListSize < newSize) {
                    binding.worldClockRecyclerView.post {
                        binding.worldClockRecyclerView.smoothScrollToPosition(newSize - 1)
                    }
                }
                // Update the stored size using the adapter's current list size after update
                previousClockListSize = newSize
            }
        }

        viewModel.isEditMode.observe(viewLifecycleOwner) { isEditing ->
            worldClockAdapter.setEditMode(isEditing)
            binding.fabAddWorldClock.isEnabled = !isEditing
        }
    }

    // Public function for Activity to call
    fun toggleEditMode() {
        viewModel.toggleEditMode()
    }

    fun showTimeZoneSelector() {
        if (parentFragmentManager.findFragmentByTag(TimeZoneSelectorBottomSheetDialogFragment.TAG) == null) {
            TimeZoneSelectorBottomSheetDialogFragment.newInstance()
                .show(parentFragmentManager, TimeZoneSelectorBottomSheetDialogFragment.TAG)
        }
    }

    override fun onDestroyView() {
        viewModel.exitEditMode()
        itemTouchHelper?.attachToRecyclerView(null)
        itemTouchHelper = null
        super.onDestroyView()
        _binding = null
    }

    override fun onItemMove(fromPosition: Int, toPosition: Int) {
        viewModel.moveClock(fromPosition, toPosition)
    }
}