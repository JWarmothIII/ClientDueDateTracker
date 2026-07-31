package dev.jwarmothiii.clientduedatetracker.domain.notes.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import dev.jwarmothiii.clientduedatetracker.database.entity.NoteEntity
import kotlinx.coroutines.flow.Flow

data class NoteWithPublicIds(
    val id: Long,
    val publicId: String,
    val clientPublicId: String,
    val requirementPublicId: String?,
    val clientInitials: String,
    val content: String,
    val pinned: Boolean,
    val createdAt: Long,
    val updatedAt: Long,
)

@Dao
interface NoteDao {
    @Query("SELECT id FROM clients WHERE public_id = :publicId")
    suspend fun clientInternalId(publicId: String): Long?

    @Query("SELECT id FROM requirements WHERE public_id = :publicId")
    suspend fun requirementInternalId(publicId: String): Long?

    @Insert
    suspend fun insert(entity: NoteEntity): Long

    @Update
    suspend fun update(entity: NoteEntity)

    @Query("SELECT * FROM notes WHERE public_id = :publicId")
    suspend fun entity(publicId: String): NoteEntity?

    @Query(
        """
        SELECT notes.id,
               notes.public_id AS publicId,
               clients.public_id AS clientPublicId,
               requirements.public_id AS requirementPublicId,
               clients.initials AS clientInitials,
               notes.content,
               notes.pinned,
               notes.created_at AS createdAt,
               notes.updated_at AS updatedAt
        FROM notes
        JOIN clients ON clients.id = notes.client_id
        LEFT JOIN requirements ON requirements.id = notes.requirement_id
        WHERE notes.id = :id
        """,
    )
    suspend fun joined(id: Long): NoteWithPublicIds?

    @Query(
        """
        SELECT notes.id,
               notes.public_id AS publicId,
               clients.public_id AS clientPublicId,
               requirements.public_id AS requirementPublicId,
               clients.initials AS clientInitials,
               notes.content,
               notes.pinned,
               notes.created_at AS createdAt,
               notes.updated_at AS updatedAt
        FROM notes
        JOIN clients ON clients.id = notes.client_id
        LEFT JOIN requirements ON requirements.id = notes.requirement_id
        WHERE notes.pinned = 1
        ORDER BY notes.updated_at DESC
        LIMIT :limit
        """,
    )
    fun observePinned(limit: Int): Flow<List<NoteWithPublicIds>>

    @Query(
        """
        SELECT COUNT(*)
        FROM requirements
        JOIN contracts ON contracts.id = requirements.contract_id
        WHERE requirements.id = :requirementId
          AND contracts.client_id = :clientId
        """,
    )
    suspend fun requirementBelongsToClient(
        requirementId: Long,
        clientId: Long,
    ): Int

    @Query("DELETE FROM notes WHERE public_id = :publicId")
    suspend fun delete(publicId: String): Int
}
