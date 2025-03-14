/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package com.shmibblez.inferno.components.toolbar

import androidx.navigation.NavController
import mozilla.components.browser.state.action.ContentAction
import mozilla.components.browser.state.ext.getUrl
import mozilla.components.browser.state.selector.findCustomTabOrSelectedTab
import mozilla.components.browser.state.selector.findTab
import mozilla.components.browser.state.selector.getNormalOrPrivateTabs
import mozilla.components.browser.state.selector.selectedTab
import mozilla.components.browser.state.state.SessionState
import mozilla.components.browser.state.store.BrowserStore
import mozilla.components.concept.engine.EngineView
import mozilla.components.concept.engine.prompt.ShareData
import mozilla.components.feature.tabs.TabsUseCases
import mozilla.components.support.ktx.kotlin.isUrl
import mozilla.components.ui.tabcounter.TabCounterMenu
//import mozilla.telemetry.glean.private.NoExtras
//import com.shmibblez.inferno.GleanMetrics.Events
//import com.shmibblez.inferno.GleanMetrics.NavigationBar
//import com.shmibblez.inferno.GleanMetrics.ReaderMode
//import com.shmibblez.inferno.GleanMetrics.Translations
import com.shmibblez.inferno.HomeActivity
import com.shmibblez.inferno.NavGraphDirections
import com.shmibblez.inferno.R
import com.shmibblez.inferno.browser.BrowserAnimator
import com.shmibblez.inferno.browser.BrowserAnimator.Companion.getToolbarNavOptions
import com.shmibblez.inferno.browser.BrowserComponentWrapperFragmentDirections
import com.shmibblez.inferno.browser.browsingmode.BrowsingMode
import com.shmibblez.inferno.browser.readermode.ReaderModeController
import com.shmibblez.inferno.components.AppStore
import com.shmibblez.inferno.components.appstate.AppAction.SnackbarAction
import com.shmibblez.inferno.components.menu.MenuAccessPoint
import com.shmibblez.inferno.components.toolbar.interactor.BrowserToolbarInteractor
import com.shmibblez.inferno.ext.components
import com.shmibblez.inferno.ext.nav
import com.shmibblez.inferno.ext.navigateSafe
import com.shmibblez.inferno.ext.settings
import com.shmibblez.inferno.home.HomeFragment
import com.shmibblez.inferno.home.HomeScreenViewModel
import com.shmibblez.inferno.utils.Settings

/**
 * An interface that handles the view manipulation of the BrowserToolbar, triggered by the Interactor
 */
interface BrowserToolbarController {
    fun handleScroll(offset: Int)
    fun handleToolbarPaste(text: String)
    fun handleToolbarPasteAndGo(text: String)
    fun handleToolbarClick()
    fun handleTabCounterClick()
    fun handleTabCounterItemInteraction(item: TabCounterMenu.Item)
    fun handleReaderModePressed(enabled: Boolean)

    /**
     * @see [BrowserToolbarInteractor.onHomeButtonClicked]
     */
    fun handleHomeButtonClick()

    /**
     * @see [BrowserToolbarInteractor.onEraseButtonClicked]
     */
    fun handleEraseButtonClick()

    /**
     * @see [BrowserToolbarInteractor.com.shmibblez.inferno]
     */
    fun handleTranslationsButtonClick()

    /**
     * @see [BrowserToolbarInteractor.onShareActionClicked]
     */
    fun onShareActionClicked()

    /**
     * @see [BrowserToolbarInteractor.onNewTabButtonClicked]
     */
    fun handleNewTabButtonClick()

    /**
     * @see [BrowserToolbarInteractor.onNewTabButtonLongClicked]
     */
    fun handleNewTabButtonLongClick()

    /**
     * @see [BrowserToolbarInteractor.onMenuButtonClicked]
     */
    fun handleMenuButtonClicked(
        accessPoint: MenuAccessPoint,
        customTabSessionId: String? = null,
        isSandboxCustomTab: Boolean = false,
    )
}

