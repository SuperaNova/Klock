package cit.edu.KlockApp.ui.main.timer

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.NumberPicker
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import cit.edu.KlockApp.databinding.FragmentAddEditPresetBinding
import java.util.concurrent.TimeUnit

class AddEditPresetFragment : Fragment() {

    private var _binding: FragmentAddEditPresetBinding? = null
    private val binding get() = _binding!!

    // Use activityViewModels to get the shared ViewModel instance
    private val viewModel: TimerViewModel by activityViewModels()
    private val args: AddEditPresetFragmentArgs by navArgs()

    private var presetToEdit: TimerPreset? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAddEditPresetBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Check if we are editing an existing preset
        args.presetId?.let { presetId ->
            presetToEdit = viewModel.getPresetById(presetId)
            presetToEdit?.let { preset ->
                populateFieldsForEdit(preset)
                // Optionally change title or button text
                binding.buttonSavePreset.text = "Update"
            }
        }

        setupNumberPickers()
        setupButtons()
    }

    private fun populateFieldsForEdit(preset: TimerPreset) {
        binding.etPresetEmoji.setText(preset.emojiIcon)
        // Populate NumberPickers based on preset.durationMillis
        val hours = TimeUnit.MILLISECONDS.toHours(preset.durationMillis).toInt()
        val minutes = (TimeUnit.MILLISECONDS.toMinutes(preset.durationMillis) % 60).toInt()
        val seconds = (TimeUnit.MILLISECONDS.toSeconds(preset.durationMillis) % 60).toInt()
        binding.pickerHoursPreset.value = hours
        binding.pickerMinutesPreset.value = minutes
        binding.pickerSecondsPreset.value = seconds
    }

    private fun setupNumberPickers() {
        // Configure NumberPickers (similar to TimerFragment)
        binding.pickerHoursPreset.minValue = 0
        binding.pickerHoursPreset.maxValue = 23
        binding.pickerMinutesPreset.minValue = 0
        binding.pickerMinutesPreset.maxValue = 59
        binding.pickerSecondsPreset.minValue = 0
        binding.pickerSecondsPreset.maxValue = 59

        val formatter = NumberPicker.Formatter { String.format("%02d", it) }
        binding.pickerHoursPreset.setFormatter(formatter)
        binding.pickerMinutesPreset.setFormatter(formatter)
        binding.pickerSecondsPreset.setFormatter(formatter)

        // Listener to enforce minimum 1 second
        val listener = NumberPicker.OnValueChangeListener { _, _, _ ->
            if (binding.pickerHoursPreset.value == 0 && binding.pickerMinutesPreset.value == 0 && binding.pickerSecondsPreset.value == 0) {
                binding.pickerSecondsPreset.postDelayed({ 
                    if (binding.pickerHoursPreset.value == 0 && binding.pickerMinutesPreset.value == 0 && binding.pickerSecondsPreset.value == 0) {
                        binding.pickerSecondsPreset.value = 1 
                    }
                }, 150)
            }
        }
        binding.pickerHoursPreset.setOnValueChangedListener(listener)
        binding.pickerMinutesPreset.setOnValueChangedListener(listener)
        binding.pickerSecondsPreset.setOnValueChangedListener(listener)

        // Default values only apply if not editing
        if (presetToEdit == null) {
             binding.pickerHoursPreset.value = 0
             binding.pickerMinutesPreset.value = 0
             binding.pickerSecondsPreset.value = 1 // Default to 1 second
        }
    }

    private fun setupButtons() {
        binding.buttonCancelPreset.setOnClickListener {
            findNavController().navigateUp() // Go back to the previous screen
        }

        binding.buttonSavePreset.setOnClickListener {
            savePresetAndReturn()
        }
    }

    private fun savePresetAndReturn() {
        val emoji = binding.etPresetEmoji.text.toString().trim()
        val hours = binding.pickerHoursPreset.value
        val minutes = binding.pickerMinutesPreset.value
        val seconds = binding.pickerSecondsPreset.value

        val durationMillis = TimeUnit.HOURS.toMillis(hours.toLong()) +
                             TimeUnit.MINUTES.toMillis(minutes.toLong()) +
                             TimeUnit.SECONDS.toMillis(seconds.toLong())

        var finalDurationMillis = durationMillis
        if (finalDurationMillis <= 0) {
            finalDurationMillis = 1000L // Force minimum 1 second
        }

        // Call ViewModel to add or update the preset with potentially adjusted duration
        if (presetToEdit != null) {
            viewModel.updatePreset(presetToEdit!!.id, emoji, finalDurationMillis)
        } else {
            viewModel.addPreset(emoji, finalDurationMillis)
        }

        findNavController().navigateUp()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
} 