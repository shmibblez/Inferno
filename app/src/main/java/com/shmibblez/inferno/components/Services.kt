/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package com.shmibblez.inferno.components

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import mozilla.components.browser.state.store.BrowserStore
import mozilla.components.feature.accounts.FirefoxAccountsAuthFeature
import mozilla.components.feature.app.links.AppLinksInterceptor
import mozilla.components.service.fxa.manager.FxaAccountManager
import com.shmibblez.inferno.ext.settings
import com.shmibblez.inferno.perf.lazyMonitored
import com.shmibblez.inferno.settings.SupportUtils

/**
 * Component group which encapsulates foreground-friendly services.
 *
 * When user requests sign in, new custom tab auth intent is launched
 */
class Services(
    private val context: Context,
    private val store: BrowserStore,
    private val accountManager: FxaAccountManager,
) {
    val accountsAuthFeature by lazyMonitored {
        FirefoxAccountsAuthFeature(accountManager, FxaServer.REDIRECT_URL) { context, authUrl ->
            var url = authUrl
            if (context.settings().useReactFxAServer) {
                url = Uri.parse(url)
                    .buildUpon()
                    .appendQueryParameter("forceExperiment", "generalizedReactApp")
                    .appendQueryParameter("forceExperimentGroup", "react")
                    .build()
                    .toString()
            }
            CoroutineScope(Dispatchers.Main).launch {
                val intent = SupportUtils.createAuthCustomTabIntent(context, url)
                context.startActivity(intent)
            }
        }
    }

    val appLinksInterceptor by lazyMonitored {
        AppLinksInterceptor(
            context = context,
            interceptLinkClicks = true,
            launchInApp = { context.settings().shouldOpenLinksInApp() },
            shouldPrompt = { context.settings().shouldPromptOpenLinksInApp() },
            launchFromInterceptor = true,
            store = store,
        )
    }
}
