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
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Emergency
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.SupportAgent
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.runtime.DisposableEffect
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import com.example.util.HazardClusterer
import com.example.util.PiiScrubber
import com.example.util.TextToSpeechHelper
import kotlinx.coroutines.launch

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
    val floorPlan by repository.getFloorPlan(4).collectAsState(initial = null)

    var selectedRoomId by remember { mutableStateOf("402") }
    val messages by repository.getChatMessagesForRoom(selectedRoomId).collectAsState(initial = emptyList())

    var responderMsgInput by remember { mutableStateOf("") }
    var responderTargetLang by remember { mutableStateOf("Spanish") }
    var showLangPicker by remember { mutableStateOf(false) }
    var piiCount by remember { mutableStateOf(0) }

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
                Row(verticalAlignment = Alignment.CenterVertically) {
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
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "FIRST RESPONDER BRIDGE",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Black,
                        color = CrisisRed
                    )
                }

                // PII Scrubber Active Badge
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(SafeGreen.copy(alpha = 0.15f))
                        .border(1.dp, SafeGreen.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Security,
                        contentDescription = "PII Shield",
                        tint = SafeGreen,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "PII SCRUBBED ($piiCount)",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = SafeGreen
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

            // Tactical Floor Evacuation & Custom Map for Responders
            TacticalEvacuationMap(
                roomId = selectedRoomId,
                floor = 4,
                nodes = nodes,
                floorPlanUrl = floorPlan?.imageUrl
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
        }
    }
}
