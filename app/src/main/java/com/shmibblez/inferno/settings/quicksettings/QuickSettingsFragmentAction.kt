/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package com.shmibblez.inferno.settings.quicksettings

import mozilla.components.lib.state.Action
import com.shmibblez.inferno.settings.PhoneFeature
import com.shmibblez.inferno.trackingprotection.ProtectionsState

/**
 * Parent [Action] for all the [QuickSettingsFragmentState] changes.
 */
sealed class QuickSettingsFragmentAction : Action

/**
 * All possible [WebsiteInfoState] changes as result of user / system interactions.
 */
sealed class WebsiteInfoAction : QuickSettingsFragmentAction()

/**
 * All possible [WebsitePermissionsState] changes as result of user / system interactions.
 */
sealed class WebsitePermissionAction(open val updatedFeature: PhoneFeature) : QuickSettingsFragmentAction() {
    /**
     * Change resulting from toggling a specific [WebsitePermission] for the current website.
     *
     * @property updatedFeature [PhoneFeature] backing a certain [WebsitePermission].
     * Allows to easily identify which permission changed
     * **Must be the name of one of the properties of [WebsitePermissionsState]**.
     * @property updatedStatus [String] the new [WebsitePermission#status] which will be shown to the user.
     * @property updatedEnabledStatus [Boolean] the new [WebsitePermission#enabled] which will be shown to the user.
     */
    class TogglePermission(
        override val updatedFeature: PhoneFeature,
        val updatedStatus: String,
        val updatedEnabledStatus: Boolean,
    ) : WebsitePermissionAction(updatedFeature)

    /**
     * Change resulting from changing a specific [WebsitePermission.Autoplay] for the current website.
     *
     * @property autoplayValue [AutoplayValue] backing a certain [WebsitePermission.Autoplay].
     * Allows to easily identify which permission changed
     */
    class ChangeAutoplay(
        val autoplayValue: AutoplayValue,
    ) : WebsitePermissionAction(PhoneFeature.AUTOPLAY)
}

/**
 * All possible [ProtectionsState] changes in the quick setting panel.
 */
sealed class TrackingProtectionAction : QuickSettingsFragmentAction() {
    /**
     * Toggles the enabled state of tracking protection.
     *
     * @property isTrackingProtectionEnabled Whether or not tracking protection is enabled.
     */
    data class ToggleTrackingProtectionEnabled(val isTrackingProtectionEnabled: Boolean) :
        TrackingProtectionAction()
}
