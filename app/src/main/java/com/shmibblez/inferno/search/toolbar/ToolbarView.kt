/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package com.shmibblez.inferno.search.toolbar

import android.annotation.SuppressLint
import android.view.ViewGroup
import androidx.annotation.VisibleForTesting
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.content.ContextCompat
import androidx.core.view.updateMargins
import mozilla.components.browser.toolbar.BrowserToolbar
import mozilla.components.concept.toolbar.Toolbar
import mozilla.components.feature.toolbar.ToolbarAutocompleteFeature
import mozilla.components.support.ktx.android.content.getColorFromAttr
import mozilla.components.support.ktx.android.content.res.resolveAttribute
import mozilla.components.support.ktx.android.view.hideKeyboard
//import com.shmibblez.inferno.GleanMetrics.Events
import com.shmibblez.inferno.R
import com.shmibblez.inferno.browser.tabstrip.isTabStripEnabled
import com.shmibblez.inferno.components.Components
import com.shmibblez.inferno.ext.settings
import com.shmibblez.inferno.search.SearchEngineSource
import com.shmibblez.inferno.search.SearchFragmentState
import com.shmibblez.inferno.utils.Settings

/**
 * Interface for the Toolbar Interactor. This interface is implemented by objects that want
 * to respond to user interaction on the [ToolbarView].
 */
interface ToolbarInteractor : SearchSelectorInteractor {

    /**
     * Called when a user hits the return key while [ToolbarView] has focus.
     *
     * @param url The text inside the [ToolbarView] when committed.
     * @param fromHomeScreen True if the toolbar has been opened from home screen.
     */
    fun onUrlCommitted(url: String, fromHomeScreen: Boolean = false)

    /**
     * Called when a user removes focus from the [ToolbarView]
     */
    fun onEditingCanceled()

    /**
     * Called whenever the text inside the [ToolbarView] changes
     *
     * @param text The current text displayed by [ToolbarView].
     */
    fun onTextChanged(text: String)
}

/**
 * View that contains and configures the BrowserToolbar to only be used in its editing mode.
 */
