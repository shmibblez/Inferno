package com.shmibblez.inferno.browser

import android.annotation.SuppressLint
import android.app.Activity.RESULT_OK
import android.app.KeyguardManager
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.content.res.Resources
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.CallSuper
import androidx.annotation.IdRes
import androidx.annotation.VisibleForTesting
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.content.res.AppCompatResources
import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.FabPosition
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScaffoldDefaults
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.motionEventSpy
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.content.ContextCompat.getColor
import androidx.core.content.getSystemService
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.children
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.findFragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.Observer
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import androidx.navigation.NavDirections
import androidx.navigation.NavOptions
import androidx.preference.PreferenceManager
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.shmibblez.inferno.HomeActivity
import com.shmibblez.inferno.IntentReceiverActivity
import com.shmibblez.inferno.NavGraphDirections
import com.shmibblez.inferno.R
import com.shmibblez.inferno.browser.browsingmode.BrowsingMode
import com.shmibblez.inferno.browser.prompts.AndroidPhotoPicker
import com.shmibblez.inferno.browser.prompts.FilePicker
import com.shmibblez.inferno.browser.prompts.FilePicker.Companion.FILE_PICKER_ACTIVITY_REQUEST_CODE
import com.shmibblez.inferno.browser.prompts.PromptComponent
import com.shmibblez.inferno.browser.prompts.compose.FirstPartyDownloadBottomSheet
import com.shmibblez.inferno.browser.prompts.compose.ThirdPartyDownloadBottomSheet
import com.shmibblez.inferno.browser.tabstrip.isTabStripEnabled
import com.shmibblez.inferno.components.Components
import com.shmibblez.inferno.components.FindInPageIntegration
import com.shmibblez.inferno.components.TabCollectionStorage
import com.shmibblez.inferno.components.accounts.FenixFxAEntryPoint
import com.shmibblez.inferno.components.accounts.FxaWebChannelIntegration
import com.shmibblez.inferno.components.appstate.AppAction
import com.shmibblez.inferno.components.appstate.AppAction.MessagingAction
import com.shmibblez.inferno.components.appstate.AppAction.ShoppingAction
import com.shmibblez.inferno.components.toolbar.BottomToolbarContainerIntegration
import com.shmibblez.inferno.components.toolbar.BrowserFragmentStore
import com.shmibblez.inferno.components.toolbar.BrowserToolbarView
import com.shmibblez.inferno.components.toolbar.FenixTabCounterMenu
import com.shmibblez.inferno.components.toolbar.ToolbarContainerView
import com.shmibblez.inferno.components.toolbar.ToolbarIntegration
import com.shmibblez.inferno.components.toolbar.ToolbarPosition
import com.shmibblez.inferno.components.toolbar.interactor.BrowserToolbarInteractor
import com.shmibblez.inferno.components.toolbar.navbar.shouldAddNavigationBar
import com.shmibblez.inferno.components.toolbar.ui.createShareBrowserAction
import com.shmibblez.inferno.compose.snackbar.AcornSnackbarHostState
import com.shmibblez.inferno.compose.snackbar.Snackbar
import com.shmibblez.inferno.compose.snackbar.SnackbarHost
import com.shmibblez.inferno.customtabs.ExternalAppBrowserActivity
import com.shmibblez.inferno.downloads.DownloadService
import com.shmibblez.inferno.downloads.dialog.DynamicDownloadDialog
import com.shmibblez.inferno.ext.DEFAULT_ACTIVE_DAYS
import com.shmibblez.inferno.ext.accessibilityManager
import com.shmibblez.inferno.ext.components
import com.shmibblez.inferno.ext.consumeFlow
import com.shmibblez.inferno.ext.getPreferenceKey
import com.shmibblez.inferno.ext.isKeyboardVisible
import com.shmibblez.inferno.ext.isLargeWindow
import com.shmibblez.inferno.ext.isToolbarAtBottom
import com.shmibblez.inferno.ext.lastOpenedNormalTab
import com.shmibblez.inferno.ext.nav
import com.shmibblez.inferno.ext.navigateWithBreadcrumb
import com.shmibblez.inferno.ext.newTab
import com.shmibblez.inferno.ext.secure
import com.shmibblez.inferno.ext.settings
import com.shmibblez.inferno.findInPageBar.BrowserFindInPageBar
import com.shmibblez.inferno.home.CrashComponent
import com.shmibblez.inferno.home.HomeComponent
import com.shmibblez.inferno.home.HomeFragment
import com.shmibblez.inferno.library.bookmarks.friendlyRootTitle
import com.shmibblez.inferno.messaging.FenixMessageSurfaceId
import com.shmibblez.inferno.messaging.MessagingFeature
import com.shmibblez.inferno.microsurvey.ui.ext.MicrosurveyUIData
import com.shmibblez.inferno.mozillaAndroidComponents.feature.prompts.consumePromptFrom
import com.shmibblez.inferno.nimbus.FxNimbus
import com.shmibblez.inferno.perf.MarkersFragmentLifecycleCallbacks
import com.shmibblez.inferno.pip.PictureInPictureIntegration
import com.shmibblez.inferno.search.AwesomeBarWrapper
import com.shmibblez.inferno.settings.SupportUtils
import com.shmibblez.inferno.settings.biometric.BiometricPromptFeature
import com.shmibblez.inferno.settings.quicksettings.protections.cookiebanners.getCookieBannerUIMode
import com.shmibblez.inferno.shopping.DefaultShoppingExperienceFeature
import com.shmibblez.inferno.shopping.ReviewQualityCheckFeature
import com.shmibblez.inferno.shortcut.PwaOnboardingObserver
import com.shmibblez.inferno.tabbar.BrowserTabBar
import com.shmibblez.inferno.tabbar.toTabList
import com.shmibblez.inferno.tabs.LastTabFeature
import com.shmibblez.inferno.tabs.tabstray.InfernoTabsTray
import com.shmibblez.inferno.tabs.tabstray.InfernoTabsTrayDisplayType
import com.shmibblez.inferno.tabs.tabstray.InfernoTabsTrayMode
import com.shmibblez.inferno.tabs.tabstray.InfernoTabsTraySelectedTab
import com.shmibblez.inferno.tabstray.Page
import com.shmibblez.inferno.tabstray.TabsTrayFragmentDirections
import com.shmibblez.inferno.tabstray.ext.isActiveDownload
import com.shmibblez.inferno.tabstray.ext.isNormalTab
import com.shmibblez.inferno.theme.FirefoxTheme
import com.shmibblez.inferno.theme.ThemeManager
import com.shmibblez.inferno.toolbar.BrowserToolbar
import com.shmibblez.inferno.toolbar.ToolbarMenuBottomSheet
import com.shmibblez.inferno.wifi.SitePermissionsWifiIntegration
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import mozilla.appservices.places.BookmarkRoot
import mozilla.appservices.places.uniffi.PlacesApiException
import mozilla.components.browser.engine.gecko.GeckoEngineView
import mozilla.components.browser.state.action.BrowserAction
import mozilla.components.browser.state.action.ContentAction
import mozilla.components.browser.state.action.DebugAction
import mozilla.components.browser.state.action.LastAccessAction
import mozilla.components.browser.state.selector.findCustomTabOrSelectedTab
import mozilla.components.browser.state.selector.findTab
import mozilla.components.browser.state.selector.findTabOrCustomTab
import mozilla.components.browser.state.selector.findTabOrCustomTabOrSelectedTab
import mozilla.components.browser.state.selector.getNormalOrPrivateTabs
import mozilla.components.browser.state.selector.normalTabs
import mozilla.components.browser.state.selector.privateTabs
import mozilla.components.browser.state.selector.selectedTab
import mozilla.components.browser.state.state.BrowserState
import mozilla.components.browser.state.state.CustomTabSessionState
import mozilla.components.browser.state.state.SessionState
import mozilla.components.browser.state.state.TabSessionState
import mozilla.components.browser.state.state.content.DownloadState
import mozilla.components.browser.state.state.selectedOrDefaultSearchEngine
import mozilla.components.browser.state.store.BrowserStore
import mozilla.components.browser.thumbnails.BrowserThumbnails
import mozilla.components.browser.toolbar.BrowserToolbar
import mozilla.components.compose.cfr.CFRPopup
import mozilla.components.compose.cfr.CFRPopupLayout
import mozilla.components.compose.cfr.CFRPopupProperties
import mozilla.components.concept.engine.EngineView
import mozilla.components.concept.engine.mediasession.MediaSession
import mozilla.components.concept.engine.permission.SitePermissions
import mozilla.components.concept.engine.prompt.PromptRequest
import mozilla.components.concept.engine.prompt.PromptRequest.File
import mozilla.components.concept.engine.prompt.ShareData
import mozilla.components.concept.storage.LoginEntry
import mozilla.components.concept.toolbar.Toolbar
import mozilla.components.feature.app.links.AppLinksUseCases
import mozilla.components.feature.contextmenu.ContextMenuCandidate
import mozilla.components.feature.contextmenu.ContextMenuFeature
import mozilla.components.feature.downloads.DownloadsFeature
import mozilla.components.feature.downloads.manager.FetchDownloadManager
import mozilla.components.feature.downloads.temporary.CopyDownloadFeature
import mozilla.components.feature.downloads.temporary.ShareDownloadFeature
import mozilla.components.feature.downloads.ui.DownloaderApp
import mozilla.components.feature.findinpage.view.FindInPageBar
import mozilla.components.feature.media.fullscreen.MediaSessionFullscreenFeature
import mozilla.components.feature.privatemode.feature.SecureWindowFeature
import mozilla.components.feature.prompts.dialog.FullScreenNotificationToast
import mozilla.components.feature.prompts.dialog.GestureNavUtils
import mozilla.components.feature.prompts.identitycredential.DialogColors
import mozilla.components.feature.prompts.identitycredential.DialogColorsProvider
import mozilla.components.feature.prompts.login.PasswordGeneratorDialogColors
import mozilla.components.feature.prompts.login.PasswordGeneratorDialogColorsProvider
import mozilla.components.feature.prompts.share.ShareDelegate
import mozilla.components.feature.readerview.ReaderViewFeature
import mozilla.components.feature.readerview.view.ReaderViewControlsBar
import mozilla.components.feature.search.SearchFeature
import mozilla.components.feature.session.FullScreenFeature
import mozilla.components.feature.session.PictureInPictureFeature
import mozilla.components.feature.session.ScreenOrientationFeature
import mozilla.components.feature.session.SessionFeature
import mozilla.components.feature.session.SwipeRefreshFeature
import mozilla.components.feature.sitepermissions.SitePermissionsFeature
import mozilla.components.feature.tab.collections.TabCollection
import mozilla.components.feature.tabs.WindowFeature
import mozilla.components.feature.webauthn.WebAuthnFeature
import mozilla.components.lib.state.DelicateAction
import mozilla.components.lib.state.Store
import mozilla.components.lib.state.ext.flowScoped
import mozilla.components.lib.state.ext.observe
import mozilla.components.service.sync.autofill.DefaultCreditCardValidationDelegate
import mozilla.components.service.sync.logins.DefaultLoginValidationDelegate
import mozilla.components.service.sync.logins.LoginsApiException
import mozilla.components.service.sync.logins.SyncableLoginsStorage
import mozilla.components.support.base.feature.ActivityResultHandler
import mozilla.components.support.base.feature.ViewBoundFeatureWrapper
import mozilla.components.support.base.log.logger.Logger
import mozilla.components.support.ktx.android.view.enterImmersiveMode
import mozilla.components.support.ktx.android.view.exitImmersiveMode
import mozilla.components.support.ktx.android.view.hideKeyboard
import mozilla.components.support.ktx.kotlin.getOrigin
import mozilla.components.support.ktx.kotlinx.coroutines.flow.ifAnyChanged
import mozilla.components.support.utils.ext.isLandscape
import mozilla.components.ui.widgets.VerticalSwipeRefreshLayout
import mozilla.components.ui.widgets.withCenterAlignedButtons
import java.lang.ref.WeakReference
import java.util.concurrent.TimeUnit
import kotlin.math.abs
import kotlin.math.roundToInt
import mozilla.components.browser.toolbar.BrowserToolbar as BrowserToolbarCompat

// fixme, bugs:
//   - when opening a new page from a link in a tab, 2 tabs appear, very buggy
// todo: huge drawable with diagonal red and black
// todo: nav host should be base composable, BrowserComponent in this case since it is now base home screen
// todo: implement layout from fragment_browser.xml
// todo: from fragment_browser.xml, for below views first wrap views with AndroidView, then
//  progressively implement in compose
//   1. implement findInPageView
//   2. implement viewDynamicDownloadDialog
//   3. implement readerViewControlsBar https://searchfox.org/mozilla-central/source/mobile/android/android-components/components/feature/readerview/src/main/res/layout/mozac_feature_readerview_view.xml
//   7. implement loginSelectBar
//   8. implement suggestStrongPasswordBar
//   9. implement addressSelectBar AddressSelectBar
//   10. implement creditCardSelectBar CreditCardSelectBar
// completed, (todo: test)
//   4. crash_reporter_view CrashContentView (com.shmibblez.inferno/crashes/CrashContentIntegration.kt)
//   5. startDownloadDialogContainer
//   6. dynamicSnackbarContainer
//   11. tabPreview (TabPreview moved to toolbar (swipe to switch tabs))

// todo: test
//   - savedLoginsLauncher

// TODO:
//  - if not scrollable make bottom bar offset swipe up or down with scroll received
//  - implement biometric authentication in SelectableListPrompt for credit cards (urgent)
//  - implement composable FindInPageBar
//  - home page
//  - move to selected tab on start
//  - use nicer icons for toolbar options
//  - fix external app browser implementation
//  - improve splash screen
//  - add home page (look at firefox source code)
//  - add default search engines, select default
//    - bundle in app
//    - add search engine settings page
//      - add search engine editor which allows removing or adding search engines
//      - add way to select search engine
//  - toolbar
//    - revisit search engines, how to modify bundled?
//  - change from datastore preferences to datastore
//    - switch from: implementation "androidx.datastore:datastore-preferences:1.1.1"
//      to: implementation "androidx.datastore:datastore:1.1.1"
//  - create Mozilla Location Service (MLS) token and put in components/Core.kt
//  - BuildConfig.MLS_TOKEN
//  - color scheme, search for FirefoxTheme usages

//companion object {
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
//}

//companion object {
/**
 * Indicates weight of a page action. The lesser the weight, the closer it is to the URL.
 *
 * A weight of -1 indicates the position is not cared for and the action will be appended at the end.
 */
const val READER_MODE_WEIGHT = 1
const val TRANSLATIONS_WEIGHT = 2
const val REVIEW_QUALITY_CHECK_WEIGHT = 3
const val SHARE_WEIGHT = 4
const val RELOAD_WEIGHT = 5
const val OPEN_IN_ACTION_WEIGHT = 6
//}

private const val NAVIGATION_CFR_VERTICAL_OFFSET = 10
private const val NAVIGATION_CFR_ARROW_OFFSET = 24
private const val NAVIGATION_CFR_MAX_MS_BETWEEN_CLICKS = 5000

fun Context.getActivity(): AppCompatActivity? = when (this) {
    is AppCompatActivity -> this
    is ContextWrapper -> baseContext.getActivity()
    else -> null
}

fun nav(
    navController: NavController,
    @IdRes id: Int?,
    directions: NavDirections,
    options: NavOptions? = null
) {
    navController.nav(id, directions, options)
}

enum class BrowserComponentMode {
    TOOLBAR, FIND_IN_PAGE,
}

enum class BrowserComponentPageType {
    CRASH, ENGINE, HOME, HOME_PRIVATE
}

object ComponentDimens {
    val TOOLBAR_HEIGHT = 40.dp
    val TAB_BAR_HEIGHT = 30.dp
    val TAB_WIDTH = 95.dp
    val TAB_CORNER_RADIUS = 8.dp
    val FIND_IN_PAGE_BAR_HEIGHT = 50.dp
    val PROGRESS_BAR_HEIGHT = 1.dp
    fun BOTTOM_BAR_HEIGHT(browserComponentMode: BrowserComponentMode): Dp {
        return when (browserComponentMode) {
            BrowserComponentMode.TOOLBAR -> TOOLBAR_HEIGHT + TAB_BAR_HEIGHT
            BrowserComponentMode.FIND_IN_PAGE -> FIND_IN_PAGE_BAR_HEIGHT
        }
    }
}

internal interface DownloadDialogData;

internal data class FirstPartyDownloadDialogData(
    val filename: String,
    val contentSize: Long,
    val positiveButtonAction: () -> Unit,
    val negativeButtonAction: () -> Unit,
) : DownloadDialogData

internal data class ThirdPartyDownloadDialogData(
    val downloaderApps: List<DownloaderApp>,
    val onAppSelected: (DownloaderApp) -> Unit,
    val negativeButtonAction: () -> Unit,
) : DownloadDialogData

private fun resolvePageType(tabSessionState: TabSessionState?): BrowserComponentPageType {
    val url = tabSessionState?.content?.url
    return if (tabSessionState?.engineState?.crashed == true) BrowserComponentPageType.CRASH
    else if (url == "inferno:home" || url == "about:blank") // TODO: create const class and set base to inferno:home
        BrowserComponentPageType.HOME
    else if (url == "inferno:privatebrowsing" || url == "about:privatebrowsing")  // TODO: add to const class and set base to inferno:private
        BrowserComponentPageType.HOME_PRIVATE
    else BrowserComponentPageType.ENGINE

    // TODO: if home, show home page and load engineView in compose tree as hidden,
    //  if page then show engineView
}

/**
 * @param sessionId session id, from Moz BaseBrowserFragment
 */
