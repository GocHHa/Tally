package com.tally.counter.widget

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.ColorFilter
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.LocalSize
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.appWidgetBackground
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.lazy.GridCells
import androidx.glance.appwidget.lazy.LazyVerticalGrid
import androidx.glance.appwidget.lazy.items
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.color.ColorProvider
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.semantics.contentDescription
import androidx.glance.semantics.semantics
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.tally.counter.R
import com.tally.counter.appContainer
import com.tally.counter.ui.MainActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map

data class WidgetItem(
    val packageName: String,
    val label: String,
    val count: Int,
    val icon: Bitmap?
)

/** Home screen widget: a grid of app icons with red count pills, highest count first. */
class TallyWidget : GlanceAppWidget() {

    override val sizeMode: SizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val container = context.appContainer
        // Widget updates are one of the moments we check whether apps were opened.
        container.usageResetChecker.check()
        val itemsFlow = widgetItems(context)
        val initial = itemsFlow.first()
        provideContent {
            val items by remember { itemsFlow }.collectAsState(initial = initial)
            WidgetContent(items)
        }
    }

    private fun widgetItems(context: Context): Flow<List<WidgetItem>> {
        val container = context.appContainer
        val apps = container.appInfoProvider
        return container.countRepository.observeActive()
            .map { rows ->
                rows.take(MAX_ITEMS).map { row ->
                    WidgetItem(
                        packageName = row.packageName,
                        label = apps.meta(row.packageName).label,
                        count = row.unreadCount,
                        icon = apps.iconBitmap(row.packageName, ICON_PX)
                    )
                }
            }
            .flowOn(Dispatchers.IO)
    }

    companion object {
        /** Keeps the RemoteViews payload well under the binder limit. */
        const val MAX_ITEMS = 18
        const val ICON_PX = 88
    }
}

class TallyWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = TallyWidget()
}

/** "Clear all" button in the widget header. */
class ClearAllAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        context.appContainer.countRepository.clearAll()
        TallyWidget().update(context, glanceId)
    }
}

private object WidgetColors {
    val background = ColorProvider(day = Color(0xFFF6F6F9), night = Color(0xFF111116))
    val title = ColorProvider(day = Color(0xFF15151A), night = Color(0xFFF2F2F5))
    val muted = ColorProvider(day = Color(0xFF6B6B76), night = Color(0xFF9A9AA6))
    val chip = ColorProvider(day = Color(0xFFE8E8EE), night = Color(0xFF22222A))
    val badge = ColorProvider(day = Color(0xFFFF3B30), night = Color(0xFFFF453A))
    val onBadge = ColorProvider(day = Color.White, night = Color.White)
    val success = ColorProvider(day = Color(0xFF1E9E55), night = Color(0xFF32D74B))
}

@Composable
private fun WidgetContent(items: List<WidgetItem>) {
    val total = items.sumOf { it.count }
    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .appWidgetBackground()
            .cornerRadius(24.dp)
            .background(WidgetColors.background)
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        Row(
            modifier = GlanceModifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Tally",
                style = TextStyle(color = WidgetColors.title, fontSize = 15.sp, fontWeight = FontWeight.Bold),
                maxLines = 1,
                modifier = GlanceModifier.defaultWeight().clickable(actionStartActivity<MainActivity>())
            )
            if (items.isNotEmpty()) {
                Text(
                    text = "$total unread",
                    style = TextStyle(color = WidgetColors.muted, fontSize = 12.sp, fontWeight = FontWeight.Medium),
                    maxLines = 1
                )
                Spacer(GlanceModifier.width(8.dp))
                Box(
                    modifier = GlanceModifier
                        .size(30.dp)
                        .cornerRadius(15.dp)
                        .background(WidgetColors.chip)
                        .clickable(actionRunCallback<ClearAllAction>())
                        .semantics { contentDescription = "Clear all counts" },
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        provider = ImageProvider(R.drawable.ic_widget_clear_all),
                        contentDescription = "Clear all counts",
                        colorFilter = ColorFilter.tint(WidgetColors.muted),
                        modifier = GlanceModifier.size(18.dp)
                    )
                }
            }
        }
        Spacer(GlanceModifier.height(6.dp))
        if (items.isEmpty()) {
            EmptyState()
        } else {
            AppGrid(items)
        }
    }
}

@Composable
private fun EmptyState() {
    Column(
        modifier = GlanceModifier.fillMaxSize(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Image(
            provider = ImageProvider(R.drawable.ic_widget_check),
            contentDescription = null,
            colorFilter = ColorFilter.tint(WidgetColors.success),
            modifier = GlanceModifier.size(26.dp)
        )
        Spacer(GlanceModifier.height(6.dp))
        Text(
            text = "All caught up",
            style = TextStyle(color = WidgetColors.muted, fontSize = 13.sp, fontWeight = FontWeight.Medium),
            maxLines = 1
        )
    }
}

@Composable
private fun AppGrid(items: List<WidgetItem>) {
    val size = LocalSize.current
    val usableWidth = size.width.value - 24f
    val columns = (usableWidth / 66f).toInt().coerceIn(1, 5)
    LazyVerticalGrid(
        gridCells = GridCells.Fixed(columns),
        modifier = GlanceModifier.fillMaxSize()
    ) {
        items(items, itemId = { it.packageName.hashCode().toLong() }) { item ->
            AppCell(item)
        }
    }
}

@Composable
private fun AppCell(item: WidgetItem) {
    val context = LocalContext.current
    val openIntent = Intent(context, OpenAppActivity::class.java).apply {
        action = OpenAppActivity.ACTION_OPEN
        data = Uri.parse("tally://open/${item.packageName}")
        putExtra(OpenAppActivity.EXTRA_PACKAGE, item.packageName)
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_NO_ANIMATION)
    }
    val countText = if (item.count > 99) "99+" else item.count.toString()
    Box(
        modifier = GlanceModifier
            .fillMaxWidth()
            .height(70.dp)
            .clickable(actionStartActivity(openIntent))
            .semantics { contentDescription = "${item.label}, ${item.count} unread. Opens the app and resets its count." },
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = GlanceModifier.size(62.dp),
            contentAlignment = Alignment.TopEnd
        ) {
            Box(
                modifier = GlanceModifier.size(62.dp),
                contentAlignment = Alignment.Center
            ) {
                if (item.icon != null) {
                    Image(
                        provider = ImageProvider(item.icon),
                        contentDescription = item.label,
                        modifier = GlanceModifier.size(46.dp)
                    )
                } else {
                    Box(
                        modifier = GlanceModifier.size(46.dp).cornerRadius(23.dp).background(WidgetColors.chip),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = item.label.take(1).uppercase(),
                            style = TextStyle(color = WidgetColors.title, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        )
                    }
                }
            }
            Box(
                modifier = GlanceModifier
                    .height(20.dp)
                    .cornerRadius(10.dp)
                    .background(WidgetColors.badge)
                    .padding(horizontal = 6.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = countText,
                    style = TextStyle(color = WidgetColors.onBadge, fontSize = 11.sp, fontWeight = FontWeight.Bold),
                    maxLines = 1
                )
            }
        }
    }
}
