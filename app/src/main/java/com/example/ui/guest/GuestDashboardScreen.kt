package com.example.ui.guest

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Emergency
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
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
import com.example.data.remote.GeminiService
import com.example.data.repository.EmergencyRepository
import com.example.ui.components.ActionBanner
import com.example.ui.components.GuestCommsDrawer
import com.example.ui.components.PulsingHeader
import com.example.ui.components.TacticalEvacuationMap
import com.example.ui.components.VoiceMediaRecorder
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

@Composable
fun GuestDashboardScreen(
    roomId: String = "402",
    onBackToHome: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val repository = remember { EmergencyRepository(context) }
    val scope = rememberCoroutineScope()

    val guestState by repository.getGuestForRoom(roomId).collectAsState(initial = null)
    val broadcasts by repository.getBroadcastsForFloor(4).collectAsState(initial = emptyList())
    val messages by repository.getChatMessagesForRoom(roomId).collectAsState(initial = emptyList())
    val nodes by repository.getFloorNodes(4).collectAsState(initial = emptyList())
    val floorPlan by repository.getFloorPlan(4).collectAsState(initial = null)

    var isSosActive by remember { mutableStateOf(false) }
    var selectedLanguage by remember { mutableStateOf("Spanish") }
    var showLangPicker by remember { mutableStateOf(false) }
    var translatedBroadcastText by remember { mutableStateOf("") }
    var isPlayingPaAudio by remember { mutableStateOf(false) }
    var isAcknowledged by remember { mutableStateOf(false) }

    val ttsHelper = remember { TextToSpeechHelper(context) }
    DisposableEffect(Unit) {
        onDispose {
            ttsHelper.shutdown()
        }
    }

    val latestBroadcast = broadcasts.firstOrNull()
    val rawBroadcastMsg = latestBroadcast?.message ?: "ATTENTION FLOOR 4: West stairwell blocked. Evacuate via East exit only."

    LaunchedEffect(latestBroadcast, selectedLanguage) {
        if (selectedLanguage.equals("English", ignoreCase = true)) {
            translatedBroadcastText = rawBroadcastMsg
        } else {
            try {
                val translated = GeminiService.translateText(rawBroadcastMsg, selectedLanguage)
                translatedBroadcastText = translated
            } catch (e: Exception) {
                translatedBroadcastText = rawBroadcastMsg
            }
        }
    }

    val scrollState = rememberScrollState()

    val supportedLanguages = listOf("Spanish", "French", "Mandarin", "Arabic", "Russian", "Hindi", "Japanese", "Tamil")

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
            // Navigation Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(
                    onClick = onBackToHome,
                    modifier = Modifier.testTag("back_to_home_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Back",
                        tint = TacticalCyan
                    )
                }

                // Language Picker Badge
                Box {
                    Row(
                        modifier = Modifier
                            .testTag("language_picker_dropdown")
                            .clip(RoundedCornerShape(20.dp))
                            .background(GlassSurface)
                            .border(1.dp, GlassBorder, RoundedCornerShape(20.dp))
                            .clickable { showLangPicker = true }
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Translate,
                            contentDescription = "Language",
                            tint = TacticalCyan,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = selectedLanguage,
                            fontSize = 12.sp,
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
                                    selectedLanguage = lang
                                    showLangPicker = false
                                }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Pulsing Header
            PulsingHeader(
                roomId = roomId,
                isSynced = true,
                roleTitle = "Guest Dashboard"
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Action Banner
            ActionBanner()

            Spacer(modifier = Modifier.height(14.dp))

            // Mass Emergency Broadcast / PA System Alert Card
            val isFloor4Targeted = latestBroadcast?.targetFloor == 4
            val targetScopeLabel = when {
                latestBroadcast?.targetFloor != null -> "DIRECTED TO FLOOR 0${latestBroadcast.targetFloor}"
                else -> "BUILDING-WIDE EVACUATION BROADCAST"
            }
            val broadcastDisplayText = if (translatedBroadcastText.isNotBlank()) translatedBroadcastText else rawBroadcastMsg

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("guest_pa_broadcast_card")
                    .clip(RoundedCornerShape(16.dp))
                    .border(
                        1.5.dp,
                        if (isFloor4Targeted) CrisisRed else WarningAmber,
                        RoundedCornerShape(16.dp)
                    ),
                colors = CardDefaults.cardColors(containerColor = SurfaceCard)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    // Header Bar
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
                                    .background(if (isFloor4Targeted) CrisisRed.copy(alpha = 0.2f) else WarningAmber.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Campaign,
                                    contentDescription = "PA Alert",
                                    tint = if (isFloor4Targeted) CrisisRed else WarningAmber,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "🚨 MASS PA EMERGENCY ALERT",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Black,
                                    color = if (isFloor4Targeted) CrisisRed else WarningAmber
                                )
                                Text(
                                    text = targetScopeLabel,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TacticalCyan
                                )
                            }
                        }

                        // Priority Pill
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (isFloor4Targeted) CrisisRed else WarningAmber)
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = if (latestBroadcast?.priority?.uppercase() == "CRITICAL") "CRITICAL" else "URGENT",
                                fontSize = 8.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Broadcast Message Text
                    Text(
                        text = broadcastDisplayText,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary,
                        lineHeight = 18.sp
                    )

                    // English reference if translated
                    if (!selectedLanguage.equals("English", ignoreCase = true) && rawBroadcastMsg != broadcastDisplayText) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Original (EN): $rawBroadcastMsg",
                            fontSize = 10.5.sp,
                            color = TextSecondary
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Audio PA Announcement Player & Acknowledgment Controls
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Play / Stop PA Audio Button
                        Button(
                            onClick = {
                                if (isPlayingPaAudio) {
                                    ttsHelper.stop()
                                    isPlayingPaAudio = false
                                } else {
                                    isPlayingPaAudio = true
                                    ttsHelper.playPaChimeAndSpeak(
                                        text = broadcastDisplayText,
                                        languageName = selectedLanguage
                                    ) {
                                        isPlayingPaAudio = false
                                    }
                                }
                            },
                            modifier = Modifier
                                .weight(1.3f)
                                .testTag("play_pa_audio_button"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isPlayingPaAudio) CrisisRed else TacticalCyan,
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(
                                imageVector = if (isPlayingPaAudio) Icons.Default.Stop else Icons.Default.VolumeUp,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isPlayingPaAudio) "STOP AUDIO" else "🔊 PLAY PA AUDIO ($selectedLanguage)",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        // Acknowledge Button
                        OutlinedButton(
                            onClick = { isAcknowledged = !isAcknowledged },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("acknowledge_broadcast_button"),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = if (isAcknowledged) SafeGreen else TextSecondary
                            ),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = if (isAcknowledged) SafeGreen else TextSecondary,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (isAcknowledged) "DELIVERED" else "ACKNOWLEDGE",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Tactical Floor Evacuation Map
            TacticalEvacuationMap(
                roomId = roomId,
                floor = 4,
                nodes = nodes,
                floorPlanUrl = floorPlan?.imageUrl
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Crisis Action Buttons (I AM SAFE vs SEND SOS)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // I AM SAFE Button
                Button(
                    onClick = {
                        isSosActive = false
                        scope.launch {
                            repository.triggerSafe(roomId, 4)
                        }
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(64.dp)
                        .testTag("i_am_safe_button"),
                    colors = ButtonDefaults.buttonColors(containerColor = SafeGreen, contentColor = Color.White),
                    shape = RoundedCornerShape(16.dp),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 6.dp)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "I AM SAFE",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 0.5.sp
                            )
                        }
                        Text(
                            text = "Resolve Alert",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White.copy(alpha = 0.8f)
                        )
                    }
                }

                // SEND SOS Button
                Button(
                    onClick = {
                        isSosActive = true
                        scope.launch {
                            repository.triggerSos(roomId, 4, "SOS Distress Flagged in Room $roomId. Heavy smoke in corridor.")
                        }
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(64.dp)
                        .testTag("send_sos_button"),
                    colors = ButtonDefaults.buttonColors(containerColor = CrisisRed, contentColor = Color.White),
                    shape = RoundedCornerShape(16.dp),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 6.dp)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Emergency,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "SEND SOS",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 0.5.sp
                            )
                        }
                        Text(
                            text = "Request Rescue",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White.copy(alpha = 0.8f)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Multimodal Media Capture (Video & Audio)
            VoiceMediaRecorder(
                onMediaCaptured = { url, type ->
                    scope.launch {
                        repository.attachMedia(roomId, 4, url, type)
                    }
                }
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Guest Comms Drawer
            GuestCommsDrawer(
                roomId = roomId,
                messages = messages,
                targetLanguage = selectedLanguage,
                onSendMessage = { text ->
                    scope.launch {
                        repository.sendChatMessage(roomId, "guest", text, selectedLanguage)
                    }
                },
                onSendChipStatus = { chip ->
                    scope.launch {
                        repository.sendChatMessage(roomId, "guest", "Status Tagged: $chip", selectedLanguage)
                        repository.triggerSos(roomId, 4, "Status Chip: $chip", listOf(chip))
                    }
                }
            )
        }
    }
}
