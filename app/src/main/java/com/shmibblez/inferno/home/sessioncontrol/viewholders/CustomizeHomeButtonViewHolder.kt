/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package com.shmibblez.inferno.home.sessioncontrol.viewholders

import android.view.View
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.LifecycleOwner
import mozilla.components.lib.state.ext.observeAsComposableState
import com.shmibblez.inferno.R
import com.shmibblez.inferno.components.components
import com.shmibblez.inferno.compose.ComposeViewHolder
import com.shmibblez.inferno.compose.button.TertiaryButton
import com.shmibblez.inferno.ext.settings
import com.shmibblez.inferno.home.sessioncontrol.CustomizeHomeIteractor
import com.shmibblez.inferno.theme.FirefoxTheme
import com.shmibblez.inferno.wallpapers.WallpaperState

class CustomizeHomeButtonViewHolder(
    composeView: ComposeView,
    viewLifecycleOwner: LifecycleOwner,
    private val interactor: CustomizeHomeIteractor,
) : ComposeViewHolder(composeView, viewLifecycleOwner) {

    companion object {
        val LAYOUT_ID = View.generateViewId()
    }

    private val bottomPadding = composeView.context.settings().getBottomToolbarContainerHeight()

    init {
        val horizontalPadding =
            composeView.resources.getDimensionPixelSize(R.dimen.home_item_horizontal_margin)
        composeView.setPadding(horizontalPadding, 0, horizontalPadding, bottomPadding)
    }

    @Composable
    override fun Content() {
        val wallpaperState = components.appStore
            .observeAsComposableState { state -> state.wallpaperState }.value ?: WallpaperState.default
        var buttonColor: Color = FirefoxTheme.colors.actionTertiary

        wallpaperState.composeRunIfWallpaperCardColorsAreAvailable { cardColorLight, cardColorDark ->
            buttonColor = if (isSystemInDarkTheme()) {
                cardColorDark
            } else {
                cardColorLight
            }
        }

        Column {
            Spacer(modifier = Modifier.height(68.dp))

            TertiaryButton(
                text = stringResource(R.string.browser_menu_customize_home_1),
                backgroundColor = buttonColor,
                onClick = interactor::openCustomizeHomePage,
            )
        }
    }
}
