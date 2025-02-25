/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package com.shmibblez.inferno.compose

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.ComposeView
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.recyclerview.widget.RecyclerView
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.shmibblez.inferno.theme.FirefoxTheme
import com.shmibblez.inferno.theme.Theme

/**
 * [RecyclerView.ViewHolder] used for Jetpack Compose UI content .
 *
 * @property composeView [ComposeView] which will be populated with Jetpack Compose UI content.
 * @param viewLifecycleOwner [LifecycleOwner] life cycle owner for the view.
 */
abstract class ComposeViewHolder(
    val composeView: ComposeView,
    viewLifecycleOwner: LifecycleOwner,
) : RecyclerView.ViewHolder(composeView) {

    /**
     * Composable that contains the content for a specific [ComposeViewHolder] implementation.
     */
    @Composable
    abstract fun Content()

    /**
     * Optional override used to disable private browsing theming and only obey dark/light theming.
     */
    open val allowPrivateTheme: Boolean = true

    init {
        composeView.setContent {
            FirefoxTheme(theme = Theme.getTheme(allowPrivateTheme = allowPrivateTheme)) {
                Content()
            }
        }

        composeView.setViewTreeLifecycleOwner(viewLifecycleOwner)
        composeView.setViewTreeSavedStateRegistryOwner(
            viewLifecycleOwner as SavedStateRegistryOwner,
        )
    }
}
