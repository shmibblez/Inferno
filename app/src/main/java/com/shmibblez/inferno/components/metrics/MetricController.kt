// FIXME: NOT USED
///* This Source Code Form is subject to the terms of the Mozilla Public
// * License, v. 2.0. If a copy of the MPL was not distributed with this
// * file, You can obtain one at http://mozilla.org/MPL/2.0/. */
//
//package com.shmibblez.inferno.components.metrics
//
//import androidx.annotation.VisibleForTesting
//import mozilla.components.browser.menu.facts.BrowserMenuFacts
//import mozilla.components.browser.toolbar.facts.ToolbarFacts
//import mozilla.components.concept.awesomebar.AwesomeBar
//import mozilla.components.feature.autofill.facts.AutofillFacts
//import mozilla.components.feature.awesomebar.facts.AwesomeBarFacts
//import mozilla.components.feature.awesomebar.provider.BookmarksStorageSuggestionProvider
//import mozilla.components.feature.awesomebar.provider.ClipboardSuggestionProvider
//import mozilla.components.feature.awesomebar.provider.HistoryStorageSuggestionProvider
//import mozilla.components.feature.awesomebar.provider.SearchSuggestionProvider
//import mozilla.components.feature.awesomebar.provider.SessionSuggestionProvider
//import mozilla.components.feature.contextmenu.facts.ContextMenuFacts
//import mozilla.components.feature.fxsuggest.FxSuggestInteractionInfo
//import mozilla.components.feature.fxsuggest.facts.FxSuggestFacts
//import mozilla.components.feature.media.facts.MediaFacts
//import mozilla.components.feature.prompts.dialog.GeneratedPasswordFacts
//import mozilla.components.feature.prompts.dialog.LoginDialogFacts
//import mozilla.components.feature.prompts.facts.AddressAutofillDialogFacts
//import mozilla.components.feature.prompts.facts.CreditCardAutofillDialogFacts
//import mozilla.components.feature.prompts.facts.LoginAutofillDialogFacts
//import mozilla.components.feature.pwa.ProgressiveWebAppFacts
//import mozilla.components.feature.search.telemetry.ads.AdsTelemetry
//import mozilla.components.feature.search.telemetry.incontent.InContentTelemetry
//import mozilla.components.feature.sitepermissions.SitePermissionsFacts
//import mozilla.components.feature.syncedtabs.facts.SyncedTabsFacts
//import mozilla.components.feature.top.sites.facts.TopSitesFacts
//import mozilla.components.service.fxa.SyncFacts
//import mozilla.components.support.base.Component
//import mozilla.components.support.base.facts.Action
//import mozilla.components.support.base.facts.Fact
//import mozilla.components.support.base.facts.FactProcessor
//import mozilla.components.support.base.facts.Facts
//import mozilla.components.support.base.log.logger.Logger
//import mozilla.components.support.webextensions.facts.WebExtensionFacts
//import mozilla.telemetry.glean.private.NoExtras
//import com.shmibblez.inferno.BuildConfig
//import com.shmibblez.inferno.GleanMetrics.Addons
//import com.shmibblez.inferno.GleanMetrics.Addresses
//import com.shmibblez.inferno.GleanMetrics.AndroidAutofill
//import com.shmibblez.inferno.GleanMetrics.Awesomebar
//import com.shmibblez.inferno.GleanMetrics.BrowserSearch
//import com.shmibblez.inferno.GleanMetrics.ContextMenu
//import com.shmibblez.inferno.GleanMetrics.ContextualMenu
//import com.shmibblez.inferno.GleanMetrics.CreditCards
//import com.shmibblez.inferno.GleanMetrics.Events
//import com.shmibblez.inferno.GleanMetrics.FxSuggest
//import com.shmibblez.inferno.GleanMetrics.GeneratedPasswordDialog
//import com.shmibblez.inferno.GleanMetrics.LoginDialog
//import com.shmibblez.inferno.GleanMetrics.Logins
//import com.shmibblez.inferno.GleanMetrics.MediaNotification
//import com.shmibblez.inferno.GleanMetrics.MediaState
//import com.shmibblez.inferno.GleanMetrics.NavigationBar
//import com.shmibblez.inferno.GleanMetrics.PerfAwesomebar
//import com.shmibblez.inferno.GleanMetrics.Pings
//import com.shmibblez.inferno.GleanMetrics.ProgressiveWebApp
//import com.shmibblez.inferno.GleanMetrics.SitePermissions
//import com.shmibblez.inferno.GleanMetrics.Sync
//import com.shmibblez.inferno.GleanMetrics.SyncedTabs
//import com.shmibblez.inferno.search.awesomebar.ShortcutsSuggestionProvider
//import com.shmibblez.inferno.utils.Settings
//import java.util.UUID
//import mozilla.components.compose.browser.awesomebar.AwesomeBarFacts as ComposeAwesomeBarFacts
//
//interface MetricController {
//    fun start(type: MetricServiceType)
//    fun stop(type: MetricServiceType)
//    fun track(event: Event)
//
//    companion object {
//        fun create(
//            services: List<MetricsService>,
//            isDataTelemetryEnabled: () -> Boolean,
//            isMarketingDataTelemetryEnabled: () -> Boolean,
//            settings: Settings,
//        ): MetricController {
//            return if (BuildConfig.TELEMETRY) {
//                ReleaseMetricController(
//                    services,
//                    isDataTelemetryEnabled,
//                    isMarketingDataTelemetryEnabled,
//                    settings,
//                )
//            } else {
//                DebugMetricController()
//            }
//        }
//    }
//}
//
//@VisibleForTesting
//internal class DebugMetricController(
//    private val logger: Logger = Logger(),
//) : MetricController {
//
//    override fun start(type: MetricServiceType) {
//        logger.debug("DebugMetricController: start")
//    }
//
//    override fun stop(type: MetricServiceType) {
//        logger.debug("DebugMetricController: stop")
//    }
//
//    override fun track(event: Event) {
//        logger.debug("DebugMetricController: track event: $event")
//    }
//}
//
//@VisibleForTesting
//@Suppress("LargeClass")
//internal class ReleaseMetricController(
//    private val services: List<MetricsService>,
//    private val isDataTelemetryEnabled: () -> Boolean,
//    private val isMarketingDataTelemetryEnabled: () -> Boolean,
//    private val settings: Settings,
//) : MetricController {
//    private var initialized = mutableSetOf<MetricServiceType>()
//
//    init {
//        Facts.registerProcessor(
//            object : FactProcessor {
//                override fun process(fact: Fact) {
//                    fact.process()
//                }
//            },
//        )
//    }
//
//    @VisibleForTesting
//    @Suppress("LongMethod")
//    internal fun Fact.process(): Unit = when (component to item) {
//        Component.FEATURE_PROMPTS to LoginDialogFacts.Items.DISPLAY -> {
//            LoginDialog.displayed.record(NoExtras())
//        }
//        Component.FEATURE_PROMPTS to LoginDialogFacts.Items.CANCEL -> {
//            LoginDialog.cancelled.record(NoExtras())
//        }
//        Component.FEATURE_PROMPTS to LoginDialogFacts.Items.NEVER_SAVE -> {
//            LoginDialog.neverSave.record(NoExtras())
//        }
//        Component.FEATURE_PROMPTS to LoginDialogFacts.Items.SAVE -> {
//            LoginDialog.saved.record(NoExtras())
//        }
//        Component.FEATURE_PROMPTS to GeneratedPasswordFacts.Items.SHOWN -> {
//            GeneratedPasswordDialog.shown.record(NoExtras())
//        }
//        Component.FEATURE_PROMPTS to GeneratedPasswordFacts.Items.FILLED -> {
//            GeneratedPasswordDialog.filled.record(NoExtras())
//        }
//        Component.FEATURE_MEDIA to MediaFacts.Items.STATE -> {
//            when (action) {
//                Action.PLAY -> MediaState.play.record(NoExtras())
//                Action.PAUSE -> MediaState.pause.record(NoExtras())
//                Action.STOP -> MediaState.stop.record(NoExtras())
//                else -> Unit
//            }
//        }
//        Component.FEATURE_MEDIA to MediaFacts.Items.NOTIFICATION -> {
//            when (action) {
//                Action.PLAY -> MediaNotification.play.record(NoExtras())
//                Action.PAUSE -> MediaNotification.pause.record(NoExtras())
//                else -> Unit
//            }
//        }
//        Component.BROWSER_TOOLBAR to ToolbarFacts.Items.MENU -> {
//            if (settings.navigationToolbarEnabled) {
//                NavigationBar.browserMenuTapped.record(NoExtras())
//            } else {
//                Events.toolbarMenuVisible.record(NoExtras())
//            }
//        }
//        Component.UI_TABCOUNTER to ToolbarFacts.Items.TOOLBAR -> {
//            if (settings.navigationToolbarEnabled) {
//                NavigationBar.browserTabTrayTapped.record(NoExtras())
//            } else {
//                Unit
//            }
//        }
//        Component.UI_TABCOUNTER to ToolbarFacts.Items.MENU -> {
//            if (settings.navigationToolbarEnabled) {
//                NavigationBar.browserTabTrayLongTapped.record(NoExtras())
//            } else {
//                Unit
//            }
//        }
//        Component.FEATURE_CONTEXTMENU to ContextMenuFacts.Items.ITEM -> {
//            metadata?.get("item")?.let { item ->
//                contextMenuAllowList[item]?.let { extraKey ->
//                    ContextMenu.itemTapped.record(ContextMenu.ItemTappedExtra(extraKey))
//                }
//            }
//            Unit
//        }
//
//        Component.BROWSER_MENU to BrowserMenuFacts.Items.WEB_EXTENSION_MENU_ITEM -> {
//            metadata?.get("id")?.let {
//                Addons.openAddonInToolbarMenu.record(Addons.OpenAddonInToolbarMenuExtra(it.toString()))
//            }
//            Unit
//        }
//        Component.FEATURE_PROMPTS to CreditCardAutofillDialogFacts.Items.AUTOFILL_CREDIT_CARD_FORM_DETECTED ->
//            CreditCards.formDetected.record(NoExtras())
//        Component.FEATURE_PROMPTS to CreditCardAutofillDialogFacts.Items.AUTOFILL_CREDIT_CARD_SUCCESS ->
//            CreditCards.autofilled.record(NoExtras())
//        Component.FEATURE_PROMPTS to CreditCardAutofillDialogFacts.Items.AUTOFILL_CREDIT_CARD_PROMPT_SHOWN ->
//            CreditCards.autofillPromptShown.record(NoExtras())
//        Component.FEATURE_PROMPTS to CreditCardAutofillDialogFacts.Items.AUTOFILL_CREDIT_CARD_PROMPT_EXPANDED ->
//            CreditCards.autofillPromptExpanded.record(NoExtras())
//        Component.FEATURE_PROMPTS to CreditCardAutofillDialogFacts.Items.AUTOFILL_CREDIT_CARD_PROMPT_DISMISSED ->
//            CreditCards.autofillPromptDismissed.record(NoExtras())
//        Component.FEATURE_PROMPTS to CreditCardAutofillDialogFacts.Items.AUTOFILL_CREDIT_CARD_CREATED ->
//            CreditCards.savePromptCreate.record(NoExtras())
//        Component.FEATURE_PROMPTS to CreditCardAutofillDialogFacts.Items.AUTOFILL_CREDIT_CARD_UPDATED ->
//            CreditCards.savePromptUpdate.record(NoExtras())
//        Component.FEATURE_PROMPTS to CreditCardAutofillDialogFacts.Items.AUTOFILL_CREDIT_CARD_SAVE_PROMPT_SHOWN ->
//            CreditCards.savePromptShown.record(NoExtras())
//
//        Component.FEATURE_PROMPTS to AddressAutofillDialogFacts.Items.AUTOFILL_ADDRESS_FORM_DETECTED ->
//            Addresses.formDetected.record(NoExtras())
//        Component.FEATURE_PROMPTS to AddressAutofillDialogFacts.Items.AUTOFILL_ADDRESS_SUCCESS ->
//            Addresses.autofilled.record(NoExtras())
//        Component.FEATURE_PROMPTS to AddressAutofillDialogFacts.Items.AUTOFILL_ADDRESS_PROMPT_SHOWN ->
//            Addresses.autofillPromptShown.record(NoExtras())
//        Component.FEATURE_PROMPTS to AddressAutofillDialogFacts.Items.AUTOFILL_ADDRESS_PROMPT_EXPANDED ->
//            Addresses.autofillPromptExpanded.record(NoExtras())
//        Component.FEATURE_PROMPTS to AddressAutofillDialogFacts.Items.AUTOFILL_ADDRESS_PROMPT_DISMISSED ->
//            Addresses.autofillPromptDismissed.record(NoExtras())
//
//        Component.FEATURE_PROMPTS to LoginAutofillDialogFacts.Items.AUTOFILL_LOGIN_PERFORMED ->
//            Logins.autofilled.record(NoExtras())
//        Component.FEATURE_PROMPTS to LoginAutofillDialogFacts.Items.AUTOFILL_LOGIN_PROMPT_SHOWN ->
//            Logins.autofillPromptShown.record(NoExtras())
//        Component.FEATURE_PROMPTS to LoginAutofillDialogFacts.Items.AUTOFILL_LOGIN_PROMPT_DISMISSED ->
//            Logins.autofillPromptDismissed.record(NoExtras())
//
//        Component.FEATURE_AUTOFILL to AutofillFacts.Items.AUTOFILL_REQUEST -> {
//            val hasMatchingLogins =
//                metadata?.get(AutofillFacts.Metadata.HAS_MATCHING_LOGINS) as Boolean?
//            if (hasMatchingLogins == true) {
//                AndroidAutofill.requestMatchingLogins.record(NoExtras())
//            } else {
//                AndroidAutofill.requestNoMatchingLogins.record(NoExtras())
//            }
//        }
//        Component.FEATURE_AUTOFILL to AutofillFacts.Items.AUTOFILL_SEARCH -> {
//            if (action == Action.SELECT) {
//                AndroidAutofill.searchItemSelected.record(NoExtras())
//            } else {
//                AndroidAutofill.searchDisplayed.record(NoExtras())
//            }
//        }
//        Component.FEATURE_AUTOFILL to AutofillFacts.Items.AUTOFILL_CONFIRMATION -> {
//            if (action == Action.CONFIRM) {
//                AndroidAutofill.confirmSuccessful.record(NoExtras())
//            } else {
//                AndroidAutofill.confirmCancelled.record(NoExtras())
//            }
//        }
//        Component.FEATURE_AUTOFILL to AutofillFacts.Items.AUTOFILL_LOCK -> {
//            if (action == Action.CONFIRM) {
//                AndroidAutofill.unlockSuccessful.record(NoExtras())
//            } else {
//                AndroidAutofill.unlockCancelled.record(NoExtras())
//            }
//        }
//        Component.FEATURE_AUTOFILL to AutofillFacts.Items.AUTOFILL_LOGIN_PASSWORD_DETECTED -> {
//            Logins.passwordDetected.record(NoExtras())
//        }
//        Component.FEATURE_SYNCEDTABS to SyncedTabsFacts.Items.SYNCED_TABS_SUGGESTION_CLICKED -> {
//            SyncedTabs.syncedTabsSuggestionClicked.record(NoExtras())
//        }
//        Component.FEATURE_AWESOMEBAR to AwesomeBarFacts.Items.BOOKMARK_SUGGESTION_CLICKED -> {
//            Awesomebar.bookmarkSuggestionClicked.record(NoExtras())
//        }
//        Component.FEATURE_AWESOMEBAR to AwesomeBarFacts.Items.CLIPBOARD_SUGGESTION_CLICKED -> {
//            Awesomebar.clipboardSuggestionClicked.record(NoExtras())
//        }
//        Component.FEATURE_AWESOMEBAR to AwesomeBarFacts.Items.HISTORY_SUGGESTION_CLICKED -> {
//            Awesomebar.historySuggestionClicked.record(NoExtras())
//        }
//        Component.FEATURE_AWESOMEBAR to AwesomeBarFacts.Items.SEARCH_ACTION_CLICKED -> {
//            Awesomebar.searchActionClicked.record(NoExtras())
//        }
//        Component.FEATURE_AWESOMEBAR to AwesomeBarFacts.Items.SEARCH_SUGGESTION_CLICKED -> {
//            Awesomebar.searchSuggestionClicked.record(NoExtras())
//        }
//        Component.FEATURE_AWESOMEBAR to AwesomeBarFacts.Items.OPENED_TAB_SUGGESTION_CLICKED -> {
//            Awesomebar.openedTabSuggestionClicked.record(NoExtras())
//        }
//        Component.FEATURE_AWESOMEBAR to AwesomeBarFacts.Items.SEARCH_TERM_SUGGESTION_CLICKED -> {
//            Awesomebar.searchTermSuggestionClicked.record(NoExtras())
//        }
//        Component.FEATURE_CONTEXTMENU to ContextMenuFacts.Items.TEXT_SELECTION_OPTION -> {
//            when (metadata?.get("textSelectionOption")?.toString()) {
//                CONTEXT_MENU_COPY -> ContextualMenu.copyTapped.record(NoExtras())
//                CONTEXT_MENU_SEARCH,
//                CONTEXT_MENU_SEARCH_PRIVATELY,
//                -> ContextualMenu.searchTapped.record(NoExtras())
//                CONTEXT_MENU_SELECT_ALL -> ContextualMenu.selectAllTapped.record(NoExtras())
//                CONTEXT_MENU_SHARE -> ContextualMenu.shareTapped.record(NoExtras())
//                else -> Unit
//            }
//        }
//
//        Component.FEATURE_FXSUGGEST to FxSuggestFacts.Items.AMP_SUGGESTION_CLICKED,
//        Component.FEATURE_FXSUGGEST to FxSuggestFacts.Items.WIKIPEDIA_SUGGESTION_CLICKED,
//        -> {
//            val clickInfo = metadata?.get(FxSuggestFacts.MetadataKeys.INTERACTION_INFO)
//
//            // Record an event for this click in the `events` ping. These events include the `client_id`.
//            when (clickInfo) {
//                is FxSuggestInteractionInfo.Amp -> {
//                    Awesomebar.sponsoredSuggestionClicked.record(
//                        Awesomebar.SponsoredSuggestionClickedExtra(
//                            provider = "amp",
//                        ),
//                    )
//                }
//                is FxSuggestInteractionInfo.Wikipedia -> {
//                    Awesomebar.nonSponsoredSuggestionClicked.record(
//                        Awesomebar.NonSponsoredSuggestionClickedExtra(
//                            provider = "wikipedia",
//                        ),
//                    )
//                }
//            }
//
//            // Submit a separate `fx-suggest` ping for this click. These pings do not include the `client_id`.
//            FxSuggest.pingType.set("fxsuggest-click")
//            FxSuggest.isClicked.set(true)
//            (metadata?.get(FxSuggestFacts.MetadataKeys.POSITION) as? Long)?.let {
//                FxSuggest.position.set(it)
//            }
//            when (clickInfo) {
//                is FxSuggestInteractionInfo.Amp -> {
//                    FxSuggest.blockId.set(clickInfo.blockId)
//                    FxSuggest.advertiser.set(clickInfo.advertiser)
//                    FxSuggest.reportingUrl.set(clickInfo.reportingUrl)
//                    FxSuggest.iabCategory.set(clickInfo.iabCategory)
//                    FxSuggest.contextId.set(UUID.fromString(clickInfo.contextId))
//                }
//                is FxSuggestInteractionInfo.Wikipedia -> {
//                    FxSuggest.advertiser.set("wikipedia")
//                    FxSuggest.contextId.set(UUID.fromString(clickInfo.contextId))
//                }
//            }
//            Pings.fxSuggest.submit()
//        }
//
//        Component.FEATURE_FXSUGGEST to FxSuggestFacts.Items.AMP_SUGGESTION_IMPRESSED,
//        Component.FEATURE_FXSUGGEST to FxSuggestFacts.Items.WIKIPEDIA_SUGGESTION_IMPRESSED,
//        -> {
//            val impressionInfo = metadata?.get(FxSuggestFacts.MetadataKeys.INTERACTION_INFO)
//            val engagementAbandoned = metadata?.get(FxSuggestFacts.MetadataKeys.ENGAGEMENT_ABANDONED) as? Boolean
//                ?: false
//
//            // Record an event for this impression in the `events` ping. These events include the `client_id`, and
//            // we record them for engaged and abandoned search sessions.
//            when (impressionInfo) {
//                is FxSuggestInteractionInfo.Amp -> {
//                    Awesomebar.sponsoredSuggestionImpressed.record(
//                        Awesomebar.SponsoredSuggestionImpressedExtra(
//                            provider = "amp",
//                        ),
//                    )
//                }
//                is FxSuggestInteractionInfo.Wikipedia -> {
//                    Awesomebar.nonSponsoredSuggestionImpressed.record(
//                        Awesomebar.NonSponsoredSuggestionImpressedExtra(
//                            provider = "wikipedia",
//                        ),
//                    )
//                }
//            }
//
//            // Submit a separate `fx-suggest` ping for this impression. These pings do not include the `client_id`,
//            // and we submit them for engaged search sessions only.
//            if (!engagementAbandoned) {
//                FxSuggest.pingType.set("fxsuggest-impression")
//                (metadata?.get(FxSuggestFacts.MetadataKeys.IS_CLICKED) as? Boolean)?.let {
//                    FxSuggest.isClicked.set(it)
//                }
//                (metadata?.get(FxSuggestFacts.MetadataKeys.POSITION) as? Long)?.let {
//                    FxSuggest.position.set(it)
//                }
//                when (impressionInfo) {
//                    is FxSuggestInteractionInfo.Amp -> {
//                        FxSuggest.blockId.set(impressionInfo.blockId)
//                        FxSuggest.advertiser.set(impressionInfo.advertiser)
//                        FxSuggest.reportingUrl.set(impressionInfo.reportingUrl)
//                        FxSuggest.iabCategory.set(impressionInfo.iabCategory)
//                        FxSuggest.contextId.set(UUID.fromString(impressionInfo.contextId))
//                    }
//                    is FxSuggestInteractionInfo.Wikipedia -> {
//                        FxSuggest.advertiser.set("wikipedia")
//                        FxSuggest.contextId.set(UUID.fromString(impressionInfo.contextId))
//                    }
//                }
//                Pings.fxSuggest.submit()
//            }
//
//            Unit
//        }
//
//        Component.FEATURE_PWA to ProgressiveWebAppFacts.Items.HOMESCREEN_ICON_TAP -> {
//            ProgressiveWebApp.homescreenTap.record(NoExtras())
//        }
//        Component.FEATURE_PWA to ProgressiveWebAppFacts.Items.INSTALL_SHORTCUT -> {
//            ProgressiveWebApp.installTap.record(NoExtras())
//        }
//
//        Component.FEATURE_SEARCH to AdsTelemetry.SERP_ADD_CLICKED -> {
//            BrowserSearch.adClicks[value!!].add()
//            track(Event.GrowthData.SerpAdClicked)
//        }
//        Component.FEATURE_SEARCH to AdsTelemetry.SERP_SHOWN_WITH_ADDS -> {
//            BrowserSearch.withAds[value!!].add()
//        }
//        Component.FEATURE_SEARCH to InContentTelemetry.IN_CONTENT_SEARCH -> {
//            BrowserSearch.inContent[value!!].add()
//            track(Event.GrowthData.UserActivated(fromSearch = true))
//        }
//        Component.SUPPORT_WEBEXTENSIONS to WebExtensionFacts.Items.WEB_EXTENSIONS_INITIALIZED -> {
//            metadata?.get("installed")?.let { installedAddons ->
//                if (installedAddons is List<*>) {
//                    settings.installedAddonsCount = installedAddons.size
//                    settings.installedAddonsList = installedAddons.joinToString(",")
//                }
//            }
//
//            metadata?.get("enabled")?.let { enabledAddons ->
//                if (enabledAddons is List<*>) {
//                    settings.enabledAddonsCount = enabledAddons.size
//                    settings.enabledAddonsList = enabledAddons.joinToString(",")
//                }
//            }
//            Unit
//        }
//        Component.COMPOSE_AWESOMEBAR to ComposeAwesomeBarFacts.Items.PROVIDER_DURATION -> {
//            metadata?.get(ComposeAwesomeBarFacts.MetadataKeys.DURATION_PAIR)
//                ?.let { providerTiming ->
//                    require(providerTiming is Pair<*, *>) { "Expected providerTiming to be a Pair" }
//                    when (val provider = providerTiming.first as AwesomeBar.SuggestionProvider) {
//                        is HistoryStorageSuggestionProvider -> PerfAwesomebar.historySuggestions
//                        is BookmarksStorageSuggestionProvider -> PerfAwesomebar.bookmarkSuggestions
//                        is SessionSuggestionProvider -> PerfAwesomebar.sessionSuggestions
//                        is SearchSuggestionProvider -> PerfAwesomebar.searchEngineSuggestions
//                        is ClipboardSuggestionProvider -> PerfAwesomebar.clipboardSuggestions
//                        is ShortcutsSuggestionProvider -> PerfAwesomebar.shortcutsSuggestions
//                        // NB: add PerfAwesomebar.syncedTabsSuggestions once we're using SyncedTabsSuggestionProvider
//                        else -> {
//                            Logger("Metrics").error("Unknown suggestion provider: $provider")
//                            null
//                        }
//                    }?.accumulateSamples(listOf(providerTiming.second as Long))
//                }
//            Unit
//        }
//        Component.FEATURE_TOP_SITES to TopSitesFacts.Items.COUNT -> {
//            value?.let {
//                var count = 0
//                try {
//                    count = it.toInt()
//                } catch (e: NumberFormatException) {
//                    // Do nothing
//                }
//
//                settings.topSitesSize = count
//            }
//            Unit
//        }
//        Component.FEATURE_SITEPERMISSIONS to SitePermissionsFacts.Items.PERMISSIONS -> {
//            when (action) {
//                Action.DISPLAY -> SitePermissions.promptShown.record(
//                    SitePermissions.PromptShownExtra(
//                        value,
//                    ),
//                )
//                Action.CONFIRM ->
//                    SitePermissions.permissionsAllowed.record(
//                        SitePermissions.PermissionsAllowedExtra(
//                            value,
//                        ),
//                    )
//                Action.CANCEL ->
//                    SitePermissions.permissionsDenied.record(
//                        SitePermissions.PermissionsDeniedExtra(
//                            value,
//                        ),
//                    )
//                else -> {
//                    // no-op
//                }
//            }
//        }
//        Component.SERVICE_FIREFOX_ACCOUNTS to SyncFacts.Items.SYNC_FAILED -> {
//            Sync.failed.record(NoExtras())
//        }
//
//        else -> {
//            // no-op
//        }
//    }
//
//    override fun start(type: MetricServiceType) {
//        val isEnabled = isTelemetryEnabled(type)
//        val isInitialized = isInitialized(type)
//        if (!isEnabled || isInitialized) {
//            return
//        }
//
//        services
//            .filter { it.type == type }
//            .forEach { it.start() }
//
//        initialized.add(type)
//    }
//
//    override fun stop(type: MetricServiceType) {
//        val isEnabled = isTelemetryEnabled(type)
//        val isInitialized = isInitialized(type)
//        if (isEnabled || !isInitialized) {
//            return
//        }
//
//        services
//            .filter { it.type == type }
//            .forEach { it.stop() }
//
//        initialized.remove(type)
//    }
//
//    override fun track(event: Event) {
//        services
//            .filter { it.shouldTrack(event) }
//            .forEach {
//                val isEnabled = isTelemetryEnabled(it.type)
//                val isInitialized = isInitialized(it.type)
//                if (!isEnabled || !isInitialized) {
//                    return@forEach
//                }
//
//                it.track(event)
//            }
//    }
//
//    private fun isInitialized(type: MetricServiceType): Boolean = initialized.contains(type)
//
//    private fun isTelemetryEnabled(type: MetricServiceType): Boolean = when (type) {
//        MetricServiceType.Data -> isDataTelemetryEnabled()
//        MetricServiceType.Marketing -> isMarketingDataTelemetryEnabled()
//    }
//
//    companion object {
//        /**
//         * Text selection long press context items to be tracked.
//         */
//        const val CONTEXT_MENU_COPY = "org.mozilla.geckoview.COPY"
//        const val CONTEXT_MENU_SEARCH = "CUSTOM_CONTEXT_MENU_SEARCH"
//        const val CONTEXT_MENU_SEARCH_PRIVATELY = "CUSTOM_CONTEXT_MENU_SEARCH_PRIVATELY"
//        const val CONTEXT_MENU_SELECT_ALL = "org.mozilla.geckoview.SELECT_ALL"
//        const val CONTEXT_MENU_SHARE = "CUSTOM_CONTEXT_MENU_SHARE"
//
//        /**
//         * Non - Text selection long press context menu items to be tracked.
//         */
//        private val contextMenuAllowList = mapOf(
//            "mozac.feature.contextmenu.open_in_new_tab" to "open_in_new_tab",
//            "mozac.feature.contextmenu.open_in_private_tab" to "open_in_private_tab",
//            "mozac.feature.contextmenu.open_image_in_new_tab" to "open_image_in_new_tab",
//            "mozac.feature.contextmenu.save_image" to "save_image",
//            "mozac.feature.contextmenu.share_link" to "share_link",
//            "mozac.feature.contextmenu.copy_link" to "copy_link",
//            "mozac.feature.contextmenu.copy_image_location" to "copy_image_location",
//            "mozac.feature.contextmenu.share_image" to "share_image",
//        )
//    }
//}
