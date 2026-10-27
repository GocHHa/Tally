package com.tally.counter.ui.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tally.counter.data.apps.AppInfoProvider
import com.tally.counter.ui.UI_ICON_PX
import com.tally.counter.ui.components.AppIcon
import com.tally.counter.ui.components.ScreenHeader
import com.tally.counter.ui.components.SettingSwitchRow
import com.tally.counter.ui.components.TallyCard
import com.tally.counter.ui.detail.CountModeSelector
import com.tally.counter.ui.theme.LocalTallyColors
import com.tally.counter.util.Haptics
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun AppsScreen(viewModel: AppsViewModel, appInfoProvider: AppInfoProvider, onBack: () -> Unit) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val colors = LocalTallyColors.current
    val insets = WindowInsets.safeDrawing.asPaddingValues()
    var expanded by rememberSaveable { mutableStateOf<String?>(null) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(
            start = 16.dp,
            end = 16.dp,
            top = insets.calculateTopPadding() + 4.dp,
            bottom = insets.calculateBottomPadding() + 32.dp
        ),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item(key = "header") { ScreenHeader(title = "Choose apps", onBack = onBack) }

        item(key = "search") {
            OutlinedTextField(
                value = state.query,
                onValueChange = viewModel::setQuery,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                placeholder = { Text("Search apps") },
                shape = RoundedCornerShape(18.dp)
            )
        }

        item(key = "system") {
            TallyCard(contentPadding = 12.dp) {
                SettingSwitchRow(
                    title = "Show system apps",
                    description = if (state.showSystem) {
                        "System components without a launcher icon are excluded unless you turn them on."
                    } else {
                        "${state.hiddenSystemCount} hidden. These are excluded by default."
                    },
                    checked = state.showSystem,
                    onCheckedChange = viewModel::setShowSystem
                )
            }
        }

        if (state.loading) {
            item(key = "loading") {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) { CircularProgressIndicator() }
            }
        } else if (state.apps.isEmpty()) {
            item(key = "none") {
                Text(
                    "No apps match.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.muted,
                    modifier = Modifier.padding(8.dp)
                )
            }
        }

        items(state.apps, key = { it.meta.packageName }) { app ->
            AppOptionRow(
                app = app,
                appInfoProvider = appInfoProvider,
                expanded = expanded == app.meta.packageName,
                onToggleExpanded = {
                    expanded = if (expanded == app.meta.packageName) null else app.meta.packageName
                },
                onCountedChange = { viewModel.setCounted(app.meta.packageName, it) },
                onCountMessagesChange = { viewModel.setCountMessages(app.meta.packageName, it) },
                onResetOnAppCancelChange = { viewModel.setResetOnAppCancel(app.meta.packageName, it) },
                modifier = Modifier.animateItem()
            )
        }
    }
}

@Composable
private fun AppOptionRow(
    app: AppOptionUi,
    appInfoProvider: AppInfoProvider,
    expanded: Boolean,
    onToggleExpanded: () -> Unit,
    onCountedChange: (Boolean) -> Unit,
    onCountMessagesChange: (Boolean) -> Unit,
    onResetOnAppCancelChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalTallyColors.current
    val view = LocalView.current
    val icon by produceState<ImageBitmap?>(initialValue = null, app.meta.packageName) {
        value = withContext(Dispatchers.IO) {
            appInfoProvider.iconBitmap(app.meta.packageName, UI_ICON_PX)?.asImageBitmap()
        }
    }

    TallyCard(modifier = modifier, contentPadding = 0.dp) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(role = Role.Switch) {
                    Haptics.toggle(view, !app.counted)
                    onCountedChange(!app.counted)
                }
                .semantics { stateDescription = if (app.counted) "Counted" else "Not counted" }
                .padding(start = 14.dp, top = 12.dp, bottom = 12.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AppIcon(icon = icon, label = app.meta.label, size = 40.dp)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    app.meta.label,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = buildString {
                        append(if (app.counted) "Counted" else "Not counted")
                        if (app.counted) append(if (app.countMessages) " · messages" else " · notifications")
                        if (app.meta.defaultExcluded) append(" · system")
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.muted,
                    maxLines = 1
                )
            }
            Switch(checked = app.counted, onCheckedChange = null)
            IconButton(onClick = {
                Haptics.tap(view)
                onToggleExpanded()
            }) {
                Icon(
                    if (expanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                    contentDescription = if (expanded) "Hide options for ${app.meta.label}" else "Options for ${app.meta.label}"
                )
            }
        }
        AnimatedVisibility(
            visible = expanded,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(start = 14.dp, end = 14.dp, bottom = 14.dp)
            ) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .clip(RoundedCornerShape(1.dp))
                        .background(colors.cardStroke)
                )
                Spacer(Modifier.size(12.dp))
                Text("Count", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
                Spacer(Modifier.size(8.dp))
                CountModeSelector(countMessages = app.countMessages, onChange = onCountMessagesChange)
                Spacer(Modifier.size(4.dp))
                SettingSwitchRow(
                    title = "Reset when the app clears its notification",
                    description = "Turn off if this app removes notifications before you've read them.",
                    checked = app.resetOnAppCancel,
                    onCheckedChange = onResetOnAppCancelChange
                )
            }
        }
    }
}
