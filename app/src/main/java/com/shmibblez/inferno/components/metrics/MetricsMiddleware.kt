// FIXME: NOT USED
///* This Source Code Form is subject to the terms of the Mozilla Public
// * License, v. 2.0. If a copy of the MPL was not distributed with this
// * file, You can obtain one at http://mozilla.org/MPL/2.0/. */
//
//package com.shmibblez.inferno.components.metrics
//
//import mozilla.components.lib.state.Middleware
//import mozilla.components.lib.state.MiddlewareContext
//import com.shmibblez.inferno.components.appstate.AppAction
//import com.shmibblez.inferno.components.appstate.AppState
//
///**
// * A middleware that will map incoming actions to relevant events for [metrics].
// */
//class MetricsMiddleware(
//    private val metrics: MetricController,
//) : Middleware<AppState, AppAction> {
//    override fun invoke(
//        context: MiddlewareContext<AppState, AppAction>,
//        next: (AppAction) -> Unit,
//        action: AppAction,
//    ) {
//        handleAction(action)
//        next(action)
//    }
//
//    private fun handleAction(action: AppAction) = when (action) {
//        is AppAction.AppLifecycleAction.ResumeAction -> {
//            metrics.track(Event.GrowthData.SetAsDefault)
//            metrics.track(Event.GrowthData.FirstAppOpenForDay)
//            metrics.track(Event.GrowthData.FirstWeekSeriesActivity)
//            metrics.track(Event.GrowthData.UsageThreshold)
//            metrics.track(Event.GrowthData.UserActivated(fromSearch = false))
//        }
//        else -> Unit
//    }
//}