/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package com.shmibblez.inferno.share

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import mozilla.components.ui.widgets.withCenterAlignedButtons
import com.shmibblez.inferno.BrowserDirection
import com.shmibblez.inferno.HomeActivity
import com.shmibblez.inferno.R
import com.shmibblez.inferno.databinding.FragmentAddNewDeviceBinding
import com.shmibblez.inferno.ext.showToolbar
import com.shmibblez.inferno.settings.SupportUtils

/**
 * Fragment to add a new device. Tabs can be shared to devices after they are added.
 */
class AddNewDeviceFragment : Fragment(R.layout.fragment_add_new_device) {

    override fun onResume() {
        super.onResume()
        showToolbar(getString(R.string.sync_add_new_device_title))
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val binding = FragmentAddNewDeviceBinding.bind(view)
        binding.learnButton.setOnClickListener {
            (activity as HomeActivity).openToBrowserAndLoad(
                searchTermOrURL = SupportUtils.getSumoURLForTopic(
                    requireContext(),
                    SupportUtils.SumoTopic.SEND_TABS,
                ),
                newTab = true,
                from = BrowserDirection.FromAddNewDeviceFragment,
            )
        }

        binding.connectButton.setOnClickListener {
            AlertDialog.Builder(requireContext()).apply {
                setMessage(R.string.sync_connect_device_dialog)
                setPositiveButton(R.string.sync_confirmation_button) { dialog, _ -> dialog.cancel() }
                create().withCenterAlignedButtons()
            }.show()
        }
    }
}
