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

class TimerFragment : Fragment() {

    private var _binding: FragmentTimerBinding? = null
    private val binding get() = _binding!!

    private val viewModel: TimerViewModel by activityViewModels()

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
        setupPresetControls()
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
        binding.pickerSeconds.value = 0
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

                if (durationMillis > 0) {
                    viewModel.setInitialDuration(durationMillis)
                    viewModel.startTimer()
                } else {
                    Toast.makeText(context, "Please set a timer duration", Toast.LENGTH_SHORT).show()
                }
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

    private fun setupPresetControls() {
        binding.presetButtonAdd.setOnClickListener {
            findNavController().navigate(R.id.action_timerFragment_to_addEditPresetFragment)
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
            populatePresetButtons(presets)
            updatePresetButtonsEnabled(viewModel.state.value == TimerState.IDLE)
        }
    }

    private fun populatePresetButtons(presets: List<TimerPreset>) {
        val container = binding.dynamicPresetRowsContainer
        container.removeAllViews()

        val rowLayoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply {
            bottomMargin = resources.getDimensionPixelSize(R.dimen.preset_row_margin_bottom)
        }

        val buttonLayoutParams = LinearLayout.LayoutParams(
            resources.getDimensionPixelSize(R.dimen.preset_button_fixed_width),
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply {
            marginEnd = resources.getDimensionPixelSize(R.dimen.preset_button_margin_end)
            topMargin = resources.getDimensionPixelSize(R.dimen.preset_button_margin_vertical)
            bottomMargin = resources.getDimensionPixelSize(R.dimen.preset_button_margin_vertical)
        }

        val maxButtonsPerRow = 3
        var currentHorizontalLayout: LinearLayout? = null

        presets.forEachIndexed { index, preset ->
            if (index % maxButtonsPerRow == 0) {
                currentHorizontalLayout = LinearLayout(context).apply {
                    orientation = LinearLayout.HORIZONTAL
                    layoutParams = rowLayoutParams
                    gravity = Gravity.CENTER_HORIZONTAL
                }
                container.addView(currentHorizontalLayout)
            }

            val button = MaterialButton(requireContext(), null, com.google.android.material.R.attr.materialButtonOutlinedStyle).apply {
                val formattedTime = formatMillisToShortTime(preset.durationMillis)
                text = "${preset.emojiIcon}\n$formattedTime"
                contentDescription = getString(R.string.preset_button_content_description, preset.emojiIcon, formattedTime)
                layoutParams = buttonLayoutParams
                minLines = 2
                maxLines = 2
                isSingleLine = false

                setOnClickListener {
                    if (viewModel.state.value == TimerState.IDLE) {
                        updatePickersFromMillis(preset.durationMillis)
                    }
                }
                setOnLongClickListener {
                    showDeletePresetConfirmationDialog(preset)
                    true
                }
                setPadding(0, resources.getDimensionPixelSize(R.dimen.preset_button_padding_vertical), 0, resources.getDimensionPixelSize(R.dimen.preset_button_padding_vertical))
                textSize = resources.getDimension(R.dimen.preset_button_text_size) / resources.displayMetrics.scaledDensity
            }

            if ((index + 1) % maxButtonsPerRow == 0 || index == presets.size - 1) {
                 (button.layoutParams as LinearLayout.LayoutParams).marginEnd = 0
            } else {
                 (button.layoutParams as LinearLayout.LayoutParams).marginEnd = resources.getDimensionPixelSize(R.dimen.preset_button_margin_end)
            }

            currentHorizontalLayout?.addView(button)
        }
        updatePresetButtonsEnabled(viewModel.state.value == TimerState.IDLE)
    }

    private fun formatMillisToShortTime(millis: Long): String {
        if (millis <= 0) return "0s"
        val hours = TimeUnit.MILLISECONDS.toHours(millis)
        val minutes = TimeUnit.MILLISECONDS.toMinutes(millis) % 60
        val seconds = TimeUnit.MILLISECONDS.toSeconds(millis) % 60

        val builder = StringBuilder()
        if (hours > 0) {
            builder.append(hours).append("h")
        }
        if (minutes > 0) {
            if (builder.isNotEmpty()) builder.append(" ")
            builder.append(minutes).append("m")
        }
        if (seconds > 0 && hours == 0L && minutes == 0L) {
             if (builder.isNotEmpty()) builder.append(" ")
             builder.append(seconds).append("s")
        } else if (builder.isEmpty()){
             builder.append("0s")
        }

        return builder.toString()
    }

    private fun updatePickersFromMillis(durationMillis: Long) {
        val hours = TimeUnit.MILLISECONDS.toHours(durationMillis).toInt()
        val minutes = (TimeUnit.MILLISECONDS.toMinutes(durationMillis) % 60).toInt()
        val seconds = (TimeUnit.MILLISECONDS.toSeconds(durationMillis) % 60).toInt()
        binding.pickerHours.value = hours
        binding.pickerMinutes.value = minutes
        binding.pickerSeconds.value = seconds
    }

    private fun showDeletePresetConfirmationDialog(preset: TimerPreset) {
         MaterialAlertDialogBuilder(requireContext())
            .setTitle("Delete Preset")
            .setMessage("Are you sure you want to delete the preset '${preset.emojiIcon}'?")
            .setPositiveButton("Delete") { dialog, which ->
                viewModel.deletePreset(preset.id)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun updateUI(state: TimerState) {
        val isIdle = state == TimerState.IDLE
        val isRunning = state == TimerState.RUNNING
        val isPaused = state == TimerState.PAUSED
        val isFinished = state == TimerState.FINISHED

        binding.groupTimerSetup.isVisible = isIdle || isFinished
        binding.groupTimerRunning.isVisible = isRunning || isPaused

        binding.cancelButton.isVisible = isRunning || isPaused || isFinished
        binding.cancelButton.isEnabled = isRunning || isPaused || isFinished

        when (state) {
            TimerState.IDLE -> {
                binding.startPauseButton.text = getString(R.string.start)
                binding.startPauseButton.isEnabled = true
                binding.startPauseButton.setBackgroundColor(resolveThemeColor(requireContext(), com.google.android.material.R.attr.colorPrimary))
            }
            TimerState.RUNNING -> {
                binding.startPauseButton.text = getString(R.string.pause)
                binding.startPauseButton.isEnabled = true
                binding.startPauseButton.setBackgroundColor(resolveThemeColor(requireContext(), com.google.android.material.R.attr.colorError))
            }
            TimerState.PAUSED -> {
                binding.startPauseButton.text = getString(R.string.resume)
                binding.startPauseButton.isEnabled = true
                binding.startPauseButton.setBackgroundColor(resolveThemeColor(requireContext(), com.google.android.material.R.attr.colorPrimary))
            }
            TimerState.FINISHED -> {
                binding.startPauseButton.text = getString(R.string.start)
                binding.startPauseButton.isEnabled = true
                binding.startPauseButton.setBackgroundColor(resolveThemeColor(requireContext(), com.google.android.material.R.attr.colorPrimary))
                Toast.makeText(context, "Timer Finished!", Toast.LENGTH_SHORT).show()
            }
        }

        updatePresetButtonsEnabled(isIdle)
    }

    private fun updatePresetButtonsEnabled(enabled: Boolean) {
        binding.presetButtonAdd.isEnabled = enabled
        val rowsContainer = binding.dynamicPresetRowsContainer
        for (i in 0 until rowsContainer.childCount) {
            val row = rowsContainer.getChildAt(i)
            if (row is LinearLayout) {
                for (j in 0 until row.childCount) {
                    val button = row.getChildAt(j)
                    if (button is Button) {
                        button.isEnabled = enabled
                    }
                }
                row.alpha = if (enabled) 1.0f else 0.5f
            }
        }
        binding.presetButtonAdd.alpha = if (enabled) 1.0f else 0.5f
    }

    @ColorInt
    private fun resolveThemeColor(context: Context, @AttrRes attr: Int): Int {
        val typedValue = TypedValue()
        val theme: Resources.Theme = context.theme
        if (theme.resolveAttribute(attr, typedValue, true)) {
            return typedValue.data
        }
        return Color.GRAY
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}