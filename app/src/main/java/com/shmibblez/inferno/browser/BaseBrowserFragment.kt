/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package com.shmibblez.inferno.browser

import android.app.KeyguardManager
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.accessibility.AccessibilityManager
import androidx.activity.result.ActivityResultLauncher
import androidx.annotation.CallSuper
import androidx.annotation.VisibleForTesting
import androidx.appcompat.app.AlertDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.content.ContextCompat.getColor
import androidx.core.content.getSystemService
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.OnApplyWindowInsetsListener
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import androidx.navigation.fragment.findNavController
import com.google.android.material.snackbar.Snackbar.LENGTH_LONG
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import mozilla.appservices.places.BookmarkRoot
import mozilla.appservices.places.uniffi.PlacesApiException
import mozilla.components.browser.menu.view.MenuButton
import mozilla.components.browser.state.action.ContentAction
import mozilla.components.browser.state.selector.findCustomTab
import mozilla.components.browser.state.selector.findCustomTabOrSelectedTab
import mozilla.components.browser.state.selector.findTab
import mozilla.components.browser.state.selector.findTabOrCustomTab
import mozilla.components.browser.state.selector.findTabOrCustomTabOrSelectedTab
import mozilla.components.browser.state.selector.getNormalOrPrivateTabs
import mozilla.components.browser.state.selector.selectedTab
import mozilla.components.browser.state.state.CustomTabSessionState
import mozilla.components.browser.state.state.SessionState
import mozilla.components.browser.state.state.TabSessionState
import mozilla.components.browser.state.state.content.DownloadState
import mozilla.components.browser.state.store.BrowserStore
import mozilla.components.browser.thumbnails.BrowserThumbnails
import mozilla.components.browser.toolbar.BrowserToolbar
import com.shmibblez.inferno.mozillaAndroidComponents.compose.base.Divider
import mozilla.components.compose.cfr.CFRPopup
import mozilla.components.compose.cfr.CFRPopupLayout
import mozilla.components.compose.cfr.CFRPopupProperties
import mozilla.components.concept.engine.permission.SitePermissions
import mozilla.components.concept.engine.prompt.ShareData
import mozilla.components.concept.storage.LoginEntry
import mozilla.components.feature.accounts.push.SendTabUseCases
import mozilla.components.feature.contextmenu.ContextMenuCandidate
import mozilla.components.feature.contextmenu.ContextMenuFeature
import mozilla.components.feature.downloads.DownloadsFeature
import mozilla.components.feature.downloads.manager.FetchDownloadManager
import mozilla.components.feature.downloads.temporary.CopyDownloadFeature
import mozilla.components.feature.downloads.temporary.ShareDownloadFeature
import mozilla.components.feature.intent.ext.EXTRA_SESSION_ID
import mozilla.components.feature.media.fullscreen.MediaSessionFullscreenFeature
import mozilla.components.feature.privatemode.feature.SecureWindowFeature
import com.shmibblez.inferno.browser.prompts.PromptFeature
import com.shmibblez.inferno.browser.prompts.PromptFeature.Companion.PIN_REQUEST
import mozilla.components.feature.prompts.address.AddressDelegate
import mozilla.components.feature.prompts.creditcard.CreditCardDelegate
import mozilla.components.feature.prompts.dialog.FullScreenNotificationToast
import mozilla.components.feature.prompts.dialog.GestureNavUtils
import com.shmibblez.inferno.browser.prompts.webPrompts.InfernoAndroidPhotoPicker
import mozilla.components.feature.prompts.identitycredential.DialogColors
import mozilla.components.feature.prompts.identitycredential.DialogColorsProvider
import mozilla.components.feature.prompts.login.LoginDelegate
import mozilla.components.feature.prompts.login.PasswordGeneratorDialogColors
import mozilla.components.feature.prompts.login.PasswordGeneratorDialogColorsProvider
import mozilla.components.feature.prompts.login.SuggestStrongPasswordDelegate
import mozilla.components.feature.prompts.share.ShareDelegate
import mozilla.components.feature.readerview.ReaderViewFeature
import mozilla.components.feature.search.SearchFeature
import mozilla.components.feature.session.FullScreenFeature
import mozilla.components.feature.session.PictureInPictureFeature
import mozilla.components.feature.session.ScreenOrientationFeature
import mozilla.components.feature.session.SessionFeature
import mozilla.components.feature.session.SwipeRefreshFeature
import mozilla.components.feature.sitepermissions.SitePermissionsFeature
import mozilla.components.feature.webauthn.WebAuthnFeature
import mozilla.components.lib.state.ext.consumeFlow
import mozilla.components.lib.state.ext.consumeFrom
import mozilla.components.lib.state.ext.flowScoped
import mozilla.components.service.sync.autofill.DefaultCreditCardValidationDelegate
import mozilla.components.service.sync.logins.DefaultLoginValidationDelegate
import mozilla.components.service.sync.logins.LoginsApiException
import mozilla.components.service.sync.logins.SyncableLoginsStorage
import mozilla.components.support.base.feature.ActivityResultHandler
import mozilla.components.support.base.feature.PermissionsFeature
import mozilla.components.support.base.feature.UserInteractionHandler
import mozilla.components.support.base.feature.ViewBoundFeatureWrapper
import mozilla.components.support.ktx.android.view.enterImmersiveMode
import mozilla.components.support.ktx.android.view.exitImmersiveMode
import mozilla.components.support.ktx.android.view.hideKeyboard
import mozilla.components.support.ktx.kotlin.getOrigin
import mozilla.components.support.ktx.kotlinx.coroutines.flow.ifAnyChanged
import mozilla.components.support.locale.ActivityContextWrapper
import mozilla.components.ui.widgets.withCenterAlignedButtons
//import mozilla.telemetry.glean.private.NoExtras
//import com.shmibblez.inferno.GleanMetrics.Events
//import com.shmibblez.inferno.GleanMetrics.Logins
//import com.shmibblez.inferno.GleanMetrics.MediaState
//import com.shmibblez.inferno.GleanMetrics.NavigationBar
//import com.shmibblez.inferno.GleanMetrics.PullToRefreshInBrowser
import com.shmibblez.inferno.HomeActivity
import com.shmibblez.inferno.IntentReceiverActivity
import com.shmibblez.inferno.NavGraphDirections
//import com.shmibblez.inferno.NavGraphDirections
import com.shmibblez.inferno.OnLongPressedListener
import com.shmibblez.inferno.OpenInFirefoxBinding
import com.shmibblez.inferno.R
import com.shmibblez.inferno.ReaderViewBinding
import com.shmibblez.inferno.bindings.FindInPageBinding
import com.shmibblez.inferno.browser.browsingmode.BrowsingMode
import com.shmibblez.inferno.browser.readermode.DefaultReaderModeController
import com.shmibblez.inferno.browser.tabstrip.TabStrip
import com.shmibblez.inferno.browser.tabstrip.isTabStripEnabled
import com.shmibblez.inferno.components.Components
import com.shmibblez.inferno.components.FindInPageIntegration
import com.shmibblez.inferno.LoginBarsIntegration
import com.shmibblez.inferno.components.StoreProvider
import com.shmibblez.inferno.components.accounts.FxaWebChannelIntegration
import com.shmibblez.inferno.components.appstate.AppAction
import com.shmibblez.inferno.components.appstate.AppAction.MessagingAction
import com.shmibblez.inferno.components.appstate.AppAction.MessagingAction.MicrosurveyAction
import com.shmibblez.inferno.components.menu.MenuAccessPoint
//import com.shmibblez.inferno.components.metrics.MetricsUtils
import com.shmibblez.inferno.components.toolbar.BottomToolbarContainerIntegration
import com.shmibblez.inferno.components.toolbar.BottomToolbarContainerView
import com.shmibblez.inferno.components.toolbar.BrowserFragmentState
import com.shmibblez.inferno.components.toolbar.BrowserFragmentStore
import com.shmibblez.inferno.components.toolbar.BrowserToolbarView
import com.shmibblez.inferno.components.toolbar.DefaultBrowserToolbarController
import com.shmibblez.inferno.components.toolbar.DefaultBrowserToolbarMenuController
import com.shmibblez.inferno.components.toolbar.FenixTabCounterMenu
import com.shmibblez.inferno.components.toolbar.NewTabMenu
import com.shmibblez.inferno.components.toolbar.ToolbarContainerView
import com.shmibblez.inferno.components.toolbar.ToolbarIntegration
import com.shmibblez.inferno.components.toolbar.ToolbarMenu
import com.shmibblez.inferno.components.toolbar.ToolbarPosition
import com.shmibblez.inferno.components.toolbar.interactor.BrowserToolbarInteractor
import com.shmibblez.inferno.components.toolbar.interactor.DefaultBrowserToolbarInteractor
import com.shmibblez.inferno.components.toolbar.navbar.BrowserNavBar
import com.shmibblez.inferno.components.toolbar.navbar.EngineViewClippingBehavior
import com.shmibblez.inferno.components.toolbar.navbar.shouldAddNavigationBar
import com.shmibblez.inferno.components.toolbar.navbar.updateNavBarForConfigurationChange
import com.shmibblez.inferno.compose.core.Action
import com.shmibblez.inferno.compose.snackbar.Snackbar
import com.shmibblez.inferno.compose.snackbar.SnackbarState
import com.shmibblez.inferno.crashes.CrashContentIntegration
import com.shmibblez.inferno.databinding.FragmentBrowserBinding
import com.shmibblez.inferno.downloads.DownloadService
import com.shmibblez.inferno.downloads.dialog.DynamicDownloadDialog
import com.shmibblez.inferno.downloads.dialog.FirstPartyDownloadDialog
import com.shmibblez.inferno.downloads.dialog.StartDownloadDialog
import com.shmibblez.inferno.downloads.dialog.ThirdPartyDownloadDialog
import com.shmibblez.inferno.ext.accessibilityManager
import com.shmibblez.inferno.ext.components
import com.shmibblez.inferno.ext.hideToolbar
import com.shmibblez.inferno.ext.isKeyboardVisible
import com.shmibblez.inferno.ext.isToolbarAtBottom
import com.shmibblez.inferno.ext.nav
import com.shmibblez.inferno.ext.navigateWithBreadcrumb
import com.shmibblez.inferno.ext.registerForActivityResult
import com.shmibblez.inferno.ext.requireComponents
import com.shmibblez.inferno.ext.runIfFragmentIsAttached
import com.shmibblez.inferno.ext.secure
import com.shmibblez.inferno.ext.settings
import com.shmibblez.inferno.ext.tabClosedUndoMessage
import com.shmibblez.inferno.ext.updateMicrosurveyPromptForConfigurationChange
import com.shmibblez.inferno.home.HomeScreenViewModel
import com.shmibblez.inferno.home.SharedViewModel
import com.shmibblez.inferno.library.bookmarks.friendlyRootTitle
import com.shmibblez.inferno.messaging.FenixMessageSurfaceId
import com.shmibblez.inferno.messaging.MessagingFeature
import com.shmibblez.inferno.microsurvey.ui.MicrosurveyRequestPrompt
import com.shmibblez.inferno.microsurvey.ui.ext.MicrosurveyUIData
import com.shmibblez.inferno.microsurvey.ui.ext.toMicrosurveyUIData
import com.shmibblez.inferno.perf.MarkersFragmentLifecycleCallbacks
import com.shmibblez.inferno.settings.SupportUtils
import com.shmibblez.inferno.settings.biometric.BiometricPromptFeature
import com.shmibblez.inferno.snackbar.FenixSnackbarDelegate
import com.shmibblez.inferno.snackbar.SnackbarBinding
import com.shmibblez.inferno.tabstray.Page
import com.shmibblez.inferno.tabstray.ext.toDisplayTitle
import com.shmibblez.inferno.theme.FirefoxTheme
import com.shmibblez.inferno.theme.ThemeManager
import com.shmibblez.inferno.utils.allowUndo
import com.shmibblez.inferno.wifi.SitePermissionsWifiIntegration
import kotlinx.coroutines.ExperimentalCoroutinesApi
import java.lang.ref.WeakReference
import kotlin.coroutines.cancellation.CancellationException
import mozilla.components.ui.widgets.behavior.EngineViewClippingBehavior as OldEngineViewClippingBehavior
import mozilla.components.ui.widgets.behavior.ToolbarPosition as OldToolbarPosition

