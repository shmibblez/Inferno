/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package com.shmibblez.inferno.home.recentvisits.view

import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.FlowColumn
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.unit.dp
import com.shmibblez.inferno.R
import com.shmibblez.inferno.compose.ext.thenConditional
import com.shmibblez.inferno.compose.list.FaviconListItem
import com.shmibblez.inferno.compose.list.IconListItem
import com.shmibblez.inferno.compose.menu.DropdownMenu
import com.shmibblez.inferno.compose.menu.MenuItem
import com.shmibblez.inferno.compose.text.Text
import com.shmibblez.inferno.ext.infernoTheme
import com.shmibblez.inferno.home.recentvisits.RecentlyVisitedItem
import com.shmibblez.inferno.home.recentvisits.RecentlyVisitedItem.RecentHistoryGroup
import com.shmibblez.inferno.home.recentvisits.RecentlyVisitedItem.RecentHistoryHighlight
import com.shmibblez.inferno.mozillaAndroidComponents.compose.base.Divider
import com.shmibblez.inferno.theme.FirefoxTheme
import mozilla.components.support.ktx.kotlin.trimmed

// Number of recently visited items per column.
private const val VISITS_PER_COLUMN = 3

private val recentlyVisitedItemMaxWidth = 320.dp

private val horizontalArrangementSpacing = 32.dp
private val contentPadding = 16.dp

/**
 * A list of recently visited items.
 *
 * @param recentVisits List of [RecentlyVisitedItem] to display.
 * @param menuItems List of [RecentVisitMenuItem] shown long clicking a [RecentlyVisitedItem].
 * @param backgroundColor The background [Color] of each item.
 * @param onRecentVisitClick Invoked when the user clicks on a recent visit. The first parameter is
 * the [RecentlyVisitedItem] that was clicked and the second parameter is the "page" or column number
 * the item resides in.
 */
@Composable
fun RecentlyVisited(
    recentVisits: List<RecentlyVisitedItem>,
    menuItems: List<RecentVisitMenuItem>,
    backgroundColor: Color = FirefoxTheme.colors.layer2,
    onRecentVisitClick: (RecentlyVisitedItem, pageNumber: Int) -> Unit = { _, _ -> },
) {
    val isSingleColumn by remember(recentVisits) { derivedStateOf { recentVisits.size <= VISITS_PER_COLUMN } }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .thenConditional(
                modifier = Modifier.horizontalScroll(state = rememberScrollState()),
                predicate = { !isSingleColumn },
            )
            .background(LocalContext.current.infernoTheme().value.secondaryBackgroundColor)
            .padding(
                vertical = 8.dp,
            ),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp)),
//            colors = CardDefaults.cardColors(containerColor = backgroundColor),
//            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
        ) {
            FlowColumn(
                modifier = Modifier.fillMaxWidth(),
                maxItemsInEachColumn = VISITS_PER_COLUMN,
                horizontalArrangement = Arrangement.spacedBy(horizontalArrangementSpacing),
            ) {
                recentVisits.forEachIndexed { index, recentVisit ->
                    // Don't display the divider when its the last item in a column or the last item
                    // in the table.
                    val showDivider =
                        (index + 1) % VISITS_PER_COLUMN != 0 && index != recentVisits.lastIndex
                    val pageIndex = index / VISITS_PER_COLUMN
                    val pageNumber = pageIndex + 1

                    Box(
                        modifier = if (isSingleColumn) {
                            Modifier.fillMaxWidth()
                        } else {
                            Modifier.widthIn(max = recentlyVisitedItemMaxWidth)
                        },
                    ) {
                        when (recentVisit) {
                            is RecentHistoryHighlight -> RecentlyVisitedHistoryHighlight(
                                recentVisit = recentVisit,
                                menuItems = menuItems,
                                onRecentVisitClick = {
                                    onRecentVisitClick(it, pageNumber)
                                },
                            )

                            is RecentHistoryGroup -> RecentlyVisitedHistoryGroup(
                                recentVisit = recentVisit,
                                menuItems = menuItems,
                                onRecentVisitClick = {
                                    onRecentVisitClick(it, pageNumber)
                                },
                            )
                        }

                        if (showDivider) {
                            Divider(
                                modifier = Modifier
                                    .align(Alignment.BottomCenter)
                                    .padding(horizontal = contentPadding),
                                color = LocalContext.current.infernoTheme().value.primaryIconColor
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * A recently visited history group.
 *
 * @param recentVisit The [RecentHistoryGroup] to display.
 * @param menuItems List of [RecentVisitMenuItem] to display in a recent visit dropdown menu.
 * @param onRecentVisitClick Invoked when the user clicks on a recent visit.
 */
@Composable
private fun RecentlyVisitedHistoryGroup(
    recentVisit: RecentHistoryGroup,
    menuItems: List<RecentVisitMenuItem>,
    onRecentVisitClick: (RecentHistoryGroup) -> Unit = { _ -> },
) {
    var isMenuExpanded by remember { mutableStateOf(false) }
    val captionId = if (recentVisit.historyMetadata.size == 1) {
        R.string.history_search_group_site_1
    } else {
        R.string.history_search_group_sites_1
    }

    Box {
        IconListItem(
            label = recentVisit.title.trimmed(),
            modifier = Modifier.combinedClickable(
                    onClick = { onRecentVisitClick(recentVisit) },
                    onLongClick = { isMenuExpanded = true },
                ),
            beforeIconPainter = painterResource(R.drawable.ic_multiple_tabs_24),
            description = stringResource(id = captionId, recentVisit.historyMetadata.size),
        )

        DropdownMenu(
            menuItems = menuItems.map { item ->
                MenuItem.TextItem(Text.String(item.title)) { item.onClick(recentVisit) }
            },
            expanded = isMenuExpanded,
            modifier = Modifier.semantics {
                testTagsAsResourceId = true
                testTag = "recent.visit.menu"
            },
            onDismissRequest = { isMenuExpanded = false },
        )
    }
}

/**
 * A recently visited history item.
 *
 * @param recentVisit The [RecentHistoryHighlight] to display.
 * @param menuItems List of [RecentVisitMenuItem] to display in a recent visit dropdown menu.
 * @param onRecentVisitClick Invoked when the user clicks on a recent visit.
 */
@Composable
private fun RecentlyVisitedHistoryHighlight(
    recentVisit: RecentHistoryHighlight,
    menuItems: List<RecentVisitMenuItem>,
    onRecentVisitClick: (RecentHistoryHighlight) -> Unit = { _ -> },
) {
    var isMenuExpanded by remember { mutableStateOf(false) }

    Box {
        FaviconListItem(
            label = recentVisit.title.trimmed(),
            url = recentVisit.url,
            modifier = Modifier.combinedClickable(
                    onClick = { onRecentVisitClick(recentVisit) },
                    onLongClick = { isMenuExpanded = true },
                ),
        )

        DropdownMenu(
            expanded = isMenuExpanded,
            menuItems = menuItems.map { item ->
                MenuItem.TextItem(Text.String(item.title)) { item.onClick(recentVisit) }
            },
            modifier = Modifier.semantics {
                testTagsAsResourceId = true
                testTag = "recent.visit.menu"
            },
            onDismissRequest = { isMenuExpanded = false },
        )
    }
}