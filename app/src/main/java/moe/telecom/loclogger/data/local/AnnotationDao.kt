package moe.telecom.loclogger.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import moe.telecom.loclogger.data.local.entity.AnnotationEntity

@Dao
interface AnnotationDao {
    @Query("SELECT * FROM annotations WHERE trackId = :trackId ORDER BY timestamp ASC")
    suspend fun getAnnotationsForTrackSync(trackId: Long): List<AnnotationEntity>

    @Query("SELECT COUNT(*) FROM annotations WHERE trackId = :trackId")
    suspend fun getAnnotationCount(trackId: Long): Int

    @Insert
    suspend fun insertAnnotation(annotation: AnnotationEntity): Long
}
