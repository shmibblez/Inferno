/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package com.shmibblez.inferno.customtabs

import android.content.Context
import mozilla.components.browser.toolbar.BrowserToolbar
import mozilla.components.concept.toolbar.ScrollableToolbar
import mozilla.components.feature.toolbar.ToolbarFeature
import com.shmibblez.inferno.components.toolbar.ToolbarIntegration
import com.shmibblez.inferno.components.toolbar.ToolbarMenu
import com.shmibblez.inferno.components.toolbar.interactor.BrowserToolbarInteractor

@Suppress("LongParameterList")
class CustomTabToolbarIntegration(
    context: Context,
    toolbar: BrowserToolbar,
    scrollableToolbar: ScrollableToolbar,
    toolbarMenu: ToolbarMenu,
    interactor: BrowserToolbarInteractor,
    customTabId: String,
    isPrivate: Boolean,
) : ToolbarIntegration(
    context = context,
    toolbar = toolbar,
    scrollableToolbar = scrollableToolbar,
    toolbarMenu = toolbarMenu,
    interactor = interactor,
    customTabId = customTabId,
    isPrivate = isPrivate,
    renderStyle = ToolbarFeature.RenderStyle.RegistrableDomain,
)
