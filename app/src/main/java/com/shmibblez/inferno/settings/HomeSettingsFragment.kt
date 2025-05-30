/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package com.shmibblez.inferno.settings

import android.os.Bundle
import androidx.navigation.findNavController
import androidx.preference.CheckBoxPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SwitchPreference
import com.shmibblez.inferno.FeatureFlags
//import com.shmibblez.inferno.GleanMetrics.CustomizeHome
import com.shmibblez.inferno.R
//import com.shmibblez.inferno.components.appstate.AppAction.ContentRecommendationsAction
import com.shmibblez.inferno.ext.navigateWithBreadcrumb
import com.shmibblez.inferno.ext.settings
import com.shmibblez.inferno.ext.showToolbar
import com.shmibblez.inferno.utils.view.addToRadioGroup

/**
 * Lets the user customize the home screen.
 */
class HomeSettingsFragment : PreferenceFragmentCompat() {

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.home_preferences, rootKey)
        setupPreferences()
    }

    override fun onResume() {
        super.onResume()
        showToolbar(getString(R.string.preferences_home_2))
    }

    private fun setupPreferences() {
        requirePreference<SwitchPreference>(R.string.pref_key_show_top_sites).apply {
            isChecked = context.settings().showTopSitesFeature
            onPreferenceChangeListener = object : SharedPreferenceUpdater() {
                override fun onPreferenceChange(preference: Preference, newValue: Any?): Boolean {
//                    CustomizeHome.preferenceToggled.record(
//                        CustomizeHome.PreferenceToggledExtra(
//                            newValue as Boolean,
//                            "most_visited_sites",
//                        ),
//                    )

                    return super.onPreferenceChange(preference, newValue)
                }
            }
        }

        requirePreference<CheckBoxPreference>(R.string.pref_key_enable_contile).apply {
            isChecked = context.settings().showContileFeature
            onPreferenceChangeListener = object : SharedPreferenceUpdater() {
                override fun onPreferenceChange(preference: Preference, newValue: Any?): Boolean {
//                    CustomizeHome.preferenceToggled.record(
//                        CustomizeHome.PreferenceToggledExtra(
//                            newValue as Boolean,
//                            "contile",
//                        ),
//                    )

                    return super.onPreferenceChange(preference, newValue)
                }
            }
        }

        requirePreference<SwitchPreference>(R.string.pref_key_recent_tabs).apply {
            isChecked = context.settings().showRecentTabsFeature
            onPreferenceChangeListener = object : SharedPreferenceUpdater() {
                override fun onPreferenceChange(preference: Preference, newValue: Any?): Boolean {
//                    CustomizeHome.preferenceToggled.record(
//                        CustomizeHome.PreferenceToggledExtra(
//                            newValue as Boolean,
//                            "jump_back_in",
//                        ),
//                    )

                    return super.onPreferenceChange(preference, newValue)
                }
            }
        }

        requirePreference<SwitchPreference>(R.string.pref_key_customization_bookmarks).apply {
            isChecked = context.settings().showBookmarksHomeFeature
            onPreferenceChangeListener = object : SharedPreferenceUpdater() {
                override fun onPreferenceChange(preference: Preference, newValue: Any?): Boolean {
//                    CustomizeHome.preferenceToggled.record(
//                        CustomizeHome.PreferenceToggledExtra(
//                            newValue as Boolean,
//                            "bookmarks",
//                        ),
//                    )

                    return super.onPreferenceChange(preference, newValue)
                }
            }
        }

        requirePreference<SwitchPreference>(R.string.pref_key_pocket_homescreen_recommendations).apply {
            isVisible = FeatureFlags.isPocketRecommendationsFeatureEnabled(context)
            isChecked = context.settings().showPocketRecommendationsFeature
            summary = context.getString(
                R.string.customize_toggle_pocket_summary,
                context.getString(R.string.pocket_product_name),
            )
            onPreferenceChangeListener = object : SharedPreferenceUpdater() {
                override fun onPreferenceChange(preference: Preference, newValue: Any?): Boolean {
//                    CustomizeHome.preferenceToggled.record(
//                        CustomizeHome.PreferenceToggledExtra(
//                            newValue as Boolean,
//                            "pocket",
//                        ),
//                    )

                    return super.onPreferenceChange(preference, newValue)
                }
            }
        }

        requirePreference<CheckBoxPreference>(R.string.pref_key_pocket_sponsored_stories).apply {
            isVisible = FeatureFlags.isPocketSponsoredStoriesFeatureEnabled(context)
            isChecked = context.settings().showPocketSponsoredStories
            onPreferenceChangeListener = object : SharedPreferenceUpdater() {
                override fun onPreferenceChange(preference: Preference, newValue: Any?): Boolean {
                    // TODO: pocket
//                    when (newValue) {
//                        true -> {
//                            context.components.core.pocketStoriesService.startPeriodicSponsoredStoriesRefresh()
//                        }
//                        false -> {
//                            context.components.core.pocketStoriesService.deleteProfile()
//                            context.components.appStore.dispatch(
//                                ContentRecommendationsAction.PocketSponsoredStoriesChange(
//                                    sponsoredStories = emptyList(),
//                                    showContentRecommendations = context.settings().showContentRecommendations,
//                                ),
//                            )
//                        }
//                    }

                    return super.onPreferenceChange(preference, newValue)
                }
            }
        }

        requirePreference<SwitchPreference>(R.string.pref_key_history_metadata_feature).apply {
            isChecked = context.settings().shouldShowHistory
            onPreferenceChangeListener = object : SharedPreferenceUpdater() {
                override fun onPreferenceChange(preference: Preference, newValue: Any?): Boolean {
//                    CustomizeHome.preferenceToggled.record(
//                        CustomizeHome.PreferenceToggledExtra(
//                            newValue as Boolean,
//                            "recently_visited",
//                        ),
//                    )

                    return super.onPreferenceChange(preference, newValue)
                }
            }
        }

        val openingScreenRadioHomepage =
            requirePreference<RadioButtonPreference>(R.string.pref_key_start_on_home_always)
        val openingScreenLastTab =
            requirePreference<RadioButtonPreference>(R.string.pref_key_start_on_home_never)
        val openingScreenAfterFourHours =
            requirePreference<RadioButtonPreference>(R.string.pref_key_start_on_home_after_four_hours)

        requirePreference<Preference>(R.string.pref_key_wallpapers).apply {
            setOnPreferenceClickListener {
                view?.findNavController()?.navigateWithBreadcrumb(
                    directions = HomeSettingsFragmentDirections.actionHomeSettingsFragmentToWallpaperSettingsFragment(),
                    navigateFrom = "HomeSettingsFragment",
                    navigateTo = "ActionHomeSettingsFragmentToWallpaperSettingsFragment",
//                    crashReporter = context.components.analytics.crashReporter,
                )
                true
            }
        }

        addToRadioGroup(
            openingScreenRadioHomepage,
            openingScreenLastTab,
            openingScreenAfterFourHours,
        )
    }
}
