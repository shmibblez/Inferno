/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package com.shmibblez.inferno.addons.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
//import com.shmibblez.inferno.mozillaAndroidComponents.base.compose.Divider
//import com.shmibblez.inferno.mozillaAndroidComponents.base.compose.annotation.LightDarkPreview
import mozilla.components.feature.addons.Addon
import mozilla.components.feature.addons.Addon.Companion.isAllURLsPermission
import mozilla.components.feature.addons.Addon.Permission
import com.shmibblez.inferno.R
import com.shmibblez.inferno.addons.AddonPermissionsUpdateRequest
import com.shmibblez.inferno.compose.LinkText
import com.shmibblez.inferno.compose.LinkTextState
import com.shmibblez.inferno.compose.SwitchWithLabel
import com.shmibblez.inferno.compose.base.InfernoText
import com.shmibblez.inferno.compose.base.InfernoTextStyle
import com.shmibblez.inferno.compose.list.TextListItem
import com.shmibblez.inferno.settings.SupportUtils
import com.shmibblez.inferno.theme.FirefoxTheme
import mozilla.components.feature.prompts.identitycredential.previews.LightDarkPreview

/**
 * The permissions screen for an addon which allows a user to edit the optional
 * and origin permissions.
 */
@Composable
@Suppress("LongParameterList", "LongMethod")
fun AddonPermissionsScreen(
    permissions: List<String>,
    optionalPermissions: List<Addon.LocalizedPermission>,
    originPermissions: List<Addon.LocalizedPermission>,
    isAllSitesSwitchVisible: Boolean,
    isAllSitesEnabled: Boolean,
    onAddOptionalPermissions: (AddonPermissionsUpdateRequest) -> Unit,
    onRemoveOptionalPermissions: (AddonPermissionsUpdateRequest) -> Unit,
    onAddAllSitesPermissions: () -> Unit,
    onRemoveAllSitesPermissions: () -> Unit,
    onLearnMoreClick: (String) -> Unit,
    bottomPadding: Dp = 54.dp,
) {
    LazyColumn(modifier = Modifier.padding(vertical = 8.dp).fillMaxWidth()) {
        if (permissions.isNotEmpty()) {
            // Required Permissions Header
            item {
                SectionHeader(
                    label = stringResource(R.string.addons_permissions_heading_required),
                )
            }

            // Required Permissions
            items(items = permissions) { permission ->
                TextListItem(
                    label = permission,
                    maxLabelLines = Int.MAX_VALUE,
                )
            }
        }

        if (permissions.isEmpty() && (optionalPermissions.isEmpty() && originPermissions.isEmpty())) {
            item {
                TextListItem(
                    label = stringResource(R.string.addons_does_not_require_permissions),
                    maxLabelLines = Int.MAX_VALUE,
                )
            }
        }

        // Optional Section
        if (optionalPermissions.isNotEmpty() || originPermissions.isNotEmpty()) {
            // Optional Section Header
            item {
//                Divider()
                VerticalDivider()

                SectionHeader(
                    label = stringResource(id = R.string.addons_permissions_heading_optional),
                )
            }

            // All Sites Toggle if needed
            if (isAllSitesSwitchVisible) {
                item {
                    AllSitesToggle(
                        enabledAllowForAll = isAllSitesEnabled,
                        onAddAllSitesPermissions,
                        onRemoveAllSitesPermissions,
                    )
                }
            }

            // Optional Permissions
            items(
                items = optionalPermissions,
                key = {
                    it.localizedName
                },
            ) { optionalPermission ->

                // Hide <all_urls> permission and use the all_urls toggle instead
                if (!optionalPermission.permission.isAllURLsPermission()) {
                    OptionalPermissionSwitch(
                        modifier = Modifier
                            .padding(horizontal = 16.dp, vertical = 6.dp),
                        localizedPermission = optionalPermission,
                        isOriginPermission = false,
                        addOptionalPermission = onAddOptionalPermissions,
                        removeOptionalPermission = onRemoveOptionalPermissions,
                    )
                }
            }

            // Origin Permissions
            items(
                items = originPermissions,
                key = {
                    it.permission.name
                },
            ) { originPermission ->
                // Hide host permissions when a user has enabled all_urls permission.
                // Also hide permissions that match all_urls because they are replaced by the all_urls toggle.
                if (!originPermission.permission.isAllURLsPermission()) {
                    OptionalPermissionSwitch(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                        localizedPermission = originPermission,
                        isOriginPermission = true,
                        isEnabled = !isAllSitesEnabled,
                        addOptionalPermission = onAddOptionalPermissions,
                        removeOptionalPermission = onRemoveOptionalPermissions,
                    )
                }
            }
        }

        // Learn More
        item {
            val learnMoreState = LinkTextState(
                text = stringResource(R.string.mozac_feature_addons_learn_more),
                url = SupportUtils.getSumoURLForTopic(
                    LocalContext.current,
                    SupportUtils.SumoTopic.MANAGE_OPTIONAL_EXTENSION_PERMISSIONS,
                ),
                onClick = {
                    onLearnMoreClick.invoke(it)
                },
            )

//            Divider()
            VerticalDivider()

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
            ) {
                LinkText(
                    text = stringResource(R.string.mozac_feature_addons_learn_more),
                    linkTextStates = listOf(learnMoreState),
                )
            }
        }

        // bottom padding for dialog buttons
        item {
            Spacer(modifier = Modifier.height(bottomPadding))
        }
    }
}

