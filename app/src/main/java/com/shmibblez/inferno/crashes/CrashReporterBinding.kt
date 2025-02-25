/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package com.shmibblez.inferno.crashes

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChangedBy
import mozilla.components.lib.crash.store.CrashState
import mozilla.components.lib.state.helpers.AbstractBinding
import com.shmibblez.inferno.components.AppStore
import com.shmibblez.inferno.components.appstate.AppState
import kotlinx.coroutines.ExperimentalCoroutinesApi

/**
 * A binding for observing the [CrashState] in the [AppStore] and displaying the crash reporter.
 *
 * @param store The [AppStore] used to observe the [CrashState].
 * @param onReporting a callback that is called when [CrashState] is [CrashState.Reporting].
 */
@OptIn(ExperimentalCoroutinesApi::class)
class CrashReporterBinding(
    store: AppStore,
    private val onReporting: () -> Unit,
) : AbstractBinding<AppState>(store) {
    override suspend fun onState(flow: Flow<AppState>) {
        flow.distinctUntilChangedBy { state -> state.crashState }
            .collect { state ->
                if (state.crashState == CrashState.Reporting) {
                    onReporting()
                }
            }
    }
}