//import com.shmibblez.inferno.GleanMetrics.TabStrip as TabStripMetrics

private const val NAVIGATION_CFR_VERTICAL_OFFSET = 10
private const val NAVIGATION_CFR_ARROW_OFFSET = 24
private const val NAVIGATION_CFR_MAX_MS_BETWEEN_CLICKS = 5000

// todo: onBiometricResult (search in this file to see where necessary)

/**
 * Base fragment extended by [BrowserFragment].
 * This class only contains shared code focused on the main browsing content.
 * UI code specific to the app or to custom tabs can be found in the subclasses.
 */
@Suppress("TooManyFunctions", "LargeClass")
abstract class BaseBrowserFragment :
    Fragment(),
    UserInteractionHandler,
    ActivityResultHandler,
    OnLongPressedListener,
    AccessibilityManager.AccessibilityStateChangeListener {

    private var _binding: FragmentBrowserBinding? = null
    internal val binding get() = _binding!!

    private lateinit var browserFragmentStore: BrowserFragmentStore
    private lateinit var browserAnimator: BrowserAnimator
    private lateinit var startForResult: ActivityResultLauncher<Intent>

    private var _browserToolbarInteractor: BrowserToolbarInteractor? = null

    @VisibleForTesting(otherwise = VisibleForTesting.PROTECTED)
    internal val browserToolbarInteractor: BrowserToolbarInteractor
        get() = _browserToolbarInteractor!!

    @VisibleForTesting
    @Suppress("VariableNaming")
    internal var _browserToolbarView: BrowserToolbarView? = null

    @VisibleForTesting
    internal val browserToolbarView: BrowserToolbarView
        get() = _browserToolbarView!!

    @VisibleForTesting(otherwise = VisibleForTesting.PROTECTED)
    @Suppress("VariableNaming")
    internal var _bottomToolbarContainerView: BottomToolbarContainerView? = null
    protected val bottomToolbarContainerView: BottomToolbarContainerView
        get() = _bottomToolbarContainerView!!

    @Suppress("VariableNaming")
    @VisibleForTesting
    internal var _menuButtonView: MenuButton? = null

    protected val readerViewFeature = ViewBoundFeatureWrapper<ReaderViewFeature>()
    protected val thumbnailsFeature = ViewBoundFeatureWrapper<BrowserThumbnails>()

    @VisibleForTesting
    internal val messagingFeatureMicrosurvey = ViewBoundFeatureWrapper<MessagingFeature>()

    private val sessionFeature = ViewBoundFeatureWrapper<SessionFeature>()
    private val contextMenuFeature = ViewBoundFeatureWrapper<ContextMenuFeature>()
    private val downloadsFeature = ViewBoundFeatureWrapper<DownloadsFeature>()
    private val shareDownloadsFeature = ViewBoundFeatureWrapper<ShareDownloadFeature>()
    private val copyDownloadsFeature = ViewBoundFeatureWrapper<CopyDownloadFeature>()
    private val promptsFeature = ViewBoundFeatureWrapper<PromptFeature>()
    private lateinit var loginBarsIntegration: LoginBarsIntegration

    @VisibleForTesting
    internal val findInPageIntegration = ViewBoundFeatureWrapper<FindInPageIntegration>()
    private val toolbarIntegration = ViewBoundFeatureWrapper<ToolbarIntegration>()
    private val bottomToolbarContainerIntegration =
        ViewBoundFeatureWrapper<BottomToolbarContainerIntegration>()
    private val sitePermissionsFeature = ViewBoundFeatureWrapper<SitePermissionsFeature>()
    private val fullScreenFeature = ViewBoundFeatureWrapper<FullScreenFeature>()
    private val swipeRefreshFeature = ViewBoundFeatureWrapper<SwipeRefreshFeature>()
    private val webchannelIntegration = ViewBoundFeatureWrapper<FxaWebChannelIntegration>()
    private val sitePermissionWifiIntegration =
        ViewBoundFeatureWrapper<SitePermissionsWifiIntegration>()
    private val secureWindowFeature = ViewBoundFeatureWrapper<SecureWindowFeature>()
    private var fullScreenMediaSessionFeature =
        ViewBoundFeatureWrapper<MediaSessionFullscreenFeature>()
    private val searchFeature = ViewBoundFeatureWrapper<SearchFeature>()
    private val webAuthnFeature = ViewBoundFeatureWrapper<WebAuthnFeature>()
    private val screenOrientationFeature = ViewBoundFeatureWrapper<ScreenOrientationFeature>()
    private val biometricPromptFeature = ViewBoundFeatureWrapper<BiometricPromptFeature>()
    private val crashContentIntegration = ViewBoundFeatureWrapper<CrashContentIntegration>()
    private val readerViewBinding = ViewBoundFeatureWrapper<ReaderViewBinding>()
    private val openInFirefoxBinding = ViewBoundFeatureWrapper<OpenInFirefoxBinding>()
    private val findInPageBinding = ViewBoundFeatureWrapper<FindInPageBinding>()
    private val snackbarBinding = ViewBoundFeatureWrapper<SnackbarBinding>()
    private val standardSnackbarErrorBinding =
        ViewBoundFeatureWrapper<StandardSnackbarErrorBinding>()

    private var pipFeature: PictureInPictureFeature? = null

    var customTabSessionId: String? = null

    @VisibleForTesting
    internal var browserInitialized: Boolean = false
    private var initUIJob: Job? = null

    @VisibleForTesting(otherwise = VisibleForTesting.PROTECTED)
    internal var webAppToolbarShouldBeVisible = true

    internal val sharedViewModel: SharedViewModel by activityViewModels()
    private val homeViewModel: HomeScreenViewModel by activityViewModels()

    private var currentStartDownloadDialog: StartDownloadDialog? = null

    private lateinit var savedLoginsLauncher: ActivityResultLauncher<Intent>

    private var lastSavedGeneratedPassword: String? = null

    // Registers a photo picker activity launcher in single-select mode.
    private val singleMediaPicker =
        InfernoAndroidPhotoPicker.singleMediaPickerFrag(
            { this },
            { null }, //getPromptsFeature() },
        )

    // Registers a photo picker activity launcher in multi-select mode.
    private val multipleMediaPicker =
        InfernoAndroidPhotoPicker.multipleMediaPickerFrag(
            { this },
            { null }, // getPromptsFeature() },
        )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        savedLoginsLauncher = registerForActivityResult { navigateToSavedLoginsFragment() }
    }

    private fun getFragment(): Fragment {
        return this
    }

    private fun getPromptsFeature(): PromptFeature? {
        return promptsFeature.get()
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @CallSuper
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        // DO NOT ADD ANYTHING ABOVE THIS getProfilerTime CALL!
        val profilerStartTime = requireComponents.core.engine.profiler?.getProfilerTime()

        customTabSessionId = requireArguments().getString(EXTRA_SESSION_ID)

        _binding = FragmentBrowserBinding.inflate(inflater, container, false)

        val activity = activity as HomeActivity
        val originalContext = ActivityContextWrapper.getOriginalContext(activity)
        binding.engineView.setActivityContext(originalContext)

        browserFragmentStore = StoreProvider.get(this) {
            BrowserFragmentStore(
                BrowserFragmentState(),
            )
        }

        startForResult = registerForActivityResult { result ->
            listOf(
                promptsFeature,
                webAuthnFeature,
            ).any {
                it.onActivityResult(PIN_REQUEST, result.data, result.resultCode)
            }
        }

        // DO NOT MOVE ANYTHING BELOW THIS addMarker CALL!
        requireComponents.core.engine.profiler?.addMarker(
            MarkersFragmentLifecycleCallbacks.MARKER_NAME,
            profilerStartTime,
            "BaseBrowserFragment.onCreateView",
        )
        return binding.root
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    final override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        // DO NOT ADD ANYTHING ABOVE THIS getProfilerTime CALL!
        val profilerStartTime = requireComponents.core.engine.profiler?.getProfilerTime()

        initializeUI(view)

        if (customTabSessionId == null) {
            // We currently only need this observer to navigate to home
            // in case all tabs have been removed on startup. No need to
            // this if we have a known session to display.
            observeRestoreComplete(requireComponents.core.store, findNavController())
        }

        observeTabSelection(requireComponents.core.store)

        if (!requireComponents.fenixOnboarding.userHasBeenOnboarded()) {
            observeTabSource(requireComponents.core.store)
        }

        requireContext().accessibilityManager.addAccessibilityStateChangeListener(this)

        requireComponents.backgroundServices.closeSyncedTabsCommandReceiver.register(
            observer = CloseLastSyncedTabObserver(
                scope = viewLifecycleOwner.lifecycleScope,
                navController = findNavController(),
            ),
            view = view,
        )

        // DO NOT MOVE ANYTHING BELOW THIS addMarker CALL!
        requireComponents.core.engine.profiler?.addMarker(
            MarkersFragmentLifecycleCallbacks.MARKER_NAME,
            profilerStartTime,
            "BaseBrowserFragment.onViewCreated",
        )
    }

    private fun initializeUI(view: View) {
        val tab = getCurrentTab()
        browserInitialized = if (tab != null) {
            initializeUI(view, tab)
            true
        } else {
            false
        }
    }

    @Suppress("ComplexMethod", "LongMethod", "DEPRECATION")
    // https://github.com/mozilla-mobile/fenix/issues/19920
    @CallSuper
    internal open fun initializeUI(view: View, tab: SessionState) {
        val context = requireContext()
        val store = context.components.core.store
        val activity = requireActivity() as HomeActivity

        browserAnimator = BrowserAnimator(
            fragment = WeakReference(this),
            engineView = WeakReference(binding.engineView),
            swipeRefresh = WeakReference(binding.swipeRefresh),
            viewLifecycleScope = WeakReference(viewLifecycleOwner.lifecycleScope),
        ).apply {
            beginAnimateInIfNecessary()
        }

        val openInFenixIntent = Intent(context, IntentReceiverActivity::class.java).apply {
            action = Intent.ACTION_VIEW
            putExtra(HomeActivity.OPEN_TO_BROWSER, true)
        }

        val readerMenuController = DefaultReaderModeController(
            readerViewFeature,
            binding.readerViewControlsBar,
            isPrivate = activity.browsingModeManager.mode.isPrivate,
            onReaderModeChanged = { activity.finishActionMode() },
        )
        val browserToolbarController = DefaultBrowserToolbarController(
            store = store,
            appStore = context.components.appStore,
            tabsUseCases = requireComponents.useCases.tabsUseCases,
            activity = activity,
            settings = context.settings(),
            navController = findNavController(),
            readerModeController = readerMenuController,
            engineView = binding.engineView,
            homeViewModel = homeViewModel,
            customTabSessionId = customTabSessionId,
            browserAnimator = browserAnimator,
            onTabCounterClicked = {
                onTabCounterClicked(activity.browsingModeManager.mode)
            },
            onCloseTab = { closedSession ->
                val closedTab =
                    store.state.findTab(closedSession.id) ?: return@DefaultBrowserToolbarController
                showUndoSnackbar(context.tabClosedUndoMessage(closedTab.content.private))
            },
        )
        val browserToolbarMenuController = DefaultBrowserToolbarMenuController(
            fragment = this,
            store = store,
            appStore = requireComponents.appStore,
            activity = activity,
            navController = findNavController(),
            settings = context.settings(),
            readerModeController = readerMenuController,
            sessionFeature = sessionFeature,
            findInPageLauncher = { findInPageIntegration.withFeature { it.launch() } },
            browserAnimator = browserAnimator,
            customTabSessionId = customTabSessionId,
            openInFenixIntent = openInFenixIntent,
            bookmarkTapped = { url: String, title: String ->
                viewLifecycleOwner.lifecycleScope.launch {
                    bookmarkTapped(url, title)
                }
            },
            scope = viewLifecycleOwner.lifecycleScope,
            tabCollectionStorage = requireComponents.core.tabCollectionStorage,
            topSitesStorage = requireComponents.core.topSitesStorage,
            pinnedSiteStorage = requireComponents.core.pinnedSiteStorage,
        )

        _browserToolbarInteractor = DefaultBrowserToolbarInteractor(
            browserToolbarController,
            browserToolbarMenuController,
        )

        _browserToolbarView = BrowserToolbarView(
            context = context,
            container = binding.browserLayout,
            snackbarParent = binding.dynamicSnackbarContainer,
            settings = context.settings(),
            interactor = browserToolbarInteractor,
            customTabSession = customTabSessionId?.let { store.state.findCustomTab(it) },
            lifecycleOwner = viewLifecycleOwner,
            tabStripContent = {
                FirefoxTheme {
                    TabStrip(
                        onAddTabClick = {
                            findNavController().navigate(
                                NavGraphDirections.actionGlobalHome(
                                    focusOnAddressBar = true,
                                ),
                            )
                        },
                        onLastTabClose = { isPrivate ->
                            requireComponents.appStore.dispatch(
                                AppAction.TabStripAction.UpdateLastTabClosed(isPrivate),
                            )
                            findNavController().navigate(
                                BrowserComponentWrapperFragmentDirections.actionGlobalHome(),
                            )
                        },
                        onSelectedTabClick = {},
                        onCloseTabClick = { isPrivate ->
                            showUndoSnackbar(context.tabClosedUndoMessage(isPrivate))
                        },
                        onPrivateModeToggleClick = { mode ->
                            activity.browsingModeManager.mode = mode
                            findNavController().navigate(
                                BrowserComponentWrapperFragmentDirections.actionGlobalHome(),
                            )
                        },
                        onTabCounterClick = {
//                            onTabCounterClicked(activity.browsingModeManager.mode)
                        },
                    )
                }
            },
        )

        loginBarsIntegration = LoginBarsIntegration(
            loginsBar = binding.loginSelectBar,
            passwordBar = binding.suggestStrongPasswordBar,
            settings = requireContext().settings(),
            onLoginsBarShown = {
                removeBottomToolbarDivider(browserToolbarView.view)
                updateNavbarDivider()
            },
            onLoginsBarHidden = {
                restoreBottomToolbarDivider(browserToolbarView.view)
                updateNavbarDivider()
            },
        )

        val shouldAddNavigationBar =
            context.shouldAddNavigationBar() && webAppToolbarShouldBeVisible
        if (shouldAddNavigationBar) {
            initializeNavBar(
                browserToolbar = browserToolbarView.view,
                view = view,
                context = context,
                activity = activity,
            )
        }

        if (context.settings().microsurveyFeatureEnabled) {
            listenForMicrosurveyMessage(context)
        }

        toolbarIntegration.set(
            feature = browserToolbarView.toolbarIntegration,
            owner = this,
            view = view,
        )

        findInPageIntegration.set(
            feature = FindInPageIntegration(
                store = store,
                appStore = requireComponents.appStore,
                sessionId = customTabSessionId,
                view = binding.findInPageView,
                engineView = binding.engineView,
                toolbarsHideCallback = {
                    expandBrowserView()
                },
                toolbarsResetCallback = {
                    onUpdateToolbarForConfigurationChange(browserToolbarView)
                    collapseBrowserView()
                },
            ),
            owner = this,
            view = view,
        )

        findInPageBinding.set(
            feature = FindInPageBinding(
                appStore = context.components.appStore,
                onFindInPageLaunch = { findInPageIntegration.withFeature { it.launch() } },
            ),
            owner = this,
            view = view,
        )

        readerViewBinding.set(
            feature = ReaderViewBinding(
                appStore = context.components.appStore,
                readerMenuController = readerMenuController,
            ),
            owner = this,
            view = view,
        )

        openInFirefoxBinding.set(
            feature = OpenInFirefoxBinding(
                activity = activity,
                appStore = context.components.appStore,
                customTabSessionId = customTabSessionId,
                customTabsUseCases = context.components.useCases.customTabsUseCases,
                openInFenixIntent = openInFenixIntent,
                sessionFeature = sessionFeature,
            ),
            owner = this,
            view = view,
        )

        browserToolbarView.view.display.setOnSiteSecurityClickedListener {
            showQuickSettingsDialog()
        }

        contextMenuFeature.set(
            feature = ContextMenuFeature(
                fragmentManager = parentFragmentManager,
                store = store,
                candidates = getContextMenuCandidates(context, binding.dynamicSnackbarContainer),
                engineView = binding.engineView,
                useCases = context.components.useCases.contextMenuUseCases,
                tabId = customTabSessionId,
            ),
            owner = this,
            view = view,
        )

        snackbarBinding.set(
            feature = SnackbarBinding(
                context = context,
                browserStore = context.components.core.store,
                appStore = context.components.appStore,
                snackbarDelegate = FenixSnackbarDelegate(binding.dynamicSnackbarContainer),
                navController = findNavController(),
                sendTabUseCases = SendTabUseCases(requireComponents.backgroundServices.accountManager),
                customTabSessionId = customTabSessionId,
            ),
            owner = this,
            view = view,
        )

        standardSnackbarErrorBinding.set(
            feature = StandardSnackbarErrorBinding(
                requireActivity(),
                binding.dynamicSnackbarContainer,
                requireActivity().components.appStore,
            ),
            owner = viewLifecycleOwner,
            view = binding.root,
        )

        val allowScreenshotsInPrivateMode = context.settings().allowScreenshotsInPrivateMode
        secureWindowFeature.set(
            feature = SecureWindowFeature(
                window = requireActivity().window,
                store = store,
                customTabId = customTabSessionId,
                isSecure = { !allowScreenshotsInPrivateMode && it.content.private },
                clearFlagOnStop = false,
            ),
            owner = this,
            view = view,
        )

        fullScreenMediaSessionFeature.set(
            feature = MediaSessionFullscreenFeature(
                requireActivity(),
                context.components.core.store,
                customTabSessionId,
            ),
            owner = this,
            view = view,
        )

        val shareDownloadFeature = ShareDownloadFeature(
            context = context.applicationContext,
            httpClient = context.components.core.client,
            store = store,
            tabId = customTabSessionId,
        )

        val copyDownloadFeature = CopyDownloadFeature(
            context = context.applicationContext,
            httpClient = context.components.core.client,
            store = store,
            tabId = customTabSessionId,
            onCopyConfirmation = {
                showSnackbarForClipboardCopy()
            },
        )

        val downloadFeature = DownloadsFeature(
            context.applicationContext,
            store = store,
            useCases = context.components.useCases.downloadUseCases,
            fragmentManager = childFragmentManager,
            tabId = customTabSessionId,
            downloadManager = FetchDownloadManager(
                context.applicationContext,
                store,
                DownloadService::class,
                notificationsDelegate = context.components.notificationsDelegate,
            ),
            shouldForwardToThirdParties = {
                context.settings().shouldUseExternalDownloadManager
//                PreferenceManager.getDefaultSharedPreferences(context).getBoolean(
//                    context.getPreferenceKey(R.string.pref_key_external_download_manager),
//                    false,
//                )
            },
            promptsStyling = DownloadsFeature.PromptsStyling(
                gravity = Gravity.BOTTOM,
                shouldWidthMatchParent = true,
                positiveButtonBackgroundColor = ThemeManager.resolveAttribute(
                    R.attr.accent,
                    context,
                ),
                positiveButtonTextColor = ThemeManager.resolveAttribute(
                    R.attr.textOnColorPrimary,
                    context,
                ),
                positiveButtonRadius = (resources.getDimensionPixelSize(R.dimen.tab_corner_radius)).toFloat(),
            ),
            onNeedToRequestPermissions = { permissions ->
                requestPermissions(permissions, REQUEST_CODE_DOWNLOAD_PERMISSIONS)
            },
            customFirstPartyDownloadDialog = { filename, contentSize, positiveAction, negativeAction ->
                run {
                    if (currentStartDownloadDialog == null) {
                        FirstPartyDownloadDialog(
                            activity = requireActivity(),
                            filename = filename.value,
                            contentSize = contentSize.value,
                            positiveButtonAction = positiveAction.value,
                            negativeButtonAction = negativeAction.value,
                        ).onDismiss {
                            currentStartDownloadDialog = null
                        }.show(binding.startDownloadDialogContainer)
                            .also {
                                currentStartDownloadDialog = it
                            }
                    }
                }
            },
            customThirdPartyDownloadDialog = { downloaderApps, onAppSelected, negativeActionCallback ->
                run {
                    if (currentStartDownloadDialog == null) {
                        ThirdPartyDownloadDialog(
                            activity = requireActivity(),
                            downloaderApps = downloaderApps.value,
                            onAppSelected = onAppSelected.value,
                            negativeButtonAction = negativeActionCallback.value,
                        ).onDismiss {
                            currentStartDownloadDialog = null
                        }.show(binding.startDownloadDialogContainer).also {
                            currentStartDownloadDialog = it
                        }
                    }
                }
            },
        )

        val passwordGeneratorColorsProvider = PasswordGeneratorDialogColorsProvider {
            PasswordGeneratorDialogColors(
                title = ThemeManager.resolveAttributeColor(attribute = R.attr.textPrimary),
                description = ThemeManager.resolveAttributeColor(attribute = R.attr.textSecondary),
                background = ThemeManager.resolveAttributeColor(attribute = R.attr.layer1),
                cancelText = ThemeManager.resolveAttributeColor(attribute = R.attr.textAccent),
                confirmButton = ThemeManager.resolveAttributeColor(attribute = R.attr.actionPrimary),
                passwordBox = ThemeManager.resolveAttributeColor(attribute = R.attr.layer2),
                boxBorder = ThemeManager.resolveAttributeColor(attribute = R.attr.textDisabled),
            )
        }

        val bottomToolbarHeight = context.settings().getBottomToolbarHeight(context)

        downloadFeature.onDownloadStopped = { downloadState, _, downloadJobStatus ->
            handleOnDownloadFinished(
                context = context,
                downloadState = downloadState,
                downloadJobStatus = downloadJobStatus,
                tryAgain = downloadFeature::tryAgain,
                browserToolbars = listOfNotNull(
                    browserToolbarView,
                    _bottomToolbarContainerView?.toolbarContainerView,
                ),
            )
        }

        resumeDownloadDialogState(
            getCurrentTab()?.id,
            store,
            context,
        )

        shareDownloadsFeature.set(
            shareDownloadFeature,
            owner = this,
            view = view,
        )

        copyDownloadsFeature.set(
            copyDownloadFeature,
            owner = this,
            view = view,
        )

        downloadsFeature.set(
            downloadFeature,
            owner = this,
            view = view,
        )

//        pipFeature = PictureInPictureFeature(
//            store = store,
//            activity = requireActivity(),
//            crashReporting = context.components.crashReporter, // context.components.analytics.crashReporter,
//            tabId = customTabSessionId,
//        )

        biometricPromptFeature.set(
            feature = BiometricPromptFeature(
                context = context,
                fragment = this,
                onAuthFailure = {
//                    promptsFeature.get()?.onBiometricResult(isAuthenticated = false)
                },
                onAuthSuccess = {
//                    promptsFeature.get()?.onBiometricResult(isAuthenticated = true)
                },
            ),
            owner = this,
            view = view,
        )

        val colorsProvider = DialogColorsProvider {
            DialogColors(
                title = ThemeManager.resolveAttributeColor(attribute = R.attr.textPrimary),
                description = ThemeManager.resolveAttributeColor(attribute = R.attr.textSecondary),
            )
        }

        promptsFeature.set(
            feature = PromptFeature(
                activity = activity,
                store = store,
                customTabId = customTabSessionId,
                fragmentManager = parentFragmentManager,
                identityCredentialColorsProvider = colorsProvider,
                tabsUseCases = requireComponents.useCases.tabsUseCases,
                fileUploadsDirCleaner = requireComponents.core.fileUploadsDirCleaner,
                creditCardValidationDelegate = DefaultCreditCardValidationDelegate(
                    context.components.core.lazyAutofillStorage,
                ),
                loginValidationDelegate = DefaultLoginValidationDelegate(
                    context.components.core.lazyPasswordsStorage,
                ),
                isLoginAutofillEnabled = {
                    context.settings().shouldAutofillLogins
                },
                isSaveLoginEnabled = {
                    context.settings().shouldPromptToSaveLogins
                },
                isCreditCardAutofillEnabled = {
                    context.settings().shouldAutofillCreditCardDetails
                },
                isAddressAutofillEnabled = {
                    context.settings().addressFeature && context.settings().shouldAutofillAddressDetails
                },
                loginExceptionStorage = context.components.core.loginExceptionStorage,
                shareDelegate = object : ShareDelegate {
                    override fun showShareSheet(
                        context: Context,
                        shareData: ShareData,
                        onDismiss: () -> Unit,
                        onSuccess: () -> Unit,
                    ) {
                        val directions = NavGraphDirections.actionGlobalShareFragment(
                            data = arrayOf(shareData),
                            showPage = true,
                            sessionId = getCurrentTab()?.id,
                        )
                        findNavController().navigate(directions)
                    }
                },
                onNeedToRequestPermissions = { permissions ->
                    requestPermissions(permissions, REQUEST_CODE_PROMPT_PERMISSIONS)
                },
                loginDelegate = object : LoginDelegate {
                    override val loginPickerView
                        get() = binding.loginSelectBar
                    override val onManageLogins = {
                        browserAnimator.captureEngineViewAndDrawStatically {
                            val directions =
                                NavGraphDirections.actionGlobalSavedLoginsAuthFragment()
                            findNavController().navigate(directions)
                        }
                    }
                },
                suggestStrongPasswordDelegate = object : SuggestStrongPasswordDelegate {
                    override val strongPasswordPromptViewListenerView
                        get() = binding.suggestStrongPasswordBar
                },
                shouldAutomaticallyShowSuggestedPassword = { context.settings().isFirstTimeEngagingWithSignup },
                onFirstTimeEngagedWithSignup = {
                    context.settings().isFirstTimeEngagingWithSignup = false
                },
                onSaveLoginWithStrongPassword = { url, password ->
                    handleOnSaveLoginWithGeneratedStrongPassword(
                        passwordsStorage = context.components.core.passwordsStorage,
                        url = url,
                        password = password,
                    )
                },
                onSaveLogin = { isUpdate ->
                    showSnackbarAfterLoginChange(
                        isUpdate,
                    )
                },
                passwordGeneratorColorsProvider = passwordGeneratorColorsProvider,
                hideUpdateFragmentAfterSavingGeneratedPassword = { username, password ->
                    hideUpdateFragmentAfterSavingGeneratedPassword(
                        username,
                        password,
                    )
                },
                removeLastSavedGeneratedPassword = { removeLastSavedGeneratedPassword() },
                creditCardDelegate = object : CreditCardDelegate {
                    override val creditCardPickerView
                        get() = binding.creditCardSelectBar
                    override val onManageCreditCards = {
                        val directions =
                            NavGraphDirections.actionGlobalAutofillSettingFragment()
                        findNavController().navigate(directions)
                    }
                    override val onSelectCreditCard = {
                        showBiometricPrompt(context)
                    }
                },
                addressDelegate = object : AddressDelegate {
                    override val addressPickerView
                        get() = binding.addressSelectBar
                    override val onManageAddresses = {
                        val directions = NavGraphDirections.actionGlobalAutofillSettingFragment()
                        findNavController().navigate(directions)
                    }
                },
                infernoAndroidPhotoPicker = InfernoAndroidPhotoPicker(
                    requireContext(),
                    singleMediaPicker,
                    multipleMediaPicker,
                ),
            ),
            owner = this,
            view = view,
        )

        sessionFeature.set(
            feature = SessionFeature(
                requireComponents.core.store,
                requireComponents.useCases.sessionUseCases.goBack,
                requireComponents.useCases.sessionUseCases.goForward,
                binding.engineView,
                customTabSessionId,
            ),
            owner = this,
            view = view,
        )

        crashContentIntegration.set(
            feature = CrashContentIntegration(
                context = context,
                browserStore = requireComponents.core.store,
                appStore = requireComponents.appStore,
                toolbar = browserToolbarView.view,
                crashReporterView = binding.crashReporterView,
                components = requireComponents,
                settings = context.settings(),
                navController = findNavController(),
                sessionId = customTabSessionId,
            ),
            owner = this,
            view = view,
        )

        searchFeature.set(
            feature = SearchFeature(store, customTabSessionId) { request, tabId ->
                val parentSession = store.state.findTabOrCustomTab(tabId)
                val useCase = if (request.isPrivate) {
                    requireComponents.useCases.searchUseCases.newPrivateTabSearch
                } else {
                    requireComponents.useCases.searchUseCases.newTabSearch
                }

                if (parentSession is CustomTabSessionState) {
                    useCase.invoke(request.query)
                    requireActivity().startActivity(openInFenixIntent)
                } else {
                    useCase.invoke(request.query, parentSessionId = parentSession?.id)
                }
            },
            owner = this,
            view = view,
        )

        val accentHighContrastColor =
            ThemeManager.resolveAttribute(R.attr.actionPrimary, context)

        sitePermissionsFeature.set(
            feature = SitePermissionsFeature(
                context = context,
                storage = context.components.core.geckoSitePermissionsStorage,
                fragmentManager = parentFragmentManager,
                promptsStyling = SitePermissionsFeature.PromptsStyling(
                    gravity = getAppropriateLayoutGravity(),
                    shouldWidthMatchParent = true,
                    positiveButtonBackgroundColor = accentHighContrastColor,
                    positiveButtonTextColor = R.color.fx_mobile_text_color_action_primary,
                ),
                sessionId = customTabSessionId,
                onNeedToRequestPermissions = { permissions ->
                    requestPermissions(permissions, REQUEST_CODE_APP_PERMISSIONS)
                },
                onShouldShowRequestPermissionRationale = {
                    shouldShowRequestPermissionRationale(
                        it,
                    )
                },
                store = store,
            ),
            owner = this,
            view = view,
        )

        sitePermissionWifiIntegration.set(
            feature = SitePermissionsWifiIntegration(
                settings = context.settings(),
                wifiConnectionMonitor = context.components.wifiConnectionMonitor,
            ),
            owner = this,
            view = view,
        )

        // This component feature only works on Fenix when built on Mozilla infrastructure.
//        if (BuildConfig.MOZILLA_OFFICIAL) {
            webAuthnFeature.set(
                feature = WebAuthnFeature(
                    engine = requireComponents.core.engine,
                    activity = requireActivity(),
                    exitFullScreen = requireComponents.useCases.sessionUseCases.exitFullscreen::invoke,
                    currentTab = { store.state.selectedTabId },
                ),
                owner = this,
                view = view,
            )
//        }

        screenOrientationFeature.set(
            feature = ScreenOrientationFeature(
                engine = requireComponents.core.engine,
                activity = requireActivity(),
            ),
            owner = this,
            view = view,
        )

        context.settings().setSitePermissionSettingListener(viewLifecycleOwner) {
            // If the user connects to WIFI while on the BrowserFragment, this will update the
            // SitePermissionsRules (specifically autoplay) accordingly
            runIfFragmentIsAttached {
                assignSitePermissionsRules()
            }
        }
        assignSitePermissionsRules()

        fullScreenFeature.set(
            feature = FullScreenFeature(
                requireComponents.core.store,
                requireComponents.useCases.sessionUseCases,
                customTabSessionId,
                ::viewportFitChange,
                ::fullScreenChanged,
            ),
            owner = this,
            view = view,
        )

        closeFindInPageBarOnNavigation(store)

        store.flowScoped(viewLifecycleOwner) { flow ->
            flow.mapNotNull { state -> state.findTabOrCustomTabOrSelectedTab(customTabSessionId) }
                .distinctUntilChangedBy { tab -> tab.content.pictureInPictureEnabled }
                .collect { tab -> pipModeChanged(tab) }
        }

        binding.swipeRefresh.isEnabled = shouldPullToRefreshBeEnabled(false)

        if (binding.swipeRefresh.isEnabled) {
            val primaryTextColor = ThemeManager.resolveAttribute(R.attr.textPrimary, context)
            val primaryBackgroundColor = ThemeManager.resolveAttribute(R.attr.layer2, context)
            binding.swipeRefresh.apply {
                setColorSchemeResources(primaryTextColor)
                setProgressBackgroundColorSchemeResource(primaryBackgroundColor)
            }
            swipeRefreshFeature.set(
                feature = SwipeRefreshFeature(
                    requireComponents.core.store,
                    context.components.useCases.sessionUseCases.reload,
                    binding.swipeRefresh,
                    { },
                    customTabSessionId,
                ),
                owner = this,
                view = view,
            )
        }

        webchannelIntegration.set(
            feature = FxaWebChannelIntegration(
                customTabSessionId = customTabSessionId,
                runtime = requireComponents.core.engine,
                store = requireComponents.core.store,
                accountManager = requireComponents.backgroundServices.accountManager,
                serverConfig = requireComponents.backgroundServices.serverConfig,
                activityRef = WeakReference(getActivity()),
            ),
            owner = this,
            view = view,
        )

        initializeEngineView(
            topToolbarHeight = context.settings().getTopToolbarHeight(
                includeTabStrip = customTabSessionId == null && context.isTabStripEnabled(),
            ),
            bottomToolbarHeight = bottomToolbarHeight,
        )

        initializeMicrosurveyFeature(context)
    }

    private fun showUndoSnackbar(message: String) {
        viewLifecycleOwner.lifecycleScope.allowUndo(
            binding.dynamicSnackbarContainer,
            message,
            requireContext().getString(R.string.snackbar_deleted_undo),
            {
                requireComponents.useCases.tabsUseCases.undo.invoke()
            },
            operation = { },
        )
    }

    /**
     * Show a [Snackbar] when data is set to the device clipboard. To avoid duplicate displays of
     * information only show a [Snackbar] for Android 12 and lower.
     *
     * [See details](https://developer.android.com/develop/ui/views/touch-and-input/copy-paste#duplicate-notifications).
     */
    private fun showSnackbarForClipboardCopy() {
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.S_V2) {
            ContextMenuSnackbarDelegate().show(
                snackBarParentView = binding.dynamicSnackbarContainer,
                text = R.string.snackbar_copy_image_to_clipboard_confirmation,
                duration = LENGTH_LONG,
            )
        }
    }

    /**
     * Show a [Snackbar] when credentials are saved or updated.
     */
    private fun showSnackbarAfterLoginChange(isUpdate: Boolean) {
        val snackbarText = if (isUpdate) {
            R.string.mozac_feature_prompt_login_snackbar_username_updated
        } else {
            R.string.mozac_feature_prompts_suggest_strong_password_saved_snackbar_title
        }
        ContextMenuSnackbarDelegate().show(
            snackBarParentView = binding.dynamicSnackbarContainer,
            text = snackbarText,
            duration = LENGTH_LONG,
        )
    }

    /**
     * Shows a biometric prompt and fallback to prompting for the password.
     */
    private fun showBiometricPrompt(context: Context) {
        if (BiometricPromptFeature.canUseFeature(context)) {
            biometricPromptFeature.get()
                ?.requestAuthentication(getString(R.string.credit_cards_biometric_prompt_unlock_message_2))
            return
        }

        // Fallback to prompting for password with the KeyguardManager
        val manager = context.getSystemService<KeyguardManager>()
        if (manager?.isKeyguardSecure == true) {
            showPinVerification(manager)
        } else {
            // Warn that the device has not been secured
            if (context.settings().shouldShowSecurityPinWarning) {
                showPinDialogWarning(context)
            } else {
//                promptsFeature.get()?.onBiometricResult(isAuthenticated = true)
            }
        }
    }

    /**
     * Shows a pin request prompt. This is only used when BiometricPrompt is unavailable.
     */
    @Suppress("DEPRECATION")
    private fun showPinVerification(manager: KeyguardManager) {
        val intent = manager.createConfirmDeviceCredentialIntent(
            getString(R.string.credit_cards_biometric_prompt_message_pin),
            getString(R.string.credit_cards_biometric_prompt_unlock_message_2),
        )

        startForResult.launch(intent)
    }

    /**
     * Shows a dialog warning about setting up a device lock PIN.
     */
    private fun showPinDialogWarning(context: Context) {
        AlertDialog.Builder(context).apply {
            setTitle(getString(R.string.credit_cards_warning_dialog_title_2))
            setMessage(getString(R.string.credit_cards_warning_dialog_message_3))

            setNegativeButton(getString(R.string.credit_cards_warning_dialog_later)) { _: DialogInterface, _ ->
//                promptsFeature.get()?.onBiometricResult(isAuthenticated = false)
            }

            setPositiveButton(getString(R.string.credit_cards_warning_dialog_set_up_now)) { it: DialogInterface, _ ->
                it.dismiss()
//                promptsFeature.get()?.onBiometricResult(isAuthenticated = false)
                startActivity(Intent(Settings.ACTION_SECURITY_SETTINGS))
            }

            create()
        }.show().withCenterAlignedButtons().secure(activity)

        context.settings().incrementSecureWarningCount()
    }

    private fun closeFindInPageBarOnNavigation(store: BrowserStore) {
        consumeFlow(store) { flow ->
            flow.mapNotNull { state ->
                state.findCustomTabOrSelectedTab(customTabSessionId)
            }
                .ifAnyChanged { tab ->
                    arrayOf(tab.content.url, tab.content.loadRequest)
                }
                .collect {
                    findInPageIntegration.onBackPressed()
                }
        }
    }

    /**
     * Preserves current state of the [DynamicDownloadDialog] to persist through tab changes and
     * other fragments navigation.
     * */
    internal fun saveDownloadDialogState(
        sessionId: String?,
        downloadState: DownloadState,
        downloadJobStatus: DownloadState.Status,
    ) {
        sessionId?.let { id ->
            sharedViewModel.downloadDialogState[id] = Pair(
                downloadState,
                downloadJobStatus == DownloadState.Status.FAILED,
            )
        }
    }

    /**
     * Re-initializes [DynamicDownloadDialog] if the user hasn't dismissed the dialog
     * before navigating away from it's original tab.
     * onTryAgain it will use [ContentAction.UpdateDownloadAction] to re-enqueue the former failed
     * download, because [DownloadsFeature] clears any queued downloads onStop.
     * */
    @VisibleForTesting
    internal fun resumeDownloadDialogState(
        sessionId: String?,
        store: BrowserStore,
        context: Context,
    ) {
        val savedDownloadState =
            sharedViewModel.downloadDialogState[sessionId]

        if (savedDownloadState == null || sessionId == null) {
            binding.viewDynamicDownloadDialog.root.visibility = View.GONE
            return
        }

        val onTryAgain: (String) -> Unit = {
            savedDownloadState.first?.let { dlState ->
                store.dispatch(
                    ContentAction.UpdateDownloadAction(
                        sessionId,
                        dlState.copy(skipConfirmation = true),
                    ),
                )
            }
        }

        val onDismiss: () -> Unit =
            { sharedViewModel.downloadDialogState.remove(sessionId) }

        DynamicDownloadDialog(
            context = context,
            downloadState = savedDownloadState.first,
            didFail = savedDownloadState.second,
            tryAgain = onTryAgain,
            onCannotOpenFile = {
                showCannotOpenFileError(binding.dynamicSnackbarContainer, context, it)
            },
            binding = binding.viewDynamicDownloadDialog,
            onDismiss = onDismiss,
        ).show()

        browserToolbarView.expand()
    }

    @VisibleForTesting
    internal fun shouldPullToRefreshBeEnabled(inFullScreen: Boolean): Boolean {
        return /* FeatureFlags.pullToRefreshEnabled && */ requireContext().settings().isPullToRefreshEnabledInBrowser &&
                !inFullScreen
    }

    /**
     * Sets up the necessary layout configurations for the engine view. If the toolbar is dynamic, this method sets a
     * [CoordinatorLayout.Behavior] that will adjust the top/bottom paddings when the tab content is being scrolled.
     * If the toolbar is not dynamic, it simply sets the top and bottom margins to ensure that content is always
     * displayed above or below the respective toolbars.
     *
     * @param topToolbarHeight The height of the top toolbar, which could be zero if the toolbar is positioned at the
     * bottom, or it could be equal to the height of [BrowserToolbar].
     * @param bottomToolbarHeight The height of the bottom toolbar, which could be equal to the height of
     * [BrowserToolbar] or [ToolbarContainerView], or zero if the toolbar is positioned at the top without a navigation
     * bar.
     */
    @VisibleForTesting
    internal fun initializeEngineView(
        topToolbarHeight: Int,
        bottomToolbarHeight: Int,
    ) {
        val context = requireContext()

        if (isToolbarDynamic(context) && webAppToolbarShouldBeVisible) {
            getEngineView().setDynamicToolbarMaxHeight(topToolbarHeight + bottomToolbarHeight)

            if (context.settings().navigationToolbarEnabled || shouldShowMicrosurveyPrompt(context)) {
                (getSwipeRefreshLayout().layoutParams as CoordinatorLayout.LayoutParams).behavior =
                    EngineViewClippingBehavior(
                        context = context,
                        attrs = null,
                        engineViewParent = getSwipeRefreshLayout(),
                        topToolbarHeight = topToolbarHeight,
                    )
            } else {
                val toolbarPosition = when (context.settings().toolbarPosition) {
                    ToolbarPosition.BOTTOM -> OldToolbarPosition.BOTTOM
                    ToolbarPosition.TOP -> OldToolbarPosition.TOP
                }

                val toolbarHeight = when (toolbarPosition) {
                    OldToolbarPosition.BOTTOM -> bottomToolbarHeight
                    OldToolbarPosition.TOP -> topToolbarHeight
                }
                (getSwipeRefreshLayout().layoutParams as CoordinatorLayout.LayoutParams).behavior =
                    OldEngineViewClippingBehavior(
                        context,
                        null,
                        getSwipeRefreshLayout(),
                        toolbarHeight,
                        toolbarPosition,
                    )
            }
        } else {
            // Ensure webpage's bottom elements are aligned to the very bottom of the engineView.
            getEngineView().setDynamicToolbarMaxHeight(0)

            // Effectively place the engineView on top/below of the toolbars if that is not dynamic.
            val swipeRefreshParams =
                getSwipeRefreshLayout().layoutParams as CoordinatorLayout.LayoutParams
            swipeRefreshParams.topMargin = topToolbarHeight
            swipeRefreshParams.bottomMargin = bottomToolbarHeight
        }
    }

    @Suppress("LongMethod")
    private fun initializeNavBar(
        browserToolbar: BrowserToolbar,
        view: View,
        context: Context,
        activity: HomeActivity,
    ) {
//        NavigationBar.browserInitializeTimespan.start()

        val isToolbarAtBottom = context.isToolbarAtBottom()

        // The toolbar view has already been added directly to the container.
        // We should remove it and add the view to the navigation bar container.
        // Should refactor this so there is no added view to remove to begin with:
        // https://bugzilla.mozilla.org/show_bug.cgi?id=1870976
        if (isToolbarAtBottom) {
            binding.browserLayout.removeView(browserToolbar)
        }

        _bottomToolbarContainerView = BottomToolbarContainerView(
            context = context,
            parent = binding.browserLayout,
            hideOnScroll = isToolbarDynamic(context),
            content = {
                val areLoginBarsShown by remember { mutableStateOf(loginBarsIntegration.isVisible) }

                FirefoxTheme {
                    Column(
                        modifier = Modifier.background(FirefoxTheme.colors.layer1),
                    ) {
                        if (!activity.isMicrosurveyPromptDismissed.value) {
                            currentMicrosurvey?.let {
                                if (isToolbarAtBottom) {
                                    removeBottomToolbarDivider(browserToolbar)
                                }

                                HorizontalDivider()

                                MicrosurveyRequestPrompt(
                                    microsurvey = it,
                                    activity = activity,
                                    onStartSurveyClicked = {
                                        context.components.appStore.dispatch(
                                            MicrosurveyAction.Started(
                                                it.id
                                            )
                                        )
                                        findNavController().nav(
                                            R.id.browserComponentWrapperFragment,
                                            BrowserComponentWrapperFragmentDirections.actionGlobalMicrosurveyDialog(
                                                it.id
                                            ),
                                        )
                                    },
                                    onCloseButtonClicked = {
                                        context.components.appStore.dispatch(
                                            MicrosurveyAction.Dismissed(it.id),
                                        )

                                        context.settings().shouldShowMicrosurveyPrompt = false
                                        activity.isMicrosurveyPromptDismissed.value = true

                                        resumeDownloadDialogState(
                                            getCurrentTab()?.id,
                                            context.components.core.store,
                                            context,
                                        )
                                    },
                                )
                            }
                        } else {
                            restoreBottomToolbarDivider(browserToolbar)
                        }

                        if (isToolbarAtBottom) {
                            AndroidView(factory = { _ -> browserToolbar })
                        }

                        NavigationButtonsCFR(
                            context = context,
                            activity = activity,
                            showDivider = !isToolbarAtBottom && !areLoginBarsShown &&
                                    (currentMicrosurvey == null || activity.isMicrosurveyPromptDismissed.value),
                        )
                    }
                }
            },
        )

        bottomToolbarContainerIntegration.set(
            feature = BottomToolbarContainerIntegration(
                toolbar = bottomToolbarContainerView.toolbarContainerView,
                store = requireComponents.core.store,
                sessionId = customTabSessionId,
            ),
            owner = this,
            view = view,
        )

//        NavigationBar.browserInitializeTimespan.stop()
    }

    @Suppress("LongMethod")
    @Composable
    internal fun NavigationButtonsCFR(
        context: Context,
        activity: HomeActivity,
        showDivider: Boolean,
    ) {
        var showCFR by remember { mutableStateOf(false) }
        val lastTimeNavigationButtonsClicked = remember { mutableLongStateOf(0L) }

        // We need a second menu button, but we could reuse the existing builder.
        val menuButton = MenuButton(context).apply {
            menuBuilder = browserToolbarView.menuToolbar.menuBuilder
            // We have to set colorFilter manually as the button isn't being managed by a [BrowserToolbarView].
            setColorFilter(
                getColor(
                    context,
                    ThemeManager.resolveAttribute(R.attr.textPrimary, context),
                ),
            )
            recordClickEvent = { }
        }
        menuButton.setHighlightStatus()
        _menuButtonView = menuButton

        CFRPopupLayout(
            showCFR = showCFR && context.settings().shouldShowNavigationButtonsCFR,
            properties = CFRPopupProperties(
                popupBodyColors = listOf(
                    FirefoxTheme.colors.layerGradientEnd.toArgb(),
                    FirefoxTheme.colors.layerGradientStart.toArgb(),
                ),
                dismissButtonColor = FirefoxTheme.colors.iconOnColor.toArgb(),
                indicatorDirection = CFRPopup.IndicatorDirection.DOWN,
                popupVerticalOffset = NAVIGATION_CFR_VERTICAL_OFFSET.dp,
                indicatorArrowStartOffset = NAVIGATION_CFR_ARROW_OFFSET.dp,
                popupAlignment = CFRPopup.PopupAlignment.BODY_TO_ANCHOR_START_WITH_OFFSET,
            ),
            onCFRShown = {
                context.settings().shouldShowNavigationButtonsCFR = false
                context.settings().lastCfrShownTimeInMillis = System.currentTimeMillis()
            },
            onDismiss = {
            },
            text = {
                FirefoxTheme {
                    Text(
                        text = stringResource(R.string.navbar_navigation_buttons_cfr_message),
                        color = FirefoxTheme.colors.textOnColorPrimary,
                        style = FirefoxTheme.typography.body2,
                    )
                }
            },
        ) {
            val tabCounterMenu = lazy {
                FenixTabCounterMenu(
                    context = context,
                    onItemTapped = { item ->
                        browserToolbarInteractor.onTabCounterMenuItemTapped(item)
                    },
                    iconColor = when (activity.browsingModeManager.mode.isPrivate) {
                        true -> getColor(context, R.color.fx_mobile_private_icon_color_primary)
                        else -> null
                    },
                ).also {
                    it.updateMenu(
                        toolbarPosition = context.settings().toolbarPosition,
                    )
                }
            }

            BrowserNavBar(
                isPrivateMode = activity.browsingModeManager.mode.isPrivate,
                showDivider = showDivider,
                browserStore = context.components.core.store,
                menuButton = menuButton,
                newTabMenu = NewTabMenu(
                    context = context,
                    onItemTapped = { item ->
                        browserToolbarInteractor.onTabCounterMenuItemTapped(item)
                    },
                    iconColor = when (activity.browsingModeManager.mode.isPrivate) {
                        true -> getColor(context, R.color.fx_mobile_private_icon_color_primary)
                        else -> null
                    },
                ),
                tabsCounterMenu = tabCounterMenu,
                onBackButtonClick = {
                    if (context.settings().shouldShowNavigationButtonsCFR) {
                        val currentTime = System.currentTimeMillis()
                        if (currentTime - lastTimeNavigationButtonsClicked.longValue <=
                            NAVIGATION_CFR_MAX_MS_BETWEEN_CLICKS
                        ) {
                            showCFR = true
                        }
                        lastTimeNavigationButtonsClicked.longValue = currentTime
                    }
                    browserToolbarInteractor.onBrowserToolbarMenuItemTapped(
                        ToolbarMenu.Item.Back(viewHistory = false),
                    )
                },
                onBackButtonLongPress = {
                    browserToolbarInteractor.onBrowserToolbarMenuItemTapped(
                        ToolbarMenu.Item.Back(viewHistory = true),
                    )
                },
                onForwardButtonClick = {
                    if (context.settings().shouldShowNavigationButtonsCFR) {
                        val currentTime = System.currentTimeMillis()
                        if (currentTime - lastTimeNavigationButtonsClicked.longValue <=
                            NAVIGATION_CFR_MAX_MS_BETWEEN_CLICKS
                        ) {
                            showCFR = true
                        }
                        lastTimeNavigationButtonsClicked.longValue = currentTime
                    }
                    browserToolbarInteractor.onBrowserToolbarMenuItemTapped(
                        ToolbarMenu.Item.Forward(viewHistory = false),
                    )
                },
                onForwardButtonLongPress = {
                    browserToolbarInteractor.onBrowserToolbarMenuItemTapped(
                        ToolbarMenu.Item.Forward(viewHistory = true),
                    )
                },
                onNewTabButtonClick = {
                    browserToolbarInteractor.onNewTabButtonClicked()
                },
                onNewTabButtonLongPress = {
                    browserToolbarInteractor.onNewTabButtonLongClicked()
                },
                onTabsButtonClick = {
                    onTabCounterClicked(activity.browsingModeManager.mode)
                },
                onTabsButtonLongPress = {
                },
                onMenuButtonClick = {
                    findNavController().nav(
                        R.id.browserComponentWrapperFragment,
                        BrowserComponentWrapperFragmentDirections.actionGlobalMenuDialogFragment(
                            accesspoint = MenuAccessPoint.Browser,
                        ),
                    )
                },
                onVisibilityUpdated = {
                    configureEngineViewWithDynamicToolbarsMaxHeight()
                },
            )
        }
    }

    private fun onTabCounterClicked(browsingMode: BrowsingMode) {
        thumbnailsFeature.get()?.requestScreenshot()
        findNavController().nav(
            R.id.browserComponentWrapperFragment,
            BrowserComponentWrapperFragmentDirections.actionGlobalTabsTrayFragment(
                page = when (browsingMode) {
                    BrowsingMode.Normal -> Page.NormalTabs
                    BrowsingMode.Private -> Page.PrivateTabs
                },
            ),
        )
    }

    @VisibleForTesting
    internal fun initializeMicrosurveyFeature(context: Context) {
        if (context.settings().isExperimentationEnabled && context.settings().microsurveyFeatureEnabled) {
            messagingFeatureMicrosurvey.set(
                feature = MessagingFeature(
                    appStore = requireComponents.appStore,
                    surface = FenixMessageSurfaceId.MICROSURVEY,
                ),
                owner = viewLifecycleOwner,
                view = binding.root,
            )
        }
    }

    @Suppress("LongMethod")
    private fun initializeMicrosurveyPrompt() {
        val context = requireContext()
        val view = requireView()

        val isToolbarAtBottom = context.isToolbarAtBottom()
        val browserToolbar = browserToolbarView.view
        // The toolbar view has already been added directly to the container.
        // See initializeNavBar for more details on improving this.
        if (isToolbarAtBottom) {
            binding.browserLayout.removeView(browserToolbar)
        }

        _bottomToolbarContainerView = BottomToolbarContainerView(
            context = context,
            parent = binding.browserLayout,
            hideOnScroll = isToolbarDynamic(context),
            content = {
                FirefoxTheme {
                    Column {
                        val activity = requireActivity() as HomeActivity

                        if (!activity.isMicrosurveyPromptDismissed.value) {
                            currentMicrosurvey?.let {
                                if (isToolbarAtBottom) {
                                    removeBottomToolbarDivider(browserToolbar)
                                }

                                Divider()

                                MicrosurveyRequestPrompt(
                                    microsurvey = it,
                                    activity = activity,
                                    onStartSurveyClicked = {
                                        context.components.appStore.dispatch(
                                            MicrosurveyAction.Started(
                                                it.id
                                            )
                                        )
                                        findNavController().nav(
                                            R.id.browserComponentWrapperFragment,
                                            BrowserComponentWrapperFragmentDirections.actionGlobalMicrosurveyDialog(
                                                it.id
                                            ),
                                        )
                                    },
                                    onCloseButtonClicked = {
                                        context.components.appStore.dispatch(
                                            MicrosurveyAction.Dismissed(it.id),
                                        )

                                        context.settings().shouldShowMicrosurveyPrompt = false
                                        activity.isMicrosurveyPromptDismissed.value = true

                                        resumeDownloadDialogState(
                                            getCurrentTab()?.id,
                                            context.components.core.store,
                                            context,
                                        )
                                    },
                                )
                            }
                        } else {
                            restoreBottomToolbarDivider(browserToolbar)
                        }

                        if (isToolbarAtBottom) {
                            AndroidView(factory = { _ -> browserToolbar })
                        }
                    }
                }
            },
        ).apply {
            // This covers the usecase when the app goes into fullscreen mode from portrait orientation.
            // Transition to fullscreen happens first, and orientation change follows. Microsurvey container is getting
            // reinitialized when going into landscape mode, but it shouldn't be visible if the app is already in the
            // fullscreen mode. It still has to be initialized to be shown after the user exits the fullscreen.
            val isFullscreen = fullScreenFeature.get()?.isFullScreen == true
            toolbarContainerView.isVisible = !isFullscreen
        }

        bottomToolbarContainerIntegration.set(
            feature = BottomToolbarContainerIntegration(
                toolbar = bottomToolbarContainerView.toolbarContainerView,
                store = requireComponents.core.store,
                sessionId = customTabSessionId,
            ),
            owner = this,
            view = view,
        )

        reinitializeEngineView()
    }

    private fun removeBottomToolbarDivider(browserToolbar: BrowserToolbar) {
        val safeContext = context ?: return
        if (safeContext.isToolbarAtBottom()) {
            val drawable = ResourcesCompat.getDrawable(
                resources,
                R.drawable.toolbar_background_no_divider,
                null,
            )
            browserToolbar.background = drawable
            browserToolbar.elevation = 0.0f
        }
    }

    private fun restoreBottomToolbarDivider(browserToolbar: BrowserToolbar) {
        val safeContext = context ?: return
        if (safeContext.isToolbarAtBottom()) {
            val defaultBackground = ResourcesCompat.getDrawable(
                resources,
                R.drawable.toolbar_background,
                context?.theme,
            )
            browserToolbar.background = defaultBackground
        }
    }

    private fun updateNavbarDivider() {
        val safeContext = context ?: return

        // Evaluate showing the navbar divider only if addressbar is shown at the top
        // and the toolbar chrome should be is visible.
        if (!safeContext.isToolbarAtBottom() && webAppToolbarShouldBeVisible) {
            resetNavbar()
        }
    }

    /**
     * Build and show a new navbar.
     * Useful when needed to force an update of it's layout.
     */
    private fun resetNavbar() {
        if (context?.shouldAddNavigationBar() != true || !webAppToolbarShouldBeVisible) return

        // Prevent showing two navigation bars at the same time.
        _bottomToolbarContainerView?.toolbarContainerView?.let {
            binding.browserLayout.removeView(it)
        }
        reinitializeNavBar()
    }

    private var currentMicrosurvey: MicrosurveyUIData? = null

    /**
     * Listens for the microsurvey message and initializes the microsurvey prompt if one is available.
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    private fun listenForMicrosurveyMessage(context: Context) {
        binding.root.consumeFrom(context.components.appStore, viewLifecycleOwner) { state ->
            state.messaging.messageToShow[FenixMessageSurfaceId.MICROSURVEY]?.let { message ->
                if (message.id != currentMicrosurvey?.id) {
                    message.toMicrosurveyUIData()?.let { microsurvey ->
                        context.components.settings.shouldShowMicrosurveyPrompt = true
                        currentMicrosurvey = microsurvey

                        _bottomToolbarContainerView?.toolbarContainerView.let {
                            binding.browserLayout.removeView(it)
                        }

                        if (context.shouldAddNavigationBar()) {
                            reinitializeNavBar()
                        } else {
                            initializeMicrosurveyPrompt()
                        }
                    }
                }
            }
        }
    }

    private fun shouldShowMicrosurveyPrompt(context: Context) =
        context.components.settings.shouldShowMicrosurveyPrompt

    private fun isToolbarDynamic(context: Context) =
        !context.settings().shouldUseFixedTopToolbar && context.settings().isDynamicToolbarEnabled

    /**
     * Returns a list of context menu items [ContextMenuCandidate] for the context menu
     */
    protected abstract fun getContextMenuCandidates(
        context: Context,
        view: View,
    ): List<ContextMenuCandidate>

    @VisibleForTesting
    internal fun observeRestoreComplete(store: BrowserStore, navController: NavController) {
        val activity = activity as HomeActivity
        consumeFlow(store) { flow ->
            flow.map { state -> state.restoreComplete }
                .distinctUntilChanged()
                .collect { restored ->
                    if (restored) {
                        // Once tab restoration is complete, if there are no tabs to show in the browser, go home
                        val tabs =
                            store.state.getNormalOrPrivateTabs(
                                activity.browsingModeManager.mode.isPrivate,
                            )
                        if (tabs.isEmpty() || store.state.selectedTabId == null) {
                            navController.popBackStack(R.id.homeFragment, false)
                        }
                    }
                }
        }
    }

    @VisibleForTesting
    internal fun observeTabSelection(store: BrowserStore) {
        consumeFlow(store) { flow ->
            flow.distinctUntilChangedBy {
                it.selectedTabId
            }
                .mapNotNull {
                    it.selectedTab
                }
                .collect {
                    currentStartDownloadDialog?.dismiss()
                    handleTabSelected(it)
                }
        }
    }

    @VisibleForTesting
    @Suppress("ComplexCondition")
    internal fun observeTabSource(store: BrowserStore) {
        consumeFlow(store) { flow ->
            flow.mapNotNull { state ->
                state.selectedTab
            }
                .collect {
                    if (!requireComponents.fenixOnboarding.userHasBeenOnboarded() &&
                        it.content.loadRequest?.triggeredByRedirect != true &&
                        it.source !is SessionState.Source.External &&
                        it.content.url !in onboardingLinksList
                    ) {
                        requireComponents.fenixOnboarding.finish()
                    }
                }
        }
    }

    private fun handleTabSelected(selectedTab: TabSessionState) {
        if (!this.isRemoving) {
            updateThemeForSession(selectedTab)
        }

        if (browserInitialized) {
            view?.let {
                fullScreenChanged(false)
                browserToolbarView.expand()

                val context = requireContext()
                resumeDownloadDialogState(selectedTab.id, context.components.core.store, context)
                it.announceForAccessibility(selectedTab.toDisplayTitle())
            }
        } else {
            view?.let { view -> initializeUI(view) }
        }
    }

    @CallSuper
    override fun onResume() {
        super.onResume()
        val components = requireComponents

        val preferredColorScheme = components.core.getPreferredColorScheme()
        if (components.core.engine.settings.preferredColorScheme != preferredColorScheme) {
            components.core.engine.settings.preferredColorScheme = preferredColorScheme
            components.useCases.sessionUseCases.reload()
        }
        hideToolbar()

        components.services.appLinksInterceptor.updateFragmentManger(
            fragmentManager = parentFragmentManager,
        )
        context?.settings()?.shouldOpenLinksInApp(customTabSessionId != null)
            ?.let { openLinksInExternalApp ->
                components.services.appLinksInterceptor.updateLaunchInApp {
                    openLinksInExternalApp
                }
            }

        evaluateMessagesForMicrosurvey(components)
    }

    private fun evaluateMessagesForMicrosurvey(components: Components) =
        components.appStore.dispatch(MessagingAction.Evaluate(FenixMessageSurfaceId.MICROSURVEY))

    @CallSuper
    override fun onPause() {
        super.onPause()
        if (findNavController().currentDestination?.id != R.id.searchDialogFragment) {
            view?.hideKeyboard()
        }

        requireComponents.services.appLinksInterceptor.updateFragmentManger(
            fragmentManager = null,
        )
    }

    @CallSuper
    override fun onStop() {
        super.onStop()
        initUIJob?.cancel()
        currentStartDownloadDialog?.dismiss()

        requireComponents.core.store.state.findTabOrCustomTabOrSelectedTab(customTabSessionId)
            ?.let { session ->
                // If we didn't enter PiP, exit full screen on stop
                if (!session.content.pictureInPictureEnabled && fullScreenFeature.onBackPressed()) {
                    fullScreenChanged(false)
                }
            }
    }

    @CallSuper
    override fun onBackPressed(): Boolean {
        return findInPageIntegration.onBackPressed() ||
                fullScreenFeature.onBackPressed() ||
                promptsFeature.onBackPressed() ||
                currentStartDownloadDialog?.let {
                    it.dismiss()
                    true
                } ?: false ||
                sessionFeature.onBackPressed() ||
                removeSessionIfNeeded()
    }

    @CallSuper
    override fun onForwardPressed(): Boolean {
        return sessionFeature.onForwardPressed()
    }

    /**
     * Forwards activity results to the [ActivityResultHandler] features.
     */
    override fun onActivityResult(requestCode: Int, data: Intent?, resultCode: Int): Boolean {
        return listOf(
            promptsFeature,
            webAuthnFeature,
        ).any { it.onActivityResult(requestCode, data, resultCode) }
    }

    /**
     * Navigate to GlobalTabHistoryDialogFragment.
     */
    private fun navigateToGlobalTabHistoryDialogFragment() {
        findNavController().navigate(
            NavGraphDirections.actionGlobalTabHistoryDialogFragment(
                activeSessionId = customTabSessionId,
            ),
        )
    }

    override fun onBackLongPressed(): Boolean {
        navigateToGlobalTabHistoryDialogFragment()
        return true
    }

    override fun onForwardLongPressed(): Boolean {
        navigateToGlobalTabHistoryDialogFragment()
        return true
    }

    /**
     * Saves the external app session ID to be restored later in [onViewStateRestored].
     */
    final override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString(KEY_CUSTOM_TAB_SESSION_ID, customTabSessionId)
        outState.putString(LAST_SAVED_GENERATED_PASSWORD, lastSavedGeneratedPassword)
    }

    /**
     * Retrieves the external app session ID saved by [onSaveInstanceState].
     */
    final override fun onViewStateRestored(savedInstanceState: Bundle?) {
        super.onViewStateRestored(savedInstanceState)
        savedInstanceState?.getString(KEY_CUSTOM_TAB_SESSION_ID)?.let {
            if (requireComponents.core.store.state.findCustomTab(it) != null) {
                customTabSessionId = it
            }
        }
        lastSavedGeneratedPassword = savedInstanceState?.getString(LAST_SAVED_GENERATED_PASSWORD)
    }

    /**
     * Forwards permission grant results to one of the features.
     */
    @Deprecated("Deprecated in Java")
    final override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray,
    ) {
        val feature: PermissionsFeature? = when (requestCode) {
            REQUEST_CODE_DOWNLOAD_PERMISSIONS -> downloadsFeature.get()
            REQUEST_CODE_PROMPT_PERMISSIONS -> promptsFeature.get()
            REQUEST_CODE_APP_PERMISSIONS -> sitePermissionsFeature.get()
            else -> null
        }
        feature?.onPermissionsResult(permissions, grantResults)
    }

    /**
     * Removes the session if it was opened by an ACTION_VIEW intent
     * or if it has a parent session and no more history
     */
    protected open fun removeSessionIfNeeded(): Boolean {
        getCurrentTab()?.let { session ->
            return if (session.source is SessionState.Source.External && !session.restored) {
                activity?.finish()
                requireComponents.useCases.tabsUseCases.removeTab(session.id)
                true
            } else {
                val hasParentSession = session is TabSessionState && session.parentId != null
                if (hasParentSession) {
                    requireComponents.useCases.tabsUseCases.removeTab(
                        session.id,
                        selectParentIfExists = true
                    )
                }
                // We want to return to home if this session didn't have a parent session to select.
                val goToOverview = !hasParentSession
                !goToOverview
            }
        }
        return false
    }

    protected abstract fun navToQuickSettingsSheet(
        tab: SessionState,
        sitePermissions: SitePermissions?,
    )

    /**
     * Returns the layout [android.view.Gravity] for the quick settings and ETP dialog.
     */
    protected fun getAppropriateLayoutGravity(): Int =
        requireComponents.settings.toolbarPosition.androidGravity

    /**
     * Configure the engine view to know where to place website's dynamic elements
     * depending on the space taken by any dynamic toolbar.
     */
    @VisibleForTesting(otherwise = VisibleForTesting.PROTECTED)
    internal fun configureEngineViewWithDynamicToolbarsMaxHeight() {
        val currentTab =
            requireComponents.core.store.state.findCustomTabOrSelectedTab(customTabSessionId)
        if (currentTab?.content?.isPdf == true) return
        if (findInPageIntegration.get()?.isFeatureActive == true) return
        val toolbarHeights = view?.let { probeToolbarHeights(it) } ?: return

        context?.also {
            if (isToolbarDynamic(it)) {
                if (!requireComponents.core.geckoRuntime.isInteractiveWidgetDefaultResizesVisual) {
                    getEngineView().setDynamicToolbarMaxHeight(toolbarHeights.first + toolbarHeights.second)
                }
            } else {
                (getSwipeRefreshLayout().layoutParams as? CoordinatorLayout.LayoutParams)?.apply {
                    bottomMargin = toolbarHeights.second
                }
            }
        }
    }

    /**
     * Get an instant reading of the top toolbar height and the bottom toolbar height.
     */
    private fun probeToolbarHeights(rootView: View): Pair<Int, Int> {
        val context = rootView.context
        // Avoid any change for scenarios where the toolbar is not shown
        if (fullScreenFeature.get()?.isFullScreen == true) return 0 to 0

        val topToolbarHeight = context.settings().getTopToolbarHeight(
            includeTabStrip = customTabSessionId == null && context.isTabStripEnabled(),
        )
        val navbarHeight = context.resources.getDimensionPixelSize(R.dimen.browser_navbar_height)
        val isKeyboardShown = rootView.isKeyboardVisible()
        val bottomToolbarHeight = context.settings().getBottomToolbarHeight(context).minus(
            when (isKeyboardShown) {
                true -> navbarHeight // When keyboard is shown the navbar is expected to be hidden. Ignore it's height.
                false -> 0
            },
        )

        return topToolbarHeight to bottomToolbarHeight
    }

    /**
     * Updates the site permissions rules based on user settings.
     */
    private fun assignSitePermissionsRules() {
        val rules = requireComponents.settings.getSitePermissionsCustomSettingsRules()

        sitePermissionsFeature.withFeature {
            it.sitePermissionsRules = rules
        }
    }

    /**
     * Displays the quick settings dialog,
     * which lets the user control tracking protection and site settings.
     */
    private fun showQuickSettingsDialog() {
        val tab = getCurrentTab() ?: return
        viewLifecycleOwner.lifecycleScope.launch(Main) {
            val sitePermissions: SitePermissions? = tab.content.url.getOrigin()?.let { origin ->
                val storage = requireComponents.core.permissionStorage
                storage.findSitePermissionsBy(origin, tab.content.private)
            }

            view?.let {
                navToQuickSettingsSheet(tab, sitePermissions)
            }
        }
    }

    /**
     * Set the activity normal/private theme to match the current session.
     */
    @VisibleForTesting
    internal fun updateThemeForSession(session: SessionState) {
        val sessionMode = BrowsingMode.fromBoolean(session.content.private)
        (activity as HomeActivity).browsingModeManager.mode = sessionMode
    }

    /**
     * A safe version of [getCurrentTab] that safely checks for context nullability.
     */
    protected fun getSafeCurrentTab(): SessionState? {
        return context?.components?.core?.store?.state?.findCustomTabOrSelectedTab(
            customTabSessionId
        )
    }

    @VisibleForTesting
    internal fun getCurrentTab(): SessionState? {
        return requireComponents.core.store.state.findCustomTabOrSelectedTab(customTabSessionId)
    }

    private suspend fun bookmarkTapped(sessionUrl: String, sessionTitle: String) = withContext(IO) {
        val bookmarksStorage = requireComponents.core.bookmarksStorage
        val existing =
            bookmarksStorage.getBookmarksWithUrl(sessionUrl).firstOrNull { it.url == sessionUrl }
        if (existing != null) {
            // Bookmark exists, go to edit fragment
            withContext(Main) {
                nav(
                    R.id.browserComponentWrapperFragment,
                    BrowserComponentWrapperFragmentDirections.actionGlobalBookmarkEditFragment(
                        existing.guid,
                        true
                    ),
                )
            }
        } else {
            // Save bookmark, then go to edit fragment
            try {
                val parentNode = Result.runCatching {
                    val parentGuid = bookmarksStorage
                        .getRecentBookmarks(1)
                        .firstOrNull()
                        ?.parentGuid
                        ?: BookmarkRoot.Mobile.id

                    bookmarksStorage.getBookmark(parentGuid)!!
                }.getOrElse {
                    // this should be a temporary hack until the menu redesign is completed
                    // see MenuDialogMiddleware for the updated version
                    throw PlacesApiException.UrlParseFailed(reason = "no parent node")
                }

                val guid = bookmarksStorage.addItem(
                    parentNode.guid,
                    url = sessionUrl,
                    title = sessionTitle,
                    position = null,
                )

//                MetricsUtils.recordBookmarkMetrics(MetricsUtils.BookmarkAction.ADD, METRIC_SOURCE)
                showBookmarkSavedSnackbar(
                    message = getString(
                        R.string.bookmark_saved_in_folder_snackbar,
                        friendlyRootTitle(requireContext(), parentNode),
                    ),
                    onClick = {
//                        MetricsUtils.recordBookmarkMetrics(
//                            MetricsUtils.BookmarkAction.EDIT,
//                            TOAST_METRIC_SOURCE,
//                        )
                        findNavController().navigateWithBreadcrumb(
                            directions = BrowserComponentWrapperFragmentDirections.actionGlobalBookmarkEditFragment(
                                guid,
                                true,
                            ),
                            navigateFrom = "BrowserFragment",
                            navigateTo = "ActionGlobalBookmarkEditFragment",
                        )
                    },
                )
            } catch (e: PlacesApiException.UrlParseFailed) {
                withContext(Main) {
                    view?.let {
                        Snackbar.make(
                            snackBarParentView = binding.dynamicSnackbarContainer,
                            snackbarState = SnackbarState(
                                message = getString(R.string.bookmark_invalid_url_error),
                                duration = SnackbarState.Duration.Preset.Long,
                            ),
                        ).show()
                    }
                }
            }
        }
    }

    private fun showBookmarkSavedSnackbar(message: String, onClick: () -> Unit) {
        Snackbar.make(
            snackBarParentView = binding.dynamicSnackbarContainer,
            snackbarState = SnackbarState(
                message = message,
                duration = SnackbarState.Duration.Preset.Long,
                action = Action(
                    label = getString(R.string.edit_bookmark_snackbar_action),
                    onClick = onClick,
                ),
            ),
        ).show()
    }

    override fun onHomePressed() = pipFeature?.onHomePressed() ?: false

    /**
     * Exit fullscreen mode when exiting PIP mode
     */
    private fun pipModeChanged(session: SessionState) {
        if (!session.content.pictureInPictureEnabled && session.content.fullScreen && isAdded) {
            onBackPressed()
            fullScreenChanged(false)
        }
    }

    final override fun onPictureInPictureModeChanged(enabled: Boolean) {
        pipFeature?.onPictureInPictureModeChanged(enabled)
    }

    private fun viewportFitChange(layoutInDisplayCutoutMode: Int) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val layoutParams = activity?.window?.attributes
            layoutParams?.layoutInDisplayCutoutMode = layoutInDisplayCutoutMode
            activity?.window?.attributes = layoutParams
        }
    }

    @VisibleForTesting
    internal fun fullScreenChanged(inFullScreen: Boolean) {
        val activity = activity ?: return
        if (inFullScreen) {
            // Close find in page bar if opened
            findInPageIntegration.onBackPressed()

            FullScreenNotificationToast(
                activity = activity,
                gestureNavString = getString(R.string.exit_fullscreen_with_gesture_short),
                backButtonString = getString(R.string.exit_fullscreen_with_back_button_short),
                GestureNavUtils,
            ).show()

            activity.enterImmersiveMode(
                setOnApplyWindowInsetsListener = { key: String, listener: OnApplyWindowInsetsListener ->
                    binding.engineView.addWindowInsetsListener(key, listener)
                },
            )
            (view as? SwipeGestureLayout)?.isSwipeEnabled = false
            expandBrowserView()

        } else {
            activity.exitImmersiveMode(
                unregisterOnApplyWindowInsetsListener = binding.engineView::removeWindowInsetsListener,
            )

            (view as? SwipeGestureLayout)?.isSwipeEnabled = true
//            (activity as? HomeActivity)?.let { homeActivity ->
//                // ExternalAppBrowserActivity exclusively handles it's own theming unless in private mode.
//                if (homeActivity !is ExternalAppBrowserActivity || homeActivity.browsingModeManager.mode.isPrivate) {
//                    homeActivity.themeManager.applyStatusBarTheme(
//                        homeActivity,
//                        homeActivity.isTabStripEnabled()
//                    )
//                }
//            }
            collapseBrowserView()
        }

        binding.swipeRefresh.isEnabled = shouldPullToRefreshBeEnabled(inFullScreen)
    }

    @VisibleForTesting(otherwise = VisibleForTesting.PROTECTED)
    internal fun expandBrowserView() {
        browserToolbarView.apply {
            collapse()
            gone()
        }
        _bottomToolbarContainerView?.toolbarContainerView?.apply {
            collapse()
            isVisible = false
        }
        val browserEngine = getSwipeRefreshLayout().layoutParams as CoordinatorLayout.LayoutParams
        browserEngine.behavior = null
        browserEngine.bottomMargin = 0
        browserEngine.topMargin = 0
        getSwipeRefreshLayout().translationY = 0f

        getEngineView().apply {
            setDynamicToolbarMaxHeight(0)
            setVerticalClipping(0)
        }
    }

    @VisibleForTesting(otherwise = VisibleForTesting.PROTECTED)
    internal fun collapseBrowserView() {
        if (webAppToolbarShouldBeVisible) {
            browserToolbarView.visible()
            _bottomToolbarContainerView?.toolbarContainerView?.isVisible = true
            reinitializeEngineView()
            browserToolbarView.expand()
            _bottomToolbarContainerView?.toolbarContainerView?.expand()
        }
    }

    @CallSuper
    internal open fun onUpdateToolbarForConfigurationChange(toolbar: BrowserToolbarView) {
        toolbar.dismissMenu()

        // If the navbar feature could be visible, we should update it's state.
        val shouldUpdateNavBarState =
            requireContext().settings().navigationToolbarEnabled && webAppToolbarShouldBeVisible
        if (shouldUpdateNavBarState) {
            updateNavBarForConfigurationChange(
                context = requireContext(),
                parent = binding.browserLayout,
                toolbarView = browserToolbarView.view,
                bottomToolbarContainerView = _bottomToolbarContainerView?.toolbarContainerView,
                reinitializeNavBar = ::reinitializeNavBar,
                reinitializeMicrosurveyPrompt = ::initializeMicrosurveyPrompt,
            )
        }

        reinitializeEngineView()

        // If the microsurvey feature is visible, we should update it's state.
        if (shouldShowMicrosurveyPrompt(requireContext()) && !shouldUpdateNavBarState) {
            updateMicrosurveyPromptForConfigurationChange(
                parent = binding.browserLayout,
                bottomToolbarContainerView = _bottomToolbarContainerView?.toolbarContainerView,
                reinitializeMicrosurveyPrompt = ::initializeMicrosurveyPrompt,
            )
        }
    }

    private fun reinitializeNavBar() {
        initializeNavBar(
            browserToolbar = browserToolbarView.view,
            view = requireView(),
            context = requireContext(),
            activity = requireActivity() as HomeActivity,
        )
    }

    @VisibleForTesting
    internal fun reinitializeEngineView() {
        val isFullscreen = fullScreenFeature.get()?.isFullScreen == true
        val shouldToolbarsBeHidden = isFullscreen || !webAppToolbarShouldBeVisible
        val topToolbarHeight = requireContext().settings().getTopToolbarHeight(
            includeTabStrip = customTabSessionId == null && requireContext().isTabStripEnabled(),
        )
        val bottomToolbarHeight =
            requireContext().settings().getBottomToolbarHeight(requireContext())

        initializeEngineView(
            topToolbarHeight = if (shouldToolbarsBeHidden) 0 else topToolbarHeight,
            bottomToolbarHeight = if (shouldToolbarsBeHidden) 0 else bottomToolbarHeight,
        )
    }

    /*
     * Dereference these views when the fragment view is destroyed to prevent memory leaks
     */
    override fun onDestroyView() {
        super.onDestroyView()

        binding.engineView.setActivityContext(null)
        requireContext().accessibilityManager.removeAccessibilityStateChangeListener(this)

        _bottomToolbarContainerView = null
        _browserToolbarView = null
        _browserToolbarInteractor = null
        _binding = null
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
    }

    override fun onDetach() {
        super.onDetach()
    }

    internal fun showCannotOpenFileError(
        container: ViewGroup,
        context: Context,
        downloadState: DownloadState,
    ) {
        Snackbar.make(
            snackBarParentView = container,
            snackbarState = SnackbarState(
                message = DynamicDownloadDialog.getCannotOpenFileErrorMessage(
                    context,
                    downloadState
                ),
            ),
        ).show()
    }

    companion object {
        private const val KEY_CUSTOM_TAB_SESSION_ID = "custom_tab_session_id"
        private const val REQUEST_CODE_DOWNLOAD_PERMISSIONS = 1
        private const val REQUEST_CODE_PROMPT_PERMISSIONS = 2
        private const val REQUEST_CODE_APP_PERMISSIONS = 3
        private const val METRIC_SOURCE = "page_action_menu"
        private const val TOAST_METRIC_SOURCE = "add_bookmark_toast"
        private const val LAST_SAVED_GENERATED_PASSWORD = "last_saved_generated_password"

        val onboardingLinksList: List<String> = listOf(
            SupportUtils.getMozillaPageUrl(SupportUtils.MozillaPage.PRIVATE_NOTICE),
            SupportUtils.FXACCOUNT_SUMO_URL,
        )
    }

    override fun onAccessibilityStateChanged(enabled: Boolean) {
        registerForActivityResult { }
        if (_browserToolbarView != null) {
            browserToolbarView.setToolbarBehavior(enabled)
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)

        if (findInPageIntegration.get()?.isFeatureActive != true &&
            fullScreenFeature.get()?.isFullScreen != true
        ) {
            _browserToolbarView?.let {
                onUpdateToolbarForConfigurationChange(it)
            }
        }
    }

    // This method is called in response to native web extension messages from
    // content scripts (e.g the reader view extension). By the time these
    // messages are processed the fragment/view may no longer be attached.
    internal fun safeInvalidateBrowserToolbarView() {
        runIfFragmentIsAttached {
            val toolbarView = _browserToolbarView
            if (toolbarView != null) {
                toolbarView.view.invalidateActions()
                toolbarView.toolbarIntegration.invalidateMenu()
            }
            _menuButtonView?.setHighlightStatus()
        }
    }

    /**
     * Convenience method for replacing EngineView (id/engineView) in unit tests.
     */
    @VisibleForTesting
    internal fun getEngineView() = binding.engineView

    /**
     * Convenience method for replacing SwipeRefreshLayout (id/swipeRefresh) in unit tests.
     */
    @VisibleForTesting
    internal fun getSwipeRefreshLayout() = binding.swipeRefresh

    internal fun shouldShowCompletedDownloadDialog(
        downloadState: DownloadState,
        status: DownloadState.Status,
    ): Boolean {
        val isValidStatus =
            status in listOf(DownloadState.Status.COMPLETED, DownloadState.Status.FAILED)
        val isSameTab = downloadState.sessionId == (getCurrentTab()?.id ?: false)

        return isValidStatus && isSameTab
    }

    private fun handleOnSaveLoginWithGeneratedStrongPassword(
        passwordsStorage: SyncableLoginsStorage,
        url: String,
        password: String,
    ) {
        lastSavedGeneratedPassword = password
        val loginToSave = LoginEntry(
            origin = url,
            httpRealm = url,
            username = "",
            password = password,
        )
        var saveLoginJob: Deferred<Unit>? = null
        lifecycleScope.launch(IO) {
            saveLoginJob = async {
                try {
                    passwordsStorage.add(loginToSave)
                } catch (loginException: LoginsApiException) {
                    loginException.printStackTrace()
                    Log.e(
                        "Add new login",
                        "Failed to add new login with generated password.",
                        loginException,
                    )
                }
                saveLoginJob?.await()
            }
            saveLoginJob?.invokeOnCompletion {
                if (it is CancellationException) {
                    saveLoginJob?.cancel()
                }
            }
        }
    }

    private fun hideUpdateFragmentAfterSavingGeneratedPassword(
        username: String,
        password: String,
    ): Boolean {
        return username.isEmpty() && password == lastSavedGeneratedPassword
    }

    private fun removeLastSavedGeneratedPassword() {
        lastSavedGeneratedPassword = null
    }

    private fun navigateToSavedLoginsFragment() {
        val navController = findNavController()
        if (navController.currentDestination?.id == R.id.browserComponentWrapperFragment) {
            val directions = BrowserComponentWrapperFragmentDirections.actionLoginsListFragment()
            navController.navigate(directions)
        }
    }
}
