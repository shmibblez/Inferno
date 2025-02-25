/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package com.shmibblez.inferno.shortcut

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
//import com.shmibblez.inferno.GleanMetrics.ProgressiveWebApp
import com.shmibblez.inferno.R
import com.shmibblez.inferno.databinding.FragmentPwaOnboardingBinding
import com.shmibblez.inferno.ext.requireComponents

/**
 * Dialog displayed the third time the user navigates to an installable web app.
 */
class PwaOnboardingDialogFragment : DialogFragment() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NO_TITLE, R.style.CreateShortcutDialogStyle)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? = inflater.inflate(R.layout.fragment_pwa_onboarding, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val components = requireComponents
        val binding = FragmentPwaOnboardingBinding.bind(view)

        binding.cancelButton.setOnClickListener {
//            ProgressiveWebApp.onboardingCancel.record()
            dismiss()
        }
        binding.addButton.setOnClickListener {
            viewLifecycleOwner.lifecycleScope.launch {
                components.useCases.webAppUseCases.addToHomescreen()
            }.invokeOnCompletion {
                dismiss()
            }
        }
    }
}
