/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package com.shmibblez.inferno.share

import com.shmibblez.inferno.components.appstate.AppAction.ShareAction
import com.shmibblez.inferno.components.appstate.AppState
import com.shmibblez.inferno.components.appstate.snackbar.SnackbarState

/**
 * [AppStore] reducer of [ShareAction]s.
 */
internal object ShareActionReducer {
    fun reduce(state: AppState, action: ShareAction): AppState = when (action) {
        is ShareAction.ShareToAppFailed -> state.copy(
            snackbarState = SnackbarState.ShareToAppFailed,
        )

        is ShareAction.SharedTabsSuccessfully -> state.copy(
            snackbarState = SnackbarState.SharedTabsSuccessfully(
                destination = action.destination,
                tabs = action.tabs,
            ),
        )

        is ShareAction.ShareTabsFailed -> state.copy(
            snackbarState = SnackbarState.ShareTabsFailed(
                destination = action.destination,
                tabs = action.tabs,
            ),
        )

        is ShareAction.CopyLinkToClipboard -> state.copy(
            snackbarState = SnackbarState.CopyLinkToClipboard,
        )
    }
}