package com.example.ui.staff

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.FloorNode
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
import com.example.util.TextToSpeechHelper
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
    val dbFloorPlan by repository.getFloorPlan(4).collectAsState(initial = null)
    val broadcasts by repository.getAllBroadcasts().collectAsState(initial = emptyList())
    val dangerZones by repository.getActiveDangerZones().collectAsState(initial = emptyList())

    // Node plotting & status state
    var selectedRoomId by remember { mutableStateOf("402") }
    var statusMessage by remember { mutableStateOf("") }
    var selectedNodeType by remember { mutableStateOf("walkable") } // "walkable" (cyan), "portal" (orange), or "inspect"
    var plottedNodes = remember { mutableStateListOf<FloorNode>() }

    // Custom Floor Map State
    var customMapUrlInput by remember { mutableStateOf("") }
    val customMapPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            customMapUrlInput = it.toString()
            scope.launch {
                repository.saveFloorPlan(4, it.toString())
                statusMessage = "Custom Map Image uploaded & published to all dashboards!"
            }
        }
    }

    // Broadcast & PA State
    var broadcastMsgInput by remember { mutableStateOf("ATTENTION FLOOR 4: West stairwell blocked. Evacuate via East exit only.") }
    var broadcastPriority by remember { mutableStateOf("critical") } // "critical", "warning", "advisory"
    var selectedTargetFloor by remember { mutableStateOf<Int?>(4) } // null = Entire Building / All Floors, or 1..5
    var isAudioPaEnabled by remember { mutableStateOf(true) }
    var isTestingAudio by remember { mutableStateOf(false) }

    val ttsHelper = remember { TextToSpeechHelper(context) }
    DisposableEffect(Unit) {
        onDispose {
            ttsHelper.shutdown()
        }
    }

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
                    // Header: Title and Action Button cleanly structured for mobile
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
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = TacticalCyan,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "GEMINI SITUATION REPORT",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = TacticalCyan,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        Button(
                            onClick = {
                                isGeneratingSitRep = true
                                scope.launch {
                                    sitRepBullets = repository.generateSitRep()
                                    isGeneratingSitRep = false
                                }
                            },
                            modifier = Modifier
                                .height(36.dp)
                                .testTag("generate_sitrep_button"),
                            colors = ButtonDefaults.buttonColors(containerColor = TacticalCyan, contentColor = Color.Black),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp)
                        ) {
                            if (isGeneratingSitRep) {
                                CircularProgressIndicator(modifier = Modifier.size(14.dp), color = Color.Black, strokeWidth = 2.dp)
                            } else {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.AutoAwesome,
                                        contentDescription = null,
                                        modifier = Modifier.size(12.dp),
                                        tint = Color.Black
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "SYNTHESIZE",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1
                                    )
                                }
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

            // 1.5 Custom Map Image Upload Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("custom_map_upload_card")
                    .clip(RoundedCornerShape(16.dp))
                    .border(1.dp, TacticalCyan.copy(alpha = 0.5f), RoundedCornerShape(16.dp)),
                colors = CardDefaults.cardColors(containerColor = SurfaceCard)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.CloudUpload,
                            contentDescription = null,
                            tint = TacticalCyan,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "CUSTOM MAP IMAGE MANAGER",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = TacticalCyan
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = "Upload custom floor plan images or blueprints for Level 4. Published maps immediately sync to Guest and First Responder tactical displays.",
                        fontSize = 11.sp,
                        color = TextSecondary
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Preset Architectural Blueprints
                    Text(
                        text = "SELECT BLUEPRINT PRESET:",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextSecondary
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    val presets = listOf(
                        "CAD Tactical Grid" to "https://images.unsplash.com/photo-1524813686514-a57563d77965?auto=format&fit=crop&w=800&q=80",
                        "Architectural Floor" to "https://images.unsplash.com/photo-1600585154340-be6161a56a0c?auto=format&fit=crop&w=800&q=80",
                        "Emergency Plan" to "https://images.unsplash.com/photo-1503387762-592deb58ef4e?auto=format&fit=crop&w=800&q=80"
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        presets.forEach { (label, url) ->
                            val isSelected = customMapUrlInput == url || (customMapUrlInput.isEmpty() && dbFloorPlan?.imageUrl == url)
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSelected) TacticalCyan else GlassSurface)
                                    .border(1.dp, TacticalCyan.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                                    .clickable {
                                        customMapUrlInput = url
                                        scope.launch {
                                            repository.saveFloorPlan(4, url)
                                            statusMessage = "Published preset map: $label"
                                        }
                                    }
                                    .padding(vertical = 8.dp, horizontal = 4.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = label,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) Color.Black else TextPrimary
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Select from device or clear map
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                customMapPickerLauncher.launch("image/*")
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("select_map_image_button"),
                            colors = ButtonDefaults.buttonColors(containerColor = TacticalCyan, contentColor = Color.Black),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Image, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("PICK IMAGE FILE", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = {
                                customMapUrlInput = ""
                                scope.launch {
                                    repository.saveFloorPlan(4, "")
                                    statusMessage = "Reset to default vector map!"
                                }
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("clear_custom_map_button"),
                            colors = ButtonDefaults.buttonColors(containerColor = CrisisRed.copy(alpha = 0.2f), contentColor = CrisisRed),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("CLEAR MAP", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Map URL Manual Entry Field
                    OutlinedTextField(
                        value = customMapUrlInput,
                        onValueChange = { customMapUrlInput = it },
                        placeholder = { Text("Or enter custom image URL / path...", fontSize = 11.sp, color = TextSecondary) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("custom_map_url_input"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = TacticalCyan,
                            unfocusedBorderColor = GlassBorder,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        ),
                        shape = RoundedCornerShape(10.dp),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Button(
                        onClick = {
                            if (customMapUrlInput.isNotBlank()) {
                                scope.launch {
                                    repository.saveFloorPlan(4, customMapUrlInput)
                                    statusMessage = "Published custom floor map URL!"
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("publish_map_url_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = SafeGreen, contentColor = Color.White),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(imageVector = Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("PUBLISH MAP URL TO MESH", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }

                    // Active Map Preview Box
                    val activeMapPreview = if (customMapUrlInput.isNotBlank()) customMapUrlInput else dbFloorPlan?.imageUrl
                    if (!activeMapPreview.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("ACTIVE MAP PREVIEW:", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = SafeGreen)
                        Spacer(modifier = Modifier.height(4.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .border(1.dp, SafeGreen.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
                        ) {
                            AsyncImage(
                                model = activeMapPreview,
                                contentDescription = "Active Custom Map Preview",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 2. Interactive Floor Plan & Tactical Evacuation Map
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
                        Row(
                            modifier = Modifier.weight(1f, fill = false),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Map,
                                contentDescription = null,
                                tint = WarningAmber,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "TACTICAL FLOOR PLAN",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = WarningAmber,
                                maxLines = 1
                            )
                        }

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(TacticalCyan.copy(alpha = 0.15f))
                                .border(1.dp, TacticalCyan.copy(alpha = 0.5f), RoundedCornerShape(6.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "FL 4 • RM $selectedRoomId",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = TacticalCyan,
                                maxLines = 1
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Room Quick Selector Row
                    Text(
                        text = "INSPECT GUEST ROOM ROUTE:",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextSecondary
                    )
                    Spacer(modifier = Modifier.height(4.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        (401..406).forEach { rm ->
                            val isSelected = selectedRoomId == rm.toString()
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (isSelected) CrisisRed else GlassSurface)
                                    .border(1.dp, if (isSelected) CrisisRed else GlassBorder, RoundedCornerShape(6.dp))
                                    .clickable { selectedRoomId = rm.toString() }
                                    .padding(vertical = 4.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "$rm",
                                    fontSize = 9.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) Color.White else TextPrimary
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        (407..412).forEach { rm ->
                            val isSelected = selectedRoomId == rm.toString()
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (isSelected) CrisisRed else GlassSurface)
                                    .border(1.dp, if (isSelected) CrisisRed else GlassBorder, RoundedCornerShape(6.dp))
                                    .clickable { selectedRoomId = rm.toString() }
                                    .padding(vertical = 4.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "$rm",
                                    fontSize = 9.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) Color.White else TextPrimary
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Mode Selector Bar (Walkable node, portal node, or inspect room)
                    Text(
                        text = "CANVAS INTERACTION MODE:",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextSecondary
                    )
                    Spacer(modifier = Modifier.height(4.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .testTag("node_type_walkable")
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (selectedNodeType == "walkable") TacticalCyan else SurfaceCard)
                                .border(1.dp, TacticalCyan, RoundedCornerShape(8.dp))
                                .clickable { selectedNodeType = "walkable" }
                                .padding(vertical = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "+ WALKABLE",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (selectedNodeType == "walkable") Color.Black else TacticalCyan
                            )
                        }

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .testTag("node_type_portal")
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (selectedNodeType == "portal") WarningAmber else SurfaceCard)
                                .border(1.dp, WarningAmber, RoundedCornerShape(8.dp))
                                .clickable { selectedNodeType = "portal" }
                                .padding(vertical = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "+ PORTAL",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (selectedNodeType == "portal") Color.Black else WarningAmber
                            )
                        }

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (selectedNodeType == "inspect") SafeGreen else SurfaceCard)
                                .border(1.dp, SafeGreen, RoundedCornerShape(8.dp))
                                .clickable { selectedNodeType = "inspect" }
                                .padding(vertical = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "📍 SELECT RM",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (selectedNodeType == "inspect") Color.Black else SafeGreen
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Tactical Evacuation & Floor Blueprint Map (Same high-contrast architectural map)
                    val activeNodesList = if (plottedNodes.isNotEmpty()) plottedNodes else dbNodes
                    val currentMapUrl = if (customMapUrlInput.isNotBlank()) customMapUrlInput else dbFloorPlan?.imageUrl

                    TacticalEvacuationMap(
                        roomId = selectedRoomId,
                        floor = 4,
                        nodes = activeNodesList,
                        dangerZones = dangerZones,
                        floorPlanUrl = currentMapUrl,
                        onMapTap = { offset, crsX, crsY ->
                            when (selectedNodeType) {
                                "walkable", "portal" -> {
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
                                    statusMessage = "Plotted $selectedNodeType node at CRS (${crsX.toInt()}, ${crsY.toInt()})"
                                }
                                "inspect" -> {
                                    val col = (((crsX - 150.0) / 120.0).toInt()).coerceIn(0, 5)
                                    val isNorth = crsY >= 450.0
                                    val tappedRoom = if (isNorth) 401 + col else 407 + col
                                    selectedRoomId = tappedRoom.toString()
                                    statusMessage = "Inspecting Room $selectedRoomId evacuation route"
                                }
                            }
                        }
                    )

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

            // 3. Mass Broadcast / PA System (One-to-Many Alerting) Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("mass_broadcast_pa_card")
                    .clip(RoundedCornerShape(16.dp))
                    .border(1.dp, CrisisRed.copy(alpha = 0.5f), RoundedCornerShape(16.dp)),
                colors = CardDefaults.cardColors(containerColor = SurfaceCard)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    // Header Bar
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            modifier = Modifier.weight(1f, fill = false),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(30.dp)
                                    .clip(CircleShape)
                                    .background(CrisisRed.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Campaign,
                                    contentDescription = null,
                                    tint = CrisisRed,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "MASS BROADCAST / PA",
                                    fontSize = 12.5.sp,
                                    fontWeight = FontWeight.Black,
                                    color = CrisisRed,
                                    maxLines = 1
                                )
                                Text(
                                    text = "EMERGENCY ALERT TRANSMITTER",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextSecondary,
                                    maxLines = 1
                                )
                            }
                        }

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(CrisisRed)
                                .padding(horizontal = 7.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = "INCIDENT CMD",
                                fontSize = 8.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                maxLines = 1
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Target Floor Selection
                    Text(
                        text = "1. SELECT TARGET FLOOR SCOPE:",
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = TacticalCyan
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    // Floor Scope Selector Chips (Single-line compact mobile layout)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        // Entire Building
                        val isAllSelected = selectedTargetFloor == null
                        Box(
                            modifier = Modifier
                                .weight(1.3f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isAllSelected) CrisisRed else GlassSurface)
                                .border(1.dp, if (isAllSelected) CrisisRed else GlassBorder, RoundedCornerShape(8.dp))
                                .clickable { selectedTargetFloor = null }
                                .padding(vertical = 7.dp, horizontal = 2.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "🏢 ALL",
                                fontSize = 10.sp,
                                fontWeight = if (isAllSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isAllSelected) Color.White else TextPrimary,
                                maxLines = 1
                            )
                        }

                        // Specific Floors 1..5
                        (1..5).forEach { floorNum ->
                            val isFloorSelected = selectedTargetFloor == floorNum
                            val isFloor4 = floorNum == 4
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(
                                        when {
                                            isFloorSelected -> if (isFloor4) CrisisRed else WarningAmber
                                            isFloor4 -> CrisisRed.copy(alpha = 0.15f)
                                            else -> GlassSurface
                                        }
                                    )
                                    .border(
                                        1.dp,
                                        when {
                                            isFloorSelected -> if (isFloor4) CrisisRed else WarningAmber
                                            isFloor4 -> CrisisRed.copy(alpha = 0.5f)
                                            else -> GlassBorder
                                        },
                                        RoundedCornerShape(8.dp)
                                    )
                                    .clickable { selectedTargetFloor = floorNum }
                                    .padding(vertical = 7.dp, horizontal = 2.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = if (isFloor4) "FL 4 🚨" else "FL $floorNum",
                                    fontSize = 10.sp,
                                    fontWeight = if (isFloorSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isFloorSelected) Color.White else TextPrimary,
                                    maxLines = 1
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Priority Level Selector (3 equal single-line buttons)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf("critical" to "🔴 CRITICAL", "warning" to "🟠 URGENT", "advisory" to "🟡 ADVISORY").forEach { (priKey, priLabel) ->
                            val isPriSelected = broadcastPriority == priKey
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(
                                        if (isPriSelected) {
                                            when (priKey) {
                                                "critical" -> CrisisRed.copy(alpha = 0.25f)
                                                "warning" -> WarningAmber.copy(alpha = 0.25f)
                                                else -> SafeGreen.copy(alpha = 0.25f)
                                            }
                                        } else GlassSurface
                                    )
                                    .border(
                                        1.dp,
                                        if (isPriSelected) {
                                            when (priKey) {
                                                "critical" -> CrisisRed
                                                "warning" -> WarningAmber
                                                else -> SafeGreen
                                            }
                                        } else GlassBorder,
                                        RoundedCornerShape(8.dp)
                                    )
                                    .clickable { broadcastPriority = priKey }
                                    .padding(vertical = 7.dp, horizontal = 4.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = priLabel,
                                    fontSize = 10.sp,
                                    fontWeight = if (isPriSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isPriSelected) Color.White else TextSecondary,
                                    maxLines = 1
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Audio PA Broadcast Mode Toggle (Clean full-width single-line control)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isAudioPaEnabled) TacticalCyan.copy(alpha = 0.15f) else GlassSurface)
                            .border(1.dp, if (isAudioPaEnabled) TacticalCyan else GlassBorder, RoundedCornerShape(8.dp))
                            .clickable { isAudioPaEnabled = !isAudioPaEnabled }
                            .padding(horizontal = 10.dp, vertical = 7.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = if (isAudioPaEnabled) Icons.Default.VolumeUp else Icons.Default.NotificationsActive,
                                contentDescription = null,
                                tint = if (isAudioPaEnabled) TacticalCyan else TextSecondary,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isAudioPaEnabled) "AUDIO PA SIREN & VOICE BROADCAST: ACTIVE" else "SILENT PUSH & TEXT ALERT (PA MUTED)",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isAudioPaEnabled) TacticalCyan else TextSecondary,
                                maxLines = 1
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Rapid Tactical Presets
                    Text(
                        text = "2. RAPID TACTICAL PRESET TEMPLATES:",
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = WarningAmber
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    val presets = listOf(
                        Triple(
                            "ATTENTION FLOOR 4: West stairwell blocked. Evacuate via East exit only.",
                            4,
                            "🚨 FL 4 Exit Blocked"
                        ),
                        Triple(
                            "ALL RESIDENTS: Evacuate immediately via nearest stairwell. Do not use elevators.",
                            null,
                            "🏢 Building Evacuate"
                        ),
                        Triple(
                            "ATTENTION FLOORS 3 & 4: Heavy smoke detected. Shelter in place & seal door gaps.",
                            4,
                            "🚪 Shelter In Place"
                        ),
                        Triple(
                            "ALL RESIDENTS: Fire incident contained in East Wing. Stand by for all-clear.",
                            null,
                            "✅ Standby All-Clear"
                        )
                    )

                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        presets.chunked(2).forEach { rowPair ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                rowPair.forEach { (textPreset, floorPreset, label) ->
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(GlassSurface)
                                            .border(1.dp, GlassBorder, RoundedCornerShape(8.dp))
                                            .clickable {
                                                broadcastMsgInput = textPreset
                                                selectedTargetFloor = floorPreset
                                            }
                                            .padding(horizontal = 8.dp, vertical = 6.dp)
                                    ) {
                                        Text(
                                            text = label,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = TextPrimary
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Message Input Field
                    Text(
                        text = "3. BROADCAST MESSAGE & ANNOUNCEMENT SCRIPT:",
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    OutlinedTextField(
                        value = broadcastMsgInput,
                        onValueChange = { broadcastMsgInput = it },
                        placeholder = { Text("Enter emergency broadcast alert text...", fontSize = 12.sp, color = TextSecondary) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("broadcast_message_input"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = CrisisRed,
                            unfocusedBorderColor = Color(0xFF334155),
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        ),
                        shape = RoundedCornerShape(12.dp),
                        minLines = 3,
                        maxLines = 5
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Live Broadcast Preview Badge
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(DarkCanvas)
                            .border(1.dp, GlassBorder, RoundedCornerShape(8.dp))
                            .padding(10.dp)
                    ) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Campaign,
                                    contentDescription = null,
                                    tint = if (selectedTargetFloor == null) CrisisRed else WarningAmber,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (selectedTargetFloor == null) "LIVE TARGET: BUILDING-WIDE (ALL FLOORS)" else "LIVE TARGET: LEVEL 0$selectedTargetFloor ONLY",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (selectedTargetFloor == null) CrisisRed else WarningAmber
                                )
                            }
                            if (isAudioPaEnabled) {
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "🔊 Audio PA chime tone + Text-to-Speech voice announcement will be broadcasted to recipients.",
                                    fontSize = 9.5.sp,
                                    color = TacticalCyan
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Action Buttons (Test PA Audio vs Dispatch Broadcast)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Test Audio Button
                        OutlinedButton(
                            onClick = {
                                if (broadcastMsgInput.isNotBlank()) {
                                    isTestingAudio = true
                                    ttsHelper.playPaChimeAndSpeak(broadcastMsgInput, "English") {
                                        isTestingAudio = false
                                    }
                                }
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("test_pa_audio_button"),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = TacticalCyan),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(
                                imageVector = if (isTestingAudio) Icons.Default.VolumeUp else Icons.Default.PlayArrow,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(if (isTestingAudio) "TESTING..." else "TEST PA AUDIO", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }

                        // Dispatch Mass Broadcast Button
                        Button(
                            onClick = {
                                if (broadcastMsgInput.isNotBlank()) {
                                    scope.launch {
                                        val targetStr = if (selectedTargetFloor == null) "all" else "floor_$selectedTargetFloor"
                                        repository.publishBroadcast(
                                            message = broadcastMsgInput,
                                            priority = broadcastPriority,
                                            target = targetStr,
                                            targetFloor = selectedTargetFloor,
                                            hasAudio = isAudioPaEnabled,
                                            audioTtsText = broadcastMsgInput,
                                            senderTitle = "Incident Commander"
                                        )

                                        if (isAudioPaEnabled) {
                                            ttsHelper.playPaChimeAndSpeak(broadcastMsgInput, "English")
                                        }

                                        val targetDesc = if (selectedTargetFloor == null) "All Floors (Building-Wide)" else "Level 0$selectedTargetFloor Units"
                                        statusMessage = "🚀 Mass PA Broadcast transmitted to $targetDesc!"
                                    }
                                }
                            },
                            modifier = Modifier
                                .weight(1.4f)
                                .testTag("publish_broadcast_button"),
                            colors = ButtonDefaults.buttonColors(containerColor = CrisisRed, contentColor = Color.White),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Campaign, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("TRANSMIT PA ALERT", fontSize = 12.sp, fontWeight = FontWeight.Black)
                        }
                    }

                    // Transmission History Log
                    if (broadcasts.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            text = "TRANSMISSION AUDIT LOG (${broadcasts.size} ALERTS):",
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextSecondary
                        )
                        Spacer(modifier = Modifier.height(6.dp))

                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            broadcasts.take(4).forEach { bc ->
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(GlassSurface)
                                        .border(1.dp, GlassBorder, RoundedCornerShape(8.dp))
                                        .padding(8.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                val targetLabel = if (bc.targetFloor != null) "FL 0${bc.targetFloor} ONLY" else "BUILDING-WIDE"
                                                Box(
                                                    modifier = Modifier
                                                        .clip(RoundedCornerShape(4.dp))
                                                        .background(if (bc.targetFloor != null) WarningAmber.copy(alpha = 0.2f) else CrisisRed.copy(alpha = 0.2f))
                                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                                ) {
                                                    Text(
                                                        text = targetLabel,
                                                        fontSize = 9.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = if (bc.targetFloor != null) WarningAmber else CrisisRed
                                                    )
                                                }
                                                if (bc.hasAudio) {
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Box(
                                                        modifier = Modifier
                                                            .clip(RoundedCornerShape(4.dp))
                                                            .background(TacticalCyan.copy(alpha = 0.2f))
                                                            .padding(horizontal = 4.dp, vertical = 2.dp)
                                                    ) {
                                                        Text(
                                                            text = "🔊 AUDIO",
                                                            fontSize = 8.5.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            color = TacticalCyan
                                                        )
                                                    }
                                                }
                                            }
                                            Spacer(modifier = Modifier.height(3.dp))
                                            Text(
                                                text = bc.message,
                                                fontSize = 11.sp,
                                                color = TextPrimary,
                                                maxLines = 2
                                            )
                                        }

                                        IconButton(
                                            onClick = {
                                                ttsHelper.playPaChimeAndSpeak(bc.message, "English")
                                            },
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.VolumeUp,
                                                contentDescription = "Replay",
                                                tint = TacticalCyan,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    if (statusMessage.isNotBlank()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(text = statusMessage, fontSize = 11.sp, color = SafeGreen, fontWeight = FontWeight.Bold)
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
