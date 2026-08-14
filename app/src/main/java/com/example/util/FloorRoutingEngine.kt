package com.example.util

import com.example.data.model.DangerZone
import com.example.data.model.FloorNode
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

data class RoutePoint(
    val x: Double,
    val y: Double,
    val label: String = ""
)

data class CalculatedEscapeRoute(
    val roomId: String,
    val floor: Int,
    val waypoints: List<RoutePoint>,
    val targetExitName: String,
    val isRerouted: Boolean,
    val activeHazardsOnFloor: List<DangerZone>,
    val obstructingHazards: List<DangerZone>,
    val navigationSteps: List<String>,
    val estimatedDistanceMeters: Double
)

object FloorRoutingEngine {

    // Default exit coordinates on Floor 4
    val EXIT_A_WEST = RoutePoint(80.0, 450.0, "Stairwell A (West Exit)")
    val EXIT_B_EAST = RoutePoint(820.0, 450.0, "Stairwell B (East Exit)")

    /**
     * Computes the Cartesian room door and interior position in CRS (900x1000).
     */
    fun getRoomCoordinate(roomId: String, floor: Int = 4): Pair<RoutePoint, RoutePoint> {
        val roomNum = roomId.toIntOrNull() ?: 402
        val indexOnFloor = (roomNum % 100) - 1 // 0..11
        val isNorth = indexOnFloor < 6
        val col = indexOnFloor % 6
        val roomX = 150.0 + col * 120.0
        val roomInteriorY = if (isNorth) 550.0 else 350.0
        val corridorDoorY = 450.0

        val interior = RoutePoint(roomX, roomInteriorY, "Room $roomId")
        val doorway = RoutePoint(roomX, corridorDoorY, "Doorway $roomId")
        return Pair(interior, doorway)
    }

    /**
     * Main entry point: calculates optimal real-time evacuation path around active fire/smoke zones.
     */
    fun calculateEscapeRoute(
        roomId: String,
        floor: Int = 4,
        dangerZones: List<DangerZone> = emptyList(),
        customNodes: List<FloorNode> = emptyList()
    ): CalculatedEscapeRoute {
        val activeFloorHazards = dangerZones.filter { it.floor == floor && it.active }
        val (roomInterior, roomDoor) = getRoomCoordinate(roomId, floor)

        // 1. Check direct path to closest exit
        val distToWest = kotlin.math.abs(roomDoor.x - EXIT_A_WEST.x)
        val distToEast = kotlin.math.abs(roomDoor.x - EXIT_B_EAST.x)
        val primaryExit = if (distToWest <= distToEast) EXIT_A_WEST else EXIT_B_EAST
        val alternateExit = if (primaryExit == EXIT_A_WEST) EXIT_B_EAST else EXIT_A_WEST

        val directPath = listOf(roomInterior, roomDoor, primaryExit)
        val directObstructing = findObstructingHazards(directPath, activeFloorHazards)

        if (directObstructing.isEmpty()) {
            val totalDist = dist(roomInterior, roomDoor) + dist(roomDoor, primaryExit)
            val steps = listOf(
                "1. Exit Room $roomId into main corridor.",
                "2. Proceed directly towards ${primaryExit.label}.",
                "3. Safe egress clear — follow emergency exit lighting."
            )
            return CalculatedEscapeRoute(
                roomId = roomId,
                floor = floor,
                waypoints = directPath,
                targetExitName = primaryExit.label,
                isRerouted = false,
                activeHazardsOnFloor = activeFloorHazards,
                obstructingHazards = emptyList(),
                navigationSteps = steps,
                estimatedDistanceMeters = (totalDist * 0.08).coerceAtLeast(8.0)
            )
        }

        // 2. Direct path is blocked by Fire/Smoke! Recalculate graph around hazards.
        val graphNodes = buildFloorGraph(activeFloorHazards, customNodes, roomDoor)
        val path = findShortestSafePath(roomDoor, listOf(primaryExit, alternateExit), graphNodes, activeFloorHazards)

        val finalWaypoints = if (path.isNotEmpty()) {
            listOf(roomInterior) + path
        } else {
            // Emergency fallback: route around nearest perimeter bypass
            buildEmergencyBypassRoute(roomInterior, roomDoor, alternateExit, activeFloorHazards)
        }

        val targetExit = if (finalWaypoints.lastOrNull()?.x ?: 0.0 > 500.0) EXIT_B_EAST else EXIT_A_WEST
        val totalDistance = calculateTotalDistance(finalWaypoints)

        val hazardNames = directObstructing.joinToString(", ") { it.label }
        val steps = listOf(
            "1. ⚠️ HAZARD ALERT: $hazardNames detected on default path.",
            "2. Escape path automatically recalculated around hazard zone.",
            "3. Exit Room $roomId and follow designated bypass corridor.",
            "4. Evacuate safely via ${targetExit.label}."
        )

        return CalculatedEscapeRoute(
            roomId = roomId,
            floor = floor,
            waypoints = finalWaypoints,
            targetExitName = targetExit.label,
            isRerouted = true,
            activeHazardsOnFloor = activeFloorHazards,
            obstructingHazards = directObstructing,
            navigationSteps = steps,
            estimatedDistanceMeters = (totalDistance * 0.08).coerceAtLeast(12.0)
        )
    }

