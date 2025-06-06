/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package com.shmibblez.inferno.settings.account

import android.os.Bundle
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import mozilla.components.concept.sync.AccountObserver
import mozilla.components.concept.sync.AuthType
import mozilla.components.concept.sync.OAuthAccount
import mozilla.components.service.fxa.manager.SCOPE_PROFILE
import mozilla.components.service.fxa.manager.SCOPE_SYNC
//import mozilla.telemetry.glean.private.NoExtras
//import com.shmibblez.inferno.GleanMetrics.SyncAuth
import com.shmibblez.inferno.R
import com.shmibblez.inferno.ext.getPreferenceKey
import com.shmibblez.inferno.ext.nav
import com.shmibblez.inferno.ext.requireComponents
import com.shmibblez.inferno.ext.showToolbar

class AccountProblemFragment : PreferenceFragmentCompat(), AccountObserver {
    private val args by navArgs<AccountProblemFragmentArgs>()

    private val signInClickListener = Preference.OnPreferenceClickListener {
        requireComponents.services.accountsAuthFeature.beginAuthentication(
            requireContext(),
            args.entrypoint,
            setOf(SCOPE_PROFILE, SCOPE_SYNC),
        )
        // TODO The sign-in web content populates session history,
        // so pressing "back" after signing in won't take us back into the settings screen, but rather up the
        // session history stack.
        // We could auto-close this tab once we get to the end of the authentication process?
        // Via an interceptor, perhaps.
        true
    }

    private val signOutClickListener = Preference.OnPreferenceClickListener {
        nav(
            R.id.accountProblemFragment,
            AccountProblemFragmentDirections.actionAccountProblemFragmentToSignOutFragment(),
        )
        true
    }

    override fun onResume() {
        super.onResume()
        showToolbar(getString(R.string.sync_reconnect))

        val accountManager = requireComponents.backgroundServices.accountManager
        accountManager.register(this, owner = this)

        // We may have fixed our auth problem, in which case close this fragment.
        if (accountManager.authenticatedAccount() != null && !accountManager.accountNeedsReauth()) {
            findNavController().popBackStack()
            return
        }
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.sync_problem, rootKey)

        val preferenceSignIn =
            findPreference<Preference>(getPreferenceKey(R.string.pref_key_sync_sign_in))
        val preferenceSignOut =
            findPreference<Preference>(getPreferenceKey(R.string.pref_key_sign_out))
        preferenceSignIn?.onPreferenceClickListener = signInClickListener
        preferenceSignOut?.onPreferenceClickListener = signOutClickListener
    }

    // We're told our auth problems have been fixed; close this fragment.
    override fun onAuthenticated(account: OAuthAccount, authType: AuthType) = closeFragment()

    // We're told there are no more auth problems since there is no more account; close this fragment.
    override fun onLoggedOut() = closeFragment()

    private fun closeFragment() {
        lifecycleScope.launch(Dispatchers.Main) {
            findNavController()
                .popBackStack(R.id.accountProblemFragment, true)
        }
    }
}
