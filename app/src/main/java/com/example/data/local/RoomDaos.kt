package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.ActiveEmergency
import com.example.data.model.Broadcast
import com.example.data.model.DangerZone
import com.example.data.model.FloorNode
import com.example.data.model.FloorPlan
import com.example.data.model.Guest
import com.example.data.model.Incident
import com.example.data.model.RoomEntry
import com.example.data.model.SosReport
import kotlinx.coroutines.flow.Flow

@Dao
interface RoomEntryDao {
    @Query("SELECT * FROM rooms ORDER BY roomId ASC")
    fun getAllRooms(): Flow<List<RoomEntry>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(room: RoomEntry)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(rooms: List<RoomEntry>)

    @Query("UPDATE rooms SET status = :status, updatedAt = :updatedAt WHERE id = :id")
    suspend fun updateStatus(id: String, status: String, updatedAt: String)
}

@Dao
interface GuestDao {
    @Query("SELECT * FROM guests WHERE roomId = :roomId")
    fun getGuestByRoomId(roomId: String): Flow<Guest?>

    @Query("SELECT * FROM guests")
    fun getAllGuests(): Flow<List<Guest>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(guest: Guest)
}

@Dao
interface SosReportDao {
    @Query("SELECT * FROM sos_reports ORDER BY createdAt DESC")
    fun getAllSosReports(): Flow<List<SosReport>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReport(report: SosReport)
}

@Dao
interface IncidentDao {
    @Query("SELECT * FROM incidents ORDER BY createdAt DESC")
    fun getAllIncidents(): Flow<List<Incident>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(incident: Incident)

    @Query("UPDATE incidents SET status = :status WHERE roomId = :roomId")
    suspend fun updateStatusByRoom(roomId: String, status: String)
}

@Dao
interface DangerZoneDao {
    @Query("SELECT * FROM danger_zones WHERE active = 1")
    fun getActiveDangerZones(): Flow<List<DangerZone>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(zone: DangerZone)
}

@Dao
interface BroadcastDao {
    @Query("SELECT * FROM broadcasts ORDER BY createdAt DESC LIMIT 12")
    fun getRecentBroadcasts(): Flow<List<Broadcast>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBroadcast(broadcast: Broadcast)
}

@Dao
interface ActiveEmergencyDao {
    @Query("SELECT * FROM active_emergencies ORDER BY lastUpdated DESC")
    fun getAllActiveEmergencies(): Flow<List<ActiveEmergency>>

    @Query("SELECT * FROM active_emergencies WHERE id = :id")
    fun getById(id: String): Flow<ActiveEmergency?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(emergency: ActiveEmergency)

    @Query("DELETE FROM active_emergencies WHERE id = :id")
    suspend fun deleteById(id: String)
}

@Dao
interface FloorNodeDao {
    @Query("SELECT * FROM floor_nodes WHERE floor = :floor")
    fun getNodesForFloor(floor: Int): Flow<List<FloorNode>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNodes(nodes: List<FloorNode>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNode(node: FloorNode)

    @Query("DELETE FROM floor_nodes WHERE id = :id")
    suspend fun deleteNodeById(id: String)

    @Query("DELETE FROM floor_nodes WHERE floor = :floor")
    suspend fun clearFloorNodes(floor: Int)
}

@Dao
interface FloorPlanDao {
    @Query("SELECT * FROM floor_plans WHERE floor = :floor")
    fun getFloorPlan(floor: Int): Flow<FloorPlan?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFloorPlan(plan: FloorPlan)
}
