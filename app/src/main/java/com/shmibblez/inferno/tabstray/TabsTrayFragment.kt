/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package com.shmibblez.inferno.tabstray

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Context
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.UiThread
import androidx.annotation.VisibleForTesting
import androidx.appcompat.app.AppCompatDialogFragment
import androidx.compose.material3.SnackbarDuration
import androidx.core.view.isVisible
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.setFragmentResultListener
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.tabs.TabLayout
import kotlinx.coroutines.Dispatchers
import mozilla.appservices.places.BookmarkRoot
import mozilla.components.browser.state.selector.normalTabs
import mozilla.components.browser.state.selector.privateTabs
import mozilla.components.browser.state.state.TabSessionState
import mozilla.components.browser.state.store.BrowserStore
import mozilla.components.feature.accounts.push.CloseTabsUseCases
import mozilla.components.feature.downloads.ui.DownloadCancelDialogFragment
import mozilla.components.feature.tabs.tabstray.TabsFeature
import mozilla.components.support.base.feature.ViewBoundFeatureWrapper
//import mozilla.telemetry.glean.private.NoExtras
import com.shmibblez.inferno.Config
//import com.shmibblez.inferno.GleanMetrics.TabsTray
import com.shmibblez.inferno.HomeActivity
import com.shmibblez.inferno.NavGraphDirections
import com.shmibblez.inferno.R
import com.shmibblez.inferno.browser.tabstrip.isTabStripEnabled
import com.shmibblez.inferno.components.StoreProvider
import com.shmibblez.inferno.compose.core.Action
import com.shmibblez.inferno.compose.snackbar.Snackbar
import com.shmibblez.inferno.compose.snackbar.SnackbarState
import com.shmibblez.inferno.compose.snackbar.toSnackbarStateDuration
import com.shmibblez.inferno.databinding.ComponentTabstray2Binding
import com.shmibblez.inferno.databinding.ComponentTabstray3Binding
import com.shmibblez.inferno.databinding.ComponentTabstray3FabBinding
import com.shmibblez.inferno.databinding.ComponentTabstrayFabBinding
import com.shmibblez.inferno.databinding.FragmentTabTrayDialogBinding
import com.shmibblez.inferno.databinding.TabsTrayTabCounter2Binding
import com.shmibblez.inferno.databinding.TabstrayMultiselectItemsBinding
import com.shmibblez.inferno.ext.actualInactiveTabs
import com.shmibblez.inferno.ext.components
import com.shmibblez.inferno.ext.requireComponents
import com.shmibblez.inferno.ext.runIfFragmentIsAttached
import com.shmibblez.inferno.ext.settings
import com.shmibblez.inferno.home.HomeScreenViewModel
import com.shmibblez.inferno.proto.InfernoSettings
import com.shmibblez.inferno.share.ShareFragment
import com.shmibblez.inferno.tabstray.browser.SelectionBannerBinding
import com.shmibblez.inferno.tabstray.browser.SelectionBannerBinding.VisibilityModifier
import com.shmibblez.inferno.tabstray.browser.SelectionHandleBinding
import com.shmibblez.inferno.tabstray.browser.TabSorter
import com.shmibblez.inferno.tabstray.ext.showWithTheme
import com.shmibblez.inferno.tabstray.syncedtabs.SyncedTabsIntegration
import com.shmibblez.inferno.theme.FirefoxTheme
import com.shmibblez.inferno.theme.Theme
import com.shmibblez.inferno.theme.ThemeManager
import com.shmibblez.inferno.utils.allowUndo
import kotlin.math.abs
import kotlin.math.max

/**
 * The action or screen that was used to navigate to the Tabs Tray.
 */
enum class TabsTrayAccessPoint {
    None,
    HomeRecentSyncedTab,
}

@Suppress("TooManyFunctions", "LargeClass")
class TabsTrayFragment : AppCompatDialogFragment() {

    @VisibleForTesting internal lateinit var tabsTrayStore: TabsTrayStore
    private lateinit var tabsTrayDialog: TabsTrayDialog
    private lateinit var tabsTrayInteractor: TabsTrayInteractor
    private lateinit var tabsTrayController: DefaultTabsTrayController
    private lateinit var navigationInteractor: DefaultNavigationInteractor

    @VisibleForTesting internal lateinit var trayBehaviorManager: TabSheetBehaviorManager

