/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package com.shmibblez.inferno.mozillaAndroidComponents.feature.customtabs

import android.app.Activity
import android.content.ActivityNotFoundException
import androidx.annotation.VisibleForTesting
import androidx.annotation.VisibleForTesting.Companion.PRIVATE
import androidx.browser.customtabs.CustomTabColorSchemeParams
import androidx.browser.customtabs.CustomTabsIntent
import androidx.core.net.toUri
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.flow.mapNotNull
import mozilla.components.browser.state.action.ContentAction
import mozilla.components.browser.state.selector.findCustomTab
import mozilla.components.browser.state.state.CustomTabConfig
import mozilla.components.browser.state.store.BrowserStore
import mozilla.components.concept.engine.window.WindowRequest
import mozilla.components.lib.state.ext.flowScoped
import mozilla.components.support.base.feature.LifecycleAwareFeature

const val SHORTCUT_CATEGORY = "mozilla.components.pwa.category.SHORTCUT"

/**
 * Feature implementation for handling window requests by opening custom tabs.
 */
class CustomTabWindowFeature(
    private val activity: Activity,
    private val store: BrowserStore,
    private val sessionId: String,
) : LifecycleAwareFeature {

    private var scope: CoroutineScope? = null

    /**
     * Transform a [CustomTabConfig] into a [CustomTabsIntent] that creates a
     * new custom tab with the same styling and layout
     */
    @Suppress("ComplexMethod")
    @VisibleForTesting(otherwise = PRIVATE)
    internal fun configToIntent(config: CustomTabConfig?): CustomTabsIntent {
        val intent = CustomTabsIntent.Builder().apply {
            setInstantAppsEnabled(false)

            val customTabColorSchemeBuilder = CustomTabColorSchemeParams.Builder()
            config?.colorSchemes?.defaultColorSchemeParams?.toolbarColor?.let {
                customTabColorSchemeBuilder.setToolbarColor(it)
            }
            config?.colorSchemes?.defaultColorSchemeParams?.navigationBarColor?.let {
                customTabColorSchemeBuilder.setNavigationBarColor(it)
            }
            setDefaultColorSchemeParams(customTabColorSchemeBuilder.build())

            if (config?.enableUrlbarHiding == true) setUrlBarHidingEnabled(true)
            config?.closeButtonIcon?.let { setCloseButtonIcon(it) }
            if (config?.showShareMenuItem == true) setShareState(CustomTabsIntent.SHARE_STATE_ON)
            config?.titleVisible?.let { setShowTitle(it) }
            config?.actionButtonConfig?.apply { setActionButton(icon, description, pendingIntent, tint) }
            config?.menuItems?.forEach { addMenuItem(it.name, it.pendingIntent) }
        }.build()

        intent.intent.`package` = activity.packageName
        intent.intent.addCategory(SHORTCUT_CATEGORY)

        return intent
    }

    /**
     * Starts observing the configured session to listen for window requests.
     */
    override fun start() {
        scope = store.flowScoped { flow ->
            flow.mapNotNull { state -> state.findCustomTab(sessionId) }
                .distinctUntilChangedBy {
                    it.content.windowRequest
                }
                .collect { state ->
                    val windowRequest = state.content.windowRequest
                    if (windowRequest?.type == WindowRequest.Type.OPEN) {
                        val intent = configToIntent(state.config)
                        val uri = windowRequest.url.toUri()
                        // This could only fail if the above intent is for our application
                        // and we are not registered to handle its schemes.
                        try {
                            intent.launchUrl(activity, uri)
                        } catch (e: ActivityNotFoundException) {
                            // Workaround for unsupported schemes
                            // See https://bugzilla.mozilla.org/show_bug.cgi?id=1878704
                            state.engineState.engineSession?.loadUrl(windowRequest.url)
                        }
                        store.dispatch(ContentAction.ConsumeWindowRequestAction(sessionId))
                    }
                }
        }
    }

    /**
     * Stops observing the configured session for incoming window requests.
     */
    override fun stop() {
        scope?.cancel()
    }
}
