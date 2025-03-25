package cit.edu.KlockApp.ui.main.timer

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class timerViewModel : ViewModel() {

    private val _text = MutableLiveData<String>().apply {
        value = "This is timer Fragment"
    }
    val text: LiveData<String> = _text
}