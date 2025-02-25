/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package com.shmibblez.inferno.home.bookmarks

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import mozilla.components.concept.storage.BookmarkNode
import mozilla.components.support.base.feature.LifecycleAwareFeature
import com.shmibblez.inferno.components.AppStore
import com.shmibblez.inferno.components.appstate.AppAction
import com.shmibblez.inferno.components.bookmarks.BookmarksUseCase
import com.shmibblez.inferno.home.HomeFragment

/**
 * View-bound feature that retrieves a list of [BookmarkNode]s and dispatches
 * updates to the [AppStore].
 *
 * @param appStore the [AppStore] that holds the state of the [HomeFragment].
 * @param bookmarksUseCase the [BookmarksUseCase] for retrieving the list of bookmarks from storage.
 * @param scope the [CoroutineScope] used to fetch the bookmarks list
 * @param ioDispatcher the [CoroutineDispatcher] for performing read/write operations.
 */
class BookmarksFeature(
    private val appStore: AppStore,
    private val bookmarksUseCase: BookmarksUseCase,
    private val scope: CoroutineScope,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : LifecycleAwareFeature {
    private var job: Job? = null

    override fun start() {
        job = scope.launch(ioDispatcher) {
            val bookmarks = bookmarksUseCase.retrieveRecentBookmarks()
            appStore.dispatch(AppAction.BookmarksChange(bookmarks))
        }
    }

    override fun stop() {
        job?.cancel()
    }
}

/**
 * The simple metadata of a bookmark.
 *
 * @property title The title of the bookmark.
 * @property url The url of the bookmark.
 * @property previewImageUrl A preview image of the page (a.k.a. the hero image), if available.
 */
data class Bookmark(
    val title: String? = null,
    val url: String? = null,
    val previewImageUrl: String? = null,
)
