/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package com.shmibblez.inferno.home.topsites

internal object TopSitesTestTag {
    const val topSites = "top_sites_list"

    const val topSiteItemRoot = "$topSites.top_site_item"
    const val topSiteTitle = "$topSiteItemRoot.top_site_title"

    // Contextual/DropDown menu
    const val topSiteContextualMenu = "$topSites.top_site_contextual_menu"
    const val openInPrivateTab = "$topSiteContextualMenu.open_in_private_tab"
    const val edit = "$topSiteContextualMenu.edit"
    const val remove = "$topSiteContextualMenu.remove"
}
