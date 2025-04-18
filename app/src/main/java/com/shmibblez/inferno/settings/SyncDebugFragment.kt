/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package com.shmibblez.inferno.settings

import android.os.Bundle
import androidx.preference.CheckBoxPreference
import androidx.preference.EditTextPreference
import androidx.preference.Preference
import androidx.preference.Preference.OnPreferenceClickListener
import androidx.preference.PreferenceFragmentCompat
import com.shmibblez.inferno.R
import com.shmibblez.inferno.ext.requireComponents
import com.shmibblez.inferno.ext.settings
import com.shmibblez.inferno.ext.showToolbar
import kotlin.system.exitProcess

/**
 * Lets the user customize Private browsing options.
 */
class SyncDebugFragment : PreferenceFragmentCompat() {
    private var hasChanges = false

    private val preferenceUpdater = object : StringSharedPreferenceUpdater() {
        override fun onPreferenceChange(preference: Preference, newValue: Any?): Boolean {
            return super.onPreferenceChange(preference, newValue).also {
                hasChanges = true
                updateMenu()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        showToolbar(getString(R.string.preferences_sync_debug))
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.sync_debug_preferences, rootKey)
        requirePreference<EditTextPreference>(R.string.pref_key_override_fxa_server).let { pref ->
            pref.setOnBindEditTextListener { it.setSingleLine() }
            pref.onPreferenceChangeListener = preferenceUpdater
        }
        requirePreference<EditTextPreference>(R.string.pref_key_override_sync_tokenserver).let { pref ->
            pref.setOnBindEditTextListener { it.setSingleLine() }
            pref.onPreferenceChangeListener = preferenceUpdater
        }
        requirePreference<EditTextPreference>(R.string.pref_key_override_push_server).let { pref ->
            pref.setOnBindEditTextListener { it.setSingleLine() }
            pref.onPreferenceChangeListener = preferenceUpdater
        }
        requirePreference<Preference>(R.string.pref_key_sync_debug_quit).let { pref ->
            pref.onPreferenceClickListener = OnPreferenceClickListener {
                // Copied from StudiesView. This feels like a dramatic way to
                // quit, is there a better way?
                exitProcess(0)
            }
        }
        requirePreference<CheckBoxPreference>(R.string.pref_key_use_react_fxa).apply {
            onPreferenceChangeListener = SharedPreferenceUpdater()
        }
        requirePreference<Preference>(R.string.pref_key_sync_debug_network_error).let { pref ->
            pref.onPreferenceClickListener = OnPreferenceClickListener {
                requireComponents.backgroundServices.accountManager.simulateNetworkError()
                true
            }
        }
        requirePreference<Preference>(R.string.pref_key_sync_debug_temporary_auth_error).let { pref ->
            pref.onPreferenceClickListener = OnPreferenceClickListener {
                requireComponents.backgroundServices.accountManager.simulateTemporaryAuthTokenIssue()
                true
            }
        }
        requirePreference<Preference>(R.string.pref_key_sync_debug_permanent_auth_error).let { pref ->
            pref.onPreferenceClickListener = OnPreferenceClickListener {
                requireComponents.backgroundServices.accountManager.simulatePermanentAuthTokenIssue()
                true
            }
        }
        updateMenu()
    }

    private fun updateMenu() {
        val settings = requireContext().settings()
        requirePreference<EditTextPreference>(R.string.pref_key_override_fxa_server).let {
            it.summary = settings.overrideFxAServer.ifEmpty { null }
        }
        requirePreference<EditTextPreference>(R.string.pref_key_override_sync_tokenserver).let {
            it.summary = settings.overrideSyncTokenServer.ifEmpty { null }
        }
        requirePreference<EditTextPreference>(R.string.pref_key_override_push_server).let {
            it.summary = settings.overridePushServer.ifEmpty { null }
        }
        requirePreference<Preference>(R.string.pref_key_sync_debug_quit).let { pref ->
            pref.isVisible = hasChanges
        }

        // val accountConnected =
        //     requireComponents.backgroundServices.accountManager.authenticatedAccount() == null
    }
}
