package cit.edu.KlockApp.ui.main.timer

import android.os.CountDownTimer
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

enum class TimerState {
    IDLE, RUNNING, PAUSED, FINISHED
}

class TimerViewModel : ViewModel() {

    private val _state = MutableLiveData(TimerState.IDLE)
    val state: LiveData<TimerState> = _state

    private val _remainingTimeMillis = MutableLiveData(0L)
    val remainingTimeMillis: LiveData<Long> = _remainingTimeMillis

    private val _formattedTime = MutableLiveData("00:00:00")
    val formattedTime: LiveData<String> = _formattedTime

    private val _progressPercentage = MutableLiveData(0)
    val progressPercentage: LiveData<Int> = _progressPercentage
    
    private val _endTimeFormatted = MutableLiveData<String?>(null)
    val endTimeFormatted: LiveData<String?> = _endTimeFormatted

    private var countDownTimer: CountDownTimer? = null
    private var initialDurationMillis: Long = 0L
    private var pauseTimeMillis: Long = 0L // Stores remaining time when paused

    fun setTimer(hours: Int, minutes: Int, seconds: Int) {
        initialDurationMillis = TimeUnit.HOURS.toMillis(hours.toLong()) +
                TimeUnit.MINUTES.toMillis(minutes.toLong()) +
                TimeUnit.SECONDS.toMillis(seconds.toLong())
        
        if (_state.value == TimerState.IDLE) {
            _remainingTimeMillis.value = initialDurationMillis
            _formattedTime.value = formatMillisToHMS(initialDurationMillis)
            _progressPercentage.value = 100 // Start at full
        }
    }

    fun startTimer() {
        if (_state.value != TimerState.IDLE || initialDurationMillis <= 0) return

        _state.value = TimerState.RUNNING
        _endTimeFormatted.value = calculateEndTime(initialDurationMillis)
        
        countDownTimer = object : CountDownTimer(initialDurationMillis, 50) { // Tick every 50ms for smoother progress
            override fun onTick(millisUntilFinished: Long) {
                _remainingTimeMillis.value = millisUntilFinished
                _formattedTime.value = formatMillisToHMS(millisUntilFinished)
                _progressPercentage.value = ((millisUntilFinished * 100) / initialDurationMillis).toInt()
            }

            override fun onFinish() {
                _remainingTimeMillis.value = 0L
                _formattedTime.value = "00:00:00"
                _progressPercentage.value = 0
                _state.value = TimerState.FINISHED
                // Optionally trigger a sound/vibration event here
                // Reset to IDLE after a short delay or explicitly
                resetTimerInternal()
            }
        }.start()
    }

    fun pauseTimer() {
        if (_state.value != TimerState.RUNNING) return
        countDownTimer?.cancel()
        pauseTimeMillis = _remainingTimeMillis.value ?: 0L // Save remaining time
        _state.value = TimerState.PAUSED
    }

    fun resumeTimer() {
        if (_state.value != TimerState.PAUSED || pauseTimeMillis <= 0) return

        _state.value = TimerState.RUNNING
        _endTimeFormatted.value = calculateEndTime(pauseTimeMillis)

        // Create a new timer starting from the paused time
        countDownTimer = object : CountDownTimer(pauseTimeMillis, 50) {
             override fun onTick(millisUntilFinished: Long) {
                _remainingTimeMillis.value = millisUntilFinished
                _formattedTime.value = formatMillisToHMS(millisUntilFinished)
                // Calculate progress based on initial duration
                _progressPercentage.value = ((millisUntilFinished * 100) / initialDurationMillis).toInt()
            }

            override fun onFinish() {
                 _remainingTimeMillis.value = 0L
                 _formattedTime.value = "00:00:00"
                 _progressPercentage.value = 0
                 _state.value = TimerState.FINISHED
                 resetTimerInternal()
            }
        }.start()
    }

    fun cancelTimer() {
        resetTimerInternal()
    }
    
    private fun resetTimerInternal() {
        countDownTimer?.cancel()
        countDownTimer = null
        initialDurationMillis = 0L
        pauseTimeMillis = 0L
        _remainingTimeMillis.value = 0L
        _formattedTime.value = "00:00:00"
        _progressPercentage.value = 0
        _endTimeFormatted.value = null
        _state.value = TimerState.IDLE
    }

    private fun formatMillisToHMS(millis: Long): String {
        val hours = TimeUnit.MILLISECONDS.toHours(millis)
        val minutes = TimeUnit.MILLISECONDS.toMinutes(millis) % 60
        val seconds = TimeUnit.MILLISECONDS.toSeconds(millis) % 60
        // Add 1 second if rounding up milliseconds, handle carefully near zero
        val roundedSeconds = if (millis % 1000 >= 500 && seconds < 59) seconds + 1 else seconds
        val finalSeconds = if (millis < 500 && hours == 0L && minutes == 0L) 0 else roundedSeconds 

        return String.format("%02d:%02d:%02d", hours, minutes, finalSeconds)
    }
    
    private fun calculateEndTime(durationMillis: Long): String {
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.MILLISECOND, durationMillis.toInt())
        val sdf = SimpleDateFormat("h:mm a", Locale.getDefault())
        return "Ends at ${sdf.format(calendar.time)}"
    }

    override fun onCleared() {
        super.onCleared()
        countDownTimer?.cancel()
    }
}