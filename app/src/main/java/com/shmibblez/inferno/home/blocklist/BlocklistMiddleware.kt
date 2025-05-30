/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package com.shmibblez.inferno.home.blocklist

import mozilla.components.lib.state.Middleware
import mozilla.components.lib.state.MiddlewareContext
import com.shmibblez.inferno.components.appstate.AppAction
import com.shmibblez.inferno.components.appstate.AppState
import com.shmibblez.inferno.home.recenttabs.RecentTab

/**
 * This [Middleware] reacts to item removals from the home screen, adding them to a blocklist.
 * Additionally, it reacts to state changes in order to filter them by the blocklist.
 *
 * @param blocklistHandler An instance of [BlocklistHandler] for interacting with the blocklist
 * stored in shared preferences.
 */
class BlocklistMiddleware(
    private val blocklistHandler: BlocklistHandler,
) : Middleware<AppState, AppAction> {

    /**
     * Will filter "Change" actions using the blocklist and use "Remove" actions to update
     * the blocklist.
     */
    override fun invoke(
        context: MiddlewareContext<AppState, AppAction>,
        next: (AppAction) -> Unit,
        action: AppAction,
    ) {
        next(getUpdatedAction(context.state, action))
    }

    @Suppress("ComplexMethod")
    private fun getUpdatedAction(
        state: AppState,
        action: AppAction,
    ) = with(blocklistHandler) {
        when (action) {
            is AppAction.Change -> {
                action.copy(
                    bookmarks = action.bookmarks.filteredByBlocklist(),
                    recentTabs = action.recentTabs.filteredByBlocklist().filterContile(),
                    recentHistory = action.recentHistory.filteredByBlocklist().filterContile(),
                    recentSyncedTabState = action.recentSyncedTabState.filteredByBlocklist().filterContile(),
                )
            }
            is AppAction.RecentTabsChange -> {
                action.copy(
                    recentTabs = action.recentTabs.filteredByBlocklist().filterContile(),
                )
            }
            is AppAction.BookmarksChange -> {
                action.copy(
                    bookmarks = action.bookmarks.filteredByBlocklist(),
                )
            }
            is AppAction.RecentHistoryChange -> {
                action.copy(recentHistory = action.recentHistory.filteredByBlocklist().filterContile())
            }
            is AppAction.RecentSyncedTabStateChange -> {
                action.copy(
                    state = action.state.filteredByBlocklist().filterContile(),
                )
            }
            is AppAction.RemoveRecentTab -> {
                if (action.recentTab is RecentTab.Tab) {
                    addUrlToBlocklist(action.recentTab.state.content.url)
                    state.toActionFilteringAllState(this)
                } else {
                    action
                }
            }
            is AppAction.RemoveBookmark -> {
                action.bookmark.url?.let { url ->
                    addUrlToBlocklist(url)
                    state.toActionFilteringAllState(this)
                } ?: action
            }
            is AppAction.RemoveRecentHistoryHighlight -> {
                addUrlToBlocklist(action.highlightUrl)
                state.toActionFilteringAllState(this)
            }
            is AppAction.RemoveRecentSyncedTab -> {
                addUrlToBlocklist(action.syncedTab.url)
                state.toActionFilteringAllState(this)
            }
            else -> action
        }
    }

    // When an item is removed from any part of the state, it should also be removed from any other
    // relevant parts that contain it.
    // This is a candidate for refactoring once context receivers lands in Kotlin 1.6.20
    // https://blog.jetbrains.com/kotlin/2022/02/kotlin-1-6-20-m1-released/#prototype-of-context-receivers-for-kotlin-jvm
    private fun AppState.toActionFilteringAllState(blocklistHandler: BlocklistHandler) =
        with(blocklistHandler) {
            AppAction.Change(
                recentTabs = recentTabs.filteredByBlocklist().filterContile(),
                bookmarks = bookmarks.filteredByBlocklist(),
                recentHistory = recentHistory.filteredByBlocklist().filterContile(),
                topSites = topSites,
                mode = mode,
                collections = collections,
                showCollectionPlaceholder = showCollectionPlaceholder,
                recentSyncedTabState = recentSyncedTabState.filteredByBlocklist().filterContile(),
            )
        }
}
