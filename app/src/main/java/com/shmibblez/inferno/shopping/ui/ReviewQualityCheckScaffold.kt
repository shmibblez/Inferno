/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package com.shmibblez.inferno.shopping.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.traversalIndex
import androidx.compose.ui.unit.dp
import com.shmibblez.inferno.mozillaAndroidComponents.compose.base.annotation.LightDarkPreview
import com.shmibblez.inferno.R
import com.shmibblez.inferno.compose.BetaLabel
import com.shmibblez.inferno.compose.BottomSheetHandle
import com.shmibblez.inferno.shopping.ui.ext.headingResource
import com.shmibblez.inferno.theme.FirefoxTheme

private val bottomSheetShape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
private const val BOTTOM_SHEET_HANDLE_WIDTH_PERCENT = 0.1f

/**
 * A scaffold for review quality check UI that implements the basic layout structure with
 * [BottomSheetHandle], [Header] and [content].
 *
 * @param onRequestDismiss Invoked when a user action requests dismissal of the bottom sheet.
 * @param modifier The modifier to be applied to the Composable.
 * @param content The content of the bottom sheet.
 */
@Composable
fun ReviewQualityCheckScaffold(
    onRequestDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Surface(
        color = FirefoxTheme.colors.layer1,
        shape = bottomSheetShape,
    ) {
        Column(
            modifier = modifier
                .background(
                    color = FirefoxTheme.colors.layer1,
                    shape = bottomSheetShape,
                )
                .verticalScroll(rememberScrollState())
                .padding(
                    vertical = 8.dp,
                    horizontal = 16.dp,
                ),
        ) {
            BottomSheetHandle(
                onRequestDismiss = onRequestDismiss,
                contentDescription = stringResource(R.string.review_quality_check_close_handle_content_description),
                modifier = Modifier
                    .fillMaxWidth(BOTTOM_SHEET_HANDLE_WIDTH_PERCENT)
                    .align(Alignment.CenterHorizontally)
                    .semantics { traversalIndex = -1f },
            )

            Spacer(modifier = Modifier.height(16.dp))

            Header()

            Spacer(modifier = Modifier.height(16.dp))

            content()

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun Header() {
    val reviewCheckerFeatureName = stringResource(R.string.review_quality_check_feature_name_2)
    val betaText = stringResource(R.string.beta_feature)
    val titleContentDescription = headingResource("$reviewCheckerFeatureName, $betaText")

    Row(
        modifier = Modifier.semantics(mergeDescendants = true) {
            heading()
            contentDescription = titleContentDescription
        },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = reviewCheckerFeatureName,
            color = FirefoxTheme.colors.textPrimary,
            style = FirefoxTheme.typography.headline6,
            modifier = Modifier.clearAndSetSemantics {},
        )

        Spacer(modifier = Modifier.width(8.dp))

        BetaLabel(modifier = Modifier.clearAndSetSemantics {})
    }
}

@LightDarkPreview
@Composable
private fun HeaderPreview() {
    FirefoxTheme {
        Box(
            modifier = Modifier
                .background(color = FirefoxTheme.colors.layer1)
                .padding(16.dp),
        ) {
            Header()
        }
    }
}
