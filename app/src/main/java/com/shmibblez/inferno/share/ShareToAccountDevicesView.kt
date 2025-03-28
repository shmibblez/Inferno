/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package com.shmibblez.inferno.share

import android.view.LayoutInflater
import android.view.ViewGroup
import mozilla.components.concept.sync.Device
import com.shmibblez.inferno.databinding.ShareToAccountDevicesBinding
import com.shmibblez.inferno.share.listadapters.AccountDevicesShareAdapter
import com.shmibblez.inferno.share.listadapters.SyncShareOption

/**
 * Callbacks for possible user interactions on the [ShareToAccountDevicesView]
 */
interface ShareToAccountDevicesInteractor {
    fun onSignIn()
    fun onReauth()
    fun onAddNewDevice()
    fun onShareToDevice(device: Device)
    fun onShareToAllDevices(devices: List<Device>)
}

class ShareToAccountDevicesView(
    containerView: ViewGroup,
    interactor: ShareToAccountDevicesInteractor,
) {

    private val adapter = AccountDevicesShareAdapter(interactor)

    init {
        val binding = ShareToAccountDevicesBinding.inflate(
            LayoutInflater.from(containerView.context),
            containerView,
            true,
        )

        binding.devicesList.adapter = adapter
    }

    fun setShareTargets(targets: List<SyncShareOption>) {
        adapter.submitList(targets)
    }
}
