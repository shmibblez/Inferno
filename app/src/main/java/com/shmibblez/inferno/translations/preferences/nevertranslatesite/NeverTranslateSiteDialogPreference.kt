/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package com.shmibblez.inferno.translations.preferences.nevertranslatesite

import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.shmibblez.inferno.mozillaAndroidComponents.compose.base.annotation.LightDarkPreview
import com.shmibblez.inferno.mozillaAndroidComponents.compose.base.button.TextButton
import com.shmibblez.inferno.R
import com.shmibblez.inferno.theme.FirefoxTheme

/**
 * Never Translate Dialog
 *
 * @param websiteUrl Title of the dialog that should be display.
 * @param onConfirmDelete Invoked when the user clicks on the "Delete" dialog button.
 * @param onCancel Invoked when the user clicks on the "Cancel" dialog button.
 */
@Composable
fun NeverTranslateSiteDialogPreference(
    websiteUrl: String,
    onConfirmDelete: () -> Unit,
    onCancel: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = {},
        modifier = Modifier.background(
            color = FirefoxTheme.colors.layer2,
            shape = RoundedCornerShape(8.dp),
        ),
        title = {
            Text(
                text = stringResource(R.string.never_translate_site_dialog_title_preference, websiteUrl),
                color = FirefoxTheme.colors.textPrimary,
                style = FirefoxTheme.typography.headline7,
            )
        },
        confirmButton = {
            TextButton(
                text = stringResource(id = R.string.never_translate_site_dialog_confirm_delete_preference),
                upperCaseText = false,
                onClick = { onConfirmDelete() },
            )
        },
        dismissButton = {
            TextButton(
                text = stringResource(id = R.string.never_translate_site_dialog_cancel_preference),
                upperCaseText = false,
                onClick = { onCancel() },
            )
        },
        containerColor = FirefoxTheme.colors.layer2,
    )
}

@Composable
@LightDarkPreview
private fun NeverTranslateSiteDialogPreferencePreview() {
    FirefoxTheme {
        NeverTranslateSiteDialogPreference(
            websiteUrl = "wikipedia.com",
            onConfirmDelete = {},
            onCancel = {},
        )
    }
}
