/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package com.shmibblez.inferno.tabstray

import mozilla.components.browser.storage.sync.Tab

/**
 * Controller for handling any actions on synced tabs in the tabs tray.
 */
interface SyncedTabsController {
    /**
     * Handles a synced tab item click.
     *
     * @param tab The synced [Tab] that was clicked.
     */
    fun handleSyncedTabClicked(tab: Tab)

    /**
     * Handles a click on the "close" button for a synced tab.
     */
    fun handleSyncedTabClosed(deviceId: String, tab: Tab)
}
