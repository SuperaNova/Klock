package cit.edu.KlockApp.ui.main.timer

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.NumberPicker
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.ViewModelProvider
import cit.edu.KlockApp.R
import cit.edu.KlockApp.databinding.FragmentTimerBinding
import android.graphics.Color
import android.util.TypedValue
import android.content.res.Resources
import androidx.annotation.AttrRes
import androidx.annotation.ColorInt
import android.widget.Toast
import android.widget.Button
import android.widget.LinearLayout
import androidx.navigation.fragment.findNavController
import java.util.concurrent.TimeUnit
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.button.MaterialButton
import android.view.Gravity
import java.lang.StringBuilder
import androidx.recyclerview.widget.GridLayoutManager
import android.util.Log

class TimerFragment : Fragment() {

    private var _binding: FragmentTimerBinding? = null
    private val binding get() = _binding!!

    private val viewModel: TimerViewModel by activityViewModels()
    private lateinit var presetAdapter: TimerPresetAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTimerBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupNumberPickers()
        setupButtons()
        setupPresetRecyclerView()
        observeViewModel()
    }

    private fun setupNumberPickers() {
        binding.pickerHours.minValue = 0
        binding.pickerHours.maxValue = 23
        binding.pickerMinutes.minValue = 0
        binding.pickerMinutes.maxValue = 59
        binding.pickerSeconds.minValue = 0
        binding.pickerSeconds.maxValue = 59

        val formatter = NumberPicker.Formatter { String.format("%02d", it) }
        binding.pickerHours.setFormatter(formatter)
        binding.pickerMinutes.setFormatter(formatter)
        binding.pickerSeconds.setFormatter(formatter)

        binding.pickerHours.value = 0
        binding.pickerMinutes.value = 0
        binding.pickerSeconds.value = 1

        // Listener to enforce minimum 1 second
        val listener = NumberPicker.OnValueChangeListener { _, _, _ ->
            if (binding.pickerHours.value == 0 && binding.pickerMinutes.value == 0 && binding.pickerSeconds.value == 0) {
                // Use postDelayed to introduce a slight delay before correcting
                binding.pickerSeconds.postDelayed({ 
                    // Double-check the condition still holds after the delay
                    if (binding.pickerHours.value == 0 && binding.pickerMinutes.value == 0 && binding.pickerSeconds.value == 0) {
                         binding.pickerSeconds.value = 1 
                    }
                }, 150) // Delay in milliseconds (e.g., 150ms)
            }
        }

        binding.pickerHours.setOnValueChangedListener(listener)
        binding.pickerMinutes.setOnValueChangedListener(listener)
        binding.pickerSeconds.setOnValueChangedListener(listener)
    }

    private fun setupButtons() {
        binding.startPauseButton.setOnClickListener {
            val currentState = viewModel.state.value
            if (currentState == TimerState.IDLE) {
                val hours = binding.pickerHours.value
                val minutes = binding.pickerMinutes.value
                val seconds = binding.pickerSeconds.value
                val durationMillis = java.util.concurrent.TimeUnit.HOURS.toMillis(hours.toLong()) +
                                     java.util.concurrent.TimeUnit.MINUTES.toMillis(minutes.toLong()) +
                                     java.util.concurrent.TimeUnit.SECONDS.toMillis(seconds.toLong())

                var finalDurationMillis = durationMillis
                if (finalDurationMillis <= 0) {
                    // If duration is 0, force it to 1 second
                    finalDurationMillis = 1000L
                }
                viewModel.setInitialDuration(finalDurationMillis)
                viewModel.startTimer()
            } else {
                when (currentState) {
                    TimerState.RUNNING -> viewModel.pauseTimer()
                    TimerState.PAUSED -> viewModel.startTimer()
                    TimerState.FINISHED -> { viewModel.resetTimer() }
                    else -> {}
                }
            }
        }

        binding.cancelButton.setOnClickListener {
            viewModel.resetTimer()
        }
    }

    private fun setupPresetRecyclerView() {
        presetAdapter = TimerPresetAdapter(
            onPresetClick = { preset ->
                if (viewModel.state.value == TimerState.IDLE) {
                    updatePickersFromMillis(preset.durationMillis)
                } else {
                    Toast.makeText(context, "Stop the current timer to use a preset", Toast.LENGTH_SHORT).show()
                }
            },
            onAddClick = {
                Log.d("TimerFragment", "Add preset button clicked")
                try {
                    val action = TimerFragmentDirections.actionTimerFragmentToAddEditPresetFragment(null)
                    findNavController().navigate(action)
                    Log.d("TimerFragment", "Navigation action triggered")
                } catch (e: Exception) {
                    Log.e("TimerFragment", "Navigation failed", e)
                    Toast.makeText(context, "Error navigating. Please try again.", Toast.LENGTH_SHORT).show()
                }
            },
            onPresetLongClick = { preset ->
                showDeleteConfirmationDialog(preset)
            }
        )

        binding.presetsRecyclerView.apply {
            adapter = presetAdapter
            setHasFixedSize(true)
        }
    }

    private fun observeViewModel() {
        viewModel.formattedTime.observe(viewLifecycleOwner) { time ->
            binding.timerDigitalDisplay.text = time
        }

        viewModel.progressPercentage.observe(viewLifecycleOwner) { progress ->
            binding.timerProgressCircle.progress = progress
        }

        viewModel.state.observe(viewLifecycleOwner) { state ->
            updateUI(state)
        }

        viewModel.endTimeFormatted.observe(viewLifecycleOwner) { endTime ->
            binding.timerEndTimeText.text = endTime
            binding.timerEndTimeText.isVisible = endTime != null
        }

        viewModel.presets.observe(viewLifecycleOwner) { presets ->
            presetAdapter.submitList(presets ?: emptyList())
        }
    }

    private fun updateUI(state: TimerState) {
        val isIdle = state == TimerState.IDLE
        val isRunning = state == TimerState.RUNNING
        val isPaused = state == TimerState.PAUSED
        val isFinished = state == TimerState.FINISHED

        binding.groupTimerSetup.isVisible = isIdle
        binding.groupTimerRunning.isVisible = !isIdle

        // Hide presets RecyclerView if timer is not idle
        binding.presetsRecyclerView.isVisible = isIdle

        binding.cancelButton.isVisible = !isIdle
        binding.cancelButton.isEnabled = !isIdle

        when (state) {
            TimerState.IDLE -> {
                binding.startPauseButton.text = getString(R.string.start)
                binding.startPauseButton.isEnabled = true
                binding.startPauseButton.setBackgroundColor(getThemeColor(requireContext(), com.google.android.material.R.attr.colorPrimary))
                binding.cancelButton.isVisible = false
            }
            TimerState.RUNNING -> {
                binding.startPauseButton.text = getString(R.string.pause)
                binding.startPauseButton.isEnabled = true
                binding.startPauseButton.setBackgroundColor(getThemeColor(requireContext(), com.google.android.material.R.attr.colorSecondary))
            }
            TimerState.PAUSED -> {
                binding.startPauseButton.text = getString(R.string.resume)
                binding.startPauseButton.isEnabled = true
                binding.startPauseButton.setBackgroundColor(getThemeColor(requireContext(), com.google.android.material.R.attr.colorPrimary))
            }
            TimerState.FINISHED -> {
                binding.startPauseButton.text = getString(R.string.reset)
                binding.startPauseButton.isEnabled = true
                binding.startPauseButton.setBackgroundColor(getThemeColor(requireContext(), com.google.android.material.R.attr.colorPrimary))
                binding.cancelButton.isVisible = false
                Toast.makeText(context, "Timer Finished!", Toast.LENGTH_LONG).show()
            }
        }
    }

    @ColorInt
    fun getThemeColor(@AttrRes attrRes: Int): Int {
        val typedValue = TypedValue()
        requireContext().theme.resolveAttribute(attrRes, typedValue, true)
        return typedValue.data
    }

    @ColorInt
    private fun getThemeColor(context: Context, @AttrRes colorAttr: Int): Int {
        val typedValue = TypedValue()
        context.theme.resolveAttribute(colorAttr, typedValue, true)
        return typedValue.data
    }

    private fun updatePickersFromMillis(durationMillis: Long) {
        val hours = TimeUnit.MILLISECONDS.toHours(durationMillis).toInt()
        val minutes = (TimeUnit.MILLISECONDS.toMinutes(durationMillis) % 60).toInt()
        val seconds = (TimeUnit.MILLISECONDS.toSeconds(durationMillis) % 60).toInt()
        binding.pickerHours.value = hours
        binding.pickerMinutes.value = minutes
        binding.pickerSeconds.value = seconds
    }

    private fun showDeleteConfirmationDialog(preset: TimerPreset) {
         MaterialAlertDialogBuilder(requireContext())
            .setTitle("Delete Preset '${preset.emojiIcon}'?")
            .setMessage("Are you sure you want to delete this timer preset?")
            .setNegativeButton(R.string.cancel, null)
            .setPositiveButton("Delete") { _, _ ->
                viewModel.deletePreset(preset.id)
                Toast.makeText(context, "Preset deleted", Toast.LENGTH_SHORT).show()
            }
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}