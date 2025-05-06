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
import android.text.format.DateFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.recyclerview.widget.ItemTouchHelper
import cit.edu.KlockApp.ui.util.OnItemMoveListener
import cit.edu.KlockApp.ui.util.SimpleItemTouchHelperCallback
import android.app.Activity
import android.content.Intent
import android.media.RingtoneManager
import android.net.Uri
import androidx.activity.result.contract.ActivityResultContracts
import android.provider.MediaStore
import android.app.AlertDialog

class TimerFragment : Fragment(), OnItemMoveListener {

    private var _binding: FragmentTimerBinding? = null
    private val binding get() = _binding!!

    private val viewModel: TimerViewModel by activityViewModels()
    private lateinit var presetAdapter: TimerPresetAdapter
    private var presetItemTouchHelper: ItemTouchHelper? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
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

        val initialMillis = viewModel.remainingTimeMillis.value ?: 0L
        updatePickersFromMillis(initialMillis.takeIf { it > 0 } ?: 1000L)

        val listener = NumberPicker.OnValueChangeListener { _, _, _ ->
            if (binding.pickerHours.value == 0 && binding.pickerMinutes.value == 0 && binding.pickerSeconds.value == 0) {
                binding.pickerSeconds.postDelayed({ 
                    if (binding.pickerHours.value == 0 && binding.pickerMinutes.value == 0 && binding.pickerSeconds.value == 0) {
                         binding.pickerSeconds.value = 1 
                    }
                }, 150) 
            }
            if (viewModel.state.value == TimerState.IDLE) {
                 val hours = binding.pickerHours.value
                 val minutes = binding.pickerMinutes.value
                 val seconds = binding.pickerSeconds.value
                 val durationMillis = TimeUnit.HOURS.toMillis(hours.toLong()) +
                                      TimeUnit.MINUTES.toMillis(minutes.toLong()) +
                                      TimeUnit.SECONDS.toMillis(seconds.toLong())
                 viewModel.setInitialDuration(durationMillis.coerceAtLeast(1000L))
            }
        }

