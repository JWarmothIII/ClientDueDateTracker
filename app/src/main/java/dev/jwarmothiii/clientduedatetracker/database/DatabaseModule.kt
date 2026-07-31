package dev.jwarmothiii.clientduedatetracker.database

import android.content.Context
import androidx.room.Room
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import dev.jwarmothiii.clientduedatetracker.domain.client.api.ClientApi
import dev.jwarmothiii.clientduedatetracker.domain.client.data.ClientDao
import dev.jwarmothiii.clientduedatetracker.domain.client.data.RoomClientApi
import dev.jwarmothiii.clientduedatetracker.domain.contractdefinition.api.ContractDefinitionApi
import dev.jwarmothiii.clientduedatetracker.domain.contractdefinition.data.DefinitionDao
import dev.jwarmothiii.clientduedatetracker.domain.contractdefinition.data.RoomContractDefinitionApi
import dev.jwarmothiii.clientduedatetracker.domain.contracttracking.api.ContractTrackingApi
import dev.jwarmothiii.clientduedatetracker.domain.contracttracking.data.RoomContractTrackingApi
import dev.jwarmothiii.clientduedatetracker.domain.contracttracking.data.TrackingDao
import dev.jwarmothiii.clientduedatetracker.domain.notes.api.NotesApi
import dev.jwarmothiii.clientduedatetracker.domain.notes.data.NoteDao
import dev.jwarmothiii.clientduedatetracker.domain.notes.data.RoomNotesApi
import dev.jwarmothiii.clientduedatetracker.domain.notification.api.NotificationApi
import dev.jwarmothiii.clientduedatetracker.domain.notification.data.NotificationDeliveryDao
import dev.jwarmothiii.clientduedatetracker.domain.notification.data.RoomNotificationApi
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    private const val DATABASE_NAME = "client_due_date_tracker.db"

    @Provides
    @Singleton
    fun provideDatabase(
        @ApplicationContext context: Context,
    ): ClientDueDateDatabase =
        Room
            .databaseBuilder(context, ClientDueDateDatabase::class.java, DATABASE_NAME)
            .build()

    @Provides
    fun provideClientDao(database: ClientDueDateDatabase): ClientDao = database.clientDao()

    @Provides
    fun provideDefinitionDao(database: ClientDueDateDatabase): DefinitionDao = database.definitionDao()

    @Provides
    fun provideTrackingDao(database: ClientDueDateDatabase): TrackingDao = database.trackingDao()

    @Provides
    fun provideNoteDao(database: ClientDueDateDatabase): NoteDao = database.noteDao()

    @Provides
    fun provideNotificationDeliveryDao(database: ClientDueDateDatabase): NotificationDeliveryDao = database.notificationDeliveryDao()
}

@Module
@InstallIn(SingletonComponent::class)
abstract class ApiBindingModule {
    @Binds
    abstract fun bindTransactionRunner(implementation: RoomDatabaseTransactionRunner): DatabaseTransactionRunner

    @Binds
    abstract fun bindClientApi(implementation: RoomClientApi): ClientApi

    @Binds
    abstract fun bindDefinitionApi(implementation: RoomContractDefinitionApi): ContractDefinitionApi

    @Binds
    abstract fun bindTrackingApi(implementation: RoomContractTrackingApi): ContractTrackingApi

    @Binds
    abstract fun bindNotesApi(implementation: RoomNotesApi): NotesApi

    @Binds
    abstract fun bindNotificationApi(implementation: RoomNotificationApi): NotificationApi
}
