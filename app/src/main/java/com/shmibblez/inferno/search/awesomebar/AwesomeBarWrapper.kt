/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package com.shmibblez.inferno.search.awesomebar

import android.content.Context
import android.util.AttributeSet
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.AbstractComposeView
import mozilla.components.browser.state.action.AwesomeBarAction
import mozilla.components.compose.browser.awesomebar.AwesomeBar
import mozilla.components.compose.browser.awesomebar.AwesomeBarDefaults
import mozilla.components.compose.browser.awesomebar.AwesomeBarOrientation
import mozilla.components.concept.awesomebar.AwesomeBar
import mozilla.components.support.ktx.android.view.hideKeyboard
//import mozilla.telemetry.glean.private.NoExtras
//import com.shmibblez.inferno.GleanMetrics.BookmarksManagement
//import com.shmibblez.inferno.GleanMetrics.History
import com.shmibblez.inferno.ext.components
import com.shmibblez.inferno.ext.settings
import com.shmibblez.inferno.theme.FirefoxTheme

/**
 * This wrapper wraps the `AwesomeBar()` composable and exposes it as a `View` and `concept-awesomebar`
 * implementation to be integrated in the view hierarchy of `SearchDialogFragment` until more parts
 * of that screen have been refactored to use Jetpack Compose.
 */
class AwesomeBarWrapper @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : AbstractComposeView(context, attrs, defStyleAttr), AwesomeBar {
    private val providers = mutableStateOf(emptyList<AwesomeBar.SuggestionProvider>())
    private val text = mutableStateOf("")
    private var onEditSuggestionListener: ((String) -> Unit)? = null
    private var onStopListener: (() -> Unit)? = null

    @Composable
    override fun Content() {
        if (providers.value.isEmpty()) {
            return
        }

        val orientation = if (context.settings().shouldUseBottomToolbar) {
            AwesomeBarOrientation.BOTTOM
        } else {
            AwesomeBarOrientation.TOP
        }

        FirefoxTheme {
            AwesomeBar(
                text = text.value,
                providers = providers.value,
                orientation = orientation,
                colors = AwesomeBarDefaults.colors(
                    background = Color.Transparent,
                    title = FirefoxTheme.colors.textPrimary,
                    description = FirefoxTheme.colors.textSecondary,
                    autocompleteIcon = FirefoxTheme.colors.textSecondary,
                    groupTitle = FirefoxTheme.colors.textSecondary,
                ),
                onSuggestionClicked = { suggestion ->
                    context.components.core.store.dispatch(AwesomeBarAction.SuggestionClicked(suggestion))
                    suggestion.onSuggestionClicked?.invoke()
//                    when {
//                        suggestion.flags.contains(AwesomeBar.Suggestion.Flag.HISTORY) -> {
//                            History.searchResultTapped.record(NoExtras())
//                        }
//                        suggestion.flags.contains(AwesomeBar.Suggestion.Flag.BOOKMARK) -> {
//                            BookmarksManagement.searchResultTapped.record(NoExtras())
//                        }
//                    }
                    onStopListener?.invoke()
                },
                onAutoComplete = { suggestion ->
                    onEditSuggestionListener?.invoke(suggestion.editSuggestion!!)
                },
                onVisibilityStateUpdated = {
                    context.components.core.store.dispatch(AwesomeBarAction.VisibilityStateUpdated(it))
                },
                onScroll = { hideKeyboard() },
                profiler = context.components.core.engine.profiler,
            )
        }
    }

    override fun addProviders(vararg providers: AwesomeBar.SuggestionProvider) {
        val newProviders = this.providers.value.toMutableList()
        newProviders.addAll(providers)
        this.providers.value = newProviders
    }

    override fun containsProvider(provider: AwesomeBar.SuggestionProvider): Boolean {
        return providers.value.any { current -> current.id == provider.id }
    }

    override fun onInputChanged(text: String) {
        this.text.value = text
    }

    override fun removeAllProviders() {
        providers.value = emptyList()
    }

    override fun removeProviders(vararg providers: AwesomeBar.SuggestionProvider) {
        val newProviders = this.providers.value.toMutableList()
        newProviders.removeAll(providers)
        this.providers.value = newProviders
    }

    override fun setOnEditSuggestionListener(listener: (String) -> Unit) {
        onEditSuggestionListener = listener
    }

    override fun setOnStopListener(listener: () -> Unit) {
        onStopListener = listener
    }
}
