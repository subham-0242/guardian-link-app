package com.example.ui.components

import android.media.MediaPlayer
import android.util.Log
import android.widget.VideoView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.ui.theme.CrisisRed
import com.example.ui.theme.GlassBorder
import com.example.ui.theme.SafeGreen
import com.example.ui.theme.SurfaceCard
import com.example.ui.theme.TacticalCyan
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.WarningAmber
import kotlinx.coroutines.delay
import java.io.File

/**
 * Encapsulates an uploaded media item for tactical viewing.
 */
data class RoomMediaItem(
    val id: String,
    val roomId: String,
    val mediaUrl: String?,
    val mediaType: String, // "audio" or "video"
    val isSuccess: Boolean = true,
    val errorMessage: String? = null,
    val durationSeconds: Double = 5.0,
    val localUri: String? = null,
    val timestamp: Long = System.currentTimeMillis(),
    val title: String? = null
)

/**
 * Tactical Media Card displayed at the bottom of Responder Triage.
 * - Video: Click to open in Full Screen and play automatically.
 * - Audio: Plays directly within the card with live wave visualizer and controls.
 * - Failed uploads: Shows only the failure message.
 */
@Composable
fun RoomMediaIntelCard(
    roomId: String,
    mediaItems: List<RoomMediaItem>,
    modifier: Modifier = Modifier
) {
    var fullscreenVideoItem by remember { mutableStateOf<RoomMediaItem?>(null) }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("room_media_intel_card_${roomId}")
            .clip(RoundedCornerShape(16.dp))
            .border(1.2.dp, TacticalCyan.copy(alpha = 0.5f), RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(containerColor = SurfaceCard)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Card Header
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
                            .background(TacticalCyan.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Videocam,
                            contentDescription = "Media Intel",
                            tint = TacticalCyan,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "ROOM $roomId ATTACHED MEDIA & SURVEILLANCE",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Black,
                            color = TacticalCyan
                        )
                        Text(
                            text = "Audio plays directly • Click video to view Full Screen",
                            fontSize = 9.sp,
                            color = TextSecondary
                        )
                    }
                }

                val successfulCount = mediaItems.count { it.isSuccess && !it.mediaUrl.isNullOrBlank() }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (successfulCount > 0) SafeGreen.copy(alpha = 0.2f) else WarningAmber.copy(alpha = 0.2f))
                        .border(1.dp, if (successfulCount > 0) SafeGreen.copy(alpha = 0.5f) else WarningAmber.copy(alpha = 0.5f), RoundedCornerShape(6.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "$successfulCount FEED${if (successfulCount != 1) "S" else ""}",
                        fontSize = 8.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (successfulCount > 0) SafeGreen else WarningAmber
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (mediaItems.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFF0F172A))
                        .padding(14.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No media files uploaded from Room $roomId yet.",
                        fontSize = 11.sp,
                        color = TextSecondary
                    )
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    mediaItems.forEach { item ->
                        val isVideo = item.mediaType.contains("video", ignoreCase = true)
                        if (!item.isSuccess || item.mediaUrl.isNullOrBlank()) {
                            // FAILED UPLOAD: display failure message only
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(CrisisRed.copy(alpha = 0.12f))
                                    .border(1.dp, CrisisRed.copy(alpha = 0.35f), RoundedCornerShape(10.dp))
                                    .padding(10.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Warning,
                                        contentDescription = "Upload Failed",
                                        tint = CrisisRed,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = item.errorMessage ?: "Upload of ${item.mediaType} from Room $roomId failed. No media stream available.",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color.White
                                    )
                                }
                            }
                        } else if (isVideo) {
                            // VIDEO: Clickable card that triggers fullscreen playback
                            TacticalVideoThumbnailCard(
                                item = item,
                                onClick = { fullscreenVideoItem = item }
                            )
                        } else {
                            // AUDIO: Plays directly inside the card
                            TacticalAudioPlayer(
                                audioUrl = item.mediaUrl,
                                roomId = item.roomId,
                                localUri = item.localUri,
                                durationSeconds = item.durationSeconds,
                                isSuccess = true
                            )
                        }
                    }
                }
            }
        }
    }

    // Full Screen Video Dialog when clicked
    fullscreenVideoItem?.let { videoItem ->
        TacticalFullscreenVideoDialog(
            item = videoItem,
            onDismiss = { fullscreenVideoItem = null }
        )
    }
}

