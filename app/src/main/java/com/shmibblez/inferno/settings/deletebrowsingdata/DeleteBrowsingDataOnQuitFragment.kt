/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package com.shmibblez.inferno.settings.deletebrowsingdata

import android.os.Bundle
import androidx.preference.CheckBoxPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SwitchPreference
import com.shmibblez.inferno.R
import com.shmibblez.inferno.ext.settings
import com.shmibblez.inferno.ext.showToolbar
import com.shmibblez.inferno.settings.SharedPreferenceUpdater
import com.shmibblez.inferno.settings.requirePreference

class DeleteBrowsingDataOnQuitFragment : PreferenceFragmentCompat() {

    private val checkboxes by lazy {
        val context = requireContext()
        DeleteBrowsingDataOnQuitType.entries
            .asSequence()
            .mapNotNull { type ->
                findPreference<CheckBoxPreference>(type.getPreferenceKey(context))?.let { pref ->
                    type to pref
                }
            }
            .toMap()
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.delete_browsing_data_quit_preferences, rootKey)
    }

    @Suppress("ComplexMethod")
    override fun onResume() {
        super.onResume()
        showToolbar(getString(R.string.preferences_delete_browsing_data_on_quit))

        // Delete Browsing Data on Quit Switch
        val deleteOnQuitPref = requirePreference<SwitchPreference>(
            R.string.pref_key_delete_browsing_data_on_quit,
        )
        deleteOnQuitPref.apply {
            onPreferenceChangeListener = object : SharedPreferenceUpdater() {
                override fun onPreferenceChange(preference: Preference, newValue: Any?): Boolean {
                    setAllCheckboxes(newValue as Boolean)
                    return super.onPreferenceChange(preference, newValue)
                }
            }
            isChecked = context.settings().shouldDeleteBrowsingDataOnQuit
        }

        val checkboxUpdater = object : SharedPreferenceUpdater() {
            override fun onPreferenceChange(preference: Preference, newValue: Any?): Boolean {
                super.onPreferenceChange(preference, newValue)
                val settings = preference.context.settings()

                if (!settings.shouldDeleteAnyDataOnQuit()) {
                    deleteOnQuitPref.isChecked = false
                    settings.shouldDeleteBrowsingDataOnQuit = false
                }
                return true
            }
        }

        checkboxes.forEach { (_, pref) ->
            pref.onPreferenceChangeListener = checkboxUpdater
        }
    }

    private fun setAllCheckboxes(newValue: Boolean) {
        checkboxes.forEach { (type, pref) ->
            pref.isChecked = newValue
            pref.context.settings().shouldDeleteBrowsingDataOnQuit =  newValue
        }
    }
}
