/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package com.shmibblez.inferno.settings.trustpanel.middleware

import mozilla.components.lib.state.Middleware
import mozilla.components.lib.state.MiddlewareContext
//import mozilla.telemetry.glean.private.NoExtras
//import com.shmibblez.inferno.GleanMetrics.TrackingProtection
import com.shmibblez.inferno.settings.trustpanel.store.TrustPanelAction
import com.shmibblez.inferno.settings.trustpanel.store.TrustPanelState
import com.shmibblez.inferno.settings.trustpanel.store.TrustPanelStore

/**
 * A [Middleware] for recording telemetry based on [TrustPanelAction]s that are dispatched to the
 * [TrustPanelStore].
 */
class TrustPanelTelemetryMiddleware : Middleware<TrustPanelState, TrustPanelAction> {

    override fun invoke(
        context: MiddlewareContext<TrustPanelState, TrustPanelAction>,
        next: (TrustPanelAction) -> Unit,
        action: TrustPanelAction,
    ) {
        val currentState = context.state

        next(action)

        when (action) {
            TrustPanelAction.ToggleTrackingProtection -> if (currentState.isTrackingProtectionEnabled) {
//                TrackingProtection.exceptionAdded.record(NoExtras())
            }

            TrustPanelAction.Navigate.Back,
            TrustPanelAction.Navigate.TrackersPanel,
            -> Unit
        }
    }
}
