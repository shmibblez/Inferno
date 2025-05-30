/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package com.shmibblez.inferno.shopping.middleware

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.shmibblez.inferno.nimbus.FxNimbus
import com.shmibblez.inferno.utils.Settings

/**
 * Interface to get and set preferences for the review quality check feature.
 */
interface ReviewQualityCheckPreferences {
    /**
     * Returns true if the user has opted in to the review quality check feature.
     */
    suspend fun enabled(): Boolean

    /**
     * Returns true if the user has turned on product recommendations, false if turned off by the
     * user, null if the product recommendations feature is disabled.
     */
    suspend fun productRecommendationsEnabled(): Boolean?

    /**
     * Sets whether the user has opted in to the review quality check feature.
     */
    suspend fun setEnabled(isEnabled: Boolean)

    /**
     * Sets user preference to turn on/off product recommendations.
     */
    suspend fun setProductRecommendationsEnabled(isEnabled: Boolean)
}

/**
 * Implementation of [ReviewQualityCheckPreferences] that uses [Settings] to store/fetch
 * preferences.
 *
 * @param settings The [Settings] instance to use.
 */
class DefaultReviewQualityCheckPreferences(
    private val settings: Settings,
) : ReviewQualityCheckPreferences {

    override suspend fun enabled(): Boolean = withContext(Dispatchers.IO) {
        settings.isReviewQualityCheckEnabled
    }

    override suspend fun productRecommendationsEnabled(): Boolean? = withContext(Dispatchers.IO) {
        if (FxNimbus.features.shoppingExperience.value().productRecommendations) {
            settings.isReviewQualityCheckProductRecommendationsEnabled
        } else {
            null
        }
    }

    override suspend fun setEnabled(isEnabled: Boolean) {
        settings.isReviewQualityCheckEnabled = isEnabled
    }

    override suspend fun setProductRecommendationsEnabled(isEnabled: Boolean) {
        settings.isReviewQualityCheckProductRecommendationsEnabled = isEnabled
    }
}
