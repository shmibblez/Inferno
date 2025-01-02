/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package com.shmibblez.inferno.tabstray

import androidx.compose.runtime.Composable
import com.shmibblez.inferno.tabstray.syncedtabs.SyncedTabsList
import com.shmibblez.inferno.tabstray.syncedtabs.SyncedTabsListItem
import com.shmibblez.inferno.tabstray.syncedtabs.OnTabClick as OnSyncedTabClick
import com.shmibblez.inferno.tabstray.syncedtabs.OnTabCloseClick as OnSyncedTabClose

/**
 * UI for displaying the Synced Tabs Page in the Tabs Tray.
 *
 * @param syncedTabs The list of [SyncedTabsListItem] to display.
 * @param onTabClick Invoked when the user clicks on a tab.
 * @param onTabClose Invoked when the user clicks to close a tab.
 */
@Composable
internal fun SyncedTabsPage(
    syncedTabs: List<SyncedTabsListItem>,
    onTabClick: OnSyncedTabClick,
    onTabClose: OnSyncedTabClose,
) {
    SyncedTabsList(
        syncedTabs = syncedTabs,
        onTabClick = onTabClick,
        onTabCloseClick = onTabClose,
    )
}