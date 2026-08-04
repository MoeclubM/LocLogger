package moe.telecom.loclogger.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tracks")
data class TrackEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val activityType: Int = 0, // ActivityType ordinal
    val startTime: Long,
    val endTime: Long? = null,
    val totalDistance: Double = 0.0,
    val maxSpeed: Float = 0f,
    val avgSpeed: Float = 0f,
    val maxAltitude: Double? = null,
    val minAltitude: Double? = null,
    val altitudeDiff: Double = 0.0,
    val bearing: Float? = null,
    val pointCount: Int = 0,
    val annotationCount: Int = 0,
    val isRecording: Boolean = false,
    val isPaused: Boolean = false
)
