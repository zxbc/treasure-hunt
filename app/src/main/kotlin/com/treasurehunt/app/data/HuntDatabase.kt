package com.treasurehunt.app.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Delete
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Relation
import androidx.room.Room
import androidx.room.RoomDatabase
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

    @Relation(parentColumn = "id", entityColumn = "listId")
    val locations: List<LocationEntity> = emptyList(),
)

@Dao
interface HuntDao {
    @Query("SELECT * FROM huntlists ORDER BY createdAt DESC")
    fun observeLists(): Flow<List<HuntListEntity>>

    @Query("SELECT * FROM huntlists WHERE id = :id")
    suspend fun listById(id: Long): HuntListEntity?

    @Insert
    suspend fun insertList(list: HuntListEntity): Long

    @Delete
    suspend fun deleteList(list: HuntListEntity)

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
