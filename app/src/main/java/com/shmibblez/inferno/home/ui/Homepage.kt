/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package com.shmibblez.inferno.home.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
//import mozilla.telemetry.glean.private.NoExtras
//import com.shmibblez.inferno.GleanMetrics.History
//import com.shmibblez.inferno.GleanMetrics.RecentlyVisitedHomepage
import com.shmibblez.inferno.R
import com.shmibblez.inferno.compose.button.TertiaryButton
import com.shmibblez.inferno.compose.home.HomeSectionHeader
import com.shmibblez.inferno.home.bookmarks.Bookmark
import com.shmibblez.inferno.home.bookmarks.interactor.BookmarksInteractor
import com.shmibblez.inferno.home.bookmarks.view.Bookmarks
import com.shmibblez.inferno.home.bookmarks.view.BookmarksMenuItem
import com.shmibblez.inferno.home.collections.Collections
import com.shmibblez.inferno.home.collections.CollectionsState
import com.shmibblez.inferno.home.fake.FakeHomepagePreview
import com.shmibblez.inferno.home.interactor.HomepageInteractor
//import com.shmibblez.inferno.home.pocket.ui.PocketSection
import com.shmibblez.inferno.home.recentsyncedtabs.view.RecentSyncedTab
import com.shmibblez.inferno.home.recenttabs.RecentTab
import com.shmibblez.inferno.home.recenttabs.interactor.RecentTabInteractor
import com.shmibblez.inferno.home.recenttabs.view.RecentTabMenuItem
import com.shmibblez.inferno.home.recenttabs.view.RecentTabs
import com.shmibblez.inferno.home.recentvisits.RecentlyVisitedItem
import com.shmibblez.inferno.home.recentvisits.RecentlyVisitedItem.RecentHistoryGroup
import com.shmibblez.inferno.home.recentvisits.RecentlyVisitedItem.RecentHistoryHighlight
import com.shmibblez.inferno.home.recentvisits.interactor.RecentVisitsInteractor
import com.shmibblez.inferno.home.recentvisits.view.RecentVisitMenuItem
import com.shmibblez.inferno.home.recentvisits.view.RecentlyVisited
import com.shmibblez.inferno.home.sessioncontrol.CollectionInteractor
import com.shmibblez.inferno.home.sessioncontrol.CustomizeHomeIteractor
import com.shmibblez.inferno.home.sessioncontrol.viewholders.FeltPrivacyModeInfoCard
import com.shmibblez.inferno.home.sessioncontrol.viewholders.PrivateBrowsingDescription
import com.shmibblez.inferno.home.store.HomepageState
import com.shmibblez.inferno.home.topsites.TopSiteColors
import com.shmibblez.inferno.home.topsites.TopSites
import com.shmibblez.inferno.theme.FirefoxTheme
import com.shmibblez.inferno.theme.Theme
import com.shmibblez.inferno.wallpapers.WallpaperState

/**
 * Top level composable for the homepage.
 *
 * @param state State representing the homepage.
 * @param interactor for interactions with the homepage UI.
 * @param onTopSitesItemBound Invoked during the composition of a top site item.
 */
