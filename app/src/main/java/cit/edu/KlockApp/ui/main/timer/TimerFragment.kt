package cit.edu.KlockApp.ui.main.timer

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.NumberPicker
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import cit.edu.KlockApp.R
import cit.edu.KlockApp.databinding.FragmentTimerBinding
import android.graphics.Color
import android.util.TypedValue
import android.content.res.Resources
import androidx.annotation.AttrRes
import androidx.annotation.ColorInt

class TimerFragment : Fragment() {

    private var _binding: FragmentTimerBinding? = null
    private val binding get() = _binding!!
    private lateinit var viewModel: TimerViewModel

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTimerBinding.inflate(inflater, container, false)
        viewModel = ViewModelProvider(this)[TimerViewModel::class.java]
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupNumberPickers()
        setupButtons()
        observeViewModel()
    }

    private fun setupNumberPickers() {
        binding.pickerHours.minValue = 0
        binding.pickerHours.maxValue = 23
        binding.pickerMinutes.minValue = 0
        binding.pickerMinutes.maxValue = 59
        binding.pickerSeconds.minValue = 0
        binding.pickerSeconds.maxValue = 59

        // Optional: Add formatters if desired to ensure leading zeros visually
        val formatter = NumberPicker.Formatter { String.format("%02d", it) }
        binding.pickerHours.setFormatter(formatter)
        binding.pickerMinutes.setFormatter(formatter)
        binding.pickerSeconds.setFormatter(formatter)
    }

    private fun setupButtons() {
        binding.startPauseButton.setOnClickListener {
            val hours = binding.pickerHours.value
            val minutes = binding.pickerMinutes.value
            val seconds = binding.pickerSeconds.value

            when (viewModel.state.value) {
                TimerState.IDLE -> {
                    viewModel.setTimer(hours, minutes, seconds)
                    viewModel.startTimer()
                }
                TimerState.RUNNING -> viewModel.pauseTimer()
                TimerState.PAUSED -> viewModel.resumeTimer()
                TimerState.FINISHED -> { /* Maybe restart with same time? */ }
                null -> {}
            }
        }

        binding.cancelButton.setOnClickListener {
            viewModel.cancelTimer()
        }
    }

    private fun observeViewModel() {
        viewModel.formattedTime.observe(viewLifecycleOwner) { time ->
            binding.timerDigitalDisplay.text = time
        }

        viewModel.progressPercentage.observe(viewLifecycleOwner) { progress ->
            binding.timerProgressCircle.progress = progress
        }
        
        viewModel.endTimeFormatted.observe(viewLifecycleOwner) { endTime ->
            binding.timerEndTimeText.text = endTime
            binding.timerEndTimeText.isVisible = endTime != null
        }

        viewModel.state.observe(viewLifecycleOwner) { state ->
            updateUiForState(state)
        }
    }

    private fun updateUiForState(state: TimerState) {
        val isIdle = state == TimerState.IDLE
        val isRunning = state == TimerState.RUNNING
        val isPaused = state == TimerState.PAUSED
        
        binding.groupTimerSetup.isVisible = isIdle
        binding.groupTimerRunning.isVisible = !isIdle
        binding.cancelButton.isVisible = isRunning || isPaused

        when (state) {
            TimerState.IDLE -> {
                binding.startPauseButton.text = getString(R.string.start)
                binding.startPauseButton.setBackgroundColor(resolveThemeColor(requireContext(), com.google.android.material.R.attr.colorPrimary))
                binding.cancelButton.text = getString(R.string.cancel) // Though invisible
            }
            TimerState.RUNNING -> {
                binding.startPauseButton.text = getString(R.string.pause)
                binding.startPauseButton.setBackgroundColor(resolveThemeColor(requireContext(), com.google.android.material.R.attr.colorError))
                binding.cancelButton.text = getString(R.string.cancel)
            }
            TimerState.PAUSED -> {
                binding.startPauseButton.text = getString(R.string.resume)
                binding.startPauseButton.setBackgroundColor(resolveThemeColor(requireContext(), com.google.android.material.R.attr.colorPrimary))
                binding.cancelButton.text = getString(R.string.cancel)
            }
            TimerState.FINISHED -> {
                // Could briefly show 'Finished' or just reset to IDLE
                binding.startPauseButton.text = getString(R.string.start) 
                binding.startPauseButton.setBackgroundColor(resolveThemeColor(requireContext(), com.google.android.material.R.attr.colorPrimary))
            }
        }
    }

    // Re-use the helper function from StopwatchFragment (or move to a common Util class)
    @ColorInt
    private fun resolveThemeColor(context: Context, @AttrRes attr: Int): Int {
        val typedValue = TypedValue()
        val theme: Resources.Theme = context.theme
        if (theme.resolveAttribute(attr, typedValue, true)) {
            return typedValue.data
        } 
        return Color.GRAY // Fallback
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}