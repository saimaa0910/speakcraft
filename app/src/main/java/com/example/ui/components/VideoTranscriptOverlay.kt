package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CenterFocusStrong
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.speakCraftColors

/**
 * Overlay component for live speech-to-text transcript and real-time coaching cues
 * during audio or video recording and playback (ARC-07, v1.5.0).
 */
@Composable
fun VideoTranscriptOverlay(
    liveTranscript: String,
    modifier: Modifier = Modifier,
    isRecording: Boolean = true,
    faceInFrame: Boolean = true,
    facingCamera: Boolean = true,
    postureLevel: Boolean = true,
    lastDetectedMistake: String? = null,
    visible: Boolean = true
) {
    val ext = MaterialTheme.speakCraftColors

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn() + slideInVertically { it / 2 },
        exit = fadeOut() + slideOutVertically { it / 2 },
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Live Video Real-Time Status Cues (Face, Gaze, Posture, Mistakes)
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Face status
                Surface(
                    shape = RoundedCornerShape(50),
                    color = if (faceInFrame) Color(0x99101010) else Color(0xCCB00020),
                    border = BorderStroke(1.dp, if (faceInFrame) Color(0x33FFFFFF) else Color(0x88FF4444))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(if (faceInFrame) ext.success else Color.Red)
                        )
                        Text(
                            text = if (faceInFrame) "Face in Frame" else "Center Face",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                // Camera gaze status
                Surface(
                    shape = RoundedCornerShape(50),
                    color = if (facingCamera) Color(0x99101010) else Color(0xCCE67E22),
                    border = BorderStroke(1.dp, if (facingCamera) Color(0x33FFFFFF) else Color(0x88E67E22))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CenterFocusStrong,
                            contentDescription = null,
                            tint = if (facingCamera) ext.success else Color(0xFFFFB74D),
                            modifier = Modifier.size(10.dp)
                        )
                        Text(
                            text = if (facingCamera) "Eye Line OK" else "Look at Lens",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                // Posture status
                Surface(
                    shape = RoundedCornerShape(50),
                    color = if (postureLevel) Color(0x99101010) else Color(0xCCE67E22),
                    border = BorderStroke(1.dp, if (postureLevel) Color(0x33FFFFFF) else Color(0x88E67E22))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = if (postureLevel) "Level Shoulders" else "Level Shoulders",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (postureLevel) Color.White else Color(0xFFFFB74D),
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                // Last mistake / tip alert badge if active
                if (!lastDetectedMistake.isNullOrBlank()) {
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = Color(0xDD991B1B),
                        border = BorderStroke(1.dp, Color(0x88EF4444))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(10.dp)
                            )
                            Text(
                                text = lastDetectedMistake,
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }

            // Live Spoken Transcript Capsule
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Color(0xCC0F172A), // Dark slate semi-transparent
                border = BorderStroke(1.dp, Color(0x3394A3B8)),
                shadowElevation = 4.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("video_transcript_overlay_capsule")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = if (isRecording) Icons.Default.Mic else Icons.Default.Subtitles,
                        contentDescription = "Spoken Audio",
                        tint = if (isRecording) ext.warning else ext.info,
                        modifier = Modifier.size(16.dp)
                    )

                    Text(
                        text = liveTranscript.ifBlank {
                            if (isRecording) "Listening... speak clearly" else "No speech captured"
                        },
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = if (liveTranscript.isNotBlank()) Color.White else Color(0xFF94A3B8),
                            fontSize = 14.sp,
                            lineHeight = 20.sp
                        ),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}
