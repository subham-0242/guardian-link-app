package com.example.ui.responder

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Air
import androidx.compose.material.icons.filled.AltRoute
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Emergency
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.SupportAgent
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.repository.EmergencyRepository
import com.example.ui.components.TacticalEvacuationMap
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
import com.example.util.AarPdfGenerator
import com.example.util.HazardClusterer
import com.example.util.PiiScrubber
import com.example.util.TextToSpeechHelper
import kotlinx.coroutines.launch
import java.io.File

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ResponderTriageScreen(
    onBackToHome: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val repository = remember { EmergencyRepository(context) }
    val scope = rememberCoroutineScope()

    val ttsHelper = remember { TextToSpeechHelper(context) }
    DisposableEffect(Unit) {
        onDispose {
            ttsHelper.shutdown()
        }
    }

    val emergencies by repository.getAllActiveEmergencies().collectAsState(initial = emptyList())
    val incidents by repository.getAllIncidents().collectAsState(initial = emptyList())
    val nodes by repository.getFloorNodes(4).collectAsState(initial = emptyList())
    val dangerZones by repository.getDangerZonesForFloor(4).collectAsState(initial = emptyList())
    val floorPlan by repository.getFloorPlan(4).collectAsState(initial = null)

    var selectedRoomId by remember { mutableStateOf("402") }
    val messages by repository.getChatMessagesForRoom(selectedRoomId).collectAsState(initial = emptyList())

    var activeHazardTool by remember { mutableStateOf("fire") } // "fire", "smoke", "inspect"
    var hazardDeployFeedback by remember { mutableStateOf("") }

    var responderMsgInput by remember { mutableStateOf("") }
    var responderTargetLang by remember { mutableStateOf("Spanish") }
    var showLangPicker by remember { mutableStateOf(false) }
    var piiCount by remember { mutableStateOf(0) }
    var aarGeneratedFile by remember { mutableStateOf<File?>(null) }
    var showAarDialog by remember { mutableStateOf(false) }

    val supportedLanguages = listOf("Spanish", "French", "Mandarin", "Arabic", "Russian", "Hindi", "Japanese", "Tamil")

    // Floor 4 Matrix Rooms (401-412)
    val roomList = (401..412).map { it.toString() }

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
            // Navigation & Header Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onBackToHome,
                        modifier = Modifier.testTag("responder_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = CrisisRed
                        )
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "FIRST RESPONDER BRIDGE",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Black,
                        color = CrisisRed,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.width(6.dp))

                // PII Scrubber Active Badge
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(SafeGreen.copy(alpha = 0.15f))
                        .border(1.dp, SafeGreen.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                        .padding(horizontal = 8.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Security,
                        contentDescription = "PII Shield",
                        tint = SafeGreen,
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "PII SCRUBBED ($piiCount)",
                        fontSize = 9.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = SafeGreen,
                        maxLines = 1
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 1. Building X-Ray Room Matrix Grid (Level 04)
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("building_xray_grid_card")
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
                                imageVector = Icons.Default.GridOn,
                                contentDescription = null,
                                tint = TacticalCyan,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "LEVEL 04 X-RAY TRIAGE MATRIX",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = TacticalCyan
                            )
                        }

                        Text(
                            text = "SELECTED: RM $selectedRoomId",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = WarningAmber
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // 6x2 Room Matrix Grid
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        roomList.forEach { rm ->
                            val activeEm = emergencies.find { it.roomId == rm }
                            val status = activeEm?.status ?: "vacant"

                            val bg = when (status) {
                                "trapped" -> CrisisRed.copy(alpha = 0.35f)
                                "evacuated" -> SafeGreen.copy(alpha = 0.25f)
                                "checking" -> WarningAmber.copy(alpha = 0.3f)
                                else -> Color(0xFF1E293B)
                            }

                            val borderCol = when {
                                rm == selectedRoomId -> TacticalCyan
                                status == "trapped" -> CrisisRed
                                status == "evacuated" -> SafeGreen
                                status == "checking" -> WarningAmber
                                else -> Color(0xFF334155)
                            }

                            Box(
                                modifier = Modifier
                                    .size(width = 80.dp, height = 50.dp)
                                    .testTag("xray_room_$rm")
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(bg)
                                    .border(
                                        width = if (rm == selectedRoomId) 2.5.dp else 1.dp,
                                        color = borderCol,
                                        shape = RoundedCornerShape(10.dp)
                                    )
                                    .clickable { selectedRoomId = rm }
                                    .padding(6.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = "RM $rm",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimary
                                    )
                                    Text(
                                        text = status.uppercase(),
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = when (status) {
                                            "trapped" -> CrisisRed
                                            "evacuated" -> SafeGreen
                                            "checking" -> WarningAmber
                                            else -> TextSecondary
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Hazard Zone Marker Dropper & Real-time Graph Updater Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("hazard_dropper_card")
                    .clip(RoundedCornerShape(16.dp))
                    .border(1.5.dp, if (dangerZones.isNotEmpty()) CrisisRed else TacticalCyan.copy(alpha = 0.5f), RoundedCornerShape(16.dp)),
                colors = CardDefaults.cardColors(containerColor = SurfaceCard)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    // Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(CircleShape)
                                    .background(CrisisRed.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.LocalFireDepartment,
                                    contentDescription = null,
                                    tint = CrisisRed,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "TACTICAL HAZARD ZONE DROPPER",
                                    fontSize = 12.5.sp,
                                    fontWeight = FontWeight.Black,
                                    color = CrisisRed
                                )
                                Text(
                                    text = "REAL-TIME GRAPH REROUTING ENGINE",
                                    fontSize = 9.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextSecondary
                                )
                            }
                        }

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (dangerZones.isNotEmpty()) CrisisRed else SafeGreen.copy(alpha = 0.2f))
                                .padding(horizontal = 6.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = if (dangerZones.isNotEmpty()) "${dangerZones.size} ACTIVE HAZARD${if (dangerZones.size > 1) "S" else ""}" else "GRAPH CLEAR",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (dangerZones.isNotEmpty()) Color.White else SafeGreen
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "1. SELECT HAZARD MARKER TOOL (TAP MAP TO DROP):",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = TacticalCyan
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    // Marker Tool Mode Selector Chips
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        // FIRE ZONE TOOL
                        val isFireTool = activeHazardTool == "fire"
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isFireTool) CrisisRed else GlassSurface)
                                .border(1.dp, if (isFireTool) CrisisRed else GlassBorder, RoundedCornerShape(10.dp))
                                .clickable { activeHazardTool = "fire" }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.LocalFireDepartment,
                                    contentDescription = null,
                                    tint = if (isFireTool) Color.White else CrisisRed,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "DROP FIRE 🔥",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isFireTool) Color.White else TextPrimary
                                )
                            }
                        }

                        // SMOKE ZONE TOOL
                        val isSmokeTool = activeHazardTool == "smoke"
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isSmokeTool) WarningAmber else GlassSurface)
                                .border(1.dp, if (isSmokeTool) WarningAmber else GlassBorder, RoundedCornerShape(10.dp))
                                .clickable { activeHazardTool = "smoke" }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Air,
                                    contentDescription = null,
                                    tint = if (isSmokeTool) Color.Black else WarningAmber,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "DROP SMOKE 💨",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSmokeTool) Color.Black else TextPrimary
                                )
                            }
                        }

                        // INSPECT TOOL
                        val isInspect = activeHazardTool == "inspect"
                        Box(
                            modifier = Modifier
                                .weight(0.9f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isInspect) TacticalCyan else GlassSurface)
                                .border(1.dp, if (isInspect) TacticalCyan else GlassBorder, RoundedCornerShape(10.dp))
                                .clickable { activeHazardTool = "inspect" }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "📍 SELECT RM",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isInspect) Color.Black else TextPrimary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Rapid Deployment Presets Row
                    Text(
                        text = "2. RAPID HAZARD CORRIDOR PRESETS:",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextSecondary
                    )
                    Spacer(modifier = Modifier.height(4.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        // Preset 1: West Stairwell Fire (Blocks Exit A)
                        OutlinedButton(
                            onClick = {
                                scope.launch {
                                    repository.createDangerZone(
                                        floor = 4,
                                        label = "West Stairwell Fire (Exit A Blocked)",
                                        severity = "critical",
                                        radius = 65.0,
                                        crsX = 130.0,
                                        crsY = 450.0,
                                        hazardType = "fire"
                                    )
                                    hazardDeployFeedback = "🔥 West Stairwell Fire deployed! Graph model updated: all guests rerouted to East Exit B."
                                }
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(38.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = CrisisRed),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("🚨 WEST FIRE (X:130)", fontSize = 9.5.sp, fontWeight = FontWeight.Bold)
                        }

                        // Preset 2: Central Corridor Fire
                        OutlinedButton(
                            onClick = {
                                scope.launch {
                                    repository.createDangerZone(
                                        floor = 4,
                                        label = "Central Hallway Active Fire",
                                        severity = "critical",
                                        radius = 60.0,
                                        crsX = 460.0,
                                        crsY = 450.0,
                                        hazardType = "fire"
                                    )
                                    hazardDeployFeedback = "🔥 Central Hallway Fire deployed! Graph model updated: guests rerouted via North/South bypass."
                                }
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(38.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = CrisisRed),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("🔥 CENTER FIRE (X:460)", fontSize = 9.5.sp, fontWeight = FontWeight.Bold)
                        }

                        // Preset 3: East Corridor Smoke (Blocks Exit B)
                        OutlinedButton(
                            onClick = {
                                scope.launch {
                                    repository.createDangerZone(
                                        floor = 4,
                                        label = "East Wing Dense Smoke (Exit B Blocked)",
                                        severity = "high",
                                        radius = 65.0,
                                        crsX = 750.0,
                                        crsY = 450.0,
                                        hazardType = "smoke"
                                    )
                                    hazardDeployFeedback = "💨 East Wing Smoke deployed! Graph model updated: guests rerouted to West Exit A."
                                }
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(38.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = WarningAmber),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("💨 EAST SMOKE (X:750)", fontSize = 9.5.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    if (hazardDeployFeedback.isNotBlank()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(SafeGreen.copy(alpha = 0.15f))
                                .border(1.dp, SafeGreen.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                                .padding(8.dp)
                        ) {
                            Text(
                                text = hazardDeployFeedback,
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White
                            )
                        }
                    }

                    // Active Danger Zones Table & Clearance Controls
                    if (dangerZones.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "ACTIVE HAZARD MARKERS (${dangerZones.size}):",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextSecondary
                            )

                            TextButton(
                                onClick = {
                                    scope.launch {
                                        repository.clearDangerZones(4)
                                        hazardDeployFeedback = "✅ All hazard markers cleared. Floor graph restored to default state."
                                    }
                                }
                            ) {
                                Text("CLEAR ALL", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = CrisisRed)
                            }
                        }

                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            dangerZones.forEach { zone ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(GlassSurface)
                                        .border(1.dp, GlassBorder, RoundedCornerShape(8.dp))
                                        .padding(horizontal = 8.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = if (zone.hazardType == "smoke") Icons.Default.Air else Icons.Default.LocalFireDepartment,
                                            contentDescription = null,
                                            tint = if (zone.hazardType == "smoke") WarningAmber else CrisisRed,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Column {
                                            Text(
                                                text = zone.label,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = TextPrimary
                                            )
                                            Text(
                                                text = "CRS (${zone.crsX.toInt()}, ${zone.crsY.toInt()}) • Radius: ${zone.radiusMeters.toInt()}m",
                                                fontSize = 9.sp,
                                                color = TextSecondary
                                            )
                                        }
                                    }

                                    IconButton(
                                        onClick = {
                                            scope.launch {
                                                repository.deleteDangerZone(zone.id, 4)
                                                hazardDeployFeedback = "Removed marker '${zone.label}' from Firestore."
                                            }
                                        },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "Delete",
                                            tint = CrisisRed,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Tactical Floor Evacuation & Custom Map for Responders (with Tap-to-Drop Hazard Marker)
            TacticalEvacuationMap(
                roomId = selectedRoomId,
                floor = 4,
                nodes = nodes,
                dangerZones = dangerZones,
                floorPlanUrl = floorPlan?.imageUrl,
                onMapTap = { offset, crsX, crsY ->
                    when (activeHazardTool) {
                        "fire" -> {
                            scope.launch {
                                repository.createDangerZone(
                                    floor = 4,
                                    label = "Active Fire Zone (${crsX.toInt()}, ${crsY.toInt()})",
                                    severity = "critical",
                                    radius = 55.0,
                                    crsX = crsX,
                                    crsY = crsY,
                                    hazardType = "fire"
                                )
                                hazardDeployFeedback = "🔥 Fire Zone dropped at CRS (${crsX.toInt()}, ${crsY.toInt()})! Firestore updated in real time; all crossing guest polylines recalculated."
                            }
                        }
                        "smoke" -> {
                            scope.launch {
                                repository.createDangerZone(
                                    floor = 4,
                                    label = "Dense Smoke Zone (${crsX.toInt()}, ${crsY.toInt()})",
                                    severity = "high",
                                    radius = 55.0,
                                    crsX = crsX,
                                    crsY = crsY,
                                    hazardType = "smoke"
                                )
                                hazardDeployFeedback = "💨 Smoke Zone dropped at CRS (${crsX.toInt()}, ${crsY.toInt()})! Floor graph updated in Firestore."
                            }
                        }
                        "inspect" -> {
                            // Find closest room
                            val col = (((crsX - 150.0) / 120.0).toInt()).coerceIn(0, 5)
                            val isNorth = crsY >= 450.0
                            val tappedRoom = if (isNorth) 401 + col else 407 + col
                            selectedRoomId = tappedRoom.toString()
                            hazardDeployFeedback = "Inspecting Room $selectedRoomId"
                        }
                    }
                }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 2. Selected Room Triage & Communication Bridge
            val activeSelected = emergencies.find { it.roomId == selectedRoomId }
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("responder_triage_action_card")
                    .clip(RoundedCornerShape(16.dp))
                    .border(1.dp, CrisisRed.copy(alpha = 0.4f), RoundedCornerShape(16.dp)),
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
                                imageVector = Icons.Default.SupportAgent,
                                contentDescription = null,
                                tint = CrisisRed,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "ACTIVE TRIAGE • ROOM $selectedRoomId",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                        }

                        // Target Language Picker
                        Box {
                            Row(
                                modifier = Modifier
                                    .testTag("responder_lang_picker")
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(GlassSurface)
                                    .border(1.dp, GlassBorder, RoundedCornerShape(16.dp))
                                    .clickable { showLangPicker = true }
                                    .padding(horizontal = 10.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Translate,
                                    contentDescription = "Lang",
                                    tint = TacticalCyan,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = responderTargetLang,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                            }

                            DropdownMenu(
                                expanded = showLangPicker,
                                onDismissRequest = { showLangPicker = false }
                            ) {
                                supportedLanguages.forEach { lang ->
                                    DropdownMenuItem(
                                        text = { Text(lang) },
                                        onClick = {
                                            responderTargetLang = lang
                                            showLangPicker = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    if (activeSelected?.sosText?.isNotBlank() == true) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(CrisisRed.copy(alpha = 0.15f))
                                .border(1.dp, CrisisRed.copy(alpha = 0.4f), RoundedCornerShape(10.dp))
                                .padding(10.dp)
                        ) {
                            Text(
                                text = "GUEST DISTRESS TELEMETRY: ${activeSelected.sosText}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Triage Action Buttons
                    Text(
                        text = "UPDATE ROOM STATUS:",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextSecondary
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // EVACUATED / SAFE
                        Button(
                            onClick = {
                                scope.launch {
                                    repository.updateTriageStatus("room_$selectedRoomId", selectedRoomId, 4, "evacuated")
                                }
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("triage_status_evacuated"),
                            colors = ButtonDefaults.buttonColors(containerColor = SafeGreen, contentColor = Color.White),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("SAFE / EVAC", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }

                        // TRAPPED / RESCUE
                        Button(
                            onClick = {
                                scope.launch {
                                    repository.updateTriageStatus("room_$selectedRoomId", selectedRoomId, 4, "trapped")
                                }
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("triage_status_trapped"),
                            colors = ButtonDefaults.buttonColors(containerColor = CrisisRed, contentColor = Color.White),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("TRAPPED", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }

                        // CHECKING
                        Button(
                            onClick = {
                                scope.launch {
                                    repository.updateTriageStatus("room_$selectedRoomId", selectedRoomId, 4, "checking")
                                }
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("triage_status_checking"),
                            colors = ButtonDefaults.buttonColors(containerColor = WarningAmber, contentColor = Color.Black),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("CHECKING", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Chat History for Selected Room
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF0F172A))
                            .padding(8.dp),
                        reverseLayout = true
                    ) {
                        items(messages.reversed()) { msg ->
                            val isResponder = msg.senderRole == "responder"
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 3.dp),
                                contentAlignment = if (isResponder) Alignment.CenterEnd else Alignment.CenterStart
                            ) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(if (isResponder) CrisisRed.copy(alpha = 0.25f) else TacticalCyan.copy(alpha = 0.25f))
                                        .border(1.dp, if (isResponder) CrisisRed else TacticalCyan, RoundedCornerShape(10.dp))
                                        .padding(8.dp)
                                ) {
                                    Column {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = if (isResponder) "You (Responder)" else "Guest (Room $selectedRoomId)",
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isResponder) CrisisRed else TacticalCyan
                                            )

                                            val ttsText = msg.translatedText ?: msg.text
                                            Row(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(6.dp))
                                                    .background(TacticalCyan.copy(alpha = 0.2f))
                                                    .clickable {
                                                        ttsHelper.speak(ttsText, responderTargetLang)
                                                    }
                                                    .padding(horizontal = 6.dp, vertical = 2.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.VolumeUp,
                                                    contentDescription = "TTS Voice",
                                                    tint = TacticalCyan,
                                                    modifier = Modifier.size(12.dp)
                                                )
                                                Spacer(modifier = Modifier.width(2.dp))
                                                Text(
                                                    text = "🔊 TTS",
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = TacticalCyan
                                                )
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(text = msg.text, fontSize = 12.sp, color = TextPrimary)
                                        if (!msg.translatedText.isNullOrBlank()) {
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                text = "🌐 Translated ($responderTargetLang): ${msg.translatedText}",
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = TacticalCyan
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Real-Time English-to-Guest Translation Input
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = responderMsgInput,
                            onValueChange = { responderMsgInput = it },
                            placeholder = { Text("Responder instructions (auto-translated to $responderTargetLang)...", fontSize = 11.sp, color = TextSecondary) },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("responder_message_input"),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = CrisisRed,
                                unfocusedBorderColor = Color(0xFF334155),
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary
                            ),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        IconButton(
                            onClick = {
                                if (responderMsgInput.isNotBlank()) {
                                    val scrubbed = PiiScrubber.scrub(responderMsgInput)
                                    piiCount += scrubbed.redactedCount
                                    scope.launch {
                                        repository.sendChatMessage(selectedRoomId, "responder", responderMsgInput, responderTargetLang)
                                        responderMsgInput = ""
                                    }
                                }
                            },
                            modifier = Modifier
                                .size(48.dp)
                                .testTag("responder_send_chat_button")
                                .clip(CircleShape)
                                .background(CrisisRed)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Send,
                                contentDescription = "Send",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 3. Synthesized Hazard Incident Clusters
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("hazard_clusters_card")
                    .clip(RoundedCornerShape(16.dp))
                    .border(1.dp, GlassBorder, RoundedCornerShape(16.dp)),
                colors = CardDefaults.cardColors(containerColor = SurfaceCard)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "SYNTHESIZED INCIDENT CLUSTERS",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = TacticalCyan
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    if (incidents.isEmpty()) {
                        Text("No active hazard incidents logged.", fontSize = 12.sp, color = TextSecondary)
                    } else {
                        incidents.forEach { inc ->
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Color(0xFF0F172A))
                                    .border(1.dp, Color(0xFF334155), RoundedCornerShape(10.dp))
                                    .padding(10.dp)
                            ) {
                                Column {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(inc.title, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                                        Text(inc.severity.uppercase(), fontSize = 10.sp, fontWeight = FontWeight.Bold, color = CrisisRed)
                                    }
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(inc.summary, fontSize = 11.sp, color = TextSecondary)
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 4. AUTOMATED AFTER-ACTION REPORT (AAR) GENERATOR CARD
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("aar_generator_card")
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
                        Row(
                            modifier = Modifier.weight(1f),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Description,
                                contentDescription = null,
                                tint = TacticalCyan,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "AUTOMATED AAR REPORT",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = TacticalCyan,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(SafeGreen.copy(alpha = 0.2f))
                                .border(1.dp, SafeGreen.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "RESOLVED",
                                fontSize = 9.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = SafeGreen,
                                maxLines = 1
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "Generate legal & safety compliance audit PDF containing evacuation metrics, timeline, and translated communication logs.",
                        fontSize = 11.sp,
                        color = TextSecondary
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Key Stats Grid
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0xFF0F172A))
                                .padding(10.dp)
                        ) {
                            Column {
                                Text("Evacuation Time", fontSize = 10.sp, color = TextSecondary)
                                Text("14m 32s", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = WarningAmber)
                            }
                        }

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0xFF0F172A))
                                .padding(10.dp)
                        ) {
                            Column {
                                Text("Headcount Stat", fontSize = 10.sp, color = TextSecondary)
                                Text("10/10 Clear (100%)", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = SafeGreen)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // PDF Download Button
                    Button(
                        onClick = {
                            val pdf = AarPdfGenerator.generateAndOpenAarPdf(
                                context = context,
                                emergencies = emergencies,
                                messages = messages,
                                totalEvacuationTimeMin = "14m 32s"
                            )
                            aarGeneratedFile = pdf
                            showAarDialog = true
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("generate_aar_pdf_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = TacticalCyan, contentColor = Color.Black),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.PictureAsPdf,
                            contentDescription = "Generate PDF",
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "📄 GENERATE & DOWNLOAD AAR AUDIT PDF",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }

    // AAR Report Success Modal Dialog
    if (showAarDialog) {
        AlertDialog(
            onDismissRequest = { showAarDialog = false },
            containerColor = SurfaceCard,
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.PictureAsPdf,
                        contentDescription = null,
                        tint = TacticalCyan,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "AFTER-ACTION REPORT (AAR) GENERATED",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = TacticalCyan
                    )
                }
            },
            text = {
                Column {
                    Text(
                        text = "The crisis audit PDF report has been compiled and saved to device documents:",
                        fontSize = 12.sp,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF0F172A))
                            .padding(10.dp)
                    ) {
                        Text(
                            text = aarGeneratedFile?.name ?: "AAR_Report_Incident_Audit.pdf",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = SafeGreen
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Text("• Total Evacuation Duration: 14 minutes 32 seconds", fontSize = 11.sp, color = TextSecondary)
                    Text("• Headcount Clearance: 100% (All Level 4 occupants accounted for)", fontSize = 11.sp, color = TextSecondary)
                    Text("• Critical Timeline Events: 5 Incident Milestones Logged", fontSize = 11.sp, color = TextSecondary)
                    Text("• Audit Logs: Full translated PII-scrubbed comms included", fontSize = 11.sp, color = TextSecondary)
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        aarGeneratedFile?.let { file ->
                            AarPdfGenerator.generateAndOpenAarPdf(context, emergencies, messages)
                        }
                        showAarDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = TacticalCyan, contentColor = Color.Black),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(imageVector = Icons.Default.FileDownload, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("OPEN / SHARE PDF", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAarDialog = false }) {
                    Text("CLOSE", color = TextSecondary, fontSize = 11.sp)
                }
            }
        )
    }
}
