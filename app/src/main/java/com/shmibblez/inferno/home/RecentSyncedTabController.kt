/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package com.shmibblez.inferno.home.recentsyncedtabs.controller

import androidx.navigation.NavController
import mozilla.components.feature.tabs.TabsUseCases
//import com.shmibblez.inferno.GleanMetrics.RecentSyncedTabs
import com.shmibblez.inferno.R
import com.shmibblez.inferno.components.AppStore
import com.shmibblez.inferno.components.appstate.AppAction
import com.shmibblez.inferno.home.HomeFragment
import com.shmibblez.inferno.home.HomeFragmentDirections
import com.shmibblez.inferno.home.recentsyncedtabs.RecentSyncedTab
import com.shmibblez.inferno.home.recentsyncedtabs.interactor.RecentSyncedTabInteractor
import com.shmibblez.inferno.tabstray.Page
import com.shmibblez.inferno.tabstray.TabsTrayAccessPoint

/**
 * An interface that handles the view manipulation of the recent synced tabs in the Home screen.
 */
interface RecentSyncedTabController {
    /**
     * @see [RecentSyncedTabInteractor.onRecentSyncedTabClicked]
     */
    fun handleRecentSyncedTabClick(tab: RecentSyncedTab)

    /**
     * @see [RecentSyncedTabInteractor.onRecentSyncedTabClicked]
     */
    fun handleSyncedTabShowAllClicked()

    /**
     * Handle removing the synced tab from the homescreen.
     *
     * @param tab The recent synced tab to be removed.
     */
    fun handleRecentSyncedTabRemoved(tab: RecentSyncedTab)
}

/**
 * The default implementation of [RecentSyncedTabController].
 *
 * @param tabsUseCase Use cases to open the synced tab when clicked.
 * @param navController [NavController] to navigate to synced tabs tray.
 * @param accessPoint The action or screen that was used to navigate to the tabs tray.
 * @param appStore The [AppStore] that holds the state of the [HomeFragment].
 */
class DefaultRecentSyncedTabController(
    private val tabsUseCase: TabsUseCases,
    private val navController: NavController,
    private val accessPoint: TabsTrayAccessPoint,
    private val appStore: AppStore,
) : RecentSyncedTabController {
    override fun handleRecentSyncedTabClick(tab: RecentSyncedTab) {
        RecentSyncedTabs.recentSyncedTabOpened[tab.deviceType.name.lowercase()].add()
        tabsUseCase.selectOrAddTab(tab.url)
        navController.navigate(R.id.browserFragment)
    }

    override fun handleSyncedTabShowAllClicked() {
        RecentSyncedTabs.showAllSyncedTabsClicked.add()
        navController.navigate(
            HomeFragmentDirections.actionGlobalTabsTrayFragment(
                page = Page.SyncedTabs,
                accessPoint = accessPoint,
            ),
        )
    }

    override fun handleRecentSyncedTabRemoved(tab: RecentSyncedTab) {
        appStore.dispatch(AppAction.RemoveRecentSyncedTab(tab))
    }
}
