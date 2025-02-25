/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package com.shmibblez.inferno.tabstray.browser

import mozilla.components.browser.state.state.TabPartition
import mozilla.components.browser.state.state.TabSessionState
import mozilla.components.browser.state.state.isActive
import mozilla.components.browser.tabstray.TabsTray
import mozilla.components.feature.tabs.tabstray.TabsFeature
import com.shmibblez.inferno.ext.maxActiveTime
import com.shmibblez.inferno.tabstray.TabsTrayAction
import com.shmibblez.inferno.tabstray.TabsTrayStore
import com.shmibblez.inferno.utils.Settings

/**
 * An intermediary layer to consume tabs from [TabsFeature] for sorting into the various adapters.
 */
class TabSorter(
    private val settings: Settings,
    private val tabsTrayStore: TabsTrayStore? = null,
) : TabsTray {

    override fun updateTabs(tabs: List<TabSessionState>, tabPartition: TabPartition?, selectedTabId: String?) {
        val privateTabs = tabs.filter { it.content.private }
        val allNormalTabs = tabs - privateTabs
        val inactiveTabs = allNormalTabs.getInactiveTabs(settings)
        val normalTabs = allNormalTabs - inactiveTabs

        // Private tabs
        tabsTrayStore?.dispatch(TabsTrayAction.UpdatePrivateTabs(privateTabs))

        // Inactive tabs
        tabsTrayStore?.dispatch(TabsTrayAction.UpdateInactiveTabs(inactiveTabs))

        // Normal tabs
        tabsTrayStore?.dispatch(TabsTrayAction.UpdateNormalTabs(normalTabs))

        // Selected tab Id
        tabsTrayStore?.dispatch(TabsTrayAction.UpdateSelectedTabId(selectedTabId))
    }
}

/**
 * Returns a list of inactive tabs based on our preferences.
 */
private fun List<TabSessionState>.getInactiveTabs(settings: Settings): List<TabSessionState> {
    val inactiveTabsEnabled = settings.inactiveTabsAreEnabled
    return if (inactiveTabsEnabled) {
        filter { !it.isActive(maxActiveTime) }
    } else {
        emptyList()
    }
}