    private val tabLayoutMediator = ViewBoundFeatureWrapper<TabLayoutMediator>()
    private val inactiveTabsBinding = ViewBoundFeatureWrapper<InactiveTabsBinding>()
    private val tabCounterBinding = ViewBoundFeatureWrapper<TabCounterBinding>()
    private val floatingActionButtonBinding = ViewBoundFeatureWrapper<FloatingActionButtonBinding>()
    private val selectionBannerBinding = ViewBoundFeatureWrapper<SelectionBannerBinding>()
    private val selectionHandleBinding = ViewBoundFeatureWrapper<SelectionHandleBinding>()
    private val tabsTrayCtaBinding = ViewBoundFeatureWrapper<TabsTrayInfoBannerBinding>()
    private val secureTabsTrayBinding = ViewBoundFeatureWrapper<SecureTabsTrayBinding>()
    private val tabsFeature = ViewBoundFeatureWrapper<TabsFeature>()
    private val tabsTrayInactiveTabsOnboardingBinding = ViewBoundFeatureWrapper<TabsTrayInactiveTabsOnboardingBinding>()
    private val syncedTabsIntegration = ViewBoundFeatureWrapper<SyncedTabsIntegration>()

    @VisibleForTesting
    @Suppress("VariableNaming")
    internal var _tabsTrayBinding: ComponentTabstray2Binding? = null
    private val tabsTrayBinding get() = _tabsTrayBinding!!

    @VisibleForTesting
    @Suppress("VariableNaming")
    internal var _tabsTrayDialogBinding: FragmentTabTrayDialogBinding? = null
    private val tabsTrayDialogBinding get() = _tabsTrayDialogBinding!!

    @VisibleForTesting
    @Suppress("VariableNaming")
    internal var _fabButtonBinding: ComponentTabstrayFabBinding? = null
    private val fabButtonBinding get() = _fabButtonBinding!!

    @VisibleForTesting
    @Suppress("VariableNaming")
    internal var _tabsTrayComposeBinding: ComponentTabstray3Binding? = null
    private val tabsTrayComposeBinding get() = _tabsTrayComposeBinding!!

    @Suppress("VariableNaming")
    internal var _fabButtonComposeBinding: ComponentTabstray3FabBinding? = null
    private val fabButtonComposeBinding get() = _fabButtonComposeBinding!!

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
//        context?.components?.analytics?.crashReporter?.recordCrashBreadcrumb(
//            Breadcrumb("TabsTrayFragment dismissTabsTray"),
//        )
        setStyle(STYLE_NO_TITLE, R.style.TabTrayDialogStyle)
    }

    @Suppress("LongMethod")
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val args by navArgs<TabsTrayFragmentArgs>()
//        args.accessPoint.takeIf { it != TabsTrayAccessPoint.None }?.let {
//            TabsTray.accessPoint[it.name.lowercase()].add()
//        }
        val initialMode = if (args.enterMultiselect) {
            TabsTrayState.Mode.Select(emptySet())
        } else {
            TabsTrayState.Mode.Normal
        }
        val initialPage = args.page
        val activity = activity as HomeActivity
        val initialInactiveExpanded = requireComponents.appStore.state.inactiveTabsExpanded
        val inactiveTabs = requireComponents.core.store.state.actualInactiveTabs(requireContext().settings())
        val normalTabs = requireComponents.core.store.state.normalTabs - inactiveTabs.toSet()

        tabsTrayStore = StoreProvider.get(this) {
            TabsTrayStore(
                initialState = TabsTrayState(
                    selectedPage = initialPage,
                    mode = initialMode,
                    inactiveTabs = inactiveTabs,
                    inactiveTabsExpanded = initialInactiveExpanded,
                    normalTabs = normalTabs,
                    privateTabs = requireComponents.core.store.state.privateTabs,
                    selectedTabId = requireComponents.core.store.state.selectedTabId,
                ),
                middlewares = listOf(
                    TabsTrayTelemetryMiddleware(),
                ),
            )
        }

        navigationInteractor =
            DefaultNavigationInteractor(
                browserStore = requireComponents.core.store,
                navController = findNavController(),
                dismissTabTray = ::dismissTabsTray,
                dismissTabTrayAndNavigateHome = ::dismissTabsTrayAndNavigateHome,
                showCancelledDownloadWarning = ::showCancelledDownloadWarning,
                accountManager = requireComponents.backgroundServices.accountManager,
            )

        tabsTrayController = DefaultTabsTrayController(
            activity = activity,
            appStore = requireComponents.appStore,
            tabsTrayStore = tabsTrayStore,
            browserStore = requireComponents.core.store,
            settings = requireContext().settings(),
            browsingModeManager = activity.browsingModeManager,
            navController = findNavController(),
            navigateToHomeAndDeleteSession = ::navigateToHomeAndDeleteSession,
            navigationInteractor = navigationInteractor,
            profiler = requireComponents.core.engine.profiler,
            tabsUseCases = requireComponents.useCases.tabsUseCases,
            closeSyncedTabsUseCases = requireComponents.useCases.closeSyncedTabsUseCases,
            bookmarksStorage = requireComponents.core.bookmarksStorage,
            ioDispatcher = Dispatchers.IO,
            collectionStorage = requireComponents.core.tabCollectionStorage,
            selectTabPosition = ::selectTabPosition,
            dismissTray = ::dismissTabsTray,
            showUndoSnackbarForTab = ::showUndoSnackbarForTab,
            showUndoSnackbarForInactiveTab = ::showUndoSnackbarForInactiveTab,
            showUndoSnackbarForSyncedTab = ::showUndoSnackbarForSyncedTab,
            showCancelledDownloadWarning = ::showCancelledDownloadWarning,
            showCollectionSnackbar = ::showCollectionSnackbar,
            showBookmarkSnackbar = ::showBookmarkSnackbar,
        )

        tabsTrayInteractor = DefaultTabsTrayInteractor(
            controller = tabsTrayController,
        )