        binding.pickerHours.setOnValueChangedListener(listener)
        binding.pickerMinutes.setOnValueChangedListener(listener)
        binding.pickerSeconds.setOnValueChangedListener(listener)
    }

    private fun setupButtons() {
        binding.startPauseButton.setOnClickListener { startTimer() }
        binding.cancelButton.setOnClickListener { cancelTimer() }
        binding.timerSoundButton.setOnClickListener { showTimerSoundDialog() }
    }

    private fun setupPresetRecyclerView() {
        presetAdapter = TimerPresetAdapter(
             onPresetClick = { preset ->
                 if (viewModel.state.value == TimerState.IDLE) {
                     viewModel.setInitialDuration(preset.durationMillis)
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
                 } catch (e: Exception) {
                     Log.e("TimerFragment", "Navigation failed", e)
                 }
             },
             onPresetLongClick = { preset ->
                 showDeleteConfirmationDialog(preset)
             }
        )
        binding.presetsRecyclerView.adapter = presetAdapter
        binding.presetsRecyclerView.layoutManager = GridLayoutManager(requireContext(), 3)
        presetItemTouchHelper = ItemTouchHelper(SimpleItemTouchHelperCallback(this))
        presetItemTouchHelper?.attachToRecyclerView(binding.presetsRecyclerView)
    }

    private fun observeViewModel() {
        viewModel.formattedTime.observe(viewLifecycleOwner) { formattedTime ->
             binding.timerDigitalDisplay.text = formattedTime
        }
        viewModel.progressPercentage.observe(viewLifecycleOwner) { progress ->
            binding.timerProgressCircle.progress = progress
        }
        viewModel.state.observe(viewLifecycleOwner) { state ->
            updateUI(state ?: TimerState.IDLE)
        }
        viewModel.endTimeMillis.observe(viewLifecycleOwner) { endTimeMillis ->
             if (endTimeMillis != null && viewModel.state.value == TimerState.RUNNING) {
                 val context = requireContext()
                 val is24Hour = DateFormat.is24HourFormat(context)
                 val pattern = if (is24Hour) "HH:mm" else "h:mm a"
                 val sdf = SimpleDateFormat(pattern, Locale.getDefault())
                 val formattedEndTime = sdf.format(Date(endTimeMillis))
                 binding.timerEndTimeText.text = getString(R.string.timer_ends_at, formattedEndTime)
                 binding.timerEndTimeText.isVisible = true
             } else {
                 binding.timerEndTimeText.isVisible = false
                 binding.timerEndTimeText.text = null
             }
        }
        viewModel.presets.observe(viewLifecycleOwner) { presets ->
            presetAdapter.submitList(presets)
        }
         viewModel.timerSoundUri.observe(viewLifecycleOwner) { uriString ->
            val uri = if (uriString.isNullOrEmpty()) null else Uri.parse(uriString)
            updateSoundButtonText(uri)
        }
    }

    private fun updateUI(state: TimerState) {
        val isIdle = state == TimerState.IDLE
        binding.groupTimerSetup.isVisible = isIdle
        binding.groupTimerRunning.isVisible = !isIdle

        binding.presetsRecyclerView.isVisible = isIdle
        binding.timerSoundButton.isVisible = isIdle

        binding.cancelButton.isVisible = !isIdle
        binding.cancelButton.isEnabled = !isIdle

        when (state) {
            TimerState.IDLE -> {
                binding.startPauseButton.text = getString(R.string.start)
                binding.startPauseButton.isEnabled = true
                binding.cancelButton.isVisible = false
                updatePickersFromMillis(1000L)
            }
            TimerState.RUNNING -> {
                binding.startPauseButton.text = getString(R.string.pause)
                binding.startPauseButton.isEnabled = true
            }
            TimerState.PAUSED -> {
                binding.startPauseButton.text = getString(R.string.resume)
                binding.startPauseButton.isEnabled = true
            }
            TimerState.FINISHED -> {
                binding.startPauseButton.text = getString(R.string.reset)
                binding.startPauseButton.isEnabled = true
                binding.cancelButton.isVisible = false
            }
        }
    }

    private fun startTimer() {
        val currentState = viewModel.state.value
        if (currentState == TimerState.IDLE) {
            val hours = binding.pickerHours.value
            val minutes = binding.pickerMinutes.value
            val seconds = binding.pickerSeconds.value
            val durationMillis = TimeUnit.HOURS.toMillis(hours.toLong()) +
                                TimeUnit.MINUTES.toMillis(minutes.toLong()) +
                                TimeUnit.SECONDS.toMillis(seconds.toLong())

            if (durationMillis <= 0) {
                Toast.makeText(context, "Please set a duration > 0", Toast.LENGTH_SHORT).show()
                return
            }
            viewModel.setInitialDuration(durationMillis)
            viewModel.startTimer()
        } else if (currentState == TimerState.PAUSED) {
            viewModel.startTimer()
        } else if (currentState == TimerState.FINISHED) {
            viewModel.resetTimer()
            updatePickersFromMillis(1000L)
        }
    }

    private fun cancelTimer() {
        viewModel.resetTimer()
    }

    private fun showTimerSoundDialog() {
        val context = requireContext()
        val ringtoneManager = RingtoneManager(context)
        ringtoneManager.setType(RingtoneManager.TYPE_ALARM or RingtoneManager.TYPE_NOTIFICATION or RingtoneManager.TYPE_RINGTONE)

        val cursor = try { ringtoneManager.cursor } catch (e: Exception) { /*...*/ return }

        val soundTitles = mutableListOf<String>()
        val soundUris = mutableListOf<Uri>()

        try {
            while (cursor.moveToNext()) {
                val title = cursor.getString(RingtoneManager.TITLE_COLUMN_INDEX) ?: "Unknown Sound"
                val uri = ringtoneManager.getRingtoneUri(cursor.position)
                soundTitles.add(title)
                soundUris.add(uri)
            }
        } catch (e: Exception) { Log.e("TimerFragment", "Error reading cursor", e) } finally { cursor.close() }

        val currentUriString = viewModel.timerSoundUri.value
        val currentUri = if (!currentUriString.isNullOrEmpty()) Uri.parse(currentUriString) else RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        var selectedIndex = soundUris.indexOfFirst { it == currentUri }.takeIf { it >= 0 } ?: 0

        AlertDialog.Builder(context)
            .setTitle("Select Timer Sound")
            .setSingleChoiceItems(soundTitles.toTypedArray(), selectedIndex) { _, which -> selectedIndex = which }
            .setPositiveButton("OK") { _, _ ->
                val selectedUri = if (selectedIndex >= 0 && selectedIndex < soundUris.size) soundUris[selectedIndex] else null
                viewModel.setTimerSound(selectedUri)
                updateSoundButtonText(selectedUri)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun updateTimerDisplay(timer: String) {
        binding.timerDigitalDisplay.text = timer
    }

    private fun updateSoundButtonText(uri: Uri?) {
        val ringtone = if (uri != null) RingtoneManager.getRingtone(context, uri) else null
        val name = ringtone?.getTitle(context) ?: "Default"
        binding.timerSoundButton.text = "Sound: $name"
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

    override fun onItemMove(fromPosition: Int, toPosition: Int) {
        viewModel.movePreset(fromPosition, toPosition)
    }

    override fun onDestroyView() {
        presetItemTouchHelper?.attachToRecyclerView(null)
        presetItemTouchHelper = null
        super.onDestroyView()
        _binding = null
    }
}