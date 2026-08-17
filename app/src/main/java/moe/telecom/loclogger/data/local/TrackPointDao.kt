package moe.telecom.loclogger.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import moe.telecom.loclogger.data.local.entity.TrackPointEntity

@Dao
interface TrackPointDao {
    @Query("SELECT * FROM track_points WHERE trackId = :trackId ORDER BY timestamp ASC")
    suspend fun getPointsForTrackSync(trackId: Long): List<TrackPointEntity>

    @Insert
    suspend fun insertPoint(point: TrackPointEntity): Long
}
