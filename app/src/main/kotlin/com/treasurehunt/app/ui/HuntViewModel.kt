package com.treasurehunt.app.ui

import android.content.Intent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.treasurehunt.app.data.HuntDatabase
import com.treasurehunt.app.data.HuntImportException
import com.treasurehunt.app.data.HuntListEntity
import com.treasurehunt.app.data.JsonImport
import com.treasurehunt.app.data.LocationDraft
import com.treasurehunt.app.data.Repository
import com.treasurehunt.app.hunt.HuntEngine
import com.treasurehunt.app.hunt.HuntService
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class HuntViewModel(application: android.app.Application) : androidx.lifecycle.ViewModel() {

    private val repo = Repository(HuntDatabase.get(application).dao())

    val lists: StateFlow<List<HuntListEntity>> = repo.lists
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val engineState: StateFlow<HuntEngine.State> = HuntEngine.state

    fun startHunt(listId: Long) {
        val intent = Intent(application, HuntService::class.java)
            .putExtra(HuntService.EXTRA_LIST_ID, listId)
        application.startForegroundService(intent)
        HuntEngine.saveActiveList(listId)
    }

    fun stopHunt() {
        val intent = Intent(application, HuntService::class.java)
            .setAction(HuntService.ACTION_STOP)
        application.startForegroundService(intent)
    }

    fun muteNearest() {
        viewModelScope.launch {
            val id = HuntEngine.muteNearestLocation()
            if (id != null) repo.setLocationMuted(id, true)
        }
    }

    fun unmute(locationId: Long) {
        viewModelScope.launch {
            HuntEngine.unmuteLocation(locationId)
            repo.setLocationMuted(locationId, false)
        }
    }

    fun setRadius(meters: Int) {
        HuntEngine.saveRadius(meters)
    }

    fun saveList(name: String, drafts: List<LocationDraft>, onSaved: (Long) -> Unit) {
        viewModelScope.launch {
            val id = repo.insert(name.trim(), drafts)
            onSaved(id)
        }
    }

    fun deleteList(list: HuntListEntity, onDone: () -> Unit) {
        viewModelScope.launch {
            if (HuntEngine.state.value.listId == list.id) stopHunt()
            repo.delete(list)
            onDone()
        }
    }

    fun loadList(listId: Long, onLoaded: (HuntListEntity?) -> Unit) {
        viewModelScope.launch {
            onLoaded(repo.get(listId))
        }
    }

    /** Throws HuntImportException when the file cannot be parsed. */
    fun importJson(text: String, defaultName: String): JsonImport.ParsedList =
        JsonImport.parse(text, defaultName)
}
