package cit.edu.KlockApp.ui.main.stopwatch

import android.os.SystemClock
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

// Define the possible states
enum class StopwatchState {
    IDLE, RUNNING, PAUSED
}

class StopwatchViewModel : ViewModel() {

    // LiveData for raw elapsed time
    private val _elapsedTimeMillis = MutableLiveData(0L)
    val elapsedTimeMillis: LiveData<Long> = _elapsedTimeMillis

    // LiveData for formatted time string
    private val _formattedTime = MutableLiveData("00:00.00") // Format with hundredths
    val formattedTime: LiveData<String> = _formattedTime

    // LiveData for the current state
    private val _state = MutableLiveData(StopwatchState.IDLE)
    val state: LiveData<StopwatchState> = _state

    // LiveData for lap times
    private val _laps = MutableLiveData<List<LapData>>(emptyList())
    val laps: LiveData<List<LapData>> = _laps

    private var timerJob: Job? = null
    private var startTime: Long = 0L // Time when the timer was last started/resumed
    private var accumulatedTime: Long = 0L // Time accumulated before the last pause
    private var lastLapTime: Long = 0L // Total elapsed time when the last lap was recorded

    // --- Timer Control Logic --- 

    private fun startTimer() {
        if (timerJob?.isActive == true) return // Already running

        startTime = SystemClock.elapsedRealtime()
        _state.value = StopwatchState.RUNNING

        timerJob = viewModelScope.launch {
            while (isActive) { // Use isActive from coroutine scope
                val currentTime = SystemClock.elapsedRealtime()
                val elapsed = accumulatedTime + (currentTime - startTime)
                _elapsedTimeMillis.postValue(elapsed)
                _formattedTime.postValue(formatTime(elapsed))
                delay(10) // Update frequently
            }
        }
    }

    fun start() {
        if (_state.value == StopwatchState.IDLE) {
            // Reset everything before starting
            accumulatedTime = 0L
            lastLapTime = 0L
            _laps.value = emptyList()
            startTimer()
        } else if (_state.value == StopwatchState.PAUSED) {
            // Resume from pause
            resume()
        }
    }

    fun pause() {
        if (_state.value == StopwatchState.RUNNING) {
            timerJob?.cancel()
            // Calculate time elapsed since last start/resume and add to accumulated
            accumulatedTime += (SystemClock.elapsedRealtime() - startTime)
            _state.value = StopwatchState.PAUSED
        }
    }

    private fun resume() {
        if (_state.value == StopwatchState.PAUSED) {
            // Start the timer again; it will use the latest accumulatedTime
            startTimer()
        }
    }

    fun lap() {
        if (_state.value == StopwatchState.RUNNING) {
            val currentTimeMillis = _elapsedTimeMillis.value ?: 0L
            // Lap duration is the difference between current total time and last lap's total time
            val currentLapDuration = currentTimeMillis - lastLapTime
            val lapNumber = (_laps.value?.size ?: 0) + 1

            // Use original field names if LapData was different
            val newLap = LapData(lapNumber, currentLapDuration, currentTimeMillis)
            val currentLaps = _laps.value.orEmpty().toMutableList()
            currentLaps.add(0, newLap) // Add new lap to the top of the list
            _laps.value = currentLaps

            // Update the time at which this lap was recorded
            lastLapTime = currentTimeMillis
            // No best/worst calculation needed
        }
    }

    fun reset() {
        timerJob?.cancel()
        _state.value = StopwatchState.IDLE
        accumulatedTime = 0L
        lastLapTime = 0L
        startTime = 0L
        _elapsedTimeMillis.value = 0L
        _formattedTime.value = formatTime(0L) // Reset formatted time too
        _laps.value = emptyList()
        // No best/worst reset needed
    }

    // --- Formatting --- 
    private fun formatTime(millis: Long): String {
        val minutes = TimeUnit.MILLISECONDS.toMinutes(millis)
        val seconds = TimeUnit.MILLISECONDS.toSeconds(millis) % 60
        // Show hundredths as per original formatting
        val hundredths = (TimeUnit.MILLISECONDS.toMillis(millis) % 1000) / 10
        return String.format("%02d:%02d.%02d", minutes, seconds, hundredths)
    }

    // --- Cleanup --- 
    override fun onCleared() {
        super.onCleared()
        timerJob?.cancel()
    }
}