@OptIn(
    ExperimentalComposeUiApi::class, ExperimentalCoroutinesApi::class, DelicateAction::class
)
@Composable
@SuppressLint(
    "UnusedMaterialScaffoldPaddingParameter", "VisibleForTests",
    "UnusedMaterial3ScaffoldPaddingParameter"
)
fun BrowserComponent(
    navController: NavController,
    sessionId: String?,
    setOnActivityResultHandler: ((OnActivityResultModel) -> Boolean) -> Unit,
    androidPhotoPicker: AndroidPhotoPicker,
    setFilePicker: (FilePicker) -> Unit
//    args: HomeFragmentArgs
) {
    Log.d("BrowserComponent", "rebuilt")
    val coroutineScope = rememberCoroutineScope()
    val lifecycleOwner = LocalLifecycleOwner.current
    val context = LocalContext.current
    val store = context.components.core.store
    val view = LocalView.current
//    val localConfiguration = LocalConfiguration.current
    val parentFragmentManager = context.getActivity()!!.supportFragmentManager
    val snackbarHostState = remember { AcornSnackbarHostState() }

    // browser state observer setup
    val localLifecycleOwner = LocalLifecycleOwner.current
    var browserStateObserver by remember {
        mutableStateOf<Store.Subscription<BrowserState, BrowserAction>?>(null)
    }
    var initialized by remember { mutableStateOf(false) }
    var tabList by remember { mutableStateOf(store.state.toTabList().first) }
    var currentTab by remember { mutableStateOf(store.state.selectedTab) }
    var pendingTabUpdate by remember { mutableStateOf(false) }
    var isPrivateSession by remember {
        mutableStateOf(
            store.state.selectedTab?.content?.private ?: false
        )
    }
    var searchEngine by remember { mutableStateOf(store.state.search.selectedOrDefaultSearchEngine!!) }
    var pageType by remember { mutableStateOf(resolvePageType(currentTab)) }
    var promptRequests by remember { mutableStateOf<List<PromptRequest>>(emptyList()) }
    var showTabsTray by remember { mutableStateOf(false) }
    var tabsTrayMode by remember { mutableStateOf<InfernoTabsTrayMode>(InfernoTabsTrayMode.Normal) }

    /* BrowserFragment vars */
    val windowFeature = ViewBoundFeatureWrapper<WindowFeature>()
//    val openInAppOnboardingObserver = ViewBoundFeatureWrapper<OpenInAppOnboardingObserver>()
    val reviewQualityCheckFeature = ViewBoundFeatureWrapper<ReviewQualityCheckFeature>()
//    val translationsBinding = ViewBoundFeatureWrapper<TranslationsBinding>()

    var readerModeAvailable = remember { mutableStateOf(false) }
    val (reviewQualityCheckAvailable, setReviewQualityCheckAvailable) = remember {
        mutableStateOf(
            false
        )
    }
    var translationsAvailable = remember { mutableStateOf(false) }

    var pwaOnboardingObserver: PwaOnboardingObserver? = null

//    @VisibleForTesting var leadingAction: BrowserToolbar.Button? = null
//    var forwardAction: BrowserToolbar.TwoStateButton? = null
//    var backAction: BrowserToolbar.TwoStateButton? = null
//    var refreshAction: BrowserToolbar.TwoStateButton? = null
    var isTablet: Boolean = false

    /* BrowserFragment  vars */

    /* BaseBrowserFragment vars */
    lateinit var browserFragmentStore: BrowserFragmentStore
    lateinit var browserAnimator: BrowserAnimator
    lateinit var startForResult: ActivityResultLauncher<Intent>

//    var _browserToolbarInteractor: BrowserToolbarInteractor? = null

    var (browserToolbarInteractor, setBrowserToolbarInteractor) = remember {
        mutableStateOf<BrowserToolbarInteractor?>(
            null
        )
    }

//    @VisibleForTesting
//    @Suppress("VariableNaming")
//    var _browserToolbarView: BrowserToolbarView? = null

//    @VisibleForTesting
//    val browserToolbarView: BrowserToolbarView
//    get() = _browserToolbarView!!

//    @VisibleForTesting(otherwise = VisibleForTesting.PROTECTED)
//    @Suppress("VariableNaming")
//    var _bottomToolbarContainerView: BottomToolbarContainerView? = null
//    val bottomToolbarContainerView: BottomToolbarContainerView
//    get() = _bottomToolbarContainerView!!

//    @Suppress("VariableNaming")
//    @VisibleForTesting
//    var _menuButtonView: MenuButton? = null

    val readerViewFeature = remember { ViewBoundFeatureWrapper<ReaderViewFeature>() }
    val thumbnailsFeature = remember { ViewBoundFeatureWrapper<BrowserThumbnails>() }

    @VisibleForTesting val messagingFeatureMicrosurvey = ViewBoundFeatureWrapper<MessagingFeature>()

    val sessionFeature = remember { ViewBoundFeatureWrapper<SessionFeature>() }
    val contextMenuFeature = remember { ViewBoundFeatureWrapper<ContextMenuFeature>() }
    val downloadsFeature = remember { ViewBoundFeatureWrapper<DownloadsFeature>() }
    val shareDownloadsFeature = remember { ViewBoundFeatureWrapper<ShareDownloadFeature>() }
    val copyDownloadsFeature = remember { ViewBoundFeatureWrapper<CopyDownloadFeature>() }
//    val promptsFeature = remember { ViewBoundFeatureWrapper<PromptFeature>() }
//    lateinit var loginBarsIntegration: LoginBarsIntegration

//    @VisibleForTesting
    val findInPageIntegration = ViewBoundFeatureWrapper<FindInPageIntegration>()
    val toolbarIntegration = ViewBoundFeatureWrapper<ToolbarIntegration>()
    val bottomToolbarContainerIntegration =
        ViewBoundFeatureWrapper<BottomToolbarContainerIntegration>()
    val sitePermissionsFeature = ViewBoundFeatureWrapper<SitePermissionsFeature>()
    val fullScreenFeature = ViewBoundFeatureWrapper<FullScreenFeature>()
    val swipeRefreshFeature = ViewBoundFeatureWrapper<SwipeRefreshFeature>()
    val webchannelIntegration = ViewBoundFeatureWrapper<FxaWebChannelIntegration>()
    val sitePermissionWifiIntegration = ViewBoundFeatureWrapper<SitePermissionsWifiIntegration>()
    val secureWindowFeature = ViewBoundFeatureWrapper<SecureWindowFeature>()
    var fullScreenMediaSessionFeature = ViewBoundFeatureWrapper<MediaSessionFullscreenFeature>()
    val searchFeature = ViewBoundFeatureWrapper<SearchFeature>()
    val webAuthnFeature = ViewBoundFeatureWrapper<WebAuthnFeature>()
    val screenOrientationFeature = ViewBoundFeatureWrapper<ScreenOrientationFeature>()
    val biometricPromptFeature = ViewBoundFeatureWrapper<BiometricPromptFeature>()
//    val crashContentIntegration = ViewBoundFeatureWrapper<CrashContentIntegration>()
//    val readerViewBinding = ViewBoundFeatureWrapper<ReaderViewBinding>()
//    val openInFirefoxBinding = ViewBoundFeatureWrapper<OpenInFirefoxBinding>()
//    val findInPageBinding = ViewBoundFeatureWrapper<FindInPageBinding>()
//    val snackbarBinding = ViewBoundFeatureWrapper<SnackbarBinding>()
//    val standardSnackbarErrorBinding =
//        ViewBoundFeatureWrapper<StandardSnackbarErrorBinding>()

    var pipFeature by remember { mutableStateOf<PictureInPictureFeature?>(null) }

    val (customTabSessionId, setCustomTabSessionId) = remember { mutableStateOf<String?>(null) }

    val (browserInitialized, setBrowserInitialized) = remember { mutableStateOf(false) }
    var initUIJob by remember { mutableStateOf<Job?>(null) }

    @VisibleForTesting(otherwise = VisibleForTesting.PROTECTED) var webAppToolbarShouldBeVisible =
        true

//    val sharedViewModel: SharedViewModel by activityViewModels()
//    val homeViewModel: HomeScreenViewModel by activityViewModels()

//    var currentStartDownloadDialog by remember { mutableStateOf<StartDownloadDialog?>(null) }
    // if not null, show corresponding download dialog
    var (downloadDialogData, setDownloadDialogData) = remember {
        mutableStateOf<DownloadDialogData?>(
            null
        )
    }

//    var savedLoginsLauncher =
//        rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
//            navigateToSavedLoginsFragment(navController)
//        } // ActivityResultLauncher<Intent>

    val (lastSavedGeneratedPassword, setLastSavedGeneratedPassword) = remember {
        mutableStateOf<String?>(
            null
        )
    }

    /// old component features
//    val appLinksFeature = remember { ViewBoundFeatureWrapper<AppLinksFeature>() }
//    val webExtensionPromptFeature =
//        remember { ViewBoundFeatureWrapper<WebExtensionPromptFeature>() }
    val sitePermissionFeature = remember { ViewBoundFeatureWrapper<SitePermissionsFeature>() }
    val pictureInPictureIntegration =
        remember { ViewBoundFeatureWrapper<PictureInPictureIntegration>() }

    val lastTabFeature = remember { ViewBoundFeatureWrapper<LastTabFeature>() }
//    val webExtToolbarFeature = remember { ViewBoundFeatureWrapper<WebExtensionToolbarFeature>() }

    var filePicker by remember { mutableStateOf<FilePicker?>(null) }

    // permission launchers
    val requestDownloadPermissionsLauncher: ActivityResultLauncher<Array<String>> =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { results ->
            val permissions = results.keys.toTypedArray()
            val grantResults = results.values.map {
                if (it) PackageManager.PERMISSION_GRANTED else PackageManager.PERMISSION_DENIED
            }.toIntArray()
            downloadsFeature.withFeature {
                it.onPermissionsResult(permissions, grantResults)
            }
        }
    val requestSitePermissionsLauncher: ActivityResultLauncher<Array<String>> =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { results ->
            val permissions = results.keys.toTypedArray()
            val grantResults = results.values.map {
                if (it) PackageManager.PERMISSION_GRANTED else PackageManager.PERMISSION_DENIED
            }.toIntArray()
            sitePermissionFeature.withFeature {
                it.onPermissionsResult(permissions, grantResults)
            }
        }
    val requestPromptsPermissionsLauncher: ActivityResultLauncher<Array<String>> =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { results ->
            val permissions = results.keys.toTypedArray()
            val grantResults = results.values.map {
                if (it) PackageManager.PERMISSION_GRANTED else PackageManager.PERMISSION_DENIED
            }.toIntArray()
//            promptsFeature.withFeature {
//                it.onPermissionsResult(permissions, grantResults)
//            }
            filePicker?.onPermissionsResult(permissions, grantResults)
        }


    /* BaseBrowserFragment vars */
    val backHandler = OnBackPressedHandler(
        context = context,
        readerViewFeature = readerViewFeature,
        findInPageIntegration = findInPageIntegration,
        fullScreenFeature = fullScreenFeature,
//        promptsFeature = promptsFeature,
        downloadDialogData = downloadDialogData,
        setDownloadDialogData = setDownloadDialogData,
        sessionFeature = sessionFeature,
    )

    // setup tab observer
    DisposableEffect(null) {
//        store.flowScoped { flow ->
//            flow.map { state -> state.toTabList().first }
//                .collect {
//                    tabList = it
//                }
//        }
        browserStateObserver = store.observe(localLifecycleOwner) {
            currentTab = it.selectedTab

            Log.d("BrowserComponent", "change in browser state")
            if (!initialized) {
                initialized = true
                return@observe
            }
            if (initialized) {
                // if no tab selected, false
                isPrivateSession = currentTab?.content?.private ?: false
                tabList =
                    if (isPrivateSession) it.privateTabs else it.normalTabs // it.toTabList().first
                // if no tab selected, select one
                if (currentTab == null && !pendingTabUpdate) {
                    if (tabList.isNotEmpty()) {
                        val lastNormalTabId =
                            context.components.core.store.state.lastOpenedNormalTab?.id
                        if (tabList.any { tab -> tab.id == lastNormalTabId }) {
                            context.components.useCases.tabsUseCases.selectTab(lastNormalTabId!!)
                        } else {
                            context.components.useCases.tabsUseCases.selectTab(tabList.last().id)
                        }
                    } else {
                        // if tab list empty add new tab
                        context.components.newTab(false)
                    }
                    pendingTabUpdate = true
                } else {
                    pendingTabUpdate = false
                }
                searchEngine = it.search.selectedOrDefaultSearchEngine!!
                pageType = resolvePageType(currentTab)
            }
        }

        onDispose {
            browserStateObserver!!.unsubscribe()
        }
    }

    val colorsProvider = DialogColorsProvider {
        DialogColors(
            title = ThemeManager.resolveAttributeColor(attribute = R.attr.textPrimary),
            description = ThemeManager.resolveAttributeColor(attribute = R.attr.textSecondary),
        )
    }

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

    // browser display mode
    val (browserMode, setBrowserMode) = remember {
        mutableStateOf(BrowserComponentMode.TOOLBAR)
    }

    // bottom sheet menu setup
    val (showMenuBottomSheet, setShowMenuBottomSheet) = remember { mutableStateOf(false) }

    if (downloadDialogData != null) {
        if (downloadDialogData is FirstPartyDownloadDialogData) {
            // show first party download dialog
            FirstPartyDownloadBottomSheet(downloadDialogData, setDownloadDialogData)
        }
        if (downloadDialogData is ThirdPartyDownloadDialogData) {
            // show third party download dialog
            ThirdPartyDownloadBottomSheet(downloadDialogData, setDownloadDialogData)
        }
    } else if (showMenuBottomSheet) {
        ToolbarMenuBottomSheet(
            tabSessionState = currentTab,
            setShowBottomMenuSheet = setShowMenuBottomSheet,
            setBrowserComponentMode = setBrowserMode,
            onNavToSettings = { navToSettings(navController) }
        )
    }



    if (showTabsTray) {
        // todo: missing functionality from [TabsTrayFragment], [DefaultTabsTrayInteractor], and [DefaultTabsTrayController]
        InfernoTabsTray(
            dismiss = { showTabsTray = false },
            mode = tabsTrayMode,
            setMode = { mode -> tabsTrayMode = mode },
            activeTabId = currentTab?.id,
            normalTabs = context.components.core.store.state.normalTabs,
            privateTabs = context.components.core.store.state.privateTabs,
            syncedTabs = context.components.core.store.state.normalTabs, // todo: synced tabs implementation
            recentlyClosedTabs = context.components.core.store.state.closedTabs,
            tabDisplayType = InfernoTabsTrayDisplayType.List,
            initiallySelectedTab = InfernoTabsTraySelectedTab.NormalTabs,
            onBookmarkSelectedTabsClick = {
                CoroutineScope(IO).launch {
                    Result.runCatching {
                        val bookmarksStorage = context.components.core.bookmarksStorage
                        val parentGuid = bookmarksStorage
                            .getRecentBookmarks(1)
                            .firstOrNull()
                            ?.parentGuid
                            ?: BookmarkRoot.Mobile.id

                        val parentNode = bookmarksStorage.getBookmark(parentGuid)

                        tabsTrayMode.selectedTabs.forEach { tab ->
                            bookmarksStorage.addItem(
                                parentGuid = parentNode!!.guid,
                                url = tab.content.url,
                                title = tab.content.title,
                                position = null,
                            )
                        }
                        withContext(Dispatchers.Main) {
                            // todo: showBookmarkSnackbar(mode.selectedTabs.size, parentNode?.title)
                        }
                    }.getOrElse {
                        // silently fail
                    }
                }
                tabsTrayMode = InfernoTabsTrayMode.Normal
            },
            onDeleteSelectedTabsClick = {
                /**
                 * Helper function to delete multiple tabs and offer an undo option.
                 */
                fun deleteMultipleTabs(tabs: Collection<TabSessionState>) {
                    val isPrivate = tabs.any { it.content.private }

                    // If user closes all the tabs from selected tabs page dismiss tray and navigate home.
                    if (tabs.size == context.components.core.store.state.getNormalOrPrivateTabs(
                            isPrivate
                        ).size
                    ) {
                        showTabsTray = false
//                        dismissTabsTrayAndNavigateHome(
//                            if (isPrivate) HomeFragment.ALL_PRIVATE_TABS else HomeFragment.ALL_NORMAL_TABS,
//                        )
                    } else {
                        tabs.map { it.id }.let {
                            context.components.useCases.tabsUseCases.removeTabs(it)
                        }
                    }
//                    todo: snackbar -> showUndoSnackbarForTab(isPrivate)
                }

                val tabs = tabsTrayMode.selectedTabs

//        TabsTray.closeSelectedTabs.record(TabsTray.CloseSelectedTabsExtra(tabCount = tabs.size))

                deleteMultipleTabs(tabs)

                tabsTrayMode = InfernoTabsTrayMode.Normal
            },
            onForceSelectedTabsAsInactiveClick = {
                val numDays: Long = DEFAULT_ACTIVE_DAYS + 1
                val tabs = tabsTrayMode.selectedTabs
                val currentTabId = context.components.core.store.state.selectedTabId
                tabs
                    .filterNot { it.id == currentTabId }
                    .forEach { tab ->
                        val daysSince = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(numDays)
                        context.components.core.store.apply {
                            dispatch(LastAccessAction.UpdateLastAccessAction(tab.id, daysSince))
                            dispatch(DebugAction.UpdateCreatedAtAction(tab.id, daysSince))
                        }
                    }

                tabsTrayMode = InfernoTabsTrayMode.Normal
            },
            onTabSettingsClick = {
                navController.navigate(
                    TabsTrayFragmentDirections.actionGlobalTabSettingsFragment(),
                )
            },
            onShareAllTabsClick = {
                // todo:
//                if (tabsTrayStore.state.selectedPage == Page.NormalTabs) {
//                    tabsTrayStore.dispatch(TabsTrayAction.ShareAllNormalTabs)
//                } else if (tabsTrayStore.state.selectedPage == Page.PrivateTabs) {
//                    tabsTrayStore.dispatch(TabsTrayAction.ShareAllPrivateTabs)
//                }
//
//                navigationInteractor.onShareTabsOfTypeClicked(
//                    private = tabsTrayStore.state.selectedPage == Page.PrivateTabs,
//                )
            },
            onDeleteAllTabsClick = {
//          // todo:
//                if (tabsTrayStore.state.selectedPage == Page.NormalTabs) {
//                    tabsTrayStore.dispatch(TabsTrayAction.CloseAllNormalTabs)
//                } else if (tabsTrayStore.state.selectedPage == Page.PrivateTabs) {
//                    tabsTrayStore.dispatch(TabsTrayAction.CloseAllPrivateTabs)
//                }
//
//                navigationInteractor.onCloseAllTabsClicked(
//                    private = tabsTrayStore.state.selectedPage == Page.PrivateTabs,
//                )
            },
            onAccountSettingsClick = {
                val isSignedIn =
                    context.components.backgroundServices.accountManager.authenticatedAccount() != null

                val direction = if (isSignedIn) {
                    TabsTrayFragmentDirections.actionGlobalAccountSettingsFragment()
                } else {
                    TabsTrayFragmentDirections.actionGlobalTurnOnSync(entrypoint = FenixFxAEntryPoint.NavigationInteraction)
                }
                navController.navigate(direction)
            },
            onTabClick = { tab ->
                fun getTabPositionFromId(tabsList: List<TabSessionState>, tabId: String): Int {
                    tabsList.forEachIndexed { index, tab -> if (tab.id == tabId) return index }
                    return -1
                }
                run outer@{
                    if (!context.settings().hasShownTabSwipeCFR &&
                        !context.isTabStripEnabled() &&
                        context.settings().isSwipeToolbarToSwitchTabsEnabled
                    ) {
                        val normalTabs = context.components.core.store.state.normalTabs
                        val currentTabId = currentTab?.id

                        if (normalTabs.size >= 2) {
                            val currentTabPosition = currentTabId
                                ?.let { getTabPositionFromId(normalTabs, it) }
                                ?: return@outer
                            val newTabPosition =
                                getTabPositionFromId(normalTabs, tab.id)

                            if (abs(currentTabPosition - newTabPosition) == 1) {
                                context.settings().shouldShowTabSwipeCFR = true
                            }
                        }
                    }
                }

                val selected = tabsTrayMode.selectedTabs
                when {
                    selected.isEmpty() && tabsTrayMode.isSelect().not() -> {
//                TabsTray.openedExistingTab.record(TabsTray.OpenedExistingTabExtra(source ?: "unknown"))
                        context.components.useCases.tabsUseCases.selectTab(tab.id)
                        val mode = BrowsingMode.fromBoolean(tab.content.private)
                        (context.getActivity()!! as HomeActivity).browsingModeManager.mode = mode
                        context.components.appStore.dispatch(AppAction.ModeChange(mode))
//                        handleNavigateToBrowser()
                        showTabsTray = false
                    }

                    tab.id in selected.map { it.id } -> {
                    // todo:
//                        handleTabUnselected(tab)
                    // aka:
                        // tabsTrayStore.dispatch(TabsTrayAction.RemoveSelectTab(tab))
                    }
                    // todo:
//                    source != TrayPagerAdapter.INACTIVE_TABS_FEATURE_NAME -> {
//                        tabsTrayStore.dispatch(TabsTrayAction.AddSelectTab(tab))
//                    }
                }

//                tabsTrayInteractor.onTabSelected(tab, TABS_TRAY_FEATURE_NAME)
            },
            onTabClose = { tab->
                fun deleteTab(tabId: String, source: String?, isConfirmed: Boolean) {
                    val browserStore = context.components.core.store
                    val tab = browserStore.state.findTab(tabId)
                    tab?.let {
                        val isLastTab = browserStore.state.getNormalOrPrivateTabs(it.content.private).size == 1
                        val isCurrentTab = browserStore.state.selectedTabId.equals(tabId)
                        if (!isLastTab || !isCurrentTab) {
                            context.components.useCases.tabsUseCases.removeTab(tabId)
//                            todo: snackbar showUndoSnackbarForTab(it.content.private)
                        } else {
                            val privateDownloads = browserStore.state.downloads.filter { map ->
                                map.value.private && map.value.isActiveDownload()
                            }
                            if (!isConfirmed && privateDownloads.isNotEmpty()) {
//                                todo: dialog showCancelledDownloadWarning(privateDownloads.size, tabId, source)
                                return
                            } else {
//                                dismissTabsTrayAndNavigateHome(tabId)
                                showTabsTray = false // todo: close tab
                                context.components.useCases.tabsUseCases.removeTab(tabId)
                            }
                        }
//            TabsTray.closedExistingTab.record(TabsTray.ClosedExistingTabExtra(source ?: "unknown"))
                    }

                    showTabsTray = false
                }

                deleteTab(tab.id, null, isConfirmed = false)
            },
            onTabMediaClick = {tab ->
                when (tab.mediaSessionState?.playbackState) {
                    MediaSession.PlaybackState.PLAYING -> {
//                GleanTab.mediaPause.record(NoExtras())
                        tab.mediaSessionState?.controller?.pause()
                    }

                    MediaSession.PlaybackState.PAUSED -> {
//                GleanTab.mediaPlay.record(NoExtras())
                        tab.mediaSessionState?.controller?.play()
                    }
                    else -> throw AssertionError(
                        "Play/Pause button clicked without play/pause state.",
                    )
                }
            },
            onTabMove = { tabId, targetId, placeAfter ->
                if (targetId != null && tabId != targetId) {
                    context.components.useCases.tabsUseCases.moveTabs(listOf(tabId), targetId, placeAfter)
                }
            },
            onTabLongClick = { tab ->
                if (tab.isNormalTab() && tabsTrayMode.selectedTabs.isEmpty()) {
//            Collections.longPress.record(NoExtras())
//                    todo: wtf to do here lol tabsTrayStore.dispatch(TabsTrayAction.AddSelectTab(tab))
//                    true
                } else {
//                    false
                }
            }
        )
    }

    /// views
    var engineView by remember { mutableStateOf<EngineView?>(null) }
    var toolbar by remember { mutableStateOf<BrowserToolbarCompat?>(null) }
    var findInPageBar by remember { mutableStateOf<FindInPageBar?>(null) }
    var swipeRefresh by remember { mutableStateOf<SwipeRefreshLayout?>(null) }
    var awesomeBar by remember { mutableStateOf<AwesomeBarWrapper?>(null) }
    var readerViewBar by remember { mutableStateOf<ReaderViewControlsBar?>(null) }
    var readerViewAppearanceButton by remember { mutableStateOf<FloatingActionButton?>(null) }

    /// event handlers
    val activityResultHandler: List<ViewBoundFeatureWrapper<*>> = listOf(
        webAuthnFeature,

//        promptsFeature,
    )
    // sets parent fragment handler for onActivityResult
    setOnActivityResultHandler { result: OnActivityResultModel ->
        Logger.info(
            "Fragment onActivityResult received with " + "requestCode: ${result.requestCode}, resultCode: ${result.resultCode}, data: ${result.data}",
        )

        // filePicker results
        with(result) {
            for (promptRequest in promptRequests) {
                if (requestCode == FILE_PICKER_ACTIVITY_REQUEST_CODE && promptRequest is File) {
                    store.consumePromptFrom(sessionId, promptRequest.uid) {
                        if (resultCode == RESULT_OK) {
                            filePicker!!.handleFilePickerIntentResult(data, promptRequest)
                        } else {
                            promptRequest.onDismiss()
                        }
                    }
                }
            }
        }
        // feature activity result handler
        activityResultHandler.any {
            it.onActivityResult(
                result.requestCode, result.data, result.resultCode
            )
        }

    }

    // connection to the nested scroll system and listen to the scroll
    val bottomBarHeightDp = ComponentDimens.BOTTOM_BAR_HEIGHT(browserMode)
    val bottomBarOffsetPx = remember { Animatable(0F) }
    val nestedScrollConnection = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(
                available: Offset, source: NestedScrollSource
            ): Offset {
                if (currentTab?.content?.loading == false) {
                    val delta = available.y
                    val newOffset = (bottomBarOffsetPx.value - delta).coerceIn(
                        0F, bottomBarHeightDp.toPx().toFloat()
                    )
                    coroutineScope.launch {
                        bottomBarOffsetPx.snapTo(newOffset)
                        engineView!!.setDynamicToolbarMaxHeight(bottomBarHeightDp.toPx() - newOffset.toInt())
                    }
                }
                return Offset.Zero
            }
        }
    }

    // on back pressed handlers
    BackHandler {
        onBackPressed(backHandler)
    }

    // moz components setup and shared preferences
    LaunchedEffect(engineView == null) {
        if (engineView == null) return@LaunchedEffect
        engineView!!.setDynamicToolbarMaxHeight(bottomBarHeightDp.toPx() - bottomBarOffsetPx.value.toInt())

        /* BaseBrowserFragment onViewCreated */
        // DO NOT ADD ANYTHING ABOVE THIS getProfilerTime CALL!
        val profilerStartTime = context.components.core.engine.profiler?.getProfilerTime()

        fun initializeUI() {
            val store = context.components.core.store
            val activity = context.getActivity()!! as HomeActivity

            // browser animations
//            browserAnimator = BrowserAnimator(
//                fragment = WeakReference(this),
//                engineView = WeakReference(engineView!!),
//                swipeRefresh = WeakReference(swipeRefresh!!),
//                viewLifecycleScope = WeakReference(coroutineScope),// viewLifecycleOwner.lifecycleScope),
//            ).apply {
//                beginAnimateInIfNecessary()
//            }

            val openInFenixIntent = Intent(context, IntentReceiverActivity::class.java).apply {
                action = Intent.ACTION_VIEW
                putExtra(HomeActivity.OPEN_TO_BROWSER, true)
            }

            // todo: readerView
//            val readerMenuController = DefaultReaderModeController(
//                readerViewFeature,
//                binding.readerViewControlsBar,
//                isPrivate = activity.browsingModeManager.mode.isPrivate,
//                onReaderModeChanged = { activity.finishActionMode() },
//            )
            // todo: toolbar
//    val browserToolbarController = DefaultBrowserToolbarController(
//        store = store,
//        appStore = context.components.appStore,
//        tabsUseCases = context.components.useCases.tabsUseCases,
//        activity = activity,
//        settings = context.settings(),
//        navController = navController,
//        readerModeController = readerMenuController,
//        engineView = binding.engineView,
//        homeViewModel = homeViewModel,
//        customTabSessionId = customTabSessionId,
//        browserAnimator = browserAnimator,
//        onTabCounterClicked = {
//            onTabCounterClicked(activity.browsingModeManager.mode)
//        },
//        onCloseTab = { closedSession ->
//            val closedTab =
//                store.state.findTab(closedSession.id) ?: return@DefaultBrowserToolbarController
//            showUndoSnackbar(context.tabClosedUndoMessage(closedTab.content.private))
//        },
//    )
//            val browserToolbarMenuController = DefaultBrowserToolbarMenuController(
//                fragment = this,
//                store = store,
//                appStore = context.components.appStore,
//                activity = activity,
//                navController = navController,
//                settings = context.settings(),
//                readerModeController = readerMenuController,
//                sessionFeature = sessionFeature,
//                findInPageLauncher = { findInPageIntegration.withFeature { it.launch() } },
//                browserAnimator = browserAnimator,
//                customTabSessionId = customTabSessionId,
//                openInFenixIntent = openInFenixIntent,
//                bookmarkTapped = { url: String, title: String ->
//                    lifecycleOwner.lifecycleScope.launch {
//                        bookmarkTapped(url, title)
//                    }
//                },
//                scope = lifecycleOwner.lifecycleScope,
//                tabCollectionStorage = context.components.core.tabCollectionStorage,
//                topSitesStorage = context.components.core.topSitesStorage,
//                pinnedSiteStorage = context.components.core.pinnedSiteStorage,
//            )
////
//            setBrowserToolbarInteractor(
//                DefaultBrowserToolbarInteractor(
//                    browserToolbarController,
//                    browserToolbarMenuController,
//                )
//            )
//
//    _browserToolbarView = BrowserToolbarView(
//        context = context,
//        container = binding.browserLayout,
//        snackbarParent = binding.dynamicSnackbarContainer,
//        settings = context.settings(),
//        interactor = browserToolbarInteractor,
//        customTabSession = customTabSessionId?.let { store.state.findCustomTab(it) },
//        lifecycleOwner = viewLifecycleOwner,
//        tabStripContent = {
//            FirefoxTheme {
//                TabStrip(
//                    onAddTabClick = {
//                        navController.navigate(
//                            NavGraphDirections.actionGlobalHome(
//                                focusOnAddressBar = true,
//                            ),
//                        )
//                    },
//                    onLastTabClose = { isPrivate ->
//                        context.components.appStore.dispatch(
//                            AppAction.TabStripAction.UpdateLastTabClosed(isPrivate),
//                        )
//                        navController.navigate(
//                            BrowserComponentWrapperFragmentDirections.actionGlobalHome(),
//                        )
//                    },
//                    onSelectedTabClick = {},
//                    onCloseTabClick = { isPrivate ->
//                        showUndoSnackbar(context.tabClosedUndoMessage(isPrivate))
//                    },
//                    onPrivateModeToggleClick = { mode ->
//                        activity.browsingModeManager.mode = mode
//                        navController.navigate(
//                            BrowserComponentWrapperFragmentDirections.actionGlobalHome(),
//                        )
//                    },
//                    onTabCounterClick = {
////                            onTabCounterClicked(activity.browsingModeManager.mode)
//                    },
//                )
//            }
//        },
//    )

            // todo: login bars
//    loginBarsIntegration = LoginBarsIntegration(
//        loginsBar = binding.loginSelectBar,
//        passwordBar = binding.suggestStrongPasswordBar,
//        settings = context.settings(),
//        onLoginsBarShown = {
//            removeBottomToolbarDivider(browserToolbarView.view)
//            updateNavbarDivider()
//        },
//        onLoginsBarHidden = {
//            restoreBottomToolbarDivider(browserToolbarView.view)
//            updateNavbarDivider()
//        },
//    )

            // todo: toolbar
//    val shouldAddNavigationBar = context.shouldAddNavigationBar() // && webAppToolbarShouldBeVisible
//    if (shouldAddNavigationBar) {
//        initializeNavBar(
//            browserToolbar = browserToolbarView.view,
//            view = view,
//            context = context,
//            activity = activity,
//        )
//    }

            if (context.settings().microsurveyFeatureEnabled) {
                listenForMicrosurveyMessage(context, lifecycleOwner)
            }

            // todo: toolbar
//    toolbarIntegration.set(
//        feature = browserToolbarView.toolbarIntegration,
//        owner = lifecycleOwner,
//        view = view,
//    )
            // todo: findInPage
//    findInPageIntegration.set(
//        feature = com.shmibblez.inferno.components.FindInPageIntegration(
//            store = store,
//            appStore = context.components.appStore,
//            sessionId = customTabSessionId,
//            view = binding.findInPageView,
//            engineView = binding.engineView,
//            toolbarsHideCallback = {
//                expandBrowserView()
//            },
//            toolbarsResetCallback = {
//                onUpdateToolbarForConfigurationChange(browserToolbarView)
//                collapseBrowserView()
//            },
//        ),
//        owner = lifecycleOwner,
//        view = view,
//    )
//
//    findInPageBinding.set(
//        feature = FindInPageBinding(
//            appStore = context.components.appStore,
//            onFindInPageLaunch = { findInPageIntegration.withFeature { it.launch() } },
//        ),
//        owner = lifecycleOwner,
//        view = view,
//    )

            // todo: readerView
//    readerViewBinding.set(
//        feature = ReaderViewBinding(
//            appStore = context.components.appStore,
//            readerMenuController = readerMenuController,
//        ),
//        owner = lifecycleOwner,
//        view = view,
//    )

            // todo: open in firefox
//    openInFirefoxBinding.set(
//        feature = OpenInFirefoxBinding(
//            activity = activity,
//            appStore = context.components.appStore,
//            customTabSessionId = customTabSessionId,
//            customTabsUseCases = context.components.useCases.customTabsUseCases,
//            openInFenixIntent = openInFenixIntent,
//            sessionFeature = sessionFeature,
//        ),
//        owner = lifecycleOwner,
//        view = view,
//    )

            // todo: toolbar
//    browserToolbarView.view.display.setOnSiteSecurityClickedListener {
//        showQuickSettingsDialog()
//    }

            // todo: context menu
//    contextMenuFeature.set(
//        feature = ContextMenuFeature(
//            fragmentManager = parentFragmentManager,
//            store = store,
//            candidates = getContextMenuCandidates(context, binding.dynamicSnackbarContainer),
//            engineView = binding.engineView,
//            useCases = context.components.useCases.contextMenuUseCases,
//            tabId = customTabSessionId,
//        ),
//        owner = lifecycleOwner,
//        view = view,
//    )

            val allowScreenshotsInPrivateMode = context.settings().allowScreenshotsInPrivateMode
            secureWindowFeature.set(
                feature = SecureWindowFeature(
                    window = context.getActivity()!!.window,
                    store = store,
                    customTabId = customTabSessionId,
                    isSecure = { !allowScreenshotsInPrivateMode && it.content.private },
                    clearFlagOnStop = false,
                ),
                owner = lifecycleOwner,
                view = view,
            )

            fullScreenMediaSessionFeature.set(
                feature = MediaSessionFullscreenFeature(
                    context.getActivity()!!,
                    context.components.core.store,
                    customTabSessionId,
                ),
                owner = lifecycleOwner,
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
                    showSnackbarForClipboardCopy(context, coroutineScope, snackbarHostState)
                },
            )

            val downloadFeature = DownloadsFeature(
                context.applicationContext,
                store = store,
                useCases = context.components.useCases.downloadUseCases,
                // todo: test since using parent frag manager
                fragmentManager = parentFragmentManager, // childFragmentManager,
                tabId = customTabSessionId,
                downloadManager = FetchDownloadManager(
                    context.applicationContext,
                    store,
                    DownloadService::class,
                    notificationsDelegate = context.components.notificationsDelegate,
                ),
                shouldForwardToThirdParties = {
                    PreferenceManager.getDefaultSharedPreferences(context).getBoolean(
                        context.getPreferenceKey(R.string.pref_key_external_download_manager),
                        false,
                    )
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
                    positiveButtonRadius = ComponentDimens.TAB_CORNER_RADIUS.value,
                ),
                onNeedToRequestPermissions = { permissions ->
                    // todo: test
                    requestDownloadPermissionsLauncher.launch(permissions)
//            requestPermissions(permissions, REQUEST_CODE_DOWNLOAD_PERMISSIONS)
                },
                customFirstPartyDownloadDialog = { filename, contentSize, positiveAction, negativeAction ->
                    run {
                        if (downloadDialogData == null) {
                            downloadDialogData = FirstPartyDownloadDialogData(
                                filename = filename.value,
                                contentSize = contentSize.value,
                                positiveButtonAction = positiveAction.value,
                                negativeButtonAction = negativeAction.value,
                            )
                        }
//                        if (currentStartDownloadDialog == null) {
//                            FirstPartyDownloadDialog(
//                                activity = context.getActivity()!!,
//                                filename = filename.value,
//                                contentSize = contentSize.value,
//                                positiveButtonAction = positiveAction.value,
//                                negativeButtonAction = negativeAction.value,
//                            ).onDismiss {
//                                currentStartDownloadDialog = null
//                            }.show(binding.startDownloadDialogContainer).also {
//                                currentStartDownloadDialog = it
//                            }
//                        }
                    }
                },
                customThirdPartyDownloadDialog = { downloaderApps, onAppSelected, negativeActionCallback ->
                    run {
                        if (downloadDialogData == null) {
                            downloadDialogData = ThirdPartyDownloadDialogData(
                                downloaderApps = downloaderApps.value,
                                onAppSelected = onAppSelected.value,
                                negativeButtonAction = negativeActionCallback.value
                            )
                        }
//                        if (currentStartDownloadDialog == null) {
//                            ThirdPartyDownloadDialog(
//                                activity = context.getActivity()!!,
//                                downloaderApps = downloaderApps.value,
//                                onAppSelected = onAppSelected.value,
//                                negativeButtonAction = negativeActionCallback.value,
//                            ).onDismiss {
//                                currentStartDownloadDialog = null
//                            }.show(binding.startDownloadDialogContainer).also {
//                                currentStartDownloadDialog = it
//                            }
//                        }
                    }
                },
            )

            val bottomToolbarHeight = context.settings().getBottomToolbarHeight(context)

            downloadFeature.onDownloadStopped = { downloadState, _, downloadJobStatus ->
                // todo: dialogs (in below function)
                // todo: toolbar
//                handleOnDownloadFinished(
//                    context = context,
//                    downloadState = downloadState,
//                    downloadJobStatus = downloadJobStatus,
//                    tryAgain = downloadFeature::tryAgain,
//                    browserToolbars = listOfNotNull(
//                        browserToolbarView,
//                        _bottomToolbarContainerView?.toolbarContainerView,
//                    ),
//                )
            }

            resumeDownloadDialogState(
                getCurrentTab(context)?.id,
                store,
                context,
            )

            shareDownloadsFeature.set(
                shareDownloadFeature,
                owner = lifecycleOwner,
                view = view,
            )

            copyDownloadsFeature.set(
                copyDownloadFeature,
                owner = lifecycleOwner,
                view = view,
            )

            downloadsFeature.set(
                downloadFeature,
                owner = lifecycleOwner,
                view = view,
            )

            pipFeature = PictureInPictureFeature(
                store = store,
                activity = context.getActivity()!!,
                crashReporting = context.components.crashReporter, // context.components.analytics.crashReporter,
                tabId = customTabSessionId,
            )

            biometricPromptFeature.set(
                feature = BiometricPromptFeature(
                    context = context,
                    fragment = view.findFragment(),
                    onAuthFailure = {
                        // todo: biometrics
//                        promptsFeature.get()?.onBiometricResult(isAuthenticated = false)
                    },
                    onAuthSuccess = {
                        // todo: biometrics
//                        promptsFeature.get()?.onBiometricResult(isAuthenticated = true)
                    },
                ),
                owner = lifecycleOwner,
                view = view,
            )

            val colorsProvider = DialogColorsProvider {
                DialogColors(
                    title = ThemeManager.resolveAttributeColor(attribute = R.attr.textPrimary),
                    description = ThemeManager.resolveAttributeColor(attribute = R.attr.textSecondary),
                )
            }

            sessionFeature.set(
                feature = SessionFeature(
                    context.components.core.store,
                    context.components.useCases.sessionUseCases.goBack,
                    context.components.useCases.sessionUseCases.goForward,
                    engineView!!, // binding.engineView,
                    customTabSessionId,
                ),
                owner = lifecycleOwner,
                view = view,
            )

//            crashContentIntegration.set(
//                feature = CrashContentIntegration(
//                    context = context,
//                    browserStore = context.components.core.store,
//                    appStore = context.components.appStore,
//                    toolbar = browserToolbarView.view,
//                    crashReporterView = binding.crashReporterView,
//                    components = context.components,
//                    settings = context.settings(),
//                    navController = navController,
//                    sessionId = customTabSessionId,
//                ),
//                owner = lifecycleOwner,
//                view = view,
//            )

            searchFeature.set(
                feature = SearchFeature(store, customTabSessionId) { request, tabId ->
                    val parentSession = store.state.findTabOrCustomTab(tabId)
                    val useCase = if (request.isPrivate) {
                        context.components.useCases.searchUseCases.newPrivateTabSearch
                    } else {
                        context.components.useCases.searchUseCases.newTabSearch
                    }

                    if (parentSession is CustomTabSessionState) {
                        useCase.invoke(request.query)
                        context.getActivity()!!.startActivity(openInFenixIntent)
                    } else {
                        useCase.invoke(request.query, parentSessionId = parentSession?.id)
                    }
                },
                owner = lifecycleOwner,
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
                        gravity = getAppropriateLayoutGravity(context),
                        shouldWidthMatchParent = true,
                        positiveButtonBackgroundColor = accentHighContrastColor,
                        positiveButtonTextColor = R.color.fx_mobile_text_color_action_primary,
                    ),
                    sessionId = customTabSessionId,
                    onNeedToRequestPermissions = { permissions ->
                        // todo: test
                        requestSitePermissionsLauncher.launch(permissions)
//                        requestPermissions(permissions, REQUEST_CODE_APP_PERMISSIONS)
                    },
                    onShouldShowRequestPermissionRationale = {
                        // todo: permissions
                        false
//                        shouldShowRequestPermissionRationale(
//                            it,
//                        )
                    },
                    store = store,
                ),
                owner = lifecycleOwner,
                view = view,
            )

            sitePermissionWifiIntegration.set(
                feature = SitePermissionsWifiIntegration(
                    settings = context.settings(),
                    wifiConnectionMonitor = context.components.wifiConnectionMonitor,
                ),
                owner = lifecycleOwner,
                view = view,
            )

            // This component feature only works on Fenix when built on Mozilla infrastructure.
            // sike
