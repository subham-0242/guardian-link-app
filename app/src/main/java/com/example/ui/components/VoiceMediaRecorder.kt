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
import androidx.compose.material.icons.filled.CloudUpload
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
import androidx.compose.runtime.rememberCoroutineScope
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
import com.example.data.remote.CloudinaryNetworkClient
import com.example.ui.theme.CrisisRed
import com.example.ui.theme.SafeGreen
import com.example.ui.theme.SurfaceCard
import com.example.ui.theme.TacticalCyan
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun VoiceMediaRecorder(
    onMediaCaptured: (mediaUrl: String, mediaType: String) -> Unit,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    var isRecordingVideo by remember { mutableStateOf(false) }
    var isRecordingAudio by remember { mutableStateOf(false) }
    var isUploading by remember { mutableStateOf(false) }
    var progressSeconds by remember { mutableStateOf(0) }
    var statusMessage by remember { mutableStateOf("") }
    var uploadedUrl by remember { mutableStateOf<String?>(null) }

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

    // Auto-stop video timer (5 seconds constraint)
    LaunchedEffect(isRecordingVideo) {
        if (isRecordingVideo) {
            progressSeconds = 0
            uploadedUrl = null
            statusMessage = "RECORDING VIDEO (5s Max constraint)..."
            for (i in 1..5) {
                delay(1000)
                progressSeconds = i
            }
            isRecordingVideo = false
            isUploading = true
            statusMessage = "UPLOADING VIDEO TO CLOUDINARY via Retrofit..."

            // Simulate captured video byte buffer (e.g. 5s H.264 video buffer)
            val dummyVideoBytes = ByteArray(1024 * 50) { 0x01 }
            val result = CloudinaryNetworkClient.uploadMediaBytes(
                bytes = dummyVideoBytes,
                mediaType = "video",
                fileName = "video_report_${System.currentTimeMillis()}",
                durationSeconds = 5.0
            )

            isUploading = false
            result.onSuccess { response ->
                val finalUrl = response.secureUrl ?: response.url ?: ""
                uploadedUrl = finalUrl
                statusMessage = "Cloudinary Secure Upload OK (Duration: ${response.duration ?: 5.0}s)"
                onMediaCaptured(finalUrl, "video")
            }.onFailure { err ->
                statusMessage = "Upload Failed: ${err.message}"
            }
        }
    }

    // Auto-stop audio timer (10 seconds constraint)
    LaunchedEffect(isRecordingAudio) {
        if (isRecordingAudio) {
            progressSeconds = 0
            uploadedUrl = null
            statusMessage = "RECORDING AUDIO (10s Max constraint)..."
            for (i in 1..10) {
                delay(1000)
                progressSeconds = i
            }
            isRecordingAudio = false
            isUploading = true
            statusMessage = "UPLOADING AUDIO TO CLOUDINARY via Retrofit..."

            // Simulate captured audio byte buffer (e.g. 10s AAC audio buffer)
            val dummyAudioBytes = ByteArray(1024 * 20) { 0x02 }
            val result = CloudinaryNetworkClient.uploadMediaBytes(
                bytes = dummyAudioBytes,
                mediaType = "audio",
                fileName = "audio_report_${System.currentTimeMillis()}",
                durationSeconds = 10.0
            )

            isUploading = false
            result.onSuccess { response ->
                val finalUrl = response.secureUrl ?: response.url ?: ""
                uploadedUrl = finalUrl
                statusMessage = "Cloudinary Secure Upload OK (Duration: ${response.duration ?: 10.0}s)"
                onMediaCaptured(finalUrl, "audio")
            }.onFailure { err ->
                statusMessage = "Upload Failed: ${err.message}"
            }
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
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "CLOUDINARY MEDIA INCIDENT REPORTER",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = TacticalCyan,
                    letterSpacing = 0.5.sp
                )
                Text(
                    text = "5s Video / 10s Audio Limit",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextSecondary
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Video Recording Button (5s max)
                Button(
                    onClick = {
                        if (!isRecordingVideo && !isRecordingAudio && !isUploading) {
                            isRecordingVideo = true
                        } else if (isRecordingVideo) {
                            isRecordingVideo = false
                        }
                    },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("record_video_button"),
                    enabled = !isUploading,
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
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Audio Recording Button (10s max)
                Button(
                    onClick = {
                        if (!isRecordingAudio && !isRecordingVideo && !isUploading) {
                            isRecordingAudio = true
                        } else if (isRecordingAudio) {
                            isRecordingAudio = false
                        }
                    },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("record_audio_button"),
                    enabled = !isUploading,
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
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            if (isRecordingVideo || isRecordingAudio || isUploading) {
                Spacer(modifier = Modifier.height(12.dp))
                val maxSecs = if (isRecordingVideo) 5f else 10f
                LinearProgressIndicator(
                    progress = { if (isUploading) 1f else progressSeconds / maxSecs },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(CircleShape),
                    color = if (isUploading) SafeGreen else CrisisRed,
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
                            .alpha(if (isRecordingVideo || isRecordingAudio || isUploading) alpha else 1.0f)
                            .background(
                                when {
                                    isRecordingVideo || isRecordingAudio -> CrisisRed
                                    isUploading -> TacticalCyan
                                    uploadedUrl != null -> SafeGreen
                                    else -> TextSecondary
                                }
                            )
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = statusMessage,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = if (uploadedUrl != null) SafeGreen else TextSecondary
                    )
                }
            }

            if (uploadedUrl != null) {
                Spacer(modifier = Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.CloudUpload,
                        contentDescription = "Cloudinary",
                        tint = SafeGreen,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = uploadedUrl!!,
                        fontSize = 10.sp,
                        color = TacticalCyan,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

