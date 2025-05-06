package cit.edu.KlockApp.ui.main.stopwatch

import android.content.Context
import android.content.res.Resources
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import cit.edu.KlockApp.R
import cit.edu.KlockApp.databinding.FragmentStopwatchBinding
import android.graphics.Color
import android.util.TypedValue
import androidx.annotation.AttrRes
import androidx.annotation.ColorInt

class StopwatchFragment : Fragment() {

    private var _binding: FragmentStopwatchBinding? = null
    private val binding get() = _binding!!
    private val viewModel: StopwatchViewModel by viewModels()
    private lateinit var lapAdapter: LapAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentStopwatchBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        setupClickListeners()
        observeViewModel()
    }

    private fun setupRecyclerView() {
        lapAdapter = LapAdapter()
        binding.lapRecyclerView.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = lapAdapter
        }
    }

    private fun setupClickListeners() {
        binding.startStopButton.setOnClickListener {
            when (viewModel.state.value) {
                StopwatchState.RUNNING -> viewModel.pause()
                StopwatchState.PAUSED -> viewModel.start()
                StopwatchState.IDLE -> viewModel.start()
                null -> {}
            }
        }
        binding.lapResetButton.setOnClickListener {
            when (viewModel.state.value) {
                StopwatchState.RUNNING -> {
                    viewModel.lap()
                    binding.analogStopwatch.recordLap()
                }
                StopwatchState.PAUSED, StopwatchState.IDLE -> {
                    viewModel.reset()
                    binding.analogStopwatch.resetLaps()
                }
                null -> {}
            }
        }
    }

    private fun observeViewModel() {
        viewModel.elapsedTimeMillis.observe(viewLifecycleOwner, Observer { time ->
            binding.analogStopwatch.setElapsedTime(time)
        })

        viewModel.formattedTime.observe(viewLifecycleOwner, Observer { formattedTime ->
            binding.digitalStopwatch.text = formattedTime
        })

        viewModel.state.observe(viewLifecycleOwner, Observer { state ->
            updateButtonStates(state ?: StopwatchState.IDLE)
        })

        viewModel.laps.observe(viewLifecycleOwner, Observer { laps ->
            lapAdapter.submitList(laps)
            if (laps.isNotEmpty()) {
                binding.lapRecyclerView.post {
                    binding.lapRecyclerView.smoothScrollToPosition(0)
                }
            }
        })
    }

    private fun updateButtonStates(state: StopwatchState) {
        val context = requireContext()
        when (state) {
            StopwatchState.IDLE -> {
                binding.startStopButton.text = getString(R.string.start)
                binding.lapResetButton.text = getString(R.string.reset)
                binding.lapResetButton.isEnabled = false
            }
            StopwatchState.RUNNING -> {
                binding.startStopButton.text = getString(R.string.pause)
                binding.lapResetButton.text = getString(R.string.lap)
                binding.lapResetButton.isEnabled = true
            }
            StopwatchState.PAUSED -> {
                binding.startStopButton.text = getString(R.string.resume)
                binding.lapResetButton.text = getString(R.string.reset)
                binding.lapResetButton.isEnabled = true
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}