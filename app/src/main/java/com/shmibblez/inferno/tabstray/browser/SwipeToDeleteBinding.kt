/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package com.shmibblez.inferno.tabstray.browser

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import mozilla.components.lib.state.helpers.AbstractBinding
import com.shmibblez.inferno.tabstray.TabsTrayState
import com.shmibblez.inferno.tabstray.TabsTrayStore

/**
 * Notifies whether a tab is accessible for using the swipe-to-delete gesture.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class SwipeToDeleteBinding(
    store: TabsTrayStore,
) : AbstractBinding<TabsTrayState>(store) {
    var isSwipeable = false
        private set

    override suspend fun onState(flow: Flow<TabsTrayState>) {
        flow.map { it.mode }
            .distinctUntilChanged()
            .collect { mode ->
                isSwipeable = mode == TabsTrayState.Mode.Normal
            }
    }
}
