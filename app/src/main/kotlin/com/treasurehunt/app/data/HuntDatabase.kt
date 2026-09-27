package com.treasurehunt.app.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Relation
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "locations")
data class LocationEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val listId: Long,
    val name: String,
    val description: String,
    val lat: Double,
    val lon: Double,
    val muted: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
)

@Entity(tableName = "huntlists")
data class HuntListEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val createdAt: Long = System.currentTimeMillis(),
)

/** List with its spots, as observed by the rest of the app. */
data class HuntListWithLocations(
    @Embedded val list: HuntListEntity,

    @Relation(parentColumn = "id", entityColumn = "listId")
    val locations: List<LocationEntity> = emptyList(),
) {
    val id: Long get() = list.id
    val name: String get() = list.name
    val createdAt: Long get() = list.createdAt
}

@Dao
interface HuntDao {
    @Transaction
    @Query("SELECT * FROM huntlists ORDER BY createdAt DESC")
    fun observeLists(): Flow<List<HuntListWithLocations>>

    @Transaction
    @Query("SELECT * FROM huntlists WHERE id = :id")
    suspend fun listById(id: Long): HuntListWithLocations?

    @Insert
    suspend fun insertList(list: HuntListEntity): Long

    @Insert
    suspend fun insertLocations(locations: List<LocationEntity>)

    @Query("DELETE FROM locations WHERE listId = :listId")
    suspend fun deleteLocationsOf(listId: Long)

    @Query("DELETE FROM huntlists WHERE id = :id")
    suspend fun deleteListById(id: Long)

    @Query("UPDATE locations SET muted = :muted WHERE id = :id")
    suspend fun setLocationMuted(id: Long, muted: Boolean)

    @Query("UPDATE locations SET muted = 0 WHERE listId = :listId")
    suspend fun unmuteAll(listId: Long)
}

@Database(
    entities = [HuntListEntity::class, LocationEntity::class],
    version = 1,
    exportSchema = false,
)
abstract class HuntDatabase : RoomDatabase() {
    abstract fun dao(): HuntDao

    companion object {
        @Volatile
        private var instance: HuntDatabase? = null

        fun get(context: Context): HuntDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    HuntDatabase::class.java,
                    "hunt.db",
                ).build().also { instance = it }
            }
    }
}