//            if (BuildConfig.MOZILLA_OFFICIAL) {
            webAuthnFeature.set(
                feature = WebAuthnFeature(
                    engine = context.components.core.engine,
                    activity = context.getActivity()!!,
                    exitFullScreen = context.components.useCases.sessionUseCases.exitFullscreen::invoke,
                    currentTab = { store.state.selectedTabId },
                ),
                owner = lifecycleOwner,
                view = view,
            )
//            }

            screenOrientationFeature.set(
                feature = ScreenOrientationFeature(
                    engine = context.components.core.engine,
                    activity = context.getActivity()!!,
                ),
                owner = lifecycleOwner,
                view = view,
            )

            // todo: site permissions
//            context.settings().setSitePermissionSettingListener(viewLifecycleOwner) {
//                // If the user connects to WIFI while on the BrowserFragment, this will update the
//                // SitePermissionsRules (specifically autoplay) accordingly
//                runIfFragmentIsAttached {
//                    assignSitePermissionsRules()
//                }
//            }
            assignSitePermissionsRules(context, sitePermissionFeature)

            fullScreenFeature.set(
                feature = FullScreenFeature(
                    context.components.core.store,
                    context.components.useCases.sessionUseCases,
                    customTabSessionId,
                    { viewportFitChange(it, context) },
                    { fullScreenChanged(false, context) },
                ),
                owner = lifecycleOwner,
                view = view,
            )

            closeFindInPageBarOnNavigation(
                store, lifecycleOwner, context, coroutineScope, findInPageIntegration
            )

            store.flowScoped(lifecycleOwner) { flow ->
                flow.mapNotNull { state ->
                    state.findTabOrCustomTabOrSelectedTab(
                        customTabSessionId
                    )
                }
                    .distinctUntilChangedBy { tab -> tab.content.pictureInPictureEnabled }
                    .collect { tab -> pipModeChanged(tab, context, backHandler) }
            }

            // todo: swipe refresh
