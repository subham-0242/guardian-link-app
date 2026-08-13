package com.example.data.repository

import android.content.Context
import com.example.data.local.AppDatabase
import com.example.data.model.ActiveEmergency
import com.example.data.model.Broadcast
import com.example.data.model.ChatMessage
import com.example.data.model.DangerZone
import com.example.data.model.FloorNode
import com.example.data.model.FloorPlan
import com.example.data.model.Guest
import com.example.data.model.Incident
import com.example.data.model.RoomEntry
import com.example.data.model.SosReport
import com.example.data.remote.FirebaseRemoteManager
import com.example.data.remote.GeminiService
import com.example.util.HazardClusterer
import com.example.util.PiiScrubber
import com.example.util.ReportDeduplicator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

class EmergencyRepository(context: Context) {
    private val db = AppDatabase.getDatabase(context)
    private val roomDao = db.roomEntryDao()
    private val guestDao = db.guestDao()
    private val sosReportDao = db.sosReportDao()
    private val incidentDao = db.incidentDao()
    private val dangerZoneDao = db.dangerZoneDao()
    private val broadcastDao = db.broadcastDao()
    private val activeEmergencyDao = db.activeEmergencyDao()
    private val floorNodeDao = db.floorNodeDao()
    private val floorPlanDao = db.floorPlanDao()

    private val scope = CoroutineScope(Dispatchers.IO)

    init {
        scope.launch {
            seedDefaultRoomsIfEmpty()
        }
    }

