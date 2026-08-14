package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AltRoute
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.DangerZone
import com.example.data.model.FloorNode
import com.example.ui.theme.CrisisRed
import com.example.ui.theme.DarkCanvas
import com.example.ui.theme.SafeGreen
import com.example.ui.theme.SurfaceCard
import com.example.ui.theme.TacticalCyan
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.WarningAmber
import com.example.util.CalculatedEscapeRoute
import com.example.util.FloorRoutingEngine
import kotlin.math.hypot

@Composable
fun TacticalEvacuationMap(
    roomId: String,
    floor: Int = 4,
    nodes: List<FloorNode> = emptyList(),
    dangerZones: List<DangerZone> = emptyList(),
    floorPlanUrl: String? = null,
    onMapTap: ((Offset, Double, Double) -> Unit)? = null,
    onMapDoubleTap: ((Offset) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val roomNum = roomId.toIntOrNull() ?: 402

    // Dynamically calculate the escape route around any active fire/smoke danger zones
    val calculatedRoute: CalculatedEscapeRoute = remember(roomId, floor, dangerZones, nodes) {
        FloorRoutingEngine.calculateEscapeRoute(
            roomId = roomId,
            floor = floor,
            dangerZones = dangerZones,
            customNodes = nodes
        )
    }

    val infiniteTransition = rememberInfiniteTransition(label = "map_animations")
    val haloRadiusFraction by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 1.4f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "halo_radius"
    )

    val hazardPulse by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "hazard_pulse"
    )

    val pathDashPhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 40f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "path_dash"
    )

    val textMeasurer = rememberTextMeasurer()

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("tactical_evacuation_map")
            .clip(RoundedCornerShape(16.dp))
            .border(1.dp, if (calculatedRoute.isRerouted) WarningAmber.copy(alpha = 0.8f) else TacticalCyan.copy(alpha = 0.4f), RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(containerColor = SurfaceCard)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // Header Row with Dynamic Route Status Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Map,
                        contentDescription = null,
                        tint = if (calculatedRoute.isRerouted) WarningAmber else TacticalCyan,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "TACTICAL FLOOR MAP • LEVEL 0$floor",
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (calculatedRoute.isRerouted) WarningAmber else TacticalCyan
                    )
                }

                if (calculatedRoute.isRerouted) {
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(WarningAmber.copy(alpha = 0.2f))
                            .border(1.dp, WarningAmber.copy(alpha = 0.6f), RoundedCornerShape(6.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.AltRoute,
                            contentDescription = null,
                            tint = WarningAmber,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = "REROUTED AROUND HAZARD",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Black,
                            color = WarningAmber
                        )
                    }
                } else {
                    Text(
                        text = "CRS 900x1000",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Canvas Container
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(270.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(DarkCanvas)
            ) {
                if (!floorPlanUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = floorPlanUrl,
                        contentDescription = "Custom Floor Plan Map",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                        alpha = 0.5f
                    )
                }

                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(Unit) {
                            detectTapGestures(
                                onTap = { offset ->
                                    val crsX = ((offset.x / size.width) * 900.0)
                                    val crsY = ((1.0 - (offset.y / size.height)) * 1000.0)
                                    onMapTap?.invoke(offset, crsX, crsY)
                                },
                                onDoubleTap = { offset ->
                                    onMapDoubleTap?.invoke(offset)
                                }
                            )
                        }
                ) {
                    val w = size.width
                    val h = size.height

                    // Map coordinate scaling: CRS 900x1000 to Canvas Size
                    fun scaleX(x: Double): Float = ((x / 900.0) * w).toFloat()
                    fun scaleY(y: Double): Float = ((1.0 - (y / 1000.0)) * h).toFloat()

                    // 1. Grid Background Lines
                    val gridSteps = 6
                    for (i in 0..gridSteps) {
                        val gx = (w / gridSteps) * i
                        val gy = (h / gridSteps) * i
                        drawLine(
                            color = TacticalCyan.copy(alpha = 0.07f),
                            start = Offset(gx, 0f),
                            end = Offset(gx, h),
                            strokeWidth = 1f
                        )
                        drawLine(
                            color = TacticalCyan.copy(alpha = 0.07f),
                            start = Offset(0f, gy),
                            end = Offset(w, gy),
                            strokeWidth = 1f
                        )
                    }

                    // 2. Corridors (Main Hallway, North Bypass, South Bypass, Vertical Crossways)
                    val mainCorridorY = scaleY(450.0)
                    val northBypassY = scaleY(620.0)
                    val southBypassY = scaleY(280.0)
                    val corridorHeight = 28f

                    // North & South Bypass Corridors
                    drawRect(
                        color = Color(0xFF0F172A),
                        topLeft = Offset(scaleX(80.0), northBypassY - 10f),
                        size = Size(scaleX(820.0) - scaleX(80.0), 20f)
                    )
                    drawRect(
                        color = Color(0xFF0F172A),
                        topLeft = Offset(scaleX(80.0), southBypassY - 10f),
                        size = Size(scaleX(820.0) - scaleX(80.0), 20f)
                    )

                    // Vertical Connector Corridors
                    listOf(100.0, 340.0, 560.0, 800.0).forEach { crossX ->
                        val cx = scaleX(crossX)
                        drawRect(
                            color = Color(0xFF1E293B),
                            topLeft = Offset(cx - 8f, northBypassY),
                            size = Size(16f, southBypassY - northBypassY)
                        )
                    }

                    // Central Main Corridor
                    drawRect(
                        color = Color(0xFF1E293B),
                        topLeft = Offset(scaleX(70.0), mainCorridorY - corridorHeight / 2),
                        size = Size(scaleX(830.0) - scaleX(70.0), corridorHeight)
                    )
                    drawRect(
                        color = TacticalCyan.copy(alpha = 0.25f),
                        topLeft = Offset(scaleX(70.0), mainCorridorY - corridorHeight / 2),
                        size = Size(scaleX(830.0) - scaleX(70.0), corridorHeight),
                        style = Stroke(width = 1.5f)
                    )

                    // 3. Draw Room Boxes (Rooms 401..406 on North, 407..412 on South)
                    val roomWidth = (w - 90f) / 6f
                    for (i in 0 until 6) {
                        val roomNumNorth = floor * 100 + i + 1
                        val rx = 45f + i * roomWidth

                        // North Room
                        val isUserNorth = roomNumNorth == roomNum
                        drawRoundRect(
                            color = if (isUserNorth) CrisisRed.copy(alpha = 0.25f) else Color(0xFF131C2E),
                            topLeft = Offset(rx + 3f, mainCorridorY - 68f),
                            size = Size(roomWidth - 6f, 44f),
                            cornerRadius = CornerRadius(5f, 5f)
                        )
                        drawRoundRect(
                            color = if (isUserNorth) CrisisRed else TacticalCyan.copy(alpha = 0.35f),
                            topLeft = Offset(rx + 3f, mainCorridorY - 68f),
                            size = Size(roomWidth - 6f, 44f),
                            cornerRadius = CornerRadius(5f, 5f),
                            style = Stroke(width = if (isUserNorth) 2f else 1f)
                        )

                        // South Room
                        val roomNumSouth = floor * 100 + i + 7
                        val isUserSouth = roomNumSouth == roomNum
                        drawRoundRect(
                            color = if (isUserSouth) CrisisRed.copy(alpha = 0.25f) else Color(0xFF131C2E),
                            topLeft = Offset(rx + 3f, mainCorridorY + 24f),
                            size = Size(roomWidth - 6f, 44f),
                            cornerRadius = CornerRadius(5f, 5f)
                        )
                        drawRoundRect(
                            color = if (isUserSouth) CrisisRed else TacticalCyan.copy(alpha = 0.35f),
                            topLeft = Offset(rx + 3f, mainCorridorY + 24f),
                            size = Size(roomWidth - 6f, 44f),
                            cornerRadius = CornerRadius(5f, 5f),
                            style = Stroke(width = if (isUserSouth) 2f else 1f)
                        )
                    }

                    // 4. Exit Portals: Exit A (West) and Exit B (East)
                    val exitAX = scaleX(80.0)
                    val exitBX = scaleX(820.0)

                    // Check if exits are blocked by hazards
                    val activeHazards = dangerZones.filter { it.floor == floor && it.active }
                    val isExitABlocked = activeHazards.any { hypot(it.crsX - 80.0, it.crsY - 450.0) <= it.radiusMeters + 15.0 }
                    val isExitBBlocked = activeHazards.any { hypot(it.crsX - 820.0, it.crsY - 450.0) <= it.radiusMeters + 15.0 }

                    // Draw Exit A (West Stairwell)
                    val colorExitA = if (isExitABlocked) CrisisRed else SafeGreen
                    drawRoundRect(
                        color = colorExitA.copy(alpha = 0.3f),
                        topLeft = Offset(exitAX - 20f, mainCorridorY - 20f),
                        size = Size(40f, 40f),
                        cornerRadius = CornerRadius(6f, 6f)
                    )
                    drawRoundRect(
                        color = colorExitA,
                        topLeft = Offset(exitAX - 20f, mainCorridorY - 20f),
                        size = Size(40f, 40f),
                        cornerRadius = CornerRadius(6f, 6f),
                        style = Stroke(width = 2f)
                    )

                    // Draw Exit B (East Stairwell)
                    val colorExitB = if (isExitBBlocked) CrisisRed else SafeGreen
                    drawRoundRect(
                        color = colorExitB.copy(alpha = 0.3f),
                        topLeft = Offset(exitBX - 20f, mainCorridorY - 20f),
                        size = Size(40f, 40f),
                        cornerRadius = CornerRadius(6f, 6f)
                    )
                    drawRoundRect(
                        color = colorExitB,
                        topLeft = Offset(exitBX - 20f, mainCorridorY - 20f),
                        size = Size(40f, 40f),
                        cornerRadius = CornerRadius(6f, 6f),
                        style = Stroke(width = 2f)
                    )

                    // 5. Render Active Danger / Fire / Smoke Zones
                    activeHazards.forEach { hazard ->
                        val hx = scaleX(hazard.crsX)
                        val hy = scaleY(hazard.crsY)
                        val r = (scaleX(hazard.radiusMeters) - scaleX(0.0)).coerceAtLeast(30f) * hazardPulse
                        val hazardColor = if (hazard.hazardType == "smoke") WarningAmber else CrisisRed

                        // Outer pulsing danger ring
                        drawCircle(
                            color = hazardColor.copy(alpha = 0.22f),
                            center = Offset(hx, hy),
                            radius = r
                        )
                        drawCircle(
                            color = hazardColor.copy(alpha = 0.7f),
                            center = Offset(hx, hy),
                            radius = r,
                            style = Stroke(
                                width = 2.5f,
                                pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 8f))
                            )
                        )

                        // Core flame/smoke center
                        drawCircle(
                            color = hazardColor,
                            center = Offset(hx, hy),
                            radius = 12f
                        )
                        drawCircle(
                            color = Color.White,
                            center = Offset(hx, hy),
                            radius = 5f
                        )
                    }

                    // 6. Draw Plotted Walkable / Portal Nodes
                    nodes.forEach { node ->
                        val nx = scaleX(node.x)
                        val ny = scaleY(node.y)
                        val nodeColor = if (node.nodeType == "portal") WarningAmber else TacticalCyan
                        drawCircle(color = nodeColor, center = Offset(nx, ny), radius = 6f)
                        drawCircle(color = Color.White, center = Offset(nx, ny), radius = 2f)
                    }

                    // 7. Draw Dynamic Calculated Escape Polyline (Recalculated around Fire!)
                    val waypoints = calculatedRoute.waypoints
                    if (waypoints.size >= 2) {
                        val escapePath = Path()
                        val startX = scaleX(waypoints[0].x)
                        val startY = scaleY(waypoints[0].y)
                        escapePath.moveTo(startX, startY)

                        for (i in 1 until waypoints.size) {
                            val px = scaleX(waypoints[i].x)
                            val py = scaleY(waypoints[i].y)
                            escapePath.lineTo(px, py)
                        }

                        // Background Glow
                        drawPath(
                            path = escapePath,
                            color = SafeGreen.copy(alpha = 0.3f),
                            style = Stroke(width = 8f)
                        )

                        // Animated Dashed Route
                        drawPath(
                            path = escapePath,
                            color = SafeGreen,
                            style = Stroke(
                                width = 4f,
                                pathEffect = PathEffect.dashPathEffect(
                                    intervals = floatArrayOf(14f, 10f),
                                    phase = pathDashPhase
                                )
                            )
                        )

                        // Intermediate turn waypoints
                        for (i in 1 until waypoints.size - 1) {
                            val wx = scaleX(waypoints[i].x)
                            val wy = scaleY(waypoints[i].y)
                            drawCircle(color = SafeGreen, center = Offset(wx, wy), radius = 6f)
                            drawCircle(color = Color.Black, center = Offset(wx, wy), radius = 3f)
                        }
                    }

                    // 8. User Current Position (Pulsing Halo)
                    val (roomInterior, _) = FloorRoutingEngine.getRoomCoordinate(roomId, floor)
                    val userX = scaleX(roomInterior.x)
                    val userY = scaleY(roomInterior.y)
                    val haloRadius = 18f * haloRadiusFraction

                    drawCircle(
                        color = CrisisRed.copy(alpha = 0.35f),
                        center = Offset(userX, userY),
                        radius = haloRadius
                    )
                    drawCircle(
                        color = CrisisRed,
                        center = Offset(userX, userY),
                        radius = 8f
                    )
                    drawCircle(
                        color = Color.White,
                        center = Offset(userX, userY),
                        radius = 3f
                    )
                }

                // Legend & Route Summary Footer Badge
                Row(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(8.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(SurfaceCard.copy(alpha = 0.92f))
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(modifier = Modifier.size(8.dp).clip(RoundedCornerShape(2.dp)).background(CrisisRed))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("YOU (RM $roomId)", fontSize = 9.sp, color = TextPrimary, fontWeight = FontWeight.Bold)

                    Spacer(modifier = Modifier.width(6.dp))
                    Box(modifier = Modifier.size(8.dp).clip(RoundedCornerShape(2.dp)).background(SafeGreen))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(if (calculatedRoute.targetExitName.contains("A")) "EXIT A" else "EXIT B", fontSize = 9.sp, color = TextPrimary, fontWeight = FontWeight.Bold)

                    Spacer(modifier = Modifier.width(6.dp))
                    Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(CrisisRed))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("FIRE ZONE", fontSize = 9.sp, color = CrisisRed, fontWeight = FontWeight.Bold)

                    if (calculatedRoute.isRerouted) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("• REROUTED", fontSize = 9.sp, color = WarningAmber, fontWeight = FontWeight.Black)
                    }
                }
            }
        }
    }
}
