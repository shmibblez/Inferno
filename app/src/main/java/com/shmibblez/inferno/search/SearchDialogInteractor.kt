/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package com.shmibblez.inferno.search

import mozilla.components.browser.state.search.SearchEngine
import mozilla.components.concept.engine.EngineSession.LoadUrlFlags
import com.shmibblez.inferno.search.awesomebar.AwesomeBarInteractor
import com.shmibblez.inferno.search.toolbar.SearchSelectorMenu
import com.shmibblez.inferno.search.toolbar.ToolbarInteractor

/**
 * Interactor for the search screen
 * Provides implementations for the AwesomeBarView and ToolbarView
 */
@Suppress("TooManyFunctions")
class SearchDialogInteractor(
    private val searchController: SearchDialogController,
) : AwesomeBarInteractor, ToolbarInteractor {

    override fun onUrlCommitted(url: String, fromHomeScreen: Boolean) {
        searchController.handleUrlCommitted(url, fromHomeScreen)
    }

    override fun onEditingCanceled() {
        searchController.handleEditingCancelled()
    }

    override fun onTextChanged(text: String) {
        searchController.handleTextChanged(text)
    }

    override fun onUrlTapped(url: String, flags: LoadUrlFlags) {
        searchController.handleUrlTapped(url, flags)
    }

    override fun onSearchTermsTapped(searchTerms: String) {
        searchController.handleSearchTermsTapped(searchTerms)
    }

    override fun onHistorySearchTermTapped(searchTerms: String) {
        searchController.handleSearchTermsTapped(searchTerms)
    }

    override fun onSearchEngineSuggestionSelected(searchEngine: SearchEngine) {
        searchController.handleSearchEngineSuggestionClicked(searchEngine)
    }

    override fun onSearchShortcutEngineSelected(searchEngine: SearchEngine) {
        searchController.handleSearchShortcutEngineSelected(searchEngine)
    }

    override fun onClickSearchEngineSettings() {
        searchController.handleClickSearchEngineSettings()
    }

    override fun onExistingSessionSelected(tabId: String) {
        searchController.handleExistingSessionSelected(tabId)
    }

    override fun onMenuItemTapped(item: SearchSelectorMenu.Item) {
        searchController.handleMenuItemTapped(item)
    }

    fun onCameraPermissionsNeeded() {
        searchController.handleCameraPermissionsNeeded()
    }
}
