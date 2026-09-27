package com.treasurehunt.app.data

import kotlinx.coroutines.flow.Flow

class Repository(private val dao: HuntDao) {

    val lists: Flow<List<HuntListEntity>> = dao.observeLists()

    suspend fun get(id: Long): HuntListEntity? = dao.listById(id)

    suspend fun insert(name: String, locations: List<LocationDraft>): Long {
        val entity = HuntListEntity(
            id = 0L,
            name = name,
            locations = locations.map {
                LocationEntity(
                    id = 0L,
                    listId = 0L,
                    name = it.name,
                    description = it.description,
                    lat = it.lat,
                    lon = it.lon,
                )
            },
        )
        return dao.insertList(entity)
    }

    suspend fun delete(list: HuntListEntity) = dao.deleteList(list)

    suspend fun setLocationMuted(id: Long, muted: Boolean) = dao.setLocationMuted(id, muted)

    suspend fun unmuteAll(listId: Long) = dao.unmuteAll(listId)
}
