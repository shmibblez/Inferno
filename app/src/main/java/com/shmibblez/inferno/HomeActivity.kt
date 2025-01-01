/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package com.shmibblez.inferno

import android.annotation.SuppressLint
import android.app.assist.AssistContent
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.Intent.ACTION_MAIN
import android.content.Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
import android.content.res.Configuration
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.StrictMode
import android.text.TextUtils
import android.text.format.DateUtils
import android.util.AttributeSet
import android.view.ActionMode
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.WindowManager.LayoutParams.FLAG_SECURE
import androidx.annotation.CallSuper
import androidx.annotation.IdRes
import androidx.annotation.RequiresApi
import androidx.annotation.VisibleForTesting
import androidx.appcompat.app.ActionBar
import androidx.appcompat.widget.Toolbar
import androidx.compose.runtime.mutableStateOf
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.doOnAttach
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.NavigationUI
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import mozilla.appservices.places.BookmarkRoot
import mozilla.components.browser.state.action.MediaSessionAction
import mozilla.components.browser.state.action.SearchAction
import mozilla.components.browser.state.search.SearchEngine
import mozilla.components.browser.state.selector.getNormalOrPrivateTabs
import mozilla.components.browser.state.selector.selectedTab
import mozilla.components.browser.state.state.SessionState
import mozilla.components.browser.state.state.WebExtensionState
import mozilla.components.concept.engine.EngineSession
import mozilla.components.concept.engine.EngineView
import mozilla.components.concept.storage.HistoryMetadataKey
import mozilla.components.feature.contextmenu.DefaultSelectionActionDelegate
import mozilla.components.feature.customtabs.isCustomTabIntent
import mozilla.components.feature.media.ext.findActiveMediaTab
import mozilla.components.feature.privatemode.notification.PrivateNotificationFeature
import mozilla.components.feature.search.BrowserStoreSearchAdapter
import mozilla.components.service.fxa.sync.SyncReason
import mozilla.components.support.base.feature.ActivityResultHandler
import mozilla.components.support.base.feature.UserInteractionHandler
import mozilla.components.support.base.feature.UserInteractionOnBackPressedCallback
import mozilla.components.support.base.log.logger.Logger
import mozilla.components.support.ktx.android.arch.lifecycle.addObservers
import mozilla.components.support.ktx.android.content.call
import mozilla.components.support.ktx.android.content.email
import mozilla.components.support.ktx.android.content.share
import mozilla.components.support.ktx.kotlin.isUrl
import mozilla.components.support.ktx.kotlin.toNormalizedUrl
import mozilla.components.support.locale.LocaleAwareAppCompatActivity
import mozilla.components.support.utils.BootUtils
import mozilla.components.support.utils.BrowsersCache
import mozilla.components.support.utils.ManufacturerCodes
import mozilla.components.support.utils.SafeIntent
import mozilla.components.support.utils.toSafeIntent
import mozilla.components.support.webextensions.WebExtensionPopupObserver
import mozilla.telemetry.glean.private.NoExtras
import org.mozilla.experiments.nimbus.initializeTooling
import com.shmibblez.inferno.GleanMetrics.AppIcon
import com.shmibblez.inferno.GleanMetrics.Events
import com.shmibblez.inferno.GleanMetrics.Metrics
import com.shmibblez.inferno.GleanMetrics.NavigationBar
import com.shmibblez.inferno.GleanMetrics.SplashScreen
import com.shmibblez.inferno.GleanMetrics.StartOnHome
import com.shmibblez.inferno.addons.ExtensionsProcessDisabledBackgroundController
import com.shmibblez.inferno.addons.ExtensionsProcessDisabledForegroundController
import com.shmibblez.inferno.browser.browsingmode.BrowsingMode
import com.shmibblez.inferno.browser.browsingmode.BrowsingModeManager
import com.shmibblez.inferno.browser.browsingmode.DefaultBrowsingModeManager
import com.shmibblez.inferno.components.appstate.AppAction
import com.shmibblez.inferno.components.appstate.OrientationMode
import com.shmibblez.inferno.components.metrics.BreadcrumbsRecorder
import com.shmibblez.inferno.components.metrics.GrowthDataWorker
import com.shmibblez.inferno.components.metrics.fonts.FontEnumerationWorker
import com.shmibblez.inferno.crashes.CrashReporterBinding
import com.shmibblez.inferno.crashes.UnsubmittedCrashDialog
import com.shmibblez.inferno.customtabs.ExternalAppBrowserActivity
import com.shmibblez.inferno.databinding.ActivityHomeBinding
import com.shmibblez.inferno.debugsettings.data.DefaultDebugSettingsRepository
import com.shmibblez.inferno.debugsettings.ui.FenixOverlay
import com.shmibblez.inferno.experiments.ResearchSurfaceDialogFragment
import com.shmibblez.inferno.ext.alreadyOnDestination
import com.shmibblez.inferno.ext.breadcrumb
import com.shmibblez.inferno.ext.components
import com.shmibblez.inferno.ext.getBreadcrumbMessage
import com.shmibblez.inferno.ext.getIntentSessionId
import com.shmibblez.inferno.ext.getIntentSource
import com.shmibblez.inferno.ext.getNavDirections
import com.shmibblez.inferno.ext.hasTopDestination
import com.shmibblez.inferno.ext.nav
import com.shmibblez.inferno.ext.recordEventInNimbus
import com.shmibblez.inferno.ext.setNavigationIcon
import com.shmibblez.inferno.ext.settings
import com.shmibblez.inferno.ext.systemGesturesInsets
import com.shmibblez.inferno.extension.WebExtensionPromptFeature
import com.shmibblez.inferno.home.HomeFragment
import com.shmibblez.inferno.home.intent.AssistIntentProcessor
import com.shmibblez.inferno.home.intent.CrashReporterIntentProcessor
import com.shmibblez.inferno.home.intent.HomeDeepLinkIntentProcessor
import com.shmibblez.inferno.home.intent.OpenBrowserIntentProcessor
import com.shmibblez.inferno.home.intent.OpenPasswordManagerIntentProcessor
import com.shmibblez.inferno.home.intent.OpenRecentlyClosedIntentProcessor
import com.shmibblez.inferno.home.intent.OpenSpecificTabIntentProcessor
import com.shmibblez.inferno.home.intent.ReEngagementIntentProcessor
import com.shmibblez.inferno.home.intent.SpeechProcessingIntentProcessor
import com.shmibblez.inferno.home.intent.StartSearchIntentProcessor
import com.shmibblez.inferno.library.bookmarks.DesktopFolders
import com.shmibblez.inferno.messaging.FenixMessageSurfaceId
import com.shmibblez.inferno.messaging.MessageNotificationWorker
import com.shmibblez.inferno.nimbus.FxNimbus
import com.shmibblez.inferno.onboarding.ReEngagementNotificationWorker
import com.shmibblez.inferno.perf.MarkersActivityLifecycleCallbacks
import com.shmibblez.inferno.perf.MarkersFragmentLifecycleCallbacks
import com.shmibblez.inferno.perf.Performance
import com.shmibblez.inferno.perf.PerformanceInflater
import com.shmibblez.inferno.perf.ProfilerMarkers
import com.shmibblez.inferno.perf.StartupPathProvider
import com.shmibblez.inferno.perf.StartupTimeline
import com.shmibblez.inferno.perf.StartupTypeTelemetry
import com.shmibblez.inferno.session.PrivateNotificationService
import com.shmibblez.inferno.settings.SupportUtils
import com.shmibblez.inferno.shortcut.NewTabShortcutIntentProcessor.Companion.ACTION_OPEN_PRIVATE_TAB
import com.shmibblez.inferno.splashscreen.ApplyExperimentsOperation
import com.shmibblez.inferno.splashscreen.DefaultExperimentsOperationStorage
import com.shmibblez.inferno.splashscreen.DefaultSplashScreenStorage
import com.shmibblez.inferno.splashscreen.FetchExperimentsOperation
import com.shmibblez.inferno.splashscreen.SplashScreenManager
import com.shmibblez.inferno.tabhistory.TabHistoryDialogFragment
import com.shmibblez.inferno.tabstray.TabsTrayFragment
import com.shmibblez.inferno.theme.DefaultThemeManager
import com.shmibblez.inferno.theme.StatusBarColorManager
import com.shmibblez.inferno.theme.ThemeManager
import com.shmibblez.inferno.utils.Settings
import com.shmibblez.inferno.utils.changeAppLauncherIcon
import java.lang.ref.WeakReference
import java.util.Locale

