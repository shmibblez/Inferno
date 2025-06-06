/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package com.shmibblez.inferno.tabstray

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChangedBy
import mozilla.components.lib.state.helpers.AbstractBinding
import com.shmibblez.inferno.components.AppStore
import com.shmibblez.inferno.components.appstate.AppState
import kotlinx.coroutines.ExperimentalCoroutinesApi

/**
 * Binding to update the [TabsTrayStore] by listening to changes to [AppState].
 *
 * @param appStore [AppStore] used to listen for changes to [AppState].
 * @param tabsTrayStore [TabsTrayStore] used to listen for changes to [TabsTrayState].
 */
@OptIn(ExperimentalCoroutinesApi::class)
class InactiveTabsBinding(
    appStore: AppStore,
    private val tabsTrayStore: TabsTrayStore,
) : AbstractBinding<AppState>(appStore) {
    override suspend fun onState(flow: Flow<AppState>) {
        flow.distinctUntilChangedBy { it.inactiveTabsExpanded }
            .collectLatest {
                tabsTrayStore.dispatch(TabsTrayAction.UpdateInactiveExpanded(expanded = it.inactiveTabsExpanded))
            }
    }
}
