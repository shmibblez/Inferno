/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package com.shmibblez.inferno.search

import android.content.DialogInterface
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.text.SpannableString
import androidx.annotation.VisibleForTesting
import androidx.appcompat.app.AlertDialog
import androidx.navigation.NavController
import mozilla.components.browser.state.action.AwesomeBarAction
import mozilla.components.browser.state.search.SearchEngine
import mozilla.components.browser.state.state.selectedOrDefaultSearchEngine
import mozilla.components.browser.state.store.BrowserStore
import mozilla.components.concept.engine.EngineSession.LoadUrlFlags
import mozilla.components.feature.tabs.TabsUseCases
import mozilla.components.support.ktx.kotlin.isUrl
import mozilla.components.ui.widgets.withCenterAlignedButtons
import com.shmibblez.inferno.BrowserDirection
//import com.shmibblez.inferno.GleanMetrics.Events
//import com.shmibblez.inferno.GleanMetrics.UnifiedSearch
import com.shmibblez.inferno.HomeActivity
import com.shmibblez.inferno.R
import com.shmibblez.inferno.components.Core
//import com.shmibblez.inferno.components.metrics.MetricsUtils
//import com.shmibblez.inferno.crashes.CrashListActivity
import com.shmibblez.inferno.ext.components
import com.shmibblez.inferno.ext.navigateSafe
import com.shmibblez.inferno.ext.telemetryName
import com.shmibblez.inferno.search.toolbar.SearchSelectorInteractor
import com.shmibblez.inferno.search.toolbar.SearchSelectorMenu
import com.shmibblez.inferno.settings.SupportUtils
import com.shmibblez.inferno.utils.Settings

/**
 * An interface that handles the view manipulation of the Search, triggered by the Interactor
 */
@Suppress("TooManyFunctions")
interface SearchController {
    fun handleUrlCommitted(url: String, fromHomeScreen: Boolean = false)
    fun handleEditingCancelled()
    fun handleTextChanged(text: String)
    fun handleUrlTapped(url: String, flags: LoadUrlFlags = LoadUrlFlags.none())
    fun handleSearchTermsTapped(searchTerms: String)
    fun handleSearchShortcutEngineSelected(searchEngine: SearchEngine)
    fun handleClickSearchEngineSettings()
    fun handleExistingSessionSelected(tabId: String)
    fun handleCameraPermissionsNeeded()
    fun handleSearchEngineSuggestionClicked(searchEngine: SearchEngine)

    /**
     * @see [SearchSelectorInteractor.onMenuItemTapped]
     */
    fun handleMenuItemTapped(item: SearchSelectorMenu.Item)
}

