package cit.edu.KlockApp.ui.main.stopwatch

import android.content.Context
import android.content.res.Resources
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import cit.edu.KlockApp.R
import cit.edu.KlockApp.databinding.FragmentStopwatchBinding
import android.graphics.Color
import android.util.TypedValue
import androidx.annotation.AttrRes
import androidx.annotation.ColorInt

class StopwatchFragment : Fragment() {

    private var _binding: FragmentStopwatchBinding? = null
    private val binding get() = _binding!!
    private lateinit var viewModel: StopwatchViewModel
    private lateinit var lapAdapter: LapAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentStopwatchBinding.inflate(inflater, container, false)
        viewModel = ViewModelProvider(this)[StopwatchViewModel::class.java]
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupLapRecyclerView()
        setupButtons()
        observeViewModel()
    }

    private fun setupLapRecyclerView() {
        lapAdapter = LapAdapter()
        binding.lapRecyclerView.adapter = lapAdapter
        // LayoutManager is set in XML
    }

    private fun setupButtons() {
        binding.startStopButton.setOnClickListener {
            when (viewModel.state.value) {
                StopwatchState.RUNNING -> viewModel.pause()
                StopwatchState.PAUSED -> viewModel.start() // start handles resume
                StopwatchState.IDLE -> viewModel.start()
                null -> {} // Should not happen
            }
        }

        binding.lapResetButton.setOnClickListener {
            when (viewModel.state.value) {
                StopwatchState.RUNNING -> viewModel.lap()
                StopwatchState.PAUSED, StopwatchState.IDLE -> viewModel.reset()
                null -> {} // Should not happen
            }
        }
    }

    private fun observeViewModel() {
        viewModel.elapsedTimeMillis.observe(viewLifecycleOwner) { millis ->
            binding.analogStopwatch.setElapsedTime(millis)
        }

        viewModel.formattedTime.observe(viewLifecycleOwner) { formattedTime ->
            binding.digitalStopwatch.text = formattedTime
        }

        viewModel.state.observe(viewLifecycleOwner) { state ->
            updateButtonStates(state)
        }

        viewModel.laps.observe(viewLifecycleOwner) { laps ->
            lapAdapter.submitList(laps)
        }
    }

    private fun updateButtonStates(state: StopwatchState) {
        val context = requireContext()
        when (state) {
            StopwatchState.IDLE -> {
                binding.startStopButton.text = getString(R.string.start)
                binding.startStopButton.setBackgroundColor(resolveThemeColor(context, com.google.android.material.R.attr.colorPrimary))
                binding.lapResetButton.text = getString(R.string.reset)
                binding.lapResetButton.isEnabled = false
            }
            StopwatchState.RUNNING -> {
                binding.startStopButton.text = getString(R.string.pause)
                binding.startStopButton.setBackgroundColor(resolveThemeColor(context, com.google.android.material.R.attr.colorError))
                binding.lapResetButton.text = getString(R.string.lap)
                binding.lapResetButton.isEnabled = true
            }
            StopwatchState.PAUSED -> {
                binding.startStopButton.text = getString(R.string.resume)
                binding.startStopButton.setBackgroundColor(resolveThemeColor(context, com.google.android.material.R.attr.colorPrimary))
                binding.lapResetButton.text = getString(R.string.reset)
                binding.lapResetButton.isEnabled = true
            }
        }
    }

    @ColorInt
    private fun resolveThemeColor(context: Context, @AttrRes attr: Int): Int {
        val typedValue = TypedValue()
        val theme: Resources.Theme = context.theme
        if (theme.resolveAttribute(attr, typedValue, true)) {
            return typedValue.data
        } 
        // Fallback color if attribute not found (shouldn't normally happen with Material theme)
        return Color.GRAY 
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}