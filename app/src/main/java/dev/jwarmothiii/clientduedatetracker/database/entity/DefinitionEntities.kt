package dev.jwarmothiii.clientduedatetracker.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "contract_types",
    indices = [
        Index(value = ["public_id"], unique = true),
        Index(value = ["lineage_id", "version"], unique = true),
        Index(value = ["active"]),
    ],
)
data class ContractTypeEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    @ColumnInfo(name = "public_id")
    val publicId: String,
    @ColumnInfo(name = "lineage_id")
    val lineageId: String,
    val version: Int,
    val name: String,
    val active: Boolean,
)

@Entity(
    tableName = "requirement_templates",
    foreignKeys = [
        ForeignKey(
            entity = ContractTypeEntity::class,
            parentColumns = ["id"],
            childColumns = ["contract_type_id"],
            onDelete = ForeignKey.RESTRICT,
        ),
    ],
    indices = [
        Index("contract_type_id"),
        Index(value = ["public_id"], unique = true),
        Index(value = ["lineage_id", "version"], unique = true),
        Index(value = ["active"]),
    ],
)
data class RequirementTemplateEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    @ColumnInfo(name = "public_id")
    val publicId: String,
    @ColumnInfo(name = "contract_type_id")
    val contractTypeId: Long,
    @ColumnInfo(name = "lineage_id")
    val lineageId: String,
    val version: Int,
    val title: String,
    val instructions: String,
    @ColumnInfo(name = "deadline_policy_json")
    val deadlinePolicyJson: String,
    @ColumnInfo(name = "notification_lead_days")
    val notificationLeadDays: Int,
    val active: Boolean,
)
