package cit.edu.KlockApp.ui.main.worldClock

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class WorldClockViewModel : ViewModel() {

    private val _text = MutableLiveData<String>().apply {
        value = "This is world clock Fragment"
    }
    val text: LiveData<String> = _text
}