class ToolbarView(
    private val settings: Settings,
    private val components: Components,
    private val interactor: ToolbarInteractor,
    private val isPrivate: Boolean,
    val view: BrowserToolbar,
    fromHomeFragment: Boolean,
) {

    @VisibleForTesting
    internal var isInitialized = false

    @VisibleForTesting
    internal val autocompleteFeature = ToolbarAutocompleteFeature(
        toolbar = view,
        engine = if (!isPrivate) components.core.engine else null,
        shouldAutocomplete = {
            when (isPrivate) {
                true -> settings.shouldAutocompleteInAwesomebarPrivate
                false -> settings.shouldAutocompleteInAwesomebar
            }
        },
    )

    init {
        view.apply {
            editMode()

            if (context.settings().navigationToolbarEnabled) {
                val toolbarPadding =
                    context.resources.getDimensionPixelSize(R.dimen.toolbar_horizontal_margin_end)
                setPadding(toolbarPadding, 0, toolbarPadding, 0)
            }

            setOnUrlCommitListener {
                // We're hiding the keyboard as early as possible to prevent the engine view
                // from resizing in case the BrowserFragment is being displayed before the
                // keyboard is gone: https://github.com/mozilla-mobile/fenix/issues/8399
                hideKeyboard()
                interactor.onUrlCommitted(it, fromHomeFragment)
                false
            }

            background = AppCompatResources.getDrawable(
                context,
                context.theme.resolveAttribute(R.attr.layer1),
            )

            edit.hint = context.getString(R.string.search_hint)

            edit.colors = edit.colors.copy(
                text = context.getColorFromAttr(R.attr.textPrimary),
                hint = context.getColorFromAttr(R.attr.textSecondary),
                suggestionBackground = ContextCompat.getColor(
                    context,
                    R.color.suggestion_highlight_color,
                ),
                clear = context.getColorFromAttr(R.attr.textPrimary),
            )

            edit.setUrlBackground(
                AppCompatResources.getDrawable(context, R.drawable.search_url_background),
            )

            private = isPrivate

            setOnEditListener(
                object : Toolbar.OnEditListener {
                    override fun onCancelEditing(): Boolean {
                        interactor.onEditingCanceled()
                        // We need to return false to not show display mode
                        return false
                    }

                    override fun onTextChanged(text: String) {
                        url = text
                        interactor.onTextChanged(text)
                    }

                    override fun onInputCleared() {
//                        Events.browserToolbarInputCleared.record()
                    }
                },
            )

            if (context.isTabStripEnabled()) {
                (layoutParams as ViewGroup.MarginLayoutParams).updateMargins(
                    top = context.resources.getDimensionPixelSize(R.dimen.tab_strip_height),
                )
            }
        }
    }

    @SuppressLint("VisibleForTests")
    fun update(searchState: SearchFragmentState) {
        if (!isInitialized) {
            view.url = searchState.pastedText ?: searchState.query

            /* Only set the search terms if pasted text is null so that the search term doesn't
            overwrite pastedText when view enters `editMode` */
            if (searchState.pastedText.isNullOrEmpty()) {
                // If we're in edit mode, setting the search term will update the toolbar,
                // so we make sure we have the correct term/query to show.
                val termOrQuery = searchState.searchTerms.ifEmpty {
                    searchState.query
                }
                view.setSearchTerms(termOrQuery)
            }

            // We must trigger an onTextChanged so when search terms are set when transitioning to `editMode`
            // we have the most up to date text
            interactor.onTextChanged(view.url.toString())

            // If search terms are displayed, move the cursor to the end instead of selecting all text.
            if (searchState.searchTerms.isNotBlank()) {
                view.editMode(cursorPlacement = Toolbar.CursorPlacement.END)
            } else {
                view.editMode()
            }

            isInitialized = true
        }

        configureAutocomplete(searchState.searchEngineSource)
    }

    private fun configureAutocomplete(searchEngineSource: SearchEngineSource) {
        when (settings.showUnifiedSearchFeature) {
            true -> configureAutocompleteWithUnifiedSearch(searchEngineSource)
            else -> configureAutocompleteWithoutUnifiedSearch(searchEngineSource)
        }
    }

    private fun configureAutocompleteWithoutUnifiedSearch(searchEngineSource: SearchEngineSource) {
        when (searchEngineSource) {
            is SearchEngineSource.Default -> {
                autocompleteFeature.updateAutocompleteProviders(
                    listOfNotNull(
                        when (settings.shouldShowHistorySuggestions) {
                            true -> components.core.historyStorage
                            false -> null
                        },
                        components.core.domainsAutocompleteProvider,
                    ),
                )
            }

            else -> {
                autocompleteFeature.updateAutocompleteProviders(emptyList())
            }
        }
    }

    private fun configureAutocompleteWithUnifiedSearch(searchEngineSource: SearchEngineSource) {
        when (searchEngineSource) {
            is SearchEngineSource.Default -> {
                autocompleteFeature.updateAutocompleteProviders(
                    listOfNotNull(
                        when (settings.shouldShowHistorySuggestions) {
                            true -> components.core.historyStorage
                            false -> null
                        },
                        when (settings.shouldShowBookmarkSuggestions) {
                            true -> components.core.bookmarksStorage
                            false -> null
                        },
                        components.core.domainsAutocompleteProvider,
                    ),
                )
            }

            is SearchEngineSource.Tabs -> {
                autocompleteFeature.updateAutocompleteProviders(
                    listOf(
                        components.core.sessionAutocompleteProvider,
                        components.backgroundServices.syncedTabsAutocompleteProvider,
                    ),
                )
            }

            is SearchEngineSource.Bookmarks -> {
                autocompleteFeature.updateAutocompleteProviders(
                    listOf(
                        components.core.bookmarksStorage,
                    ),
                )
            }

            is SearchEngineSource.History -> {
                autocompleteFeature.updateAutocompleteProviders(
                    listOf(
                        components.core.historyStorage,
                    ),
                )
            }

            else -> {
                autocompleteFeature.updateAutocompleteProviders(emptyList())
            }
        }
    }
}
