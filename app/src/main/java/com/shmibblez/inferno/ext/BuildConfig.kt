/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package com.shmibblez.inferno.ext

//import com.shmibblez.inferno.BuildConfig

val isCrashReportActive: Boolean
    get() = false //!BuildConfig.DEBUG && BuildConfig.CRASH_REPORTING_ENABLED
