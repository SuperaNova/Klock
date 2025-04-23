package cit.edu.KlockApp.ui.main.stopwatch

import android.os.SystemClock
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

enum class StopwatchState {
    IDLE, RUNNING, PAUSED
}

class StopwatchViewModel : ViewModel() {

    private val _elapsedTimeMillis = MutableLiveData(0L)
    val elapsedTimeMillis: LiveData<Long> = _elapsedTimeMillis

    private val _formattedTime = MutableLiveData("00:00.000")
    val formattedTime: LiveData<String> = _formattedTime

    private val _state = MutableLiveData(StopwatchState.IDLE)
    val state: LiveData<StopwatchState> = _state

    private val _laps = MutableLiveData<List<LapData>>(emptyList())
    val laps: LiveData<List<LapData>> = _laps

    private var timerJob: Job? = null
    private var startTime: Long = 0L
    private var accumulatedTime: Long = 0L
    private var lastLapTime: Long = 0L

    private fun startTimer() {
        if (timerJob?.isActive == true) return // Already running

        startTime = SystemClock.elapsedRealtime()
        _state.value = StopwatchState.RUNNING

        timerJob = viewModelScope.launch {
            while (_state.value == StopwatchState.RUNNING) {
                val currentTime = SystemClock.elapsedRealtime()
                val elapsed = accumulatedTime + (currentTime - startTime)
                _elapsedTimeMillis.postValue(elapsed)
                _formattedTime.postValue(formatMillis(elapsed))
                delay(10) // Update roughly every 10ms for smooth display
            }
        }
    }

    fun start() {
        if (_state.value == StopwatchState.IDLE) {
            accumulatedTime = 0L
            lastLapTime = 0L
            _laps.value = emptyList()
            startTimer()
        } else if (_state.value == StopwatchState.PAUSED) {
            resume()
        }
    }

    fun pause() {
        if (_state.value == StopwatchState.RUNNING) {
            timerJob?.cancel()
            accumulatedTime += (SystemClock.elapsedRealtime() - startTime)
            _state.value = StopwatchState.PAUSED
        }
    }

    private fun resume() {
        if (_state.value == StopwatchState.PAUSED) {
            startTimer() // Reuses accumulatedTime
        }
    }

    fun lap() {
        if (_state.value == StopwatchState.RUNNING) {
            val currentTimeMillis = _elapsedTimeMillis.value ?: 0L
            val currentLapTime = currentTimeMillis - lastLapTime
            val lapNumber = (_laps.value?.size ?: 0) + 1
            
            val newLap = LapData(lapNumber, currentLapTime, currentTimeMillis)
            val currentLaps = _laps.value.orEmpty().toMutableList()
            currentLaps.add(0, newLap) // Add new lap to the top
            _laps.value = currentLaps
            
            lastLapTime = currentTimeMillis
        }
    }

    fun reset() {
        timerJob?.cancel()
        _state.value = StopwatchState.IDLE
        accumulatedTime = 0L
        lastLapTime = 0L
        startTime = 0L
        _elapsedTimeMillis.value = 0L
        _formattedTime.value = "00:00.000"
        _laps.value = emptyList()
    }

    private fun formatMillis(millis: Long): String {
        val minutes = TimeUnit.MILLISECONDS.toMinutes(millis)
        val seconds = TimeUnit.MILLISECONDS.toSeconds(millis) % 60
        val milliseconds = millis % 1000
        return String.format("%02d:%02d.%03d", minutes, seconds, milliseconds)
    }

    override fun onCleared() {
        super.onCleared()
        timerJob?.cancel()
    }
}