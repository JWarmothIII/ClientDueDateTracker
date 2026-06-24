package dev.jwarmothiii.clientduedatetracker.data.database

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PersistanceTestTableDaoTest {
    private lateinit var database: ClientDueDateDatabase
    private lateinit var persistanceTestTableDao: PersistanceTestTableDao

    @Before
    fun createDatabase() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database =
            Room
                .inMemoryDatabaseBuilder(
                    context,
                    ClientDueDateDatabase::class.java,
                ).build()
        persistanceTestTableDao = database.persistenceTestTableDao()
    }

    @After
    fun closeDatabase() {
        database.close()
    }

    @Test
    fun upsertStoresAndReadsTestTable() =
        runBlocking {
            val testTable =
                PersistanceTestTableEntity(
                    label = "startup-test-table",
                    message = "Room persisted this persistance test table row.",
                    createdAtEpochMillis = 1_000,
                )

            persistanceTestTableDao.upsert(testTable)

            val storedTestTable = persistanceTestTableDao.findByLabel("startup-test-table")
            assertEquals("Room persisted this persistance test table row.", storedTestTable?.message)
        }

    @Test
    fun upsertReplacesTestTableWithSameLabel() =
        runBlocking {
            persistanceTestTableDao.upsert(
                PersistanceTestTableEntity(
                    label = "startup-test-table",
                    message = "First test table.",
                    createdAtEpochMillis = 1_000,
                ),
            )

            persistanceTestTableDao.upsert(
                PersistanceTestTableEntity(
                    label = "startup-test-table",
                    message = "Updated test table.",
                    createdAtEpochMillis = 2_000,
                ),
            )

            val storedTestTables = persistanceTestTableDao.observeAll().first()
            assertEquals(1, storedTestTables.size)
            assertEquals("Updated test table.", storedTestTables.single().message)
        }

    @Test
    fun findByLabelReturnsNullWhenTestTableDoesNotExist() =
        runBlocking {
            val storedTestTable = persistanceTestTableDao.findByLabel("missing-test-table")

            assertNull(storedTestTable)
        }
}
