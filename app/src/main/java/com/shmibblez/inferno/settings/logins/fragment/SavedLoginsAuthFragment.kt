/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package com.shmibblez.inferno.settings.logins.fragment

import android.os.Bundle
import androidx.navigation.fragment.findNavController
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SwitchPreference
import mozilla.components.feature.autofill.preference.AutofillPreference
import mozilla.components.service.fxa.SyncEngine
//import mozilla.telemetry.glean.private.NoExtras
//import com.shmibblez.inferno.GleanMetrics.Logins
import com.shmibblez.inferno.R
import com.shmibblez.inferno.components.accounts.FenixFxAEntryPoint
import com.shmibblez.inferno.ext.components
import com.shmibblez.inferno.ext.navigateWithBreadcrumb
import com.shmibblez.inferno.ext.requireComponents
import com.shmibblez.inferno.ext.settings
import com.shmibblez.inferno.ext.showToolbar
import com.shmibblez.inferno.settings.SharedPreferenceUpdater
import com.shmibblez.inferno.settings.SyncPreferenceView
import com.shmibblez.inferno.settings.requirePreference

@Suppress("TooManyFunctions")
class SavedLoginsAuthFragment : PreferenceFragmentCompat() {

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.logins_preferences, rootKey)
    }

    @Suppress("LongMethod")
    override fun onResume() {
        super.onResume()
        showToolbar(getString(R.string.preferences_passwords_logins_and_passwords_2))

        requirePreference<Preference>(R.string.pref_key_save_logins_settings).apply {
            summary = getString(
                if (context.settings().shouldPromptToSaveLogins) {
                    R.string.preferences_passwords_save_logins_ask_to_save
                } else {
                    R.string.preferences_passwords_save_logins_never_save
                },
            )
            setOnPreferenceClickListener {
                navigateToSaveLoginSettingFragment()
                true
            }
        }

        requirePreference<AutofillPreference>(R.string.pref_key_android_autofill).apply {
            update()
        }

        requirePreference<Preference>(R.string.pref_key_login_exceptions).apply {
            setOnPreferenceClickListener {
                navigateToLoginExceptionFragment()
                true
            }
        }

        requirePreference<SwitchPreference>(R.string.pref_key_autofill_logins).apply {
            title = context.getString(
                R.string.preferences_passwords_autofill2,
                getString(R.string.app_name),
            )
            summary = context.getString(
                R.string.preferences_passwords_autofill_description,
                getString(R.string.app_name),
            )
            isChecked = context.settings().shouldAutofillLogins
            onPreferenceChangeListener = object : SharedPreferenceUpdater() {
                override fun onPreferenceChange(preference: Preference, newValue: Any?): Boolean {
                    context.components.core.engine.settings.loginAutofillEnabled =
                        newValue as Boolean
                    return super.onPreferenceChange(preference, newValue)
                }
            }
        }

        requirePreference<Preference>(R.string.pref_key_saved_logins).setOnPreferenceClickListener {
            navigateToSavedLoginsFragment()
            true
        }

        SyncPreferenceView(
            syncPreference = requirePreference(R.string.pref_key_sync_logins),
            lifecycleOwner = viewLifecycleOwner,
            accountManager = requireComponents.backgroundServices.accountManager,
            syncEngine = SyncEngine.Passwords,
            loggedOffTitle = requireContext()
                .getString(R.string.preferences_passwords_sync_logins_across_devices_2),
            loggedInTitle = requireContext()
                .getString(R.string.preferences_passwords_sync_logins_2),
            onSyncSignInClicked = {
                val directions =
                    SavedLoginsAuthFragmentDirections.actionSavedLoginsAuthFragmentToTurnOnSyncFragment(
                        entrypoint = FenixFxAEntryPoint.SavedLogins,
                    )
                findNavController().navigate(directions)
            },
            onReconnectClicked = {
                val directions =
                    SavedLoginsAuthFragmentDirections.actionGlobalAccountProblemFragment(
                        entrypoint = FenixFxAEntryPoint.SavedLogins,
                    )
                findNavController().navigate(directions)
            },
        )
    }

    private fun navigateToSavedLoginsFragment() {
        if (findNavController().currentDestination?.id == R.id.savedLoginsAuthFragment) {
//            Logins.openLogins.record(NoExtras())
            val directions =
                SavedLoginsAuthFragmentDirections.actionSavedLoginsAuthFragmentToLoginsListFragment()
            findNavController().navigate(directions)
        }
    }

    private fun navigateToSaveLoginSettingFragment() {
        val directions =
            SavedLoginsAuthFragmentDirections.actionSavedLoginsAuthFragmentToSavedLoginsSettingFragment()
        findNavController().navigateWithBreadcrumb(
            directions = directions,
            navigateFrom = "SavedLoginsAuthFragment",
            navigateTo = "ActionSavedLoginsAuthFragmentToSavedLoginsSettingFragment",
//            crashReporter = requireComponents.analytics.crashReporter,
        )
    }

    private fun navigateToLoginExceptionFragment() {
        val directions =
            SavedLoginsAuthFragmentDirections.actionSavedLoginsAuthFragmentToLoginExceptionsFragment()
        findNavController().navigate(directions)
    }
}
