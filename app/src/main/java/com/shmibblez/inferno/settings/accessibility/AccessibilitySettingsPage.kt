package com.shmibblez.inferno.settings.accessibility

import android.annotation.SuppressLint
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.shmibblez.inferno.R
import com.shmibblez.inferno.proto.InfernoSettings
import com.shmibblez.inferno.proto.infernoSettingsDataStore
import com.shmibblez.inferno.settings.compose.components.InfernoSettingsPage
import com.shmibblez.inferno.settings.compose.components.PreferenceSlider
import com.shmibblez.inferno.settings.compose.components.PreferenceSwitch
import kotlinx.coroutines.launch

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@Composable
fun AccessibilitySettingsPage(goBack: () -> Unit) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val settings by context.infernoSettingsDataStore.data.collectAsState(InfernoSettings.getDefaultInstance())

    InfernoSettingsPage(
        title = stringResource(R.string.preferences_accessibility),
        goBack = goBack,
    ) { edgeInsets ->
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .padding(edgeInsets),
            horizontalAlignment = Alignment.Start,
            verticalArrangement = Arrangement.Top,
        ) {
            // size font automatically
            item {
                PreferenceSwitch(
                    text = stringResource(R.string.preference_accessibility_auto_size_2),
                    summary = stringResource(R.string.preference_accessibility_auto_size_summary),
                    selected = settings.shouldSizeFontAutomatically,
                    onSelectedChange = { selected ->
                        coroutineScope.launch {
                            context.infernoSettingsDataStore.updateData {
                                it.toBuilder().setShouldSizeFontAutomatically(selected).build()
                            }
                        }

                    },
                    enabled = true,
                )
            }

            // font size factor
            item {
                PreferenceSlider(
                    text = stringResource(R.string.preference_accessibility_font_size_title),
                    summary = stringResource(R.string.preference_accessibility_text_size_summary),
                    initialPosition = settings.fontSizeFactor,
                    onSet = { newFactor ->
                        coroutineScope.launch {
                            context.infernoSettingsDataStore.updateData {
                                it.toBuilder().setFontSizeFactor(newFactor).build()
                            }
                        }
                    },
                    enabled = !settings.shouldSizeFontAutomatically,
                )
            }

            // force enable zoom
            item {
                PreferenceSwitch(
                    text = stringResource(R.string.preference_accessibility_force_enable_zoom),
                    summary = stringResource(R.string.preference_accessibility_force_enable_zoom_summary),
                    selected = settings.shouldForceEnableZoomInWebsites,
                    onSelectedChange = { selected ->
                        coroutineScope.launch {
                            context.infernoSettingsDataStore.updateData {
                                it.toBuilder().setShouldForceEnableZoomInWebsites(selected).build()
                            }
                        }

                    },
                    enabled = true,
                )
            }

            // always request desktop
            item {
                PreferenceSwitch(
                    text = stringResource(R.string.preference_feature_desktop_mode_default),
                    summary = null,
                    selected = settings.alwaysRequestDesktopSite,
                    onSelectedChange = { selected ->
                        coroutineScope.launch {
                            context.infernoSettingsDataStore.updateData {
                                it.toBuilder().setAlwaysRequestDesktopSite(selected).build()
                            }
                        }
                    },
                    enabled = true,
                )
            }
        }
    }
}