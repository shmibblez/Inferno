/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package com.shmibblez.inferno.compose.home

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.shmibblez.inferno.R
import com.shmibblez.inferno.components.components
import com.shmibblez.inferno.compose.base.InfernoText
import com.shmibblez.inferno.compose.base.InfernoTextStyle
import com.shmibblez.inferno.ext.infernoTheme
import com.shmibblez.inferno.mozillaAndroidComponents.compose.base.utils.inComposePreview
import com.shmibblez.inferno.theme.FirefoxTheme
import com.shmibblez.inferno.wallpapers.Wallpaper
import mozilla.components.lib.state.ext.observeAsComposableState

/**
 * Homepage header.
 *
 * @param headerText The header string.
 * @param description The content description for the "Show all" button.
 * @param onShowAllClick Invoked when "Show all" button is clicked.
 */
@Composable
fun HomeSectionHeader(
    headerText: String,
    description: String = "",
    onShowAllClick: (() -> Unit)? = null,
) {
    if (inComposePreview) {
        HomeSectionHeaderContent(
            headerText = headerText,
            description = description,
            onShowAllClick = onShowAllClick,
        )
    } else {
        val wallpaperState =
            components.appStore.observeAsComposableState { state -> state.wallpaperState }.value

        val wallpaperAdaptedTextColor =
            wallpaperState?.currentWallpaper?.textColor?.let { Color(it) }

        val isWallpaperDefault =
            (wallpaperState?.currentWallpaper ?: Wallpaper.Default) == Wallpaper.Default

        HomeSectionHeaderContent(
            headerText = headerText,
            textColor = when (isWallpaperDefault) {
                true -> LocalContext.current.infernoTheme().value.primaryTextColor
                false -> wallpaperAdaptedTextColor
                    ?: LocalContext.current.infernoTheme().value.primaryTextColor
            },
            description = description,
            showAllTextColor = when (isWallpaperDefault) {
                true -> LocalContext.current.infernoTheme().value.primaryActionColor
                false -> wallpaperAdaptedTextColor
                    ?: LocalContext.current.infernoTheme().value.primaryActionColor
            },
            onShowAllClick = onShowAllClick,
        )
    }
}

/**
 * Homepage header content.
 *
 * @param headerText The header string.
 * @param textColor [Color] to apply to the text.
 * @param description The content description for the "Show all" button.
 * @param showAllTextColor [Color] for the "Show all" button.
 * @param onShowAllClick Invoked when "Show all" button is clicked.
 */
@Composable
private fun HomeSectionHeaderContent(
    headerText: String,
    textColor: Color = LocalContext.current.infernoTheme().value.primaryTextColor,
    description: String = "",
    showAllTextColor: Color = LocalContext.current.infernoTheme().value.primaryActionColor,
    onShowAllClick: (() -> Unit)? = null,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        InfernoText(
            text = headerText,
            infernoStyle = InfernoTextStyle.Title,
            modifier = Modifier
                .weight(1f)
                .wrapContentHeight(align = Alignment.Top)
                .semantics { heading() },
            fontColor = textColor,
            overflow = TextOverflow.Ellipsis,
            maxLines = 2,
//            style = FirefoxTheme.typography.headline6,
        )

        onShowAllClick?.let {
            TextButton(onClick = { onShowAllClick() }) {
                InfernoText(
                    text = stringResource(id = R.string.recent_tabs_show_all),
                    modifier = Modifier
                        .padding(start = 16.dp)
                        .semantics {
                            contentDescription = description
                        },
                    infernoStyle = InfernoTextStyle.Small,
                    fontColor = showAllTextColor,
//                    style = TextStyle(
//                        color = showAllTextColor,
//                        fontSize = 14.sp,
//                    ),
                )
            }
        }
    }
}

@Composable
@Preview
private fun HomeSectionsHeaderPreview() {
    FirefoxTheme {
        HomeSectionHeader(
            headerText = stringResource(R.string.home_bookmarks_title),
            description = stringResource(R.string.home_bookmarks_show_all_content_description),
            onShowAllClick = {},
        )
    }
}
