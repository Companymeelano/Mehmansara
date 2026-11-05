package ir.ahvaz.mehra.core.di

import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import ir.ahvaz.mehra.data.local.MehraDatabase
import ir.ahvaz.mehra.data.local.dao.ApiCacheDao
import ir.ahvaz.mehra.data.local.dao.NotificationDao
import ir.ahvaz.mehra.data.local.dao.PendingOperationDao
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): MehraDatabase =
        Room.databaseBuilder(context, MehraDatabase::class.java, MehraDatabase.NAME)
            // The database only holds cache + outbox; a destructive migration is safe and
            // avoids shipping half-applied migrations for a cache.
            .fallbackToDestructiveMigration()
            .build()

    @Provides
    fun provideApiCacheDao(database: MehraDatabase): ApiCacheDao = database.apiCacheDao()

    @Provides
    fun providePendingOperationDao(database: MehraDatabase): PendingOperationDao =
        database.pendingOperationDao()

    @Provides
    fun provideNotificationDao(database: MehraDatabase): NotificationDao = database.notificationDao()
}
