package dev.jwarmothiii.clientduedatetracker.database

import androidx.room.Database
import androidx.room.RoomDatabase
import dev.jwarmothiii.clientduedatetracker.database.entity.ClientEntity
import dev.jwarmothiii.clientduedatetracker.database.entity.ContractDefinitionAssignmentEntity
import dev.jwarmothiii.clientduedatetracker.database.entity.ContractEntity
import dev.jwarmothiii.clientduedatetracker.database.entity.ContractEventEntity
import dev.jwarmothiii.clientduedatetracker.database.entity.ContractTypeEntity
import dev.jwarmothiii.clientduedatetracker.database.entity.NoteEntity
import dev.jwarmothiii.clientduedatetracker.database.entity.NotificationDeliveryEntity
import dev.jwarmothiii.clientduedatetracker.database.entity.RequirementEntity
import dev.jwarmothiii.clientduedatetracker.database.entity.RequirementSuppressionEntity
import dev.jwarmothiii.clientduedatetracker.database.entity.RequirementTemplateEntity
import dev.jwarmothiii.clientduedatetracker.domain.client.data.ClientDao
import dev.jwarmothiii.clientduedatetracker.domain.contractdefinition.data.DefinitionDao
import dev.jwarmothiii.clientduedatetracker.domain.contracttracking.data.TrackingDao
import dev.jwarmothiii.clientduedatetracker.domain.notes.data.NoteDao
import dev.jwarmothiii.clientduedatetracker.domain.notification.data.NotificationDeliveryDao

@Database(
    entities = [
        ClientEntity::class,
        ContractTypeEntity::class,
        RequirementTemplateEntity::class,
        ContractEntity::class,
        ContractDefinitionAssignmentEntity::class,
        ContractEventEntity::class,
        RequirementEntity::class,
        RequirementSuppressionEntity::class,
        NoteEntity::class,
        NotificationDeliveryEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
abstract class ClientDueDateDatabase : RoomDatabase() {
    abstract fun clientDao(): ClientDao

    abstract fun definitionDao(): DefinitionDao

    abstract fun trackingDao(): TrackingDao

    abstract fun noteDao(): NoteDao

    abstract fun notificationDeliveryDao(): NotificationDeliveryDao
}
