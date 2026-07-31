package dev.jwarmothiii.clientduedatetracker.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "contracts",
    foreignKeys = [
        ForeignKey(
            entity = ClientEntity::class,
            parentColumns = ["id"],
            childColumns = ["client_id"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = ContractTypeEntity::class,
            parentColumns = ["id"],
            childColumns = ["contract_type_id"],
            onDelete = ForeignKey.RESTRICT,
        ),
    ],
    indices = [
        Index(value = ["public_id"], unique = true),
        Index(value = ["client_id"], unique = true),
        Index("contract_type_id"),
        Index("active"),
    ],
)
data class ContractEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    @ColumnInfo(name = "public_id")
    val publicId: String,
    @ColumnInfo(name = "client_id")
    val clientId: Long,
    @ColumnInfo(name = "contract_type_id")
    val contractTypeId: Long,
    @ColumnInfo(name = "start_date")
    val startDate: Long,
    val active: Boolean,
    @ColumnInfo(name = "planned_exit_date")
    val plannedExitDate: Long?,
    @ColumnInfo(name = "actual_exit_date")
    val actualExitDate: Long?,
    @ColumnInfo(name = "exit_kind")
    val exitKind: String?,
)

@Entity(
    tableName = "contract_definition_assignments",
    primaryKeys = ["contract_id", "template_id"],
    foreignKeys = [
        ForeignKey(
            entity = ContractEntity::class,
            parentColumns = ["id"],
            childColumns = ["contract_id"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = RequirementTemplateEntity::class,
            parentColumns = ["id"],
            childColumns = ["template_id"],
            onDelete = ForeignKey.RESTRICT,
        ),
    ],
    indices = [Index("template_id")],
)
data class ContractDefinitionAssignmentEntity(
    @ColumnInfo(name = "contract_id")
    val contractId: Long,
    @ColumnInfo(name = "template_id")
    val templateId: Long,
)

@Entity(
    tableName = "contract_events",
    foreignKeys = [
        ForeignKey(
            entity = ContractEntity::class,
            parentColumns = ["id"],
            childColumns = ["contract_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index(value = ["public_id"], unique = true),
        Index("contract_id"),
        Index(value = ["contract_id", "kind", "event_date"]),
    ],
)
data class ContractEventEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    @ColumnInfo(name = "public_id")
    val publicId: String,
    @ColumnInfo(name = "contract_id")
    val contractId: Long,
    val kind: String,
    @ColumnInfo(name = "event_date")
    val eventDate: Long,
    @ColumnInfo(name = "created_at")
    val createdAt: Long,
)

@Entity(
    tableName = "requirements",
    foreignKeys = [
        ForeignKey(
            entity = ContractEntity::class,
            parentColumns = ["id"],
            childColumns = ["contract_id"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = RequirementTemplateEntity::class,
            parentColumns = ["id"],
            childColumns = ["template_id"],
            onDelete = ForeignKey.RESTRICT,
        ),
    ],
    indices = [
        Index(value = ["public_id"], unique = true),
        Index("contract_id"),
        Index("template_id"),
        Index(value = ["occurrence_key"], unique = true),
        Index(value = ["status", "due_date"]),
    ],
)
data class RequirementEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    @ColumnInfo(name = "public_id")
    val publicId: String,
    @ColumnInfo(name = "contract_id")
    val contractId: Long,
    @ColumnInfo(name = "template_id")
    val templateId: Long?,
    val origin: String,
    @ColumnInfo(name = "occurrence_key")
    val occurrenceKey: String?,
    val title: String,
    val instructions: String,
    @ColumnInfo(name = "due_date")
    val dueDate: Long,
    @ColumnInfo(name = "notification_lead_days")
    val notificationLeadDays: Int,
    val status: String,
    @ColumnInfo(name = "completed_at")
    val completedAt: Long?,
    @ColumnInfo(name = "manually_customized")
    val manuallyCustomized: Boolean,
)

@Entity(
    tableName = "requirement_suppressions",
    foreignKeys = [
        ForeignKey(
            entity = ContractEntity::class,
            parentColumns = ["id"],
            childColumns = ["contract_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index("contract_id"),
        Index(value = ["occurrence_key"], unique = true),
    ],
)
data class RequirementSuppressionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    @ColumnInfo(name = "contract_id")
    val contractId: Long,
    @ColumnInfo(name = "occurrence_key")
    val occurrenceKey: String,
    @ColumnInfo(name = "created_at")
    val createdAt: Long,
)
