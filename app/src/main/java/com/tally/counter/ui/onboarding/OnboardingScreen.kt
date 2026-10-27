package com.tally.counter.ui.onboarding

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tally.counter.ui.components.BadgeSize
import com.tally.counter.ui.components.CountBadge
import com.tally.counter.ui.components.OnResume
import com.tally.counter.ui.components.SectionTitle
import com.tally.counter.ui.settings.PermissionList
import com.tally.counter.ui.settings.PermissionsViewModel
import com.tally.counter.ui.settings.PrivacyCard
import com.tally.counter.ui.theme.LocalTallyColors
import com.tally.counter.util.Haptics
import kotlinx.coroutines.delay

@Composable
fun OnboardingScreen(viewModel: PermissionsViewModel, onFinished: () -> Unit) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val colors = LocalTallyColors.current
    val view = LocalView.current
    val insets = WindowInsets.safeDrawing.asPaddingValues()
    OnResume { viewModel.refresh() }

    val finish = {
        Haptics.confirm(view)
        viewModel.completeOnboarding()
        onFinished()
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(
            start = 20.dp,
            end = 20.dp,
            top = insets.calculateTopPadding() + 24.dp,
            bottom = insets.calculateBottomPadding() + 32.dp
        ),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item { Hero() }

        item {
            Text(
                text = "Unread counts that stick",
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics { heading() }
            )
        }
        item {
            Text(
                text = "On your Samsung, icon badges vanish when you clear the notification panel — even if " +
                    "you haven't read anything. Tally keeps its own count for every app and shows it on a " +
                    "home screen widget, until you actually open the app or tap the notification.",
                style = MaterialTheme.typography.bodyLarge,
                color = colors.muted,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }

        item { PrivacyCard(modifier = Modifier.padding(top = 8.dp)) }

        item { SectionTitle("What Tally needs") }

        item {
            PermissionList(state = state, onFixListener = viewModel::fixListener)
        }

        item {
            Spacer(Modifier.height(8.dp))
            Button(
                onClick = finish,
                enabled = state.permissions.notificationAccess,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 56.dp),
                shape = RoundedCornerShape(18.dp)
            ) {
                Text(
                    if (state.permissions.notificationAccess) "Start counting" else "Allow notification access to continue",
                    style = MaterialTheme.typography.titleMedium
                )
            }
            if (!state.permissions.notificationAccess) {
                TextButton(onClick = finish, modifier = Modifier.fillMaxWidth()) {
                    Text("Skip for now")
                }
            }
        }
    }
}

/** App-icon mock with a badge that counts up and bounces. */
@Composable
private fun Hero() {
    val colors = LocalTallyColors.current
    var count by remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit) {
        delay(500)
        while (count < 7) {
            count += 1
            delay(if (count < 4) 450L else 320L)
        }
    }
    val transition = rememberInfiniteTransition(label = "float")
    val float by transition.animateFloat(
        initialValue = -4f,
        targetValue = 4f,
        animationSpec = infiniteRepeatable(tween(2200), RepeatMode.Reverse),
        label = "floatY"
    )
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(200.dp)
                .clip(RoundedCornerShape(100.dp))
                .background(
                    Brush.radialGradient(listOf(colors.badgeTop.copy(alpha = colors.glowAlpha), Color.Transparent))
                )
        )
        Box(
            modifier = Modifier.graphicsLayer { translationY = float * density },
            contentAlignment = Alignment.TopEnd
        ) {
            Box(
                modifier = Modifier
                    .padding(top = 14.dp, end = 14.dp)
                    .size(104.dp)
                    .clip(RoundedCornerShape(30.dp))
                    .background(
                        Brush.linearGradient(
                            listOf(MaterialTheme.colorScheme.surfaceContainerHighest, MaterialTheme.colorScheme.surface)
                        )
                    )
                    .border(1.dp, colors.cardStroke, RoundedCornerShape(30.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Filled.Notifications,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(48.dp)
                )
            }
            CountBadge(count = count, size = BadgeSize.Large, modifier = Modifier.offset(x = 6.dp, y = (-6).dp))
        }
    }
}
