/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package com.shmibblez.inferno.messaging.state

import com.shmibblez.inferno.components.appstate.AppAction
import com.shmibblez.inferno.components.appstate.AppAction.MessagingAction.ConsumeMessageToShow
import com.shmibblez.inferno.components.appstate.AppAction.MessagingAction.UpdateMessageToShow
import com.shmibblez.inferno.components.appstate.AppAction.MessagingAction.UpdateMessages
import com.shmibblez.inferno.components.appstate.AppState
import com.shmibblez.inferno.messaging.MessagingState

/**
 * Reducer for [MessagingState].
 */
internal object MessagingReducer {
    fun reduce(state: AppState, action: AppAction): AppState = when (action) {
        is UpdateMessageToShow -> {
            val messageToShow = state.messaging.messageToShow.toMutableMap()
            messageToShow[action.message.surface] = action.message
            state.copy(
                messaging = state.messaging.copy(
                    messageToShow = messageToShow,
                ),
            )
        }
        is UpdateMessages -> {
            state.copy(
                messaging = state.messaging.copy(
                    messages = action.messages,
                ),
            )
        }
        is ConsumeMessageToShow -> {
            val messageToShow = state.messaging.messageToShow.toMutableMap()
            messageToShow.remove(action.surface)
            state.copy(
                messaging = state.messaging.copy(
                    messageToShow = messageToShow,
                ),
            )
        }
        else -> state
    }
}
