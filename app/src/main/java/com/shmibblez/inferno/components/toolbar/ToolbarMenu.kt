/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package com.shmibblez.inferno.components.toolbar

import mozilla.components.browser.menu.BrowserMenuBuilder
import mozilla.components.browser.menu.item.BrowserMenuItemToolbar
import com.shmibblez.inferno.components.accounts.AccountState

interface ToolbarMenu {
    sealed class Item {
        object Settings : Item()
        data class RequestDesktop(val isChecked: Boolean) : Item()

        /**
         * Opens the current private tabs in a regular tab.
         */
        object OpenInRegularTab : Item()
        object FindInPage : Item()

        /**
         * Opens the translations flow.
         */
        object Translate : Item()
        object Share : Item()
        data class Back(val viewHistory: Boolean) : Item()
        data class Forward(val viewHistory: Boolean) : Item()
        data class Reload(val bypassCache: Boolean) : Item()
        object Stop : Item()
        object OpenInFenix : Item()
        object SaveToCollection : Item()

        /**
         * Prints the currently displayed page content.
         */
        object PrintContent : Item()
        object AddToTopSites : Item()
        object RemoveFromTopSites : Item()
        object InstallPwaToHomeScreen : Item()
        object AddToHomeScreen : Item()
        data class SyncAccount(val accountState: AccountState) : Item()
        object AddonsManager : Item()
        object Quit : Item()
        object OpenInApp : Item()
        object SetDefaultBrowser : Item()
        object Bookmark : Item()
        object CustomizeReaderView : Item()
        object Bookmarks : Item()
        object History : Item()

        /**
         * The Passwords menu item
         */
        object Passwords : Item()
        object Downloads : Item()
        object NewTab : Item()
    }

    val menuBuilder: BrowserMenuBuilder
    val menuToolbar: BrowserMenuItemToolbar
}
