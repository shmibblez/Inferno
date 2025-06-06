package com.shmibblez.inferno.browser.awesomebar

/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */


import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.shmibblez.inferno.browser.UiConst
import com.shmibblez.inferno.compose.base.InfernoText
import com.shmibblez.inferno.ext.infernoTheme

/**
 * Renders a header for a group of suggestions.
 */
@Composable
internal fun SuggestionGroup(
    title: String,
) {
    InfernoText(
        title,
        fontColor = LocalContext.current.infernoTheme().value.primaryTextColor,
        modifier = Modifier
            .background(LocalContext.current.infernoTheme().value.primaryBackgroundColor.copy(alpha = UiConst.BAR_BG_ALPHA))
            .padding(
                vertical = 12.dp,
                horizontal = 16.dp,
            )
            .fillMaxWidth(),
        fontSize = 14.sp,
    )
}
