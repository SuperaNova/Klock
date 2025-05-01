package cit.edu.KlockApp.ui.main.worldClock

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SearchView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import cit.edu.KlockApp.R
import cit.edu.KlockApp.databinding.FragmentWorldclockBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java.util.TimeZone

class WorldClockFragment : Fragment() {

    private var _binding: FragmentWorldclockBinding? = null
    private val binding get() = _binding!!
    internal lateinit var viewModel: WorldClockViewModel
    private lateinit var worldClockListAdapter: WorldClockAdapter

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

        viewModel = ViewModelProvider(this)[WorldClockViewModel::class.java]

        setupRecyclerView()
        observeViewModel()
        setupFab()
    }

    private fun setupRecyclerView() {
        worldClockListAdapter = WorldClockAdapter()
        binding.worldClockRecyclerView.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = worldClockListAdapter
        }
        setupSwipeToDelete(binding.worldClockRecyclerView)
    }

    private fun setupSwipeToDelete(recyclerView: RecyclerView) {
        ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT) {
            override fun onMove(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder, target: RecyclerView.ViewHolder) = false

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val position = viewHolder.bindingAdapterPosition
                if (position != RecyclerView.NO_POSITION && position < worldClockListAdapter.currentList.size) {
                    val item = worldClockListAdapter.currentList[position]
                    viewModel.removeWorldClock(item.timeZoneId)
                }
            }
        }).attachToRecyclerView(recyclerView)
    }

    private fun observeViewModel() {
        viewModel.worldClocks.observe(viewLifecycleOwner) { worldClocks ->
            worldClockListAdapter.submitList(worldClocks)
        }
    }

    private fun setupFab() {
        binding.fabAddWorldClock.setOnClickListener {
            showTimeZoneSelectionDialog()
        }
    }

    fun showTimeZoneSelectionDialog() {
        val timeZones = TimeZone.getAvailableIDs()
            .map { it.replace("_", " ") }
            .sorted()

        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_timezone_search, null)
        val searchView = dialogView.findViewById<SearchView>(R.id.searchView)
        val timezoneRecyclerView = dialogView.findViewById<RecyclerView>(R.id.timezoneRecyclerView)

        val dialog = MaterialAlertDialogBuilder(requireContext(), R.style.ThemeOverlay_App_MaterialAlertDialog)
            .setTitle("Select Time Zone")
            .setView(dialogView)
            .setNegativeButton(android.R.string.cancel, null)
            .create()

        val timezoneAdapter = TimeZoneAdapter(timeZones) { selectedTimeZone ->
            viewModel.addWorldClock(selectedTimeZone.replace(" ", "_"))
            dialog.dismiss()
        }

        timezoneRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        timezoneRecyclerView.adapter = timezoneAdapter

        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean = false

            override fun onQueryTextChange(newText: String?): Boolean {
                timezoneAdapter.filter(newText ?: "")
                return true
            }
        })

        dialog.show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}