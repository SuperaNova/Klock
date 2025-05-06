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
import cit.edu.KlockApp.R
import cit.edu.KlockApp.databinding.FragmentTimerBinding
import android.util.TypedValue
import androidx.annotation.AttrRes
import androidx.annotation.ColorInt
import android.widget.Toast
import androidx.navigation.fragment.findNavController
import java.util.concurrent.TimeUnit
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import androidx.recyclerview.widget.GridLayoutManager
import android.util.Log
import android.text.format.DateFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.recyclerview.widget.ItemTouchHelper
import cit.edu.KlockApp.util.OnItemMoveListener
import cit.edu.KlockApp.util.SimpleItemTouchHelperCallback
import android.content.Intent
import android.media.Ringtone
import android.media.RingtoneManager
import android.net.Uri
import android.app.AlertDialog
import android.os.Handler
import android.os.Looper
import androidx.core.content.ContextCompat
import android.app.NotificationManager
import android.provider.Settings
import android.os.Build

class TimerFragment : Fragment(), OnItemMoveListener {

    private var _binding: FragmentTimerBinding? = null
    private val binding get() = _binding!!

    private val viewModel: TimerViewModel by activityViewModels()
    private lateinit var presetAdapter: TimerPresetAdapter
    private var presetItemTouchHelper: ItemTouchHelper? = null

    // Handler and Runnable for blinking animation
    private val blinkHandler = Handler(Looper.getMainLooper())
    private var blinkRunnable: Runnable? = null
    private var isViewFlashing = false