/**
 * Toggle that handles requesting adding or removing any all_urls permissions.
 * This includes wildcard urls that are considered all_urls permissions such as
 * http://&#42;/, https:///&#42;/, and file:///&#42;//&#42;
 */
@Composable
private fun AllSitesToggle(
    enabledAllowForAll: Boolean,
    onAddAllSitesPermissions: () -> Unit,
    onRemoveAllSitesPermissions: () -> Unit,
) {
    SwitchWithLabel(
        label = stringResource(R.string.addons_permissions_allow_for_all_sites),
        checked = enabledAllowForAll,
        modifier = Modifier
            .padding(horizontal = 16.dp, vertical = 6.dp),
        description = stringResource(R.string.addons_permissions_allow_for_all_sites_subtitle),
    ) { enabled ->
        if (enabled) {
            onAddAllSitesPermissions()
        } else {
            onRemoveAllSitesPermissions()
        }
    }
}

@Composable
private fun SectionHeader(label: String, testTag: String = "") {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .also {
                if (testTag.isNotEmpty()) {
                    it.testTag(testTag)
                }
            },
    ) {
        InfernoText(
            text = label,
            infernoStyle = InfernoTextStyle.Normal,
            fontWeight = FontWeight.Bold,
//            color =  MaterialTheme.colorScheme.secondary, // FirefoxTheme.colors.textAccent,
//            style = MaterialTheme.typography.headlineMedium ,// FirefoxTheme.typography.headline8,
            modifier = Modifier
                .weight(1f),
//                .semantics { heading() },
        )
    }
}

@Composable
private fun OptionalPermissionSwitch(
    modifier: Modifier,
    localizedPermission: Addon.LocalizedPermission,
    isOriginPermission: Boolean,
    isEnabled: Boolean = true,
    addOptionalPermission: (AddonPermissionsUpdateRequest) -> Unit,
    removeOptionalPermission: (AddonPermissionsUpdateRequest) -> Unit,
) {
    SwitchWithLabel(
        label = localizedPermission.localizedName,
        checked = localizedPermission.permission.granted,
        modifier = modifier,
        enabled = isEnabled,
    ) { enabled ->
        if (enabled) {
            addOptionalPermission(
                AddonPermissionsUpdateRequest(
                    optionalPermissions = if (!isOriginPermission) {
                        listOf(localizedPermission.permission.name)
                    } else { emptyList() },
                    originPermissions = if (isOriginPermission) {
                        listOf(localizedPermission.permission.name)
                    } else { emptyList() },
                ),
            )
        } else {
            removeOptionalPermission(
                AddonPermissionsUpdateRequest(
                    optionalPermissions = if (!isOriginPermission) {
                        listOf(localizedPermission.permission.name)
                    } else { emptyList() },
                    originPermissions = if (isOriginPermission) {
                        listOf(localizedPermission.permission.name)
                    } else { emptyList() },
                ),
            )
        }
    }
}

@Composable
@LightDarkPreview
private fun AddonPermissionsScreenPreview() {
    val permissions: List<String> = listOf("Permission required 1", "Permission required 2")
    val optionalPermissions: List<Addon.LocalizedPermission> = listOf(
        Addon.LocalizedPermission(
            "Optional Permission 1",
            Permission("Optional permission 1", false),
        ),
    )
    val originPermissions: List<Addon.LocalizedPermission> = listOf(
        Addon.LocalizedPermission(
            "https://required.website",
            Permission("https://required.website", true),
        ),
        Addon.LocalizedPermission(
            "https://optional-suggested.website...",
            Permission("https://optional-suggested.website...", false),
        ),
        Addon.LocalizedPermission(
            "https://user-added.website.com",
            Permission("https://user-added.website.com", false),
        ),
    )

    FirefoxTheme {
        Column(modifier = Modifier.background(MaterialTheme.colorScheme.background /*FirefoxTheme.colors.layer1*/)) {
            AddonPermissionsScreen(
                permissions = permissions,
                optionalPermissions = optionalPermissions,
                originPermissions = originPermissions,
                isAllSitesSwitchVisible = true,
                isAllSitesEnabled = false,
                onAddOptionalPermissions = { _ -> },
                onRemoveOptionalPermissions = { _ -> },
                onAddAllSitesPermissions = {},
                onRemoveAllSitesPermissions = {},
                onLearnMoreClick = { _ -> },
            )
        }
    }
}

@Composable
@LightDarkPreview
private fun AddonPermissionsScreenWithPermissionsPreview() {
    FirefoxTheme {
        Column(modifier = Modifier.background(MaterialTheme.colorScheme.background /*FirefoxTheme.colors.layer1*/)) {
            AddonPermissionsScreen(
                permissions = emptyList(),
                optionalPermissions = emptyList(),
                originPermissions = emptyList(),
                isAllSitesSwitchVisible = true,
                isAllSitesEnabled = false,
                onAddOptionalPermissions = { _ -> },
                onRemoveOptionalPermissions = { _ -> },
                onAddAllSitesPermissions = {},
                onRemoveAllSitesPermissions = {},
                onLearnMoreClick = { _ -> },
            )
        }
    }
}
