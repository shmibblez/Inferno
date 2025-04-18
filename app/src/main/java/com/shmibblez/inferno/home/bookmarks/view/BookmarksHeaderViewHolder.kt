/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package com.shmibblez.inferno.home.bookmarks.view

import android.view.View
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.LifecycleOwner
import com.shmibblez.inferno.R
import com.shmibblez.inferno.compose.ComposeViewHolder
import com.shmibblez.inferno.compose.home.HomeSectionHeader
import com.shmibblez.inferno.home.bookmarks.interactor.BookmarksInteractor

/**
 * View holder for the bookmarks header and "Show all" button.
 *
 * @param composeView [ComposeView] which will be populated with Jetpack Compose UI content.
 * @param viewLifecycleOwner [LifecycleOwner] life cycle owner for the view.
 * @param interactor [BookmarksInteractor] which will have delegated to all user interactions.
 */
class BookmarksHeaderViewHolder(
    composeView: ComposeView,
    viewLifecycleOwner: LifecycleOwner,
    private val interactor: BookmarksInteractor,
) : ComposeViewHolder(composeView, viewLifecycleOwner) {

    init {
        val horizontalPadding =
            composeView.resources.getDimensionPixelSize(R.dimen.home_item_horizontal_margin)
        composeView.setPadding(horizontalPadding, 0, horizontalPadding, 0)
    }

    @Composable
    override fun Content() {
        Column {
            Spacer(modifier = Modifier.height(40.dp))

            HomeSectionHeader(
                headerText = stringResource(R.string.home_bookmarks_title),
                description = stringResource(R.string.home_bookmarks_show_all_content_description),
                onShowAllClick = {
                    interactor.onShowAllBookmarksClicked()
                },
            )

            Spacer(Modifier.height(16.dp))
        }
    }

    companion object {
        val LAYOUT_ID = View.generateViewId()
    }
}
