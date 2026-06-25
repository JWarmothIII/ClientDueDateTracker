package dev.jwarmothiii.clientduedatetracker.data.database

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "persistence_test_table",
    indices = [Index(value = ["label"], unique = true)],
)
data class PersistenceTestTableEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val label: String,
    val message: String,
    val createdAtEpochMillis: Long,
)