@Suppress("LongMethod")
@Composable
internal fun Homepage(
    state: HomepageState,
    interactor: HomepageInteractor,
    onTopSitesItemBound: () -> Unit,
) {
    Column(
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .verticalScroll(rememberScrollState()),
    ) {
        with(state) {
            when (this) {
                is HomepageState.Private -> {
                    if (feltPrivateBrowsingEnabled) {
                        FeltPrivacyModeInfoCard(
                            onLearnMoreClick = interactor::onLearnMoreClicked,
                        )
                    } else {
                        PrivateBrowsingDescription(
                            onLearnMoreClick = interactor::onLearnMoreClicked,
                        )
                    }
                }

                is HomepageState.Normal -> {
                    if (showTopSites) {
                        TopSites(
                            topSites = topSites,
                            topSiteColors = topSiteColors,
                            interactor = interactor,
                            onTopSitesItemBound = onTopSitesItemBound,
                        )
                    }

                    if (showRecentTabs) {
                        RecentTabsSection(
                            interactor = interactor,
                            cardBackgroundColor = cardBackgroundColor,
                            recentTabs = recentTabs,
                        )

                        if (showRecentSyncedTab) {
                            Spacer(modifier = Modifier.height(8.dp))

                            RecentSyncedTab(
                                tab = syncedTab,
                                backgroundColor = cardBackgroundColor,
                                buttonBackgroundColor = buttonBackgroundColor,
                                buttonTextColor = buttonTextColor,
                                onRecentSyncedTabClick = interactor::onRecentSyncedTabClicked,
                                onSeeAllSyncedTabsButtonClick = interactor::onSyncedTabShowAllClicked,
                                onRemoveSyncedTab = interactor::onRemovedRecentSyncedTab,
                            )
                        }
                    }

                    if (showBookmarks) {
                        BookmarksSection(
                            bookmarks = bookmarks,
                            cardBackgroundColor = cardBackgroundColor,
                            interactor = interactor,
                        )
                    }

                    if (showRecentlyVisited) {
                        RecentlyVisitedSection(
                            recentVisits = recentlyVisited,
                            cardBackgroundColor = cardBackgroundColor,
                            interactor = interactor,
                        )
                    }

                    CollectionsSection(collectionsState = collectionsState, interactor = interactor)

//                    if (showPocketStories) {
//                        PocketSection(
//                            state = pocketState,
//                            cardBackgroundColor = cardBackgroundColor,
//                            interactor = interactor,
//                        )
//                    }

                    if (showCustomizeHome) {
                        CustomizeHomeButton(
                            buttonBackgroundColor = buttonBackgroundColor,
                            interactor = interactor,
                        )
                    }

                    // This is a temporary value until I can fix layout issues
                    Spacer(Modifier.height(288.dp))
                }
            }
        }
    }
}

@Composable
private fun RecentTabsSection(
    interactor: RecentTabInteractor,
    cardBackgroundColor: Color,
    recentTabs: List<RecentTab>,
) {
    Spacer(modifier = Modifier.height(40.dp))

    HomeSectionHeader(
        headerText = stringResource(R.string.recent_tabs_header),
        description = stringResource(R.string.recent_tabs_show_all_content_description_2),
        onShowAllClick = interactor::onRecentTabShowAllClicked,
    )

    Spacer(Modifier.height(16.dp))

    RecentTabs(
        recentTabs = recentTabs,
        backgroundColor = cardBackgroundColor,
        onRecentTabClick = { interactor.onRecentTabClicked(it) },
        menuItems = listOf(
            RecentTabMenuItem(
                title = stringResource(id = R.string.recent_tab_menu_item_remove),
                onClick = interactor::onRemoveRecentTab,
            ),
        ),
    )
}

@Composable
private fun BookmarksSection(
    bookmarks: List<Bookmark>,
    cardBackgroundColor: Color,
    interactor: BookmarksInteractor,
) {
    Spacer(modifier = Modifier.height(40.dp))

    HomeSectionHeader(
        headerText = stringResource(R.string.home_bookmarks_title),
        description = stringResource(R.string.home_bookmarks_show_all_content_description),
        onShowAllClick = interactor::onShowAllBookmarksClicked,
    )

    Spacer(Modifier.height(16.dp))

    Bookmarks(
        bookmarks = bookmarks,
        menuItems = listOf(
            BookmarksMenuItem(
                stringResource(id = R.string.home_bookmarks_menu_item_remove),
                onClick = interactor::onBookmarkRemoved,
            ),
        ),
        backgroundColor = cardBackgroundColor,
        onBookmarkClick = interactor::onBookmarkClicked,
    )
}

