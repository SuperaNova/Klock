package cit.edu.KlockApp.ui.main.alarm

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import java.sql.Date
import java.sql.Time

class AlarmViewModel : ViewModel() {

    private val _alarms = MutableLiveData<List<Alarm>>().apply {
        value = listOf(
            Alarm(Time.valueOf("08:00:00"), true, Date.valueOf("2025-03-26")),
            Alarm(Time.valueOf("12:00:00"), false, Date.valueOf("2025-03-27")),
            Alarm(Time.valueOf("18:00:00"), true, Date.valueOf("2025-03-28"))
        )
    }

    val alarms: LiveData<List<Alarm>> = _alarms
    }