//            binding.swipeRefresh.isEnabled = shouldPullToRefreshBeEnabled(false)
//
//            if (binding.swipeRefresh.isEnabled) {
//                val primaryTextColor = ThemeManager.resolveAttribute(R.attr.textPrimary, context)
//                val primaryBackgroundColor = ThemeManager.resolveAttribute(R.attr.layer2, context)
//                binding.swipeRefresh.apply {
//                    setColorSchemeResources(primaryTextColor)
//                    setProgressBackgroundColorSchemeResource(primaryBackgroundColor)
//                }
            swipeRefreshFeature.set(
                feature = SwipeRefreshFeature(
                    context.components.core.store,
                    context.components.useCases.sessionUseCases.reload,
                    swipeRefresh!!,
                    { },
                    customTabSessionId,
                ),
                owner = lifecycleOwner,
                view = view,
            )
//            }

            webchannelIntegration.set(
                feature = FxaWebChannelIntegration(
                    customTabSessionId = customTabSessionId,
                    runtime = context.components.core.engine,
                    store = context.components.core.store,
                    accountManager = context.components.backgroundServices.accountManager,
                    serverConfig = context.components.backgroundServices.serverConfig,
                    activityRef = WeakReference(context.getActivity()),
                ),
                owner = lifecycleOwner,
                view = view,
            )

            initializeEngineView(
                topToolbarHeight = context.settings().getTopToolbarHeight(
                    includeTabStrip = customTabSessionId == null && context.isTabStripEnabled(),
                ),
                bottomToolbarHeight = bottomToolbarHeight,
                context = context,
            )

            initializeMicrosurveyFeature(context, lifecycleOwner, messagingFeatureMicrosurvey)

            // TODO: super
//    super.initializeUI(view, tab)

            /* super */
            // TODO
//    val tab = getCurrentTab()
//    browserInitialized = if (tab != null) {
//        initializeUI(view, tab)
//        true
//    } else {
//        false
//    }
            /* super */

            val components = context.components

            updateBrowserToolbarLeadingAndNavigationActions(
                context = context,
                redesignEnabled = context.settings().navigationToolbarEnabled,
                isLandscape = context.isLandscape(),
                isTablet = isLargeWindow(context),
                isPrivate = (context.getActivity()!! as HomeActivity).browsingModeManager.mode.isPrivate,
                feltPrivateBrowsingEnabled = context.settings().feltPrivateBrowsingEnabled,
                isWindowSizeSmall = true, // AcornWindowSize.getWindowSize(context) == AcornWindowSize.Small,
            )

            updateBrowserToolbarMenuVisibility()

            initReaderMode(context, view)
            // todo: translation, browser toolbar interactor
//            initTranslationsAction(
//                context, view, browserToolbarInteractor!!, translationsAvailable.value
//            )
            initReviewQualityCheck(
                context,
                lifecycleOwner,
                view,
                navController,
                setReviewQualityCheckAvailable,
                reviewQualityCheckAvailable,
                reviewQualityCheckFeature
            )
            // todo: init share page action, browser toolbar interactor
//            initSharePageAction(context, browserToolbarInteractor)
            initReloadAction(context)

            thumbnailsFeature.set(
                feature = BrowserThumbnails(context, engineView!!, components.core.store),
                owner = lifecycleOwner,
                view = view,
            )

            windowFeature.set(
                feature = WindowFeature(
                    store = components.core.store,
                    tabsUseCases = components.useCases.tabsUseCases,
                ),
                owner = lifecycleOwner,
                view = view,
            )

//        if (context.settings().shouldShowOpenInAppCfr) {
//            openInAppOnboardingObserver.set(
//                feature = OpenInAppOnboardingObserver(
//                    context = context,
//                    store = context.components.core.store,
//                    lifecycleowner = lifecycleOwner,
//                    navController = navController,
//                    settings = context.settings(),
//                    appLinksUseCases = context.components.useCases.appLinksUseCases,
//                    container = binding.browserLayout as ViewGroup,
//                    shouldScrollWithTopToolbar = !context.settings().shouldUseBottomToolbar,
//                ),
//                owner = lifecycleOwner,
//                view = view,
//            )
//        }
        }
        initializeUI()

        observeTabSelection(
            context.components.core.store,
            context,
            lifecycleOwner,
            coroutineScope,
            setDownloadDialogData,
            browserInitialized,
        )

        if (!context.components.fenixOnboarding.userHasBeenOnboarded()) {
            observeTabSource(
                context.components.core.store,
                context,
                lifecycleOwner,
                coroutineScope
            )
        }

        // todo: accessibility
//        context.accessibilityManager.addAccessibilityStateChangeListener(view) // this)

        context.components.backgroundServices.closeSyncedTabsCommandReceiver.register(
            observer = CloseLastSyncedTabObserver(
                scope = coroutineScope, // viewLifecycleOwner.lifecycleScope,
                navController = navController,
            ),
            view = view,
        )

        // DO NOT MOVE ANYTHING BELOW THIS addMarker CALL!
        context.components.core.engine.profiler?.addMarker(
            MarkersFragmentLifecycleCallbacks.MARKER_NAME,
            profilerStartTime,
            "BaseBrowserFragment.onViewCreated",
        )/* BaseBrowserFragment onViewCreated */
    }

    // run only once
    LaunchedEffect(null) {
        filePicker = FilePicker(
            container = context.getActivity()!!,
            store = store,
            sessionId = customTabSessionId,
            fileUploadsDirCleaner = context.components.core.fileUploadsDirCleaner,
            androidPhotoPicker,
            onNeedToRequestPermissions = { permissions ->
                requestPromptsPermissionsLauncher.launch(
                    permissions
                )
            },
        )

        setFilePicker(filePicker!!)
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_CREATE -> {
//                    super.onCreate(savedInstanceState)
//                    savedLoginsLauncher = registerForActivityResult { navigateToSavedLoginsFragment(navController) }
                }

                Lifecycle.Event.ON_DESTROY -> {
//                    super.onDestroyView()
                    engineView!!.setActivityContext(null)
                    context.accessibilityManager.removeAccessibilityStateChangeListener(view.findFragment<BrowserComponentWrapperFragment>())

//                    _bottomToolbarContainerView = null
//                    _browserToolbarView = null
//                    _browserToolbarInteractor = null
//                    _binding = null


                    isTablet = false
//                    leadingAction = null
//                    forwardAction = null
//                    backAction = null
//                    refreshAction = null
                }

                Lifecycle.Event.ON_START -> {
//                    super.onStart()
//                    val context = context
                    val settings = context.settings()

                    if (!settings.userKnowsAboutPwas) {
                        pwaOnboardingObserver = PwaOnboardingObserver(
                            store = context.components.core.store,
                            lifecycleOwner = lifecycleOwner,
                            navController = navController,
                            settings = settings,
                            webAppUseCases = context.components.useCases.webAppUseCases,
                        ).also {
                            it.start()
                        }
                    }

                    subscribeToTabCollections(context, lifecycleOwner)
                    updateLastBrowseActivity(context)
                }

                Lifecycle.Event.ON_STOP -> {
//                    super.onStop()
                    initUIJob?.cancel()
                    setDownloadDialogData(null)
//                    currentStartDownloadDialog?.dismiss()

                    context.components.core.store.state.findTabOrCustomTabOrSelectedTab(
                        customTabSessionId
                    )?.let { session ->
                        // If we didn't enter PiP, exit full screen on stop
                        if (!session.content.pictureInPictureEnabled && fullScreenFeature.onBackPressed()) {
                            fullScreenChanged(false, context)
                        }
                    }

                    updateLastBrowseActivity(context)
                    updateHistoryMetadata(context)
                    pwaOnboardingObserver?.stop()
                }

                Lifecycle.Event.ON_PAUSE -> {
//                    super.onPause()
                    if (navController.currentDestination?.id != R.id.searchDialogFragment) {
                        view?.hideKeyboard()
                    }

                    context.components.services.appLinksInterceptor.updateFragmentManger(
                        fragmentManager = null,
                    )
                }

                Lifecycle.Event.ON_RESUME -> {
                    val components = context.components

                    val preferredColorScheme = components.core.getPreferredColorScheme()
                    if (components.core.engine.settings.preferredColorScheme != preferredColorScheme) {
                        components.core.engine.settings.preferredColorScheme =
                            preferredColorScheme
                        components.useCases.sessionUseCases.reload()
                    }
                    hideToolbar(context)

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

                    context.components.core.tabCollectionStorage.register(
                        collectionStorageObserver(
                            context, navController, view, coroutineScope, snackbarHostState
                        ),
                        lifecycleOwner,
                    )
                }

                else -> {

                }
            }

        }
        lifecycleOwner.lifecycle.addObserver(observer)

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    PromptComponent(
        setPromptRequests = { pr -> promptRequests = pr },
        currentTab = currentTab,
//        store = store,
        customTabId = customTabSessionId,
//        fragmentManager = parentFragmentManager,
        filePicker = filePicker,
//        identityCredentialColorsProvider = colorsProvider,
//        tabsUseCases = context.components.useCases.tabsUseCases,
//        fileUploadsDirCleaner = context.components.core.fileUploadsDirCleaner,
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
//        loginExceptionStorage = context.components.core.loginExceptionStorage,
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
                    sessionId = getCurrentTab(context)?.id,
                )
                navController.navigate(directions)
            }
        },
//        onNeedToRequestPermissions = { permissions ->
//            // todo: test
//            requestPromptsPermissionsLauncher.launch(permissions)
//        },
//        loginDelegate = object : LoginDelegate {
//            // todo: login delegate
////                        override val loginPickerView
////                            get() = binding.loginSelectBar
////                        override val onManageLogins = {
////                            browserAnimator.captureEngineViewAndDrawStatically {
////                                val directions = NavGraphDirections.actionGlobalSavedLoginsAuthFragment()
////                                navController.navigate(directions)
////                            }
////                        }
//        },
//        suggestStrongPasswordDelegate = object : SuggestStrongPasswordDelegate {
//            // todo: password delegate
////                        override val strongPasswordPromptViewListenerView
////                            get() = binding.suggestStrongPasswordBar
//        },
        shouldAutomaticallyShowSuggestedPassword = { context.settings().isFirstTimeEngagingWithSignup },
//        onFirstTimeEngagedWithSignup = {
//            context.settings().isFirstTimeEngagingWithSignup = false
//        },
//        onSaveLoginWithStrongPassword = { url, password ->
//            handleOnSaveLoginWithGeneratedStrongPassword(
//                passwordsStorage = context.components.core.passwordsStorage,
//                url = url,
//                password = password,
//                lifecycleScope = coroutineScope,
//                setLastSavedGeneratedPassword,
//            )
//        },
        onSaveLogin = { isUpdate ->
            showSnackbarAfterLoginChange(
                isUpdate,
                context,
                coroutineScope,
                snackbarHostState,
            )
        },
//        passwordGeneratorColorsProvider = passwordGeneratorColorsProvider,
        hideUpdateFragmentAfterSavingGeneratedPassword = { username, password ->
            hideUpdateFragmentAfterSavingGeneratedPassword(
                username,
                password,
                lastSavedGeneratedPassword,
            )
        },
        removeLastSavedGeneratedPassword = {
            removeLastSavedGeneratedPassword(
                setLastSavedGeneratedPassword
            )
        },
//        creditCardDelegate = object : CreditCardDelegate {
//            // todo: credit card delegate
////                        override val creditCardPickerView
////                            get() = binding.creditCardSelectBar
//            override val onManageCreditCards = {
//                val directions = NavGraphDirections.actionGlobalAutofillSettingFragment()
//                navController.navigate(directions)
//            }
//            override val onSelectCreditCard = {
//                // todo: biometrics
////                showBiometricPrompt(context, biometricPromptFeature, promptsFeature)
//            }
//        },
//        addressDelegate = object : AddressDelegate {
//            // todo: address delegate
//            override val addressPickerView
//                // todo: address select bar
//                get() = AddressSelectBar(context) // binding.addressSelectBar
//            override val onManageAddresses = {
//                val directions = NavGraphDirections.actionGlobalAutofillSettingFragment()
//                navController.navigate(directions)
//            }
//        },
//        androidPhotoPicker = androidPhotoPicker,
    )

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState = snackbarHostState) },
        contentWindowInsets = ScaffoldDefaults.contentWindowInsets,
        content = {
            Box(
                modifier = Modifier.fillMaxSize(),
            ) {
                MozAwesomeBar(setView = { ab -> awesomeBar = ab })
                MozEngineView(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(
                            start = 0.dp,
                            top = 0.dp,
                            end = 0.dp,
                            bottom = 0.dp
                        )
                        .nestedScroll(nestedScrollConnection)
                        .motionEventSpy {
                            if (it.action == MotionEvent.ACTION_UP || it.action == MotionEvent.ACTION_CANCEL) {
                                // set bottom bar position
                                coroutineScope.launch {
                                    if (bottomBarOffsetPx.value <= (bottomBarHeightDp.toPx() / 2)) {
                                        // if more than halfway up, go up
                                        bottomBarOffsetPx.animateTo(0F)
                                    } else {
                                        // if more than halfway down, go down
                                        bottomBarOffsetPx.animateTo(
                                            bottomBarHeightDp
                                                .toPx()
                                                .toFloat()
                                        )
                                    }
                                    engineView!!.setDynamicToolbarMaxHeight(0)
                                }
                            }
//                        else if (it.action == MotionEvent.ACTION_SCROLL) {
//                            // TODO: move nested scroll connection logic here
//                        }
                        },
                    setEngineView = { ev -> engineView = ev },
                    setSwipeView = { sr -> swipeRefresh = sr },
                )
                when (pageType) {
                    BrowserComponentPageType.HOME_PRIVATE -> {
                        HomeComponent(private = true)
                    }

                    BrowserComponentPageType.HOME -> {
                        HomeComponent(private = false)
                    }

                    BrowserComponentPageType.CRASH -> {
                        CrashComponent()
                    }

                    else -> {
                        // engine view already shown, kept there so engine view doesn't reset
                    }
                }
            }
        },
        floatingActionButtonPosition = FabPosition.End,
        floatingActionButton = {
            MozFloatingActionButton { fab -> readerViewAppearanceButton = fab }
        },
        bottomBar = {
            // hide and show when scrolling
            BottomAppBar(contentPadding = PaddingValues(0.dp),
                modifier = Modifier
                    .height(bottomBarHeightDp)
                    .offset {
                        IntOffset(
                            x = 0, y = bottomBarOffsetPx.value.roundToInt()
                        )
                    }) {
                Column(
                    Modifier
                        .fillMaxSize()
                        .background(Color.Black)
                ) {
                    if (browserMode == BrowserComponentMode.TOOLBAR) {
                        BrowserTabBar(tabList, currentTab)
                        BrowserToolbar(
                            tabSessionState = currentTab,
                            searchEngine = searchEngine,
                            tabCount = tabList.size,
                            setShowMenu = setShowMenuBottomSheet,
                            onNavToTabsTray = { showTabsTray({ showTabsTray = true }) }
                        )
                    }
                    if (browserMode == BrowserComponentMode.FIND_IN_PAGE) {
                        BrowserFindInPageBar()
                    }
                    MozFindInPageBar { fip -> findInPageBar = fip }
                    MozBrowserToolbar { bt -> toolbar = bt }
                    MozReaderViewControlsBar { cb -> readerViewBar = cb }
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height(10.dp)
                            .background(Color.Magenta)
                    )
                }
            }
        },
    )
}

