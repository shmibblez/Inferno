/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package com.shmibblez.inferno.home.sessioncontrol.viewholders

import android.view.View
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.LifecycleOwner
import com.shmibblez.inferno.R
import com.shmibblez.inferno.compose.ComposeViewHolder
import com.shmibblez.inferno.ext.infernoTheme
import com.shmibblez.inferno.ext.settings
import com.shmibblez.inferno.home.privatebrowsing.interactor.PrivateBrowsingInteractor

/**
 * View holder for a private browsing description.
 *
 * @param composeView [ComposeView] which will be populated with Jetpack Compose UI content.
 * @param viewLifecycleOwner [LifecycleOwner] life cycle owner for the view.
 * @property interactor [PrivateBrowsingInteractor] which will have delegated to all user interactions.
 */
class PrivateBrowsingDescriptionViewHolder(
    composeView: ComposeView,
    viewLifecycleOwner: LifecycleOwner,
    val interactor: PrivateBrowsingInteractor,
) : ComposeViewHolder(composeView, viewLifecycleOwner) {

    init {
        val horizontalPadding =
            composeView.resources.getDimensionPixelSize(R.dimen.home_item_horizontal_margin)
        composeView.setPadding(horizontalPadding, 0, horizontalPadding, 0)
    }

    @Composable
    override fun Content() {
        val settings = composeView.context.settings()

        if (settings.feltPrivateBrowsingEnabled) {
            FeltPrivacyModeInfoCard(
                onLearnMoreClick = interactor::onLearnMoreClicked,
            )
        } else {
            PrivateBrowsingDescription(
                onLearnMoreClick = interactor::onLearnMoreClicked,
            )
        }
    }

    companion object {
        val LAYOUT_ID = View.generateViewId()
    }
}

/**
 * Private browsing mode description.
 *
 * @param onLearnMoreClick Invoked when the user clicks on the learn more link.
 */
@Composable
fun PrivateBrowsingDescription(
    onLearnMoreClick: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }

    Column(
        modifier = Modifier.padding(horizontal = 4.dp),
    ) {
        Text(
            text = stringResource(
                R.string.private_browsing_placeholder_description_2,
                stringResource(R.string.app_name),
            ),
            modifier = Modifier.padding(top = 4.dp),
            color = LocalContext.current.infernoTheme().value.primaryTextColor,
            fontSize = 14.sp,
            lineHeight = 20.sp,
        )

        // The text is wrapped in a box to increase the tap area.
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    onClickLabel = stringResource(R.string.a11y_action_label_read_article),
                    onClick = onLearnMoreClick,
                ),
        ) {
            Text(
                text = stringResource(R.string.private_browsing_common_myths),
                modifier = Modifier.padding(top = 10.dp),
                style = TextStyle(
                    color = LocalContext.current.infernoTheme().value.primaryTextColor,
                    fontSize = 14.sp,
                    textDecoration = TextDecoration.Underline,
                    textDirection = TextDirection.Content,
                ),
            )
        }
    }
}

@Composable
@Preview
private fun PrivateBrowsingDescriptionPreview() {
    PrivateBrowsingDescription(
        onLearnMoreClick = {},
    )
}
