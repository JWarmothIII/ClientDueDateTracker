package dev.jwarmothiii.clientduedatetracker.domain.notes.model

import dev.jwarmothiii.clientduedatetracker.domain.client.model.ClientId
import dev.jwarmothiii.clientduedatetracker.domain.contracttracking.model.RequirementId
import java.time.Instant

@JvmInline
value class NoteId(
    val value: String,
) {
    init {
        require(value.isNotBlank()) { "NoteId cannot be blank." }
    }
}

data class Note(
    val id: NoteId,
    val clientId: ClientId,
    val requirementId: RequirementId?,
    val content: String,
    val pinned: Boolean,
    val createdAt: Instant,
    val updatedAt: Instant,
) {
    init {
        require(content.isNotBlank()) { "Note content cannot be blank." }
    }
}
