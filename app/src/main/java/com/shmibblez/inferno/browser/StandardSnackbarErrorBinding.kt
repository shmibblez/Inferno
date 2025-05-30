/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package com.shmibblez.inferno.browser

import android.content.Context
import android.view.ViewGroup
import androidx.compose.material3.SnackbarDuration
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import mozilla.components.lib.state.helpers.AbstractBinding
import com.shmibblez.inferno.R
import com.shmibblez.inferno.components.AppStore
import com.shmibblez.inferno.components.appstate.AppAction
import com.shmibblez.inferno.components.appstate.AppState
import com.shmibblez.inferno.compose.core.Action
import com.shmibblez.inferno.compose.snackbar.Snackbar
import com.shmibblez.inferno.compose.snackbar.SnackbarState
import kotlinx.coroutines.ExperimentalCoroutinesApi

/**
 * A binding that shows standard snackbar errors.
 *
 * @param context [Context] used for system interactions and accessing resources.
 * @param snackbarParent [ViewGroup] in which to find a suitable parent for displaying the snackbar.
 * @param appStore The [AppStore] containing information about when to show a snackbar styled for errors.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class StandardSnackbarErrorBinding(
    private val context: Context,
    private val snackbarParent: ViewGroup,
    private val appStore: AppStore,
) : AbstractBinding<AppState>(appStore) {

    override suspend fun onState(flow: Flow<AppState>) {
        flow.map { state -> state.standardSnackbarError }
            .distinctUntilChanged()
            .collect {
                it?.let { standardSnackbarError ->
                    snackbarParent.let { view ->
                        val snackbar = Snackbar.make(
                            snackBarParentView = view,
                            snackbarState = SnackbarState(
                                message = standardSnackbarError.message,
                                duration = SnackbarState.Duration.Preset.Indefinite,
                                type = SnackbarState.Type.Warning,
                                action = Action(
                                    label = context.getString(R.string.standard_snackbar_error_dismiss),
                                    onClick = {
                                        appStore.dispatch(
                                            AppAction.UpdateStandardSnackbarErrorAction(
                                                standardSnackbarError = null,
                                            ),
                                        )
                                    },
                                ),
                            ),
                        )
                        snackbar.show()
                    }
                }
            }
    }
}

/**
 * Standard Snackbar Error data class.
 *
 * @property message that will appear on the snackbar.
 */
data class StandardSnackbarError(
    val message: String,
)