    private fun nowIso(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US)
        return sdf.format(Date())
    }

    private suspend fun seedDefaultRoomsIfEmpty() {
        val rooms = roomDao.getAllRooms().first()
        if (rooms.isEmpty()) {
            val initialRooms = mutableListOf<RoomEntry>()
            // Floor 4 (401-412)
            for (i in 1..12) {
                val num = 400 + i
                initialRooms.add(
                    RoomEntry(
                        id = "room_$num",
                        roomId = num.toString(),
                        floor = 4,
                        status = "vacant",
                        updatedAt = nowIso()
                    )
                )
            }
            // Floor 3 (301-306)
            for (i in 1..6) {
                val num = 300 + i
                initialRooms.add(
                    RoomEntry(
                        id = "room_$num",
                        roomId = num.toString(),
                        floor = 3,
                        status = "vacant",
                        updatedAt = nowIso()
                    )
                )
            }
            // Floor 2 (201-206)
            for (i in 1..6) {
                val num = 200 + i
                initialRooms.add(
                    RoomEntry(
                        id = "room_$num",
                        roomId = num.toString(),
                        floor = 2,
                        status = "vacant",
                        updatedAt = nowIso()
                    )
                )
            }
            roomDao.insertAll(initialRooms)

            // Seed initial danger zone
            val initZone = DangerZone(
                id = "dz_01",
                floor = 4,
                label = "Active Smoke Corridor 4E",
                severity = "critical",
                lat = 37.7750,
                lng = -122.4190,
                radiusMeters = 15.0,
                active = true,
                updatedAt = nowIso()
            )
            dangerZoneDao.insertOrUpdate(initZone)
            FirebaseRemoteManager.addMemoryDangerZone(initZone)

            // Seed initial broadcast
            val initBc = Broadcast(
                id = "bc_01",
                message = "CRITICAL ALERT: Fire detected on Level 4. Stay inside your room and seal doors.",
                priority = "critical",
                target = "all",
                delivery = "sent",
                createdAt = nowIso()
            )
            broadcastDao.insertBroadcast(initBc)
            FirebaseRemoteManager.addMemoryBroadcast(initBc)

            // Seed initial nodes for Floor 4
            val defaultNodes = listOf(
                FloorNode("fn_401", 4, "walkable", 200.0, 450.0, "East Hallway"),
                FloorNode("fn_402", 4, "walkable", 400.0, 450.0, "Central Corridor"),
                FloorNode("fn_403", 4, "walkable", 600.0, 450.0, "West Hallway"),
                FloorNode("fn_404", 4, "portal", 750.0, 450.0, "Stairwell B (Exit)")
            )
            floorNodeDao.insertNodes(defaultNodes)
            FirebaseRemoteManager.setFloorNodes(4, defaultNodes)
        }
    }

    // --- Guest Operations ---
    fun getGuestForRoom(roomId: String): Flow<Guest?> = guestDao.getGuestByRoomId(roomId)

    suspend fun triggerSos(
        roomId: String,
        floor: Int,
        message: String,
        statusChips: List<String> = emptyList()
    ) {
        val iso = nowIso()
        val piiResult = PiiScrubber.scrub(message)
        val cleanMessage = if (piiResult.scrubbedText.isBlank()) "SOS Distress Flagged" else piiResult.scrubbedText

        val guest = Guest(
            id = roomId,
            roomId = roomId,
            floor = floor,
            status = "needs_help",
            updatedAt = iso
        )
        guestDao.insertOrUpdate(guest)

        val chipsCsv = statusChips.joinToString(",")
        val emergency = ActiveEmergency(
            id = "room_$roomId",
            roomId = roomId,
            floor = floor,
            status = "trapped",
            statusChipsCsv = chipsCsv,
            lastUpdated = iso,
            occupantCount = 1,
            signalStrength = "high",
            sosText = cleanMessage
        )
        activeEmergencyDao.insertOrUpdate(emergency)
        FirebaseRemoteManager.updateMemoryEmergency(emergency)

        // Check deduplication against existing reports
        val currentReports = sosReportDao.getAllSosReports().first()
        val isDup = currentReports.any { prev ->
            ReportDeduplicator.isDuplicate(
                reportAText = prev.message,
                reportBText = cleanMessage,
                roomA = prev.roomId,
                roomB = roomId,
                floorA = prev.floor,
                floorB = floor
            )
        }

        val reportId = UUID.randomUUID().toString()
        val report = SosReport(
            id = reportId,
            roomId = roomId,
            floor = floor,
            message = cleanMessage,
            status = if (isDup) "clustered" else "new",
            createdAt = iso,
            updatedAt = iso
        )
        sosReportDao.insertReport(report)

        // Synthesize incident cluster
        val (catKey, catLabel) = HazardClusterer.classifyCategory(cleanMessage)
        val incident = Incident(
            id = "inc_$roomId",
            title = "$catLabel in Room $roomId (Floor $floor)",
            summary = cleanMessage,
            location = "Floor $floor, Room $roomId",
            severity = if (cleanMessage.contains("smoke", ignoreCase = true) || cleanMessage.contains("trapped", ignoreCase = true)) "critical" else "high",
            status = "new",
            trapped = 1,
            sourceReportIds = reportId,
            createdAt = iso,
            updatedAt = iso,
            roomId = roomId
        )
        incidentDao.insertOrUpdate(incident)
        FirebaseRemoteManager.addMemoryIncident(incident)

        // Add welcome message to chat stream
        val systemMsg = ChatMessage(
            id = UUID.randomUUID().toString(),
            roomId = roomId,
            senderRole = "system",
            text = "SOS distress received. Responders have been alerted. Stay in your room and seal doors with damp towels.",
            timestamp = System.currentTimeMillis()
        )
        FirebaseRemoteManager.addChatMessage(systemMsg)
    }

    suspend fun triggerSafe(roomId: String, floor: Int) {
        val iso = nowIso()
        val guest = Guest(
            id = roomId,
            roomId = roomId,
            floor = floor,
            status = "safe",
            updatedAt = iso
        )
        guestDao.insertOrUpdate(guest)

        val emergency = ActiveEmergency(
            id = "room_$roomId",
            roomId = roomId,
            floor = floor,
            status = "evacuated",
            statusChipsCsv = "",
            lastUpdated = iso,
            occupantCount = 0,
            signalStrength = "high",
            sosText = "Occupant Evacuated / Safe"
        )
        activeEmergencyDao.insertOrUpdate(emergency)
        FirebaseRemoteManager.updateMemoryEmergency(emergency)

        incidentDao.updateStatusByRoom(roomId, "resolved")
        FirebaseRemoteManager.updateMemoryIncidentStatus(roomId, "resolved")

        val safeMsg = ChatMessage(
            id = UUID.randomUUID().toString(),
            roomId = roomId,
            senderRole = "system",
            text = "Room $roomId status updated to EVACUATED / SAFE. Stay at designated assembly area.",
            timestamp = System.currentTimeMillis()
        )
        FirebaseRemoteManager.addChatMessage(safeMsg)
    }

    suspend fun attachMedia(roomId: String, floor: Int, mediaUrl: String, mediaType: String) {
        val iso = nowIso()
        val incident = Incident(
            id = "inc_media_${UUID.randomUUID()}",
            title = "$mediaType Submission from Room $roomId",
            summary = "Multimodal $mediaType submitted by guest in Room $roomId.",
            location = "Floor $floor, Room $roomId",
            severity = "high",
            status = "new",
            trapped = 1,
            sourceReportIds = "",
            createdAt = iso,
            updatedAt = iso,
            roomId = roomId,
            mediaUrl = mediaUrl,
            mediaType = mediaType
        )
        incidentDao.insertOrUpdate(incident)
        FirebaseRemoteManager.addMemoryIncident(incident)
    }

    // --- Chat & Translation ---
    fun getChatMessagesForRoom(roomId: String): Flow<List<ChatMessage>> {
        return combine(FirebaseRemoteManager.memoryChatMessages) { msgs ->
            msgs.first().filter { it.roomId == roomId }
        }
    }

    suspend fun sendChatMessage(
        roomId: String,
        senderRole: String,
        text: String,
        targetLang: String = "Spanish"
    ) {
        val scrubbed = PiiScrubber.scrub(text).scrubbedText
        val translated = if (targetLang.isNotBlank() && targetLang != "English") {
            GeminiService.translateText(scrubbed, targetLang)
        } else null

        val msg = ChatMessage(
            id = UUID.randomUUID().toString(),
            roomId = roomId,
            senderRole = senderRole,
            text = scrubbed,
            translatedText = translated,
            timestamp = System.currentTimeMillis()
        )
        FirebaseRemoteManager.addChatMessage(msg)
    }

    // --- Responder Operations ---
    fun getAllActiveEmergencies(): Flow<List<ActiveEmergency>> {
        return activeEmergencyDao.getAllActiveEmergencies()
    }

    suspend fun updateTriageStatus(emergencyId: String, roomId: String, floor: Int, status: String) {
        val iso = nowIso()
        val updated = ActiveEmergency(
            id = emergencyId,
            roomId = roomId,
            floor = floor,
            status = status, // "evacuated", "trapped", "checking"
            lastUpdated = iso,
            occupantCount = if (status == "evacuated") 0 else 1,
            signalStrength = "high",
            sosText = "Status set to $status by Responder"
        )
        activeEmergencyDao.insertOrUpdate(updated)
        FirebaseRemoteManager.updateMemoryEmergency(updated)

        if (status == "evacuated") {
            triggerSafe(roomId, floor)
        }
    }

    // --- Staff Operations ---
    fun getAllBroadcasts(): Flow<List<Broadcast>> = broadcastDao.getRecentBroadcasts()
    fun getActiveDangerZones(): Flow<List<DangerZone>> = dangerZoneDao.getActiveDangerZones()
    fun getAllIncidents(): Flow<List<Incident>> = incidentDao.getAllIncidents()
    fun getFloorNodes(floor: Int): Flow<List<FloorNode>> = floorNodeDao.getNodesForFloor(floor)
    fun getFloorPlan(floor: Int): Flow<FloorPlan?> = floorPlanDao.getFloorPlan(floor)

    suspend fun publishBroadcast(message: String, priority: String, target: String) {
        val iso = nowIso()
        val bc = Broadcast(
            id = "bc_${UUID.randomUUID()}",
            message = message,
            priority = priority,
            target = target,
            delivery = "sent",
            createdAt = iso
        )
        broadcastDao.insertBroadcast(bc)
        FirebaseRemoteManager.addMemoryBroadcast(bc)
    }

    suspend fun createDangerZone(floor: Int, label: String, severity: String, radius: Double) {
        val iso = nowIso()
        val dz = DangerZone(
            id = "dz_${UUID.randomUUID()}",
            floor = floor,
            label = label,
            severity = severity,
            lat = 37.7749 + (floor * 0.0001),
            lng = -122.4194 + (floor * 0.0001),
            radiusMeters = radius,
            active = true,
            updatedAt = iso
        )
        dangerZoneDao.insertOrUpdate(dz)
        FirebaseRemoteManager.addMemoryDangerZone(dz)
    }

    suspend fun saveFloorPlan(floor: Int, url: String) {
        val fp = FloorPlan(floor, url)
        floorPlanDao.insertFloorPlan(fp)
        FirebaseRemoteManager.setFloorPlanUrl(floor, url)
    }

    suspend fun saveFloorNodes(floor: Int, nodes: List<FloorNode>) {
        floorNodeDao.clearFloorNodes(floor)
        floorNodeDao.insertNodes(nodes)
        FirebaseRemoteManager.setFloorNodes(floor, nodes)
    }

    suspend fun addFloorNode(node: FloorNode) {
        floorNodeDao.insertNode(node)
        val current = floorNodeDao.getNodesForFloor(node.floor).first()
        FirebaseRemoteManager.setFloorNodes(node.floor, current)
    }

    suspend fun deleteFloorNode(id: String, floor: Int) {
        floorNodeDao.deleteNodeById(id)
        val current = floorNodeDao.getNodesForFloor(floor).first()
        FirebaseRemoteManager.setFloorNodes(floor, current)
    }

    suspend fun generateSitRep(): List<String> {
        val activeEm = activeEmergencyDao.getAllActiveEmergencies().first()
        val incidents = incidentDao.getAllIncidents().first()

        val total = 24
        val trapped = activeEm.count { it.status == "trapped" }
        val evacuated = activeEm.count { it.status == "evacuated" }
        val summaryText = incidents.take(5).joinToString("; ") { "${it.title}: ${it.summary}" }

        return GeminiService.generateSituationReport(
            totalRooms = total,
            evacuatedCount = evacuated,
            trappedCount = trapped,
            incidentsSummary = if (summaryText.isBlank()) "No critical incident logs reported." else summaryText
        )
    }
}
