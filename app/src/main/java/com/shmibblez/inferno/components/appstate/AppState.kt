/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package com.shmibblez.inferno.components.appstate

import mozilla.components.concept.storage.BookmarkNode
import mozilla.components.feature.tab.collections.TabCollection
import mozilla.components.feature.top.sites.TopSite
import mozilla.components.lib.crash.Crash.NativeCodeCrash
import mozilla.components.lib.crash.store.CrashState
import mozilla.components.lib.state.State
import com.shmibblez.inferno.browser.StandardSnackbarError
import com.shmibblez.inferno.browser.browsingmode.BrowsingMode
import com.shmibblez.inferno.components.appstate.readerview.ReaderViewState
import com.shmibblez.inferno.components.appstate.recommendations.ContentRecommendationsState
import com.shmibblez.inferno.components.appstate.shopping.ShoppingState
import com.shmibblez.inferno.components.appstate.snackbar.SnackbarState
import com.shmibblez.inferno.components.appstate.webcompat.WebCompatState
//import com.shmibblez.inferno.home.HomeFragment
import com.shmibblez.inferno.home.bookmarks.Bookmark
import com.shmibblez.inferno.home.recentsyncedtabs.RecentSyncedTabState
import com.shmibblez.inferno.home.recenttabs.RecentTab
import com.shmibblez.inferno.home.recentvisits.RecentlyVisitedItem
import com.shmibblez.inferno.library.history.PendingDeletionHistory
import com.shmibblez.inferno.messaging.MessagingState
import com.shmibblez.inferno.search.SearchDialogFragment
//import com.shmibblez.inferno.wallpapers.WallpaperState

/**
 * Value type that represents the state of the tabs tray.
 *
 * @property isForeground Whether or not the app is in the foreground.
 * @property inactiveTabsExpanded A flag to know if the Inactive Tabs section of the Tabs Tray
 * should be expanded when the tray is opened.
 * @property firstFrameDrawn Flag indicating whether the first frame of the homescreen has been drawn.
 * @property isSearchDialogVisible Flag indicating whether the user is interacting with the [SearchDialogFragment].
 * @property openInFirefoxRequested Flag indicating whether a custom tab should be opened in the browser.
 * @property nonFatalCrashes List of non-fatal crashes that allow the app to continue being used.
 * @property collections The list of [TabCollection] to display in the [HomeFragment].
 * @property expandedCollections A set containing the ids of the [TabCollection] that are expanded
 * in the [HomeFragment].
 * @property mode Whether the app is in private browsing mode.
 * @property orientation Current orientation of the application.
 * @property selectedTabId The currently selected tab ID. This should be bound to [BrowserStore].
 * @property topSites The list of [TopSite] in the [HomeFragment].
 * @property showCollectionPlaceholder If true, shows a placeholder when there are no collections.
 * @property recentTabs The list of recent [RecentTab] in the [HomeFragment].
 * @property recentSyncedTabState The [RecentSyncedTabState] in the [HomeFragment].
 * @property bookmarks The list of recently saved [BookmarkNode]s to show on the [HomeFragment].
 * @property recentHistory The list of [RecentlyVisitedItem]s.
 * @property recommendationState The [ContentRecommendationsState] to display.
 * @property messaging State related messages.
 * @property pendingDeletionHistoryItems The set of History items marked for removal in the UI,
 * awaiting to be removed once the Undo snackbar hides away.
 * Also serves as an in memory cache of all stories mapped by category allowing for quick stories filtering.
 * @property wallpaperState The [WallpaperState] to display in the [HomeFragment].
 * @property standardSnackbarError A snackbar error message to display.
 * @property readerViewState The [ReaderViewState] to display.
 * @property shoppingState Holds state for shopping feature that's required to live the lifetime of a session.
 * @property snackbarState The [SnackbarState] to display.
 * @property showFindInPage Whether or not to show the find in page feature.
 * @property crashState State related to the crash reporter.
 * @property wasLastTabClosedPrivate Whether the last remaining tab that was closed in private mode. This is used to
 * display an undo snackbar message relevant to the browsing mode. If null, no snackbar is shown.
 * @property wasNativeDefaultBrowserPromptShown Whether the native default browser prompt was shown to the user.
 * @property webCompatState The [WebCompatState] when the feature was last used.
 */
data class AppState(
    val isForeground: Boolean = true,
    val inactiveTabsExpanded: Boolean = false,
    val firstFrameDrawn: Boolean = false,
    val isSearchDialogVisible: Boolean = false,
    val openInFirefoxRequested: Boolean = false,
    val nonFatalCrashes: List<NativeCodeCrash> = emptyList(),
    val collections: List<TabCollection> = emptyList(),
    val expandedCollections: Set<Long> = emptySet(),
    val mode: BrowsingMode = BrowsingMode.Normal,
    val orientation: OrientationMode = OrientationMode.Undefined,
    val selectedTabId: String? = null,
    val topSites: List<TopSite> = emptyList(),
    val showCollectionPlaceholder: Boolean = false,
    val recentTabs: List<RecentTab> = emptyList(),
    val recentSyncedTabState: RecentSyncedTabState = RecentSyncedTabState.None,
    val bookmarks: List<Bookmark> = emptyList(),
    val recentHistory: List<RecentlyVisitedItem> = emptyList(),
    val recommendationState: ContentRecommendationsState = ContentRecommendationsState(),
    val messaging: MessagingState = MessagingState(),
    val pendingDeletionHistoryItems: Set<PendingDeletionHistory> = emptySet(),
    val wallpaperState: WallpaperState = WallpaperState.default,
    val standardSnackbarError: StandardSnackbarError? = null,
    val readerViewState: ReaderViewState = ReaderViewState.None,
    val shoppingState: ShoppingState = ShoppingState(),
    val snackbarState: SnackbarState = SnackbarState.None,
    val showFindInPage: Boolean = false,
    val crashState: CrashState = CrashState.Idle,
    val wasLastTabClosedPrivate: Boolean? = null,
    val wasNativeDefaultBrowserPromptShown: Boolean = false,
    val webCompatState: WebCompatState? = null,
) : State