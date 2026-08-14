package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

data class Hotel(
    val id: String = "hotel_01",
    val name: String = "Grand Horizon Hotel",
    val address: String = "742 Evergreen Terrace",
    val floors: Int = 5,
    val updatedAt: String = ""
)

@Entity(tableName = "rooms")
data class RoomEntry(
    @PrimaryKey val id: String, // e.g. "room_402"
    val hotelId: String = "hotel_01",
    val roomId: String, // e.g. "402"
    val floor: Int,
    val status: String, // "vacant", "occupied", "evacuated", "needs_help"
    val updatedAt: String = ""
)

@Entity(tableName = "guests")
data class Guest(
    @PrimaryKey val id: String, // matches roomId or device session
    val roomId: String,
    val floor: Int,
    val lat: Double = 37.7749,
    val lng: Double = -122.4194,
    val status: String, // "safe", "needs_help", "no_response"
    val updatedAt: String = ""
)

data class MediaPayload(
    val mimeType: String = "",
    val size: Long = 0L,
    val mediaUrl: String = ""
)

@Entity(tableName = "sos_reports")
data class SosReport(
    @PrimaryKey val id: String,
    val roomId: String,
    val floor: Int,
    val hotelName: String = "Grand Horizon Hotel",
    val message: String,
    val mediaUrl: String? = null,
    val mimeType: String? = null,
    val status: String = "new", // "new", "triaged", "clustered"
    val createdAt: String = "",
    val updatedAt: String = ""
)

data class TimelineItem(
    val id: String,
    val note: String,
    val status: String,
    val createdAt: String
)

@Entity(tableName = "incidents")
data class Incident(
    @PrimaryKey val id: String,
    val title: String,
    val summary: String,
    val location: String,
    val severity: String, // "low", "medium", "high", "critical"
    val status: String, // "new", "investigating", "contained", "resolved"
    val trapped: Int = 0,
    val sourceReportIds: String = "", // Comma-separated or JSON list
    val createdAt: String = "",
    val updatedAt: String = "",
    val roomId: String? = null,
    val mediaUrl: String? = null,
    val mediaType: String? = null
)

@Entity(tableName = "danger_zones")
data class DangerZone(
    @PrimaryKey val id: String,
    val floor: Int = 4,
    val label: String = "Fire Hazard Zone",
    val severity: String = "critical", // "low", "medium", "high", "critical"
    val lat: Double = 37.7749,
    val lng: Double = -122.4194,
    val crsX: Double = 150.0,
    val crsY: Double = 450.0,
    val radiusMeters: Double = 55.0,
    val hazardType: String = "fire", // "fire", "smoke", "blockage"
    val active: Boolean = true,
    val updatedAt: String = ""
)

@Entity(tableName = "broadcasts")
data class Broadcast(
    @PrimaryKey val id: String,
    val message: String,
    val priority: String = "critical", // "low", "normal", "high", "critical"
    val target: String = "all", // "all", "floor_1", "floor_2", "floor_3", "floor_4", "floor_5", "staff", "guests"
    val targetFloor: Int? = null, // null for all floors / building-wide, or 1..5
    val hasAudio: Boolean = false,
    val audioUrl: String? = null,
    val audioTtsText: String? = null,
    val senderTitle: String = "Incident Commander",
    val delivery: String = "sent", // "sent", "queued", "failed"
    val createdAt: String = ""
)

@Entity(tableName = "active_emergencies")
data class ActiveEmergency(
    @PrimaryKey val id: String, // e.g. "room_402"
    val roomId: String,
    val floor: Int,
    val status: String, // "checking", "evacuated", "trapped", "no_response"
    val statusChipsCsv: String = "", // e.g. "Heavy Smoke,Door Blocked"
    val lastUpdated: String = "",
    val occupantCount: Int = 1,
    val signalStrength: String = "high", // "low", "medium", "high"
    val sosText: String = ""
)

@Entity(tableName = "floor_nodes")
data class FloorNode(
    @PrimaryKey val id: String,
    val floor: Int,
    val nodeType: String, // "walkable" (cyan), "portal" (orange)
    val x: Double,
    val y: Double,
    val label: String = ""
)

@Entity(tableName = "floor_plans")
data class FloorPlan(
    @PrimaryKey val floor: Int,
    val imageUrl: String
)

data class ChatMessage(
    val id: String,
    val roomId: String,
    val senderRole: String, // "guest", "responder", "system"
    val text: String,
    val translatedText: String? = null,
    val timestamp: Long = System.currentTimeMillis(),
    val audioUrl: String? = null
)
