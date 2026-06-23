package dev.jwarmothiii.clientduedatetracker.data.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface PersistanceTestTableDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(testTable: PersistanceTestTableEntity): Long

    @Query("SELECT * FROM persistance_test_table ORDER BY createdAtEpochMillis DESC")
    fun observeAll(): Flow<List<PersistanceTestTableEntity>>

    @Query("SELECT * FROM persistance_test_table WHERE label = :label LIMIT 1")
    suspend fun findByLabel(label: String): PersistanceTestTableEntity?

    @Query("DELETE FROM persistance_test_table")
    suspend fun deleteAll()
}
