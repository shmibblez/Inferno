/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package com.shmibblez.inferno.compose.tabstray

import androidx.compose.animation.rememberSplineBasedDecay
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.text.BidiFormatter
import mozilla.components.browser.state.state.TabSessionState
import mozilla.components.browser.state.state.createTab
import com.shmibblez.inferno.mozillaAndroidComponents.compose.base.Divider
import com.shmibblez.inferno.mozillaAndroidComponents.compose.base.annotation.LightDarkPreview
import mozilla.components.support.ktx.kotlin.MAX_URI_LENGTH
import mozilla.components.ui.colors.PhotonColors
import com.shmibblez.inferno.R
import com.shmibblez.inferno.compose.HorizontalFadingEdgeBox
import com.shmibblez.inferno.compose.SwipeToDismissBox
import com.shmibblez.inferno.compose.SwipeToDismissState
import com.shmibblez.inferno.compose.TabThumbnail
import com.shmibblez.inferno.compose.rememberSwipeToDismissState
import com.shmibblez.inferno.tabstray.TabsTrayTestTag
import com.shmibblez.inferno.tabstray.ext.toDisplayTitle
import com.shmibblez.inferno.theme.FirefoxTheme

/**
 * Tab grid item used to display a tab that supports clicks,
 * long clicks, multiple selection, and media controls.
 *
 * @param tab The given tab to be render as view a grid item.
 * @param thumbnailSize Size of tab's thumbnail.
 * @param isSelected Indicates if the item should be render as selected.
 * @param multiSelectionEnabled Indicates if the item should be render with multi selection options,
 * enabled.
 * @param multiSelectionSelected Indicates if the item should be render as multi selection selected
 * option.
 * @param shouldClickListen Whether or not the item should stop listening to click events.
 * @param swipeState The swipe state of the item.
 * @param onCloseClick Callback to handle the click event of the close button.
 * @param onMediaClick Callback to handle when the media item is clicked.
 * @param onClick Callback to handle when item is clicked.
 * @param onLongClick Optional callback to handle when item is long clicked.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
@Suppress("LongMethod")
fun TabGridItem(
    tab: TabSessionState,
    thumbnailSize: Int,
    isSelected: Boolean = false,
    multiSelectionEnabled: Boolean = false,
    multiSelectionSelected: Boolean = false,
    shouldClickListen: Boolean = true,
    swipeState: SwipeToDismissState = rememberSwipeToDismissState(
        density = LocalDensity.current,
        decayAnimationSpec = rememberSplineBasedDecay(),
    ),
    onCloseClick: (tab: TabSessionState) -> Unit,
    onMediaClick: (tab: TabSessionState) -> Unit,
    onClick: (tab: TabSessionState) -> Unit,
    onLongClick: ((tab: TabSessionState) -> Unit)? = null,
) {
    val tabBorderModifier = if (isSelected) {
        Modifier.border(
            4.dp,
            FirefoxTheme.colors.borderAccent,
            RoundedCornerShape(12.dp),
        )
    } else {
        Modifier
    }

    // Used to propagate the ripple effect to the whole tab
    val interactionSource = remember { MutableInteractionSource() }

    SwipeToDismissBox(
        state = swipeState,
        backgroundContent = {},
        onItemDismiss = {
            onCloseClick(tab)
        },
    ) {
        Box(
            modifier = Modifier
                .wrapContentSize()
                .testTag(TabsTrayTestTag.tabItemRoot),
        ) {
            val clickableModifier = if (onLongClick == null) {
                Modifier.clickable(
                    enabled = shouldClickListen,
                    interactionSource = interactionSource,
                    indication = ripple(
                        color = clickableColor(),
                    ),
                    onClick = { onClick(tab) },
                )
            } else {
                Modifier.combinedClickable(
                    enabled = shouldClickListen,
                    interactionSource = interactionSource,
                    indication = ripple(
                        color = clickableColor(),
                    ),
                    onLongClick = { onLongClick(tab) },
                    onClick = { onClick(tab) },
                )
            }
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(202.dp)
                    .padding(4.dp)
                    .then(tabBorderModifier)
                    .padding(4.dp)
                    .then(clickableModifier)
                    .semantics {
                        selected = isSelected
                    },
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                shape = RoundedCornerShape(dimensionResource(id = R.dimen.tab_tray_grid_item_border_radius)),
                border = BorderStroke(1.dp, FirefoxTheme.colors.borderPrimary),
            ) {
                Column(
                    modifier = Modifier.background(FirefoxTheme.colors.layer2),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .wrapContentHeight(),
                    ) {
                        Spacer(modifier = Modifier.width(8.dp))

                        tab.content.icon?.let { icon ->
                            icon.prepareToDraw()
                            Image(
                                bitmap = icon.asImageBitmap(),
                                contentDescription = null,
                                modifier = Modifier
                                    .align(Alignment.CenterVertically)
                                    .size(16.dp),
                            )
                        }

                        HorizontalFadingEdgeBox(
                            modifier = Modifier
                                .weight(1f)
                                .wrapContentHeight()
                                .requiredHeight(30.dp)
                                .padding(7.dp, 5.dp)
                                .clipToBounds(),
                            backgroundColor = FirefoxTheme.colors.layer2,
                            isContentRtl = BidiFormatter.getInstance().isRtl(tab.content.title),
                        ) {
                            Text(
                                text = tab.toDisplayTitle().take(MAX_URI_LENGTH),
                                fontSize = 14.sp,
                                maxLines = 1,
                                softWrap = false,
                                style = TextStyle(
                                    color = FirefoxTheme.colors.textPrimary,
                                    textDirection = TextDirection.Content,
                                ),
                            )
                        }

                        if (!multiSelectionEnabled) {
                            IconButton(
                                modifier = Modifier
                                    .size(24.dp)
                                    .align(Alignment.CenterVertically)
                                    .testTag(TabsTrayTestTag.tabItemClose),
                                onClick = {
                                    onCloseClick(tab)
                                },
                            ) {
                                Icon(
                                    painter = painterResource(id = R.drawable.mozac_ic_cross_20),
                                    contentDescription = stringResource(
                                        id = R.string.close_tab_title,
                                        tab.toDisplayTitle(),
                                    ),
                                    tint = FirefoxTheme.colors.iconPrimary,
                                )
                            }
                        }
                    }

                    Divider()

                    Thumbnail(
                        tab = tab,
                        size = thumbnailSize,
                        multiSelectionSelected = multiSelectionSelected,
                    )
                }
            }

            if (!multiSelectionEnabled) {
                MediaImage(
                    tab = tab,
                    onMediaIconClicked = { onMediaClick(tab) },
                    modifier = Modifier
                        .align(Alignment.TopStart),
                    interactionSource = interactionSource,
                )
            }
        }
    }
}

@Composable
private fun clickableColor() = when (isSystemInDarkTheme()) {
    true -> PhotonColors.White
    false -> PhotonColors.Black
}

/**
 * Thumbnail specific for the [TabGridItem], which can be selected.
 *
 * @param tab Tab, containing the thumbnail to be displayed.
 * @param size Size of the thumbnail.
 * @param multiSelectionSelected Whether or not the multiple selection is enabled.
 */