@Composable
private fun RecentlyVisitedSection(
    recentVisits: List<RecentlyVisitedItem>,
    cardBackgroundColor: Color,
    interactor: RecentVisitsInteractor,
) {
    Spacer(modifier = Modifier.height(40.dp))

    HomeSectionHeader(
        headerText = stringResource(R.string.history_metadata_header_2),
        description = stringResource(R.string.past_explorations_show_all_content_description_2),
        onShowAllClick = interactor::onHistoryShowAllClicked,
    )

    Spacer(Modifier.height(16.dp))

    RecentlyVisited(
        recentVisits = recentVisits,
        menuItems = listOfNotNull(
            RecentVisitMenuItem(
                title = stringResource(R.string.recently_visited_menu_item_remove),
                onClick = { visit ->
                    when (visit) {
                        is RecentHistoryGroup -> interactor.onRemoveRecentHistoryGroup(visit.title)
                        is RecentHistoryHighlight -> interactor.onRemoveRecentHistoryHighlight(
                            visit.url,
                        )
                    }
                },
            ),
        ),
        backgroundColor = cardBackgroundColor,
        onRecentVisitClick = { recentlyVisitedItem, pageNumber ->
            when (recentlyVisitedItem) {
                is RecentHistoryHighlight -> {
//                    RecentlyVisitedHomepage.historyHighlightOpened.record(NoExtras())
                    interactor.onRecentHistoryHighlightClicked(recentlyVisitedItem)
                }

                is RecentHistoryGroup -> {
//                    RecentlyVisitedHomepage.searchGroupOpened.record(NoExtras())
                    // TODO: history
//                    History.recentSearchesTapped.record(
//                        History.RecentSearchesTappedExtra(
//                            pageNumber.toString(),
//                        ),
//                    )
                    interactor.onRecentHistoryGroupClicked(recentlyVisitedItem)
                }
            }
        },
    )
}

@Composable
private fun CollectionsSection(
    collectionsState: CollectionsState,
    interactor: CollectionInteractor,
) {
    when (collectionsState) {
        is CollectionsState.Content -> {
            Column {
                Spacer(Modifier.height(56.dp))

                HomeSectionHeader(headerText = stringResource(R.string.collections_header))

                Spacer(Modifier.height(10.dp))

                with(collectionsState) {
                    Collections(
                        collections = collections,
                        expandedCollections = expandedCollections,
                        showAddTabToCollection = showSaveTabsToCollection,
                        interactor = interactor,
                    )
                }
            }
        }

        CollectionsState.Gone -> {} // no-op. Nothing is shown where there are no collections.
        is CollectionsState.Placeholder -> {
            CollectionsPlaceholder(collectionsState.showSaveTabsToCollection, interactor)
        }
    }
}

@Composable
private fun CustomizeHomeButton(buttonBackgroundColor: Color, interactor: CustomizeHomeIteractor) {
    Spacer(modifier = Modifier.height(68.dp))

    TertiaryButton(
        text = stringResource(R.string.browser_menu_customize_home_1),
        backgroundColor = buttonBackgroundColor,
        onClick = interactor::openCustomizeHomePage,
    )
}

@Composable
@PreviewLightDark
private fun HomepagePreview() {
    FirefoxTheme {
        Homepage(
            HomepageState.Normal(
                topSites = FakeHomepagePreview.topSites(),
                recentTabs = FakeHomepagePreview.recentTabs(),
                syncedTab = FakeHomepagePreview.recentSyncedTab(),
                bookmarks = FakeHomepagePreview.bookmarks(),
                recentlyVisited = FakeHomepagePreview.recentHistory(),
                collectionsState = CollectionsState.Placeholder(true),
//                pocketState = FakeHomepagePreview.pocketState(),
                showTopSites = true,
                showRecentTabs = true,
                showRecentSyncedTab = true,
                showBookmarks = true,
                showRecentlyVisited = true,
//                showPocketStories = true,
                topSiteColors = TopSiteColors.colors(),
                cardBackgroundColor = WallpaperState.default.cardBackgroundColor,
                buttonTextColor = WallpaperState.default.buttonTextColor,
                buttonBackgroundColor = WallpaperState.default.buttonBackgroundColor,
            ),
            interactor = FakeHomepagePreview.homepageInteractor,
            onTopSitesItemBound = {},
        )
    }
}

@Composable
@Preview
private fun PrivateHomepagePreview() {
    FirefoxTheme(theme = Theme.Private) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(color = FirefoxTheme.colors.layer1),
        ) {
            Homepage(
                HomepageState.Private(
                    feltPrivateBrowsingEnabled = false,
                ),
                interactor = FakeHomepagePreview.homepageInteractor,
                onTopSitesItemBound = {},
            )
        }
    }
}
