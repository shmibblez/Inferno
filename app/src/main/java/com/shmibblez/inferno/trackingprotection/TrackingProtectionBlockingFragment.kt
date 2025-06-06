/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package com.shmibblez.inferno.trackingprotection

import android.os.Bundle
import android.view.View
import androidx.annotation.VisibleForTesting
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.navArgs
import com.shmibblez.inferno.R
import com.shmibblez.inferno.databinding.FragmentTrackingProtectionBlockingBinding
import com.shmibblez.inferno.ext.settings
import com.shmibblez.inferno.ext.showToolbar

class TrackingProtectionBlockingFragment :
    Fragment(R.layout.fragment_tracking_protection_blocking) {

    private val args: TrackingProtectionBlockingFragmentArgs by navArgs()
    private val settings by lazy { requireContext().settings() }

    @VisibleForTesting
    internal lateinit var binding: FragmentTrackingProtectionBlockingBinding

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding = FragmentTrackingProtectionBlockingBinding.bind(view)

        // Text for the updated "Total cookie protection" option should be updated as part of a staged rollout
        if (requireContext().settings().enabledTotalCookieProtection) {
            binding.categoryCookies.apply {
                trackingProtectionCategoryTitle.text = getText(R.string.etp_cookies_title_2)
                trackingProtectionCategoryItemDescription.text = getText(R.string.etp_cookies_description_2)
            }
        }

        when (args.protectionMode) {
            TrackingProtectionMode.STANDARD -> {
                binding.categoryTrackingContent.isVisible = false
            }

            TrackingProtectionMode.STRICT -> {}

            TrackingProtectionMode.CUSTOM -> {
                binding.categoryFingerprinters.isVisible =
                    settings.blockFingerprintersInCustomTrackingProtection
                binding.categoryCryptominers.isVisible =
                    settings.blockCryptominersInCustomTrackingProtection
                binding.categoryCookies.isVisible =
                    settings.blockCookiesInCustomTrackingProtection
                binding.categoryTrackingContent.isVisible =
                    settings.blockTrackingContentInCustomTrackingProtection
                binding.categoryRedirectTrackers.isVisible =
                    settings.blockRedirectTrackersInCustomTrackingProtection
                binding.categorySuspectedFingerprinters.isVisible =
                    settings.blockSuspectedFingerprintersInCustomTrackingProtectionInNormalTabs
            }
        }
    }

    override fun onResume() {
        super.onResume()
        showToolbar(getString(args.protectionMode.titleRes))
    }
}
