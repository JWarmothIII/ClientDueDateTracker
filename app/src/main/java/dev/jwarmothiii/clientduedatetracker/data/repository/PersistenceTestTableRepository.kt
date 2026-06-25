package dev.jwarmothiii.clientduedatetracker.data.repository

import dev.jwarmothiii.clientduedatetracker.data.database.PersistenceTestTableDao
import dev.jwarmothiii.clientduedatetracker.data.database.PersistenceTestTableEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PersistenceTestTableRepository
    @Inject
    constructor(
        private val persistenceTestTableDao: PersistenceTestTableDao,
    ) {
        fun observeTestTables(): Flow<List<PersistenceTestTableEntity>> = persistenceTestTableDao.observeAll()

        suspend fun ensureStartupTestTable() {
            val existingTestTable = persistenceTestTableDao.findByLabel(STARTUP_TEST_TABLE_LABEL)
            if (existingTestTable == null) {
                persistenceTestTableDao.upsert(
                    PersistenceTestTableEntity(
                        label = STARTUP_TEST_TABLE_LABEL,
                        message = "Room persisted this persistence test table row.",
                        createdAtEpochMillis = System.currentTimeMillis(),
                    ),
                )
            }
        }

        companion object {
            const val STARTUP_TEST_TABLE_LABEL = "startup-test-table"
        }
    }
