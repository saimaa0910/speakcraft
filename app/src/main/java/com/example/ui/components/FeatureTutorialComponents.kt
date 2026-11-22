package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.model.FeatureTutorial
import com.example.data.model.FeatureTutorialRegistry
import com.example.ui.theme.SpeakCraftSpacing
import com.example.ui.theme.speakCraftColors

@Composable
fun FeatureGuideStrip(
    featureKey: String,
    modifier: Modifier = Modifier,
    initiallyExpanded: Boolean = false
) {
    val tutorial = remember(featureKey) { FeatureTutorialRegistry.forRoute(featureKey) }
    val ext = MaterialTheme.speakCraftColors
    val reducedMotion = rememberReducedMotionEnabled()
    var expanded by rememberSaveable(featureKey) { mutableStateOf(initiallyExpanded) }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .testTag("feature_guide_strip_$featureKey"),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.7f))
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .defaultMinSize(minHeight = SpeakCraftSpacing.minTouchTarget)
                    .clickable { expanded = !expanded }
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = ext.infoContainer,
                        modifier = Modifier.size(32.dp)
                    ) {
                        androidx.compose.foundation.layout.Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.AutoMirrored.Filled.HelpOutline,
                                contentDescription = "Feature tutorial",
                                tint = ext.info,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                    Column {
                        Text(
                            text = "How to use & what improves",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "${tutorial.categoryBadge} • ${tutorial.estimatedTime}",
                            style = MaterialTheme.typography.labelSmall,
                            color = ext.info
                        )
                    }
                }
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = ext.infoContainer.copy(alpha = 0.7f)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                    ) {
                        Text(
                            text = if (expanded) "Hide" else "Guide",
                            style = MaterialTheme.typography.labelMedium,
                            color = ext.info
                        )
                        Spacer(Modifier.width(2.dp))
                        Icon(
                            imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = if (expanded) "Collapse tutorial" else "Expand tutorial",
                            tint = ext.info,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            AnimatedVisibility(
                visible = expanded,
                enter = if (reducedMotion) fadeIn(tween(0)) else expandVertically() + fadeIn(),
                exit = if (reducedMotion) fadeOut(tween(0)) else shrinkVertically() + fadeOut()
            ) {
                Column(
                    modifier = Modifier.padding(start = 14.dp, end = 14.dp, bottom = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                    Text(
                        text = tutorial.summary,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    TutorialSectionContent(tutorial = tutorial)
                }
            }
        }
    }
}

@Composable
private fun TutorialSectionContent(tutorial: FeatureTutorial) {
    val ext = MaterialTheme.speakCraftColors
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = "How to Use",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary
        )
        tutorial.howToUseSteps.forEachIndexed { idx, step ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.Top
            ) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)
                ) {
                    Text(
                        text = "${idx + 1}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                    )
                }
                Text(
                    text = step,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        Spacer(Modifier.height(2.dp))
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Icon(Icons.AutoMirrored.Filled.TrendingUp, contentDescription = null, tint = ext.success, modifier = Modifier.size(16.dp))
            Text(
                text = "What Will Improve By Doing This",
                style = MaterialTheme.typography.labelLarge,
                color = ext.success
            )
        }
        tutorial.whatImproves.forEach { gain ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.Top
            ) {
                Icon(
                    Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = ext.success,
                    modifier = Modifier.size(15.dp).padding(top = 2.dp)
                )
                Text(
                    text = gain,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        Surface(
            shape = MaterialTheme.shapes.small,
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.Top
            ) {
                Icon(Icons.Default.Lightbulb, contentDescription = null, tint = ext.warning, modifier = Modifier.size(16.dp))
                Text(
                    text = "Coach Tip: ${tutorial.coachTip}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

private val directlyNavigableTutorialRoutes = setOf(
    "dashboard", "daily_practice", "learning_paths", "vocabulary", "speaking_lab",
    "challenges", "articulation_lab", "pronunciation", "voice_training", "conversation",
    "listening_lab", "grammar_speaking", "public_speaking", "real_world_tasks",
    "analytics", "ai_mentor", "settings", "app_updates"
)

@Composable
fun FeatureTutorialDialog(
    initialTutorial: FeatureTutorial,
    onDismiss: () -> Unit,
    onNavigateToFeature: ((String) -> Unit)? = null,
    onStartInteractiveTour: (() -> Unit)? = null
) {
    val ext = MaterialTheme.speakCraftColors
    var selectedTab by remember { mutableIntStateOf(0) }
    var searchQuery by remember { mutableStateOf("") }
    var inspectedTutorial by remember(initialTutorial) { mutableStateOf(initialTutorial) }

    val allMatches = remember(searchQuery) {
        val q = searchQuery.trim()
        if (q.isBlank()) FeatureTutorialRegistry.allTutorials
        else FeatureTutorialRegistry.allTutorials.filter { t ->
            t.title.contains(q, ignoreCase = true) ||
                t.summary.contains(q, ignoreCase = true) ||
                t.categoryBadge.contains(q, ignoreCase = true) ||
                t.whatImproves.any { it.contains(q, ignoreCase = true) }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.testTag("feature_tutorial_dialog"),
        title = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("SpeakCraft Feature Guide", style = MaterialTheme.typography.titleLarge)
                TabRow(selectedTabIndex = selectedTab) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = { Text("Guide Detail") },
                        modifier = Modifier.testTag("tutorial_tab_current")
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = { Text("All Features (${FeatureTutorialRegistry.allTutorials.size})") },
                        modifier = Modifier.testTag("tutorial_tab_all")
                    )
                }
            }
        },
        text = {
            if (selectedTab == 0) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 440.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = ext.infoContainer
                    ) {
                        Text(
                            text = "${inspectedTutorial.categoryBadge} • ${inspectedTutorial.estimatedTime}",
                            style = MaterialTheme.typography.labelSmall,
                            color = ext.info,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                    Text(inspectedTutorial.title, style = MaterialTheme.typography.titleMedium)
                    Text(
                        inspectedTutorial.summary,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    TutorialSectionContent(tutorial = inspectedTutorial)
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 440.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Search features or skills to improve...") },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                        trailingIcon = {
                            if (searchQuery.isNotBlank()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(Icons.Default.Clear, contentDescription = "Clear search")
                                }
                            }
                        },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("tutorial_search_input")
                    )
                    LazyColumn(
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(allMatches, key = { it.featureKey }) { item ->
                            Surface(
                                shape = MaterialTheme.shapes.small,
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(MaterialTheme.shapes.small)
                                    .clickable {
                                        inspectedTutorial = item
                                        selectedTab = 0
                                    }
                                    .testTag("tutorial_list_item_${item.featureKey}")
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            item.title,
                                            style = MaterialTheme.typography.labelLarge,
                                            modifier = Modifier.weight(1f)
                                        )
                                        Text(
                                            item.estimatedTime,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = ext.info
                                        )
                                    }
                                    Text(
                                        item.whatImproves.firstOrNull().orEmpty(),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = ext.success,
                                        maxLines = 2
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            if (selectedTab == 0 &&
                onNavigateToFeature != null &&
                directlyNavigableTutorialRoutes.contains(inspectedTutorial.featureKey) &&
                inspectedTutorial.featureKey != initialTutorial.featureKey
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = onDismiss) { Text("Close") }
                    Button(
                        onClick = {
                            onDismiss()
                            onNavigateToFeature(inspectedTutorial.featureKey)
                        }
                    ) {
                        Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Open")
                    }
                }
            } else {
                Button(onClick = onDismiss, modifier = Modifier.testTag("tutorial_close_button")) {
                    Text("Got It")
                }
            }
        },
        dismissButton = {
            if (selectedTab == 0 && inspectedTutorial.featureKey != initialTutorial.featureKey) {
                TextButton(onClick = { selectedTab = 1 }) {
                    Text("All Features")
                }
            } else if (selectedTab == 0 && onStartInteractiveTour != null) {
                TextButton(
                    onClick = {
                        onDismiss()
                        onStartInteractiveTour()
                    },
                    modifier = Modifier.testTag("tutorial_launch_overlay_button")
                ) {
                    Text("Launch Tour")
                }
            }
        }
    )
}
