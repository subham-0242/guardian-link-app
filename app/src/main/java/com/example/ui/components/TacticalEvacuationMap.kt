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
import com.example.ui.theme.TacticalCyanLight
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

            // Canvas Container with High-Contrast Tactical Blueprint Styling
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(320.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF0E1726)) // Deep rich navy blueprint canvas
                    .border(1.5.dp, Color(0xFF1E3A5F), RoundedCornerShape(12.dp))
            ) {
                if (!floorPlanUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = floorPlanUrl,
                        contentDescription = "Custom Floor Plan Map",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                        alpha = 0.4f
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

                    // 1. Blueprint Grid Lines (High Contrast Clean Grid)
                    val gridCols = 8
                    val gridRows = 6
                    for (i in 0..gridCols) {
                        val gx = (w / gridCols) * i
                        drawLine(
                            color = Color(0xFF1A3050).copy(alpha = 0.5f),
                            start = Offset(gx, 0f),
                            end = Offset(gx, h),
                            strokeWidth = 1f
                        )
                    }
                    for (i in 0..gridRows) {
                        val gy = (h / gridRows) * i
                        drawLine(
                            color = Color(0xFF1A3050).copy(alpha = 0.5f),
                            start = Offset(0f, gy),
                            end = Offset(w, gy),
                            strokeWidth = 1f
                        )
                    }

                    // 2. Corridors & Walkable Hallways (High Visibility Walkways)
                    val mainCorridorY = scaleY(450.0)
                    val northBypassY = scaleY(620.0)
                    val southBypassY = scaleY(280.0)
                    val mainCorridorHeight = 36f
                    val bypassHeight = 24f

                    val corridorFillColor = Color(0xFF1B2C46) // Bright readable hallway fill
                    val corridorBorderColor = Color(0xFF38BDF8).copy(alpha = 0.6f) // Cyan border
                    val bypassFillColor = Color(0xFF142338)
                    val bypassBorderColor = Color(0xFF2563EB).copy(alpha = 0.5f)

                    // North & South Bypass Corridors
                    val bypassStartX = scaleX(80.0)
                    val bypassEndX = scaleX(820.0)
                    val bypassWidth = bypassEndX - bypassStartX

                    // North Bypass
                    drawRoundRect(
                        color = bypassFillColor,
                        topLeft = Offset(bypassStartX, northBypassY - bypassHeight / 2),
                        size = Size(bypassWidth, bypassHeight),
                        cornerRadius = CornerRadius(4f, 4f)
                    )
                    drawRoundRect(
                        color = bypassBorderColor,
                        topLeft = Offset(bypassStartX, northBypassY - bypassHeight / 2),
                        size = Size(bypassWidth, bypassHeight),
                        cornerRadius = CornerRadius(4f, 4f),
                        style = Stroke(width = 1.2f)
                    )

                    // South Bypass
                    drawRoundRect(
                        color = bypassFillColor,
                        topLeft = Offset(bypassStartX, southBypassY - bypassHeight / 2),
                        size = Size(bypassWidth, bypassHeight),
                        cornerRadius = CornerRadius(4f, 4f)
                    )
                    drawRoundRect(
                        color = bypassBorderColor,
                        topLeft = Offset(bypassStartX, southBypassY - bypassHeight / 2),
                        size = Size(bypassWidth, bypassHeight),
                        cornerRadius = CornerRadius(4f, 4f),
                        style = Stroke(width = 1.2f)
                    )

                    // Vertical Connecting Walkways (Crossways)
                    listOf(90.0, 330.0, 570.0, 810.0).forEach { crossX ->
                        val cx = scaleX(crossX)
                        val vertTop = northBypassY
                        val vertHeight = southBypassY - northBypassY
                        drawRect(
                            color = bypassFillColor,
                            topLeft = Offset(cx - 10f, vertTop),
                            size = Size(20f, vertHeight)
                        )
                        drawRect(
                            color = bypassBorderColor,
                            topLeft = Offset(cx - 10f, vertTop),
                            size = Size(20f, vertHeight),
                            style = Stroke(width = 1f)
                        )
                    }

                    // Central Main Corridor (Highlight Walkway)
                    val mainStartX = scaleX(70.0)
                    val mainEndX = scaleX(830.0)
                    val mainWidth = mainEndX - mainStartX
                    drawRoundRect(
                        color = corridorFillColor,
                        topLeft = Offset(mainStartX, mainCorridorY - mainCorridorHeight / 2),
                        size = Size(mainWidth, mainCorridorHeight),
                        cornerRadius = CornerRadius(6f, 6f)
                    )
                    drawRoundRect(
                        color = corridorBorderColor,
                        topLeft = Offset(mainStartX, mainCorridorY - mainCorridorHeight / 2),
                        size = Size(mainWidth, mainCorridorHeight),
                        cornerRadius = CornerRadius(6f, 6f),
                        style = Stroke(width = 1.8f)
                    )

                    // Corridor Ambient Center Walkway Line
                    drawLine(
                        color = TacticalCyan.copy(alpha = 0.3f),
                        start = Offset(mainStartX + 10f, mainCorridorY),
                        end = Offset(mainEndX - 10f, mainCorridorY),
                        strokeWidth = 1.5f,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f))
                    )

                    // Corridor Text Labels
                    val mainCorridorLayout = textMeasurer.measure(
                        text = "MAIN CORRIDOR",
                        style = TextStyle(
                            color = TacticalCyanLight.copy(alpha = 0.85f),
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                    drawText(
                        textLayoutResult = mainCorridorLayout,
                        topLeft = Offset(w / 2f - mainCorridorLayout.size.width / 2f, mainCorridorY - mainCorridorLayout.size.height / 2f)
                    )

                    val northBypassLayout = textMeasurer.measure(
                        text = "NORTH BYPASS",
                        style = TextStyle(
                            color = TextSecondary.copy(alpha = 0.8f),
                            fontSize = 7.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                    drawText(
                        textLayoutResult = northBypassLayout,
                        topLeft = Offset(w / 2f - northBypassLayout.size.width / 2f, northBypassY - northBypassLayout.size.height / 2f)
                    )

                    val southBypassLayout = textMeasurer.measure(
                        text = "SOUTH BYPASS",
                        style = TextStyle(
                            color = TextSecondary.copy(alpha = 0.8f),
                            fontSize = 7.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                    drawText(
                        textLayoutResult = southBypassLayout,
                        topLeft = Offset(w / 2f - southBypassLayout.size.width / 2f, southBypassY - southBypassLayout.size.height / 2f)
                    )

                    // 3. Draw Room Blocks (North: 401..406, South: 407..412) with Clear High-Contrast Visibility
                    val roomWidth = (w - 110f) / 6f
                    val roomHeight = 52f

                    for (i in 0 until 6) {
                        val rx = 55f + i * roomWidth

                        // --- NORTH ROOM ---
                        val roomNumNorth = floor * 100 + i + 1
                        val isUserNorth = roomNumNorth == roomNum
                        val northRoomTop = mainCorridorY - mainCorridorHeight / 2 - roomHeight - 6f

                        // Room Fill
                        val northFill = if (isUserNorth) {
                            CrisisRed.copy(alpha = 0.35f)
                        } else {
                            Color(0xFF223552) // Bright readable room surface
                        }
                        val northBorder = if (isUserNorth) {
                            CrisisRed
                        } else {
                            Color(0xFF4A6B94) // High contrast room wall
                        }

                        drawRoundRect(
                            color = northFill,
                            topLeft = Offset(rx + 2f, northRoomTop),
                            size = Size(roomWidth - 4f, roomHeight),
                            cornerRadius = CornerRadius(6f, 6f)
                        )
                        drawRoundRect(
                            color = northBorder,
                            topLeft = Offset(rx + 2f, northRoomTop),
                            size = Size(roomWidth - 4f, roomHeight),
                            cornerRadius = CornerRadius(6f, 6f),
                            style = Stroke(width = if (isUserNorth) 2.5f else 1.5f)
                        )

                        // Doorway Indicator (North Room opening to Main Corridor)
                        val doorNorthX = rx + (roomWidth / 2f) - 7f
                        drawRect(
                            color = if (isUserNorth) CrisisRed else Color(0xFF38BDF8),
                            topLeft = Offset(doorNorthX, northRoomTop + roomHeight - 1f),
                            size = Size(14f, 7f)
                        )

                        // Room Number Text (North)
                        val roomNorthText = "RM $roomNumNorth"
                        val roomNorthLayout = textMeasurer.measure(
                            text = roomNorthText,
                            style = TextStyle(
                                color = if (isUserNorth) Color.White else Color(0xFFF1F5F9),
                                fontSize = 9.5.sp,
                                fontWeight = if (isUserNorth) FontWeight.Black else FontWeight.Bold
                            )
                        )
                        drawText(
                            textLayoutResult = roomNorthLayout,
                            topLeft = Offset(
                                rx + (roomWidth - roomNorthLayout.size.width) / 2f,
                                northRoomTop + (if (isUserNorth) 6f else 16f)
                            )
                        )

                        if (isUserNorth) {
                            val youBadgeLayout = textMeasurer.measure(
                                text = "YOU ARE HERE",
                                style = TextStyle(
                                    color = Color(0xFFFFD1D1),
                                    fontSize = 6.5.sp,
                                    fontWeight = FontWeight.Black
                                )
                            )
                            drawText(
                                textLayoutResult = youBadgeLayout,
                                topLeft = Offset(
                                    rx + (roomWidth - youBadgeLayout.size.width) / 2f,
                                    northRoomTop + 24f
                                )
                            )
                        }

                        // --- SOUTH ROOM ---
                        val roomNumSouth = floor * 100 + i + 7
                        val isUserSouth = roomNumSouth == roomNum
                        val southRoomTop = mainCorridorY + mainCorridorHeight / 2 + 6f

                        val southFill = if (isUserSouth) {
                            CrisisRed.copy(alpha = 0.35f)
                        } else {
                            Color(0xFF223552)
                        }
                        val southBorder = if (isUserSouth) {
                            CrisisRed
                        } else {
                            Color(0xFF4A6B94)
                        }

                        drawRoundRect(
                            color = southFill,
                            topLeft = Offset(rx + 2f, southRoomTop),
                            size = Size(roomWidth - 4f, roomHeight),
                            cornerRadius = CornerRadius(6f, 6f)
                        )
                        drawRoundRect(
                            color = southBorder,
                            topLeft = Offset(rx + 2f, southRoomTop),
                            size = Size(roomWidth - 4f, roomHeight),
                            cornerRadius = CornerRadius(6f, 6f),
                            style = Stroke(width = if (isUserSouth) 2.5f else 1.5f)
                        )

                        // Doorway Indicator (South Room opening to Main Corridor)
                        val doorSouthX = rx + (roomWidth / 2f) - 7f
                        drawRect(
                            color = if (isUserSouth) CrisisRed else Color(0xFF38BDF8),
                            topLeft = Offset(doorSouthX, southRoomTop - 6f),
                            size = Size(14f, 7f)
                        )

                        // Room Number Text (South)
                        val roomSouthText = "RM $roomNumSouth"
                        val roomSouthLayout = textMeasurer.measure(
                            text = roomSouthText,
                            style = TextStyle(
                                color = if (isUserSouth) Color.White else Color(0xFFF1F5F9),
                                fontSize = 9.5.sp,
                                fontWeight = if (isUserSouth) FontWeight.Black else FontWeight.Bold
                            )
                        )
                        drawText(
                            textLayoutResult = roomSouthLayout,
                            topLeft = Offset(
                                rx + (roomWidth - roomSouthLayout.size.width) / 2f,
                                southRoomTop + (if (isUserSouth) 6f else 16f)
                            )
                        )

                        if (isUserSouth) {
                            val youBadgeLayout = textMeasurer.measure(
                                text = "YOU ARE HERE",
                                style = TextStyle(
                                    color = Color(0xFFFFD1D1),
                                    fontSize = 6.5.sp,
                                    fontWeight = FontWeight.Black
                                )
                            )
                            drawText(
                                textLayoutResult = youBadgeLayout,
                                topLeft = Offset(
                                    rx + (roomWidth - youBadgeLayout.size.width) / 2f,
                                    southRoomTop + 24f
                                )
                            )
                        }
                    }

                    // 4. Exit Portals: Exit A (West) and Exit B (East) - Highly Visible Signs
                    val exitAX = scaleX(80.0)
                    val exitBX = scaleX(820.0)

                    val activeHazards = dangerZones.filter { it.floor == floor && it.active }
                    val isExitABlocked = activeHazards.any { hypot(it.crsX - 80.0, it.crsY - 450.0) <= it.radiusMeters + 15.0 }
                    val isExitBBlocked = activeHazards.any { hypot(it.crsX - 820.0, it.crsY - 450.0) <= it.radiusMeters + 15.0 }

                    // --- EXIT A (WEST) ---
                    val colorExitA = if (isExitABlocked) CrisisRed else SafeGreen
                    val fillExitA = if (isExitABlocked) CrisisRed.copy(alpha = 0.35f) else SafeGreen.copy(alpha = 0.3f)

                    // Exit A Glow and Box
                    drawRoundRect(
                        color = fillExitA,
                        topLeft = Offset(exitAX - 26f, mainCorridorY - 26f),
                        size = Size(52f, 52f),
                        cornerRadius = CornerRadius(8f, 8f)
                    )
                    drawRoundRect(
                        color = colorExitA,
                        topLeft = Offset(exitAX - 26f, mainCorridorY - 26f),
                        size = Size(52f, 52f),
                        cornerRadius = CornerRadius(8f, 8f),
                        style = Stroke(width = 2.5f)
                    )

                    // Exit A Text Labels
                    val exitATitleLayout = textMeasurer.measure(
                        text = if (isExitABlocked) "EXIT A" else "EXIT A",
                        style = TextStyle(
                            color = Color.White,
                            fontSize = 8.5.sp,
                            fontWeight = FontWeight.Black
                        )
                    )
                    drawText(
                        textLayoutResult = exitATitleLayout,
                        topLeft = Offset(exitAX - exitATitleLayout.size.width / 2f, mainCorridorY - 18f)
                    )

                    val exitASubLayout = textMeasurer.measure(
                        text = if (isExitABlocked) "BLOCKED" else "WEST EXIT",
                        style = TextStyle(
                            color = if (isExitABlocked) Color(0xFFFFCCCC) else SafeGreen,
                            fontSize = 6.5.sp,
                            fontWeight = FontWeight.Black
                        )
                    )
                    drawText(
                        textLayoutResult = exitASubLayout,
                        topLeft = Offset(exitAX - exitASubLayout.size.width / 2f, mainCorridorY + 2f)
                    )

                    // --- EXIT B (EAST) ---
                    val colorExitB = if (isExitBBlocked) CrisisRed else SafeGreen
                    val fillExitB = if (isExitBBlocked) CrisisRed.copy(alpha = 0.35f) else SafeGreen.copy(alpha = 0.3f)

                    // Exit B Glow and Box
                    drawRoundRect(
                        color = fillExitB,
                        topLeft = Offset(exitBX - 26f, mainCorridorY - 26f),
                        size = Size(52f, 52f),
                        cornerRadius = CornerRadius(8f, 8f)
                    )
                    drawRoundRect(
                        color = colorExitB,
                        topLeft = Offset(exitBX - 26f, mainCorridorY - 26f),
                        size = Size(52f, 52f),
                        cornerRadius = CornerRadius(8f, 8f),
                        style = Stroke(width = 2.5f)
                    )

                    // Exit B Text Labels
                    val exitBTitleLayout = textMeasurer.measure(
                        text = if (isExitBBlocked) "EXIT B" else "EXIT B",
                        style = TextStyle(
                            color = Color.White,
                            fontSize = 8.5.sp,
                            fontWeight = FontWeight.Black
                        )
                    )
                    drawText(
                        textLayoutResult = exitBTitleLayout,
                        topLeft = Offset(exitBX - exitBTitleLayout.size.width / 2f, mainCorridorY - 18f)
                    )

                    val exitBSubLayout = textMeasurer.measure(
                        text = if (isExitBBlocked) "BLOCKED" else "EAST EXIT",
                        style = TextStyle(
                            color = if (isExitBBlocked) Color(0xFFFFCCCC) else SafeGreen,
                            fontSize = 6.5.sp,
                            fontWeight = FontWeight.Black
                        )
                    )
                    drawText(
                        textLayoutResult = exitBSubLayout,
                        topLeft = Offset(exitBX - exitBSubLayout.size.width / 2f, mainCorridorY + 2f)
                    )

                    // 5. Render Active Danger / Fire / Smoke Zones (Vivid Warnings)
                    activeHazards.forEach { hazard ->
                        val hx = scaleX(hazard.crsX)
                        val hy = scaleY(hazard.crsY)
                        val r = (scaleX(hazard.radiusMeters) - scaleX(0.0)).coerceAtLeast(34f) * hazardPulse
                        val hazardColor = if (hazard.hazardType == "smoke") WarningAmber else CrisisRed

                        // Outer glowing danger pulse
                        drawCircle(
                            color = hazardColor.copy(alpha = 0.28f),
                            center = Offset(hx, hy),
                            radius = r
                        )
                        drawCircle(
                            color = hazardColor.copy(alpha = 0.85f),
                            center = Offset(hx, hy),
                            radius = r,
                            style = Stroke(
                                width = 3f,
                                pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 8f))
                            )
                        )

                        // Core flame/smoke center
                        drawCircle(
                            color = hazardColor,
                            center = Offset(hx, hy),
                            radius = 14f
                        )
                        drawCircle(
                            color = Color.White,
                            center = Offset(hx, hy),
                            radius = 6f
                        )

                        // Hazard Label
                        val hLabel = if (hazard.hazardType == "smoke") "DENSE SMOKE" else "ACTIVE FIRE"
                        val hazardTextLayout = textMeasurer.measure(
                            text = hLabel,
                            style = TextStyle(
                                color = Color.White,
                                fontSize = 7.5.sp,
                                fontWeight = FontWeight.Black
                            )
                        )
                        drawRoundRect(
                            color = Color.Black.copy(alpha = 0.75f),
                            topLeft = Offset(hx - hazardTextLayout.size.width / 2f - 4f, hy + 16f),
                            size = Size(hazardTextLayout.size.width + 8f, hazardTextLayout.size.height + 4f),
                            cornerRadius = CornerRadius(4f, 4f)
                        )
                        drawText(
                            textLayoutResult = hazardTextLayout,
                            topLeft = Offset(hx - hazardTextLayout.size.width / 2f, hy + 18f)
                        )
                    }

                    // 6. Draw Plotted Walkable / Portal Nodes
                    nodes.forEach { node ->
                        val nx = scaleX(node.x)
                        val ny = scaleY(node.y)
                        val nodeColor = if (node.nodeType == "portal") WarningAmber else TacticalCyanLight
                        drawCircle(color = nodeColor, center = Offset(nx, ny), radius = 7f)
                        drawCircle(color = Color.White, center = Offset(nx, ny), radius = 3f)
                    }

                    // 7. Dynamic Calculated Escape Polyline (Vivid Neon Green Path with Glowing Core)
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

                        // Broad Ambient Glow
                        drawPath(
                            path = escapePath,
                            color = SafeGreen.copy(alpha = 0.45f),
                            style = Stroke(width = 12f)
                        )

                        // Mid-tier Bright Glow
                        drawPath(
                            path = escapePath,
                            color = SafeGreen.copy(alpha = 0.8f),
                            style = Stroke(width = 7f)
                        )

                        // Animated Dashed Route Core
                        drawPath(
                            path = escapePath,
                            color = Color(0xFFD1FAE5), // Bright mint-white core
                            style = Stroke(
                                width = 3.5f,
                                pathEffect = PathEffect.dashPathEffect(
                                    intervals = floatArrayOf(16f, 10f),
                                    phase = pathDashPhase
                                )
                            )
                        )

                        // Intermediate turn waypoints
                        for (i in 1 until waypoints.size - 1) {
                            val wx = scaleX(waypoints[i].x)
                            val wy = scaleY(waypoints[i].y)
                            drawCircle(color = SafeGreen, center = Offset(wx, wy), radius = 7f)
                            drawCircle(color = Color.White, center = Offset(wx, wy), radius = 3f)
                        }
                    }

                    // 8. User Current Position (Vibrant Pulsing Beacon)
                    val (roomInterior, _) = FloorRoutingEngine.getRoomCoordinate(roomId, floor)
                    val userX = scaleX(roomInterior.x)
                    val userY = scaleY(roomInterior.y)
                    val haloRadius = 22f * haloRadiusFraction

                    drawCircle(
                        color = CrisisRed.copy(alpha = 0.4f),
                        center = Offset(userX, userY),
                        radius = haloRadius
                    )
                    drawCircle(
                        color = CrisisRed,
                        center = Offset(userX, userY),
                        radius = 10f
                    )
                    drawCircle(
                        color = Color.White,
                        center = Offset(userX, userY),
                        radius = 4f
                    )
                }

                // Legend & Route Summary Footer Badge (High Contrast Floating Card)
                Row(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(8.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF0F172A).copy(alpha = 0.96f))
                        .border(1.dp, Color(0xFF334155), RoundedCornerShape(8.dp))
                        .padding(horizontal = 8.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(modifier = Modifier.size(9.dp).clip(CircleShape).background(CrisisRed))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("YOU (RM $roomId)", fontSize = 9.5.sp, color = Color.White, fontWeight = FontWeight.Black)

                    Spacer(modifier = Modifier.width(8.dp))
                    Box(modifier = Modifier.size(9.dp).clip(RoundedCornerShape(2.dp)).background(SafeGreen))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(if (calculatedRoute.targetExitName.contains("A")) "EXIT A" else "EXIT B", fontSize = 9.5.sp, color = SafeGreen, fontWeight = FontWeight.Black)

                    Spacer(modifier = Modifier.width(8.dp))
                    Box(modifier = Modifier.size(9.dp).clip(CircleShape).background(CrisisRed))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("FIRE HAZARD", fontSize = 9.5.sp, color = CrisisRed, fontWeight = FontWeight.Black)

                    if (calculatedRoute.isRerouted) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("• REROUTED", fontSize = 9.5.sp, color = WarningAmber, fontWeight = FontWeight.Black)
                    }
                }
            }
        }
    }
}
