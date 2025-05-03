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

    // New LiveData for the end time timestamp (Long)
    private val _endTimeMillis = MutableLiveData<Long?>()
    val endTimeMillis: LiveData<Long?> = _endTimeMillis

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
            _endTimeMillis.value = null // Clear end time timestamp
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
             _endTimeMillis.value = System.currentTimeMillis() + initialDurationMillis // Set timestamp
             startCountdown(initialDurationMillis)
        } else if (currentState == TimerState.PAUSED) {
             _state.value = TimerState.RUNNING
             val resumeDuration = _remainingTimeMillis.value ?: 0L
             if (resumeDuration > 0) {
                 _endTimeMillis.value = System.currentTimeMillis() + resumeDuration // Set timestamp
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
                 _endTimeMillis.value = null // Clear end time timestamp
                 // TODO: Optionally trigger a notification or sound here
             }
         }.start()
     }

    fun pauseTimer() {
        if (_state.value == TimerState.RUNNING) {
            countDownTimer?.cancel()
            _state.value = TimerState.PAUSED
            _endTimeMillis.value = null // Clear end time timestamp on pause
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
        _endTimeMillis.value = null // Clear end time timestamp
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
                    val emoji = jsonObject.optString("emojiIcon", "")
                    val id = jsonObject.optString("id", UUID.randomUUID().toString())
                    val duration = jsonObject.getLong("durationMillis")
                    loadedPresets.add(TimerPreset(id = id, emojiIcon = emoji, durationMillis = duration))
                }
                _presets.value = loadedPresets
                Log.d("TimerViewModel", "Loaded presets count: ${loadedPresets.size}")
            } catch (e: Exception) {
                Log.e("TimerViewModel", "Error loading presets", e)
                _presets.value = emptyList()
                savePresetsInternal(emptyList())
            }
        } else {
            _presets.value = emptyList()
        }
    }

    private fun savePresetsInternal(presetsToSave: List<TimerPreset>) {
        try {
            val jsonArray = JSONArray()
            presetsToSave.forEach { preset ->
                val jsonObject = JSONObject()
                jsonObject.put("id", preset.id)
                jsonObject.put("emojiIcon", preset.emojiIcon)
                jsonObject.put("durationMillis", preset.durationMillis)
                jsonArray.put(jsonObject)
            }
            prefs.edit().putString("timer_presets_json", jsonArray.toString()).apply()
        } catch (e: Exception) {
            Log.e("TimerViewModel", "Error saving presets", e)
        }
    }

    fun addPreset(emojiIcon: String, durationMillis: Long) {
        if (durationMillis <= 0) {
            Log.w("TimerViewModel", "Invalid input for addPreset: Duration <= 0")
            return
        }
        val currentList = _presets.value.orEmpty()
        val newPreset = TimerPreset(emojiIcon = emojiIcon, durationMillis = durationMillis)
        val updatedList = currentList + newPreset
        _presets.value = updatedList
        savePresetsInternal(updatedList)
    }

    // Function to delete a preset - order maintained by filtering
    fun deletePreset(id: String) {
        val currentList = _presets.value.orEmpty()
        val updatedList = currentList.filterNot { it.id == id }
        _presets.value = updatedList
        savePresetsInternal(updatedList)
    }

    // Function to get a preset by ID (needed for editing)
    fun getPresetById(id: String): TimerPreset? {
        return _presets.value?.firstOrNull { it.id == id }
    }

    // Function to update an existing preset
    fun updatePreset(id: String, newEmojiIcon: String, newDurationMillis: Long) {
         if (newDurationMillis <= 0) {
            Log.w("TimerViewModel", "Invalid input for updatePreset: Duration <= 0")
            return
        }
        val currentList = _presets.value.orEmpty().toMutableList()
        val index = currentList.indexOfFirst { it.id == id }
        if (index != -1) {
            val updatedPreset = TimerPreset(id = id, emojiIcon = newEmojiIcon, durationMillis = newDurationMillis)
            currentList[index] = updatedPreset
            _presets.value = currentList
            savePresetsInternal(currentList)
        } else {
            Log.w("TimerViewModel", "Preset with ID $id not found for update.")
        }
    }

    // New method for reordering (will be used by ItemTouchHelper later)
    fun movePreset(fromPosition: Int, toPosition: Int) {
        val currentList = _presets.value?.toMutableList() ?: return
        if (fromPosition < 0 || fromPosition >= currentList.size || toPosition < 0 || toPosition >= currentList.size) return
        
        val item = currentList.removeAt(fromPosition)
        currentList.add(toPosition, item)
        _presets.value = currentList
        savePresetsInternal(currentList)
    }

    override fun onCleared() {
        super.onCleared()
        countDownTimer?.cancel() // Clean up timer
    }
}