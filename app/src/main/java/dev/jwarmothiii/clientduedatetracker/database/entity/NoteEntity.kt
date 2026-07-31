package dev.jwarmothiii.clientduedatetracker.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "notes",
    foreignKeys = [
        ForeignKey(
            entity = ClientEntity::class,
            parentColumns = ["id"],
            childColumns = ["client_id"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = RequirementEntity::class,
            parentColumns = ["id"],
            childColumns = ["requirement_id"],
            onDelete = ForeignKey.SET_NULL,
        ),
    ],
    indices = [
        Index(value = ["public_id"], unique = true),
        Index("client_id"),
        Index("requirement_id"),
        Index(value = ["pinned", "updated_at"]),
    ],
)
data class NoteEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    @ColumnInfo(name = "public_id")
    val publicId: String,
    @ColumnInfo(name = "client_id")
    val clientId: Long,
    @ColumnInfo(name = "requirement_id")
    val requirementId: Long?,
    val content: String,
    val pinned: Boolean,
    @ColumnInfo(name = "created_at")
    val createdAt: Long,
    @ColumnInfo(name = "updated_at")
    val updatedAt: Long,
)
