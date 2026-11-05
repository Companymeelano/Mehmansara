package ir.ahvaz.mehra.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import ir.ahvaz.mehra.data.local.dao.ApiCacheDao
import ir.ahvaz.mehra.data.local.dao.NotificationDao
import ir.ahvaz.mehra.data.local.dao.PendingOperationDao
import ir.ahvaz.mehra.data.local.entity.ApiCacheEntity
import ir.ahvaz.mehra.data.local.entity.CachedNotificationEntity
import ir.ahvaz.mehra.data.local.entity.PendingOperationEntity

@Database(
    entities = [
        ApiCacheEntity::class,
        PendingOperationEntity::class,
        CachedNotificationEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
abstract class MehraDatabase : RoomDatabase() {
    abstract fun apiCacheDao(): ApiCacheDao
    abstract fun pendingOperationDao(): PendingOperationDao
    abstract fun notificationDao(): NotificationDao

    companion object {
        const val NAME = "mehra.db"
    }
}
