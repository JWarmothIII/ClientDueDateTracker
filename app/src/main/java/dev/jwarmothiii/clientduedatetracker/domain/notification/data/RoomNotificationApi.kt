package dev.jwarmothiii.clientduedatetracker.domain.notification.data

import androidx.room.withTransaction
import dev.jwarmothiii.clientduedatetracker.database.ClientDueDateDatabase
import dev.jwarmothiii.clientduedatetracker.database.entity.NotificationDeliveryEntity
import dev.jwarmothiii.clientduedatetracker.domain.contracttracking.api.ContractTrackingApi
import dev.jwarmothiii.clientduedatetracker.domain.notification.api.DeliveryKind
import dev.jwarmothiii.clientduedatetracker.domain.notification.api.NotificationApi
import dev.jwarmothiii.clientduedatetracker.domain.notification.api.ReminderCandidate
import dev.jwarmothiii.clientduedatetracker.domain.notification.model.DeliveryHistory
import dev.jwarmothiii.clientduedatetracker.domain.notification.model.ReminderPlanner
import java.time.Instant
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RoomNotificationApi
    @Inject
    constructor(
        private val database: ClientDueDateDatabase,
        private val trackingApi: ContractTrackingApi,
        private val deliveryDao: NotificationDeliveryDao,
    ) : NotificationApi {
        override suspend fun reminderCandidates(today: LocalDate): List<ReminderCandidate> {
            val pending = trackingApi.pendingRequirements()
            val publicIdsByInternalId =
                pending
                    .mapNotNull { row ->
                        deliveryDao
                            .requirementInternalId(row.requirement.id.value)
                            ?.let { it to row.requirement.id }
                    }.toMap()
            val deliveries =
                deliveryDao.deliveries().mapNotNull { delivery ->
                    val publicId = publicIdsByInternalId[delivery.requirementId] ?: return@mapNotNull null
                    DeliveryHistory(
                        requirementId = publicId,
                        kind = DeliveryKind.valueOf(delivery.deliveryKind),
                        deliveryDate = LocalDate.ofEpochDay(delivery.deliveryDate),
                    )
                }
            return ReminderPlanner.candidates(pending, deliveries, today)
        }

        override suspend fun recordDelivered(
            candidates: List<ReminderCandidate>,
            deliveryDate: LocalDate,
            deliveredAt: Instant,
        ) {
            database.withTransaction {
                candidates.forEach { candidate ->
                    val requirementId = deliveryDao.requirementInternalId(candidate.requirementId.value) ?: return@forEach
                    deliveryDao.insert(
                        NotificationDeliveryEntity(
                            requirementId = requirementId,
                            deliveryKind = candidate.kind.name,
                            deliveryDate = deliveryDate.toEpochDay(),
                            deliveredAt = deliveredAt.toEpochMilli(),
                        ),
                    )
                }
            }
        }
    }