@Suppress("TooManyFunctions", "LongParameterList")
class SearchDialogController(
    private val activity: HomeActivity,
    private val store: BrowserStore,
    private val tabsUseCases: TabsUseCases,
    private val fragmentStore: SearchFragmentStore,
    private val navController: NavController,
    private val settings: Settings,
    private val dismissDialog: () -> Unit,
    private val clearToolbarFocus: () -> Unit,
    private val focusToolbar: () -> Unit,
    private val clearToolbar: () -> Unit,
    private val dismissDialogAndGoBack: () -> Unit,
) : SearchController {

    override fun handleUrlCommitted(url: String, fromHomeScreen: Boolean) {
        // Do not load URL if application search engine is selected.
        if (fragmentStore.state.searchEngineSource.searchEngine?.type == SearchEngine.Type.APPLICATION) {
            return
        }

        when (url) {
            // TODO: crashes
            "about:crashes" -> {
                // The list of past crashes can be accessed via "settings > about", but desktop and
                // fennec users may be used to navigating to "about:crashes". So we intercept this here
                // and open the crash list activity instead.
//                activity.startActivity(Intent(activity, CrashListActivity::class.java))
                store.dispatch(AwesomeBarAction.EngagementFinished(abandoned = false))
            }
            "about:addons" -> {
                val directions =
                    SearchDialogFragmentDirections.actionGlobalAddonsManagementFragment()
                navController.navigateSafe(R.id.searchDialogFragment, directions)
                store.dispatch(AwesomeBarAction.EngagementFinished(abandoned = false))
            }
            "moz://a" -> openSearchOrUrl(
                SupportUtils.getMozillaPageUrl(SupportUtils.MozillaPage.MANIFESTO),
            )
            else ->
                if (url.isNotBlank()) {
                    openSearchOrUrl(url)
                } else {
                    store.dispatch(AwesomeBarAction.EngagementFinished(abandoned = true))
                }
        }
        dismissDialog()
    }

    private fun openSearchOrUrl(url: String) {
        clearToolbarFocus()

        val searchEngine = fragmentStore.state.searchEngineSource.searchEngine
        val isDefaultEngine = searchEngine == fragmentStore.state.defaultEngine
        val newTab = if (settings.enableHomepageAsNewTab) {
            false
        } else {
            fragmentStore.state.tabId == null
        }

        activity.openToBrowserAndLoad(
            searchTermOrURL = url,
            newTab = newTab,
            from = BrowserDirection.FromSearchDialog,
            engine = searchEngine,
            forceSearch = !isDefaultEngine,
        )

//        if (url.isUrl() || searchEngine == null) {
////            Events.enteredUrl.record(Events.EnteredUrlExtra(autocomplete = false))
//        } else {
////            val searchAccessPoint = when (fragmentStore.state.searchAccessPoint) {
//////                MetricsUtils.Source.NONE -> MetricsUtils.Source.ACTION
////                else -> fragmentStore.state.searchAccessPoint
////            }
//
////            MetricsUtils.recordSearchMetrics(
////                searchEngine,
////                isDefaultEngine,
////                searchAccessPoint,
////                activity.components.nimbus.events,
////            )
//        }

        store.dispatch(AwesomeBarAction.EngagementFinished(abandoned = false))
    }

    override fun handleEditingCancelled() {
        clearToolbarFocus()
        dismissDialogAndGoBack()
        store.dispatch(AwesomeBarAction.EngagementFinished(abandoned = true))
    }

    override fun handleTextChanged(text: String) {
        fragmentStore.dispatch(SearchFragmentAction.UpdateQuery(text))

        // For felt private browsing mode we're no longer going to prompt the user to enable search
        // suggestions while using private browsing mode. The preference to enable them will still
        // remain in settings.
        val isFeltPrivacyEnabled = settings.feltPrivateBrowsingEnabled

        if (!isFeltPrivacyEnabled) {
            fragmentStore.dispatch(
                SearchFragmentAction.AllowSearchSuggestionsInPrivateModePrompt(
                    text.isNotEmpty() &&
                        activity.browsingModeManager.mode.isPrivate &&
                        settings.shouldShowSearchSuggestions &&
                        !settings.shouldShowSearchSuggestionsInPrivate &&
                        !settings.showSearchSuggestionsInPrivateOnboardingFinished,
                ),
            )
        }
    }

    override fun handleUrlTapped(url: String, flags: LoadUrlFlags) {
        clearToolbarFocus()

        activity.openToBrowserAndLoad(
            searchTermOrURL = url,
            newTab = if (settings.enableHomepageAsNewTab) {
                false
            } else {
                fragmentStore.state.tabId == null
            },
            from = BrowserDirection.FromSearchDialog,
            flags = flags,
        )

//        Events.enteredUrl.record(Events.EnteredUrlExtra(autocomplete = false))

        store.dispatch(AwesomeBarAction.EngagementFinished(abandoned = false))
    }

    override fun handleSearchTermsTapped(searchTerms: String) {
        clearToolbarFocus()

        val searchEngine = fragmentStore.state.searchEngineSource.searchEngine

        activity.openToBrowserAndLoad(
            searchTermOrURL = searchTerms,
            newTab = if (settings.enableHomepageAsNewTab) {
                false
            } else {
                fragmentStore.state.tabId == null
            },
            from = BrowserDirection.FromSearchDialog,
            engine = searchEngine,
            forceSearch = true,
        )

//        val searchAccessPoint = when (fragmentStore.state.searchAccessPoint) {
////            MetricsUtils.Source.NONE -> MetricsUtils.Source.SUGGESTION
//            else -> fragmentStore.state.searchAccessPoint
//        }

//        if (searchEngine != null) {
//            MetricsUtils.recordSearchMetrics(
//                searchEngine,
//                searchEngine == store.state.search.selectedOrDefaultSearchEngine,
//                searchAccessPoint,
//                activity.components.nimbus.events,
//            )
//        }

        store.dispatch(AwesomeBarAction.EngagementFinished(abandoned = false))
    }

    override fun handleSearchShortcutEngineSelected(searchEngine: SearchEngine) {
        focusToolbar()

        when {
            searchEngine.type == SearchEngine.Type.APPLICATION && searchEngine.id == Core.HISTORY_SEARCH_ENGINE_ID -> {
                fragmentStore.dispatch(SearchFragmentAction.SearchHistoryEngineSelected(searchEngine))
            }
            searchEngine.type == SearchEngine.Type.APPLICATION &&
                searchEngine.id == Core.BOOKMARKS_SEARCH_ENGINE_ID -> {
                fragmentStore.dispatch(SearchFragmentAction.SearchBookmarksEngineSelected(searchEngine))
            }
            searchEngine.type == SearchEngine.Type.APPLICATION && searchEngine.id == Core.TABS_SEARCH_ENGINE_ID -> {
                fragmentStore.dispatch(SearchFragmentAction.SearchTabsEngineSelected(searchEngine))
            }
            searchEngine == store.state.search.selectedOrDefaultSearchEngine -> {
                fragmentStore.dispatch(
                    SearchFragmentAction.SearchDefaultEngineSelected(
                        engine = searchEngine,
                        browsingMode = activity.browsingModeManager.mode,
                        settings = settings,
                    ),
                )
            }
            else -> {
                fragmentStore.dispatch(
                    SearchFragmentAction.SearchShortcutEngineSelected(
                        engine = searchEngine,
                        browsingMode = activity.browsingModeManager.mode,
                        settings = settings,
                    ),
                )
            }
        }

//        UnifiedSearch.engineSelected.record(UnifiedSearch.EngineSelectedExtra(searchEngine.telemetryName()))
    }

    override fun handleClickSearchEngineSettings() {
        clearToolbarFocus()
        val directions = SearchDialogFragmentDirections.actionGlobalSearchEngineFragment()
        navController.navigateSafe(R.id.searchDialogFragment, directions)
        store.dispatch(AwesomeBarAction.EngagementFinished(abandoned = true))
    }

    override fun handleExistingSessionSelected(tabId: String) {
        clearToolbarFocus()

        tabsUseCases.selectTab(tabId)

        activity.openToBrowser(
            from = BrowserDirection.FromSearchDialog,
        )

        store.dispatch(AwesomeBarAction.EngagementFinished(abandoned = false))
    }

    /**
     * Creates and shows an [AlertDialog] when camera permissions are needed.
     *
     * In versions above M, [AlertDialog.BUTTON_POSITIVE] takes the user to the app settings. This
     * intent only exists in M and above. Below M, [AlertDialog.BUTTON_POSITIVE] routes to a SUMO
     * help page to find the app settings.
     *
     * [AlertDialog.BUTTON_NEGATIVE] dismisses the dialog.
     */
    override fun handleCameraPermissionsNeeded() {
        val dialog = buildDialog()
        dialog.show()
    }

    override fun handleSearchEngineSuggestionClicked(searchEngine: SearchEngine) {
        clearToolbar()
        handleSearchShortcutEngineSelected(searchEngine)
    }

    override fun handleMenuItemTapped(item: SearchSelectorMenu.Item) {
        when (item) {
            SearchSelectorMenu.Item.SearchSettings -> handleClickSearchEngineSettings()
            is SearchSelectorMenu.Item.SearchEngine -> handleSearchShortcutEngineSelected(item.searchEngine)
        }
    }

    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    fun buildDialog(): AlertDialog.Builder {
        return AlertDialog.Builder(activity).apply {
            val spannableText = SpannableString(
                activity.resources.getString(R.string.camera_permissions_needed_message),
            )
            setMessage(spannableText)
            setNegativeButton(R.string.camera_permissions_needed_negative_button_text) { _, _ ->
                dismissDialog()
            }
            setPositiveButton(R.string.camera_permissions_needed_positive_button_text) {
                    dialog: DialogInterface, _ ->
                val intent: Intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                } else {
                    SupportUtils.createCustomTabIntent(
                        activity,
                        SupportUtils.getSumoURLForTopic(
                            activity,
                            SupportUtils.SumoTopic.QR_CAMERA_ACCESS,
                        ),
                    )
                }
                val uri = Uri.fromParts("package", activity.packageName, null)
                intent.data = uri
                dialog.cancel()
                activity.startActivity(intent)
            }
            setOnDismissListener {
                store.dispatch(AwesomeBarAction.EngagementFinished(abandoned = true))
            }
            create().withCenterAlignedButtons()
        }
    }
}
