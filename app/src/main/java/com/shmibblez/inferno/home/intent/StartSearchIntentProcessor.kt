/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package com.shmibblez.inferno.home.intent

import android.content.Intent
import androidx.navigation.NavController
import androidx.navigation.navOptions
import com.shmibblez.inferno.HomeActivity
import com.shmibblez.inferno.NavGraphDirections
import com.shmibblez.inferno.R
import com.shmibblez.inferno.ext.nav

/**
 * When the search widget is tapped, Fenix should open to the search fragment.
 * Tapping the private browsing mode launcher icon should also open to the search fragment.
 */
class StartSearchIntentProcessor : HomeIntentProcessor {
    override fun process(intent: Intent, navController: NavController, out: Intent): Boolean {
        val event = intent.extras?.getString(HomeActivity.OPEN_TO_SEARCH)
        return if (event != null) {
            val source = when (event) {
                SEARCH_WIDGET -> {
//                    SearchWidget.newTabButton.record(NoExtras())
//                    MetricsUtils.Source.WIDGET
                }
                STATIC_SHORTCUT_NEW_TAB,
                STATIC_SHORTCUT_NEW_PRIVATE_TAB,
                PRIVATE_BROWSING_PINNED_SHORTCUT,
                -> {
//                    MetricsUtils.Source.SHORTCUT
                }
                else -> null
            }

            out.removeExtra(HomeActivity.OPEN_TO_SEARCH)

            val directions = source?.let {
                NavGraphDirections.actionGlobalSearchDialog(
                    sessionId = null,
//                    searchAccessPoint = it,
                )
            }
            directions?.let {
                val options = navOptions {
                    popUpTo(R.id.homeFragment)
                }
                navController.nav(null, it, options)
            }
            true
        } else {
            false
        }
    }

    companion object {
        const val SEARCH_WIDGET = "search_widget"
        const val STATIC_SHORTCUT_NEW_TAB = "static_shortcut_new_tab"
        const val STATIC_SHORTCUT_NEW_PRIVATE_TAB = "static_shortcut_new_private_tab"
        const val PRIVATE_BROWSING_PINNED_SHORTCUT = "private_browsing_pinned_shortcut"
    }
}
