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
class PersistenceTestTableDaoTest {
    private lateinit var database: ClientDueDateDatabase
    private lateinit var persistenceTestTableDao: PersistenceTestTableDao

    @Before
    fun createDatabase() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database =
            Room
                .inMemoryDatabaseBuilder(
                    context,
                    ClientDueDateDatabase::class.java,
                ).build()
        persistenceTestTableDao = database.persistenceTestTableDao()
    }

    @After
    fun closeDatabase() {
        database.close()
    }

    @Test
    fun upsertStoresAndReadsTestTable() =
        runBlocking {
            val testTable =
                PersistenceTestTableEntity(
                    label = "startup-test-table",
                    message = "Room persisted this persistence test table row.",
                    createdAtEpochMillis = 1_000,
                )

            persistenceTestTableDao.upsert(testTable)

            val storedTestTable = persistenceTestTableDao.findByLabel("startup-test-table")
            assertEquals("Room persisted this persistence test table row.", storedTestTable?.message)
        }

    @Test
    fun upsertReplacesTestTableWithSameLabel() =
        runBlocking {
            persistenceTestTableDao.upsert(
                PersistenceTestTableEntity(
                    label = "startup-test-table",
                    message = "First test table.",
                    createdAtEpochMillis = 1_000,
                ),
            )

            persistenceTestTableDao.upsert(
                PersistenceTestTableEntity(
                    label = "startup-test-table",
                    message = "Updated test table.",
                    createdAtEpochMillis = 2_000,
                ),
            )

            val storedTestTables = persistenceTestTableDao.observeAll().first()
            assertEquals(1, storedTestTables.size)
            assertEquals("Updated test table.", storedTestTables.single().message)
        }

    @Test
    fun findByLabelReturnsNullWhenTestTableDoesNotExist() =
        runBlocking {
            val storedTestTable = persistenceTestTableDao.findByLabel("missing-test-table")

            assertNull(storedTestTable)
        }
}
