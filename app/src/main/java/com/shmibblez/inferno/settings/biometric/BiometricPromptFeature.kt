/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package com.shmibblez.inferno.settings.biometric

import android.content.Context
import android.os.Build.VERSION.SDK_INT
import android.os.Build.VERSION_CODES.M
import androidx.annotation.VisibleForTesting
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_WEAK
import androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import mozilla.components.support.base.feature.LifecycleAwareFeature
import mozilla.components.support.base.log.logger.Logger
import com.shmibblez.inferno.settings.biometric.ext.isEnrolled
import com.shmibblez.inferno.settings.biometric.ext.isHardwareAvailable

/**
 * A [LifecycleAwareFeature] for the Android Biometric API to prompt for user authentication.
 *
 * @param context Android context.
 * @param fragment The fragment on which this feature will live.
 * @param onAuthFailure A failure callback if authentication failed.
 * @param onAuthSuccess A success callback.
 */
class BiometricPromptFeature(
    private val context: Context,
    private val fragment: Fragment? = null,
    private val onAuthFailure: () -> Unit,
    private val onAuthSuccess: () -> Unit,
) : LifecycleAwareFeature {
    private val logger = Logger(javaClass.simpleName)

    @VisibleForTesting
    internal var biometricPrompt: BiometricPrompt? = null

    // todo: biometric, create state, use in component, when necessary show prompt
    //  or just skip prompt altogether, pass biometric state to other components like
    //  card prompt ( in InfernoWebPrompter), and state handles requesting and handling biometric
    override fun start() {
        val executor = ContextCompat.getMainExecutor(context)
//        biometricPrompt = BiometricPrompt(fragment, executor, PromptCallback())
    }

    override fun stop() {
        biometricPrompt = null
    }

    /**
     * Requests the user for biometric authentication.
     *
     * @param title Adds a title for the authentication prompt.
     */
    fun requestAuthentication(title: String) {
        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setAllowedAuthenticators(BIOMETRIC_WEAK or DEVICE_CREDENTIAL)
            .setTitle(title)
            .build()

        biometricPrompt?.authenticate(promptInfo)
    }

    internal inner class PromptCallback : BiometricPrompt.AuthenticationCallback() {
        override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
            logger.error("onAuthenticationError $errString")
            onAuthFailure.invoke()
        }

        override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
            logger.debug("onAuthenticationSucceeded")
            onAuthSuccess.invoke()
        }

        override fun onAuthenticationFailed() {
            logger.error("onAuthenticationFailed")
            onAuthFailure.invoke()
        }
    }

    companion object {

        /**
         * Checks if the appropriate SDK version and hardware capabilities are met to use the feature.
         */
        fun canUseFeature(context: Context): Boolean {
            return if (SDK_INT >= M) {
                val manager = BiometricManager.from(context)

                manager.isHardwareAvailable() && manager.isEnrolled()
            } else {
                false
            }
        }
    }
}
