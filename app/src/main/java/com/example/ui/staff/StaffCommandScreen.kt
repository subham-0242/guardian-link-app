package com.example.ui.staff

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddLocation
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.FloorNode
import com.example.data.repository.EmergencyRepository
import com.example.ui.theme.CrisisRed
import com.example.ui.theme.DarkCanvas
import com.example.ui.theme.GlassBorder
import com.example.ui.theme.GlassSurface
import com.example.ui.theme.SafeGreen
import com.example.ui.theme.SurfaceCard
import com.example.ui.theme.TacticalCyan
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.WarningAmber
import kotlinx.coroutines.launch
import java.util.UUID

@Composable
fun StaffCommandScreen(
    onBackToHome: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val repository = remember { EmergencyRepository(context) }
    val scope = rememberCoroutineScope()

    val dbNodes by repository.getFloorNodes(4).collectAsState(initial = emptyList())
    val broadcasts by repository.getAllBroadcasts().collectAsState(initial = emptyList())
    val dangerZones by repository.getActiveDangerZones().collectAsState(initial = emptyList())

    // Node plotting state
    var selectedNodeType by remember { mutableStateOf("walkable") } // "walkable" (cyan) or "portal" (orange)
    var plottedNodes = remember { mutableStateListOf<FloorNode>() }
    var statusMessage by remember { mutableStateOf("") }

    // Broadcast state
    var broadcastMsgInput by remember { mutableStateOf("") }
    var broadcastPriority by remember { mutableStateOf("critical") }

    // Danger zone state
    var zoneLabelInput by remember { mutableStateOf("") }
    var zoneRadiusInput by remember { mutableStateOf("15.0") }

    // Gemini SitRep state
    var isGeneratingSitRep by remember { mutableStateOf(false) }
    var sitRepBullets by remember { mutableStateOf<List<String>>(emptyList()) }

    val scrollState = rememberScrollState()

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(DarkCanvas)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(scrollState)
        ) {
            // Header Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onBackToHome,
                        modifier = Modifier.testTag("staff_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = WarningAmber
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "STAFF COMMAND CENTER",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Black,
                        color = WarningAmber
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(WarningAmber.copy(alpha = 0.2f))
                        .border(1.dp, WarningAmber, RoundedCornerShape(12.dp))
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "LEVEL 04 SETUP",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = WarningAmber
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 1. AI Gemini Situation Report Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("sitrep_card")
                    .clip(RoundedCornerShape(16.dp))
                    .border(1.dp, TacticalCyan.copy(alpha = 0.4f), RoundedCornerShape(16.dp)),
                colors = CardDefaults.cardColors(containerColor = SurfaceCard)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = TacticalCyan,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "GEMINI SYNTHESIZED SITUATION REPORT",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = TacticalCyan
                            )
                        }

                        Button(
                            onClick = {
                                isGeneratingSitRep = true
                                scope.launch {
                                    sitRepBullets = repository.generateSitRep()
                                    isGeneratingSitRep = false
                                }
                            },
                            modifier = Modifier.testTag("generate_sitrep_button"),
                            colors = ButtonDefaults.buttonColors(containerColor = TacticalCyan, contentColor = Color.Black),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            if (isGeneratingSitRep) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.Black, strokeWidth = 2.dp)
                            } else {
                                Text("SYNTHESIZE SIT-REP", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    if (sitRepBullets.isEmpty()) {
                        Text(
                            text = "Tap 'Synthesize Sit-Rep' to generate real-time AI summary from active room telemetry, hazard clusters, and guest distress reports.",
                            fontSize = 12.sp,
                            color = TextSecondary
                        )
                    } else {
                        sitRepBullets.forEach { bullet ->
                            Row(
                                modifier = Modifier.padding(vertical = 4.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                Text("• ", fontSize = 13.sp, color = TacticalCyan, fontWeight = FontWeight.Bold)
                                Text(bullet, fontSize = 12.sp, color = TextPrimary, fontWeight = FontWeight.Medium)
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 2. Interactive Floor Node Plotter
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("floor_node_plotter_card")
                    .clip(RoundedCornerShape(16.dp))
                    .border(1.dp, GlassBorder, RoundedCornerShape(16.dp)),
                colors = CardDefaults.cardColors(containerColor = SurfaceCard)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Map,
                                contentDescription = null,
                                tint = WarningAmber,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "INTERACTIVE FLOOR PLAN NODE PLOTTER",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = WarningAmber
                            )
                        }

                        Text(
                            text = "Floor 4 (900x1000 CRS)",
                            fontSize = 11.sp,
                            color = TextSecondary
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Node Type Toggle Bar
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .testTag("node_type_walkable")
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (selectedNodeType == "walkable") TacticalCyan else SurfaceCard)
                                .border(1.dp, TacticalCyan, RoundedCornerShape(10.dp))
                                .clickable { selectedNodeType = "walkable" }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "WALKABLE NODE (CYAN)",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (selectedNodeType == "walkable") Color.Black else TacticalCyan
                            )
                        }

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .testTag("node_type_portal")
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (selectedNodeType == "portal") WarningAmber else SurfaceCard)
                                .border(1.dp, WarningAmber, RoundedCornerShape(10.dp))
                                .clickable { selectedNodeType = "portal" }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "PORTAL / EGRESS (ORANGE)",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (selectedNodeType == "portal") Color.Black else WarningAmber
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Plotter Canvas
                    val activeNodesList = if (plottedNodes.isNotEmpty()) plottedNodes else dbNodes
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(240.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(DarkCanvas)
                    ) {
                        Canvas(
                            modifier = Modifier
                                .fillMaxSize()
                                .pointerInput(Unit) {
                                    detectTapGestures { offset ->
                                        // Tap on map plots a new node coordinate in CRS (0..900, 0..1000)
                                        val crsX = ((offset.x / size.width) * 900.0)
                                        val crsY = ((1.0 - (offset.y / size.height)) * 1000.0)

                                        val newNode = FloorNode(
                                            id = "fn_${UUID.randomUUID().toString().take(6)}",
                                            floor = 4,
                                            nodeType = selectedNodeType,
                                            x = crsX,
                                            y = crsY,
                                            label = if (selectedNodeType == "portal") "Stairwell / Exit" else "Walkable Node"
                                        )

                                        if (plottedNodes.isEmpty()) {
                                            plottedNodes.addAll(dbNodes)
                                        }
                                        plottedNodes.add(newNode)
                                        statusMessage = "Plotted node at CRS (${crsX.toInt()}, ${crsY.toInt()})"
                                    }
                                }
                        ) {
                            val w = size.width
                            val h = size.height

                            fun scaleX(x: Double): Float = ((x / 900.0) * w).toFloat()
                            fun scaleY(y: Double): Float = ((1.0 - (y / 1000.0)) * h).toFloat()

                            // Draw Grid
                            for (i in 0..6) {
                                val gx = (w / 6) * i
                                drawLine(color = TacticalCyan.copy(alpha = 0.1f), start = Offset(gx, 0f), end = Offset(gx, h))
                                val gy = (h / 6) * i
                                drawLine(color = TacticalCyan.copy(alpha = 0.1f), start = Offset(0f, gy), end = Offset(w, gy))
                            }

                            // Draw Corridors
                            val cy = scaleY(450.0)
                            drawRect(color = Color(0xFF1E293B), topLeft = Offset(10f, cy - 20f), size = Size(w - 20f, 40f))

                            // Draw All Active Nodes
                            activeNodesList.forEach { n ->
                                val nx = scaleX(n.x)
                                val ny = scaleY(n.y)
                                val color = if (n.nodeType == "portal") WarningAmber else TacticalCyan
                                drawCircle(color = color, center = Offset(nx, ny), radius = 10f)
                                drawCircle(color = Color.White, center = Offset(nx, ny), radius = 4f)
                            }
                        }

                        Text(
                            text = "TAP CANVAS TO PLOT NEW NODE",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextSecondary,
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .padding(8.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                scope.launch {
                                    val toSave = if (plottedNodes.isNotEmpty()) plottedNodes else dbNodes
                                    repository.saveFloorNodes(4, toSave)
                                    statusMessage = "Saved ${toSave.size} Floor Nodes to Mesh DB!"
                                }
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("save_nodes_button"),
                            colors = ButtonDefaults.buttonColors(containerColor = SafeGreen, contentColor = Color.White),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("SAVE NODES", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = {
                                plottedNodes.clear()
                                scope.launch {
                                    repository.saveFloorNodes(4, emptyList())
                                    statusMessage = "Cleared nodes!"
                                }
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("clear_nodes_button"),
                            colors = ButtonDefaults.buttonColors(containerColor = CrisisRed.copy(alpha = 0.2f), contentColor = CrisisRed),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("CLEAR NODES", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    if (statusMessage.isNotBlank()) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(text = statusMessage, fontSize = 11.sp, color = SafeGreen, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 3. Broadcast Alert Publisher Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("broadcast_publisher_card")
                    .clip(RoundedCornerShape(16.dp))
                    .border(1.dp, GlassBorder, RoundedCornerShape(16.dp)),
                colors = CardDefaults.cardColors(containerColor = SurfaceCard)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Campaign,
                            contentDescription = null,
                            tint = CrisisRed,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "PUBLISH EMERGENCY BROADCAST MESH",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = CrisisRed
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = broadcastMsgInput,
                        onValueChange = { broadcastMsgInput = it },
                        placeholder = { Text("e.g. CRITICAL ALERT: Fire crews on Level 4. Stay inside rooms.", fontSize = 12.sp, color = TextSecondary) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("broadcast_message_input"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = CrisisRed,
                            unfocusedBorderColor = Color(0xFF334155),
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Button(
                        onClick = {
                            if (broadcastMsgInput.isNotBlank()) {
                                scope.launch {
                                    repository.publishBroadcast(broadcastMsgInput, broadcastPriority, "all")
                                    broadcastMsgInput = ""
                                    statusMessage = "Broadcast published to all guest units!"
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("publish_broadcast_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = CrisisRed, contentColor = Color.White),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("TRANSMIT BROADCAST ALERT", fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 4. Create Hazard Danger Zone Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("hazard_zone_card")
                    .clip(RoundedCornerShape(16.dp))
                    .border(1.dp, GlassBorder, RoundedCornerShape(16.dp)),
                colors = CardDefaults.cardColors(containerColor = SurfaceCard)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = WarningAmber,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "MARK HAZARD DANGER ZONE",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = WarningAmber
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = zoneLabelInput,
                        onValueChange = { zoneLabelInput = it },
                        label = { Text("Zone Label (e.g. Active Smoke Corridor 4E)") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("danger_zone_label_input"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = WarningAmber,
                            unfocusedBorderColor = Color(0xFF334155),
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        ),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Button(
                        onClick = {
                            val label = if (zoneLabelInput.isNotBlank()) zoneLabelInput else "Heavy Smoke Floor 4"
                            val rad = zoneRadiusInput.toDoubleOrNull() ?: 15.0
                            scope.launch {
                                repository.createDangerZone(4, label, "critical", rad)
                                zoneLabelInput = ""
                                statusMessage = "Hazard zone marked!"
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("create_danger_zone_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = WarningAmber, contentColor = Color.Black),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("CREATE HAZARD ZONE", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
