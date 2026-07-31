package dev.jwarmothiii.clientduedatetracker.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "notification_deliveries",
    foreignKeys = [
        ForeignKey(
            entity = RequirementEntity::class,
            parentColumns = ["id"],
            childColumns = ["requirement_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index("requirement_id"),
        Index(value = ["requirement_id", "delivery_kind", "delivery_date"], unique = true),
    ],
)
data class NotificationDeliveryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    @ColumnInfo(name = "requirement_id")
    val requirementId: Long,
    @ColumnInfo(name = "delivery_kind")
    val deliveryKind: String,
    @ColumnInfo(name = "delivery_date")
    val deliveryDate: Long,
    @ColumnInfo(name = "delivered_at")
    val deliveredAt: Long,
)
