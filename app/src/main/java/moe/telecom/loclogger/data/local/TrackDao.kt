package moe.telecom.loclogger.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import moe.telecom.loclogger.data.local.entity.TrackEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TrackDao {
    @Query("SELECT * FROM tracks ORDER BY startTime DESC")
    fun getAllTracks(): Flow<List<TrackEntity>>

    @Query("SELECT * FROM tracks WHERE id = :id")
    suspend fun getTrackById(id: Long): TrackEntity?

    @Insert
    suspend fun insertTrack(track: TrackEntity): Long

    @Delete
    suspend fun deleteTrack(track: TrackEntity)

    @Query("UPDATE tracks SET name = :name WHERE id = :id")
    suspend fun renameTrack(id: Long, name: String)

    @Query("UPDATE tracks SET activityType = :activityType WHERE id = :id")
    suspend fun updateActivityType(id: Long, activityType: Int)

    @Query("UPDATE tracks SET isRecording = 0, isPaused = 0, endTime = :endTime, pointCount = :pointCount, totalDistance = :distance, maxSpeed = :maxSpeed, avgSpeed = :avgSpeed, maxAltitude = :maxAlt, minAltitude = :minAlt, altitudeDiff = :altDiff, annotationCount = :annotationCount WHERE id = :id")
    suspend fun finishTrack(
        id: Long,
        endTime: Long,
        pointCount: Int,
        distance: Double,
        maxSpeed: Float,
        avgSpeed: Float,
        maxAlt: Double?,
        minAlt: Double?,
        altDiff: Double,
        annotationCount: Int
    )
}
