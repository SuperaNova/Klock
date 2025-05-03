package cit.edu.KlockApp.ui.main.worldClock

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import org.json.JSONArray

class WorldClockViewModel(application: Application) : AndroidViewModel(application) {
    private val prefs = application.getSharedPreferences("world_clocks", Context.MODE_PRIVATE)
    private val _worldClocks = MutableLiveData<List<WorldClockItem>>()
    val worldClocks: LiveData<List<WorldClockItem>> = _worldClocks

    // LiveData for edit mode state
    private val _isEditMode = MutableLiveData(false)
    val isEditMode: LiveData<Boolean> = _isEditMode

    init {
        loadSavedClocks()
    }

    private fun loadSavedClocks() {
        val jsonString = prefs.getString("saved_clocks_ordered", null)
        val clockList = mutableListOf<WorldClockItem>()
        if (jsonString != null) {
            try {
                val jsonArray = JSONArray(jsonString)
                for (i in 0 until jsonArray.length()) {
                    clockList.add(WorldClockItem(jsonArray.getString(i)))
                }
            } catch (e: Exception) {
                Log.e("WorldClockViewModel", "Error parsing saved clocks", e)
                prefs.edit().remove("saved_clocks_ordered").apply()
                prefs.edit().remove("saved_clocks").apply()
            }
        }
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
        val removed = currentList.removeAll { it.timeZoneId == timeZoneId }
        if (removed) {
            _worldClocks.value = currentList
            saveClocks(currentList)
        }
    }

    fun moveClock(fromPosition: Int, toPosition: Int) {
        val currentList = _worldClocks.value?.toMutableList() ?: return
        if (fromPosition < 0 || fromPosition >= currentList.size || toPosition < 0 || toPosition >= currentList.size) return

        val item = currentList.removeAt(fromPosition)
        currentList.add(toPosition, item)
        _worldClocks.value = currentList
        saveClocks(currentList)
    }

    // Function to toggle edit mode
    fun toggleEditMode() {
        _isEditMode.value = !(_isEditMode.value ?: false)
    }

    // Function to exit edit mode explicitly (e.g., on fragment destroy)
    fun exitEditMode() {
        _isEditMode.value = false
    }

    private fun saveClocks(clocks: List<WorldClockItem>) {
        val jsonArray = JSONArray()
        clocks.forEach { jsonArray.put(it.timeZoneId) }
        prefs.edit().putString("saved_clocks_ordered", jsonArray.toString()).apply()
        prefs.edit().remove("saved_clocks").apply()
    }
}


