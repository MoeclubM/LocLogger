package moe.telecom.loclogger.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import moe.telecom.loclogger.data.local.entity.AnnotationEntity
import moe.telecom.loclogger.data.local.entity.TrackEntity
import moe.telecom.loclogger.data.local.entity.TrackPointEntity

@Database(
    entities = [
        TrackEntity::class,
        TrackPointEntity::class,
        AnnotationEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun trackDao(): TrackDao
    abstract fun trackPointDao(): TrackPointDao
    abstract fun annotationDao(): AnnotationDao

    companion object {
        const val DATABASE_NAME = "gps_logger.db"
    }
}
