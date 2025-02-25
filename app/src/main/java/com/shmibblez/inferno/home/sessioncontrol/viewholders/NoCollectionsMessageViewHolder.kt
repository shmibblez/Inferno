/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package com.shmibblez.inferno.home.sessioncontrol.viewholders

import android.view.View
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.lifecycle.LifecycleOwner
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import mozilla.components.browser.state.selector.normalTabs
import mozilla.components.browser.state.store.BrowserStore
import mozilla.components.lib.state.ext.flowScoped
import mozilla.components.support.ktx.android.content.getColorFromAttr
import com.shmibblez.inferno.R
import com.shmibblez.inferno.components.AppStore
import com.shmibblez.inferno.databinding.NoCollectionsMessageBinding
import com.shmibblez.inferno.ext.increaseTapArea
import com.shmibblez.inferno.ext.isSystemInDarkTheme
import com.shmibblez.inferno.home.sessioncontrol.CollectionInteractor
import com.shmibblez.inferno.utils.view.ViewHolder

class NoCollectionsMessageViewHolder(
    view: View,
    viewLifecycleOwner: LifecycleOwner,
    store: BrowserStore,
    appStore: AppStore,
    interactor: CollectionInteractor,
) : ViewHolder(view) {

    init {
        val binding = NoCollectionsMessageBinding.bind(view)

        binding.addTabsToCollectionsButton.apply {
            setOnClickListener {
                interactor.onAddTabsToCollectionTapped()
            }
            isVisible = store.state.normalTabs.isNotEmpty()
        }

        binding.removeCollectionPlaceholder.apply {
            increaseTapArea(
                view.resources.getDimensionPixelSize(R.dimen.tap_increase_16),
            )
            setOnClickListener {
                interactor.onRemoveCollectionsPlaceholder()
            }
        }

        store.flowScoped(viewLifecycleOwner) { flow ->
            flow.map { state -> state.normalTabs.size }
                .distinctUntilChanged()
                .collect { tabs ->
                    binding.addTabsToCollectionsButton.isVisible = tabs > 0
                }
        }

        appStore.flowScoped(viewLifecycleOwner) { flow ->
            flow.map { state -> state.wallpaperState }
                .distinctUntilChanged()
                .collect { wallpaperState ->
                    val textColor = wallpaperState.currentWallpaper.textColor
                    if (textColor == null) {
                        val context = view.context
                        binding.noCollectionsHeader.setTextColor(
                            context.getColorFromAttr(R.attr.textPrimary),
                        )
                        binding.noCollectionsDescription.setTextColor(
                            context.getColorFromAttr(R.attr.textSecondary),
                        )
                        binding.removeCollectionPlaceholder.setColorFilter(
                            context.getColorFromAttr(R.attr.textPrimary),
                        )
                    } else {
                        val color = Color(textColor).toArgb()
                        binding.noCollectionsHeader.setTextColor(color)
                        binding.noCollectionsDescription.setTextColor(color)
                        binding.removeCollectionPlaceholder.setColorFilter(color)
                    }

                    var buttonColor = ContextCompat.getColor(view.context, R.color.fx_mobile_action_color_primary)
                    var buttonTextColor = ContextCompat.getColor(
                        view.context,
                        R.color.fx_mobile_text_color_action_primary,
                    )
                    wallpaperState.runIfWallpaperCardColorsAreAvailable { _, _ ->
                        buttonColor = ContextCompat.getColor(view.context, R.color.fx_mobile_layer_color_1)

                        if (!view.context.isSystemInDarkTheme()) {
                            buttonTextColor = ContextCompat.getColor(
                                view.context,
                                R.color.fx_mobile_text_color_action_secondary,
                            )
                        }
                    }

                    binding.addTabsToCollectionsButton.setBackgroundColor(buttonColor)
                    binding.addTabsToCollectionsButton.setTextColor(buttonTextColor)
                }
        }
    }

    companion object {
        var LAYOUT_ID = R.layout.no_collections_message
    }
}