/**
 * Clickable Tactical Video Preview Card.
 * Clicking this opens the video in full screen and starts playing.
 */
@Composable
fun TacticalVideoThumbnailCard(
    item: RoomMediaItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF020617))
            .border(1.5.dp, CrisisRed.copy(alpha = 0.7f), RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .testTag("video_intel_thumbnail_${item.roomId}")
            .padding(12.dp)
    ) {
        Column {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(CrisisRed.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Videocam,
                            contentDescription = "Video Feed",
                            tint = CrisisRed,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "VIDEO NOTE • ROOM ${item.roomId}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = CrisisRed
                        )
                        Text(
                            text = "Duration: ${String.format(java.util.Locale.US, "%.1f", item.durationSeconds)}s • Cloudinary Verified",
                            fontSize = 8.5.sp,
                            color = SafeGreen
                        )
                    }
                }

                // Click to play full screen badge
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(CrisisRed.copy(alpha = 0.25f))
                        .border(1.dp, CrisisRed.copy(alpha = 0.6f), RoundedCornerShape(6.dp))
                        .padding(horizontal = 8.dp, vertical = 3.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Fullscreen,
                        contentDescription = "Fullscreen",
                        tint = CrisisRed,
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "CLICK FOR FULL SCREEN",
                        fontSize = 8.5.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Thumbnail / Frame Viewport
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF0F172A))
                    .border(1.dp, Color(0xFF334155), RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                // Tactical Reticle
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val w = size.width
                    val h = size.height

                    // Grid lines
                    drawLine(
                        color = TacticalCyan.copy(alpha = 0.2f),
                        start = Offset(0f, h / 2),
                        end = Offset(w, h / 2),
                        strokeWidth = 1f,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f))
                    )
                    drawLine(
                        color = TacticalCyan.copy(alpha = 0.2f),
                        start = Offset(w / 2, 0f),
                        end = Offset(w / 2, h),
                        strokeWidth = 1f,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f))
                    )

                    // Corner brackets
                    val bracketLen = 16f
                    drawLine(CrisisRed, Offset(8f, 8f), Offset(8f + bracketLen, 8f), 2f)
                    drawLine(CrisisRed, Offset(8f, 8f), Offset(8f, 8f + bracketLen), 2f)
                    drawLine(CrisisRed, Offset(w - 8f, 8f), Offset(w - 8f - bracketLen, 8f), 2f)
                    drawLine(CrisisRed, Offset(w - 8f, 8f), Offset(w - 8f, 8f + bracketLen), 2f)
                    drawLine(CrisisRed, Offset(8f, h - 8f), Offset(8f + bracketLen, h - 8f), 2f)
                    drawLine(CrisisRed, Offset(8f, h - 8f), Offset(8f, h - 8f - bracketLen), 2f)
                    drawLine(CrisisRed, Offset(w - 8f, h - 8f), Offset(w - 8f - bracketLen, h - 8f), 2f)
                    drawLine(CrisisRed, Offset(w - 8f, h - 8f), Offset(w - 8f, h - 8f - bracketLen), 2f)
                }

                // HUD Overlays
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.TopCenter)
                        .padding(8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "CAM-RM${item.roomId} [RECORDED]",
                        fontSize = 8.5.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = CrisisRed
                    )
                    Text(
                        text = "00:00 / 00:0${item.durationSeconds.toInt()}",
                        fontSize = 8.5.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = Color.White
                    )
                }

                // Large Central Play Action
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(CrisisRed)
                            .border(2.dp, Color.White.copy(alpha = 0.8f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "Play Video in Fullscreen",
                            tint = Color.White,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "TAP TO PLAY FULL SCREEN",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White
                    )
                }
            }
        }
    }
}

/**
 * Full Screen Video Player Dialog that auto-starts playing immediately.
 */
