package dev.jwarmothiii.clientduedatetracker.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "clients",
    indices = [Index(value = ["public_id"], unique = true)],
)
data class ClientEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    @ColumnInfo(name = "public_id")
    val publicId: String,
    val initials: String,
    @ColumnInfo(name = "intake_date")
    val intakeDate: Long,
    @ColumnInfo(name = "scheduled_assessment_date")
    val scheduledAssessmentDate: Long,
    @ColumnInfo(name = "actual_assessment_completed_date")
    val actualAssessmentCompletedDate: Long?,
    @ColumnInfo(name = "planned_exit_date")
    val plannedExitDate: Long?,
)
