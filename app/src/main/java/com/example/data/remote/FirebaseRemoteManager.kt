package com.example.data.remote

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
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.callbackFlow

object FirebaseRemoteManager {
    private var firestore: FirebaseFirestore? = null

    init {
        try {
            firestore = FirebaseFirestore.getInstance()
        } catch (e: Exception) {
            firestore = null
        }
    }

    // Fallback in-memory state when Firestore SDK is unprovisioned or offline
    private val _memoryRooms = MutableStateFlow<List<RoomEntry>>(emptyList())
    val memoryRooms = _memoryRooms.asStateFlow()

    private val _memoryEmergencies = MutableStateFlow<List<ActiveEmergency>>(emptyList())
    val memoryEmergencies = _memoryEmergencies.asStateFlow()

    private val _memoryBroadcasts = MutableStateFlow<List<Broadcast>>(emptyList())
    val memoryBroadcasts = _memoryBroadcasts.asStateFlow()

    private val _memoryDangerZones = MutableStateFlow<List<DangerZone>>(emptyList())
    val memoryDangerZones = _memoryDangerZones.asStateFlow()

    private val _memoryIncidents = MutableStateFlow<List<Incident>>(emptyList())
    val memoryIncidents = _memoryIncidents.asStateFlow()

    private val _memoryChatMessages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val memoryChatMessages = _memoryChatMessages.asStateFlow()

    private val _memoryFloorNodes = MutableStateFlow<Map<Int, List<FloorNode>>>(emptyMap())
    val memoryFloorNodes = _memoryFloorNodes.asStateFlow()

    private val _memoryFloorPlans = MutableStateFlow<Map<Int, String>>(emptyMap())
    val memoryFloorPlans = _memoryFloorPlans.asStateFlow()

    fun updateMemoryEmergency(emergency: ActiveEmergency) {
        val current = _memoryEmergencies.value.toMutableList()
        current.removeAll { it.id == emergency.id }
        current.add(0, emergency)
        _memoryEmergencies.value = current
    }

    fun removeMemoryEmergency(id: String) {
        val current = _memoryEmergencies.value.toMutableList()
        current.removeAll { it.id == id }
        _memoryEmergencies.value = current
    }

    fun addMemoryBroadcast(broadcast: Broadcast) {
        val current = _memoryBroadcasts.value.toMutableList()
        current.add(0, broadcast)
        _memoryBroadcasts.value = current
    }

    fun addMemoryDangerZone(zone: DangerZone) {
        val current = _memoryDangerZones.value.toMutableList()
        current.removeAll { it.id == zone.id }
        current.add(0, zone)
        _memoryDangerZones.value = current
    }

    fun removeMemoryDangerZone(id: String) {
        val current = _memoryDangerZones.value.toMutableList()
        current.removeAll { it.id == id }
        _memoryDangerZones.value = current
    }

    fun clearMemoryDangerZones(floor: Int) {
        val current = _memoryDangerZones.value.toMutableList()
        current.removeAll { it.floor == floor }
        _memoryDangerZones.value = current
    }

    fun addMemoryIncident(incident: Incident) {
        val current = _memoryIncidents.value.toMutableList()
        current.add(0, incident)
        _memoryIncidents.value = current
    }

    fun updateMemoryIncidentStatus(roomId: String, status: String) {
        val current = _memoryIncidents.value.map {
            if (it.roomId == roomId) it.copy(status = status) else it
        }
        _memoryIncidents.value = current
    }

    fun addChatMessage(msg: ChatMessage) {
        val current = _memoryChatMessages.value.toMutableList()
        current.add(msg)
        _memoryChatMessages.value = current
    }

    fun setFloorNodes(floor: Int, nodes: List<FloorNode>) {
        val current = _memoryFloorNodes.value.toMutableMap()
        current[floor] = nodes
        _memoryFloorNodes.value = current
    }

    fun setFloorPlanUrl(floor: Int, url: String) {
        val current = _memoryFloorPlans.value.toMutableMap()
        current[floor] = url
        _memoryFloorPlans.value = current
    }
}
