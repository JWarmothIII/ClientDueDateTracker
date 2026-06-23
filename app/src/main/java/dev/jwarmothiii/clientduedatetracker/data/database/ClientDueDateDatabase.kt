package dev.jwarmothiii.clientduedatetracker.data.database

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [PersistanceTestTableEntity::class],
    version = 1,
    exportSchema = true,
)
abstract class ClientDueDateDatabase : RoomDatabase() {
    abstract fun persistanceTestTableDao(): PersistanceTestTableDao
}
