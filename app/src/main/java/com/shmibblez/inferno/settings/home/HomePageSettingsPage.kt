package com.shmibblez.inferno.settings.home

import android.annotation.SuppressLint
import android.content.Context
import android.webkit.URLUtil
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.shmibblez.inferno.R
import com.shmibblez.inferno.compose.base.InfernoButton
import com.shmibblez.inferno.compose.base.InfernoOutlinedTextField
import com.shmibblez.inferno.compose.base.InfernoText
import com.shmibblez.inferno.compose.base.InfernoTextStyle
import com.shmibblez.inferno.proto.InfernoSettings
import com.shmibblez.inferno.proto.infernoSettingsDataStore
import com.shmibblez.inferno.settings.compose.components.InfernoSettingsPage
import com.shmibblez.inferno.settings.compose.components.PrefUiConst
import com.shmibblez.inferno.settings.compose.components.PreferenceSelect
import com.shmibblez.inferno.settings.compose.components.PreferenceSwitch
import com.shmibblez.inferno.settings.compose.components.PreferenceTitle
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import mozilla.components.support.utils.WebURLFinder.Companion.isValidWebURL

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@Composable
fun HomePageSettingsPage(goBack: () -> Unit) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val settings by context.infernoSettingsDataStore.data.collectAsState(InfernoSettings.getDefaultInstance())


    InfernoSettingsPage(
        title = stringResource(R.string.preferences_home_2),
        goBack = goBack,
    ) { edgeInsets ->
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .padding(edgeInsets),
            horizontalAlignment = Alignment.Start,
            verticalArrangement = Arrangement.Top,
        ) {
            // todo: not implemented yet, defaultTopSitesAdded and shouldShowSearchWidget

            item { PreferenceTitle(stringResource(R.string.preferences_category_general)) }


            // use inferno homepage
            item {
                PreferenceSwitch(
                    text = "Use Inferno Homepage:", // todo: string res more descriptive (mention tab bar if enabled)
                    summary = null,
                    selected = settings.shouldUseInfernoHome,
                    onSelectedChange = { shouldUseInfernoHome ->
                        coroutineScope.launch {
                            context.infernoSettingsDataStore.updateData {
                                it.toBuilder().setShouldUseInfernoHome(shouldUseInfernoHome).build()
                            }
                        }
                    },
                )
            }

            when (settings.shouldUseInfernoHome) {
                // if true, show options for inferno home
                true -> {
                    // inferno home settings
                    item { PreferenceTitle("Inferno Home Settings") } // todo: string res

                    // show top sites
                    item {
                        PreferenceSwitch(
                            text = "Show top sites:", // todo: string res more descriptive (mention tab bar if enabled)
                            summary = null,
                            selected = settings.shouldShowTopSites,
                            onSelectedChange = { shouldShowTopSites ->
                                coroutineScope.launch {
                                    context.infernoSettingsDataStore.updateData {
                                        it.toBuilder().setShouldShowTopSites(shouldShowTopSites)
                                            .build()
                                    }
                                }
                            },
                        )
                    }

                    // show recent tabs
                    item {
                        PreferenceSwitch(
                            text = "Show recent tabs:", // todo: string res more descriptive (mention tab bar if enabled)
                            summary = null,
                            selected = settings.shouldShowRecentTabs,
                            onSelectedChange = { shouldShowRecentTabs ->
                                coroutineScope.launch {
                                    context.infernoSettingsDataStore.updateData {
                                        it.toBuilder().setShouldShowRecentTabs(shouldShowRecentTabs)
                                            .build()
                                    }
                                }
                            },
                        )
                    }

                    // show bookmarks
                    item {
                        PreferenceSwitch(
                            text = "Show bookmarks:", // todo: string res more descriptive (mention tab bar if enabled)
                            summary = null,
                            selected = settings.shouldShowBookmarks,
                            onSelectedChange = { shouldShowBookmarks ->
                                coroutineScope.launch {
                                    context.infernoSettingsDataStore.updateData {
                                        it.toBuilder().setShouldShowBookmarks(shouldShowBookmarks)
                                            .build()
                                    }
                                }
                            },
                        )
                    }

                    // show history
                    item {
                        PreferenceSwitch(
                            text = "Show history:", // todo: string res more descriptive (mention tab bar if enabled)
                            summary = null,
                            selected = settings.shouldShowHistory,
                            onSelectedChange = { shouldShowHistory ->
                                coroutineScope.launch {
                                    context.infernoSettingsDataStore.updateData {
                                        it.toBuilder().setShouldShowHistory(shouldShowHistory)
                                            .build()
                                    }
                                }
                            },
                        )
                    }

                    item { PreferenceTitle("Navigation") } // todo: string res

                    // page to open on
                    item {
                        PreferenceSelect(
                            text = "Page to open on:", // todo: string res
                            description = "What page to open on when app opened", // todo: string res
                            enabled = true,
                            selectedMenuItem = settings.pageWhenBrowserReopened,
                            menuItems = listOf(
                                InfernoSettings.PageWhenBrowserReopened.OPEN_ON_LAST_TAB,
                                InfernoSettings.PageWhenBrowserReopened.OPEN_ON_HOME_ALWAYS,
                                InfernoSettings.PageWhenBrowserReopened.OPEN_ON_HOME_AFTER_FOUR_HOURS,
                            ),
                            mapToTitle = { it.toPrefString(context) },
                            onSelectMenuItem = { selected ->
                                MainScope().launch {
                                    context.infernoSettingsDataStore.updateData {
                                        it.toBuilder().setPageWhenBrowserReopened(selected).build()
                                    }
                                }
                            },
                        )
                    }
                }

                // if should not use inferno home,
                // show option for setting home url
                false -> {
                    item {
                        fun checkForUrlError(url: String): String? {
                            return when (url.isValidWebURL()) {
                                true -> null
                                false -> "Invalid url." // todo: string res
                            }
                        }

                        var customUrl by remember { mutableStateOf(settings.customHomeUrl.ifBlank { PrefUiConst.CUSTOM_HOME_URL_DEFAULT }) }
                        var urlError by remember { mutableStateOf(checkForUrlError(customUrl)) }

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(
                                    horizontal = PrefUiConst.PREFERENCE_HORIZONTAL_PADDING,
                                    vertical = PrefUiConst.PREFERENCE_VERTICAL_PADDING,
                                ),
                            verticalArrangement = Arrangement.spacedBy(PrefUiConst.PREFERENCE_VERTICAL_INTERNAL_PADDING),
                            horizontalAlignment = Alignment.Start,
                        ) {
                            // custom url
                            InfernoText(stringResource(R.string.top_sites_edit_dialog_url_title))// "Custom Url:") // todo: string res

                            // url editor
                            InfernoOutlinedTextField(
                                value = customUrl,
                                onValueChange = {
                                    customUrl = it.trim()
                                    urlError = checkForUrlError(customUrl)
                                },
                                modifier = Modifier.fillMaxWidth(),
                                isError = urlError != null,
                                supportingText = {
                                    if (urlError != null) {
                                        InfernoText(
                                            urlError!!, infernoStyle = InfernoTextStyle.Error
                                        )
                                    }
                                },
                            )

                            // save button
                            if (customUrl != settings.customHomeUrl) {
                                InfernoButton(
                                    text = stringResource(R.string.save_changes_to_login_2),
                                    modifier = Modifier.fillMaxWidth(),
                                    onClick = {
                                        val url = customUrl
                                        if (URLUtil.isValidUrl(url)) {
                                            coroutineScope.launch {
                                                context.infernoSettingsDataStore.updateData {
                                                    it.toBuilder().setCustomHomeUrl(url).build()
                                                }
                                            }
                                        }
                                    },
                                    enabled = URLUtil.isValidUrl(customUrl),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

fun InfernoSettings.PageWhenBrowserReopened.toPrefString(context: Context): String {
    return when (this) {
        InfernoSettings.PageWhenBrowserReopened.OPEN_ON_LAST_TAB -> "Open on last tab" // todo: string res
        InfernoSettings.PageWhenBrowserReopened.OPEN_ON_HOME_ALWAYS -> "Always open on home" // todo: string res
        InfernoSettings.PageWhenBrowserReopened.OPEN_ON_HOME_AFTER_FOUR_HOURS -> "Open on home after 4 hours" // todo: string res
    }
}