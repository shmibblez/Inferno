/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package com.shmibblez.inferno.debugsettings.store

import mozilla.components.lib.state.Action
//import com.shmibblez.inferno.debugsettings.gleandebugtools.ui.GleanDebugToolsScreen
import com.shmibblez.inferno.debugsettings.ui.DebugDrawerHome
import com.shmibblez.inferno.debugsettings.addresses.AddressesTools as AddressesScreen
import com.shmibblez.inferno.debugsettings.cfrs.CfrTools as CfrToolsScreen
import com.shmibblez.inferno.debugsettings.logins.LoginsTools as LoginsScreen
import com.shmibblez.inferno.debugsettings.tabs.TabTools as TabToolsScreen

/**
 * [Action] implementation related to [DebugDrawerStore].
 */
sealed class DebugDrawerAction : Action {

    /**
     * [DebugDrawerAction] fired when the user opens the drawer.
     */
    data object DrawerOpened : DebugDrawerAction()

    /**
     * [DebugDrawerAction] fired when the user closes the drawer.
     */
    data object DrawerClosed : DebugDrawerAction()

    /**
     * [DebugDrawerAction] fired when a navigation event occurs for a specific destination.
     */
    sealed class NavigateTo : DebugDrawerAction() {

        /**
         * [NavigateTo] action fired when the debug drawer needs to navigate to [DebugDrawerHome].
         */
        data object Home : NavigateTo()

        /**
         * [NavigateTo] action fired when the debug drawer needs to navigate to [TabToolsScreen].
         */
        data object TabTools : NavigateTo()

        /**
         * [NavigateTo] action fired when the debug drawer needs to navigate to [LoginsScreen].
         */
        data object Logins : NavigateTo()

        /**
         * [NavigateTo] action fired when the debug drawer needs to navigate to [AddressesScreen].
         */
        data object Addresses : NavigateTo()

        /**
         * [NavigateTo] action fired when the debug drawer needs to navigate to [CfrToolsScreen].
         */
        data object CfrTools : NavigateTo()

//        /**
//         * [NavigateTo] action fired when the debug drawer needs to navigate to [GleanDebugToolsScreen].
//         */
//        object GleanDebugTools : NavigateTo()
    }

    /**
     * [DebugDrawerAction] fired when a back navigation event occurs.
     */
    data object OnBackPressed : DebugDrawerAction()
}
