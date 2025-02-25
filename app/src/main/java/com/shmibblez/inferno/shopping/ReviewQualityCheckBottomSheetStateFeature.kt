/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package com.shmibblez.inferno.shopping

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.scan
import mozilla.components.lib.state.helpers.AbstractBinding
import com.shmibblez.inferno.shopping.store.BottomSheetViewState
import com.shmibblez.inferno.shopping.store.ReviewQualityCheckState
import com.shmibblez.inferno.shopping.store.ReviewQualityCheckStore
import kotlinx.coroutines.ExperimentalCoroutinesApi

/**
 * View-bound feature that requests the bottom sheet state to be changed to expanded or collapsed when
 * the store state changes from [ReviewQualityCheckState.Initial] to [ReviewQualityCheckState.NotOptedIn].
 *
 * @param store The store to observe.
 * @param isScreenReaderEnabled Used to fully expand bottom sheet when a screen reader is on.
 * @param onRequestStateUpdate Callback to request the bottom sheet to be updated.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ReviewQualityCheckBottomSheetStateFeature(
    store: ReviewQualityCheckStore,
    private val isScreenReaderEnabled: Boolean,
    private val onRequestStateUpdate: (expanded: BottomSheetViewState) -> Unit,
) : AbstractBinding<ReviewQualityCheckState>(store) {
    @ExperimentalCoroutinesApi
    override suspend fun onState(flow: Flow<ReviewQualityCheckState>) {
        if (isScreenReaderEnabled) {
            onRequestStateUpdate(BottomSheetViewState.FULL_VIEW)
        } else {
            val initial = Pair<ReviewQualityCheckState?, ReviewQualityCheckState?>(null, null)
            flow.scan(initial) { acc, value ->
                Pair(acc.second, value)
            }.filter {
                it.first is ReviewQualityCheckState.Initial
            }.map {
                when (it.second) {
                    is ReviewQualityCheckState.NotOptedIn -> BottomSheetViewState.FULL_VIEW
                    else -> BottomSheetViewState.HALF_VIEW
                }
            }.collect {
                onRequestStateUpdate(it)
            }
        }
    }
}
