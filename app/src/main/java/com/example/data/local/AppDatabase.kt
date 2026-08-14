package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.model.ActiveEmergency
import com.example.data.model.Broadcast
import com.example.data.model.DangerZone
import com.example.data.model.FloorNode
import com.example.data.model.FloorPlan
import com.example.data.model.Guest
import com.example.data.model.Incident
import com.example.data.model.RoomEntry
import com.example.data.model.SosReport

@Database(
    entities = [
        RoomEntry::class,
        Guest::class,
        SosReport::class,
        Incident::class,
        DangerZone::class,
        Broadcast::class,
        ActiveEmergency::class,
        FloorNode::class,
        FloorPlan::class
    ],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun roomEntryDao(): RoomEntryDao
    abstract fun guestDao(): GuestDao
    abstract fun sosReportDao(): SosReportDao
    abstract fun incidentDao(): IncidentDao
    abstract fun dangerZoneDao(): DangerZoneDao
    abstract fun broadcastDao(): BroadcastDao
    abstract fun activeEmergencyDao(): ActiveEmergencyDao
    abstract fun floorNodeDao(): FloorNodeDao
    abstract fun floorPlanDao(): FloorPlanDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "guardianlink_db"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}
