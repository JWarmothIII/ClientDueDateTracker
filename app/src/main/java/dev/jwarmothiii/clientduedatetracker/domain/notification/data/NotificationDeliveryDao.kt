package dev.jwarmothiii.clientduedatetracker.domain.notification.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import dev.jwarmothiii.clientduedatetracker.database.entity.NotificationDeliveryEntity

@Dao
interface NotificationDeliveryDao {
    @Query("SELECT * FROM notification_deliveries")
    suspend fun deliveries(): List<NotificationDeliveryEntity>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(entity: NotificationDeliveryEntity): Long

    @Query("SELECT id FROM requirements WHERE public_id = :publicId")
    suspend fun requirementInternalId(publicId: String): Long?
}