//        context?.components?.analytics?.crashReporter?.recordCrashBreadcrumb(
//            Breadcrumb("TabsTrayFragment onCreateDialog"),
//        )
        tabsTrayDialog = TabsTrayDialog(requireContext(), theme) { tabsTrayInteractor }
        return tabsTrayDialog
    }

    override fun onPause() {
        super.onPause()
//        context?.components?.analytics?.crashReporter?.recordCrashBreadcrumb(
//            Breadcrumb("TabsTrayFragment onPause"),
//        )
        dialog?.window?.setWindowAnimations(R.style.DialogFragmentRestoreAnimation)
    }

    @Suppress("LongMethod")
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _tabsTrayDialogBinding = FragmentTabTrayDialogBinding.inflate(
            inflater,
            container,
            false,
        )

        if (requireContext().settings().enableTabsTrayToCompose) {
            _tabsTrayComposeBinding = ComponentTabstray3Binding.inflate(
                inflater,
                tabsTrayDialogBinding.root,
                true,
            )

            _fabButtonComposeBinding = ComponentTabstray3FabBinding.inflate(
                inflater,
                tabsTrayDialogBinding.root,
                true,
            )

            tabsTrayComposeBinding.root.setContent {
                FirefoxTheme(theme = Theme.getTheme(allowPrivateTheme = false)) {
                    TabsTray(
                        tabsTrayStore = tabsTrayStore,
                        displayTabsInGrid = requireContext().settings().tabTrayStyle == InfernoSettings.TabTrayStyle.TAB_TRAY_GRID,
                        isInDebugMode = Config.channel.isDebug ||
                            requireComponents.settings.showSecretDebugMenuThisSession,
                        shouldShowTabAutoCloseBanner = requireContext().settings().shouldShowAutoCloseTabsBanner &&
                            requireContext().settings().canShowCfr,
                        shouldShowInactiveTabsAutoCloseDialog =
                        requireContext().settings()::shouldShowInactiveTabsAutoCloseDialog,
                        onTabPageClick = { page ->
                            tabsTrayInteractor.onTrayPositionSelected(page.ordinal, false)
                        },
                        onTabClose = { tab ->
                            tabsTrayInteractor.onTabClosed(tab, TABS_TRAY_FEATURE_NAME)
                        },
                        onTabMediaClick = tabsTrayInteractor::onMediaClicked,
                        onTabClick = { tab ->
                            run outer@{
                                if (!requireContext().settings().hasShownTabSwipeCFR &&
                                    !requireContext().isTabStripEnabled() &&
                                    requireContext().settings().isSwipeToolbarToSwitchTabsEnabled
                                ) {
                                    val normalTabs = tabsTrayStore.state.normalTabs
                                    val currentTabId = tabsTrayStore.state.selectedTabId

                                    if (normalTabs.size >= 2) {
                                        val currentTabPosition = currentTabId
                                            ?.let { getTabPositionFromId(normalTabs, it) }
                                            ?: return@outer
                                        val newTabPosition =
                                            getTabPositionFromId(normalTabs, tab.id)

                                        if (abs(currentTabPosition - newTabPosition) == 1) {
                                            requireContext().settings().shouldShowTabSwipeCFR = true
                                        }
                                    }
                                }
                            }

                            tabsTrayInteractor.onTabSelected(tab, TABS_TRAY_FEATURE_NAME)
                        },
                        onTabLongClick = tabsTrayInteractor::onTabLongClicked,
                        onInactiveTabsHeaderClick = tabsTrayInteractor::onInactiveTabsHeaderClicked,
                        onDeleteAllInactiveTabsClick = tabsTrayInteractor::onDeleteAllInactiveTabsClicked,
                        onInactiveTabsAutoCloseDialogShown = {
                            tabsTrayStore.dispatch(TabsTrayAction.TabAutoCloseDialogShown)
                        },
                        onInactiveTabAutoCloseDialogCloseButtonClick =
                        tabsTrayInteractor::onAutoCloseDialogCloseButtonClicked,
                        onEnableInactiveTabAutoCloseClick = {
                            tabsTrayInteractor.onEnableAutoCloseClicked()
                            showInactiveTabsAutoCloseConfirmationSnackbar()
                        },
                        onInactiveTabClick = tabsTrayInteractor::onInactiveTabClicked,
                        onInactiveTabClose = tabsTrayInteractor::onInactiveTabClosed,
                        onSyncedTabClick = tabsTrayInteractor::onSyncedTabClicked,
                        onSyncedTabClose = tabsTrayInteractor::onSyncedTabClosed,
                        onSaveToCollectionClick = tabsTrayInteractor::onAddSelectedTabsToCollectionClicked,
                        onShareSelectedTabsClick = tabsTrayInteractor::onShareSelectedTabs,
                        onShareAllTabsClick = {
                            if (tabsTrayStore.state.selectedPage == Page.NormalTabs) {
                                tabsTrayStore.dispatch(TabsTrayAction.ShareAllNormalTabs)
                            } else if (tabsTrayStore.state.selectedPage == Page.PrivateTabs) {
                                tabsTrayStore.dispatch(TabsTrayAction.ShareAllPrivateTabs)
                            }

                            navigationInteractor.onShareTabsOfTypeClicked(
                                private = tabsTrayStore.state.selectedPage == Page.PrivateTabs,
                            )
                        },
                        onTabSettingsClick = navigationInteractor::onTabSettingsClicked,
                        onRecentlyClosedClick = navigationInteractor::onOpenRecentlyClosedClicked,
                        onAccountSettingsClick = navigationInteractor::onAccountSettingsClicked,
                        onDeleteAllTabsClick = {
                            if (tabsTrayStore.state.selectedPage == Page.NormalTabs) {
                                tabsTrayStore.dispatch(TabsTrayAction.CloseAllNormalTabs)
                            } else if (tabsTrayStore.state.selectedPage == Page.PrivateTabs) {
                                tabsTrayStore.dispatch(TabsTrayAction.CloseAllPrivateTabs)
                            }

                            navigationInteractor.onCloseAllTabsClicked(
                                private = tabsTrayStore.state.selectedPage == Page.PrivateTabs,
                            )
                        },
                        onDeleteSelectedTabsClick = tabsTrayInteractor::onDeleteSelectedTabsClicked,
                        onBookmarkSelectedTabsClick = tabsTrayInteractor::onBookmarkSelectedTabsClicked,
                        onForceSelectedTabsAsInactiveClick = tabsTrayInteractor::onForceSelectedTabsAsInactiveClicked,
                        onTabsTrayDismiss = ::onTabsTrayDismissed,
                        onTabAutoCloseBannerViewOptionsClick = {
                            navigationInteractor.onTabSettingsClicked()
                            requireContext().settings().shouldShowAutoCloseTabsBanner = false
                            requireContext().settings().lastCfrShownTimeInMillis = System.currentTimeMillis()
                        },
                        onTabAutoCloseBannerDismiss = {
                            requireContext().settings().shouldShowAutoCloseTabsBanner = false
                            requireContext().settings().lastCfrShownTimeInMillis = System.currentTimeMillis()
                        },
                        onTabAutoCloseBannerShown = {},
                        onMove = tabsTrayInteractor::onTabsMove,
                        shouldShowInactiveTabsCFR = {
                            requireContext().settings().shouldShowInactiveTabsOnboardingPopup &&
                                requireContext().settings().canShowCfr
                        },
                        onInactiveTabsCFRShown = {
//                            TabsTray.inactiveTabsCfrVisible.record(NoExtras())
                        },
                        onInactiveTabsCFRClick = {
                            requireContext().settings().shouldShowInactiveTabsOnboardingPopup = false
                            requireContext().settings().lastCfrShownTimeInMillis = System.currentTimeMillis()
                            navigationInteractor.onTabSettingsClicked()
//                            TabsTray.inactiveTabsCfrSettings.record(NoExtras())
                            onTabsTrayDismissed()
                        },
                        onInactiveTabsCFRDismiss = {
                            requireContext().settings().shouldShowInactiveTabsOnboardingPopup = false
                            requireContext().settings().lastCfrShownTimeInMillis = System.currentTimeMillis()
//                            TabsTray.inactiveTabsCfrDismissed.record(NoExtras())
                        },
                    )
                }
            }

            fabButtonComposeBinding.root.setContent {
                FirefoxTheme(theme = Theme.getTheme(allowPrivateTheme = false)) {
                    TabsTrayFab(
                        tabsTrayStore = tabsTrayStore,
                        isSignedIn = requireContext().settings().signedInFxaAccount,
                        onNormalTabsFabClicked = tabsTrayInteractor::onNormalTabsFabClicked,
                        onPrivateTabsFabClicked = tabsTrayInteractor::onPrivateTabsFabClicked,
                        onSyncedTabsFabClicked = tabsTrayInteractor::onSyncedTabsFabClicked,
                    )
                }
            }
        } else {
            _tabsTrayBinding = ComponentTabstray2Binding.inflate(
                inflater,
                tabsTrayDialogBinding.root,
                true,
            )
            _fabButtonBinding = ComponentTabstrayFabBinding.inflate(
                inflater,
                tabsTrayDialogBinding.root,
                true,
            )
        }

        return tabsTrayDialogBinding.root
    }

    override fun onStart() {
        super.onStart()
//        context?.components?.analytics?.crashReporter?.recordCrashBreadcrumb(
//            Breadcrumb("TabsTrayFragment onStart"),
//        )
        findPreviousDialogFragment()?.let { dialog ->
            dialog.onAcceptClicked = ::onCancelDownloadWarningAccepted
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
//        context?.components?.analytics?.crashReporter?.recordCrashBreadcrumb(
//            Breadcrumb("TabsTrayFragment onDestroyView"),
//        )
        _tabsTrayBinding = null
        _tabsTrayDialogBinding = null
        _fabButtonBinding = null
        _tabsTrayComposeBinding = null
        _fabButtonComposeBinding = null
    }

    @Suppress("LongMethod")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
//        TabsTray.opened.record(NoExtras())

        val rootView = if (requireContext().settings().enableTabsTrayToCompose) {
            tabsTrayComposeBinding.root
        } else {
            tabsTrayBinding.tabWrapper
        }

        val newTabFab = if (requireContext().settings().enableTabsTrayToCompose) {
            fabButtonComposeBinding.root
        } else {
            fabButtonBinding.newTabButton
        }

        val behavior = BottomSheetBehavior.from(rootView).apply {
            addBottomSheetCallback(
                TraySheetBehaviorCallback(
                    this,
                    navigationInteractor,
                    tabsTrayDialog,
                    newTabFab,
                ),
            )
            skipCollapsed = true
        }

        trayBehaviorManager = TabSheetBehaviorManager(
            behavior = behavior,
            orientation = resources.configuration.orientation,
            maxNumberOfTabs = max(
                requireContext().components.core.store.state.normalTabs.size,
                requireContext().components.core.store.state.privateTabs.size,
            ),
            numberForExpandingTray = if (requireContext().settings().tabTrayStyle == InfernoSettings.TabTrayStyle.TAB_TRAY_GRID) {
                EXPAND_AT_GRID_SIZE
            } else {
                EXPAND_AT_LIST_SIZE
            },
            displayMetrics = requireContext().resources.displayMetrics,
        )

        setupBackgroundDismissalListener {
            onTabsTrayDismissed()
        }

        if (!requireContext().settings().enableTabsTrayToCompose) {
            val activity = activity as HomeActivity

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
                fabButtonBinding.newTabButton.accessibilityTraversalAfter =
                    tabsTrayBinding.tabLayout.id
            }

            setupMenu(navigationInteractor)
            setupPager(
                context = view.context,
                lifecycleOwner = viewLifecycleOwner,
                store = tabsTrayStore,
                trayInteractor = tabsTrayInteractor,
            )

            tabsTrayCtaBinding.set(
                feature = TabsTrayInfoBannerBinding(
                    context = view.context,
                    store = requireComponents.core.store,
                    infoBannerView = tabsTrayBinding.infoBanner,
                    settings = requireComponents.settings,
                    navigationInteractor = navigationInteractor,
                ),
                owner = this,
                view = view,
            )

            tabLayoutMediator.set(
                feature = TabLayoutMediator(
                    tabLayout = tabsTrayBinding.tabLayout,
                    tabPager = tabsTrayBinding.tabsTray,
                    interactor = tabsTrayInteractor,
                    browsingModeManager = activity.browsingModeManager,
                    tabsTrayStore = tabsTrayStore,
                ),
                owner = this,
                view = view,
            )

            val tabsTrayTabCounter2Binding = TabsTrayTabCounter2Binding.bind(
                tabsTrayBinding.tabLayout,
            )

            tabCounterBinding.set(
                feature = TabCounterBinding(
                    store = requireComponents.core.store,
                    counter = tabsTrayTabCounter2Binding.tabCounter,
                ),
                owner = this,
                view = view,
            )

            floatingActionButtonBinding.set(
                feature = FloatingActionButtonBinding(
                    store = tabsTrayStore,
                    actionButton = fabButtonBinding.newTabButton,
                    interactor = tabsTrayInteractor,
                    isSignedIn = requireContext().settings().signedInFxaAccount,
                ),
                owner = this,
                view = view,
            )

            val tabsTrayMultiselectItemsBinding = TabstrayMultiselectItemsBinding.bind(
                tabsTrayBinding.root,
            )

            selectionBannerBinding.set(
                feature = SelectionBannerBinding(
                    context = requireContext(),
                    binding = tabsTrayBinding,
                    tabsTrayStore = tabsTrayStore,
                    interactor = tabsTrayInteractor,
                    backgroundView = tabsTrayBinding.topBar,
                    showOnSelectViews = VisibilityModifier(
                        tabsTrayMultiselectItemsBinding.collectMultiSelect,
                        tabsTrayMultiselectItemsBinding.shareMultiSelect,
                        tabsTrayMultiselectItemsBinding.menuMultiSelect,
                        tabsTrayBinding.multiselectTitle,
                        tabsTrayBinding.exitMultiSelect,
                    ),
                    showOnNormalViews = VisibilityModifier(
                        tabsTrayBinding.tabLayout,
                        tabsTrayBinding.tabTrayOverflow,
                        fabButtonBinding.newTabButton,
                    ),
                ),
                owner = this,
                view = view,
            )

            selectionHandleBinding.set(
                feature = SelectionHandleBinding(
                    store = tabsTrayStore,
                    handle = tabsTrayBinding.handle,
                    containerLayout = tabsTrayBinding.tabWrapper,
                ),
                owner = this,
                view = view,
            )

            tabsTrayInactiveTabsOnboardingBinding.set(
                feature = TabsTrayInactiveTabsOnboardingBinding(
                    context = requireContext(),
                    browserStore = requireComponents.core.store,
                    tabsTrayBinding = tabsTrayBinding,
                    settings = requireComponents.settings,
                    navigationInteractor = navigationInteractor,
                ),
                owner = this,
                view = view,
            )
        }

        inactiveTabsBinding.set(
            feature = InactiveTabsBinding(
                tabsTrayStore = tabsTrayStore,
                appStore = requireComponents.appStore,
            ),
            owner = this,
            view = view,
        )

        tabsFeature.set(
            feature = TabsFeature(
                tabsTray = TabSorter(
                    requireContext().settings(),
                    tabsTrayStore,
                ),
                store = requireContext().components.core.store,
            ),
            owner = this,
            view = view,
        )

        secureTabsTrayBinding.set(
            feature = SecureTabsTrayBinding(
                store = tabsTrayStore,
                settings = requireComponents.settings,
                fragment = this,
                dialog = dialog as TabsTrayDialog,
            ),
            owner = this,
            view = view,
        )

        syncedTabsIntegration.set(
            feature = SyncedTabsIntegration(
                store = tabsTrayStore,
                context = requireContext(),
                navController = findNavController(),
                storage = requireComponents.backgroundServices.syncedTabsStorage,
                commands = requireComponents.backgroundServices.syncedTabsCommands,
                accountManager = requireComponents.backgroundServices.accountManager,
                lifecycleOwner = this,
            ),
            owner = this,
            view = view,
        )

        setFragmentResultListener(ShareFragment.RESULT_KEY) { _, _ ->
            dismissTabsTray()
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)

        trayBehaviorManager.updateDependingOnOrientation(newConfig.orientation)
        if (!requireContext().settings().enableTabsTrayToCompose && requireContext().settings().tabTrayStyle == InfernoSettings.TabTrayStyle.TAB_TRAY_GRID) {
            tabsTrayBinding.tabsTray.adapter?.notifyDataSetChanged()
        }
    }

    @VisibleForTesting
    internal fun onCancelDownloadWarningAccepted(tabId: String?, source: String?) {
        if (tabId != null) {
            tabsTrayInteractor.onDeletePrivateTabWarningAccepted(tabId, source)
        } else {
            navigationInteractor.onCloseAllPrivateTabsWarningConfirmed(private = true)
        }
    }

    @VisibleForTesting
    internal fun showCancelledDownloadWarning(downloadCount: Int, tabId: String?, source: String?) {
//        context?.components?.analytics?.crashReporter?.recordCrashBreadcrumb(
//            Breadcrumb("DownloadCancelDialogFragment show"),
//        )
        val dialog = DownloadCancelDialogFragment.newInstance(
            downloadCount = downloadCount,
            tabId = tabId,
            source = source,
            promptStyling = DownloadCancelDialogFragment.PromptStyling(
                gravity = Gravity.BOTTOM,
                shouldWidthMatchParent = true,
                positiveButtonBackgroundColor = ThemeManager.resolveAttribute(
                    R.attr.accent,
                    requireContext(),
                ),
                positiveButtonTextColor = ThemeManager.resolveAttribute(
                    R.attr.textOnColorPrimary,
                    requireContext(),
                ),
                positiveButtonRadius = (resources.getDimensionPixelSize(R.dimen.tab_corner_radius)).toFloat(),
            ),

            onPositiveButtonClicked = ::onCancelDownloadWarningAccepted,
        )
        dialog.show(parentFragmentManager, DOWNLOAD_CANCEL_DIALOG_FRAGMENT_TAG)
    }

    @UiThread
    internal fun showUndoSnackbarForSyncedTab(closeOperation: CloseTabsUseCases.UndoableOperation) {
        lifecycleScope.allowUndo(
            view = requireView(),
            message = getString(R.string.snackbar_tab_closed),
            undoActionTitle = getString(R.string.snackbar_deleted_undo),
            onCancel = closeOperation::undo,
            operation = { },
            elevation = ELEVATION,
            anchorView = getSnackbarAnchor(),
        )
    }

    @VisibleForTesting
    internal fun showUndoSnackbarForTab(isPrivate: Boolean) {
        val snackbarMessage =
            when (isPrivate) {
                true -> getString(R.string.snackbar_private_tab_closed)
                false -> getString(R.string.snackbar_tab_closed)
            }
        val pagePosition = if (isPrivate) Page.PrivateTabs.ordinal else Page.NormalTabs.ordinal

        lifecycleScope.allowUndo(
            view = requireView(),
            message = snackbarMessage,
            undoActionTitle = getString(R.string.snackbar_deleted_undo),
            onCancel = {
                requireComponents.useCases.tabsUseCases.undo.invoke()

                if (requireContext().settings().enableTabsTrayToCompose) {
                    tabsTrayStore.dispatch(TabsTrayAction.PageSelected(Page.positionToPage(pagePosition)))
                } else {
                    tabLayoutMediator.withFeature {
                        it.selectTabAtPosition(pagePosition)
                    }
                }
            },
            operation = { },
            elevation = ELEVATION,
            anchorView = getSnackbarAnchor(),
        )
    }

    @VisibleForTesting
    internal fun showUndoSnackbarForInactiveTab(numClosed: Int) {
        val snackbarMessage =
            when (numClosed == 1) {
                true -> getString(R.string.snackbar_tab_closed)
                false -> getString(R.string.snackbar_num_tabs_closed, numClosed.toString())
            }

        lifecycleScope.allowUndo(
            view = requireView(),
            message = snackbarMessage,
            undoActionTitle = getString(R.string.snackbar_deleted_undo),
            onCancel = {
                requireComponents.useCases.tabsUseCases.undo.invoke()

                if (requireContext().settings().enableTabsTrayToCompose) {
                    tabsTrayStore.dispatch(TabsTrayAction.PageSelected(Page.positionToPage(Page.NormalTabs.ordinal)))
                } else {
                    tabLayoutMediator.withFeature {
                        it.selectTabAtPosition(Page.NormalTabs.ordinal)
                    }
                }
            },
            operation = { },
            elevation = ELEVATION,
            anchorView = getSnackbarAnchor(),
        )
    }

    @VisibleForTesting
    internal fun setupPager(
        context: Context,
        lifecycleOwner: LifecycleOwner,
        store: TabsTrayStore,
        trayInteractor: TabsTrayInteractor,
    ) {
        tabsTrayBinding.tabsTray.apply {
            adapter = TrayPagerAdapter(
                context = context,
                lifecycleOwner = lifecycleOwner,
                tabsTrayStore = store,
                interactor = trayInteractor,
                browserStore = requireComponents.core.store,
                appStore = requireComponents.appStore,
            )
            isUserInputEnabled = false
        }
    }

    @VisibleForTesting
    internal fun setupMenu(navigationInteractor: NavigationInteractor) {
        tabsTrayBinding.tabTrayOverflow.setOnClickListener { anchor ->

//            TabsTray.menuOpened.record(NoExtras())

            val menu = getTrayMenu(
                context = requireContext(),
                browserStore = requireComponents.core.store,
                tabsTrayStore = tabsTrayStore,
                tabLayout = tabsTrayBinding.tabLayout,
                navigationInteractor = navigationInteractor,
            ).build()

            menu.showWithTheme(anchor)
        }
    }

    @VisibleForTesting
    internal fun getTrayMenu(
        context: Context,
        browserStore: BrowserStore,
        tabsTrayStore: TabsTrayStore,
        tabLayout: TabLayout,
        navigationInteractor: NavigationInteractor,
    ) = MenuIntegration(context, browserStore, tabsTrayStore, tabLayout, navigationInteractor)

    @VisibleForTesting
    internal fun setupBackgroundDismissalListener(block: (View) -> Unit) {
        tabsTrayDialogBinding.tabLayout.setOnClickListener(block)
        if (!requireContext().settings().enableTabsTrayToCompose) {
            tabsTrayBinding.handle.setOnClickListener(block)
        }
    }

    @VisibleForTesting
    internal fun dismissTabsTrayAndNavigateHome(sessionId: String) {
        navigateToHomeAndDeleteSession(sessionId)
        dismissTabsTray()
    }

    internal val homeViewModel: HomeScreenViewModel by activityViewModels()

    @VisibleForTesting
    internal fun navigateToHomeAndDeleteSession(sessionId: String) {
        homeViewModel.sessionToDelete = sessionId
        val directions = NavGraphDirections.actionGlobalHome()
        findNavController().navigate(directions)
    }

    @VisibleForTesting
    internal fun selectTabPosition(position: Int, smoothScroll: Boolean) {
        if (!requireContext().settings().enableTabsTrayToCompose) {
            tabsTrayBinding.tabsTray.setCurrentItem(position, smoothScroll)
            tabsTrayBinding.tabLayout.getTabAt(position)?.select()
        }
    }

    @VisibleForTesting
    internal fun getTabPositionFromId(tabsList: List<TabSessionState>, tabId: String): Int {
        tabsList.forEachIndexed { index, tab -> if (tab.id == tabId) return index }
        return -1
    }

    @VisibleForTesting
    internal fun dismissTabsTray() {
        // This should always be the last thing we do because nothing (e.g. telemetry)
        // is guaranteed after that.
//        context?.components?.analytics?.crashReporter?.recordCrashBreadcrumb(
//            Breadcrumb("TabsTrayFragment dismissTabsTray"),
//        )
        dismissAllowingStateLoss()
    }

    private fun showCollectionSnackbar(
        tabSize: Int,
        isNewCollection: Boolean = false,
    ) {
        runIfFragmentIsAttached {
            showSnackbar(
                snackBarParentView = requireView(),
                snackbarState = SnackbarState(
                    message = getString(
                        when {
                            isNewCollection -> {
                                R.string.create_collection_tabs_saved_new_collection
                            }
                            tabSize > 1 -> {
                                R.string.create_collection_tabs_saved
                            }
                            else -> {
                                R.string.create_collection_tab_saved
                            }
                        },
                    ),
                    duration = SnackbarDuration.Long.toSnackbarStateDuration(),
                    action = Action(
                        label = getString(R.string.create_collection_view),
                        onClick = {
                            findNavController().navigate(
                                TabsTrayFragmentDirections.actionGlobalHome(
                                    focusOnAddressBar = false,
                                    scrollToCollection = true,
                                ),
                            )
                            dismissTabsTray()
                        },
                    ),
                ),
            )
        }
    }

    private fun showBookmarkSnackbar(
        tabSize: Int,
        parentFolderTitle: String?,
    ) {
        val displayFolderTitle = parentFolderTitle ?: getString(R.string.library_bookmarks)
        val displayResId = when {
            tabSize > 1 -> {
                R.string.snackbar_message_bookmarks_saved_in
            }
            else -> {
                R.string.bookmark_saved_in_folder_snackbar
            }
        }

        showSnackbar(
            snackBarParentView = requireView(),
            snackbarState = SnackbarState(
                message = getString(displayResId, displayFolderTitle),
                duration = SnackbarDuration.Long.toSnackbarStateDuration(),
                action = Action(
                    label = getString(R.string.create_collection_view),
                    onClick = {
                        findNavController().navigate(
                            TabsTrayFragmentDirections.actionGlobalBookmarkFragment(BookmarkRoot.Mobile.id),
                        )
                        dismissTabsTray()
                    },
                ),
            ),
        )
    }

    private fun findPreviousDialogFragment(): DownloadCancelDialogFragment? {
        return parentFragmentManager
            .findFragmentByTag(DOWNLOAD_CANCEL_DIALOG_FRAGMENT_TAG) as? DownloadCancelDialogFragment
    }

    private fun getSnackbarAnchor(): View? = when {
        requireContext().settings().enableTabsTrayToCompose -> fabButtonComposeBinding.root
        fabButtonBinding.newTabButton.isVisible -> fabButtonBinding.newTabButton
        else -> null
    }

    private fun showInactiveTabsAutoCloseConfirmationSnackbar() {
        showSnackbar(
            snackBarParentView = tabsTrayComposeBinding.root,
            snackbarState = SnackbarState(
                message = getString(R.string.inactive_tabs_auto_close_message_snackbar),
                duration = SnackbarDuration.Long.toSnackbarStateDuration(),
            ),
        )
    }

    private fun showSnackbar(
        snackBarParentView: View,
        snackbarState: SnackbarState,
    ) {
        Snackbar.make(
            snackBarParentView = snackBarParentView,
            snackbarState = snackbarState,
        ).apply {
            setAnchorView(getSnackbarAnchor())
            view.elevation = ELEVATION
            show()
        }
    }

    private fun onTabsTrayDismissed() {
//        context?.components?.analytics?.crashReporter?.recordCrashBreadcrumb(
//            Breadcrumb("TabsTrayFragment onTabsTrayDismissed"),
//        )
//        TabsTray.closed.record(NoExtras())
        dismissAllowingStateLoss()
    }

    companion object {
        private const val DOWNLOAD_CANCEL_DIALOG_FRAGMENT_TAG = "DOWNLOAD_CANCEL_DIALOG_FRAGMENT_TAG"

        // Minimum number of list items for which to show the tabs tray as expanded.
        const val EXPAND_AT_LIST_SIZE = 4

        // Minimum number of grid items for which to show the tabs tray as expanded.
        private const val EXPAND_AT_GRID_SIZE = 3

        // Elevation for undo toasts
        @VisibleForTesting
        internal const val ELEVATION = 80f

        private const val TABS_TRAY_FEATURE_NAME = "Tabs tray"
    }
}