fun hideToolbar(context: Context) {
    (context.getActivity()!! as AppCompatActivity).supportActionBar?.hide()
}

private fun viewportFitChanged(viewportFit: Int, context: Context) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        context.getActivity()!!.window.attributes.layoutInDisplayCutoutMode = viewportFit
    }
}

private fun fullScreenChanged(
    enabled: Boolean,
    context: Context,
    toolbar: BrowserToolbarCompat,
    engineView: EngineView,
    bottomBarTotalHeight: Int
) {
    if (enabled) {
        context.getActivity()?.enterImmersiveMode()
        toolbar.visibility = View.GONE
        engineView.setDynamicToolbarMaxHeight(0)
    } else {
        context.getActivity()?.exitImmersiveMode()
        toolbar.visibility = View.VISIBLE
        engineView.setDynamicToolbarMaxHeight(bottomBarTotalHeight)
    }
}

/*** navigation ***/
/**
 * navigate to tabs tray fragment
 */
private fun showTabsTray(showTabsTray: () -> Unit) {
    showTabsTray.invoke()
}

/**
 * navigate to settings fragment
 */
private fun navToSettings(nav: NavController) {
    nav.nav(
        R.id.browserComponentWrapperFragment,
        BrowserComponentWrapperFragmentDirections.actionGlobalSettingsFragment(),
    )
}

private data class OnBackPressedHandler(
    val context: Context,
    val readerViewFeature: ViewBoundFeatureWrapper<ReaderViewFeature>,
    val findInPageIntegration: ViewBoundFeatureWrapper<FindInPageIntegration>,
    val fullScreenFeature: ViewBoundFeatureWrapper<FullScreenFeature>,
//    val promptsFeature: ViewBoundFeatureWrapper<PromptFeature>,
    val downloadDialogData: DownloadDialogData?,
    val setDownloadDialogData: (DownloadDialogData?) -> Unit,
    val sessionFeature: ViewBoundFeatureWrapper<SessionFeature>,
)

// combines Moz BrowserFragment and Moz BaseBrowserFragment implementations
private fun onBackPressed(
    onBackPressedHandler: OnBackPressedHandler
): Boolean {
    with(onBackPressedHandler) {
        return readerViewFeature.onBackPressed() || findInPageIntegration.onBackPressed() || fullScreenFeature.onBackPressed() || /* promptsFeature.onBackPressed()  || */ downloadDialogData?.let {
            setDownloadDialogData(null)
            true
        } ?: false || sessionFeature.onBackPressed() || removeSessionIfNeeded(context)
    }
}

fun Dp.toPx(): Int {
    return (this.value * Resources.getSystem().displayMetrics.density).toInt()
}

@Composable
fun MozAwesomeBar(setView: (AwesomeBarWrapper) -> Unit) {
    AndroidView(modifier = Modifier
        .height(0.dp)
        .width(0.dp),
        factory = { context ->
            val v = AwesomeBarWrapper(context)
            setView(v)
            v
        },
        update = {
            it.visibility = View.GONE
            it.layoutParams.width = LayoutParams.MATCH_PARENT
            it.layoutParams.height = LayoutParams.MATCH_PARENT
            it.setPadding(4.dp.toPx(), 4.dp.toPx(), 4.dp.toPx(), 4.dp.toPx())
        })
}

@Composable
fun MozEngineView(
    modifier: Modifier = Modifier,
    setSwipeView: (VerticalSwipeRefreshLayout) -> Unit,
    setEngineView: (GeckoEngineView) -> Unit
) {
    AndroidView(modifier = modifier, factory = { context ->
        val vl = VerticalSwipeRefreshLayout(context)
        val gv = GeckoEngineView(context)
        setSwipeView(vl)
        setEngineView(gv)
        vl.layoutParams = LayoutParams(
            LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT
        )
        // todo: user prefs if swipe refresh enabled or not
        vl.visibility = View.VISIBLE
        vl.isEnabled = true
        vl.isActivated = true
        vl.isVisible = true
        vl.addView(gv)
        gv.layoutParams.width = LayoutParams.MATCH_PARENT
        gv.layoutParams.height = LayoutParams.MATCH_PARENT
        gv.setBackgroundColor(android.graphics.Color.TRANSPARENT)
        gv.visibility = View.VISIBLE
        gv.isEnabled = true
        gv.isActivated = true
        gv.isVisible = true
        vl
    }, update = { sv ->
        var gv: GeckoEngineView? = null
        for (v in sv.children) {
            if (v is GeckoEngineView) {
                gv = v
                break
            }
        }
//        // setup views
//        with(sv.layoutParams) {
//            this.width = LayoutParams.MATCH_PARENT
//            this.height = LayoutParams.MATCH_PARENT
//        }
//        with(gv!!.layoutParams) {
//            this.width = LayoutParams.MATCH_PARENT
//            this.height = LayoutParams.MATCH_PARENT
//        }
        gv!!.isEnabled = true
        gv.isActivated = true
    })
}

@Composable
fun MozBrowserToolbar(setView: (BrowserToolbarCompat) -> Unit) {
    AndroidView(modifier = Modifier
        .fillMaxWidth()
        .height(dimensionResource(id = R.dimen.browser_toolbar_height))
        .background(Color.Black)
        .padding(horizontal = 8.dp, vertical = 0.dp),
        factory = { context ->
            val v = BrowserToolbarCompat(context)
            setView(v)
            v
        },
        update = { bt ->
            bt.layoutParams.height = R.dimen.browser_toolbar_height
            bt.layoutParams.width = LayoutParams.MATCH_PARENT
            bt.visibility = View.VISIBLE
            bt.setBackgroundColor(0xFF0000)
            bt.displayMode()
        })
}

/**
 * @param setView function to set view variable in parent
 */
@Composable
fun MozFindInPageBar(setView: (FindInPageBar) -> Unit) {
    AndroidView(modifier = Modifier
        .fillMaxSize()
        .height(0.dp)
        .width(0.dp),
        factory = { context ->
            val v = FindInPageBar(context)
            setView(v)
            v
        },
        update = {
            it.visibility = View.GONE
        })
}

@Composable
fun MozReaderViewControlsBar(
    setView: (ReaderViewControlsBar) -> Unit
) {
    AndroidView(modifier = Modifier
        .fillMaxSize()
        .background(colorResource(id = R.color.toolbarBackgroundColor))
        .height(0.dp)
        .width(0.dp),
        factory = { context ->
            val v = ReaderViewControlsBar(context)
            setView(v)
            v
        },
        update = { it.visibility = View.GONE })
}

// reader view button, what this for?
@Composable
fun MozFloatingActionButton(
    setView: (FloatingActionButton) -> Unit
) {
    AndroidView(modifier = Modifier.fillMaxSize(),
        factory = { context ->
            val v = FloatingActionButton(context)
            setView(v)
            v
        },
        update = { it.visibility = View.GONE })
}

/* new functions *//* BaseBrowserFragment funs */
private fun getFragment(view: View): Fragment {
//    return this
    return view.findFragment()
}

//private fun getPromptsFeature(promptsFeature: ViewBoundFeatureWrapper<PromptFeature>): PromptFeature? {
//    return promptsFeature.get()
//}


private fun initializeUI(
    view: View,
    context: Context,
    setBrowserInitialized: (Boolean) -> Unit
) {
    val tab = getCurrentTab(context)
    setBrowserInitialized(
        if (tab != null) {
            initializeUI(view, context, setBrowserInitialized)// tab, context)
            true
        } else {
            false
        }
    )
}

@Suppress("ComplexMethod", "LongMethod", "DEPRECATION")
// https://github.com/mozilla-mobile/fenix/issues/19920
@CallSuper
internal fun initializeUI(
    view: View,
    tab: SessionState,
    context: Context,
    navController: NavController,
    lifecycleOwner: LifecycleOwner,
    readerViewFeature: ViewBoundFeatureWrapper<ReaderViewIntegration>,
) {

}

private fun showUndoSnackbar(
    message: String,
    context: Context,
    coroutineScope: CoroutineScope,
    lifecycleOwner: LifecycleOwner,
    snackbarHostState: AcornSnackbarHostState
) {
    coroutineScope.launch {
        val result = snackbarHostState.defaultSnackbarHostState.showSnackbar(
            message, actionLabel = context.getString(R.string.snackbar_deleted_undo), //"undo",
            duration = SnackbarDuration.Long
        )
        when (result) {
            SnackbarResult.ActionPerformed -> {
                context.components.useCases.tabsUseCases.undo.invoke()
            }

            SnackbarResult.Dismissed -> {
                // nothing
            }
        }
    }
}

/**
 * Show a [Snackbar] when data is set to the device clipboard. To avoid duplicate displays of
 * information only show a [Snackbar] for Android 12 and lower.
 *
 * [See details](https://developer.android.com/develop/ui/views/touch-and-input/copy-paste#duplicate-notifications).
 */
private fun showSnackbarForClipboardCopy(
    context: Context, coroutineScope: CoroutineScope, snackbarHostState: AcornSnackbarHostState
) {
    if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.S_V2) {
        coroutineScope.launch {
            snackbarHostState.defaultSnackbarHostState.showSnackbar(
                message = context.getString(R.string.snackbar_copy_image_to_clipboard_confirmation),
                duration = SnackbarDuration.Long,
            )
        }
    }
}

/**
 * Show a [Snackbar] when credentials are saved or updated.
 */
private fun showSnackbarAfterLoginChange(
    isUpdate: Boolean,
    context: Context,
    coroutineScope: CoroutineScope,
    snackbarHostState: AcornSnackbarHostState
) {
    val snackbarText = if (isUpdate) {
        R.string.mozac_feature_prompt_login_snackbar_username_updated
    } else {
        R.string.mozac_feature_prompts_suggest_strong_password_saved_snackbar_title
    }
    coroutineScope.launch {
        snackbarHostState.defaultSnackbarHostState.showSnackbar(
            message = context.getString(snackbarText),
            duration = SnackbarDuration.Long,
        )
    }
}

/**
 * Shows a biometric prompt and fallback to prompting for the password.
 */
private fun showBiometricPrompt(
    context: Context,
    biometricPromptFeature: ViewBoundFeatureWrapper<BiometricPromptFeature>,
//    promptFeature: ViewBoundFeatureWrapper<PromptFeature>
) {
    if (BiometricPromptFeature.canUseFeature(context)) {
        biometricPromptFeature.get()
            ?.requestAuthentication(context.getString(R.string.credit_cards_biometric_prompt_unlock_message_2))
        return
    }

    // Fallback to prompting for password with the KeyguardManager
    val manager = context.getSystemService<KeyguardManager>()
    if (manager?.isKeyguardSecure == true) {
        showPinVerification(manager, context)
    } else {
        // Warn that the device has not been secured
        if (context.settings().shouldShowSecurityPinWarning) {
            // todo: biometrics, integrate with prompt component for credit card select
//            showPinDialogWarning(context, promptFeature)
        } else {
//            promptFeature.get()?.onBiometricResult(isAuthenticated = true)
        }
    }
}

/**
 * Shows a pin request prompt. This is only used when BiometricPrompt is unavailable.
 */
@Suppress("DEPRECATION")
private fun showPinVerification(manager: KeyguardManager, context: Context) {
    val intent = manager.createConfirmDeviceCredentialIntent(
        context.getString(R.string.credit_cards_biometric_prompt_message_pin),
        context.getString(R.string.credit_cards_biometric_prompt_unlock_message_2),
    )

    // todo: start for result
//    startForResult.launch(intent)
}

/**
 * Shows a dialog warning about setting up a device lock PIN.
 */
private fun showPinDialogWarning(
    context: Context, // promptsFeature: ViewBoundFeatureWrapper<PromptFeature>
) {
    AlertDialog.Builder(context).apply {
        setTitle(context.getString(R.string.credit_cards_warning_dialog_title_2))
        setMessage(context.getString(R.string.credit_cards_warning_dialog_message_3))

        // todo: biometric
//        setNegativeButton(context.getString(R.string.credit_cards_warning_dialog_later)) { _: DialogInterface, _ ->
//            promptsFeature.get()?.onBiometricResult(isAuthenticated = false)
//        }

        // todo: biometric
//        setPositiveButton(context.getString(R.string.credit_cards_warning_dialog_set_up_now)) { it: DialogInterface, _ ->
//            it.dismiss()
//            promptsFeature.get()?.onBiometricResult(isAuthenticated = false)
//            context.getActivity()?.startActivity(Intent(Settings.ACTION_SECURITY_SETTINGS))
//        }

        create()
    }.show().withCenterAlignedButtons().secure(context.getActivity())

    context.settings().incrementSecureWarningCount()
}

