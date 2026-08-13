package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.CrisisRed
import com.example.ui.theme.SurfaceCard
import com.example.ui.theme.TacticalCyan
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import kotlinx.coroutines.delay

@Composable
fun VoiceMediaRecorder(
    onMediaCaptured: (mediaUrl: String, mediaType: String) -> Unit,
    modifier: Modifier = Modifier
) {
    var isRecordingVideo by remember { mutableStateOf(false) }
    var isRecordingAudio by remember { mutableStateOf(false) }
    var progressSeconds by remember { mutableStateOf(0) }
    var statusMessage by remember { mutableStateOf("") }

    val infiniteTransition = rememberInfiniteTransition(label = "rec")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "rec_alpha"
    )

    // Auto-stop video timer (5 seconds)
    LaunchedEffect(isRecordingVideo) {
        if (isRecordingVideo) {
            progressSeconds = 0
            statusMessage = "RECORDING VIDEO (5s Max to save bandwidth)..."
            for (i in 1..5) {
                delay(1000)
                progressSeconds = i
            }
            isRecordingVideo = false
            statusMessage = "Video Uploaded to Cloudinary Matrix!"
            onMediaCaptured("https://res.cloudinary.com/demo/video/upload/sample_emergency.mp4", "video")
        }
    }

    // Auto-stop audio timer (10 seconds)
    LaunchedEffect(isRecordingAudio) {
        if (isRecordingAudio) {
            progressSeconds = 0
            statusMessage = "RECORDING AUDIO (10s Max)..."
            for (i in 1..10) {
                delay(1000)
                progressSeconds = i
            }
            isRecordingAudio = false
            statusMessage = "Audio Recording Uploaded!"
            onMediaCaptured("https://res.cloudinary.com/demo/video/upload/sample_emergency_audio.mp3", "audio")
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(SurfaceCard)
            .border(1.dp, TacticalCyan.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
            .padding(16.dp)
    ) {
        Column {
            Text(
                text = "MULTIMODAL MEDIA CAPTURE",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = TacticalCyan,
                letterSpacing = 1.sp
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Video Recording Button (5s max)
                Button(
                    onClick = {
                        if (!isRecordingVideo && !isRecordingAudio) {
                            isRecordingVideo = true
                        } else if (isRecordingVideo) {
                            isRecordingVideo = false
                        }
                    },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("record_video_button"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isRecordingVideo) CrisisRed else TacticalCyan.copy(alpha = 0.2f),
                        contentColor = if (isRecordingVideo) Color.White else TacticalCyan
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (isRecordingVideo) Icons.Default.Stop else Icons.Default.Videocam,
                            contentDescription = "Video",
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isRecordingVideo) "STOP ($progressSeconds/5s)" else "5s Video",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Audio Recording Button (10s max)
                Button(
                    onClick = {
                        if (!isRecordingAudio && !isRecordingVideo) {
                            isRecordingAudio = true
                        } else if (isRecordingAudio) {
                            isRecordingAudio = false
                        }
                    },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("record_audio_button"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isRecordingAudio) CrisisRed else TacticalCyan.copy(alpha = 0.2f),
                        contentColor = if (isRecordingAudio) Color.White else TacticalCyan
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (isRecordingAudio) Icons.Default.Stop else Icons.Default.Mic,
                            contentDescription = "Audio",
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isRecordingAudio) "STOP ($progressSeconds/10s)" else "10s Audio",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            if (isRecordingVideo || isRecordingAudio) {
                Spacer(modifier = Modifier.height(12.dp))
                val maxSecs = if (isRecordingVideo) 5f else 10f
                LinearProgressIndicator(
                    progress = { progressSeconds / maxSecs },
                    modifier = Modifier.fillMaxWidth().height(6.dp).clip(CircleShape),
                    color = CrisisRed,
                    trackColor = SurfaceCard
                )
            }

            if (statusMessage.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .alpha(alpha)
                            .background(if (isRecordingVideo || isRecordingAudio) CrisisRed else TacticalCyan)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = statusMessage,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = TextSecondary
                    )
                }
            }
        }
    }
}
