/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package com.shmibblez.inferno.lifecycle

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.LifecycleOwner
import mozilla.components.browser.state.action.AppLifecycleAction
import mozilla.components.browser.state.store.BrowserStore
import com.shmibblez.inferno.components.AppStore
import com.shmibblez.inferno.components.appstate.AppAction

/**
 * [LifecycleObserver] to dispatch app lifecycle actions to the [AppStore] and [BrowserStore].
 */
class StoreLifecycleObserver(
    private val appStore: AppStore,
    private val browserStore: BrowserStore,
) : DefaultLifecycleObserver {
    override fun onPause(owner: LifecycleOwner) {
        super.onPause(owner)
        appStore.dispatch(AppAction.AppLifecycleAction.PauseAction)
        browserStore.dispatch(AppLifecycleAction.PauseAction)
    }

    override fun onResume(owner: LifecycleOwner) {
        super.onResume(owner)
        appStore.dispatch(AppAction.AppLifecycleAction.ResumeAction)
        browserStore.dispatch(AppLifecycleAction.ResumeAction)
    }
}