@Suppress("LongParameterList")
class DefaultBrowserToolbarController(
    private val store: BrowserStore,
    private val appStore: AppStore,
    private val tabsUseCases: TabsUseCases,
    private val activity: HomeActivity,
    private val settings: Settings,
    private val navController: NavController,
    private val readerModeController: ReaderModeController,
    private val engineView: EngineView,
    private val homeViewModel: HomeScreenViewModel,
    private val customTabSessionId: String?,
    private val browserAnimator: BrowserAnimator,
    private val onTabCounterClicked: () -> Unit,
    private val onCloseTab: (SessionState) -> Unit,
) : BrowserToolbarController {

    private val currentSession
        get() = store.state.findCustomTabOrSelectedTab(customTabSessionId)

    override fun handleToolbarPaste(text: String) {
        navController.nav(
            R.id.browserComponentWrapperFragment,
            BrowserComponentWrapperFragmentDirections.actionGlobalSearchDialog(
                sessionId = currentSession?.id,
                pastedText = text,
            ),
            getToolbarNavOptions(activity),
        )
    }

    override fun handleToolbarPasteAndGo(text: String) {
        if (text.isUrl()) {
            store.updateSearchTermsOfSelectedSession("")
            activity.components.useCases.sessionUseCases.loadUrl(text)
            return
        }

        store.updateSearchTermsOfSelectedSession(text)
        activity.components.useCases.searchUseCases.defaultSearch.invoke(
            text,
            sessionId = store.state.selectedTabId,
        )
    }

    override fun handleToolbarClick() {
//        Events.searchBarTapped.record(Events.SearchBarTappedExtra("BROWSER"))
        // If we're displaying awesomebar search results, Home screen will not be visible (it's
        // covered up with the search results). So, skip the navigation event in that case.
        // If we don't, there's a visual flickr as we navigate to Home and then display search
        // results on top it.
        if (currentSession?.content?.searchTerms.isNullOrBlank()) {
            browserAnimator.captureEngineViewAndDrawStatically {
                navController.navigate(
                    BrowserComponentWrapperFragmentDirections.actionGlobalHome(),
                )
                navController.navigate(
                    BrowserComponentWrapperFragmentDirections.actionGlobalSearchDialog(
                        currentSession?.id,
                    ),
                    getToolbarNavOptions(activity),
                )
            }
        } else {
            navController.navigate(
                BrowserComponentWrapperFragmentDirections.actionGlobalSearchDialog(
                    currentSession?.id,
                ),
                getToolbarNavOptions(activity),
            )
        }
    }

    override fun handleTabCounterClick() {
        onTabCounterClicked.invoke()
    }

    override fun handleReaderModePressed(enabled: Boolean) {
        if (enabled) {
            readerModeController.showReaderView()
//            ReaderMode.opened.record(NoExtras())
        } else {
            readerModeController.hideReaderView()
//            ReaderMode.closed.record(NoExtras())
        }
    }

    override fun handleTabCounterItemInteraction(item: TabCounterMenu.Item) {
        when (item) {
            is TabCounterMenu.Item.CloseTab -> {
                store.state.selectedTab?.let {
                    // When closing the last tab we must show the undo snackbar in the home fragment
                    if (store.state.getNormalOrPrivateTabs(it.content.private).count() == 1) {
                        homeViewModel.sessionToDelete = it.id
                        navController.navigate(
                            BrowserComponentWrapperFragmentDirections.actionGlobalHome(),
                        )
                    } else {
                        onCloseTab.invoke(it)
                        tabsUseCases.removeTab(it.id, selectParentIfExists = true)
                    }
                }
            }
            is TabCounterMenu.Item.NewTab -> {
                activity.browsingModeManager.mode = BrowsingMode.Normal
                navController.navigate(
                    BrowserComponentWrapperFragmentDirections.actionGlobalHome(focusOnAddressBar = true),
                )
            }
            is TabCounterMenu.Item.NewPrivateTab -> {
                activity.browsingModeManager.mode = BrowsingMode.Private
                navController.navigate(
                    BrowserComponentWrapperFragmentDirections.actionGlobalHome(focusOnAddressBar = true),
                )
            }
        }
    }

    override fun handleScroll(offset: Int) {
        if (activity.settings().isDynamicToolbarEnabled) {
            engineView.setVerticalClipping(offset)
        }
    }

    override fun handleHomeButtonClick() {
//        Events.browserToolbarHomeTapped.record(NoExtras())
        browserAnimator.captureEngineViewAndDrawStatically {
            navController.navigate(
                BrowserComponentWrapperFragmentDirections.actionGlobalHome(),
            )
        }
    }

    override fun handleEraseButtonClick() {
//        Events.browserToolbarEraseTapped.record(NoExtras())
        homeViewModel.sessionToDelete = HomeFragment.ALL_PRIVATE_TABS
        val directions = BrowserComponentWrapperFragmentDirections.actionGlobalHome()
        navController.navigate(directions)
    }

    override fun handleTranslationsButtonClick() {
//        Translations.action.record(Translations.ActionExtra("main_flow_toolbar"))

        appStore.dispatch(SnackbarAction.SnackbarDismissed)

        val directions =
            BrowserComponentWrapperFragmentDirections.actionBrowserFragmentToTranslationsDialogFragment()
        navController.navigateSafe(R.id.browserComponentWrapperFragment, directions)
    }

    override fun onShareActionClicked() {
        val sessionId = currentSession?.id
        val url = sessionId?.let {
            store.state.findTab(it)?.getUrl()
        }
        val directions = NavGraphDirections.actionGlobalShareFragment(
            sessionId = sessionId,
            data = arrayOf(
                ShareData(
                    url = url,
                    title = currentSession?.content?.title,
                ),
            ),
            showPage = true,
        )
        navController.navigate(directions)
    }

    override fun handleNewTabButtonClick() {
        if (settings.enableHomepageAsNewTab) {
            tabsUseCases.addTab.invoke(
                startLoading = false,
                private = currentSession?.content?.private ?: false,
            )
        }

//        NavigationBar.browserNewTabTapped.record(NoExtras())

        browserAnimator.captureEngineViewAndDrawStatically {
            navController.navigate(
                BrowserComponentWrapperFragmentDirections.actionGlobalHome(focusOnAddressBar = true),
            )
        }
    }

    override fun handleNewTabButtonLongClick() {
//        NavigationBar.browserNewTabLongTapped.record(NoExtras())
    }

    override fun handleMenuButtonClicked(
        accessPoint: MenuAccessPoint,
        customTabSessionId: String?,
        isSandboxCustomTab: Boolean,
    ) {
        navController.navigate(
            BrowserComponentWrapperFragmentDirections.actionGlobalMenuDialogFragment(
                accesspoint = accessPoint,
                customTabSessionId = customTabSessionId,
                isSandboxCustomTab = isSandboxCustomTab,
            ),
        )
    }

    companion object {
        internal const val TELEMETRY_BROWSER_IDENTIFIER = "browserMenu"
    }
}

private fun BrowserStore.updateSearchTermsOfSelectedSession(
    searchTerms: String,
) {
    val selectedTabId = state.selectedTabId ?: return

    dispatch(
        ContentAction.UpdateSearchTermsAction(
            selectedTabId,
            searchTerms,
        ),
    )
}
