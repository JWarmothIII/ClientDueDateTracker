package dev.jwarmothiii.clientduedatetracker.domain.notes.data

import dev.jwarmothiii.clientduedatetracker.database.entity.NoteEntity
import dev.jwarmothiii.clientduedatetracker.domain.client.model.ClientId
import dev.jwarmothiii.clientduedatetracker.domain.contracttracking.model.RequirementId
import dev.jwarmothiii.clientduedatetracker.domain.notes.api.DashboardNote
import dev.jwarmothiii.clientduedatetracker.domain.notes.api.NoteCommandResult
import dev.jwarmothiii.clientduedatetracker.domain.notes.api.NotesApi
import dev.jwarmothiii.clientduedatetracker.domain.notes.model.Note
import dev.jwarmothiii.clientduedatetracker.domain.notes.model.NoteId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Instant
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RoomNotesApi
    @Inject
    constructor(
        private val noteDao: NoteDao,
    ) : NotesApi {
        override suspend fun create(
            clientId: ClientId,
            content: String,
            requirementId: RequirementId?,
            pinned: Boolean,
            now: Instant,
        ): NoteCommandResult<Note> {
            if (content.isBlank()) return NoteCommandResult.ValidationError("Note content is required.")
            val clientInternalId = noteDao.clientInternalId(clientId.value) ?: return NoteCommandResult.NotFound
            val requirementInternalId = requirementId?.let { noteDao.requirementInternalId(it.value) }
            if (requirementId != null && requirementInternalId == null) return NoteCommandResult.NotFound
            if (
                requirementInternalId != null &&
                noteDao.requirementBelongsToClient(requirementInternalId, clientInternalId) != 1
            ) {
                return NoteCommandResult.Conflict("Requirement belongs to a different client.")
            }
            val entity =
                NoteEntity(
                    publicId = UUID.randomUUID().toString(),
                    clientId = clientInternalId,
                    requirementId = requirementInternalId,
                    content = content.trim(),
                    pinned = pinned,
                    createdAt = now.toEpochMilli(),
                    updatedAt = now.toEpochMilli(),
                )
            val id = noteDao.insert(entity)
            return NoteCommandResult.Success(
                entity.copy(id = id).toModel(clientId, requirementId),
            )
        }

        override suspend fun update(
            noteId: NoteId,
            content: String,
            pinned: Boolean,
            now: Instant,
        ): NoteCommandResult<Note> {
            if (content.isBlank()) return NoteCommandResult.ValidationError("Note content is required.")
            val entity = noteDao.entity(noteId.value) ?: return NoteCommandResult.NotFound
            val updated = entity.copy(content = content.trim(), pinned = pinned, updatedAt = now.toEpochMilli())
            noteDao.update(updated)
            return NoteCommandResult.Success(
                noteDao.joined(updated.id)?.toNote()
                    ?: return NoteCommandResult.NotFound,
            )
        }

        override suspend fun permanentlyDelete(noteId: NoteId): NoteCommandResult<Unit> =
            if (noteDao.delete(noteId.value) == 1) {
                NoteCommandResult.Success(Unit)
            } else {
                NoteCommandResult.NotFound
            }

        override fun observeRecentlyUpdatedPinned(limit: Int): Flow<List<DashboardNote>> =
            noteDao.observePinned(limit).map { rows ->
                rows.map { DashboardNote(it.toNote(), it.clientInitials) }
            }
    }

private fun NoteEntity.toModel(
    clientId: ClientId,
    requirementId: RequirementId?,
): Note =
    Note(
        id = NoteId(publicId),
        clientId = clientId,
        requirementId = requirementId,
        content = content,
        pinned = pinned,
        createdAt = Instant.ofEpochMilli(createdAt),
        updatedAt = Instant.ofEpochMilli(updatedAt),
    )

private fun NoteWithPublicIds.toNote(): Note =
    Note(
        id = NoteId(publicId),
        clientId = ClientId(clientPublicId),
        requirementId = requirementPublicId?.let(::RequirementId),
        content = content,
        pinned = pinned,
        createdAt = Instant.ofEpochMilli(createdAt),
        updatedAt = Instant.ofEpochMilli(updatedAt),
    )
