/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package com.shmibblez.inferno.components.menu.compose.header

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.shmibblez.inferno.mozillaAndroidComponents.compose.base.annotation.LightDarkPreview
import com.shmibblez.inferno.R
import com.shmibblez.inferno.theme.FirefoxTheme
import com.shmibblez.inferno.theme.Theme

@Composable
internal fun SubmenuHeader(
    header: String,
    backButtonContentDescription: String? = null,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .padding(start = 4.dp, end = 16.dp)
            .defaultMinSize(minHeight = 56.dp)
            .verticalScroll(rememberScrollState()),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(
            onClick = { onClick() },
            modifier = Modifier.semantics {
                backButtonContentDescription?.also { this.contentDescription = it }
            },
        ) {
            Icon(
                painter = painterResource(id = R.drawable.mozac_ic_back_24),
                contentDescription = null,
                tint = FirefoxTheme.colors.iconSecondary,
            )
        }

        Spacer(modifier = Modifier.width(4.dp))

        Text(
            text = header,
            modifier = Modifier
                .weight(1f)
                .semantics { heading() },
            color = FirefoxTheme.colors.textSecondary,
            style = FirefoxTheme.typography.headline7,
        )
    }
}

@LightDarkPreview
@Composable
private fun SubmenuHeaderPreview() {
    FirefoxTheme {
        Column(
            modifier = Modifier
                .background(color = FirefoxTheme.colors.layer3),
        ) {
            SubmenuHeader(
                header = "sub-menu header",
                onClick = {},
            )
        }
    }
}

@Preview
@Composable
private fun SubmenuMenuHeaderPrivatePreview() {
    FirefoxTheme(theme = Theme.Private) {
        Column(
            modifier = Modifier
                .background(color = FirefoxTheme.colors.layer3),
        ) {
            SubmenuHeader(
                header = "sub-menu header",
                onClick = {},
            )
        }
    }
}
