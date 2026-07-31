package dev.jwarmothiii.clientduedatetracker.domain.notes.api

import dev.jwarmothiii.clientduedatetracker.domain.client.model.ClientId
import dev.jwarmothiii.clientduedatetracker.domain.contracttracking.model.RequirementId
import dev.jwarmothiii.clientduedatetracker.domain.notes.model.Note
import dev.jwarmothiii.clientduedatetracker.domain.notes.model.NoteId
import kotlinx.coroutines.flow.Flow
import java.time.Instant

sealed interface NoteCommandResult<out T> {
    data class Success<T>(
        val value: T,
    ) : NoteCommandResult<T>

    data class ValidationError(
        val message: String,
    ) : NoteCommandResult<Nothing>

    data object NotFound : NoteCommandResult<Nothing>

    data class Conflict(
        val message: String,
    ) : NoteCommandResult<Nothing>
}

data class DashboardNote(
    val note: Note,
    val clientInitials: String,
)

interface NotesApi {
    suspend fun create(
        clientId: ClientId,
        content: String,
        requirementId: RequirementId?,
        pinned: Boolean,
        now: Instant,
    ): NoteCommandResult<Note>

    suspend fun update(
        noteId: NoteId,
        content: String,
        pinned: Boolean,
        now: Instant,
    ): NoteCommandResult<Note>

    suspend fun permanentlyDelete(noteId: NoteId): NoteCommandResult<Unit>

    fun observeRecentlyUpdatedPinned(limit: Int = 3): Flow<List<DashboardNote>>
}
