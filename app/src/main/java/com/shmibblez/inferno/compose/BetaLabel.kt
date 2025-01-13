/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package com.shmibblez.inferno.compose

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
//import com.shmibblez.inferno.mozillaAndroidComponents.base.compose.annotation.LightDarkPreview
import mozilla.components.ui.colors.PhotonColors
import com.shmibblez.inferno.R
import com.shmibblez.inferno.theme.FirefoxTheme

/**
 * Beta label.
 *
 * @param modifier The modifier to apply to this layout.
 */
@Composable
fun BetaLabel(modifier: Modifier = Modifier) {
    val borderColor: Color
    val textColor: Color

    if (isSystemInDarkTheme()) {
        borderColor = PhotonColors.LightGrey10
        textColor = FirefoxTheme.colors.textActionPrimary
    } else {
        borderColor = FirefoxTheme.colors.actionTertiary
        textColor = FirefoxTheme.colors.textSecondary
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(width = 2.dp, color = borderColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Text(
            text = stringResource(R.string.beta_feature),
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
            color = textColor,
            style = FirefoxTheme.typography.body2,
        )
    }
}

//@LightDarkPreview
@Composable
private fun HeaderPreview() {
    FirefoxTheme {
        Box(
            modifier = Modifier
                .background(color = FirefoxTheme.colors.layer2)
                .padding(16.dp),
        ) {
            BetaLabel()
        }
    }
}