private fun closeFindInPageBarOnNavigation(
    store: BrowserStore,
    lifecycleOwner: LifecycleOwner,
    context: Context,
    coroutineScope: CoroutineScope,
    findInPageIntegration: ViewBoundFeatureWrapper<FindInPageIntegration>,
    customTabSessionId: String? = null,
) {
    consumeFlow(store, lifecycleOwner, context, coroutineScope) { flow ->
        flow.mapNotNull { state ->
            state.findCustomTabOrSelectedTab(customTabSessionId)
        }.ifAnyChanged { tab ->
            arrayOf(tab.content.url, tab.content.loadRequest)
        }.collect {
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
        // todo: download
//        sharedViewModel.downloadDialogState[id] = Pair(
//            downloadState,
//            downloadJobStatus == DownloadState.Status.FAILED,
//        )
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
    // todo: download
//    val savedDownloadState = sharedViewModel.downloadDialogState[sessionId]
//
//    if (savedDownloadState == null || sessionId == null) {
//        binding.viewDynamicDownloadDialog.root.visibility = View.GONE
//        return
//    }
//
//    val onTryAgain: (String) -> Unit = {
//        savedDownloadState.first?.let { dlState ->
//            store.dispatch(
//                ContentAction.UpdateDownloadAction(
//                    sessionId,
//                    dlState.copy(skipConfirmation = true),
//                ),
//            )
//        }
//    }

    // todo: dismiss
//    val onDismiss: () -> Unit = { sharedViewModel.downloadDialogState.remove(sessionId) }

//    DynamicDownloadDialog(
//        context = context,
//        downloadState = savedDownloadState.first,
//        didFail = savedDownloadState.second,
//        tryAgain = onTryAgain,
//        onCannotOpenFile = {
//            showCannotOpenFileError(binding.dynamicSnackbarContainer, context, it)
//        },
//        binding = binding.viewDynamicDownloadDialog,
//        onDismiss = onDismiss,
//    ).show()

//    browserToolbarView.expand()
}

@VisibleForTesting
internal fun shouldPullToRefreshBeEnabled(inFullScreen: Boolean, context: Context): Boolean {
    return /* FeatureFlags.pullToRefreshEnabled && */ context.settings().isPullToRefreshEnabledInBrowser && !inFullScreen
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
    context: Context,
) {
    if (isToolbarDynamic(context)) { // && webAppToolbarShouldBeVisible) {
        // todo: engine view
//        getEngineView().setDynamicToolbarMaxHeight(topToolbarHeight + bottomToolbarHeight)

        if (context.settings().navigationToolbarEnabled || shouldShowMicrosurveyPrompt(context)) {
            // todo: swipe refresh
//            (getSwipeRefreshLayout().layoutParams as CoordinatorLayout.LayoutParams).behavior =
//                EngineViewClippingBehavior(
//                    context = context,
//                    attrs = null,
//                    engineViewParent = getSwipeRefreshLayout(),
//                    topToolbarHeight = topToolbarHeight,
//                )
        } else {
            val toolbarPosition = when (context.settings().toolbarPosition) {
                ToolbarPosition.BOTTOM -> mozilla.components.ui.widgets.behavior.ToolbarPosition.BOTTOM
                ToolbarPosition.TOP -> mozilla.components.ui.widgets.behavior.ToolbarPosition.TOP
            }

            val toolbarHeight = when (toolbarPosition) {
                mozilla.components.ui.widgets.behavior.ToolbarPosition.BOTTOM -> bottomToolbarHeight
                mozilla.components.ui.widgets.behavior.ToolbarPosition.TOP -> topToolbarHeight
            }
            // todo: swipe refresh
//            (getSwipeRefreshLayout().layoutParams as CoordinatorLayout.LayoutParams).behavior =
//                mozilla.components.ui.widgets.behavior.EngineViewClippingBehavior(
//                    context,
//                    null,
//                    getSwipeRefreshLayout(),
//                    toolbarHeight,
//                    toolbarPosition,
//                )
        }
    } else {
        // todo: engine view
//        // Ensure webpage's bottom elements are aligned to the very bottom of the engineView.
//        getEngineView().setDynamicToolbarMaxHeight(0)
//
//        // Effectively place the engineView on top/below of the toolbars if that is not dynamic.
//        val swipeRefreshParams =
//            getSwipeRefreshLayout().layoutParams as CoordinatorLayout.LayoutParams
//        swipeRefreshParams.topMargin = topToolbarHeight
//        swipeRefreshParams.bottomMargin = bottomToolbarHeight
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
    // todo: toolbar
//    if (isToolbarAtBottom) {
//        binding.browserLayout.removeView(browserToolbar)
//    }

    // todo: toolbar
//    _bottomToolbarContainerView = BottomToolbarContainerView(
//        context = context,
//        parent = binding.browserLayout,
//        hideOnScroll = isToolbarDynamic(context),
//        content = {
//            val areLoginBarsShown by remember { mutableStateOf(loginBarsIntegration.isVisible) }
//
//            FirefoxTheme {
//                Column(
//                    modifier = Modifier.background(FirefoxTheme.colors.layer1),
//                ) {
//                    if (!activity.isMicrosurveyPromptDismissed.value) {
//                        currentMicrosurvey?.let {
//                            if (isToolbarAtBottom) {
//                                removeBottomToolbarDivider(browserToolbar)
//                            }
//
//                            HorizontalDivider()
//
//                            MicrosurveyRequestPrompt(
//                                microsurvey = it,
//                                activity = activity,
//                                onStartSurveyClicked = {
//                                    context.components.appStore.dispatch(
//                                        MicrosurveyAction.Started(
//                                            it.id
//                                        )
//                                    )
//                                    navController.nav(
//                                        R.id.browserFragment,
//                                        BrowserComponentWrapperFragmentDirections.actionGlobalMicrosurveyDialog(
//                                            it.id
//                                        ),
//                                    )
//                                },
//                                onCloseButtonClicked = {
//                                    context.components.appStore.dispatch(
//                                        MicrosurveyAction.Dismissed(it.id),
//                                    )
//
//                                    context.settings().shouldShowMicrosurveyPrompt = false
//                                    activity.isMicrosurveyPromptDismissed.value = true
//
//                                    resumeDownloadDialogState(
//                                        getCurrentTab()?.id,
//                                        context.components.core.store,
//                                        context,
//                                    )
//                                },
//                            )
//                        }
//                    } else {
//                        restoreBottomToolbarDivider(browserToolbar)
//                    }
//
//                    if (isToolbarAtBottom) {
//                        AndroidView(factory = { _ -> browserToolbar })
//                    }
//
//                    NavigationButtonsCFR(
//                        context = context,
//                        activity = activity,
//                        showDivider = !isToolbarAtBottom && !areLoginBarsShown && (currentMicrosurvey == null || activity.isMicrosurveyPromptDismissed.value),
//                    )
//                }
//            }
//        },
//    )
//
//    bottomToolbarContainerIntegration.set(
//        feature = BottomToolbarContainerIntegration(
//            toolbar = bottomToolbarContainerView.toolbarContainerView,
//            store = context.components.core.store,
//            sessionId = customTabSessionId,
//        ),
//        owner = lifecycleOwner,
//        view = view,
//    )

//        NavigationBar.browserInitializeTimespan.stop()
}

@Suppress("LongMethod")
@Composable
internal fun NavigationButtonsCFR(
    context: Context,
    navController: NavController,
    activity: HomeActivity,
    showDivider: Boolean,
    browserToolbarInteractor: BrowserToolbarInteractor
) {
    var showCFR by remember { mutableStateOf(false) }
    val lastTimeNavigationButtonsClicked = remember { mutableLongStateOf(0L) }

    // todo: menu button
    // We need a second menu button, but we could reuse the existing builder.
//    val menuButton = MenuButton(context).apply {
//        menuBuilder = browserToolbarView.menuToolbar.menuBuilder
//        // We have to set colorFilter manually as the button isn't being managed by a [BrowserToolbarView].
//        setColorFilter(
//            getColor(
//                context,
//                ThemeManager.resolveAttribute(R.attr.textPrimary, context),
//            ),
//        )
//        recordClickEvent = { }
//    }
//    menuButton.setHighlightStatus()
//    _menuButtonView = menuButton

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
        onDismiss = {},
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
            // todo: tab counter
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

        // todo: navbar
//        BrowserNavBar(
//            isPrivateMode = activity.browsingModeManager.mode.isPrivate,
//            showDivider = showDivider,
//            browserStore = context.components.core.store,
//            menuButton = menuButton,
//            newTabMenu = NewTabMenu(
//                context = context,
//                onItemTapped = { item ->
//                    browserToolbarInteractor.onTabCounterMenuItemTapped(item)
//                },
//                iconColor = when (activity.browsingModeManager.mode.isPrivate) {
//                    true -> getColor(context, R.color.fx_mobile_private_icon_color_primary)
//                    else -> null
//                },
//            ),
//            tabsCounterMenu = tabCounterMenu,
//            onBackButtonClick = {
//                if (context.settings().shouldShowNavigationButtonsCFR) {
//                    val currentTime = System.currentTimeMillis()
//                    if (currentTime - lastTimeNavigationButtonsClicked.longValue <= NAVIGATION_CFR_MAX_MS_BETWEEN_CLICKS) {
//                        showCFR = true
//                    }
//                    lastTimeNavigationButtonsClicked.longValue = currentTime
//                }
//                browserToolbarInteractor.onBrowserToolbarMenuItemTapped(
//                    ToolbarMenu.Item.Back(viewHistory = false),
//                )
//            },
//            onBackButtonLongPress = {
//                browserToolbarInteractor.onBrowserToolbarMenuItemTapped(
//                    ToolbarMenu.Item.Back(viewHistory = true),
//                )
//            },
//            onForwardButtonClick = {
//                if (context.settings().shouldShowNavigationButtonsCFR) {
//                    val currentTime = System.currentTimeMillis()
//                    if (currentTime - lastTimeNavigationButtonsClicked.longValue <= NAVIGATION_CFR_MAX_MS_BETWEEN_CLICKS) {
//                        showCFR = true
//                    }
//                    lastTimeNavigationButtonsClicked.longValue = currentTime
//                }
//                browserToolbarInteractor.onBrowserToolbarMenuItemTapped(
//                    ToolbarMenu.Item.Forward(viewHistory = false),
//                )
//            },
//            onForwardButtonLongPress = {
//                browserToolbarInteractor.onBrowserToolbarMenuItemTapped(
//                    ToolbarMenu.Item.Forward(viewHistory = true),
//                )
//            },
//            onNewTabButtonClick = {
//                browserToolbarInteractor.onNewTabButtonClicked()
//            },
//            onNewTabButtonLongPress = {
//                browserToolbarInteractor.onNewTabButtonLongClicked()
//            },
//            onTabsButtonClick = {
//                onTabCounterClicked(activity.browsingModeManager.mode, navController, thumbnailsFeature)
//            },
//            onTabsButtonLongPress = {},
//            onMenuButtonClick = {
//                navController.nav(
//                    R.id.browserFragment,
//                    BrowserComponentWrapperFragmentDirections.actionGlobalMenuDialogFragment(
//                        accesspoint = MenuAccessPoint.Browser,
//                    ),
//                )
//            },
//            onVisibilityUpdated = {
//                configureEngineViewWithDynamicToolbarsMaxHeight(
//                    context, customTabSessionId, findInPageIntegration
//                )
//            },
//        )
    }
}

private fun onTabCounterClicked(
    browsingMode: BrowsingMode,
    navController: NavController,
    thumbnailsFeature: ViewBoundFeatureWrapper<BrowserThumbnails>
) {
    thumbnailsFeature.get()?.requestScreenshot()
    navController.nav(
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
internal fun initializeMicrosurveyFeature(
    context: Context,
    lifecycleOwner: LifecycleOwner,
    messagingFeatureMicrosurvey: ViewBoundFeatureWrapper<MessagingFeature>
) {
    if (context.settings().isExperimentationEnabled && context.settings().microsurveyFeatureEnabled) {
        // todo: microsurvey
//        messagingFeatureMicrosurvey.set(
//            feature = MessagingFeature(
//                appStore = context.components.appStore,
//                surface = FenixMessageSurfaceId.MICROSURVEY,
//            ),
//            owner = lifecycleOwner,
//            view = binding.root,
//        )
    }
}

@Suppress("LongMethod")
private fun initializeMicrosurveyPrompt(
    context: Context, view: View, fullScreenFeature: ViewBoundFeatureWrapper<FullScreenFeature>
) {
//    val context = context
//    val view = requireView()

    // todo: toolbar
//    val isToolbarAtBottom = context.isToolbarAtBottom()
//    val browserToolbar = browserToolbarView.view
//    // The toolbar view has already been added directly to the container.
//    // See initializeNavBar for more details on improving this.
//    if (isToolbarAtBottom) {
//        binding.browserLayout.removeView(browserToolbar)
//    }

//    _bottomToolbarContainerView = BottomToolbarContainerView(
//        context = context,
//        parent = binding.browserLayout,
//        hideOnScroll = isToolbarDynamic(context),
//        content = {
//            FirefoxTheme {
//                Column {
//                    val activity = context.getActivity()!! as HomeActivity
//
//                    if (!activity.isMicrosurveyPromptDismissed.value) {
//                        currentMicrosurvey?.let {
//                            if (isToolbarAtBottom) {
//                                removeBottomToolbarDivider(browserToolbar)
//                            }
//
//                            Divider()
//
//                            MicrosurveyRequestPrompt(
//                                microsurvey = it,
//                                activity = activity,
//                                onStartSurveyClicked = {
//                                    context.components.appStore.dispatch(
//                                        MicrosurveyAction.Started(
//                                            it.id
//                                        )
//                                    )
//                                    navController.nav(
//                                        R.id.browserFragment,
//                                        BrowserComponentWrapperFragmentDirections.actionGlobalMicrosurveyDialog(
//                                            it.id
//                                        ),
//                                    )
//                                },
//                                onCloseButtonClicked = {
//                                    context.components.appStore.dispatch(
//                                        MicrosurveyAction.Dismissed(it.id),
//                                    )
//
//                                    context.settings().shouldShowMicrosurveyPrompt = false
//                                    activity.isMicrosurveyPromptDismissed.value = true
//
//                                    resumeDownloadDialogState(
//                                        getCurrentTab()?.id,
//                                        context.components.core.store,
//                                        context,
//                                    )
//                                },
//                            )
//                        }
//                    } else {
//                        restoreBottomToolbarDivider(browserToolbar)
//                    }
//
//                    if (isToolbarAtBottom) {
//                        AndroidView(factory = { _ -> browserToolbar })
//                    }
//                }
//            }
//        },
//    ).apply {
//        // This covers the usecase when the app goes into fullscreen mode from portrait orientation.
//        // Transition to fullscreen happens first, and orientation change follows. Microsurvey container is getting
//        // reinitialized when going into landscape mode, but it shouldn't be visible if the app is already in the
//        // fullscreen mode. It still has to be initialized to be shown after the user exits the fullscreen.
//        val isFullscreen = fullScreenFeature.get()?.isFullScreen == true
//        toolbarContainerView.isVisible = !isFullscreen
//    }
//
//    bottomToolbarContainerIntegration.set(
//        feature = BottomToolbarContainerIntegration(
//            toolbar = bottomToolbarContainerView.toolbarContainerView,
//            store = context.components.core.store,
//            sessionId = customTabSessionId,
//        ),
//        owner = lifecycleOwner,
//        view = view,
//    )

    reinitializeEngineView(context, fullScreenFeature)
}

private fun removeBottomToolbarDivider(browserToolbar: BrowserToolbar, context: Context) {
    val safeContext = context ?: return
    if (safeContext.isToolbarAtBottom()) {
        val drawable = ResourcesCompat.getDrawable(
            context.resources,
            R.drawable.toolbar_background_no_divider,
            null,
        )
        browserToolbar.background = drawable
        browserToolbar.elevation = 0.0f
    }
}

private fun restoreBottomToolbarDivider(browserToolbar: BrowserToolbar, context: Context) {
    val safeContext = context ?: return
    if (safeContext.isToolbarAtBottom()) {
        val defaultBackground = ResourcesCompat.getDrawable(
            context.resources,
            R.drawable.toolbar_background,
            context?.theme,
        )
        browserToolbar.background = defaultBackground
    }
}

private fun updateNavbarDivider(context: Context) {
    val safeContext = context ?: return

    // Evaluate showing the navbar divider only if addressbar is shown at the top
    // and the toolbar chrome should be is visible.
    if (!safeContext.isToolbarAtBottom()) { // && webAppToolbarShouldBeVisible) {
        resetNavbar(context)
    }
}

/**
 * Build and show a new navbar.
 * Useful when needed to force an update of it's layout.
 */
private fun resetNavbar(context: Context) {
    if (context?.shouldAddNavigationBar() != true) return // || !webAppToolbarShouldBeVisible) return

    // todo: toolbar
    // Prevent showing two navigation bars at the same time.
//    _bottomToolbarContainerView?.toolbarContainerView?.let {
//        binding.browserLayout.removeView(it)
//    }
    reinitializeNavBar()
}

private var currentMicrosurvey: MicrosurveyUIData? = null

/**
 * Listens for the microsurvey message and initializes the microsurvey prompt if one is available.
 */
@OptIn(ExperimentalCoroutinesApi::class)
private fun listenForMicrosurveyMessage(context: Context, lifecycleOwner: LifecycleOwner) {
    // todo: microsurvey
//    binding.root.consumeFrom(context.components.appStore, lifecycleOwner) { state ->
//        state.messaging.messageToShow[FenixMessageSurfaceId.MICROSURVEY]?.let { message ->
//            if (message.id != currentMicrosurvey?.id) {
//                message.toMicrosurveyUIData()?.let { microsurvey ->
//                    context.components.settings.shouldShowMicrosurveyPrompt = true
//                    currentMicrosurvey = microsurvey
//
//                    _bottomToolbarContainerView?.toolbarContainerView.let {
//                        binding.browserLayout.removeView(it)
//                    }
//
//                    if (context.shouldAddNavigationBar()) {
//                        reinitializeNavBar()
//                    } else {
//                        initializeMicrosurveyPrompt()
//                    }
//                }
//            }
//        }
//    }
}

private fun shouldShowMicrosurveyPrompt(context: Context) =
    context.components.settings.shouldShowMicrosurveyPrompt

private fun isToolbarDynamic(context: Context) =
    !context.settings().shouldUseFixedTopToolbar && context.settings().isDynamicToolbarEnabled

///**
// * Returns a list of context menu items [ContextMenuCandidate] for the context menu
// */
//abstract fun getContextMenuCandidates(
//    context: Context,
//    view: View,
//): List<ContextMenuCandidate>


@VisibleForTesting
internal fun observeTabSelection(
    store: BrowserStore,
    context: Context,
    lifecycleOwner: LifecycleOwner,
    coroutineScope: CoroutineScope,
    setDownloadDialogData: (DownloadDialogData?) -> Unit,
    browserInitialized: Boolean,
) {
    consumeFlow(store, lifecycleOwner, context, coroutineScope) { flow ->
        flow.distinctUntilChangedBy {
            it.selectedTabId
        }.mapNotNull {
            it.selectedTab
        }.collect {
            setDownloadDialogData(null)
//            currentStartDownloadDialog?.dismiss()
            handleTabSelected(it, browserInitialized)
        }
    }
}

@VisibleForTesting
@Suppress("ComplexCondition")
internal fun observeTabSource(
    store: BrowserStore,
    context: Context,
    lifecycleOwner: LifecycleOwner,
    coroutineScope: CoroutineScope
) {
    consumeFlow(store, lifecycleOwner, context, coroutineScope) { flow ->
        flow.mapNotNull { state ->
            state.selectedTab
        }.collect {
            if (!context.components.fenixOnboarding.userHasBeenOnboarded() && it.content.loadRequest?.triggeredByRedirect != true && it.source !is SessionState.Source.External && it.content.url !in onboardingLinksList) {
                context.components.fenixOnboarding.finish()
            }
        }
    }
}

private fun handleTabSelected(selectedTab: TabSessionState, browserInitialized: Boolean) {
    // todo: theme
//    if (!this.isRemoving) {
//        updateThemeForSession(selectedTab)
//    }

    // todo: toolbar
//    if (browserInitialized) {
//        view?.let {
//            fullScreenChanged(false)
////            browserToolbarView.expand()
//
//            val context = context
//            resumeDownloadDialogState(selectedTab.id, context.components.core.store, context)
//            it.announceForAccessibility(selectedTab.toDisplayTitle())
//        }
//    } else {
//        view?.let { view -> initializeUI(view) }
//    }
}


private fun evaluateMessagesForMicrosurvey(components: Components) =
    components.appStore.dispatch(MessagingAction.Evaluate(FenixMessageSurfaceId.MICROSURVEY))


@CallSuper
fun onForwardPressed(sessionFeature: ViewBoundFeatureWrapper<SessionFeature>): Boolean {
    return sessionFeature.onForwardPressed()
}

/**
 * Forwards activity results to the [ActivityResultHandler] features.
 *//* override */ fun onActivityResult(
    requestCode: Int,
    data: Intent?,
    resultCode: Int
): Boolean {
    // todo: onActivityResult
//    return listOf(
//        promptsFeature,
//        webAuthnFeature,
//    ).any { it.onActivityResult(requestCode, data, resultCode) }
    return true
}

/**
 * Navigate to GlobalTabHistoryDialogFragment.
 */
private fun navigateToGlobalTabHistoryDialogFragment(
    navController: NavController, customTabSessionId: String?
) {
    navController.navigate(
        NavGraphDirections.actionGlobalTabHistoryDialogFragment(
            activeSessionId = customTabSessionId,
        ),
    )
}

/* override */ fun onBackLongPressed(): Boolean {
    // todo:
//    navigateToGlobalTabHistoryDialogFragment()
    return true
}

/* override */ fun onForwardLongPressed(): Boolean {
    // todo:
//    navigateToGlobalTabHistoryDialogFragment()
    return true
}

/**
 * Saves the external app session ID to be restored later in [onViewStateRestored].
 *//* override */ fun onSaveInstanceState(outState: Bundle) {
    // todo:
//    super.onSaveInstanceState(outState)
//    outState.putString(KEY_CUSTOM_TAB_SESSION_ID, customTabSessionId)
//    outState.putString(LAST_SAVED_GENERATED_PASSWORD, lastSavedGeneratedPassword)
}

/**
 * Retrieves the external app session ID saved by [onSaveInstanceState].
 *//* override */ fun onViewStateRestored(savedInstanceState: Bundle?) {
    // todo:
//    super.onViewStateRestored(savedInstanceState)
//    savedInstanceState?.getString(KEY_CUSTOM_TAB_SESSION_ID)?.let {
//        if (context.components.core.store.state.findCustomTab(it) != null) {
//            customTabSessionId = it
//        }
//    }
//    lastSavedGeneratedPassword = savedInstanceState?.getString(LAST_SAVED_GENERATED_PASSWORD)
}

/**
 * Forwards permission grant results to one of the features.
 */
@Deprecated("Deprecated in Java")/* override */ fun onRequestPermissionsResult(
    requestCode: Int,
    permissions: Array<String>,
    grantResults: IntArray,
) {
    // todo: permissions
//    val feature: PermissionsFeature? = when (requestCode) {
//        REQUEST_CODE_DOWNLOAD_PERMISSIONS -> downloadsFeature.get()
//        REQUEST_CODE_PROMPT_PERMISSIONS -> promptsFeature.get()
//        REQUEST_CODE_APP_PERMISSIONS -> sitePermissionsFeature.get()
//        else -> null
//    }
//    feature?.onPermissionsResult(permissions, grantResults)
}

/**
 * Removes the session if it was opened by an ACTION_VIEW intent
 * or if it has a parent session and no more history
 */
fun removeSessionIfNeeded(context: Context): Boolean {
    getCurrentTab(context)?.let { session ->
        return if (session.source is SessionState.Source.External && !session.restored) {
            context.getActivity()?.finish()
            context.components.useCases.tabsUseCases.removeTab(session.id)
            true
        } else {
            val hasParentSession = session is TabSessionState && session.parentId != null
            if (hasParentSession) {
                context.components.useCases.tabsUseCases.removeTab(
                    session.id, selectParentIfExists = true
                )
            }
            // We want to return to home if this session didn't have a parent session to select.
            val goToOverview = !hasParentSession
            !goToOverview
        }
    }
    return false
}

/**
 * Returns the layout [android.view.Gravity] for the quick settings and ETP dialog.
 */
fun getAppropriateLayoutGravity(context: Context): Int =
    context.components.settings.toolbarPosition.androidGravity

/**
 * Configure the engine view to know where to place website's dynamic elements
 * depending on the space taken by any dynamic toolbar.
 */
@VisibleForTesting(otherwise = VisibleForTesting.PROTECTED)
internal fun configureEngineViewWithDynamicToolbarsMaxHeight(
    context: Context,
    customTabSessionId: String?,
    findInPageIntegration: ViewBoundFeatureWrapper<FindInPageIntegration>
) {
    val currentTab =
        context.components.core.store.state.findCustomTabOrSelectedTab(customTabSessionId)
    if (currentTab?.content?.isPdf == true) return
    if (findInPageIntegration.get()?.isFeatureActive == true) return
//    val toolbarHeights = view?.let { probeToolbarHeights(it) } ?: return

    context?.also {
        if (isToolbarDynamic(it)) {
            // todo: toolbar
//            if (!context.components.core.geckoRuntime.isInteractiveWidgetDefaultResizesVisual) {
//                getEngineView().setDynamicToolbarMaxHeight(toolbarHeights.first + toolbarHeights.second)
//            }
        } else {
            // todo: toolbar
//            (getSwipeRefreshLayout().layoutParams as? CoordinatorLayout.LayoutParams)?.apply {
//                bottomMargin = toolbarHeights.second
//            }
        }
    }
}

/**
 * Get an instant reading of the top toolbar height and the bottom toolbar height.
 */
private fun probeToolbarHeights(
    rootView: View,
    customTabSessionId: String?,
    fullScreenFeature: ViewBoundFeatureWrapper<FullScreenFeature>,
): Pair<Int, Int> {
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
private fun assignSitePermissionsRules(
    context: Context,
    sitePermissionsFeature: ViewBoundFeatureWrapper<SitePermissionsFeature>,
) {
    // todo:
//    val rules = context.components.settings.getSitePermissionsCustomSettingsRules()
//
//    sitePermissionsFeature.withFeature {
//        it.sitePermissionsRules = rules
//    }
}

/**
 * Displays the quick settings dialog,
 * which lets the user control tracking protection and site settings.
 */
private fun showQuickSettingsDialog(
    context: Context,
    lifecycleOwner: LifecycleOwner,
    coroutineScope: CoroutineScope,
    navController: NavController,
    view: View,
    customTabSessionId: String?,
) {
    val tab = getCurrentTab(context, customTabSessionId) ?: return
    lifecycleOwner.lifecycleScope.launch(Main) {
        val sitePermissions: SitePermissions? = tab.content.url.getOrigin()?.let { origin ->
            val storage = context.components.core.permissionStorage
            storage.findSitePermissionsBy(origin, tab.content.private)
        }

        view?.let {
            navToQuickSettingsSheet(
                tab,
                sitePermissions,
                context = context,
                coroutineScope = coroutineScope,
                navController = navController
            )
        }
    }
}

/**
 * Set the activity normal/private theme to match the current session.
 */
@VisibleForTesting
internal fun updateThemeForSession(session: SessionState, context: Context) {
    val sessionMode = BrowsingMode.fromBoolean(session.content.private)
    (context.getActivity()!! as HomeActivity).browsingModeManager.mode = sessionMode
}

/**
 * A safe version of [getCurrentTab] that safely checks for context nullability.
 */
fun getSafeCurrentTab(context: Context, customTabSessionId: String?): SessionState? {
    return context.components.core.store.state.findCustomTabOrSelectedTab(
        customTabSessionId
    )
}

@VisibleForTesting
fun getCurrentTab(context: Context, customTabSessionId: String? = null): SessionState? {
    return context.components.core.store.state.findCustomTabOrSelectedTab(customTabSessionId)
}

private suspend fun bookmarkTapped(
    sessionUrl: String,
    sessionTitle: String,
    context: Context,
    navController: NavController,
    coroutineScope: CoroutineScope,
    snackbarHostState: AcornSnackbarHostState,
) = withContext(IO) {
    val bookmarksStorage = context.components.core.bookmarksStorage
    val existing =
        bookmarksStorage.getBookmarksWithUrl(sessionUrl).firstOrNull { it.url == sessionUrl }
    if (existing != null) {
        // Bookmark exists, go to edit fragment
        withContext(Main) {
            nav(
                navController,
                R.id.browserComponentWrapperFragment,
                BrowserComponentWrapperFragmentDirections.actionGlobalBookmarkEditFragment(
                    existing.guid, true
                ),
            )
        }
    } else {
        // Save bookmark, then go to edit fragment
        try {
            val parentNode = Result.runCatching {
                val parentGuid =
                    bookmarksStorage.getRecentBookmarks(1).firstOrNull()?.parentGuid
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
                message = context.getString(
                    R.string.bookmark_saved_in_folder_snackbar,
                    friendlyRootTitle(context, parentNode),
                ),
                context = context,
                coroutineScope = coroutineScope,
                snackbarHostState = snackbarHostState,
                onClick = {
//                        MetricsUtils.recordBookmarkMetrics(
//                            MetricsUtils.BookmarkAction.EDIT,
//                            TOAST_METRIC_SOURCE,
//                        )
                    navController.navigateWithBreadcrumb(
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
                coroutineScope.launch {
                    snackbarHostState.warningSnackbarHostState.showSnackbar(
                        message = context.getString(R.string.bookmark_invalid_url_error),
                        duration = SnackbarDuration.Long,
                    )
                }
            }
        }
    }
}

private fun showBookmarkSavedSnackbar(
    message: String,
    context: Context,
    coroutineScope: CoroutineScope,
    snackbarHostState: AcornSnackbarHostState,
    onClick: () -> Unit
) {
    coroutineScope.launch {
        val result = snackbarHostState.defaultSnackbarHostState.showSnackbar(
            message = message,
            actionLabel = context.getString(R.string.edit_bookmark_snackbar_action),
            duration = SnackbarDuration.Long,
        )
        when (result) {
            SnackbarResult.ActionPerformed -> {
                onClick.invoke()
            }

            SnackbarResult.Dismissed -> {}
        }
    }
}

fun onHomePressed(pipFeature: PictureInPictureFeature) = pipFeature?.onHomePressed() ?: false

/**
 * Exit fullscreen mode when exiting PIP mode
 */
private fun pipModeChanged(
    session: SessionState, context: Context, backPressedHandler: OnBackPressedHandler
) {
    // todo: isAdded
    if (!session.content.pictureInPictureEnabled && session.content.fullScreen) { // && isAdded) {
        onBackPressed(backPressedHandler)
        fullScreenChanged(false, context)
    }
}

fun onPictureInPictureModeChanged(enabled: Boolean, pipFeature: PictureInPictureFeature?) {
    pipFeature?.onPictureInPictureModeChanged(enabled)
}

private fun viewportFitChange(layoutInDisplayCutoutMode: Int, context: Context) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        val layoutParams = context.getActivity()?.window?.attributes
        layoutParams?.layoutInDisplayCutoutMode = layoutInDisplayCutoutMode
        context.getActivity()?.window?.attributes = layoutParams
    }
}

@VisibleForTesting
internal fun fullScreenChanged(
    inFullScreen: Boolean,
    context: Context,
) {
    val activity = context.getActivity() ?: return
    if (inFullScreen) {
        // Close find in page bar if opened
        // todo: find in page
//        findInPageIntegration.onBackPressed()

        FullScreenNotificationToast(
            activity = activity,
            gestureNavString = context.getString(R.string.exit_fullscreen_with_gesture_short),
            backButtonString = context.getString(R.string.exit_fullscreen_with_back_button_short),
            GestureNavUtils,
        ).show()

        // todo: engine view
//        activity.enterImmersiveMode(
//            setOnApplyWindowInsetsListener = { key: String, listener: OnApplyWindowInsetsListener ->
//                binding.engineView.addWindowInsetsListener(key, listener)
//            },
//        )
//        (view as? SwipeGestureLayout)?.isSwipeEnabled = false
        expandBrowserView()

    } else {
        // todo: engine view
//        activity.exitImmersiveMode(
//            unregisterOnApplyWindowInsetsListener = binding.engineView::removeWindowInsetsListener,
//        )

//        (view as? SwipeGestureLayout)?.isSwipeEnabled = true
        (context.getActivity() as? HomeActivity)?.let { homeActivity ->
            // ExternalAppBrowserActivity exclusively handles it's own theming unless in private mode.
            if (homeActivity !is ExternalAppBrowserActivity || homeActivity.browsingModeManager.mode.isPrivate) {
                homeActivity.themeManager.applyStatusBarTheme(
                    homeActivity, homeActivity.isTabStripEnabled()
                )
            }
        }
        collapseBrowserView()
    }

    // todo: swipe refresh
//    binding.swipeRefresh.isEnabled = shouldPullToRefreshBeEnabled(inFullScreen)
}

@VisibleForTesting(otherwise = VisibleForTesting.PROTECTED)
internal fun expandBrowserView() {
    // todo: toolbar
//    browserToolbarView.apply {
//        collapse()
//        gone()
//    }
//    _bottomToolbarContainerView?.toolbarContainerView?.apply {
//        collapse()
//        isVisible = false
//    }
    // todo: engine
//    val browserEngine = getSwipeRefreshLayout().layoutParams as CoordinatorLayout.LayoutParams
//    browserEngine.behavior = null
//    browserEngine.bottomMargin = 0
//    browserEngine.topMargin = 0
    // todo:
//    getSwipeRefreshLayout().translationY = 0f

//    getEngineView().apply {
    // todo: engine view
//        setDynamicToolbarMaxHeight(0)
//        setVerticalClipping(0)
//    }
}

@VisibleForTesting(otherwise = VisibleForTesting.PROTECTED)
internal fun collapseBrowserView() {
    // todo:
//    if (webAppToolbarShouldBeVisible) {
//        browserToolbarView.visible()
//        _bottomToolbarContainerView?.toolbarContainerView?.isVisible = true
//        reinitializeEngineView()
//        browserToolbarView.expand()
//        _bottomToolbarContainerView?.toolbarContainerView?.expand()
//    }
}

@CallSuper
fun onUpdateToolbarForConfigurationChange(
    toolbar: BrowserToolbarView,
    context: Context,
    fullScreenFeature: ViewBoundFeatureWrapper<FullScreenFeature>,
) {
    toolbar.dismissMenu()

    // If the navbar feature could be visible, we should update it's state.
    val shouldUpdateNavBarState =
        // todo: webAppToolbarShouldBeVisible
        context.settings().navigationToolbarEnabled // && webAppToolbarShouldBeVisible
    if (shouldUpdateNavBarState) {
        // todo: navbar
//        updateNavBarForConfigurationChange(
//            context = context,
//            parent = binding.browserLayout,
//            toolbarView = browserToolbarView.view,
//            bottomToolbarContainerView = _bottomToolbarContainerView?.toolbarContainerView,
//            reinitializeNavBar = ::reinitializeNavBar,
//            reinitializeMicrosurveyPrompt = ::initializeMicrosurveyPrompt,
//        )
    }

    reinitializeEngineView(context, fullScreenFeature)

    // If the microsurvey feature is visible, we should update it's state.
    if (shouldShowMicrosurveyPrompt(context) && !shouldUpdateNavBarState) {
        // todo: microsurvey
//        updateMicrosurveyPromptForConfigurationChange(
//            parent = binding.browserLayout,
//            bottomToolbarContainerView = _bottomToolbarContainerView?.toolbarContainerView,
//            reinitializeMicrosurveyPrompt = ::initializeMicrosurveyPrompt,
//        )
    }
}

private fun reinitializeNavBar() {
    // todo: navbar
//    initializeNavBar(
//        browserToolbar = browserToolbarView.view,
//        view = requireView(),
//        context = context,
//        activity = context.getActivity()!! as HomeActivity,
//    )
}

@VisibleForTesting
internal fun reinitializeEngineView(
    context: Context,
    fullScreenFeature: ViewBoundFeatureWrapper<FullScreenFeature>,
    customTabSessionId: String? = null,
) {
    val isFullscreen = fullScreenFeature.get()?.isFullScreen == true
    val shouldToolbarsBeHidden = isFullscreen // || !webAppToolbarShouldBeVisible
    val topToolbarHeight = context.settings().getTopToolbarHeight(
        includeTabStrip = customTabSessionId == null && context.isTabStripEnabled(),
    )
    val bottomToolbarHeight = context.settings().getBottomToolbarHeight(context)

    initializeEngineView(
        topToolbarHeight = if (shouldToolbarsBeHidden) 0 else topToolbarHeight,
        bottomToolbarHeight = if (shouldToolbarsBeHidden) 0 else bottomToolbarHeight,
        context = context,
    )
}

/*
 * Dereference these views when the fragment view is destroyed to prevent memory leaks
 */

//override fun onAttach(context: Context) {
//    super.onAttach(context)
//}
//
//override fun onDetach() {
//    super.onDetach()
//}

internal fun showCannotOpenFileError(
    container: ViewGroup,
    context: Context,
    downloadState: DownloadState,
    coroutineScope: CoroutineScope,
    snackbarHostState: AcornSnackbarHostState
) {
    coroutineScope.launch {
        snackbarHostState.warningSnackbarHostState.showSnackbar(
            message = DynamicDownloadDialog.getCannotOpenFileErrorMessage(
                context, downloadState
            ), duration = SnackbarDuration.Long
        )
    }
}

fun onAccessibilityStateChanged(enabled: Boolean) {
    // todo: toolbar
//    if (_browserToolbarView != null) {
//        browserToolbarView.setToolbarBehavior(enabled)
//    }
}

fun onConfigurationChanged(newConfig: Configuration) {
//    super.onConfigurationChanged(newConfig)
//
    // todo: find in page
//    if (findInPageIntegration.get()?.isFeatureActive != true && fullScreenFeature.get()?.isFullScreen != true) {
//        _browserToolbarView?.let {
//            onUpdateToolbarForConfigurationChange(it)
//        }
//    }
}

// This method is called in response to native web extension messages from
// content scripts (e.g the reader view extension). By the time these
// messages are processed the fragment/view may no longer be attached.
internal fun safeInvalidateBrowserToolbarView() {
    // todo: toolbar
//    runIfFragmentIsAttached {
//        val toolbarView = _browserToolbarView
//        if (toolbarView != null) {
//            toolbarView.view.invalidateActions()
//            toolbarView.toolbarIntegration.invalidateMenu()
//        }
//        _menuButtonView?.setHighlightStatus()
//    }
}

///**
// * Convenience method for replacing EngineView (id/engineView) in unit tests.
// */
//@VisibleForTesting
//internal fun getEngineView() = engineView!!// binding.engineView

///**
// * Convenience method for replacing SwipeRefreshLayout (id/swipeRefresh) in unit tests.
// */
//@VisibleForTesting
//internal fun getSwipeRefreshLayout() = binding.swipeRefresh

internal fun shouldShowCompletedDownloadDialog(
    downloadState: DownloadState,
    status: DownloadState.Status,
    context: Context,
): Boolean {
    val isValidStatus =
        status in listOf(DownloadState.Status.COMPLETED, DownloadState.Status.FAILED)
    val isSameTab = downloadState.sessionId == (getCurrentTab(context)?.id ?: false)

    return isValidStatus && isSameTab
}

private fun handleOnSaveLoginWithGeneratedStrongPassword(
    passwordsStorage: SyncableLoginsStorage,
    url: String,
    password: String,
    lifecycleScope: CoroutineScope,
    setLastSavedGeneratedPassword: (String) -> Unit
) {
    setLastSavedGeneratedPassword(password)
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
    lastSavedGeneratedPassword: String?,
): Boolean {
    return username.isEmpty() && password == lastSavedGeneratedPassword
}

private fun removeLastSavedGeneratedPassword(setLastSavedGeneratedPassword: (String?) -> Unit) {
    setLastSavedGeneratedPassword(null)
}

private fun navigateToSavedLoginsFragment(navController: NavController) {
    if (navController.currentDestination?.id == R.id.browserComponentWrapperFragment) {
        val directions = BrowserComponentWrapperFragmentDirections.actionLoginsListFragment()
        navController.navigate(directions)
    }
}

/* BaseBrowserFragment funs */

/* BrowserFragment funs */

private fun initSharePageAction(
    context: Context, browserToolbarInteractor: BrowserToolbarInteractor
) {
    if (!context.settings().navigationToolbarEnabled || context.isTabStripEnabled()) {
        return
    }

    val sharePageAction = BrowserToolbar.createShareBrowserAction(
        context = context,
    ) {
//            AddressToolbar.shareTapped.record((NoExtras()))
        browserToolbarInteractor.onShareActionClicked()
    }

    // todo: toolbar
//    browserToolbarView.view.addPageAction(sharePageAction)
}

private fun initTranslationsAction(
    context: Context,
    view: View,
    browserToolbarInteractor: BrowserToolbarInteractor,
    translationsAvailable: Boolean
) {
    if (!FxNimbus.features.translations.value().mainFlowToolbarEnabled) {
        return
    }

    val translationsAction = Toolbar.ActionButton(
        AppCompatResources.getDrawable(
            context,
            R.drawable.mozac_ic_translate_24,
        ),
        contentDescription = context.getString(R.string.browser_toolbar_translate),
        iconTintColorResource = ThemeManager.resolveAttribute(R.attr.textPrimary, context),
        visible = { translationsAvailable },
        weight = { TRANSLATIONS_WEIGHT },
        listener = {
//            browserToolbarInteractor.onTranslationsButtonClicked()
        },
    )
    // todo: toolbar
//    browserToolbarView.view.addPageAction(translationsAction)

    // todo: translations
//    translationsBinding.set(
//        feature = TranslationsBinding(browserStore = context.components.core.store,
//            onTranslationsActionUpdated = {
//                translationsAvailable = it.isVisible
//
//                translationsAction.updateView(
//                    tintColorResource = if (it.isTranslated) {
//                        R.color.fx_mobile_icon_color_accent_violet
//                    } else {
//                        ThemeManager.resolveAttribute(R.attr.textPrimary, context)
//                    },
//                    contentDescription = if (it.isTranslated) {
//                        context.getString(
//                            R.string.browser_toolbar_translated_successfully,
//                            it.fromSelectedLanguage?.localizedDisplayName,
//                            it.toSelectedLanguage?.localizedDisplayName,
//                        )
//                    } else {
//                        context.getString(R.string.browser_toolbar_translate)
//                    },
//                )
//
//                safeInvalidateBrowserToolbarView()
//
//                if (!it.isTranslateProcessing) {
    // todo: snackbar dismiss
//                    context.components.appStore.dispatch(SnackbarAction.SnackbarDismissed)
//                }
//            },
//            onShowTranslationsDialog = {} //FIXME: browserToolbarInteractor.onTranslationsButtonClicked(),
//        ),
//        owner = lifecycleOwner,
//        view = view,
//    )
}

@SuppressLint("VisibleForTests")
private fun initReloadAction(context: Context) {
    if (!context.settings().navigationToolbarEnabled) {
        return
    }

    // todo: refresh action
//    refreshAction = BrowserToolbar.TwoStateButton(
//        primaryImage = AppCompatResources.getDrawable(
//            context,
//            R.drawable.mozac_ic_arrow_clockwise_24,
//        )!!,
//        primaryContentDescription = context.getString(R.string.browser_menu_refresh),
//        primaryImageTintResource = ThemeManager.resolveAttribute(R.attr.textPrimary, context),
//        isInPrimaryState = {
//            getSafeCurrentTab()?.content?.loading == false
//        },
//        secondaryImage = AppCompatResources.getDrawable(
//            context,
//            R.drawable.mozac_ic_stop,
//        )!!,
//        secondaryContentDescription = context.getString(R.string.browser_menu_stop),
//        disableInSecondaryState = false,
//        weight = { RELOAD_WEIGHT },
//        longClickListener = {
//            browserToolbarInteractor.onBrowserToolbarMenuItemTapped(
//                ToolbarMenu.Item.Reload(bypassCache = true),
//            )
//        },
//        listener = {
//            if (getCurrentTab()?.content?.loading == true) {
//                browserToolbarInteractor.onBrowserToolbarMenuItemTapped(ToolbarMenu.Item.Stop)
//            } else {
//                browserToolbarInteractor.onBrowserToolbarMenuItemTapped(
//                    ToolbarMenu.Item.Reload(bypassCache = false),
//                )
//            }
//        },
//    )
//
//    refreshAction?.let {
//        browserToolbarView.view.addPageAction(it)
//    }
}

private fun initReaderMode(context: Context, view: View) {
    // todo: reader mode
//    val readerModeAction = BrowserToolbar.ToggleButton(
//        image = AppCompatResources.getDrawable(
//            context,
//            R.drawable.ic_readermode,
//        )!!,
//        imageSelected = AppCompatResources.getDrawable(
//            context,
//            R.drawable.ic_readermode_selected,
//        )!!,
//        contentDescription = context.getString(R.string.browser_menu_read),
//        contentDescriptionSelected = context.getString(R.string.browser_menu_read_close),
//        visible = {
//            readerModeAvailable && !reviewQualityCheckAvailable
//        },
//        weight = { READER_MODE_WEIGHT },
//        selected = getSafeCurrentTab()?.let {
//            activity?.components?.core?.store?.state?.findTab(it.id)?.readerState?.active
//        } ?: false,
//        listener = browserToolbarInteractor::onReaderModePressed,
//    )

    // todo: toolbar
//    browserToolbarView.view.addPageAction(readerModeAction)
//
//    readerViewFeature.set(
//        feature = context.components.strictMode.resetAfter(StrictMode.allowThreadDiskReads()) {
//            ReaderViewFeature(
//                context = context,
//                engine = context.components.core.engine,
//                store = context.components.core.store,
//                controlsView = binding.readerViewControlsBar,
//            ) { available, active ->
//                readerModeAvailable = available
//                readerModeAction.setSelected(active)
//                safeInvalidateBrowserToolbarView()
//            }
//        },
//        owner = lifecycleOwner,
//        view = view,
//    )
}

private fun initReviewQualityCheck(
    context: Context,
    lifecycleOwner: LifecycleOwner,
    view: View,
    navController: NavController,
    setReviewQualityCheckAvailable: (Boolean) -> Unit,
    reviewQualityCheckAvailable: Boolean,
    reviewQualityCheckFeature: ViewBoundFeatureWrapper<ReviewQualityCheckFeature>,
) {
    val reviewQualityCheck = BrowserToolbar.ToggleButton(
        image = AppCompatResources.getDrawable(
            context,
            R.drawable.mozac_ic_shopping_24,
        )!!.apply {
            setTint(getColor(context, R.color.fx_mobile_text_color_primary))
        },
        imageSelected = AppCompatResources.getDrawable(
            context,
            R.drawable.ic_shopping_selected,
        )!!,
        contentDescription = context.getString(R.string.review_quality_check_open_handle_content_description),
        contentDescriptionSelected = context.getString(R.string.review_quality_check_close_handle_content_description),
        visible = { reviewQualityCheckAvailable },
        weight = { REVIEW_QUALITY_CHECK_WEIGHT },
        listener = { _ ->
            context.components.appStore.dispatch(
                ShoppingAction.ShoppingSheetStateUpdated(expanded = true),
            )
            // todo: nonexistent
//            navController.navigate(
//                BrowserComponentWrapperFragmentDirections.actionBrowserFragmentToReviewQualityCheckDialogFragment(),
//            )
        },
    )

    // todo: toolbar
//    browserToolbarView.view.addPageAction(reviewQualityCheck)

    reviewQualityCheckFeature.set(
        feature = ReviewQualityCheckFeature(
            appStore = context.components.appStore,
            browserStore = context.components.core.store,
            shoppingExperienceFeature = DefaultShoppingExperienceFeature(),
            onIconVisibilityChange = {
                setReviewQualityCheckAvailable(it)
                safeInvalidateBrowserToolbarView()
            },
            onBottomSheetStateChange = {
                reviewQualityCheck.setSelected(selected = it, notifyListener = false)
            },
            onProductPageDetected = {
                // Shopping.productPageVisits.add()
            },
        ),
        owner = lifecycleOwner,
        view = view,
    )
}

@VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
internal fun addLeadingAction(
    context: Context,
    showHomeButton: Boolean,
    showEraseButton: Boolean,
) {
    // todo: leading action
//    if (leadingAction != null) return
//
//    leadingAction = if (showEraseButton) {
//        BrowserToolbar.Button(
//            imageDrawable = AppCompatResources.getDrawable(
//                context,
//                R.drawable.mozac_ic_data_clearance_24,
//            )!!,
//            contentDescription = context.getString(R.string.browser_toolbar_erase),
//            iconTintColorResource = ThemeManager.resolveAttribute(R.attr.textPrimary, context),
//            listener = browserToolbarInteractor::onEraseButtonClicked,
//        )
//    } else if (showHomeButton) {
//        BrowserToolbar.Button(
//            imageDrawable = AppCompatResources.getDrawable(
//                context,
//                R.drawable.mozac_ic_home_24,
//            )!!,
//            contentDescription = context.getString(R.string.browser_toolbar_home),
//            iconTintColorResource = ThemeManager.resolveAttribute(R.attr.textPrimary, context),
//            listener = browserToolbarInteractor::onHomeButtonClicked,
//        )
//    } else {
//        null
//    }
//
//    leadingAction?.let {
//        browserToolbarView.view.addNavigationAction(it)
//    }
}

@VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
internal fun removeLeadingAction() {
    // todo: leading action
//    leadingAction?.let {
//        browserToolbarView.view.removeNavigationAction(it)
//    }
//    leadingAction = null
}

/**
 * This code takes care of the [BrowserToolbar] leading and navigation actions.
 * The older design requires a HomeButton followed by navigation buttons for tablets.
 * The newer design expects NavigationButtons and a HomeButton in landscape mode for phones and in both modes
 * for tablets.
 */
@VisibleForTesting
internal fun updateBrowserToolbarLeadingAndNavigationActions(
    context: Context,
    redesignEnabled: Boolean,
    isLandscape: Boolean,
    isTablet: Boolean,
    isPrivate: Boolean,
    feltPrivateBrowsingEnabled: Boolean,
    isWindowSizeSmall: Boolean,
) {
    if (redesignEnabled) {
        updateAddressBarNavigationActions(
            context = context,
            isWindowSizeSmall = isWindowSizeSmall,
        )
        updateAddressBarLeadingAction(
            redesignEnabled = true,
            isLandscape = isLandscape,
            isTablet = isTablet,
            isPrivate = isPrivate,
            feltPrivateBrowsingEnabled = feltPrivateBrowsingEnabled,
            context = context,
        )
    } else {
        updateAddressBarLeadingAction(
            redesignEnabled = false,
            isLandscape = isLandscape,
            isPrivate = isPrivate,
            isTablet = isTablet,
            feltPrivateBrowsingEnabled = feltPrivateBrowsingEnabled,
            context = context,
        )
        updateTabletToolbarActions(isTablet = isTablet, context)
    }
    // todo: toolbar
//    browserToolbarView.view.invalidateActions()
}

private fun updateBrowserToolbarMenuVisibility() {
    // todo: toolbar
//    browserToolbarView.updateMenuVisibility(
//        isVisible = false // !context.shouldAddNavigationBar(),
//    )
}

@VisibleForTesting
internal fun updateAddressBarLeadingAction(
    redesignEnabled: Boolean,
    isLandscape: Boolean,
    isTablet: Boolean,
    isPrivate: Boolean,
    feltPrivateBrowsingEnabled: Boolean,
    context: Context,
) {
    val showHomeButton = !redesignEnabled
    val showEraseButton = feltPrivateBrowsingEnabled && isPrivate && (isLandscape || isTablet)

    if (showHomeButton || showEraseButton) {
        addLeadingAction(
            context = context,
            showHomeButton = showHomeButton,
            showEraseButton = showEraseButton,
        )
    } else {
        removeLeadingAction()
    }
}

@VisibleForTesting
internal fun updateAddressBarNavigationActions(
    context: Context,
    isWindowSizeSmall: Boolean,
) {
    if (!isWindowSizeSmall) {
        addNavigationActions(context)
    } else {
        removeNavigationActions()
    }
}

fun onUpdateToolbarForConfigurationChange(toolbar: BrowserToolbarView, context: Context) {
//    super.onUpdateToolbarForConfigurationChange(toolbar)

    updateBrowserToolbarLeadingAndNavigationActions(
        context = context,
        redesignEnabled = context.settings().navigationToolbarEnabled,
        isLandscape = context.isLandscape(),
        isTablet = isLargeWindow(context),
        isPrivate = (context.getActivity()!! as HomeActivity).browsingModeManager.mode.isPrivate,
        feltPrivateBrowsingEnabled = context.settings().feltPrivateBrowsingEnabled,
        isWindowSizeSmall = false, // AcornWindowSize.getWindowSize(context) == AcornWindowSize.Small,
    )

    updateBrowserToolbarMenuVisibility()
}

@VisibleForTesting
internal fun updateTabletToolbarActions(isTablet: Boolean, context: Context) {
    // todo: isTablet
//    if (isTablet == this.isTablet) return
//
//    if (isTablet) {
//        addTabletActions(context)
//    } else {
//        removeTabletActions()
//    }
//
//    this.isTablet = isTablet
}

@VisibleForTesting
internal fun addNavigationActions(context: Context) {
    val enableTint = ThemeManager.resolveAttribute(R.attr.textPrimary, context)
    val disableTint = ThemeManager.resolveAttribute(R.attr.textDisabled, context)

    // todo: back action
//    if (backAction == null) {
//        backAction = BrowserToolbar.TwoStateButton(
//            primaryImage = AppCompatResources.getDrawable(
//                context,
//                R.drawable.mozac_ic_back_24,
//            )!!,
//            primaryContentDescription = context.getString(R.string.browser_menu_back),
//            primaryImageTintResource = enableTint,
//            isInPrimaryState = { getSafeCurrentTab()?.content?.canGoBack ?: false },
//            secondaryImageTintResource = disableTint,
//            disableInSecondaryState = true,
//            longClickListener = {
////                    if (!this.isTablet) {
////                        NavigationBar.browserBackLongTapped.record(NoExtras())
////                    }
//                browserToolbarInteractor.onBrowserToolbarMenuItemTapped(
//                    ToolbarMenu.Item.Back(viewHistory = true),
//                )
//            },
//            listener = {
////                    if (!this.isTablet) {
////                        NavigationBar.browserBackTapped.record(NoExtras())
////                    }
//                browserToolbarInteractor.onBrowserToolbarMenuItemTapped(
//                    ToolbarMenu.Item.Back(viewHistory = false),
//                )
//            },
//        ).also {
//            browserToolbarView.view.addNavigationAction(it)
//        }
//    }

    // todo: forward action
//    if (forwardAction == null) {
//        forwardAction = BrowserToolbar.TwoStateButton(
//            primaryImage = AppCompatResources.getDrawable(
//                context,
//                R.drawable.mozac_ic_forward_24,
//            )!!,
//            primaryContentDescription = context.getString(R.string.browser_menu_forward),
//            primaryImageTintResource = enableTint,
//            isInPrimaryState = { getSafeCurrentTab()?.content?.canGoForward ?: false },
//            secondaryImageTintResource = disableTint,
//            disableInSecondaryState = true,
//            longClickListener = {
////                    if (!this.isTablet) {
////                        NavigationBar.browserForwardLongTapped.record(NoExtras())
////                    }
//                browserToolbarInteractor.onBrowserToolbarMenuItemTapped(
//                    ToolbarMenu.Item.Forward(viewHistory = true),
//                )
//            },
//            listener = {
////                    if (!this.isTablet) {
////                        NavigationBar.browserForwardTapped.record(NoExtras())
////                    }
//                browserToolbarInteractor.onBrowserToolbarMenuItemTapped(
//                    ToolbarMenu.Item.Forward(viewHistory = false),
//                )
//            },
//        ).also {
//            browserToolbarView.view.addNavigationAction(it)
//        }
//    }
}

@VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
internal fun addTabletActions(context: Context) {
    addNavigationActions(context)

    val enableTint = ThemeManager.resolveAttribute(R.attr.textPrimary, context)
    // todo: refresh action
//    if (refreshAction == null) {
//        refreshAction = BrowserToolbar.TwoStateButton(
//            primaryImage = AppCompatResources.getDrawable(
//                context,
//                R.drawable.mozac_ic_arrow_clockwise_24,
//            )!!,
//            primaryContentDescription = context.getString(R.string.browser_menu_refresh),
//            primaryImageTintResource = enableTint,
//            isInPrimaryState = {
//                getSafeCurrentTab()?.content?.loading == false
//            },
//            secondaryImage = AppCompatResources.getDrawable(
//                context,
//                R.drawable.mozac_ic_stop,
//            )!!,
//            secondaryContentDescription = context.getString(R.string.browser_menu_stop),
//            disableInSecondaryState = false,
//            longClickListener = {
//                browserToolbarInteractor.onBrowserToolbarMenuItemTapped(
//                    ToolbarMenu.Item.Reload(bypassCache = true),
//                )
//            },
//            listener = {
//                if (getCurrentTab()?.content?.loading == true) {
//                    browserToolbarInteractor.onBrowserToolbarMenuItemTapped(ToolbarMenu.Item.Stop)
//                } else {
//                    browserToolbarInteractor.onBrowserToolbarMenuItemTapped(
//                        ToolbarMenu.Item.Reload(bypassCache = false),
//                    )
//                }
//            },
//        ).also {
//            browserToolbarView.view.addNavigationAction(it)
//        }
//    }
}

@VisibleForTesting
internal fun removeNavigationActions() {
    // todo: forward action
//    forwardAction?.let {
//        browserToolbarView.view.removeNavigationAction(it)
//    }
//    forwardAction = null
    // todo: back action
//    backAction?.let {
//        browserToolbarView.view.removeNavigationAction(it)
//    }
//    backAction = null
}

@VisibleForTesting
internal fun removeTabletActions() {
    removeNavigationActions()

    // todo: refresh action
//    refreshAction?.let {
//        browserToolbarView.view.removeNavigationAction(it)
//    }
}

@SuppressLint("VisibleForTests")
private fun updateHistoryMetadata(context: Context) {
    getCurrentTab(context)?.let { tab ->
        (tab as? TabSessionState)?.historyMetadata?.let {
            context.components.core.historyMetadataService.updateMetadata(it, tab)
        }
    }
}

private fun subscribeToTabCollections(context: Context, lifecycleOwner: LifecycleOwner) {
    Observer<List<TabCollection>> {
        context.components.core.tabCollectionStorage.cachedTabCollections = it
    }.also { observer ->
        context.components.core.tabCollectionStorage.getCollections()
            .observe(lifecycleOwner, observer)
    }
}


fun navToQuickSettingsSheet(
    tab: SessionState,
    sitePermissions: SitePermissions?,
    context: Context,
    coroutineScope: CoroutineScope,
    navController: NavController,
) {
    val useCase = context.components.useCases.trackingProtectionUseCases
//        FxNimbus.features.cookieBanners.recordExposure()
    useCase.containsException(tab.id) { hasTrackingProtectionException ->
//        lifecycleScope.launch {
        coroutineScope.launch {
            val cookieBannersStorage = context.components.core.cookieBannersStorage
            val cookieBannerUIMode = cookieBannersStorage.getCookieBannerUIMode(
                context,
                tab,
            )
            withContext(Main) {
                // todo: check if fragment attached
//                runIfFragmentIsAttached {
//                    val isTrackingProtectionEnabled =
//                        tab.trackingProtection.enabled && !hasTrackingProtectionException
//                    val directions = if (context.settings().enableUnifiedTrustPanel) {
//                        BrowserComponentWrapperFragmentDirections.actionBrowserFragmentToTrustPanelFragment(
//                            sessionId = tab.id,
//                            url = tab.content.url,
//                            title = tab.content.title,
//                            isSecured = tab.content.securityInfo.secure,
//                            sitePermissions = sitePermissions,
//                            certificateName = tab.content.securityInfo.issuer,
//                            permissionHighlights = tab.content.permissionHighlights,
//                            isTrackingProtectionEnabled = isTrackingProtectionEnabled,
//                            cookieBannerUIMode = cookieBannerUIMode,
//                        )
//                    } else {
//                        BrowserComponentWrapperFragmentDirections.actionBrowserFragmentToQuickSettingsSheetDialogFragment(
//                            sessionId = tab.id,
//                            url = tab.content.url,
//                            title = tab.content.title,
//                            isSecured = tab.content.securityInfo.secure,
//                            sitePermissions = sitePermissions,
//                            gravity = getAppropriateLayoutGravity(),
//                            certificateName = tab.content.securityInfo.issuer,
//                            permissionHighlights = tab.content.permissionHighlights,
//                            isTrackingProtectionEnabled = isTrackingProtectionEnabled,
//                            cookieBannerUIMode = cookieBannerUIMode,
//                        )
//                    }
//                    nav(navController, R.id.browserFragment, directions)
//                }
            }
        }
    }
}

private fun collectionStorageObserver(
    context: Context,
    navController: NavController,
    view: View,
    coroutineScope: CoroutineScope,
    snackbarHostState: AcornSnackbarHostState,
): TabCollectionStorage.Observer {
    return object : TabCollectionStorage.Observer {
        override fun onCollectionCreated(
            title: String,
            sessions: List<TabSessionState>,
            id: Long?,
        ) {
            showTabSavedToCollectionSnackbar(
                sessions.size, context, navController, coroutineScope, snackbarHostState, true
            )
        }

        override fun onTabsAdded(
            tabCollection: TabCollection,
            sessions: List<TabSessionState>
        ) {
            showTabSavedToCollectionSnackbar(
                sessions.size, context, navController, coroutineScope, snackbarHostState
            )
        }

        fun showTabSavedToCollectionSnackbar(
            tabSize: Int,
            context: Context,
            navController: NavController,
            coroutineScope: CoroutineScope,
            snackbarHostState: AcornSnackbarHostState,
            isNewCollection: Boolean = false,
        ) {
            view?.let {
                val messageStringRes = when {
                    isNewCollection -> {
                        R.string.create_collection_tabs_saved_new_collection
                    }

                    tabSize > 1 -> {
                        R.string.create_collection_tabs_saved
                    }

                    else -> {
                        R.string.create_collection_tab_saved
                    }
                }

                // show snackbar
                coroutineScope.launch {
                    val result = snackbarHostState.defaultSnackbarHostState.showSnackbar(
                        message = context.getString(messageStringRes),
                        duration = SnackbarDuration.Long,
                        actionLabel = context.getString(R.string.create_collection_view),
                    )
                    when (result) {
                        SnackbarResult.ActionPerformed -> {
                            navController.navigate(
                                BrowserComponentWrapperFragmentDirections.actionGlobalHome(
                                    focusOnAddressBar = false,
                                    scrollToCollection = true,
                                ),
                            )
                        }

                        SnackbarResult.Dismissed -> {}
                    }
                }

//                Snackbar.make(
//                    snackBarParentView = binding.dynamicSnackbarContainer,
//                    snackbarState = SnackbarState(
//                        message = context.getString(messageStringRes),
//                        action = Action(
//                            label = context.getString(R.string.create_collection_view),
//                            onClick = {
//                                navController.navigate(
//                                    BrowserComponentWrapperFragmentDirections.actionGlobalHome(
//                                        focusOnAddressBar = false,
//                                        scrollToCollection = true,
//                                    ),
//                                )
//                            },
//                        ),
//                    ),
//                ).show()
            }
        }
    }
}

fun getContextMenuCandidates(
    context: Context,
    view: View,
): List<ContextMenuCandidate> {
    val contextMenuCandidateAppLinksUseCases = AppLinksUseCases(
        context,
        { true },
    )

    return ContextMenuCandidate.defaultCandidates(
        context,
        context.components.useCases.tabsUseCases,
        context.components.useCases.contextMenuUseCases,
        view,
        ContextMenuSnackbarDelegate(),
    ) + ContextMenuCandidate.createOpenInExternalAppCandidate(
        context,
        contextMenuCandidateAppLinksUseCases,
    )
}

/**
 * Updates the last time the user was active on the [BrowserFragment].
 * This is useful to determine if the user has to start on the [HomeFragment]
 * or it should go directly to the [BrowserFragment].
 */
@VisibleForTesting
fun updateLastBrowseActivity(context: Context) {
    context.settings().lastBrowseActivity = System.currentTimeMillis()
}