/**
 * The main activity of the application. The application is primarily a single Activity (this one)
 * with fragments switching out to display different views. The most important views shown here are the:
 * - home screen
 * - browser screen
 */
@SuppressWarnings("TooManyFunctions", "LargeClass", "LongMethod")
open class HomeActivity : LocaleAwareAppCompatActivity(), NavHostActivity {
    @VisibleForTesting
    internal lateinit var binding: ActivityHomeBinding
    lateinit var themeManager: ThemeManager
    lateinit var browsingModeManager: BrowsingModeManager

    private var isVisuallyComplete = false

    var isMicrosurveyPromptDismissed = mutableStateOf(false)

    private var privateNotificationObserver: PrivateNotificationFeature<PrivateNotificationService>? =
        null

    private var isToolbarInflated = false

    private val webExtensionPopupObserver by lazy {
        WebExtensionPopupObserver(components.core.store, ::openPopup)
    }

    val webExtensionPromptFeature by lazy {
        WebExtensionPromptFeature(
            store = components.core.store,
            context = this@HomeActivity,
            fragmentManager = supportFragmentManager,
            onLinkClicked = { url, shouldOpenInBrowser ->
                if (shouldOpenInBrowser) {
                    openToBrowserAndLoad(
                        searchTermOrURL = url,
                        newTab = true,
                        from = BrowserDirection.FromGlobal,
                    )
                } else {
                    startActivity(
                        SupportUtils.createCustomTabIntent(
                            context = this,
                            url = url,
                        ),
                    )
                }
            },
        )
    }

    private val crashReporterBinding by lazy {
        CrashReporterBinding(
            store = components.appStore,
            onReporting = ::showCrashReporter,
        )
    }

    private val extensionsProcessDisabledForegroundController by lazy {
        ExtensionsProcessDisabledForegroundController(this@HomeActivity)
    }

    private val extensionsProcessDisabledBackgroundController by lazy {
        ExtensionsProcessDisabledBackgroundController(
            browserStore = components.core.store,
            appStore = components.appStore,
        )
    }

    private val serviceWorkerSupport by lazy {
        ServiceWorkerSupportFeature(this)
    }

    private var inflater: LayoutInflater? = null

    private val navHost by lazy {
        supportFragmentManager.findFragmentById(R.id.container) as NavHostFragment
    }

    private val externalSourceIntentProcessors by lazy {
        listOf(
            HomeDeepLinkIntentProcessor(this),
            SpeechProcessingIntentProcessor(this, components.core.store),
            AssistIntentProcessor(),
            StartSearchIntentProcessor(),
            OpenBrowserIntentProcessor(this, ::getIntentSessionId),
            OpenSpecificTabIntentProcessor(this),
            OpenPasswordManagerIntentProcessor(),
            OpenRecentlyClosedIntentProcessor(),
            ReEngagementIntentProcessor(this, settings()),
        )
    }

    // See onKeyDown for why this is necessary
    private var backLongPressJob: Job? = null

    private lateinit var navigationToolbar: Toolbar

    // Tracker for contextual menu (Copy|Search|Select all|etc...)
    private var actionMode: ActionMode? = null

    private val startupPathProvider = StartupPathProvider()
    private lateinit var startupTypeTelemetry: StartupTypeTelemetry

    private val onBackPressedCallback = UserInteractionOnBackPressedCallback(
        fragmentManager = supportFragmentManager,
        dispatcher = onBackPressedDispatcher,
    )

