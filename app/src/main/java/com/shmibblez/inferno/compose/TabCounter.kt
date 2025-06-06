/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package com.shmibblez.inferno.compose

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.shmibblez.inferno.mozillaAndroidComponents.compose.base.annotation.LightDarkPreview
import com.shmibblez.inferno.R
import com.shmibblez.inferno.compose.base.InfernoIcon
import com.shmibblez.inferno.compose.ext.toLocaleString
import com.shmibblez.inferno.tabstray.TabsTrayTestTag
import com.shmibblez.inferno.theme.FirefoxTheme

private const val MAX_VISIBLE_TABS = 99
private const val SO_MANY_TABS_OPEN = "∞"
private val NORMAL_TABS_BOTTOM_PADDING = 0.dp
private const val ONE_DIGIT_SIZE_RATIO = 0.5f
private const val TWO_DIGITS_SIZE_RATIO = 0.4f
private const val MIN_SINGLE_DIGIT = 0
private const val MAX_SINGLE_DIGIT = 9
private const val TWO_DIGIT_THRESHOLD = 10
private const val TAB_TEXT_BOTTOM_PADDING_RATIO = 6

/**
 * UI for displaying the number of opened tabs.
*
* This composable uses LocalContentColor, provided by CompositionLocalProvider,
* to set the color of its icons and text.
*
* @param tabCount the number to be displayed inside the counter.
* @param showPrivacyBadge if true, show the privacy badge.
* @param textColor the color of the text inside of tab counter.
* @param iconColor the border color of the tab counter.
*/

@Composable
fun TabCounter(
    tabCount: Int,
    showPrivacyBadge: Boolean = false,
    textColor: Color = FirefoxTheme.colors.textPrimary,
    iconColor: Color = FirefoxTheme.colors.iconPrimary,
) {
    val formattedTabCount = tabCount.toLocaleString()
    val normalTabCountText: String
    val tabCountTextRatio: Float
    val needsBottomPaddingForInfiniteTabs: Boolean

    when (tabCount) {
        in MIN_SINGLE_DIGIT..MAX_SINGLE_DIGIT -> {
            normalTabCountText = formattedTabCount
            tabCountTextRatio = ONE_DIGIT_SIZE_RATIO
            needsBottomPaddingForInfiniteTabs = false
        }

        in TWO_DIGIT_THRESHOLD..MAX_VISIBLE_TABS -> {
            normalTabCountText = formattedTabCount
            tabCountTextRatio = TWO_DIGITS_SIZE_RATIO
            needsBottomPaddingForInfiniteTabs = false
        }

        else -> {
            normalTabCountText = SO_MANY_TABS_OPEN
            tabCountTextRatio = ONE_DIGIT_SIZE_RATIO
            needsBottomPaddingForInfiniteTabs = true
        }
    }

    val normalTabsContentDescription = stringResource(
        R.string.mozac_tab_counter_open_tab_tray,
        formattedTabCount,
    )

    val counterBoxWidthDp =
        dimensionResource(id = mozilla.components.ui.tabcounter.R.dimen.mozac_tab_counter_box_width_height)
    val counterBoxWidthPx = LocalDensity.current.run { counterBoxWidthDp.roundToPx() }
    val counterTabsTextSize = (tabCountTextRatio * counterBoxWidthPx).toInt()

    val normalTabsTextModifier = if (needsBottomPaddingForInfiniteTabs) {
        val bottomPadding = with(LocalDensity.current) { counterTabsTextSize.toDp() / TAB_TEXT_BOTTOM_PADDING_RATIO }
        Modifier.padding(bottom = bottomPadding)
    } else {
        Modifier.padding(bottom = NORMAL_TABS_BOTTOM_PADDING)
    }

    Box(
        modifier = Modifier
            .semantics(mergeDescendants = true) {
                testTag = TabsTrayTestTag.normalTabsCounter
            },
        contentAlignment = Alignment.Center,
    ) {
        InfernoIcon(
            painter = painterResource(
                id = mozilla.components.ui.tabcounter.R.drawable.mozac_ui_tabcounter_box,
            ),
            contentDescription = normalTabsContentDescription,
            tint = iconColor,
        )

        Text(
            text = normalTabCountText,
            modifier = normalTabsTextModifier,
            color = textColor,
            fontSize = with(LocalDensity.current) { counterTabsTextSize.toDp().toSp() },
            fontWeight = FontWeight.W700,
            textAlign = TextAlign.Center,
        )

        if (showPrivacyBadge) {
            Image(
                painter = painterResource(id = R.drawable.mozac_ic_private_mode_circle_fill_stroke_20),
                contentDescription = null,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(0.dp)
                    .offset(x = 8.dp, y = (-8).dp),
            )
        }
    }
}

@LightDarkPreview
@Preview(locale = "ar")
@Composable
private fun TabCounterPreview() {
    FirefoxTheme {
        Box(
            modifier = Modifier.background(color = FirefoxTheme.colors.layer1),
        ) {
            TabCounter(tabCount = 55)
        }
    }
}
