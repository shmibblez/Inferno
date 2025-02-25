/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package com.shmibblez.inferno.exceptions.trackingprotection

import android.text.method.LinkMovementMethod
import android.view.ViewGroup
import mozilla.components.concept.engine.content.blocking.TrackingProtectionException
import com.shmibblez.inferno.exceptions.ExceptionsView
import com.shmibblez.inferno.ext.addUnderline

class TrackingProtectionExceptionsView(
    container: ViewGroup,
    interactor: TrackingProtectionExceptionsInteractor,
) : ExceptionsView<TrackingProtectionException>(container, interactor) {

    override val exceptionsAdapter = TrackingProtectionExceptionsAdapter(interactor)

    init {
        binding.exceptionsList.apply {
            adapter = exceptionsAdapter
        }

        with(binding.exceptionsLearnMore) {
            addUnderline()

            movementMethod = LinkMovementMethod.getInstance()
            setOnClickListener { interactor.onLearnMore() }
        }
    }
}
