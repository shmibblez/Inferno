/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package com.shmibblez.inferno.addons

import android.view.View
import androidx.compose.material3.SnackbarDefaults
import androidx.compose.material3.SnackbarDuration
import mozilla.components.feature.addons.Addon
import mozilla.components.feature.addons.ui.AddonsManagerAdapterDelegate
import com.shmibblez.inferno.BrowserDirection
import com.shmibblez.inferno.BuildConfig
import com.shmibblez.inferno.HomeActivity
import com.shmibblez.inferno.compose.snackbar.Snackbar
import com.shmibblez.inferno.compose.snackbar.SnackbarState
import com.shmibblez.inferno.compose.snackbar.toMillis
import com.shmibblez.inferno.settings.SupportUtils

/**
 * Shows the Fenix Snackbar in the given view along with the provided text.
 *
 * @param view A [View] used to determine a parent for the [Snackbar].
 * @param text The text to display in the [Snackbar].
 * @param duration The duration to show the [Snackbar] for.
 */
internal fun showSnackBar(view: View, text: String, duration: SnackbarState.Duration = SnackbarState.Duration.Preset.Short) {
    Snackbar.make(
        snackBarParentView = view,
        snackbarState = SnackbarState(
            message = text,
            duration = duration,
        ),
    ).show()
}

internal fun openLearnMoreLink(
    activity: HomeActivity,
    link: AddonsManagerAdapterDelegate.LearnMoreLinks,
    addon: Addon,
    from: BrowserDirection,
) {
    val url = when (link) {
        AddonsManagerAdapterDelegate.LearnMoreLinks.BLOCKLISTED_ADDON ->
            "${BuildConfig.AMO_BASE_URL}/android/blocked-addon/${addon.id}/${addon.version}/"
        AddonsManagerAdapterDelegate.LearnMoreLinks.ADDON_NOT_CORRECTLY_SIGNED ->
            SupportUtils.getSumoURLForTopic(activity.baseContext, SupportUtils.SumoTopic.UNSIGNED_ADDONS)
    }
    openLinkInNewTab(activity, url, from)
}

internal fun openLinkInNewTab(activity: HomeActivity, url: String, from: BrowserDirection) {
    activity.openToBrowserAndLoad(searchTermOrURL = url, newTab = true, from = from)
}