@Composable
private fun Thumbnail(
    tab: TabSessionState,
    size: Int,
    multiSelectionSelected: Boolean,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(FirefoxTheme.colors.layer2)
            .semantics(mergeDescendants = true) {
                testTag = TabsTrayTestTag.tabItemThumbnail
            },
    ) {
        TabThumbnail(
            tab = tab,
            size = size,
            modifier = Modifier.fillMaxSize(),
        )

        if (multiSelectionSelected) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(FirefoxTheme.colors.layerAccentNonOpaque),
            )

            Card(
                colors = CardDefaults.cardColors(containerColor = FirefoxTheme.colors.layerAccent),
                modifier = Modifier
                    .size(size = 40.dp)
                    .align(alignment = Alignment.Center),
                shape = CircleShape,
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.mozac_ic_checkmark_24),
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(all = 8.dp),
                    contentDescription = null,
                    tint = colorResource(id = R.color.mozac_ui_icons_fill),
                )
            }
        }
    }
}

@Composable
@LightDarkPreview
private fun TabGridItemPreview() {
    FirefoxTheme {
        TabGridItem(
            tab = createTab(
                url = "www.mozilla.com",
                title = "Mozilla Domain",
            ),
            thumbnailSize = 108,
            onCloseClick = {},
            onMediaClick = {},
            onClick = {},
        )
    }
}

@Composable
@LightDarkPreview
private fun TabGridItemSelectedPreview() {
    FirefoxTheme {
        TabGridItem(
            tab = createTab(url = "www.mozilla.com", title = "Mozilla"),
            thumbnailSize = 108,
            isSelected = true,
            onCloseClick = {},
            onMediaClick = {},
            onClick = {},
            onLongClick = {},
        )
    }
}

@Composable
@LightDarkPreview
private fun TabGridItemMultiSelectedPreview() {
    FirefoxTheme {
        TabGridItem(
            tab = createTab(url = "www.mozilla.com", title = "Mozilla"),
            thumbnailSize = 108,
            multiSelectionEnabled = true,
            multiSelectionSelected = true,
            onCloseClick = {},
            onMediaClick = {},
            onClick = {},
            onLongClick = {},
        )
    }
}
