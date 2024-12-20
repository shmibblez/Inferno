/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package com.shmibblez.inferno.toolbar

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.shmibblez.inferno.compose.browserStore
import com.shmibblez.inferno.compose.sessionUseCases
import com.shmibblez.inferno.state.observeAsState
import mozilla.components.browser.state.selector.selectedTab
import mozilla.components.browser.state.state.TabSessionState

// TODO:
//  -[ ] implement moz AwesomeBarFeature
//  -[ ] implement moz TabsToolbarFeature
//  -[ ] implement moz ReaderViewIntegration
//  -[ ] implement moz WebExtensionToolbarFeature
//  -[ ] implement moz engineView!!.setDynamicToolbarMaxHeight
//  -[ ] implement moz ToolbarIntegration

@Composable
fun BrowserToolbar(tabSessionState: TabSessionState) {
    val context = LocalContext.current
    val searchTerms by remember { mutableStateOf(tabSessionState.content.searchTerms) }
    val url: String? by browserStore().observeAsState { state -> state.selectedTab?.content?.url }
    val (editMode, setEditMode) = remember { mutableStateOf(false) }
    val useCases = sessionUseCases()
    val (originBounds, setOriginBounds) = remember { mutableStateOf(OriginBounds(0.dp, 0.dp)) }
//    val siteSecure = tabSessionState.content.securityInfo.secure
//    val trackingProtectionEnabled = tabSessionState.trackingProtection.enabled

    if (editMode) {
        BrowserEditToolbar(
            url = url ?: "<empty>",
            setEditMode = setEditMode,
            searchTerms = searchTerms,
            originBounds = originBounds,
        )
    } else {
        BrowserDisplayToolbar(
            url = url ?: "<empty>",
            searchTerms = searchTerms,
            setOriginBounds = setOriginBounds,
            tabSessionState = tabSessionState,
            setEditMode = setEditMode
        )
    }
}

fun detectSiteTrackingProtection(tabSessionState: TabSessionState): SiteTrackingProtection {
    return if (!tabSessionState.trackingProtection.enabled) SiteTrackingProtection.OFF_GLOBALLY
    else {
        if (tabSessionState.trackingProtection.ignoredOnTrackingProtection) SiteTrackingProtection.OFF_FOR_A_SITE
        else if (tabSessionState.trackingProtection.blockedTrackers.isEmpty()) SiteTrackingProtection.ON_NO_TRACKERS_BLOCKED
        else SiteTrackingProtection.ON_TRACKERS_BLOCKED
    }
}

fun detectSiteSecurity(tabSessionState: TabSessionState): SiteSecurity {
    return if (tabSessionState.content.securityInfo.secure) SiteSecurity.SECURE else SiteSecurity.INSECURE
}

@Composable
fun BrowserDisplayToolbar(
    url: String?,
    searchTerms: String,
    setOriginBounds: (OriginBounds) -> Unit,
    tabSessionState: TabSessionState,
    setEditMode: (Boolean) -> Unit,
) {
    var textFullSize by remember { mutableStateOf(true) }

    LaunchedEffect(true) {
        textFullSize = false
    }

    Row {
        if (!textFullSize) {
            ToolbarLeftArrow(enabled = tabSessionState.content.canGoBack)
            ToolbarRightArrow(enabled = tabSessionState.content.canGoForward)
        }
        ToolbarOrigin(
            modifier = Modifier.animateContentSize() { initialValue, targetValue ->
                // finished callback
            }, toolbarOriginData = ToolbarOriginData(
                url = url,
                searchTerms = searchTerms,
                siteSecure = detectSiteSecurity(tabSessionState),
                siteTrackingProtection = detectSiteTrackingProtection(tabSessionState),
                setEditMode = setEditMode,
            ), setOriginBounds = setOriginBounds
        )
        if (!textFullSize) {
            ToolbarReload(enabled = true)
            ToolbarShowTabsTray()
        }
    }
}

// TODO: test and add blend animation
@Composable
fun BrowserEditToolbar(
    url: String,
    setEditMode: (Boolean) -> Unit,
    searchTerms: String,
    originBounds: OriginBounds,
) {
    var input by remember { mutableStateOf(searchTerms.ifEmpty { url }) }
    var textFullSize by remember { mutableStateOf(false) }
    val useCases = sessionUseCases()
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(true) {
        // animate to fill width after first compose
        textFullSize = true
    }

    Row {
        if (!textFullSize) Box(
            modifier = Modifier
                .width(originBounds.left)
                .fillMaxHeight()
        )
        TextField(
            value = input,
            onValueChange = { value -> input = value },
            singleLine = true,
            modifier = Modifier
                .fillMaxHeight()
                .weight(1F)
                .padding(all = 4.dp)
                .border(BorderStroke(0.dp, Color.Transparent), shape = RoundedCornerShape(2.dp))
                .background(Color.DarkGray)
                .onFocusChanged { focusState ->
                    // if focus lost, go back to editing mode
                    if (!focusState.isFocused) setEditMode(false)
                }
                .focusable(true)
                .focusRequester(focusRequester)
                .animateContentSize() { initialValue, targetValue ->
                    if (initialValue.width > targetValue.width) {
                        // after animation, request focus and show keyboard
                        focusRequester.requestFocus()
                    }
                },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Uri, imeAction = ImeAction.Go
            ),
            keyboardActions = KeyboardActions(
                onGo = {
                    useCases.loadUrl(searchTerms)
                    setEditMode(false)
                },
            ),
        )
        if (!textFullSize) Box(
            modifier = Modifier
                .width(originBounds.right)
                .fillMaxHeight()
        )
    }
}
