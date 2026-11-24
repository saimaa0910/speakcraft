package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.speech.pipeline.InsightSeverity
import com.example.speech.pipeline.TranscriptInsight
import com.example.ui.theme.speakCraftColors

/**
 * Enhanced transcript display card that renders spoken text with inline mistake highlights
 * and actionable coaching tips (v1.5.0).
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TranscriptDisplayCard(
    transcript: String,
    insights: List<TranscriptInsight>,
    summary: String? = null,
    modifier: Modifier = Modifier,
    onInsightClick: ((TranscriptInsight) -> Unit)? = null
) {
    val ext = MaterialTheme.speakCraftColors
    var selectedInsight by remember { mutableStateOf<TranscriptInsight?>(null) }
    var selectedFilter by remember { mutableStateOf("All") }

    val errorCount = remember(insights) { insights.count { it.severity == InsightSeverity.ERROR } }
    val warningCount = remember(insights) { insights.count { it.severity == InsightSeverity.WARNING } }
    val tipCount = remember(insights) { insights.count { it.severity == InsightSeverity.TIP } }

    val filteredInsights = remember(insights, selectedFilter) {
        when (selectedFilter) {
            "Grammar" -> insights.filter { it.severity == InsightSeverity.ERROR }
            "Fillers & Delivery" -> insights.filter { it.severity == InsightSeverity.WARNING }
            "Tips" -> insights.filter { it.severity == InsightSeverity.TIP }
            else -> insights
        }
    }

    // Build highlighted annotated string
    val annotatedTranscript = remember(transcript, insights) {
        buildAnnotatedTranscript(
            text = transcript,
            insights = insights,
            errorBg = Color(0xFFFFD7D7),
            errorColor = Color(0xFFBA1A1A),
            warningBg = ext.pastelPeachBg,
            warningColor = ext.pastelPeachInk,
            tipBg = ext.pastelSkyBg,
            tipColor = ext.pastelSkyInk
        )
    }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)),
        modifier = modifier
            .fillMaxWidth()
            .testTag("transcript_display_card")
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        Icons.Default.Subtitles,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "Spoken Audio & Video Transcript",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                }

                if (insights.isNotEmpty()) {
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = if (errorCount > 0) MaterialTheme.colorScheme.errorContainer else ext.pastelMintBg
                    ) {
                        Text(
                            text = if (errorCount > 0) "$errorCount issues detected" else "Clean speech",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (errorCount > 0) MaterialTheme.colorScheme.onErrorContainer else ext.pastelMintInk,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }
            }

            // Summary banner if present
            if (!summary.isNullOrBlank()) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = if (errorCount > 0) MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.25f)
                    else MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f),
                    border = BorderStroke(
                        1.dp,
                        if (errorCount > 0) MaterialTheme.colorScheme.error.copy(alpha = 0.2f)
                        else MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            if (errorCount > 0) Icons.Default.Warning else Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = if (errorCount > 0) MaterialTheme.colorScheme.error else ext.success,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = summary,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            // Annotated transcript body
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = annotatedTranscript,
                    style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 22.sp),
                    modifier = Modifier.padding(14.dp)
                )
            }

            // Mistake / Tip inspection section
            if (insights.isNotEmpty()) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

                Text(
                    text = "Detected Mistakes & Coaching Tips",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold
                )

                // Filter tabs
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    FilterChip(
                        selected = selectedFilter == "All",
                        onClick = { selectedFilter = "All" },
                        label = { Text("All (${insights.size})") }
                    )
                    if (errorCount > 0) {
                        FilterChip(
                            selected = selectedFilter == "Grammar",
                            onClick = { selectedFilter = "Grammar" },
                            label = { Text("Grammar ($errorCount)") }
                        )
                    }
                    if (warningCount > 0) {
                        FilterChip(
                            selected = selectedFilter == "Fillers & Delivery",
                            onClick = { selectedFilter = "Fillers & Delivery" },
                            label = { Text("Fillers ($warningCount)") }
                        )
                    }
                }

                // List of mistake cards with actionable tips
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    filteredInsights.forEach { insight ->
                        MistakeInsightRow(
                            insight = insight,
                            isSelected = selectedInsight == insight,
                            onClick = {
                                selectedInsight = if (selectedInsight == insight) null else insight
                                onInsightClick?.invoke(insight)
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MistakeInsightRow(
    insight: TranscriptInsight,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val ext = MaterialTheme.speakCraftColors

    val (badgeBg, badgeInk, icon) = when (insight.severity) {
        InsightSeverity.ERROR -> Triple(
            MaterialTheme.colorScheme.errorContainer,
            MaterialTheme.colorScheme.onErrorContainer,
            Icons.Default.ErrorOutline
        )
        InsightSeverity.WARNING -> Triple(
            ext.pastelPeachBg,
            ext.pastelPeachInk,
            Icons.Default.Warning
        )
        InsightSeverity.TIP -> Triple(
            ext.pastelSkyBg,
            ext.pastelSkyInk,
            Icons.Default.Lightbulb
        )
    }

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = if (isSelected) badgeBg.copy(alpha = 0.35f) else MaterialTheme.colorScheme.surface,
        border = BorderStroke(
            if (isSelected) 1.5.dp else 1.dp,
            if (isSelected) badgeInk.copy(alpha = 0.6f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("insight_row_${insight.spanStart}")
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(icon, contentDescription = null, tint = badgeInk, modifier = Modifier.size(16.dp))
                    Text(
                        text = insight.issueTitle,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = badgeInk
                    )
                }

                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = badgeBg
                ) {
                    Text(
                        text = "\"${insight.highlightedText}\"",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = badgeInk,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Text(
                text = insight.suggestion,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

/**
 * Builds an annotated string highlighting each insight's span with severity-appropriate styles.
 */
internal fun buildAnnotatedTranscript(
    text: String,
    insights: List<TranscriptInsight>,
    errorBg: Color,
    errorColor: Color,
    warningBg: Color,
    warningColor: Color,
    tipBg: Color,
    tipColor: Color
): AnnotatedString {
    if (text.isEmpty() || insights.isEmpty()) {
        return AnnotatedString(text)
    }

    // Sort by start position
    val sortedInsights = insights.sortedBy { it.spanStart }

    return buildAnnotatedString {
        var cursor = 0

        for (insight in sortedInsights) {
            val start = insight.spanStart.coerceIn(0, text.length)
            val end = insight.spanEnd.coerceIn(0, text.length)

            if (start > cursor) {
                append(text.substring(cursor, start))
            }

            if (start < end && start >= cursor) {
                val spanStyle = when (insight.severity) {
                    InsightSeverity.ERROR -> SpanStyle(
                        background = errorBg,
                        color = errorColor,
                        fontWeight = FontWeight.Bold,
                        textDecoration = TextDecoration.Underline
                    )
                    InsightSeverity.WARNING -> SpanStyle(
                        background = warningBg,
                        color = warningColor,
                        fontWeight = FontWeight.SemiBold
                    )
                    InsightSeverity.TIP -> SpanStyle(
                        background = tipBg,
                        color = tipColor,
                        fontWeight = FontWeight.Medium
                    )
                }

                val targetText = text.substring(start, end)
                val spanStartIdx = length
                append(targetText)
                addStyle(spanStyle, spanStartIdx, length)
                cursor = end
            }
        }

        if (cursor < text.length) {
            append(text.substring(cursor))
        }
    }
}
