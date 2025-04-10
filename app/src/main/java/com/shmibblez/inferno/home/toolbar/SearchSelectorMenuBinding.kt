/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package com.shmibblez.inferno.home.toolbar

import android.content.Context
import androidx.core.graphics.drawable.toDrawable
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import mozilla.components.browser.state.search.SearchEngine
import mozilla.components.browser.state.state.BrowserState
import mozilla.components.browser.state.store.BrowserStore
import mozilla.components.concept.menu.candidate.DrawableMenuIcon
import mozilla.components.concept.menu.candidate.TextMenuCandidate
import mozilla.components.lib.state.helpers.AbstractBinding
import mozilla.components.support.ktx.android.content.getColorFromAttr
import com.shmibblez.inferno.R
import com.shmibblez.inferno.search.ext.searchEngineShortcuts
import com.shmibblez.inferno.search.toolbar.SearchSelectorInteractor
import com.shmibblez.inferno.search.toolbar.SearchSelectorMenu
import kotlinx.coroutines.ExperimentalCoroutinesApi

/**
 * A binding that updates the search engine menu items in the search selector menu.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class SearchSelectorMenuBinding(
    private val context: Context,
    private val interactor: SearchSelectorInteractor,
    private val searchSelectorMenu: SearchSelectorMenu,
    browserStore: BrowserStore,
) : AbstractBinding<BrowserState>(browserStore) {

    override suspend fun onState(flow: Flow<BrowserState>) {
        flow.map { state -> state.search }
            .distinctUntilChanged()
            .collect { search ->
                updateSearchSelectorMenu(search.searchEngineShortcuts)
            }
    }

    private fun updateSearchSelectorMenu(searchEngines: List<SearchEngine>) {
        val searchEngineList = searchEngines
            .map {
                TextMenuCandidate(
                    text = it.name,
                    start = DrawableMenuIcon(
                        drawable = it.icon.toDrawable(context.resources),
                        tint = if (it.type == SearchEngine.Type.APPLICATION) {
                            context.getColorFromAttr(R.attr.textPrimary)
                        } else {
                            null
                        },
                    ),
                ) {
                    interactor.onMenuItemTapped(SearchSelectorMenu.Item.SearchEngine(it))
                }
            }

        searchSelectorMenu.menuController.submitList(searchSelectorMenu.menuItems(searchEngineList))
    }
}
