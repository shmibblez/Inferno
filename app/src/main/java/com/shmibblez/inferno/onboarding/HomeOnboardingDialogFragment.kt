/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package com.shmibblez.inferno.onboarding

import android.annotation.SuppressLint
import android.content.pm.ActivityInfo
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.DialogFragment
import androidx.navigation.fragment.findNavController
import mozilla.components.lib.state.ext.observeAsComposableState
import com.shmibblez.inferno.R
import com.shmibblez.inferno.components.accounts.FenixFxAEntryPoint
import com.shmibblez.inferno.components.components
import com.shmibblez.inferno.ext.nav
import com.shmibblez.inferno.ext.settings
import com.shmibblez.inferno.onboarding.view.UpgradeOnboarding
import com.shmibblez.inferno.theme.FirefoxTheme

/**
 * Dialog displaying a welcome and sync sign in onboarding.
 */
class HomeOnboardingDialogFragment : DialogFragment() {
    @SuppressLint("SourceLockedOrientationActivity")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NO_TITLE, R.style.HomeOnboardingDialogStyle)
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
    }

    override fun onDestroy() {
        super.onDestroy()
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View = ComposeView(requireContext()).apply {
        setContent {
            FirefoxTheme {
                val account =
                    components.backgroundServices.syncStore.observeAsComposableState { state -> state.account }

                UpgradeOnboarding(
                    isSyncSignIn = account.value != null,
                    onDismiss = ::onDismiss,
                    onSignInButtonClick = {
                        findNavController().nav(
                            R.id.homeOnboardingDialogFragment,
                            HomeOnboardingDialogFragmentDirections.actionGlobalTurnOnSync(
                                entrypoint = FenixFxAEntryPoint.HomeOnboardingDialog,
                            ),
                        )
                        onDismiss()
                    },
                )
            }
        }
    }

    private fun onDismiss() {
        context?.settings()?.showHomeOnboardingDialog = false
        dismiss()
    }
}
