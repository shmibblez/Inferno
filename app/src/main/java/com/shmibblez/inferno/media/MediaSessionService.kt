/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package com.shmibblez.inferno.media

import mozilla.components.browser.state.store.BrowserStore
import mozilla.components.concept.base.crash.CrashReporting
import mozilla.components.feature.media.service.AbstractMediaSessionService
import mozilla.components.support.base.android.NotificationsDelegate
import com.shmibblez.inferno.ext.components

/**
 * [AbstractMediaSessionService] implementation for injecting [BrowserStore] singleton.
 */
class MediaSessionService : AbstractMediaSessionService() {
    override val store: BrowserStore by lazy { components.core.store }
    override val crashReporter = null // CrashReporting by lazy { components.analytics.crashReporter }
    override val notificationsDelegate: NotificationsDelegate by lazy { components.notificationsDelegate }
}
