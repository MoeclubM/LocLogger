package moe.telecom.loclogger.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import moe.telecom.loclogger.data.local.entity.TrackPointEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TrackPointDao {
    @Query("SELECT * FROM track_points WHERE trackId = :trackId ORDER BY timestamp ASC")
    fun getPointsForTrack(trackId: Long): Flow<List<TrackPointEntity>>

    @Query("SELECT * FROM track_points WHERE trackId = :trackId ORDER BY timestamp ASC")
    suspend fun getPointsForTrackSync(trackId: Long): List<TrackPointEntity>

    @Query("SELECT * FROM track_points WHERE trackId = :trackId ORDER BY timestamp DESC LIMIT 1")
    suspend fun getLastPoint(trackId: Long): TrackPointEntity?

    @Query("SELECT COUNT(*) FROM track_points WHERE trackId = :trackId")
    suspend fun getPointCount(trackId: Long): Int

    @Insert
    suspend fun insertPoint(point: TrackPointEntity): Long

    @Insert
    suspend fun insertPoints(points: List<TrackPointEntity>)

    @Query("DELETE FROM track_points WHERE trackId = :trackId")
    suspend fun deletePointsForTrack(trackId: Long)
}
