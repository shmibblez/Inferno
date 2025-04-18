/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package com.shmibblez.inferno.theme

import android.app.Activity
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.navigation.fragment.NavHostFragment
import com.shmibblez.inferno.browser.BrowserComponentWrapperFragment
import com.shmibblez.inferno.browser.tabstrip.isTabStripEnabled
import com.shmibblez.inferno.customtabs.ExternalAppBrowserFragment
import com.shmibblez.inferno.home.HomeFragment
import com.shmibblez.inferno.search.SearchDialogFragment

/**
 * Uses the [ThemeManager] to set the status bar color based on the current fragment.
 *
 * @param themeManager The [ThemeManager] to use for setting the status bar color.
 * @param activity The [Activity] to set the status bar color on.
 */
class StatusBarColorManager(
    private val themeManager: ThemeManager,
    private val activity: Activity,
) : FragmentManager.FragmentLifecycleCallbacks() {

    private val isTabStripEnabled = activity.isTabStripEnabled()

    override fun onFragmentResumed(fragmentManager: FragmentManager, fragment: Fragment) {
        if (fragment is NavHostFragment ||
            fragment is DialogFragment ||
            fragment is ExternalAppBrowserFragment
        ) {
            return
        }

        val isTabStripVisible = isTabStripEnabled && isTabStripRelatedFragment(fragment)
        themeManager.applyStatusBarTheme(activity, isTabStripVisible)
    }

    private fun isTabStripRelatedFragment(fragment: Fragment) =
        fragment is HomeFragment || fragment is BrowserComponentWrapperFragment || fragment is SearchDialogFragment
}