    @Suppress("ComplexMethod")
    final override fun onCreate(savedInstanceState: Bundle?) {
        // DO NOT MOVE ANYTHING ABOVE THIS getProfilerTime CALL.
        val startTimeProfiler = components.core.engine.profiler?.getProfilerTime()

        // Setup nimbus-cli tooling. This is a NOOP when launching normally.
        components.nimbus.sdk.initializeTooling(applicationContext, intent)
        components.strictMode.attachListenerToDisablePenaltyDeath(supportFragmentManager)
        MarkersFragmentLifecycleCallbacks.register(supportFragmentManager, components.core.engine)

        // There is disk read violations on some devices such as samsung and pixel for android 9/10
        components.strictMode.resetAfter(StrictMode.allowThreadDiskReads()) {
            // Browsing mode & theme setup should always be called before super.onCreate.
            setupBrowsingMode(getModeFromIntentOrLastKnown(intent))
            setupTheme()

            super.onCreate(savedInstanceState)
        }

        // Checks if Activity is currently in PiP mode if launched from external intents, then exits it
        checkAndExitPiP()

        // Diagnostic breadcrumb for "Display already aquired" crash:
        // https://github.com/mozilla-mobile/android-components/issues/7960
        breadcrumb(
            message = "onCreate()",
            data = mapOf(
                "recreated" to (savedInstanceState != null).toString(),
                "intent" to (intent?.action ?: "null"),
            ),
        )

        components.publicSuffixList.prefetch()

        // Changing a language on the Language screen restarts the activity, but the activity keeps
        // the old layout direction. We have to update the direction manually.
        window.decorView.layoutDirection = TextUtils.getLayoutDirectionFromLocale(Locale.getDefault())

        binding = ActivityHomeBinding.inflate(layoutInflater)

        val shouldShowOnboarding = settings().shouldShowOnboarding(
            hasUserBeenOnboarded = components.fenixOnboarding.userHasBeenOnboarded(),
            isLauncherIntent = intent.toSafeIntent().isLauncherIntent,
        )

        SplashScreenManager(
            splashScreenOperation = if (FxNimbus.features.splashScreen.value().offTrainOnboarding) {
                ApplyExperimentsOperation(
                    storage = DefaultExperimentsOperationStorage(components.settings),
                    nimbus = components.nimbus.sdk,
                )
            } else {
                FetchExperimentsOperation(
                    storage = DefaultExperimentsOperationStorage(components.settings),
                    nimbus = components.nimbus.sdk,
                )
            },
            splashScreenTimeout = FxNimbus.features.splashScreen.value().maximumDurationMs.toLong(),
            isDeviceSupported = { Build.VERSION.SDK_INT > Build.VERSION_CODES.M },
            storage = DefaultSplashScreenStorage(components.settings),
            showSplashScreen = { installSplashScreen().setKeepOnScreenCondition(it) },
            onSplashScreenFinished = { result ->
                if (result.sendTelemetry) {
                    SplashScreen.firstLaunchExtended.record(
                        SplashScreen.FirstLaunchExtendedExtra(dataFetched = result.wasDataFetched),
                    )
                }

                if (savedInstanceState == null && shouldShowOnboarding) {
                    navHost.navController.navigate(NavGraphDirections.actionGlobalOnboarding())
                }
            },
        ).showSplashScreen()

        lifecycleScope.launch {
            val debugSettingsRepository = DefaultDebugSettingsRepository(
                context = this@HomeActivity,
                writeScope = this,
            )

            debugSettingsRepository.debugDrawerEnabled
                .distinctUntilChanged()
                .collect { enabled ->
                    with(binding.debugOverlay) {
                        if (enabled) {
                            visibility = View.VISIBLE

                            setContent {
                                FenixOverlay(
                                    browserStore = components.core.store,
                                    inactiveTabsEnabled = settings().inactiveTabsAreEnabled,
                                    loginsStorage = components.core.passwordsStorage,
                                )
                            }
                        } else {
                            setContent {}

                            visibility = View.GONE
                        }
                    }
                }
        }

        setContentView(binding.root)
        ProfilerMarkers.addListenerForOnGlobalLayout(components.core.engine, this, binding.root)

        // Must be after we set the content view
        if (isVisuallyComplete) {
            components.performance.visualCompletenessQueue
                .attachViewToRunVisualCompletenessQueueLater(WeakReference(binding.rootContainer))
        }

        privateNotificationObserver = PrivateNotificationFeature(
            applicationContext,
            components.core.store,
            PrivateNotificationService::class,
        ).also {
            it.start()
        }

        if (!shouldShowOnboarding) {
            lifecycleScope.launch(IO) {
                showFullscreenMessageIfNeeded(applicationContext)
            }

            // Unless the activity is recreated, navigate to home first (without rendering it)
            // to add it to the back stack.
            if (savedInstanceState == null) {
                navigateToHome(navHost.navController)
            }

            if (!shouldStartOnHome() && shouldNavigateToBrowserOnColdStart(savedInstanceState)) {
                navigateToBrowserOnColdStart()
            } else {
                StartOnHome.enterHomeScreen.record(NoExtras())
            }

            if (settings().showHomeOnboardingDialog && components.fenixOnboarding.userHasBeenOnboarded()) {
                navHost.navController.navigate(NavGraphDirections.actionGlobalHomeOnboardingDialog())
            }
        }

        Performance.processIntentIfPerformanceTest(intent, this)

        if (settings().isTelemetryEnabled) {
            lifecycle.addObserver(
                BreadcrumbsRecorder(
                    components.analytics.crashReporter,
                    navHost.navController,
                    ::getBreadcrumbMessage,
                ),
            )

            val safeIntent = intent?.toSafeIntent()
            safeIntent
                ?.let(::getIntentSource)
                ?.also { source ->
                    Events.appOpened.record(
                        Events.AppOpenedExtra(
                            source = source,
                        ),
                    )
                    // This will record an event in Nimbus' internal event store. Used for behavioral targeting
                    recordEventInNimbus("app_opened")

                    if (safeIntent.action.equals(ACTION_OPEN_PRIVATE_TAB) && source == APP_ICON) {
                        AppIcon.newPrivateTabTapped.record(NoExtras())
                    }
                }
        }
        supportActionBar?.hide()

        lifecycle.addObservers(
            webExtensionPopupObserver,
            extensionsProcessDisabledForegroundController,
            extensionsProcessDisabledBackgroundController,
            serviceWorkerSupport,
            crashReporterBinding,
        )

        if (!isCustomTabIntent(intent)) {
            lifecycle.addObserver(webExtensionPromptFeature)
        }

        if (shouldAddToRecentsScreen(intent)) {
            intent.removeExtra(START_IN_RECENTS_SCREEN)
            moveTaskToBack(true)
        }

        captureSnapshotTelemetryMetrics()

        startupTelemetryOnCreateCalled(intent.toSafeIntent())
        startupPathProvider.attachOnActivityOnCreate(lifecycle, intent)
        startupTypeTelemetry = StartupTypeTelemetry(components.startupStateProvider, startupPathProvider).apply {
            attachOnHomeActivityOnCreate(lifecycle)
        }

        components.core.requestInterceptor.setNavigationController(navHost.navController)

        supportFragmentManager.registerFragmentLifecycleCallbacks(
            StatusBarColorManager(themeManager, this),
            true,
        )

        if (settings().showContileFeature) {
            components.core.contileTopSitesUpdater.startPeriodicWork()
        }

        if (!settings().hiddenEnginesRestored) {
            settings().hiddenEnginesRestored = true
            components.useCases.searchUseCases.restoreHiddenSearchEngines.invoke()
        }

        // To assess whether the Pocket stories are to be downloaded or not multiple SharedPreferences
        // are read possibly needing to load them on the current thread. Move that to a background thread.
        lifecycleScope.launch(IO) {
            if (settings().showPocketRecommendationsFeature) {
                components.core.pocketStoriesService.startPeriodicStoriesRefresh()
            }

            if (settings().showPocketSponsoredStories) {
                components.core.pocketStoriesService.startPeriodicSponsoredStoriesRefresh()
                // If the secret setting for sponsored stories parameters is set,
                // force refresh the sponsored Pocket stories.
                if (settings().useCustomConfigurationForSponsoredStories) {
                    components.core.pocketStoriesService.refreshSponsoredStories()
                }
            }

            if (settings().showContentRecommendations) {
                components.core.pocketStoriesService.startPeriodicContentRecommendationsRefresh()
            }
        }

        components.backgroundServices.accountManagerAvailableQueue.runIfReadyOrQueue {
            lifecycleScope.launch(IO) {
                // If we're authenticated, kick-off a sync and a device state refresh.
                components.backgroundServices.accountManager.authenticatedAccount()?.let {
                    components.backgroundServices.accountManager.syncNow(reason = SyncReason.Startup)
                }
            }
        }

        components.core.engine.profiler?.addMarker(
            MarkersActivityLifecycleCallbacks.MARKER_NAME,
            startTimeProfiler,
            "HomeActivity.onCreate",
        )

        components.notificationsDelegate.bindToActivity(this)

        components.settings.coldStartsBetweenSetAsDefaultPrompts++

        components.appStore.dispatch(
            AppAction.OrientationChange(
                orientation = OrientationMode.fromInteger(resources.configuration.orientation),
            ),
        )

        onBackPressedDispatcher.addCallback(
            owner = this,
            onBackPressedCallback = onBackPressedCallback,
        )

        StartupTimeline.onActivityCreateEndHome(this) // DO NOT MOVE ANYTHING BELOW HERE.
    }

