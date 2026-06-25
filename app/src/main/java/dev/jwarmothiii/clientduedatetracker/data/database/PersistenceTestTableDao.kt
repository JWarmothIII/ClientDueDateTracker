package dev.jwarmothiii.clientduedatetracker.data.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface PersistenceTestTableDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(testTable: PersistenceTestTableEntity): Long

    @Query("SELECT * FROM persistence_test_table ORDER BY createdAtEpochMillis DESC")
    fun observeAll(): Flow<List<PersistenceTestTableEntity>>

    @Query("SELECT * FROM persistence_test_table WHERE label = :label LIMIT 1")
    suspend fun findByLabel(label: String): PersistenceTestTableEntity?

    @Query("DELETE FROM persistence_test_table")
    suspend fun deleteAll()
}
