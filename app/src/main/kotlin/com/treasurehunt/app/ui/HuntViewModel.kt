package com.treasurehunt.app.ui

import android.app.Application
import android.content.Intent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.treasurehunt.app.data.HuntDatabase
import com.treasurehunt.app.data.HuntImportException
import com.treasurehunt.app.data.HuntListWithLocations
import com.treasurehunt.app.data.JsonImport
import com.treasurehunt.app.data.LocationDraft
import com.treasurehunt.app.data.Repository
import com.treasurehunt.app.hunt.HuntEngine
import com.treasurehunt.app.hunt.HuntService
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class HuntViewModel(app: android.app.Application) : AndroidViewModel(app) {

    private val db = HuntDatabase.get(getApplication())
    private val repo = Repository(db)

    val lists: StateFlow<List<HuntListWithLocations>> = repo.lists
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val engineState: StateFlow<HuntEngine.State> = HuntEngine.state

    fun startHunt(listId: Long) {
        val intent = Intent(getApplication(), HuntService::class.java)
            .putExtra(HuntService.EXTRA_LIST_ID, listId)
        getApplication<Application>().startForegroundService(intent)
        HuntEngine.saveActiveList(listId)
    }

    fun stopHunt() {
        val intent = Intent(getApplication(), HuntService::class.java)
            .setAction(HuntService.ACTION_STOP)
        getApplication<Application>().startForegroundService(intent)
    }

    fun unmute(locationId: Long) {
        viewModelScope.launch {
            HuntEngine.unmuteLocation(locationId)
            repo.setLocationMuted(locationId, false)
        }
    }

    /** Toggles a location's muted state; persisted to the list. */
    fun toggleMuted(locationId: Long, muted: Boolean) {
        viewModelScope.launch {
            if (muted) HuntEngine.muteLocation(locationId) else HuntEngine.unmuteLocation(locationId)
            repo.setLocationMuted(locationId, muted)
        }
    }

    fun setRadius(meters: Int) {
        HuntEngine.saveRadius(meters)
    }

    fun setTracked(locationId: Long?) {
        HuntEngine.setTracked(locationId)
    }

    fun saveList(name: String, drafts: List<LocationDraft>, onSaved: (Long) -> Unit) {
        viewModelScope.launch {
            val id = repo.insert(name.trim(), drafts)
            onSaved(id)
        }
    }

    fun deleteList(list: HuntListWithLocations, onDone: () -> Unit) {
        viewModelScope.launch {
            if (HuntEngine.state.value.listId == list.id) stopHunt()
            repo.delete(list)
            onDone()
        }
    }

    fun loadList(listId: Long, onLoaded: (HuntListWithLocations?) -> Unit) {
        viewModelScope.launch {
            onLoaded(repo.get(listId))
        }
    }

    /** Throws HuntImportException when the file cannot be parsed. */
    fun importJson(text: String, defaultName: String): JsonImport.ParsedList =
        JsonImport.parse(getApplication(), text, defaultName)
}