    /**
     * Checks whether a polyline path crosses any active hazard circle.
     */
    private fun findObstructingHazards(
        path: List<RoutePoint>,
        hazards: List<DangerZone>
    ): List<DangerZone> {
        val obstructing = mutableListOf<DangerZone>()
        for (hazard in hazards) {
            val hx = hazard.crsX
            val hy = hazard.crsY
            val radius = hazard.radiusMeters.coerceAtLeast(35.0)

            for (i in 0 until path.size - 1) {
                val p1 = path[i]
                val p2 = path[i + 1]
                if (distanceSegmentToPoint(p1.x, p1.y, p2.x, p2.y, hx, hy) <= radius) {
                    obstructing.add(hazard)
                    break
                }
            }
        }
        return obstructing
    }

    /**
     * Builds navigation graph with Main Corridor, North Bypass, South Bypass and cross corridors.
     */
    private fun buildFloorGraph(
        hazards: List<DangerZone>,
        customNodes: List<FloorNode>,
        roomDoor: RoutePoint
    ): Map<RoutePoint, List<RoutePoint>> {
        val nodes = mutableListOf<RoutePoint>()

        // Exits
        nodes.add(EXIT_A_WEST)
        nodes.add(EXIT_B_EAST)

        // Main Corridor Waypoints (Y = 450)
        val xSteps = listOf(80.0, 150.0, 270.0, 390.0, 510.0, 630.0, 750.0, 820.0)
        val mainCorridor = xSteps.map { RoutePoint(it, 450.0, "Corridor X=${it.toInt()}") }
        nodes.addAll(mainCorridor)

        // North Bypass Waypoints (Y = 620)
        val northBypass = xSteps.map { RoutePoint(it, 620.0, "North Bypass X=${it.toInt()}") }
        nodes.addAll(northBypass)

        // South Bypass Waypoints (Y = 280)
        val southBypass = xSteps.map { RoutePoint(it, 280.0, "South Bypass X=${it.toInt()}") }
        nodes.addAll(southBypass)

        // Add Room Doorway
        nodes.add(roomDoor)

        // Add Custom Walkable Nodes
        customNodes.forEach {
            nodes.add(RoutePoint(it.x, it.y, it.label.ifBlank { "Node ${it.id}" }))
        }

        // Build adjacency map based on safe walkable connections
        val adj = mutableMapOf<RoutePoint, MutableList<RoutePoint>>()
        nodes.forEach { adj[it] = mutableListOf() }

        fun tryAddEdge(a: RoutePoint, b: RoutePoint) {
            if (isEdgeSafe(a, b, hazards)) {
                adj[a]?.add(b)
                adj[b]?.add(a)
            }
        }

        // 1. Connect adjacent points on Main Corridor
        for (i in 0 until mainCorridor.size - 1) {
            tryAddEdge(mainCorridor[i], mainCorridor[i + 1])
        }

        // 2. Connect adjacent points on North Bypass
        for (i in 0 until northBypass.size - 1) {
            tryAddEdge(northBypass[i], northBypass[i + 1])
        }

        // 3. Connect adjacent points on South Bypass
        for (i in 0 until southBypass.size - 1) {
            tryAddEdge(southBypass[i], southBypass[i + 1])
        }

        // 4. Connect vertical cross corridors (Main to North Bypass, Main to South Bypass)
        for (i in xSteps.indices) {
            tryAddEdge(mainCorridor[i], northBypass[i])
            tryAddEdge(mainCorridor[i], southBypass[i])
        }

        // 5. Connect room door to closest corridor & bypass points
        val closestMain = mainCorridor.minByOrNull { dist(roomDoor, it) }
        val closestNorth = northBypass.minByOrNull { dist(roomDoor, it) }
        val closestSouth = southBypass.minByOrNull { dist(roomDoor, it) }

        closestMain?.let { tryAddEdge(roomDoor, it) }
        closestNorth?.let { tryAddEdge(roomDoor, it) }
        closestSouth?.let { tryAddEdge(roomDoor, it) }

        // 6. Connect Exits to endpoints
        tryAddEdge(EXIT_A_WEST, mainCorridor.first())
        tryAddEdge(EXIT_A_WEST, northBypass.first())
        tryAddEdge(EXIT_A_WEST, southBypass.first())

        tryAddEdge(EXIT_B_EAST, mainCorridor.last())
        tryAddEdge(EXIT_B_EAST, northBypass.last())
        tryAddEdge(EXIT_B_EAST, southBypass.last())

        return adj
    }

