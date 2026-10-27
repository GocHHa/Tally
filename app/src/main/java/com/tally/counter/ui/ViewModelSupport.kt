package com.tally.counter.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.tally.counter.data.apps.AppInfoProvider

/** Builds a ViewModel factory from a lambda (manual DI, no framework). */
inline fun <reified VM : ViewModel> factoryOf(crossinline create: () -> VM): ViewModelProvider.Factory =
    viewModelFactory {
        initializer { create() }
    }

/** Everything the UI needs to draw an app: name, icon and glow colour. */
data class AppVisual(
    val packageName: String,
    val label: String,
    val icon: ImageBitmap?,
    val accent: Color
)

const val UI_ICON_PX = 144

/** Call from a background thread. */
fun AppInfoProvider.visual(packageName: String): AppVisual = AppVisual(
    packageName = packageName,
    label = meta(packageName).label,
    icon = iconBitmap(packageName, UI_ICON_PX)?.asImageBitmap(),
    accent = Color(accentColor(packageName))
)
