/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package com.shmibblez.inferno.tabstray.ext

import mozilla.components.browser.state.state.content.DownloadState

fun DownloadState.isActiveDownload(): Boolean {
    return status == DownloadState.Status.INITIATED ||
        status == DownloadState.Status.DOWNLOADING ||
        status == DownloadState.Status.PAUSED
}
