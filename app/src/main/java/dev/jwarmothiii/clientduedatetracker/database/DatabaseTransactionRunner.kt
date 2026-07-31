package dev.jwarmothiii.clientduedatetracker.database

import androidx.room.withTransaction
import javax.inject.Inject
import javax.inject.Singleton

interface DatabaseTransactionRunner {
    suspend fun <T> runInTransaction(block: suspend () -> T): T
}

@Singleton
class RoomDatabaseTransactionRunner
    @Inject
    constructor(
        private val database: ClientDueDateDatabase,
    ) : DatabaseTransactionRunner {
        override suspend fun <T> runInTransaction(block: suspend () -> T): T = database.withTransaction { block() }
    }