@Composable
fun TacticalFullscreenVideoDialog(
    item: RoomMediaItem,
    onDismiss: () -> Unit
) {
    var isPlaying by remember { mutableStateOf(true) }
    var currentProgressSeconds by remember { mutableFloatStateOf(0f) }
    val totalSecs = item.durationSeconds.coerceIn(1.0, 10.0).toFloat()

    val infiniteTransition = rememberInfiniteTransition(label = "fullscreen_scan")
    val scanLineY by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(2000, easing = LinearEasing), RepeatMode.Restart),
        label = "scanline"
    )

    // Playback progression timer
    LaunchedEffect(isPlaying) {
        if (isPlaying) {
            while (isPlaying && currentProgressSeconds < totalSecs) {
                delay(100)
                currentProgressSeconds += 0.1f
            }
            if (currentProgressSeconds >= totalSecs) {
                currentProgressSeconds = 0f // loop
            }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF020617))
                .padding(16.dp),
            color = Color(0xFF020617)
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Header Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(CrisisRed.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Videocam,
                                contentDescription = null,
                                tint = CrisisRed,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "FULL SCREEN SURVEILLANCE • ROOM ${item.roomId}",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Black,
                                color = TextPrimary
                            )
                            Text(
                                text = "Cloudinary Stream • Playing Full Resolution",
                                fontSize = 9.5.sp,
                                color = SafeGreen
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF1E293B))
                    ) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = TextPrimary)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Big Video Viewport Frame with Tactical HUD & Scanlines
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xFF0B132B))
                        .border(2.dp, CrisisRed, RoundedCornerShape(16.dp))
                        .clickable { isPlaying = !isPlaying }
                ) {
                    // If local video exists, attach VideoView
                    if (!item.localUri.isNullOrBlank() && File(item.localUri).exists()) {
                        AndroidView(
                            factory = { ctx ->
                                VideoView(ctx).apply {
                                    setVideoPath(item.localUri)
                                    setOnPreparedListener { mp ->
                                        mp.isLooping = true
                                        if (isPlaying) start()
                                    }
                                }
                            },
                            update = { view ->
                                if (isPlaying) view.start() else view.pause()
                            },
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    // Tactical HUD Overlays & Scanline
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val w = size.width
                        val h = size.height

                        // Center Crosshair
                        drawLine(
                            color = TacticalCyan.copy(alpha = 0.35f),
                            start = Offset(0f, h / 2),
                            end = Offset(w, h / 2),
                            strokeWidth = 1.5f,
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 12f))
                        )
                        drawLine(
                            color = TacticalCyan.copy(alpha = 0.35f),
                            start = Offset(w / 2, 0f),
                            end = Offset(w / 2, h),
                            strokeWidth = 1.5f,
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 12f))
                        )

                        // Corner targeting brackets
                        val bLen = 30f
                        drawLine(CrisisRed, Offset(16f, 16f), Offset(16f + bLen, 16f), 3f)
                        drawLine(CrisisRed, Offset(16f, 16f), Offset(16f, 16f + bLen), 3f)
                        drawLine(CrisisRed, Offset(w - 16f, 16f), Offset(w - 16f - bLen, 16f), 3f)
                        drawLine(CrisisRed, Offset(w - 16f, 16f), Offset(w - 16f, 16f + bLen), 3f)
                        drawLine(CrisisRed, Offset(16f, h - 16f), Offset(16f + bLen, h - 16f), 3f)
                        drawLine(CrisisRed, Offset(16f, h - 16f), Offset(16f, h - 16f - bLen), 3f)
                        drawLine(CrisisRed, Offset(w - 16f, h - 16f), Offset(w - 16f - bLen, h - 16f), 3f)
                        drawLine(CrisisRed, Offset(w - 16f, h - 16f), Offset(w - 16f, h - 16f - bLen), 3f)

                        // Animated scanning laser line
                        if (isPlaying) {
                            drawLine(
                                color = CrisisRed.copy(alpha = 0.6f),
                                start = Offset(0f, h * scanLineY),
                                end = Offset(w, h * scanLineY),
                                strokeWidth = 3f
                            )
                        }
                    }

                    // Top HUD Telemetry
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "🔴 LIVE FEED RM-${item.roomId}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.Monospace,
                            color = CrisisRed
                        )
                        Text(
                            text = "${String.format(java.util.Locale.US, "00:%02d", currentProgressSeconds.toInt())} / ${String.format(java.util.Locale.US, "00:%02d", totalSecs.toInt())}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.Monospace,
                            color = Color.White
                        )
                    }

                    // Play/Pause Overlay indicator
                    if (!isPlaying) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.Center)
                                .size(70.dp)
                                .clip(CircleShape)
                                .background(CrisisRed.copy(alpha = 0.9f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = "Play",
                                tint = Color.White,
                                modifier = Modifier.size(40.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Bottom Timeline and Controls
                Column {
                    LinearProgressIndicator(
                        progress = { (currentProgressSeconds / totalSecs).coerceIn(0f, 1f) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(CircleShape),
                        color = CrisisRed,
                        trackColor = Color(0xFF1E293B)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = { isPlaying = !isPlaying },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = if (isPlaying) CrisisRed else TacticalCyan),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(
                                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isPlaying) "PAUSE VIDEO" else "RESUME PLAYBACK",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }

                        Button(
                            onClick = onDismiss,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF334155)),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("CLOSE", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }
}

/**
 * Direct In-Card Tactical Audio Player Component
 * Plays audio directly within the card with live wave visualizer and controls.
 */
@Composable
fun TacticalAudioPlayer(
    audioUrl: String?,
    roomId: String,
    modifier: Modifier = Modifier,
    localUri: String? = null,
    durationSeconds: Double = 5.0,
    isSuccess: Boolean = true,
    errorMessage: String? = null
) {
    val isUploadValid = isSuccess && !audioUrl.isNullOrBlank()

    if (!isUploadValid) {
        // When upload is NOT successful, display ONLY the message
        Box(
            modifier = modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(CrisisRed.copy(alpha = 0.12f))
                .border(1.dp, CrisisRed.copy(alpha = 0.35f), RoundedCornerShape(10.dp))
                .padding(10.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = "Audio Upload Failed",
                    tint = CrisisRed,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = errorMessage ?: "Audio upload from Room $roomId failed. No media stream available.",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White
                )
            }
        }
        return
    }

    // --- SUCCESSFUL AUDIO UPLOAD: Direct In-Card Player ---
    var isPlaying by remember { mutableStateOf(false) }
    var currentProgressSeconds by remember { mutableFloatStateOf(0f) }
    val totalSecs = durationSeconds.coerceIn(1.0, 10.0).toFloat()
    var isMuted by remember { mutableStateOf(false) }
    var mediaPlayer by remember { mutableStateOf<MediaPlayer?>(null) }

    val infiniteTransition = rememberInfiniteTransition(label = "audio_bars")
    val bar1 by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(tween(350, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "b1"
    )
    val bar2 by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 0.2f,
        animationSpec = infiniteRepeatable(tween(420, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "b2"
    )
    val bar3 by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 0.9f,
        animationSpec = infiniteRepeatable(tween(280, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "b3"
    )
    val bar4 by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(tween(500, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "b4"
    )

    // Release MediaPlayer on dispose
    DisposableEffect(Unit) {
        onDispose {
            try {
                mediaPlayer?.stop()
                mediaPlayer?.release()
                mediaPlayer = null
            } catch (_: Exception) {}
        }
    }

    // Playback loop controller
    LaunchedEffect(isPlaying) {
        if (isPlaying) {
            try {
                val mp = MediaPlayer()
                if (!localUri.isNullOrBlank() && File(localUri).exists()) {
                    mp.setDataSource(localUri)
                } else if (!audioUrl.isNullOrBlank() && audioUrl.startsWith("http")) {
                    mp.setDataSource(audioUrl)
                }
                mp.prepareAsync()
                mp.setOnPreparedListener {
                    if (isMuted) it.setVolume(0f, 0f) else it.setVolume(1f, 1f)
                    it.start()
                }
                mp.setOnCompletionListener {
                    isPlaying = false
                    currentProgressSeconds = 0f
                }
                mediaPlayer = mp
            } catch (e: Exception) {
                Log.w("TacticalAudioPlayer", "Audio player fallback: ${e.message}")
            }

            while (isPlaying && currentProgressSeconds < totalSecs) {
                delay(100)
                currentProgressSeconds += 0.1f
            }
            if (currentProgressSeconds >= totalSecs) {
                isPlaying = false
                currentProgressSeconds = 0f
                try {
                    mediaPlayer?.stop()
                    mediaPlayer?.release()
                    mediaPlayer = null
                } catch (_: Exception) {}
            }
        } else {
            try {
                mediaPlayer?.pause()
            } catch (_: Exception) {}
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF0B192C))
            .border(1.5.dp, TacticalCyan.copy(alpha = 0.6f), RoundedCornerShape(12.dp))
            .padding(12.dp)
    ) {
        Column {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(TacticalCyan.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Mic,
                            contentDescription = "Voice Intel",
                            tint = TacticalCyan,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "VOICE DISPATCH • ROOM $roomId",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = TacticalCyan
                        )
                        Text(
                            text = "Direct Audio Stream • Ready to Play",
                            fontSize = 8.5.sp,
                            color = SafeGreen
                        )
                    }
                }

                // Verified Badge
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(SafeGreen.copy(alpha = 0.2f))
                        .border(1.dp, SafeGreen.copy(alpha = 0.5f), RoundedCornerShape(6.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "DIRECT PLAY",
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold,
                        color = SafeGreen
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Player Controls & Waveform Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Play / Pause Button
                Button(
                    onClick = {
                        if (isPlaying) {
                            isPlaying = false
                        } else {
                            if (currentProgressSeconds >= totalSecs) {
                                currentProgressSeconds = 0f
                            }
                            isPlaying = true
                        }
                    },
                    modifier = Modifier
                        .height(36.dp)
                        .testTag("play_audio_direct_button_${roomId}"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isPlaying) CrisisRed else TacticalCyan,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (isPlaying) "Pause" else "Play",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (isPlaying) "PAUSE" else "PLAY AUDIO",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Sound Waveform Equalizer
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .height(28.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color(0xFF0F172A))
                        .padding(horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    val heights = if (isPlaying) listOf(bar1, bar2, bar3, bar4, bar2, bar1, bar3) else listOf(0.3f, 0.4f, 0.5f, 0.3f, 0.4f, 0.3f, 0.2f)
                    heights.forEach { h ->
                        Box(
                            modifier = Modifier
                                .width(3.dp)
                                .height((20 * h).dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(if (isPlaying) TacticalCyan else TextSecondary.copy(alpha = 0.5f))
                        )
                    }
                }

                // Timer Display
                Text(
                    text = "${String.format(java.util.Locale.US, "0:%02d", currentProgressSeconds.toInt())} / ${String.format(java.util.Locale.US, "0:%02d", totalSecs.toInt())}",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    color = TextPrimary
                )

                // Mute / Unmute
                IconButton(
                    onClick = {
                        isMuted = !isMuted
                        try {
                            if (isMuted) mediaPlayer?.setVolume(0f, 0f) else mediaPlayer?.setVolume(1f, 1f)
                        } catch (_: Exception) {}
                    },
                    modifier = Modifier.size(30.dp)
                ) {
                    Icon(
                        imageVector = if (isMuted) Icons.Default.VolumeOff else Icons.Default.VolumeUp,
                        contentDescription = "Mute",
                        tint = if (isMuted) CrisisRed else TacticalCyan,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Playback Progress Indicator
            LinearProgressIndicator(
                progress = { (currentProgressSeconds / totalSecs).coerceIn(0f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp)
                    .clip(CircleShape),
                color = TacticalCyan,
                trackColor = Color(0xFF1E293B)
            )
        }
    }
}

/**
 * Tactical Video Player wrapper for compatibility:
 * Opens video in fullscreen on click and plays directly.
 */
@Composable
fun TacticalVideoPlayer(
    videoUrl: String?,
    roomId: String,
    modifier: Modifier = Modifier,
    localUri: String? = null,
    durationSeconds: Double = 5.0,
    isSuccess: Boolean = true,
    errorMessage: String? = null
) {
    var showFullscreen by remember { mutableStateOf(false) }
    val item = remember(videoUrl, roomId, localUri, durationSeconds, isSuccess, errorMessage) {
        RoomMediaItem(
            id = "vid_$roomId",
            roomId = roomId,
            mediaUrl = videoUrl,
            mediaType = "video",
            isSuccess = isSuccess,
            errorMessage = errorMessage,
            durationSeconds = durationSeconds,
            localUri = localUri
        )
    }

    TacticalVideoThumbnailCard(
        item = item,
        onClick = { showFullscreen = true },
        modifier = modifier
    )

    if (showFullscreen) {
        TacticalFullscreenVideoDialog(
            item = item,
            onDismiss = { showFullscreen = false }
        )
    }
}

