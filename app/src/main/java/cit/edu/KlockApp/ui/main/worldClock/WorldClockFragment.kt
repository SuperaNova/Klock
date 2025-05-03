package cit.edu.KlockApp.ui.main.worldClock

import android.graphics.Canvas
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.*
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.setFragmentResultListener
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import cit.edu.KlockApp.R
import cit.edu.KlockApp.databinding.FragmentWorldclockBinding
import cit.edu.KlockApp.databinding.FragmentWorldclockItemBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit
import cit.edu.KlockApp.ui.util.OnItemMoveListener
import cit.edu.KlockApp.ui.util.SimpleItemTouchHelperCallback

class WorldClockFragment : Fragment(), OnItemMoveListener {

    private var _binding: FragmentWorldclockBinding? = null
    private val binding get() = _binding!!
    private val viewModel: WorldClockViewModel by activityViewModels()
    private lateinit var worldClockAdapter: WorldClockAdapter
    private var itemTouchHelper: ItemTouchHelper? = null
    private var editMenuItem: MenuItem? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)

        setFragmentResultListener(TimeZoneSelectorBottomSheetDialogFragment.REQUEST_KEY) { _, bundle ->
            val selectedTimeZoneId = bundle.getString(TimeZoneSelectorBottomSheetDialogFragment.SELECTED_TIMEZONE_ID_KEY)
            selectedTimeZoneId?.let {
                viewModel.addWorldClock(it)
                binding.worldClockRecyclerView.postDelayed({
                   binding.worldClockRecyclerView.smoothScrollToPosition(worldClockAdapter.itemCount - 1)
                }, 100)
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
        setupFab()
    }

    private fun setupRecyclerView() {
        val callback = SimpleItemTouchHelperCallback(this)
        itemTouchHelper = ItemTouchHelper(callback)

        worldClockAdapter = WorldClockAdapter(itemTouchHelper!!) { clockItem ->
            // Replaced direct delete confirmation with Snackbar UNDO logic in onSwiped
            // showDeleteConfirmation(clockItem)
            // Let's use the onSwiped logic primarily, long-press might be redundant or need different handling
            // For now, we can trigger the delete via ViewModel directly on long-press if desired,
            // but the swipe-to-delete provides UNDO. Let's keep the swipe for deletion.
            // Maybe long-press could enter edit mode in the future?
        }

        binding.worldClockRecyclerView.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = worldClockAdapter
        }
        itemTouchHelper?.attachToRecyclerView(binding.worldClockRecyclerView)
    }

    private fun observeViewModel() {
        viewModel.worldClocks.observe(viewLifecycleOwner, Observer { clocks ->
            worldClockAdapter.submitList(clocks)
        })

        viewModel.isEditMode.observe(viewLifecycleOwner) { isEditing ->
            worldClockAdapter.setEditMode(isEditing)
            updateEditMenuIcon(isEditing)
            binding.fabAddWorldClock.isEnabled = !isEditing
        }
    }

    private fun setupFab() {
        binding.fabAddWorldClock.setOnClickListener {
            showTimeZoneSelector()
        }
    }

    fun showTimeZoneSelector() {
        if (childFragmentManager.findFragmentByTag(TimeZoneSelectorBottomSheetDialogFragment.TAG) == null) {
            TimeZoneSelectorBottomSheetDialogFragment.newInstance()
                .show(childFragmentManager, TimeZoneSelectorBottomSheetDialogFragment.TAG)
        }
    }

    override fun onDestroyView() {
        viewModel.exitEditMode()
        itemTouchHelper?.attachToRecyclerView(null)
        itemTouchHelper = null
        editMenuItem = null
        super.onDestroyView()
        _binding = null
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
                showTimeZoneSelector()
                true
            }
            R.id.action_edit_reorder -> {
                viewModel.toggleEditMode()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}