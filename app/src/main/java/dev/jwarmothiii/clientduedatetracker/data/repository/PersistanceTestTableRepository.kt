package dev.jwarmothiii.clientduedatetracker.data.repository

import dev.jwarmothiii.clientduedatetracker.data.database.PersistanceTestTableDao
import dev.jwarmothiii.clientduedatetracker.data.database.PersistanceTestTableEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PersistanceTestTableRepository
    @Inject
    constructor(
        private val persistanceTestTableDao: PersistanceTestTableDao,
    ) {
        fun observeTestTables(): Flow<List<PersistanceTestTableEntity>> = persistanceTestTableDao.observeAll()

        suspend fun ensureStartupTestTable() {
            val existingTestTable = persistanceTestTableDao.findByLabel(STARTUP_TEST_TABLE_LABEL)
            if (existingTestTable == null) {
                persistanceTestTableDao.upsert(
                    PersistanceTestTableEntity(
                        label = STARTUP_TEST_TABLE_LABEL,
                        message = "Room persisted this persistance test table row.",
                        createdAtEpochMillis = System.currentTimeMillis(),
                    ),
                )
            }
        }

        companion object {
            const val STARTUP_TEST_TABLE_LABEL = "startup-test-table"
        }
    }
