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
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
    val broadcasts by repository.getAllBroadcasts().collectAsState(initial = emptyList())
    val messages by repository.getChatMessagesForRoom(roomId).collectAsState(initial = emptyList())
    val nodes by repository.getFloorNodes(4).collectAsState(initial = emptyList())
    val floorPlan by repository.getFloorPlan(4).collectAsState(initial = null)

    var isSosActive by remember { mutableStateOf(false) }
    var selectedLanguage by remember { mutableStateOf("Spanish") }
    var showLangPicker by remember { mutableStateOf(false) }
    var translatedBroadcastText by remember { mutableStateOf("") }

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

            // Multilingual Broadcast Ticker Bar
            val latestBc = broadcasts.firstOrNull()?.message ?: "Fire crews actively clearing Level 4 East Stairwell. Stay inside room."
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(GlassSurface)
                    .border(1.dp, GlassBorder, RoundedCornerShape(12.dp))
                    .padding(12.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Campaign,
                        contentDescription = "Broadcast",
                        tint = WarningAmber,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "LATEST BROADCAST ALERT ($selectedLanguage TRANSLATED):",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = WarningAmber
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = latestBc,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = TextPrimary
                        )
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
