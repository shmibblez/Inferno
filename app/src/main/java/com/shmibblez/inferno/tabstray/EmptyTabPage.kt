/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package com.shmibblez.inferno.tabstray

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.shmibblez.inferno.R
import com.shmibblez.inferno.theme.FirefoxTheme

/**
 * UI for displaying the Empty Tab Page in the Tabs Tray.
 *
 * @param isPrivate Whether or not the tab is private.
 */
@Composable
internal fun EmptyTabPage(isPrivate: Boolean) {
    val testTag: String
    val emptyTextId: Int
    if (isPrivate) {
        testTag = TabsTrayTestTag.emptyPrivateTabsList
        emptyTextId = R.string.no_private_tabs_description
    } else {
        testTag = TabsTrayTestTag.emptyNormalTabsList
        emptyTextId = R.string.no_open_tabs_description
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .testTag(testTag),
    ) {
        Text(
            text = stringResource(id = emptyTextId),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 80.dp),
            color = FirefoxTheme.colors.textSecondary,
            style = FirefoxTheme.typography.body1,
        )
    }
}
