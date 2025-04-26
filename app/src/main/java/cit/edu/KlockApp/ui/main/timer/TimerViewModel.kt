package cit.edu.KlockApp.ui.main.timer

import android.app.Application
import android.content.Context
import android.os.CountDownTimer
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit
import cit.edu.KlockApp.util.Event
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

// Define TimerPreset data class
data class TimerPreset(
    val id: String = UUID.randomUUID().toString(),
    val emojiIcon: String,
    val durationMillis: Long
)

enum class TimerState {
    IDLE, RUNNING, PAUSED, FINISHED // FINISHED state might be redundant if reset on finish
}

class TimerViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs = application.getSharedPreferences("timer_prefs", Context.MODE_PRIVATE)

    private var initialDurationMillis: Long = 0L
    private var countDownTimer: CountDownTimer? = null
    private var timeElapsedBeforePause: Long = 0L // To accurately calculate remaining time on resume

    private val _state = MutableLiveData(TimerState.IDLE)
    val state: LiveData<TimerState> = _state

    private val _remainingTimeMillis = MutableLiveData<Long>()
    val remainingTimeMillis: LiveData<Long> = _remainingTimeMillis

    // LiveData for formatted time string (e.g., HH:MM:SS or MM:SS)
    private val _formattedTime = MutableLiveData<String>("00:00")
    val formattedTime: LiveData<String> = _formattedTime

    // LiveData for progress (0-100)
    private val _progressPercentage = MutableLiveData<Int>(100)
    val progressPercentage: LiveData<Int> = _progressPercentage

    // LiveData for presets
    private val _presets = MutableLiveData<List<TimerPreset>>(emptyList())
    val presets: LiveData<List<TimerPreset>> = _presets

    // Optional: LiveData for formatted end time
    private val _endTimeFormatted = MutableLiveData<String?>()
    val endTimeFormatted: LiveData<String?> = _endTimeFormatted

    // Optional: LiveData for showing duplicate preset error
    private val _showDuplicatePresetError = MutableLiveData<Event<Unit>>()
    val showDuplicatePresetError: LiveData<Event<Unit>> = _showDuplicatePresetError

    init {
        setInitialDuration(0L) // Start with 0 duration initially
        loadPresets() // Load saved presets
    }

    fun setInitialDuration(durationMillis: Long) {
        if (_state.value == TimerState.IDLE) {
            initialDurationMillis = durationMillis
            _remainingTimeMillis.value = initialDurationMillis
            _formattedTime.value = formatMillisToHMS(initialDurationMillis)
            _progressPercentage.value = 100 // Full progress when idle/reset
            timeElapsedBeforePause = 0L // Reset elapsed time
            _endTimeFormatted.value = null // Clear end time
        }
    }

    fun setTimerFromPreset(preset: TimerPreset) {
        if (_state.value == TimerState.IDLE) {
            setInitialDuration(preset.durationMillis)
        }
    }

    fun startTimer() {
        val currentState = _state.value
        if (currentState == TimerState.IDLE && initialDurationMillis > 0) {
             _state.value = TimerState.RUNNING
             calculateAndSetEndTime(initialDurationMillis)
             startCountdown(initialDurationMillis)
        } else if (currentState == TimerState.PAUSED) {
             _state.value = TimerState.RUNNING
             val resumeDuration = _remainingTimeMillis.value ?: 0L
             if (resumeDuration > 0) {
                 calculateAndSetEndTime(resumeDuration)
                 startCountdown(resumeDuration)
             } else {
                 resetTimer() // Reset if trying to resume at 0
             }
        }
    }

     private fun startCountdown(duration: Long) {
         countDownTimer?.cancel() // Cancel any existing timer
         countDownTimer = object : CountDownTimer(duration, 50) { // Tick frequently for smooth progress
             override fun onTick(millisUntilFinished: Long) {
                 _remainingTimeMillis.value = millisUntilFinished
                 _formattedTime.value = formatMillisToHMS(millisUntilFinished)
                 // Calculate progress based on the initial duration set for *this* run
                 val progress = if (initialDurationMillis > 0) {
                     ((millisUntilFinished * 100) / initialDurationMillis).toInt()
                 } else {
                     0 // Progress is 0 if initial duration was 0
                 }
                 _progressPercentage.value = progress.coerceIn(0, 100)
             }

             override fun onFinish() {
                 _remainingTimeMillis.value = 0L
                 _formattedTime.value = formatMillisToHMS(0L)
                 _progressPercentage.value = 0
                 _state.value = TimerState.FINISHED // Indicate completion
                 _endTimeFormatted.value = null // Clear end time
                 // TODO: Optionally trigger a notification or sound here
             }
         }.start()
     }

     private fun calculateAndSetEndTime(durationMillis: Long) {
         val endTimeMillis = System.currentTimeMillis() + durationMillis
         val sdf = SimpleDateFormat("h:mm a", Locale.getDefault()) // Format like "3:45 PM"
         _endTimeFormatted.value = "Ends at ${sdf.format(Date(endTimeMillis))}"
     }


    fun pauseTimer() {
        if (_state.value == TimerState.RUNNING) {
            countDownTimer?.cancel()
            _state.value = TimerState.PAUSED
             _endTimeFormatted.value = null // Clear end time on pause
            // remainingTimeMillis is already updated by onTick
        }
    }

    fun resetTimer() {
        countDownTimer?.cancel()
        _state.value = TimerState.IDLE
        // Reset to 0, not initial duration, user needs to set a new time or preset
        _remainingTimeMillis.value = 0L
        _formattedTime.value = formatMillisToHMS(0L)
        _progressPercentage.value = 100 // Visually full circle when idle
        initialDurationMillis = 0L // Clear the stored initial duration
        _endTimeFormatted.value = null
    }

    private fun formatMillisToHMS(millis: Long): String {
        val hours = TimeUnit.MILLISECONDS.toHours(millis)
        val minutes = TimeUnit.MILLISECONDS.toMinutes(millis) % 60
        val seconds = TimeUnit.MILLISECONDS.toSeconds(millis) % 60

        return if (hours > 0) {
            String.format("%d:%02d:%02d", hours, minutes, seconds) // No leading zero for hours > 0
        } else {
            String.format("%02d:%02d", minutes, seconds)
        }
    }

    // --- Preset Management ---

    private fun loadPresets() {
        val jsonString = prefs.getString("timer_presets_json", null)
        if (jsonString != null) {
            try {
                val jsonArray = JSONArray(jsonString)
                val loadedPresets = mutableListOf<TimerPreset>()
                for (i in 0 until jsonArray.length()) {
                    val jsonObject = jsonArray.getJSONObject(i)
                    // Ensure backward compatibility or handle cases where "name" might still exist
                    val identifier = if (jsonObject.has("emojiIcon")) {
                        jsonObject.getString("emojiIcon")
                    } else {
                        // Handle potential old format or provide a default/fallback
                        jsonObject.optString("name", "?") // Fallback to "?" if neither exists
                    }
                    if (identifier.isNotBlank()) { // Only add if we have a valid identifier
                        loadedPresets.add(
                            TimerPreset(
                                id = jsonObject.optString("id", UUID.randomUUID().toString()),
                                emojiIcon = identifier, // Use the determined identifier
                                durationMillis = jsonObject.getLong("durationMillis")
                            )
                        )
                    }
                }
                // Sort by emojiIcon string value
                _presets.value = loadedPresets.sortedBy { it.emojiIcon }
            } catch (e: Exception) {
                // Log the error e.g., Log.e("TimerViewModel", "Error loading presets", e)
                _presets.value = emptyList()
                savePresetsInternal(emptyList()) // Clear potentially corrupted data
            }
        } else {
            _presets.value = emptyList() // No presets saved yet
        }
    }

    private fun savePresetsInternal(presetsToSave: List<TimerPreset>) {
        try {
            val jsonArray = JSONArray()
            presetsToSave.forEach { preset ->
                val jsonObject = JSONObject()
                jsonObject.put("id", preset.id)
                jsonObject.put("emojiIcon", preset.emojiIcon) // Save emojiIcon
                jsonObject.put("durationMillis", preset.durationMillis)
                jsonArray.put(jsonObject)
            }
            prefs.edit().putString("timer_presets_json", jsonArray.toString()).apply()
        } catch (e: Exception) {
            // Log error or notify user
        }
    }

    // Function to add a new preset
    fun addPreset(emojiIcon: String, durationMillis: Long) {
        // Basic validation: Ensure emojiIcon is not empty and duration is positive
        if (emojiIcon.isBlank() || durationMillis <= 0) {
            // Handle invalid input (e.g., show error Toast or log)
            // Consider adding more robust emoji validation if needed (e.g., check length or character properties)
            Log.w("TimerViewModel", "Invalid input for addPreset: emojiIcon='$emojiIcon', durationMillis=$durationMillis")
            return
        }

        val currentList = _presets.value.orEmpty()
        // Check for duplicates based on emojiIcon (case-insensitive for simplicity, adjust if needed)
        if (currentList.any { it.emojiIcon.equals(emojiIcon, ignoreCase = true) }) {
            // Handle duplicate emoji icon (e.g., show error Toast)
            Log.w("TimerViewModel", "Duplicate preset emoji attempted: $emojiIcon")
            _showDuplicatePresetError.value = Event(Unit) // Trigger event for UI
            return
        }

        val newPreset = TimerPreset(emojiIcon = emojiIcon, durationMillis = durationMillis)
        // Sort by emojiIcon string value
        val updatedList = (currentList + newPreset).sortedBy { it.emojiIcon }
        _presets.value = updatedList
        savePresetsInternal(updatedList)
    }

    // Function to delete a preset by its ID
    fun deletePreset(presetId: String) {
        val currentList = _presets.value.orEmpty()
        val updatedList = currentList.filterNot { it.id == presetId }
        if (currentList.size != updatedList.size) {
            _presets.value = updatedList
            savePresetsInternal(updatedList)
        }
    }

    override fun onCleared() {
        super.onCleared()
        countDownTimer?.cancel() // Clean up the timer
    }
}