    private fun checkAndExitPiP() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && isInPictureInPictureMode && intent != null) {
            // Exit PiP mode
            moveTaskToBack(false)
            startActivity(Intent(this, this::class.java).setFlags(FLAG_ACTIVITY_REORDER_TO_FRONT))
        }
    }

    private fun startupTelemetryOnCreateCalled(safeIntent: SafeIntent) {
        // We intentionally only record this in HomeActivity and not ExternalBrowserActivity (e.g.
        // PWAs) so we don't include more unpredictable code paths in the results.
        components.performance.coldStartupDurationTelemetry.onHomeActivityOnCreate(
            components.performance.visualCompletenessQueue,
            components.startupStateProvider,
            safeIntent,
            binding.rootContainer,
        )
    }

    @CallSuper
    @Suppress("TooGenericExceptionCaught")
    override fun onResume() {
        super.onResume()

        // Diagnostic breadcrumb for "Display already aquired" crash:
        // https://github.com/mozilla-mobile/android-components/issues/7960
        breadcrumb(
            message = "onResume()",
        )

        lifecycleScope.launch(IO) {
            try {
                if (settings().showContileFeature) {
                    components.core.contileTopSitesProvider.refreshTopSitesIfCacheExpired()
                }
            } catch (e: Exception) {
                Logger.error("Failed to refresh contile top sites", e)
            }

            if (settings().checkIfFenixIsDefaultBrowserOnAppResume()) {
                if (components.appStore.state.wasNativeDefaultBrowserPromptShown) {
                    Metrics.defaultBrowserChangedViaNativeSystemPrompt.record(NoExtras())
                }
                Events.defaultBrowserChanged.record(NoExtras())
            }

            collectOSNavigationTelemetry()
            GrowthDataWorker.sendActivatedSignalIfNeeded(applicationContext)
            FontEnumerationWorker.sendActivatedSignalIfNeeded(applicationContext)
            ReEngagementNotificationWorker.setReEngagementNotificationIfNeeded(applicationContext)
            MessageNotificationWorker.setMessageNotificationWorker(applicationContext)
        }

        onBackPressedCallback.isEnabled = true

        // This was done in order to refresh search engines when app is running in background
        // and the user changes the system language
        // More details here: https://github.com/mozilla-mobile/fenix/pull/27793#discussion_r1029892536
        components.core.store.dispatch(SearchAction.RefreshSearchEnginesAction)
    }

    final override fun onStart() {
        // DO NOT MOVE ANYTHING ABOVE THIS getProfilerTime CALL.
        val startProfilerTime = components.core.engine.profiler?.getProfilerTime()

        super.onStart()

        // Diagnostic breadcrumb for "Display already aquired" crash:
        // https://github.com/mozilla-mobile/android-components/issues/7960
        breadcrumb(
            message = "onStart()",
        )

        ProfilerMarkers.homeActivityOnStart(binding.rootContainer, components.core.engine.profiler)
        components.core.engine.profiler?.addMarker(
            MarkersActivityLifecycleCallbacks.MARKER_NAME,
            startProfilerTime,
            "HomeActivity.onStart",
        ) // DO NOT MOVE ANYTHING BELOW THIS addMarker CALL.
    }

    final override fun onStop() {
        super.onStop()

        // Diagnostic breadcrumb for "Display already aquired" crash:
        // https://github.com/mozilla-mobile/android-components/issues/7960
        breadcrumb(
            message = "onStop()",
            data = mapOf(
                "finishing" to isFinishing.toString(),
            ),
        )

        if (FxNimbus.features.alternativeAppLauncherIcon.value().enabled) {
            // User has been enrolled in alternative app icon experiment.
            with(applicationContext) {
                changeAppLauncherIcon(
                    context = this,
                    appAlias = ComponentName(this, "$packageName.App"),
                    alternativeAppAlias = ComponentName(this, "$packageName.AlternativeApp"),
                    resetToDefault = FxNimbus.features.alternativeAppLauncherIcon.value().resetToDefault,
                )
            }
        }
    }

    final override fun onPause() {
        // We should return to the browser if there were normal tabs when we left the app
        settings().shouldReturnToBrowser =
            components.core.store.state.getNormalOrPrivateTabs(private = false).isNotEmpty()

        lifecycleScope.launch(IO) {
            val desktopFolders = DesktopFolders(
                applicationContext,
                showMobileRoot = false,
            )
            settings().desktopBookmarksSize = desktopFolders.count()

            settings().mobileBookmarksSize = components.core.bookmarksStorage.countBookmarksInTrees(
                listOf(BookmarkRoot.Mobile.id),
            ).toInt()
        }

        super.onPause()

        // Diagnostic breadcrumb for "Display already aquired" crash:
        // https://github.com/mozilla-mobile/android-components/issues/7960
        breadcrumb(
            message = "onPause()",
            data = mapOf(
                "finishing" to isFinishing.toString(),
            ),
        )

        // Every time the application goes into the background, it is possible that the user
        // is about to change the browsers installed on their system. Therefore, we reset the cache of
        // all the installed browsers.
        //
        // NB: There are ways for the user to install new products without leaving the browser.
        BrowsersCache.resetAll()
    }

    @RequiresApi(Build.VERSION_CODES.M)
    override fun onProvideAssistContent(outContent: AssistContent?) {
        super.onProvideAssistContent(outContent)
        val currentTabUrl = components.core.store.state.selectedTab?.content?.url
        outContent?.webUri = currentTabUrl?.let { Uri.parse(it) }
    }

    @CallSuper
    override fun onDestroy() {
        super.onDestroy()

        // Diagnostic breadcrumb for "Display already aquired" crash:
        // https://github.com/mozilla-mobile/android-components/issues/7960
        breadcrumb(
            message = "onDestroy()",
            data = mapOf(
                "finishing" to isFinishing.toString(),
            ),
        )

        components.core.contileTopSitesUpdater.stopPeriodicWork()
        components.core.pocketStoriesService.stopPeriodicStoriesRefresh()
        components.core.pocketStoriesService.stopPeriodicSponsoredStoriesRefresh()
        components.core.pocketStoriesService.stopPeriodicContentRecommendationsRefresh()
        privateNotificationObserver?.stop()
        components.notificationsDelegate.unBindActivity(this)

        val activityStartedWithLink = startupPathProvider.startupPathForActivity == StartupPathProvider.StartupPath.VIEW
        if (this !is ExternalAppBrowserActivity && !activityStartedWithLink) {
            stopMediaSession()
        }
    }

    final override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)

        // Diagnostic breadcrumb for "Display already aquired" crash:
        // https://github.com/mozilla-mobile/android-components/issues/7960
        breadcrumb(
            message = "onConfigurationChanged()",
        )

        components.appStore.dispatch(
            AppAction.OrientationChange(
                orientation = OrientationMode.fromInteger(newConfig.orientation),
            ),
        )
    }

    final override fun recreate() {
        // Diagnostic breadcrumb for "Display already aquired" crash:
        // https://github.com/mozilla-mobile/android-components/issues/7960
        breadcrumb(
            message = "recreate()",
        )

        super.recreate()
    }

    /**
     * Handles intents received when the activity is open.
     */
    final override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleNewIntent(intent)
        startupPathProvider.onIntentReceived(intent)
    }

    @VisibleForTesting
    internal fun handleNewIntent(intent: Intent) {
        if (this is ExternalAppBrowserActivity) {
            return
        }

        // Diagnostic breadcrumb for "Display already aquired" crash:
        // https://github.com/mozilla-mobile/android-components/issues/7960
        breadcrumb(
            message = "onNewIntent()",
            data = mapOf(
                "intent" to intent.action.toString(),
            ),
        )

        val tab = components.core.store.state.findActiveMediaTab()
        if (tab != null) {
            components.useCases.sessionUseCases.exitFullscreen(tab.id)
        }

        val intentProcessors =
            listOf(
                CrashReporterIntentProcessor(components.appStore),
            ) + externalSourceIntentProcessors
        val intentHandled =
            intentProcessors.any { it.process(intent, navHost.navController, this.intent) }
        browsingModeManager.mode = getModeFromIntentOrLastKnown(intent)

        if (intentHandled) {
            supportFragmentManager
                .primaryNavigationFragment
                ?.childFragmentManager
                ?.fragments
                ?.lastOrNull()
                ?.let { it as? TabsTrayFragment }
                ?.also { it.dismissAllowingStateLoss() }
        }
    }

    /**
     * Overrides view inflation to inject a custom [EngineView] from [components].
     */
    final override fun onCreateView(
        parent: View?,
        name: String,
        context: Context,
        attrs: AttributeSet,
    ): View? = when (name) {
        EngineView::class.java.name -> components.core.engine.createView(context, attrs).apply {
            selectionActionDelegate = DefaultSelectionActionDelegate(
                BrowserStoreSearchAdapter(
                    components.core.store,
                    tabId = getIntentSessionId(intent.toSafeIntent()),
                ),
                resources = context.resources,
                shareTextClicked = { share(it) },
                emailTextClicked = { email(it) },
                callTextClicked = { call(it) },
                actionSorter = ::actionSorter,
            )
        }.asView()
        else -> super.onCreateView(parent, name, context, attrs)
    }

    final override fun onActionModeStarted(mode: ActionMode?) {
        actionMode = mode
        super.onActionModeStarted(mode)
    }

    final override fun onActionModeFinished(mode: ActionMode?) {
        actionMode = null
        super.onActionModeFinished(mode)
    }

    fun finishActionMode() {
        actionMode?.finish().also { actionMode = null }
    }

    @Suppress("MagicNumber")
    // Defining the positions as constants doesn't seem super useful here.
    private fun actionSorter(actions: Array<String>): Array<String> {
        val order = hashMapOf<String, Int>()

        order["CUSTOM_CONTEXT_MENU_EMAIL"] = 0
        order["CUSTOM_CONTEXT_MENU_CALL"] = 1
        order["org.mozilla.geckoview.COPY"] = 2
        order["CUSTOM_CONTEXT_MENU_SEARCH"] = 3
        order["CUSTOM_CONTEXT_MENU_SEARCH_PRIVATELY"] = 4
        order["org.mozilla.geckoview.PASTE"] = 5
        order["org.mozilla.geckoview.SELECT_ALL"] = 6
        order["CUSTOM_CONTEXT_MENU_SHARE"] = 7

        return actions.sortedBy { actionName ->
            // Sort the actions in our preferred order, putting "other" actions unsorted at the end
            order[actionName] ?: actions.size
        }.toTypedArray()
    }

    @Deprecated("Deprecated in Java")
    // https://github.com/mozilla-mobile/fenix/issues/19919
    final override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        supportFragmentManager.primaryNavigationFragment?.childFragmentManager?.fragments?.forEach {
            if (it is ActivityResultHandler && it.onActivityResult(requestCode, data, resultCode)) {
                return
            }
        }
        @Suppress("DEPRECATION")
        super.onActivityResult(requestCode, resultCode, data)
    }

    private fun shouldUseCustomBackLongPress(): Boolean {
        val isAndroidN =
            Build.VERSION.SDK_INT == Build.VERSION_CODES.N || Build.VERSION.SDK_INT == Build.VERSION_CODES.N_MR1
        // Huawei devices seem to have problems with onKeyLongPress
        // See https://github.com/mozilla-mobile/fenix/issues/13498
        return isAndroidN || ManufacturerCodes.isHuawei
    }

    private fun handleBackLongPress(): Boolean {
        supportFragmentManager.primaryNavigationFragment?.childFragmentManager?.fragments?.forEach {
            if (it is OnLongPressedListener && it.onBackLongPressed()) {
                return true
            }
        }
        return false
    }

    private fun handleForwardLongPress(): Boolean {
        supportFragmentManager.primaryNavigationFragment?.childFragmentManager?.fragments?.forEach {
            if (it is OnLongPressedListener && it.onForwardLongPressed()) {
                return true
            }
        }
        return false
    }

    final override fun dispatchTouchEvent(ev: MotionEvent?): Boolean {
        ProfilerMarkers.addForDispatchTouchEvent(components.core.engine.profiler, ev)
        return super.dispatchTouchEvent(ev)
    }

    final override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        // Inspired by https://searchfox.org/mozilla-esr68/source/mobile/android/base/java/org/mozilla/gecko/BrowserApp.java#584-613
        // Android N and Huawei devices have broken onKeyLongPress events for the back button, so we
        // instead implement the long press behavior ourselves
        // - For short presses, we cancel the callback in onKeyUp
        // - For long presses, the normal keypress is marked as cancelled, hence won't be handled elsewhere
        //   (but Android still provides the haptic feedback), and the long press action is run
        if (shouldUseCustomBackLongPress() && keyCode == KeyEvent.KEYCODE_BACK) {
            backLongPressJob = lifecycleScope.launch {
                delay(ViewConfiguration.getLongPressTimeout().toLong())
                handleBackLongPress()
            }
        }

        if (keyCode == KeyEvent.KEYCODE_FORWARD) {
            event?.startTracking()
            return true
        }

        return super.onKeyDown(keyCode, event)
    }

    @Suppress("ReturnCount")
    final override fun onKeyUp(keyCode: Int, event: KeyEvent): Boolean {
        if (shouldUseCustomBackLongPress() && keyCode == KeyEvent.KEYCODE_BACK) {
            backLongPressJob?.cancel()

            // check if the key has been pressed for longer than the time needed for a press to turn into a long press
            // and if tab history is already visible we do not want to dismiss it.
            if (event.eventTime - event.downTime >= ViewConfiguration.getLongPressTimeout() &&
                navHost.navController.hasTopDestination(TabHistoryDialogFragment.NAME)
            ) {
                // returning true avoids further processing of the KeyUp event and avoids dismissing tab history.
                return true
            }
        }

        if (keyCode == KeyEvent.KEYCODE_FORWARD) {
            if (navHost.navController.hasTopDestination(TabHistoryDialogFragment.NAME)) {
                // returning true avoids further processing of the KeyUp event
                return true
            }

            supportFragmentManager.primaryNavigationFragment?.childFragmentManager?.fragments?.forEach {
                if (it is UserInteractionHandler && it.onForwardPressed()) {
                    return true
                }
            }
        }

        return super.onKeyUp(keyCode, event)
    }

    final override fun onKeyLongPress(keyCode: Int, event: KeyEvent?): Boolean {
        // onKeyLongPress is broken in Android N so we don't handle back button long presses here
        // for N. The version check ensures we don't handle back button long presses twice.
        if (!shouldUseCustomBackLongPress() && keyCode == KeyEvent.KEYCODE_BACK) {
            return handleBackLongPress()
        }

        if (keyCode == KeyEvent.KEYCODE_FORWARD) {
            return handleForwardLongPress()
        }

        return super.onKeyLongPress(keyCode, event)
    }

    final override fun onUserLeaveHint() {
        // The notification permission prompt will trigger onUserLeaveHint too.
        // We shouldn't treat this situation as user leaving.
        if (!components.notificationsDelegate.isRequestingPermission) {
            supportFragmentManager.primaryNavigationFragment?.childFragmentManager?.fragments?.forEach {
                if (it is UserInteractionHandler && it.onHomePressed()) {
                    return
                }
            }
        }

        super.onUserLeaveHint()
    }

    /**
     * External sources such as 3rd party links and shortcuts use this function to enter
     * private mode directly before the content view is created. Returns the mode set by the intent
     * otherwise falls back to the last known mode.
     */
    @VisibleForTesting
    internal fun getModeFromIntentOrLastKnown(intent: Intent?): BrowsingMode {
        intent?.toSafeIntent()?.let {
            if (it.hasExtra(PRIVATE_BROWSING_MODE)) {
                val startPrivateMode = it.getBooleanExtra(PRIVATE_BROWSING_MODE, false)
                return BrowsingMode.fromBoolean(isPrivate = startPrivateMode)
            }
        }
        return settings().lastKnownMode
    }

    /**
     * Determines whether the activity should be pushed to be backstack (i.e., 'minimized' to the recents
     * screen) upon starting.
     * @param intent - The intent that started this activity. Is checked for having the 'START_IN_RECENTS_SCREEN'-extra.
     * @return true if the activity should be started and pushed to the recents screen, false otherwise.
     */
    private fun shouldAddToRecentsScreen(intent: Intent?): Boolean {
        intent?.toSafeIntent()?.let {
            return it.getBooleanExtra(START_IN_RECENTS_SCREEN, false)
        }
        return false
    }

    private fun setupBrowsingMode(mode: BrowsingMode) {
        settings().lastKnownMode = mode
        browsingModeManager = createBrowsingModeManager(mode)
    }

    private fun setupTheme() {
        themeManager = createThemeManager()
        // ExternalAppBrowserActivity exclusively handles it's own theming unless in private mode.
        if (this !is ExternalAppBrowserActivity || browsingModeManager.mode.isPrivate) {
            themeManager.setActivityTheme(this)
            themeManager.applyStatusBarTheme(this)
        }
    }

    // Stop active media when activity is destroyed.
    private fun stopMediaSession() {
        if (isFinishing) {
            components.core.store.state.tabs.forEach {
                it.mediaSessionState?.controller?.stop()
            }

            components.core.store.state.findActiveMediaTab()?.let {
                components.core.store.dispatch(
                    MediaSessionAction.DeactivatedMediaSessionAction(
                        it.id,
                    ),
                )
            }
        }
    }

    /**
     * Returns the [supportActionBar], inflating it if necessary.
     * Everyone should call this instead of supportActionBar.
     */
    final override fun getSupportActionBarAndInflateIfNecessary(): ActionBar {
        if (!isToolbarInflated) {
            navigationToolbar = binding.navigationToolbarStub.inflate() as Toolbar

            setSupportActionBar(navigationToolbar)
            // Add ids to this that we don't want to have a toolbar back button
            setupNavigationToolbar()
            setNavigationIcon(R.drawable.ic_back_button)

            isToolbarInflated = true
        }
        return supportActionBar!!
    }

    @Suppress("SpreadOperator")
    private fun setupNavigationToolbar(vararg topLevelDestinationIds: Int) {
        NavigationUI.setupWithNavController(
            navigationToolbar,
            navHost.navController,
            AppBarConfiguration.Builder(*topLevelDestinationIds).build(),
        )

        navigationToolbar.setNavigationOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }
    }

    /**
     * Navigates to the browser fragment and loads a URL or performs a search (depending on the
     * value of [searchTermOrURL]).
     *
     * @param searchTermOrURL The entered search term to search or URL to be loaded.
     * @param newTab Whether or not to load the URL in a new tab.
     * @param from The [BrowserDirection] to indicate which fragment the browser is being
     * opened from.
     * @param customTabSessionId Optional custom tab session ID if navigating from a custom tab.
     * @param engine Optional [SearchEngine] to use when performing a search.
     * @param forceSearch Whether or not to force performing a search.
     * @param flags Flags that will be used when loading the URL (not applied to searches).
     * @param historyMetadata The [HistoryMetadataKey] of the new tab in case this tab
     * was opened from history.
     * @param additionalHeaders The extra headers to use when loading the URL.
     */
    fun openToBrowserAndLoad(
        searchTermOrURL: String,
        newTab: Boolean,
        from: BrowserDirection,
        customTabSessionId: String? = null,
        engine: SearchEngine? = null,
        forceSearch: Boolean = false,
        flags: EngineSession.LoadUrlFlags = EngineSession.LoadUrlFlags.none(),
        historyMetadata: HistoryMetadataKey? = null,
        additionalHeaders: Map<String, String>? = null,
    ) {
        openToBrowser(from, customTabSessionId)
        load(
            searchTermOrURL = searchTermOrURL,
            newTab = newTab,
            engine = engine,
            forceSearch = forceSearch,
            flags = flags,
            historyMetadata = historyMetadata,
            additionalHeaders = additionalHeaders,
        )
    }

    fun openToBrowser(from: BrowserDirection, customTabSessionId: String? = null) {
        if (navHost.navController.alreadyOnDestination(R.id.browserFragment)) return
        @IdRes val fragmentId = if (from.fragmentId != 0) from.fragmentId else null
        val directions = getNavDirections(from, customTabSessionId)
        if (directions != null) {
            navHost.navController.nav(fragmentId, directions)
        }
    }

    /**
     * Loads a URL or performs a search (depending on the value of [searchTermOrURL]).
     *
     * @param searchTermOrURL The entered search term to search or URL to be loaded.
     * @param newTab Whether or not to load the URL in a new tab.
     * @param engine Optional [SearchEngine] to use when performing a search.
     * @param forceSearch Whether or not to force performing a search.
     * @param flags Flags that will be used when loading the URL (not applied to searches).
     * @param historyMetadata The [HistoryMetadataKey] of the new tab in case this tab
     * was opened from history.
     * @param additionalHeaders The extra headers to use when loading the URL.
     */
    private fun load(
        searchTermOrURL: String,
        newTab: Boolean,
        engine: SearchEngine?,
        forceSearch: Boolean,
        flags: EngineSession.LoadUrlFlags = EngineSession.LoadUrlFlags.none(),
        historyMetadata: HistoryMetadataKey? = null,
        additionalHeaders: Map<String, String>? = null,
    ) {
        val startTime = components.core.engine.profiler?.getProfilerTime()
        val mode = browsingModeManager.mode

        val private = when (mode) {
            BrowsingMode.Private -> true
            BrowsingMode.Normal -> false
        }

        // In situations where we want to perform a search but have no search engine (e.g. the user
        // has removed all of them, or we couldn't load any) we will pass searchTermOrURL to Gecko
        // and let it try to load whatever was entered.
        if ((!forceSearch && searchTermOrURL.isUrl()) || engine == null) {
            if (newTab) {
                components.useCases.tabsUseCases.addTab(
                    url = searchTermOrURL.toNormalizedUrl(),
                    flags = flags,
                    private = private,
                    historyMetadata = historyMetadata,
                )
            } else {
                components.useCases.sessionUseCases.loadUrl(
                    url = searchTermOrURL.toNormalizedUrl(),
                    flags = flags,
                )
            }
        } else {
            if (newTab) {
                val searchUseCase = if (mode.isPrivate) {
                    components.useCases.searchUseCases.newPrivateTabSearch
                } else {
                    components.useCases.searchUseCases.newTabSearch
                }
                searchUseCase.invoke(
                    searchTerms = searchTermOrURL,
                    source = SessionState.Source.Internal.UserEntered,
                    selected = true,
                    searchEngine = engine,
                    flags = flags,
                    additionalHeaders = additionalHeaders,
                )
            } else {
                components.useCases.searchUseCases.defaultSearch.invoke(
                    searchTerms = searchTermOrURL,
                    searchEngine = engine,
                    flags = flags,
                    additionalHeaders = additionalHeaders,
                )
            }
        }

        if (components.core.engine.profiler?.isProfilerActive() == true) {
            // Wrapping the `addMarker` method with `isProfilerActive` even though it's no-op when
            // profiler is not active. That way, `text` argument will not create a string builder all the time.
            components.core.engine.profiler?.addMarker(
                "HomeActivity.load",
                startTime,
                "newTab: $newTab",
            )
        }
    }

    @VisibleForTesting
    internal fun navigateToBrowserOnColdStart() {
        if (this is ExternalAppBrowserActivity) {
            return
        }

        // Normal tabs + cold start -> Should go back to browser if we had any tabs open when we left last
        // except for PBM + Cold Start there won't be any tabs since they're evicted so we never will navigate
        if (settings().shouldReturnToBrowser && !browsingModeManager.mode.isPrivate) {
            // Navigate to home first (without rendering it) to add it to the back stack.
            openToBrowser(BrowserDirection.FromGlobal, null)
        }
    }

    @VisibleForTesting
    internal fun navigateToHome(navController: NavController) {
        if (this is ExternalAppBrowserActivity) {
            return
        }

        navController.navigate(NavGraphDirections.actionStartupHome())
    }

    final override fun attachBaseContext(base: Context) {
        base.components.strictMode.resetAfter(StrictMode.allowThreadDiskReads()) {
            super.attachBaseContext(base)
        }
    }

    final override fun getSystemService(name: String): Any? {
        // Issue #17759 had a crash with the PerformanceInflater.kt on Android 5.0 and 5.1
        // when using the TimePicker. Since the inflater was created for performance monitoring
        // purposes and that we test on new android versions, this means that any difference in
        // inflation will be caught on those devices.
        if (LAYOUT_INFLATER_SERVICE == name && Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP_MR1) {
            if (inflater == null) {
                inflater = PerformanceInflater(LayoutInflater.from(baseContext), this)
            }
            return inflater
        }
        return super.getSystemService(name)
    }

    private fun createBrowsingModeManager(initialMode: BrowsingMode): BrowsingModeManager {
        return DefaultBrowsingModeManager(initialMode, components.settings) { newMode ->
            updateSecureWindowFlags(newMode)
            themeManager.currentTheme = newMode
        }.also {
            updateSecureWindowFlags(initialMode)
        }
    }

    private fun updateSecureWindowFlags(mode: BrowsingMode = browsingModeManager.mode) {
        if (mode == BrowsingMode.Private && !settings().allowScreenshotsInPrivateMode) {
            window.addFlags(FLAG_SECURE)
        } else {
            window.clearFlags(FLAG_SECURE)
        }
    }

    private fun createThemeManager(): ThemeManager {
        return DefaultThemeManager(browsingModeManager.mode, this)
    }

    private fun openPopup(webExtensionState: WebExtensionState) {
        val action = NavGraphDirections.actionGlobalWebExtensionActionPopupFragment(
            webExtensionId = webExtensionState.id,
            webExtensionTitle = webExtensionState.name,
        )
        navHost.navController.navigate(action)
    }

    /**
     * The root container is null at this point, so let the HomeActivity know that
     * we are visually complete.
     */
    fun setVisualCompletenessQueueReady() {
        isVisuallyComplete = true
    }

    private fun captureSnapshotTelemetryMetrics() = CoroutineScope(IO).launch {
        // PWA
        val recentlyUsedPwaCount = components.core.webAppShortcutManager.recentlyUsedWebAppsCount(
            activeThresholdMs = PWA_RECENTLY_USED_THRESHOLD,
        )
        if (recentlyUsedPwaCount == 0) {
            Metrics.hasRecentPwas.set(false)
        } else {
            Metrics.hasRecentPwas.set(true)
            // This metric's lifecycle is set to 'application', meaning that it gets reset upon
            // application restart. Combined with the behaviour of the metric type itself (a growing counter),
            // it's important that this metric is only set once per application's lifetime.
            // Otherwise, we're going to over-count.
            Metrics.recentlyUsedPwaCount.add(recentlyUsedPwaCount)
        }
    }

    @VisibleForTesting
    internal fun isActivityColdStarted(startingIntent: Intent, activityIcicle: Bundle?): Boolean {
        // First time opening this activity in the task.
        // Cold start / start from Recents after back press.
        return activityIcicle == null &&
            // Activity was restarted from Recents after it was destroyed by Android while in background
            // in cases of memory pressure / "Don't keep activities".
            startingIntent.flags and Intent.FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY == 0
    }

    /**
     *  Indicates if the user should be redirected to the [BrowserFragment] or to the [HomeFragment],
     *  links from an external apps should always opened in the [BrowserFragment].
     */
    @VisibleForTesting
    internal fun shouldStartOnHome(intent: Intent? = this.intent): Boolean {
        return components.strictMode.resetAfter(StrictMode.allowThreadDiskReads()) {
            // We only want to open on home when users tap the app,
            // we want to ignore other cases when the app gets open by users clicking on links.
            getSettings().shouldStartOnHome() && intent?.action == ACTION_MAIN
        }
    }

    fun processIntent(intent: Intent): Boolean {
        return externalSourceIntentProcessors.any {
            it.process(
                intent,
                navHost.navController,
                this.intent,
            )
        }
    }

    @VisibleForTesting
    internal fun getSettings(): Settings = settings()

    private fun shouldNavigateToBrowserOnColdStart(savedInstanceState: Bundle?): Boolean {
        return isActivityColdStarted(intent, savedInstanceState) &&
            !processIntent(intent)
    }

    private suspend fun showFullscreenMessageIfNeeded(context: Context) {
        val messaging = context.components.nimbus.messaging
        val nextMessage = messaging.getNextMessage(FenixMessageSurfaceId.SURVEY) ?: return
        val researchSurfaceDialogFragment = ResearchSurfaceDialogFragment.newInstance(
            keyMessageText = nextMessage.text,
            keyAcceptButtonText = nextMessage.buttonLabel,
            keyDismissButtonText = null,
        )

        researchSurfaceDialogFragment.onAccept = {
            processIntent(messaging.getIntentForMessage(nextMessage))
            components.appStore.dispatch(AppAction.MessagingAction.MessageClicked(nextMessage))
        }

        researchSurfaceDialogFragment.onDismiss = {
            components.appStore.dispatch(AppAction.MessagingAction.MessageDismissed(nextMessage))
        }

        lifecycleScope.launch(Main) {
            researchSurfaceDialogFragment.showNow(
                supportFragmentManager,
                ResearchSurfaceDialogFragment.FRAGMENT_TAG,
            )
        }

        // Update message as displayed.
        val currentBootUniqueIdentifier = BootUtils.getBootIdentifier(context)

        messaging.onMessageDisplayed(nextMessage, currentBootUniqueIdentifier)
    }

    @VisibleForTesting
    @SuppressLint("NewApi") // The Android Q check is done in the systemGesturesInsets property getter
    internal fun collectOSNavigationTelemetry() {
        binding.root.doOnAttach {
            val systemGestureInsets = binding.root.systemGesturesInsets

            val isUsingGesturesNavigation =
                (systemGestureInsets?.left ?: 0) > 0 && (systemGestureInsets?.right ?: 0) > 0
            NavigationBar.osNavigationUsesGestures.set(isUsingGesturesNavigation)
        }
    }

    private fun showCrashReporter() {
        if (!settings().useNewCrashReporterDialog) {
            return
        }
        UnsubmittedCrashDialog(
            dispatcher = { action -> components.appStore.dispatch(AppAction.CrashActionWrapper(action)) },
        ).show(supportFragmentManager, UnsubmittedCrashDialog.TAG)
    }

    companion object {
        const val OPEN_TO_BROWSER = "open_to_browser"
        const val OPEN_TO_BROWSER_AND_LOAD = "open_to_browser_and_load"
        const val OPEN_TO_SEARCH = "open_to_search"
        const val PRIVATE_BROWSING_MODE = "private_browsing_mode"
        const val START_IN_RECENTS_SCREEN = "start_in_recents_screen"
        const val OPEN_PASSWORD_MANAGER = "open_password_manager"
        const val APP_ICON = "APP_ICON"

        // PWA must have been used within last 30 days to be considered "recently used" for the
        // telemetry purposes.
        private const val PWA_RECENTLY_USED_THRESHOLD = DateUtils.DAY_IN_MILLIS * 30L
    }
}