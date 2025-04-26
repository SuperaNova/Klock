package cit.edu.KlockApp.ui.main.timer

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.NumberPicker
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import cit.edu.KlockApp.databinding.FragmentAddEditPresetBinding
import java.util.concurrent.TimeUnit

class AddEditPresetFragment : Fragment() {

    private var _binding: FragmentAddEditPresetBinding? = null
    private val binding get() = _binding!!

    // Use activityViewModels to get the shared ViewModel instance
    private val viewModel: TimerViewModel by activityViewModels()

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

        setupNumberPickers()
        setupButtons()
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

        // TODO: Add logic here if editing an existing preset to populate pickers
        binding.pickerHoursPreset.value = 0
        binding.pickerMinutesPreset.value = 0
        binding.pickerSecondsPreset.value = 10 // Default to 10s for new presets maybe?
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
        val name = binding.etPresetName.text.toString().trim()
        val hours = binding.pickerHoursPreset.value
        val minutes = binding.pickerMinutesPreset.value
        val seconds = binding.pickerSecondsPreset.value

        // Basic Validation
        if (name.isEmpty()) {
            binding.tilPresetName.error = "Preset name cannot be empty"
            return
        } else {
            binding.tilPresetName.error = null // Clear error if name is now valid
        }

        val durationMillis = TimeUnit.HOURS.toMillis(hours.toLong()) +
                             TimeUnit.MINUTES.toMillis(minutes.toLong()) +
                             TimeUnit.SECONDS.toMillis(seconds.toLong())

        if (durationMillis <= 0) {
            Toast.makeText(context, "Preset duration must be greater than 0 seconds", Toast.LENGTH_SHORT).show()
            return
        }

        // TODO: Add logic to check for duplicate names if required by ViewModel/UI
        // val presetExists = viewModel.presets.value?.any { it.name.equals(name, ignoreCase = true) } ?: false
        // if (presetExists) { ... handle error ... }

        // Call ViewModel to add the preset
        viewModel.addPreset(name, durationMillis)

        // Navigate back
        findNavController().navigateUp()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
} 