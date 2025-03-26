package cit.edu.KlockApp.ui.main.worldClock

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class WorldClockViewModel : ViewModel() {

    private val _textList = MutableLiveData<MutableList<String>>(mutableListOf("This is world clock Fragment"))
    val textList: MutableLiveData<MutableList<String>> = _textList


    fun addNewWorldClock(newText: String) { // add world clock
        _textList.value = _textList.value?.apply { add(newText) }
    }

    fun removeWorldClockItem(index: Int) {
        _textList.value?.let {
            if (index in it.indices) {
                it.removeAt(index)
                _textList.value = it
            }
        }
    }

    fun clearWorldClockList() {
        _textList.value = mutableListOf()
    }
}


