/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package com.shmibblez.inferno.components

import android.content.Context
import android.os.StrictMode
import mozilla.components.browser.state.store.BrowserStore
import mozilla.components.concept.engine.Engine
import mozilla.components.concept.fetch.Client
import mozilla.components.concept.storage.BookmarksStorage
import mozilla.components.concept.storage.HistoryStorage
import mozilla.components.feature.accounts.push.CloseTabsUseCases
import mozilla.components.feature.app.links.AppLinksUseCases
import mozilla.components.feature.contextmenu.ContextMenuUseCases
import mozilla.components.feature.downloads.DownloadsUseCases
import mozilla.components.feature.pwa.WebAppShortcutManager
import mozilla.components.feature.pwa.WebAppUseCases
import mozilla.components.feature.search.SearchUseCases
import mozilla.components.feature.session.SessionUseCases
import mozilla.components.feature.session.SettingsUseCases
import mozilla.components.feature.session.TrackingProtectionUseCases
import mozilla.components.feature.syncedtabs.commands.SyncedTabsCommands
import mozilla.components.feature.tabs.CustomTabsUseCases
import mozilla.components.feature.tabs.TabsUseCases
import mozilla.components.feature.top.sites.TopSitesStorage
import mozilla.components.feature.top.sites.TopSitesUseCases
import mozilla.components.support.locale.LocaleManager
import mozilla.components.support.locale.LocaleUseCases
import com.shmibblez.inferno.components.bookmarks.BookmarksUseCase
import com.shmibblez.inferno.perf.StrictModeManager
import com.shmibblez.inferno.perf.lazyMonitored
import com.shmibblez.inferno.wallpapers.WallpapersUseCases

/**
 * Component group for all use cases. Use cases are provided by feature
 * modules and can be triggered by UI interactions.
 */
@Suppress("LongParameterList")
class UseCases(
    private val context: Context,
    private val engine: Engine,
    private val store: BrowserStore,
    private val shortcutManager: WebAppShortcutManager,
    private val topSitesStorage: TopSitesStorage,
    private val bookmarksStorage: BookmarksStorage,
    private val historyStorage: HistoryStorage,
    private val syncedTabsCommands: SyncedTabsCommands,
    appStore: AppStore,
    client: Client,
    strictMode: StrictModeManager,
) {
    /**
     * Use cases that provide engine interactions for a given browser session.
     */
    val sessionUseCases by lazyMonitored { SessionUseCases(store) }

    /**
     * Use cases that provide tab management.
     */
    val tabsUseCases: TabsUseCases by lazyMonitored { TabsUseCases(store) }

    /**
     * Use cases for managing custom tabs.
     */
    val customTabsUseCases: CustomTabsUseCases by lazyMonitored {
        CustomTabsUseCases(store, sessionUseCases.loadUrl)
    }

    /**
     * Use cases that provide search engine integration.
     */
    val searchUseCases by lazyMonitored {
        SearchUseCases(
            store,
            tabsUseCases,
            sessionUseCases,
        )
    }

    /**
     * Use cases that provide settings management.
     */
    val settingsUseCases by lazyMonitored { SettingsUseCases(engine, store) }

    val appLinksUseCases by lazyMonitored { AppLinksUseCases(context.applicationContext) }

    val webAppUseCases by lazyMonitored {
        WebAppUseCases(context, store, shortcutManager)
    }

    val downloadUseCases by lazyMonitored { DownloadsUseCases(store) }

    val contextMenuUseCases by lazyMonitored { ContextMenuUseCases(store) }

    val trackingProtectionUseCases by lazyMonitored { TrackingProtectionUseCases(store, engine) }

    /**
     * Use cases that provide top sites management.
     */
    val topSitesUseCase by lazyMonitored { TopSitesUseCases(topSitesStorage) }

    /**
     * Use cases that handle locale management.
     */
    val localeUseCases by lazyMonitored { LocaleUseCases(store) }

    /**
     * Use cases that provide bookmark management.
     */
    val bookmarksUseCases by lazyMonitored { BookmarksUseCase(bookmarksStorage, historyStorage) }

    val wallpaperUseCases by lazyMonitored {
        // Required to even access context.filesDir property and to retrieve current locale
        val (rootStorageDirectory, currentLocale) = strictMode.resetAfter(StrictMode.allowThreadDiskReads()) {
            val rootStorageDirectory = context.filesDir
            val currentLocale = LocaleManager.getCurrentLocale(context)?.toLanguageTag()
                ?: LocaleManager.getSystemDefault().toLanguageTag()
            rootStorageDirectory to currentLocale
        }
        WallpapersUseCases(context, appStore, client, rootStorageDirectory, currentLocale)
    }

    val closeSyncedTabsUseCases by lazyMonitored { CloseTabsUseCases(syncedTabsCommands) }
}
