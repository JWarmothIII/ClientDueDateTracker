package dev.jwarmothiii.clientduedatetracker.data.database

import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
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
            .databaseBuilder(
                context,
                ClientDueDateDatabase::class.java,
                DATABASE_NAME,
            ).build()

    @Provides
    fun providePersistenceTestTableDao(database: ClientDueDateDatabase): PersistenceTestTableDao = database.persistenceTestTableDao()
}