    private fun isEdgeSafe(a: RoutePoint, b: RoutePoint, hazards: List<DangerZone>): Boolean {
        for (hazard in hazards) {
            val hx = hazard.crsX
            val hy = hazard.crsY
            val radius = hazard.radiusMeters.coerceAtLeast(35.0)
            if (distanceSegmentToPoint(a.x, a.y, b.x, b.y, hx, hy) < radius * 0.9) {
                return false
            }
        }
        return true
    }

    /**
     * Dijkstra algorithm to find shortest safe path to any reachable exit.
     */
    private fun findShortestSafePath(
        start: RoutePoint,
        targetExits: List<RoutePoint>,
        graph: Map<RoutePoint, List<RoutePoint>>,
        hazards: List<DangerZone>
    ): List<RoutePoint> {
        val distances = mutableMapOf<RoutePoint, Double>()
        val previous = mutableMapOf<RoutePoint, RoutePoint?>()
        val unvisited = graph.keys.toMutableSet()

        graph.keys.forEach { node ->
            distances[node] = Double.MAX_VALUE
            previous[node] = null
        }
        distances[start] = 0.0

        while (unvisited.isNotEmpty()) {
            val current = unvisited.minByOrNull { distances[it] ?: Double.MAX_VALUE } ?: break
            val currentDist = distances[current] ?: Double.MAX_VALUE
            if (currentDist == Double.MAX_VALUE) break

            unvisited.remove(current)

            // If we reached a target exit
            if (targetExits.contains(current)) {
                // Reconstruct path
                val path = mutableListOf<RoutePoint>()
                var curr: RoutePoint? = current
                while (curr != null) {
                    path.add(0, curr)
                    curr = previous[curr]
                }
                return path
            }

            val neighbors = graph[current] ?: emptyList()
            for (neighbor in neighbors) {
                if (neighbor in unvisited) {
                    val weight = dist(current, neighbor)
                    val newDist = currentDist + weight
                    if (newDist < (distances[neighbor] ?: Double.MAX_VALUE)) {
                        distances[neighbor] = newDist
                        previous[neighbor] = current
                    }
                }
            }
        }

        return emptyList()
    }

    /**
     * Fallback bypass builder when graph is heavily partitioned.
     */
    private fun buildEmergencyBypassRoute(
        roomInterior: RoutePoint,
        roomDoor: RoutePoint,
        fallbackExit: RoutePoint,
        hazards: List<DangerZone>
    ): List<RoutePoint> {
        // Choose North vs South bypass based on hazard locations
        val avgHazardY = hazards.map { it.crsY }.average().takeIf { !it.isNaN() } ?: 450.0
        val bypassY = if (avgHazardY <= 450.0) 620.0 else 280.0

        return listOf(
            roomInterior,
            roomDoor,
            RoutePoint(roomDoor.x, bypassY, "Bypass Entry"),
            RoutePoint(fallbackExit.x, bypassY, "Bypass Corridor"),
            fallbackExit
        )
    }

    fun dist(a: RoutePoint, b: RoutePoint): Double {
        return hypot(a.x - b.x, a.y - b.y)
    }

    private fun calculateTotalDistance(points: List<RoutePoint>): Double {
        var sum = 0.0
        for (i in 0 until points.size - 1) {
            sum += dist(points[i], points[i + 1])
        }
        return sum
    }

    private fun distanceSegmentToPoint(
        x1: Double, y1: Double,
        x2: Double, y2: Double,
        px: Double, py: Double
    ): Double {
        val dx = x2 - x1
        val dy = y2 - y1
        val l2 = dx * dx + dy * dy
        if (l2 == 0.0) return hypot(px - x1, py - y1)

        val t = max(0.0, min(1.0, ((px - x1) * dx + (py - y1) * dy) / l2))
        val projX = x1 + t * dx
        val projY = y1 + t * dy
        return hypot(px - projX, py - projY)
    }
}
