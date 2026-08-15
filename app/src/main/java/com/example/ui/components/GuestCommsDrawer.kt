package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.SupportAgent
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ChatMessage
import com.example.data.remote.GeminiService
import com.example.ui.theme.CrisisRed
import com.example.ui.theme.SafeGreen
import com.example.ui.theme.SurfaceCard
import com.example.ui.theme.TacticalCyan
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.WarningAmber
import com.example.util.TextToSpeechHelper
import kotlinx.coroutines.launch

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun GuestCommsDrawer(
    roomId: String,
    messages: List<ChatMessage>,
    targetLanguage: String,
    onSendMessage: (text: String) -> Unit,
    onSendChipStatus: (chipLabel: String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var textInput by remember { mutableStateOf("") }
    var isPttHolding by remember { mutableStateOf(false) }
    var showVoiceDialog by remember { mutableStateOf(false) }

    // TextToSpeech Engine
    val ttsHelper = remember { TextToSpeechHelper(context) }
    DisposableEffect(Unit) {
        onDispose {
            ttsHelper.shutdown()
        }
    }

    val quickChips = listOf(
        "Heavy Smoke 🔥",
        "Door Blocked 🚪",
        "Injured Person 🤕",
        "Multiple People 👥",
        "Water Rising 🌊"
    )

    val sampleVoiceTranscripts = listOf(
        "2 people trapped in Room $roomId near window. Smoke is filling room rapidly.",
        "Door blocked by fallen beam, need crowbar or rescue squad for Room $roomId.",
        "Water level rising on floor 4 corridor, elderly guest needs evacuation assistance.",
        "Room $roomId safe and clear, waiting for responder entry command."
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("guest_comms_drawer")
            .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
            .border(1.dp, TacticalCyan.copy(alpha = 0.4f), RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)),
        colors = CardDefaults.cardColors(containerColor = SurfaceCard)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.SupportAgent,
                        contentDescription = "Responder Comms",
                        tint = TacticalCyan,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "LIVE RESPONDER BRIDGE (RM $roomId)",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = TacticalCyan
                    )
                }

                if (targetLanguage.isNotBlank() && targetLanguage != "English") {
                    Text(
                        text = "🌐 $targetLanguage TTS",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Quick Status Chips
            Text(
                text = "QUICK STATUS CHIPS (1-TAP TRANSMIT):",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = TextSecondary
            )

            Spacer(modifier = Modifier.height(6.dp))

            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                quickChips.forEach { chip ->
                    Box(
                        modifier = Modifier
                            .testTag("chip_${chip.take(5)}")
                            .clip(RoundedCornerShape(16.dp))
                            .background(CrisisRed.copy(alpha = 0.15f))
                            .border(1.dp, CrisisRed.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                            .clickable { onSendChipStatus(chip) }
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = chip,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Chat Messages List with TTS conversion
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF0F172A))
                    .padding(8.dp),
                reverseLayout = true
            ) {
                items(messages.reversed()) { msg ->
                    val isUser = msg.senderRole == "guest"
                    val isSystem = msg.senderRole == "system"

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalAlignment = if (isSystem) Alignment.CenterHorizontally else if (isUser) Alignment.End else Alignment.Start
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    when {
                                        isSystem -> Color(0xFF334155)
                                        isUser -> TacticalCyan.copy(alpha = 0.25f)
                                        else -> CrisisRed.copy(alpha = 0.25f)
                                    }
                                )
                                .border(
                                    1.dp,
                                    when {
                                        isSystem -> Color(0xFF475569)
                                        isUser -> TacticalCyan.copy(alpha = 0.5f)
                                        else -> CrisisRed.copy(alpha = 0.5f)
                                    },
                                    RoundedCornerShape(12.dp)
                                )
                                .padding(10.dp)
                        ) {
                            val liveTranslation = remember(msg.text, targetLanguage) {
                                if (targetLanguage.equals("English", ignoreCase = true)) null
                                else com.example.util.EmergencyTranslator.translate(msg.text, targetLanguage)
                            }
                            val textToPlay = liveTranslation ?: msg.text
                            Column {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = when {
                                            isSystem -> "[SYSTEM ALERT]"
                                            isUser -> "You (Room $roomId)"
                                            else -> "First Responder Squad"
                                        },
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isUser) TacticalCyan else if (isSystem) TextSecondary else CrisisRed
                                    )

                                    // TTS Voice Conversion Button in Responder Language
                                    Row(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(TacticalCyan.copy(alpha = 0.2f))
                                            .clickable {
                                                ttsHelper.speak(textToPlay, targetLanguage)
                                            }
                                            .padding(horizontal = 6.dp, vertical = 2.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.VolumeUp,
                                            contentDescription = "Speak TTS",
                                            tint = TacticalCyan,
                                            modifier = Modifier.size(12.dp)
                                        )
                                        Spacer(modifier = Modifier.width(3.dp))
                                        Text(
                                            text = "🔊 TTS (${targetLanguage.take(3)})",
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = TacticalCyan
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(3.dp))
                                Text(
                                    text = msg.text,
                                    fontSize = 12.sp,
                                    color = TextPrimary
                                )

                                if (!liveTranslation.isNullOrBlank() && !liveTranslation.equals(msg.text, ignoreCase = true)) {
                                    Spacer(modifier = Modifier.height(3.dp))
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(Color(0xFF1E293B))
                                            .padding(6.dp)
                                    ) {
                                        Text(
                                            text = "🌐 TRANSLATED ($targetLanguage): $liveTranslation",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = SafeGreen
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Input Row & Voice STT Converter
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Voice Speech-To-Text Button
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("push_to_talk_button")
                        .clip(CircleShape)
                        .background(if (isPttHolding) CrisisRed else TacticalCyan.copy(alpha = 0.2f))
                        .border(
                            1.dp,
                            if (isPttHolding) CrisisRed else TacticalCyan,
                            CircleShape
                        )
                        .clickable { showVoiceDialog = true },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.RecordVoiceOver,
                        contentDescription = "Voice STT",
                        tint = if (isPttHolding) Color.White else TacticalCyan,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Text Input
                OutlinedTextField(
                    value = textInput,
                    onValueChange = { textInput = it },
                    placeholder = { Text("Tap 🎙️ Voice STT or type text...", fontSize = 12.sp, color = TextSecondary) },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("chat_input_field"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = TacticalCyan,
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
                        if (textInput.isNotBlank()) {
                            onSendMessage(textInput)
                            textInput = ""
                        }
                    },
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("chat_send_button")
                        .clip(CircleShape)
                        .background(TacticalCyan)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Send",
                        tint = Color.Black,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }

    // Voice Speech-To-Text & Translation Modal Dialog
    if (showVoiceDialog) {
        var selectedSampleIndex by remember { mutableStateOf(0) }
        var transcribedText by remember { mutableStateOf(sampleVoiceTranscripts[0]) }
        var translatedPreviewText by remember { mutableStateOf("") }
        var isTranslating by remember { mutableStateOf(false) }

        LaunchedEffect(transcribedText, targetLanguage) {
            if (transcribedText.isNotBlank()) {
                isTranslating = true
                translatedPreviewText = GeminiService.translateText(
                    text = transcribedText,
                    targetLanguage = targetLanguage
                )
                isTranslating = false
            }
        }

        AlertDialog(
            onDismissRequest = { showVoiceDialog = false },
            containerColor = SurfaceCard,
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.RecordVoiceOver,
                        contentDescription = null,
                        tint = TacticalCyan,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "SPEECH-TO-TEXT & TRANSLATION ENGINE",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = TacticalCyan
                    )
                }
            },
            text = {
                Column {
                    Text(
                        text = "1. VOICE SPEECH-TO-TEXT TRANSCRIPTION:",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextSecondary
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    OutlinedTextField(
                        value = transcribedText,
                        onValueChange = { transcribedText = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(90.dp),
                        label = { Text("Transcribed Text", fontSize = 11.sp) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = TacticalCyan,
                            unfocusedBorderColor = Color(0xFF334155),
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        ),
                        shape = RoundedCornerShape(10.dp)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "SELECT QUICK VOICE TEMPLATE:",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextSecondary
                    )
                    Spacer(modifier = Modifier.height(4.dp))

                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        sampleVoiceTranscripts.forEachIndexed { idx, sample ->
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (idx == selectedSampleIndex) TacticalCyan.copy(alpha = 0.2f) else Color(0xFF0F172A))
                                    .border(1.dp, if (idx == selectedSampleIndex) TacticalCyan else Color(0xFF334155), RoundedCornerShape(8.dp))
                                    .clickable {
                                        selectedSampleIndex = idx
                                        transcribedText = sample
                                    }
                                    .padding(8.dp)
                            ) {
                                Text(
                                    text = "🎙️ \"$sample\"",
                                    fontSize = 11.sp,
                                    color = TextPrimary
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "2. RESPONDER LANGUAGE TRANSLATION ENGINE ($targetLanguage):",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = WarningAmber
                    )
                    Spacer(modifier = Modifier.height(4.dp))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFF0F172A))
                            .border(1.dp, SafeGreen.copy(alpha = 0.4f), RoundedCornerShape(10.dp))
                            .padding(10.dp)
                    ) {
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Target Language: $targetLanguage",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TacticalCyan
                                )

                                Button(
                                    onClick = {
                                        ttsHelper.speak(
                                            if (translatedPreviewText.isNotBlank()) translatedPreviewText else transcribedText,
                                            targetLanguage
                                        )
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = SafeGreen, contentColor = Color.Black),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.height(30.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.VolumeUp,
                                        contentDescription = "Test TTS",
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("TEST TTS VOICE", fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                }
                            }

                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = if (isTranslating) "Translating via Gemini Engine..." else translatedPreviewText,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = TextPrimary
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (transcribedText.isNotBlank()) {
                            onSendMessage(transcribedText)
                            showVoiceDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = TacticalCyan, contentColor = Color.Black),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("TRANSMIT CONVERTED VOICE TEXT", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showVoiceDialog = false }) {
                    Text("CANCEL", color = TextSecondary, fontSize = 11.sp)
                }
            }
        )
    }
}

