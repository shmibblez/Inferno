/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package com.shmibblez.inferno.compose

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.shmibblez.inferno.theme.FirefoxTheme

/**
 * Placeholder of a [ListItemTabLarge] with the same dimensions but only a centered text.
 * Has the following structure:
 * ```
 * ---------------------------------------------
 * |                                           |
 * |                                           |
 * | Placeholder text                          |
 * |                                           |
 * |                                           |
 * ---------------------------------------------
 * ```
 *
 * @param text The only [String] that this will display.
 * @param backgroundColor The background [Color] of the list item placeholder.
 * @param onClick Optional callback to be invoked when this composable is clicked.
 */
@Composable
fun ListItemTabLargePlaceholder(
    text: String,
    backgroundColor: Color,
    onClick: () -> Unit = { },
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        modifier = Modifier
            .size(ITEM_WIDTH.dp, ITEM_HEIGHT.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation= 6.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = text,
                color = FirefoxTheme.colors.textPrimary,
                textAlign = TextAlign.Center,
                overflow = TextOverflow.Ellipsis,
                maxLines = 1,
                style = TextStyle(fontSize = 20.sp),
            )
        }
    }
}

@Composable
@Preview
private fun ListItemTabLargePlaceholderPreview() {
    FirefoxTheme {
        ListItemTabLargePlaceholder(
            text = "Item placeholder",
            backgroundColor = FirefoxTheme.colors.layer2,
        )
    }
}
