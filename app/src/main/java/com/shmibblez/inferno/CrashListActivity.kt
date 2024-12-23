/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package com.shmibblez.inferno

import android.content.Intent
import android.net.Uri
import mozilla.components.lib.crash.CrashReporter
import mozilla.components.lib.crash.ui.AbstractCrashListActivity
import com.shmibblez.inferno.ext.components

class CrashListActivity : AbstractCrashListActivity() {
    override val crashReporter: CrashReporter by lazy { components.analytics.crashReporter }

    override fun onCrashServiceSelected(url: String) {
        val intent = Intent(Intent.ACTION_VIEW).apply {
            data = Uri.parse(url)
            `package` = packageName
        }
        startActivity(intent)
        finish()
    }
}
