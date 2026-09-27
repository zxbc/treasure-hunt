package com.treasurehunt.app.data

import androidx.room.withTransaction
import kotlinx.coroutines.flow.Flow

class Repository(private val db: HuntDatabase) {

    private val dao: HuntDao = db.dao()

    val lists: Flow<List<HuntListWithLocations>> = dao.observeLists()

    suspend fun get(id: Long): HuntListWithLocations? = dao.listById(id)

    suspend fun insert(name: String, locations: List<LocationDraft>): Long {
        return db.withTransaction {
            val listId = dao.insertList(HuntListEntity(id = 0L, name = name))
            dao.insertLocations(
                locations.map {
                    LocationEntity(
                        id = 0L,
                        listId = listId,
                        name = it.name,
                        description = it.description,
                        lat = it.lat,
                        lon = it.lon,
                    )
                },
            )
            listId
        }
    }

    suspend fun delete(list: HuntListWithLocations): Unit {
        db.withTransaction {
            dao.deleteLocationsOf(list.id)
            dao.deleteListById(list.id)
        }
    }

    suspend fun setLocationMuted(id: Long, muted: Boolean) = dao.setLocationMuted(id, muted)

    suspend fun unmuteAll(listId: Long) = dao.unmuteAll(listId)
}
