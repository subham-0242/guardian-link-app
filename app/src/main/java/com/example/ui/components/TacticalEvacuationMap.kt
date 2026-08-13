package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CompassCalibration
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.FloorNode
import com.example.ui.theme.CrisisRed
import com.example.ui.theme.DarkCanvas
import com.example.ui.theme.SafeGreen
import com.example.ui.theme.SurfaceCard
import com.example.ui.theme.TacticalCyan
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.WarningAmber
import kotlin.math.floor

@Composable
fun TacticalEvacuationMap(
    roomId: String,
    floor: Int = 4,
    nodes: List<FloorNode> = emptyList(),
    floorPlanUrl: String? = null,
    onMapDoubleTap: ((Offset) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val roomNum = roomId.toIntOrNull() ?: 402
    // Cartesian formulas from prompt:
    // X = 150 + ((roomNum - 1) % 6) * 130
    // Y = 700 - floor((roomNum - 1) / 6) * 300
    val indexOnFloor = (roomNum % 100) - 1
    val cartesianX = (150 + (indexOnFloor % 6) * 130).toDouble()
    val cartesianY = (700 - floor(indexOnFloor / 6.0) * 300)

    val infiniteTransition = rememberInfiniteTransition(label = "halo")
    val haloRadiusFraction by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 1.4f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "halo_radius"
    )

    val textMeasurer = rememberTextMeasurer()

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("tactical_evacuation_map")
            .clip(RoundedCornerShape(16.dp))
            .border(1.dp, TacticalCyan.copy(alpha = 0.4f), RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(containerColor = SurfaceCard)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Map,
                    contentDescription = null,
                    tint = TacticalCyan,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "TACTICAL FLOOR MAP (CRS 900x1000) • LEVEL 0$floor",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = TacticalCyan
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Canvas Container
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(260.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(DarkCanvas)
            ) {
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(Unit) {
                            detectTapGestures(
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
                            color = TacticalCyan.copy(alpha = 0.08f),
                            start = Offset(gx, 0f),
                            end = Offset(gx, h),
                            strokeWidth = 1f
                        )
                        drawLine(
                            color = TacticalCyan.copy(alpha = 0.08f),
                            start = Offset(0f, gy),
                            end = Offset(w, gy),
                            strokeWidth = 1f
                        )
                    }

                    // 2. Vector Fallback Corridor Mesh & Rooms Layout
                    val corridorY = scaleY(450.0)
                    val corridorHeight = 35f

                    // Draw Central Hallway Mesh
                    drawRect(
                        color = Color(0xFF1E293B),
                        topLeft = Offset(20f, corridorY - corridorHeight / 2),
                        size = Size(w - 40f, corridorHeight)
                    )
                    drawRect(
                        color = TacticalCyan.copy(alpha = 0.3f),
                        topLeft = Offset(20f, corridorY - corridorHeight / 2),
                        size = Size(w - 40f, corridorHeight),
                        style = Stroke(width = 2f)
                    )

                    // Draw Room Boxes (Rooms 401..406 on North, 407..412 on South)
                    val roomWidth = (w - 100f) / 6f
                    for (i in 0 until 6) {
                        val roomNumNorth = floor * 100 + i + 1
                        val rx = 50f + i * roomWidth

                        // North Room
                        drawRoundRect(
                            color = if (roomNumNorth == roomNum) CrisisRed.copy(alpha = 0.25f) else Color(0xFF131C2E),
                            topLeft = Offset(rx + 4f, corridorY - 75f),
                            size = Size(roomWidth - 8f, 50f),
                            cornerRadius = CornerRadius(6f, 6f)
                        )
                        drawRoundRect(
                            color = if (roomNumNorth == roomNum) CrisisRed else TacticalCyan.copy(alpha = 0.4f),
                            topLeft = Offset(rx + 4f, corridorY - 75f),
                            size = Size(roomWidth - 8f, 50f),
                            cornerRadius = CornerRadius(6f, 6f),
                            style = Stroke(width = 2f)
                        )

                        // South Room
                        val roomNumSouth = floor * 100 + i + 7
                        drawRoundRect(
                            color = if (roomNumSouth == roomNum) CrisisRed.copy(alpha = 0.25f) else Color(0xFF131C2E),
                            topLeft = Offset(rx + 4f, corridorY + 25f),
                            size = Size(roomWidth - 8f, 50f),
                            cornerRadius = CornerRadius(6f, 6f)
                        )
                        drawRoundRect(
                            color = if (roomNumSouth == roomNum) CrisisRed else TacticalCyan.copy(alpha = 0.4f),
                            topLeft = Offset(rx + 4f, corridorY + 25f),
                            size = Size(roomWidth - 8f, 50f),
                            cornerRadius = CornerRadius(6f, 6f),
                            style = Stroke(width = 2f)
                        )
                    }

                    // 3. Smoke Hazard Zone (Floor 4 East Corridor)
                    val smokeX = scaleX(150.0)
                    drawCircle(
                        color = CrisisRed.copy(alpha = 0.2f),
                        center = Offset(smokeX, corridorY),
                        radius = 45f
                    )
                    drawCircle(
                        color = CrisisRed.copy(alpha = 0.6f),
                        center = Offset(smokeX, corridorY),
                        radius = 45f,
                        style = Stroke(width = 2f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f)))
                    )

                    // 4. Safe Evacuation Path (Green Dashed Route to Stairwell B)
                    val userX = scaleX(cartesianX)
                    val userY = scaleY(cartesianY)
                    val exitX = scaleX(820.0)
                    val exitY = corridorY

                    val path = Path().apply {
                        moveTo(userX, userY)
                        lineTo(userX, corridorY)
                        lineTo(exitX, exitY)
                    }
                    drawPath(
                        path = path,
                        color = SafeGreen,
                        style = Stroke(width = 4f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 8f)))
                    )

                    // 5. Exit Stairwell B Portal Marker
                    drawRoundRect(
                        color = SafeGreen,
                        topLeft = Offset(exitX - 25f, corridorY - 25f),
                        size = Size(50f, 50f),
                        cornerRadius = CornerRadius(8f, 8f)
                    )

                    // 6. Plotted Nodes (Walkable Cyan / Portal Orange)
                    nodes.forEach { node ->
                        val nx = scaleX(node.x)
                        val ny = scaleY(node.y)
                        val nodeColor = if (node.nodeType == "portal") WarningAmber else TacticalCyan
                        drawCircle(color = nodeColor, center = Offset(nx, ny), radius = 8f)
                        drawCircle(color = Color.White, center = Offset(nx, ny), radius = 3f)
                    }

                    // 7. User Location Pulsing Halo & Marker
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

                // Legend Badge
                Row(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(8.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(SurfaceCard.copy(alpha = 0.9f))
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(modifier = Modifier.size(8.dp).clip(RoundedCornerShape(2.dp)).background(CrisisRed))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("YOU", fontSize = 10.sp, color = TextPrimary, fontWeight = FontWeight.Bold)

                    Spacer(modifier = Modifier.width(8.dp))
                    Box(modifier = Modifier.size(8.dp).clip(RoundedCornerShape(2.dp)).background(SafeGreen))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("EXIT B", fontSize = 10.sp, color = TextPrimary, fontWeight = FontWeight.Bold)

                    Spacer(modifier = Modifier.width(8.dp))
                    Box(modifier = Modifier.size(8.dp).clip(RoundedCornerShape(2.dp)).background(TacticalCyan))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("WALKABLE", fontSize = 10.sp, color = TextPrimary, fontWeight = FontWeight.Bold)

                    Spacer(modifier = Modifier.width(8.dp))
                    Box(modifier = Modifier.size(8.dp).clip(RoundedCornerShape(2.dp)).background(WarningAmber))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("PORTAL", fontSize = 10.sp, color = TextPrimary, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
