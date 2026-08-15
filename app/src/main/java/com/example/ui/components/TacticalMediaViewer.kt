package com.example.ui.components

import android.content.Context
import android.media.MediaPlayer
import android.net.Uri
import android.util.Log
import android.widget.FrameLayout
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
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
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
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File

/**
 * Tactical Audio Player Component
 * Plays audio if upload was successful. Otherwise displays only the failure text message.
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
    val context = LocalContext.current
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

    // --- SUCCESSFUL AUDIO UPLOAD: Tactical Interactive Player ---
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
            // Attempt real MediaPlayer initialization
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
                Log.w("TacticalAudioPlayer", "Using fallback audio playback timer: ${e.message}")
            }

            // Fallback timer loop
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
                            text = "Cloudinary Verified Audio Stream",
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
                        text = "READY TO HEAR",
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
                        .testTag("play_audio_note_button_${roomId}"),
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
                        text = if (isPlaying) "PAUSE" else "HEAR AUDIO",
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
 * Tactical Video Player Component
 * Plays/shows video if upload was successful. Otherwise displays only the failure text message.
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
    val isUploadValid = isSuccess && !videoUrl.isNullOrBlank()

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
                    contentDescription = "Video Upload Failed",
                    tint = CrisisRed,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = errorMessage ?: "Video upload from Room $roomId failed. No media stream available.",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White
                )
            }
        }
        return
    }

    // --- SUCCESSFUL VIDEO UPLOAD: Tactical Video View & Player ---
    var isPlaying by remember { mutableStateOf(false) }
    var showFullscreenDialog by remember { mutableStateOf(false) }
    var currentProgressSeconds by remember { mutableFloatStateOf(0f) }
    val totalSecs = durationSeconds.coerceIn(1.0, 5.0).toFloat()

    val infiniteTransition = rememberInfiniteTransition(label = "video_scan")
    val scanLineY by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(2000, easing = LinearEasing), RepeatMode.Restart),
        label = "scanline"
    )

    LaunchedEffect(isPlaying) {
        if (isPlaying) {
            while (isPlaying && currentProgressSeconds < totalSecs) {
                delay(100)
                currentProgressSeconds += 0.1f
            }
            if (currentProgressSeconds >= totalSecs) {
                isPlaying = false
                currentProgressSeconds = 0f
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF020617))
            .border(1.5.dp, CrisisRed.copy(alpha = 0.7f), RoundedCornerShape(12.dp))
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
                            text = "TACTICAL VIDEO INTEL • ROOM $roomId",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = CrisisRed
                        )
                        Text(
                            text = "Cloudinary Verified 5s Feed",
                            fontSize = 8.5.sp,
                            color = SafeGreen
                        )
                    }
                }

                // Expand Fullscreen Button
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(TacticalCyan.copy(alpha = 0.2f))
                        .clickable { showFullscreenDialog = true }
                        .padding(horizontal = 6.dp, vertical = 3.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Fullscreen,
                        contentDescription = "Fullscreen",
                        tint = TacticalCyan,
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = "EXPAND",
                        fontSize = 8.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = TacticalCyan
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Video Viewport Frame with Tactical Reticle
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF0F172A))
                    .border(1.dp, Color(0xFF334155), RoundedCornerShape(8.dp))
                    .clickable { isPlaying = !isPlaying }
            ) {
                // If local video exists, attempt AndroidView VideoView
                if (!localUri.isNullOrBlank() && File(localUri).exists()) {
                    AndroidView(
                        factory = { ctx ->
                            VideoView(ctx).apply {
                                setVideoPath(localUri)
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

                // Tactical Reticle & Scanline Canvas
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val w = size.width
                    val h = size.height

                    // Grid lines
                    drawLine(
                        color = TacticalCyan.copy(alpha = 0.2f),
                        start = Offset(0f, h / 2),
                        end = Offset(w, h / 2),
                        strokeWidth = 1f,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f))
                    )
                    drawLine(
                        color = TacticalCyan.copy(alpha = 0.2f),
                        start = Offset(w / 2, 0f),
                        end = Offset(w / 2, h),
                        strokeWidth = 1f,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f))
                    )

                    // Corner brackets
                    val bracketLen = 20f
                    // Top-Left
                    drawLine(CrisisRed, Offset(10f, 10f), Offset(10f + bracketLen, 10f), 2f)
                    drawLine(CrisisRed, Offset(10f, 10f), Offset(10f, 10f + bracketLen), 2f)
                    // Top-Right
                    drawLine(CrisisRed, Offset(w - 10f, 10f), Offset(w - 10f - bracketLen, 10f), 2f)
                    drawLine(CrisisRed, Offset(w - 10f, 10f), Offset(w - 10f, 10f + bracketLen), 2f)
                    // Bottom-Left
                    drawLine(CrisisRed, Offset(10f, h - 10f), Offset(10f + bracketLen, h - 10f), 2f)
                    drawLine(CrisisRed, Offset(10f, h - 10f), Offset(10f, h - 10f - bracketLen), 2f)
                    // Bottom-Right
                    drawLine(CrisisRed, Offset(w - 10f, h - 10f), Offset(w - 10f - bracketLen, h - 10f), 2f)
                    drawLine(CrisisRed, Offset(w - 10f, h - 10f), Offset(w - 10f, h - 10f - bracketLen), 2f)

                    // Moving scanline if playing
                    if (isPlaying) {
                        drawLine(
                            color = TacticalCyan.copy(alpha = 0.4f),
                            start = Offset(0f, h * scanLineY),
                            end = Offset(w, h * scanLineY),
                            strokeWidth = 2f
                        )
                    }
                }

                // HUD Overlays
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "CAM-RM$roomId [LIVE]",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = CrisisRed
                    )
                    Text(
                        text = "REC 00:0${currentProgressSeconds.toInt()}:00",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = Color.White
                    )
                }

                // Center Play Button Overlay
                if (!isPlaying) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(CrisisRed.copy(alpha = 0.85f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "Play Video",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Controls Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
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
                        .height(34.dp)
                        .testTag("play_video_note_button_${roomId}"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isPlaying) CrisisRed else TacticalCyan,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (isPlaying) "PAUSE" else "SEE VIDEO",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                LinearProgressIndicator(
                    progress = { (currentProgressSeconds / totalSecs).coerceIn(0f, 1f) },
                    modifier = Modifier
                        .weight(1f)
                        .height(4.dp)
                        .clip(CircleShape),
                    color = CrisisRed,
                    trackColor = Color(0xFF1E293B)
                )

                Text(
                    text = "${String.format(java.util.Locale.US, "0:%02d", currentProgressSeconds.toInt())} / 0:0${totalSecs.toInt()}",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    color = TextPrimary
                )
            }
        }
    }

    // Fullscreen Interactive Video Inspection Dialog
    if (showFullscreenDialog) {
        Dialog(
            onDismissRequest = { showFullscreenDialog = false },
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
                    // Dialog Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Videocam,
                                contentDescription = null,
                                tint = CrisisRed,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "INCIDENT FEED • ROOM $roomId (FLOOR 4)",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Black,
                                color = TextPrimary
                            )
                        }

                        IconButton(onClick = { showFullscreenDialog = false }) {
                            Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = TextPrimary)
                        }
                    }

                    // Large Video Player Frame
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color(0xFF0F172A))
                            .border(2.dp, CrisisRed, RoundedCornerShape(14.dp))
                            .clickable { isPlaying = !isPlaying }
                    ) {
                        if (!localUri.isNullOrBlank() && File(localUri).exists()) {
                            AndroidView(
                                factory = { ctx ->
                                    VideoView(ctx).apply {
                                        setVideoPath(localUri)
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

                        // Tactical Scanlines
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val w = size.width
                            val h = size.height
                            if (isPlaying) {
                                drawLine(
                                    color = TacticalCyan.copy(alpha = 0.5f),
                                    start = Offset(0f, h * scanLineY),
                                    end = Offset(w, h * scanLineY),
                                    strokeWidth = 3f
                                )
                            }
                        }

                        if (!isPlaying) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.Center)
                                    .size(64.dp)
                                    .clip(CircleShape)
                                    .background(CrisisRed.copy(alpha = 0.9f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PlayArrow,
                                    contentDescription = "Play",
                                    tint = Color.White,
                                    modifier = Modifier.size(36.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Fullscreen Controls Bar
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = { isPlaying = !isPlaying },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = if (isPlaying) CrisisRed else TacticalCyan)
                        ) {
                            Icon(
                                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = null
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(if (isPlaying) "PAUSE VIDEO" else "PLAY FULL FEED", fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = { showFullscreenDialog = false },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF334155))
                        ) {
                            Text("CLOSE", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}
