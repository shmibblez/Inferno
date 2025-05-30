/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package com.shmibblez.inferno.shopping.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.shmibblez.inferno.mozillaAndroidComponents.compose.base.annotation.LightDarkPreview
import com.shmibblez.inferno.R
import com.shmibblez.inferno.compose.InfoCardContainer
import com.shmibblez.inferno.compose.LinkText
import com.shmibblez.inferno.compose.LinkTextState
import com.shmibblez.inferno.compose.button.PrimaryButton
import com.shmibblez.inferno.shopping.store.ReviewQualityCheckState
import com.shmibblez.inferno.shopping.store.ReviewQualityCheckState.ProductVendor
import com.shmibblez.inferno.shopping.ui.ext.displayName
import com.shmibblez.inferno.shopping.ui.ext.headingResource
import com.shmibblez.inferno.theme.FirefoxTheme

private const val MAX_SUPPORTED_VENDORS_PER_TLD = 3

/**
 * A placeholder UI for review quality check contextual onboarding. The actual UI will be
 * implemented as part of Bug 1840103 with the illustration.
 *
 * @param productVendors List of retailers to be displayed in order.
 * @param onLearnMoreClick Invoked when a user clicks on the learn more link.
 * @param onPrivacyPolicyClick Invoked when a user clicks on the privacy policy link.
 * @param onTermsOfUseClick Invoked when a user clicks on the terms of use link.
 * @param onPrimaryButtonClick Invoked when a user clicks on the primary button.
 * @param onSecondaryButtonClick Invoked when a user clicks on the secondary button.
 */
@Suppress("LongMethod")
@Composable
fun ReviewQualityCheckContextualOnboarding(
    productVendors: List<ProductVendor>,
    onLearnMoreClick: () -> Unit,
    onPrivacyPolicyClick: () -> Unit,
    onTermsOfUseClick: () -> Unit,
    onPrimaryButtonClick: () -> Unit,
    onSecondaryButtonClick: () -> Unit,
) {
    val learnMoreText =
        stringResource(id = R.string.review_quality_check_contextual_onboarding_learn_more_link)
    val privacyPolicyText =
        stringResource(id = R.string.review_quality_check_contextual_onboarding_privacy_policy_3)
    val termsOfUseText =
        stringResource(id = R.string.review_quality_check_contextual_onboarding_terms_use)
    val titleContentDescription =
        headingResource(R.string.review_quality_check_contextual_onboarding_title)

    InfoCardContainer(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = stringResource(R.string.review_quality_check_contextual_onboarding_title),
            color = FirefoxTheme.colors.textPrimary,
            style = FirefoxTheme.typography.headline5,
            modifier = Modifier.semantics {
                heading()
                contentDescription = titleContentDescription
            },
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = createDescriptionString(productVendors),
            color = FirefoxTheme.colors.textSecondary,
            style = FirefoxTheme.typography.body2,
        )

        Spacer(modifier = Modifier.height(16.dp))

        LinkText(
            text = stringResource(
                id = R.string.review_quality_check_contextual_onboarding_learn_more,
                stringResource(id = R.string.shopping_product_name),
                learnMoreText,
            ),
            linkTextStates = listOf(
                LinkTextState(
                    text = learnMoreText,
                    url = "",
                    onClick = {
                        onLearnMoreClick()
                    },
                ),
            ),
            linkTextDecoration = TextDecoration.Underline,
        )

        Spacer(modifier = Modifier.height(16.dp))

        LinkText(
            text = stringResource(
                id = R.string.review_quality_check_contextual_onboarding_caption_4,
                stringResource(id = R.string.firefox),
                privacyPolicyText,
                stringResource(id = R.string.shopping_product_name),
                termsOfUseText,
            ),
            linkTextStates = listOf(
                LinkTextState(
                    text = privacyPolicyText,
                    url = "",
                    onClick = {
                        onPrivacyPolicyClick()
                    },
                ),
                LinkTextState(
                    text = termsOfUseText,
                    url = "",
                    onClick = {
                        onTermsOfUseClick()
                    },
                ),
            ),
            linkTextDecoration = TextDecoration.Underline,
        )

        Spacer(modifier = Modifier.height(16.dp))

        Image(
            painter = painterResource(id = R.drawable.shopping_onboarding),
            contentDescription = null,
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
                .padding(all = 16.dp),
        )

        Spacer(modifier = Modifier.height(16.dp))

        PrimaryButton(
            text = stringResource(R.string.review_quality_check_contextual_onboarding_primary_button_text),
            onClick = onPrimaryButtonClick,
        )

        Spacer(modifier = Modifier.height(8.dp))

        TextButton(
            onClick = onSecondaryButtonClick,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                text = stringResource(R.string.review_quality_check_contextual_onboarding_secondary_button_text),
                color = FirefoxTheme.colors.textAccent,
                style = FirefoxTheme.typography.button,
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun createDescriptionString(
    retailers: List<ProductVendor>,
) = buildAnnotatedString {
    val retailerNames = retailers.map { it.displayName() }

    val description = if (retailers.size == MAX_SUPPORTED_VENDORS_PER_TLD) {
        stringResource(
            id = R.string.review_quality_check_contextual_onboarding_description,
            retailerNames[0],
            stringResource(R.string.app_name),
            retailerNames[1],
            retailerNames[2],
        )
    } else {
        stringResource(
            id = R.string.review_quality_check_contextual_onboarding_description_one_vendor,
            retailerNames.first(),
            stringResource(R.string.app_name),
        )
    }

    append(description)

    retailerNames.forEach { retailer ->
        val start = description.indexOf(retailer)

        addStyle(
            style = SpanStyle(fontWeight = FontWeight.Bold),
            start = start,
            end = start + retailer.length,
        )
    }
}

@Composable
@LightDarkPreview
private fun ProductAnalysisPreview() {
    FirefoxTheme {
        ReviewQualityCheckScaffold(
            onRequestDismiss = {},
        ) {
            ReviewQualityCheckContextualOnboarding(
                productVendors = ReviewQualityCheckState.NotOptedIn().productVendors,
                onPrimaryButtonClick = {},
                onLearnMoreClick = {},
                onPrivacyPolicyClick = {},
                onTermsOfUseClick = {},
                onSecondaryButtonClick = {},
            )
        }
    }
}
