package cit.edu.KlockApp.ui.main.worldClock

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData

class WorldClockViewModel(application: Application) : AndroidViewModel(application) {
    private val prefs = application.getSharedPreferences("world_clocks", Context.MODE_PRIVATE)
    private val _worldClocks = MutableLiveData<List<WorldClockItem>>()
    val worldClocks: LiveData<List<WorldClockItem>> = _worldClocks

    init {
        loadSavedClocks()
    }

    private fun loadSavedClocks() {
        val savedClocks = prefs.getStringSet("saved_clocks", emptySet()) ?: emptySet()
        val clockList = savedClocks.map { WorldClockItem(it) }.toList()
        _worldClocks.postValue(clockList)
    }

    fun addWorldClock(timeZoneId: String) {
        val currentList = _worldClocks.value?.toMutableList() ?: mutableListOf()
        if (!currentList.any { it.timeZoneId == timeZoneId }) {
            currentList.add(WorldClockItem(timeZoneId))
            _worldClocks.value = currentList
            saveClocks(currentList)
        }
    }

    fun removeWorldClock(timeZoneId: String) {
        val currentList = _worldClocks.value?.toMutableList() ?: mutableListOf()
        currentList.removeAll { it.timeZoneId == timeZoneId }
        _worldClocks.value = currentList
        saveClocks(currentList)
    }

    private fun saveClocks(clocks: List<WorldClockItem>) {
        val clockIds = clocks.map { it.timeZoneId }.toSet()
        prefs.edit().putStringSet("saved_clocks", clockIds).apply()
    }
}