    private var currentPreviewRingtone: Ringtone? = null // For sound preview

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
            if (state == TimerState.FINISHED) {
                startBlinkingEffect()
            } else {
                stopBlinkingEffect()
            }
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
        viewModel.requestExactAlarmPermissionEvent.observe(viewLifecycleOwner) { event ->
            event.getContentIfNotHandled()?.let { // Only proceed if the event has not been handled
                showExactAlarmPermissionDialog()
            }
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
            // Cancel the user-visible notification
            val notificationManager = requireContext().getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.cancel(TimerFinishedReceiver.USER_NOTIFICATION_ID)
            Log.d("TimerFragment", "Cancelled user notification (ID: ${TimerFinishedReceiver.USER_NOTIFICATION_ID}) on reset.")

            // Stop the sound service
            val stopSoundServiceIntent = Intent(requireContext(), TimerSoundService::class.java).apply {
                action = TimerSoundService.ACTION_STOP_SOUND
            }
            requireContext().startService(stopSoundServiceIntent)
            Log.d("TimerFragment", "Sent stop intent to TimerSoundService on reset.")

            viewModel.resetTimer() // Reset the viewmodel state
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

        val cursor = try {
            ringtoneManager.cursor
        } catch (e: Exception) {
            Log.e("TimerFragment", "Failed to get ringtone cursor", e)
            Toast.makeText(context, "Could not load sounds.", Toast.LENGTH_SHORT).show()
            return
        }

        val soundTitles = mutableListOf<String>()
        val soundUris = mutableListOf<Uri>()

        try {
            if (cursor.moveToFirst()) {
                do {
                    val title = cursor.getString(RingtoneManager.TITLE_COLUMN_INDEX) ?: "Unknown Sound"
                    val uri = ringtoneManager.getRingtoneUri(cursor.position)
                    soundTitles.add(title)
                    soundUris.add(uri)
                } while (cursor.moveToNext())
            }
        } catch (e: Exception) {
            Log.e("TimerFragment", "Error reading ringtone cursor", e)
        } finally {
            cursor.close()
        }

        if (soundTitles.isEmpty()) {
            Toast.makeText(context, "No sounds found.", Toast.LENGTH_SHORT).show()
            return
        }

        val currentUriString = viewModel.timerSoundUri.value
        val currentUri = if (!currentUriString.isNullOrEmpty()) Uri.parse(currentUriString) else RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        var selectedIndex = soundUris.indexOfFirst { it == currentUri }.takeIf { it >= 0 } ?: 0

        val dialog = AlertDialog.Builder(context)
            .setTitle("Select Timer Sound")
            .setSingleChoiceItems(soundTitles.toTypedArray(), selectedIndex) { dialogInterface, which ->
                selectedIndex = which
                // Stop any previously playing preview
                currentPreviewRingtone?.stop()
                currentPreviewRingtone = null

                // Play the newly selected sound for preview
                if (selectedIndex >= 0 && selectedIndex < soundUris.size) {
                    try {
                        currentPreviewRingtone = RingtoneManager.getRingtone(context, soundUris[selectedIndex])
                        currentPreviewRingtone?.play()
                    } catch (e: Exception) {
                        Log.e("TimerFragment", "Error playing preview sound", e)
                        Toast.makeText(context, "Could not play sound preview.", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .setPositiveButton("OK") { _, _ ->
                val selectedUri = if (selectedIndex >= 0 && selectedIndex < soundUris.size) soundUris[selectedIndex] else null
                Log.d("TimerFragment", "OK clicked. Selected URI for ViewModel: ${selectedUri?.toString()}")
                viewModel.setTimerSound(selectedUri)
                updateSoundButtonText(selectedUri) // Update button text immediately
                // No need to stop preview here, setOnDismissListener will handle it
            }
            .setNegativeButton("Cancel", null) // No need to stop preview here, setOnDismissListener
            .setOnDismissListener { // This is crucial to stop sound when dialog closes for any reason
                currentPreviewRingtone?.stop()
                currentPreviewRingtone = null
            }
            .create() // Create the dialog first to be able to call .show()
        
        dialog.show() // Show the dialog
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

    private fun startBlinkingEffect() {
        if (isViewFlashing || !isAdded || _binding == null) return
        isViewFlashing = true
        Log.d("TimerFragment", "Starting blinking effect.")

        val flashColor = ContextCompat.getColor(requireContext(), R.color.timer_finished_flash_red)
        // Resolve surfaceColor once at the start of blinking, assuming theme doesn't change mid-blink
        val surfaceColor = getThemeColor(requireContext(), com.google.android.material.R.attr.colorSurface)
        var isFlashedToRed = false // Tracks if current state is red or surfaceColor

        blinkRunnable = object : Runnable {
            override fun run() {
                if (!isAdded || _binding == null) { // Fragment detached or binding lost
                    stopBlinkingEffect() // Attempt to clean up
                    return
                }
                if (viewModel.state.value == TimerState.FINISHED) {
                    binding.root.setBackgroundColor(if (isFlashedToRed) surfaceColor else flashColor)
                    isFlashedToRed = !isFlashedToRed
                    blinkHandler.postDelayed(this, 500) // Blink interval 500ms
                } else {
                    // ViewModel state is no longer FINISHED, ensure restoration and stop
                    if (isAdded && _binding != null) {
                        binding.root.setBackgroundColor(surfaceColor) // Restore to surface color
                    }
                    stopBlinkingEffect() // Call the main stop logic which also clears runnable
                }
            }
        }
        // Initial flash to red, then start repeating
        if (viewModel.state.value == TimerState.FINISHED) { // Double check state before initial flash
             binding.root.setBackgroundColor(flashColor)
             isFlashedToRed = true
             blinkHandler.postDelayed(blinkRunnable!!, 500)
        } else {
            isViewFlashing = false // Should not have started if not FINISHED
        }
    }

    private fun stopBlinkingEffect() {
        val wasFlashing = isViewFlashing // Store current flashing state
        isViewFlashing = false // Set to false immediately to prevent re-entry or race conditions

        blinkRunnable?.let { blinkHandler.removeCallbacks(it) }
        blinkRunnable = null

        if (wasFlashing && isAdded && _binding != null) {
            // Only restore if it was actually flashing and view is valid
            val surfaceColor = getThemeColor(requireContext(), com.google.android.material.R.attr.colorSurface)
            binding.root.setBackgroundColor(surfaceColor)
            Log.d("TimerFragment", "Blinking stopped. Restored background to theme surface color.")
        } else {
            // Log.d("TimerFragment", "StopBlinkingEffect called but was not flashing or view invalid.")
        }
    }

    @ColorInt
    private fun getThemeColor(context: Context, @AttrRes colorAttr: Int): Int {
        val typedValue = TypedValue()
        return if (context.theme.resolveAttribute(colorAttr, typedValue, true)) {
            typedValue.data
        } else {
            Log.w("TimerFragment", "Failed to resolve theme attribute: $colorAttr. Falling back.")
            // Fallback to a default color if theme resolution fails (e.g., transparent or a hardcoded sensible default)
            ContextCompat.getColor(context, android.R.color.transparent) // Example fallback
        }
    }

    private fun showExactAlarmPermissionDialog() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            MaterialAlertDialogBuilder(requireContext())
                .setTitle("Permission Required for Timers")
                .setMessage("To ensure timers work reliably and notify you at the exact time, please grant the \"Alarms & reminders\" permission.")
                .setPositiveButton("Go to Settings") { _, _ ->
                    try {
                        startActivity(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                            // Optionally, add data Uri for specific app if needed, but usually not required for this action
                            // data = Uri.parse("package:${requireContext().packageName}")
                        })
                    } catch (e: Exception) {
                        Log.e("TimerFragment", "Could not open exact alarm settings", e)
                        Toast.makeText(requireContext(), "Could not open settings. Please grant manually.", Toast.LENGTH_LONG).show()
                    }
                }
                .setNegativeButton(R.string.cancel, null)
                .show()
        } else {
            // On older versions, this permission isn't user-configurable this way, or wasn't needed.
            // ViewModel should ideally not trigger this event on pre-S devices if canScheduleExactAlarms isn't relevant.
            Log.d("TimerFragment", "Exact alarm permission dialog not shown on pre-Android S device.")
        }
    }

    override fun onDestroyView() {
        currentPreviewRingtone?.stop() // Stop preview if fragment view is destroyed
        currentPreviewRingtone = null
        stopBlinkingEffect()
        presetItemTouchHelper?.attachToRecyclerView(null)
        presetItemTouchHelper = null
        super.onDestroyView()
        _binding = null
    }
}