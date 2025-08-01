/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package com.shmibblez.inferno.ext

import android.app.Activity
import android.app.role.RoleManager
import android.content.Intent
import android.os.Build
import android.provider.Settings
import android.view.View
import android.view.WindowManager
import androidx.annotation.DrawableRes
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.os.bundleOf
import androidx.navigation.NavDirections
import com.shmibblez.inferno.BrowserDirection
import com.shmibblez.inferno.HomeActivity
import com.shmibblez.inferno.NavGraphDirections
import com.shmibblez.inferno.R
import com.shmibblez.inferno.addons.AddonDetailsFragmentDirections
import com.shmibblez.inferno.addons.AddonPermissionsDetailsFragmentDirections
import com.shmibblez.inferno.addons.AddonsManagementFragmentDirections
import com.shmibblez.inferno.components.menu.MenuDialogFragmentDirections
import com.shmibblez.inferno.exceptions.trackingprotection.TrackingProtectionExceptionsFragmentDirections
import com.shmibblez.inferno.home.HomeFragmentDirections
import com.shmibblez.inferno.library.bookmarks.BookmarkFragmentDirections
import com.shmibblez.inferno.library.history.HistoryFragmentDirections
import com.shmibblez.inferno.library.historymetadata.HistoryMetadataGroupFragmentDirections
import com.shmibblez.inferno.library.recentlyclosed.RecentlyClosedFragmentDirections
import com.shmibblez.inferno.search.SearchDialogFragmentDirections
import com.shmibblez.inferno.settings.HttpsOnlyFragmentDirections
import com.shmibblez.inferno.settings.SettingsFragmentDirections
import com.shmibblez.inferno.settings.SupportUtils
import com.shmibblez.inferno.settings.TrackingProtectionFragmentDirections
import com.shmibblez.inferno.settings.about.AboutFragmentDirections
import com.shmibblez.inferno.settings.logins.fragment.LoginDetailFragmentDirections
import com.shmibblez.inferno.settings.logins.fragment.SavedLoginsAuthFragmentDirections
import com.shmibblez.inferno.settings.search.SaveSearchEngineFragmentDirections
import com.shmibblez.inferno.settings.search.SearchEngineFragmentDirections
import com.shmibblez.inferno.settings.studies.StudiesFragmentDirections
import com.shmibblez.inferno.settings.wallpaper.WallpaperSettingsFragmentDirections
import com.shmibblez.inferno.share.AddNewDeviceFragmentDirections
import com.shmibblez.inferno.shopping.ReviewQualityCheckFragmentDirections
import com.shmibblez.inferno.tabstray.TabsTrayFragmentDirections
import com.shmibblez.inferno.trackingprotection.TrackingProtectionPanelDialogFragmentDirections
import com.shmibblez.inferno.translations.TranslationsDialogFragmentDirections
import com.shmibblez.inferno.translations.preferences.downloadlanguages.DownloadLanguagesPreferenceFragmentDirections
import com.shmibblez.inferno.webcompat.ui.WebCompatReporterFragmentDirections
import mozilla.components.concept.engine.EngineSession
import mozilla.components.feature.intent.ext.getSessionId
import mozilla.components.support.utils.EXTRA_ACTIVITY_REFERRER_PACKAGE
import mozilla.components.support.utils.SafeIntent

/**
 * Attempts to call immersive mode using the View to hide the status bar and navigation buttons.
 *
 * We don't use the equivalent function from Android Components because the stable flag messes
 * with the toolbar. See #1998 and #3272.
 */
@Deprecated(
    message = "Use the Android Component implementation instead.",
    replaceWith = ReplaceWith(
        "enterToImmersiveMode()",
        "mozilla.components.support.ktx.android.view.enterToImmersiveMode",
    ),
)
fun Activity.enterToImmersiveMode() {
    window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    // This will be addressed on https://github.com/mozilla-mobile/fenix/issues/17804
    @Suppress("DEPRECATION")
    window.decorView.systemUiVisibility = (
        View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
            or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
            or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
            or View.SYSTEM_UI_FLAG_FULLSCREEN
            or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        )
}

//fun Activity.breadcrumb(
//    message: String,
//    data: Map<String, String> = emptyMap(),
//) {
//    components.analytics.crashReporter.recordCrashBreadcrumb(
//        Breadcrumb(
//            category = this::class.java.simpleName,
//            message = message,
//            data = data + mapOf(
//                "instance" to this.hashCode().toString(),
//            ),
//            level = Breadcrumb.Level.INFO,
//        ),
//    )
//}

/**
 * Opens Android's Manage Default Apps Settings if possible.
 * Otherwise navigates to the Sumo article indicating why it couldn't open it.
 *
 * @param from fallback direction in case, couldn't open the setting.
 * @param flags fallback flags for when opening the Sumo article page.
 * @param useCustomTab fallback to open the Sumo article in a custom tab.
 */
fun Activity.openSetDefaultBrowserOption(
    from: BrowserDirection = BrowserDirection.FromSettings,
    flags: EngineSession.LoadUrlFlags = EngineSession.LoadUrlFlags.none(),
    useCustomTab: Boolean = false,
) {
    when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q -> {
            getSystemService(RoleManager::class.java).also {
                if (it.isRoleAvailable(RoleManager.ROLE_BROWSER) && !it.isRoleHeld(
                        RoleManager.ROLE_BROWSER,
                    )
                ) {
                    startActivityForResult(
                        it.createRequestRoleIntent(RoleManager.ROLE_BROWSER),
                        REQUEST_CODE_BROWSER_ROLE,
                    )
                } else {
                    navigateToDefaultBrowserAppsSettings(
                        useCustomTab = useCustomTab,
                        from = from,
                        flags = flags,
                    )
                }
            }
        }
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.N -> {
            navigateToDefaultBrowserAppsSettings(
                useCustomTab = useCustomTab,
                from = from,
                flags = flags,
            )
        }
        else -> {
            openDefaultBrowserSumoPage(useCustomTab, from, flags)
        }
    }
}

/**
 * Checks if the app can prompt the user to set it as the default browser.
 *
 * From Android 10, a new method to prompt the user to set default apps has been introduced.
 * This method checks if the app can prompt the user to set it as the default browser
 * based on the Android version and the availability of the ROLE_BROWSER.
 */
fun Activity.isDefaultBrowserPromptSupported(): Boolean {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        getSystemService(RoleManager::class.java).also {
            if (it.isRoleAvailable(RoleManager.ROLE_BROWSER) && !it.isRoleHeld(
                    RoleManager.ROLE_BROWSER,
                )
            ) {
                return true
            }
        }
    }
    return false
}

@RequiresApi(Build.VERSION_CODES.N)
private fun Activity.navigateToDefaultBrowserAppsSettings(
    from: BrowserDirection,
    flags: EngineSession.LoadUrlFlags,
    useCustomTab: Boolean,
) {
    val intent = Intent(Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS).apply {
        putExtra(SETTINGS_SELECT_OPTION_KEY, DEFAULT_BROWSER_APP_OPTION)
        putExtra(
            SETTINGS_SHOW_FRAGMENT_ARGS,
            bundleOf(SETTINGS_SELECT_OPTION_KEY to DEFAULT_BROWSER_APP_OPTION),
        )
    }
    startExternalActivitySafe(
        intent = intent,
        onActivityNotPresent = {
            openDefaultBrowserSumoPage(useCustomTab = useCustomTab, from = from, flags = flags)
        },
    )
}

private fun Activity.openDefaultBrowserSumoPage(
    useCustomTab: Boolean,
    from: BrowserDirection,
    flags: EngineSession.LoadUrlFlags,
) {
    val sumoDefaultBrowserUrl = SupportUtils.getGenericSumoURLForTopic(
        topic = SupportUtils.SumoTopic.SET_AS_DEFAULT_BROWSER,
    )
    if (useCustomTab) {
        startActivity(
            SupportUtils.createSandboxCustomTabIntent(
                context = this,
                url = sumoDefaultBrowserUrl,
            ),
        )
    } else {
        (this as HomeActivity).openToBrowserAndLoad(
            searchTermOrURL = sumoDefaultBrowserUrl,
            newTab = true,
            from = from,
            flags = flags,
        )
    }
}

/**
 * Sets the icon for the back (up) navigation button.
 * @param icon The resource id of the icon.
 */
fun Activity.setNavigationIcon(
    @DrawableRes icon: Int,
) {
//    (this as? AppCompatActivity)?.supportActionBar?.let {
//        it.setDisplayHomeAsUpEnabled(true)
//        it.setHomeAsUpIndicator(icon)
//        it.setHomeActionContentDescription(R.string.action_bar_up_description)
//    }
}

private fun getHomeNavDirections(
    from: BrowserDirection,
): NavDirections = when (from) {
    BrowserDirection.FromGlobal -> NavGraphDirections.actionGlobalBrowser()

    BrowserDirection.FromHome -> HomeFragmentDirections.actionGlobalBrowser()

    BrowserDirection.FromWallpaper -> WallpaperSettingsFragmentDirections.actionGlobalBrowser()

    BrowserDirection.FromSearchDialog -> SearchDialogFragmentDirections.actionGlobalBrowser()

    BrowserDirection.FromSettings -> SettingsFragmentDirections.actionGlobalBrowser()

    BrowserDirection.FromBookmarks -> BookmarkFragmentDirections.actionGlobalBrowser()

    BrowserDirection.FromHistory -> HistoryFragmentDirections.actionGlobalBrowser()

    BrowserDirection.FromHistoryMetadataGroup -> HistoryMetadataGroupFragmentDirections.actionGlobalBrowser()

    BrowserDirection.FromTrackingProtectionExceptions ->
        TrackingProtectionExceptionsFragmentDirections.actionGlobalBrowser()

    BrowserDirection.FromHttpsOnlyMode -> HttpsOnlyFragmentDirections.actionGlobalBrowser()

    BrowserDirection.FromAbout -> AboutFragmentDirections.actionGlobalBrowser()

    BrowserDirection.FromTrackingProtection -> TrackingProtectionFragmentDirections.actionGlobalBrowser()

    BrowserDirection.FromTrackingProtectionDialog ->
        TrackingProtectionPanelDialogFragmentDirections.actionGlobalBrowser()

    BrowserDirection.FromSavedLoginsFragment -> SavedLoginsAuthFragmentDirections.actionGlobalBrowser()

    BrowserDirection.FromAddNewDeviceFragment -> AddNewDeviceFragmentDirections.actionGlobalBrowser()

    BrowserDirection.FromSearchEngineFragment -> SearchEngineFragmentDirections.actionGlobalBrowser()

    BrowserDirection.FromSaveSearchEngineFragment -> SaveSearchEngineFragmentDirections.actionGlobalBrowser()

    BrowserDirection.FromAddonDetailsFragment -> AddonDetailsFragmentDirections.actionGlobalBrowser()

    BrowserDirection.FromAddonPermissionsDetailsFragment ->
        AddonPermissionsDetailsFragmentDirections.actionGlobalBrowser()

    BrowserDirection.FromLoginDetailFragment -> LoginDetailFragmentDirections.actionGlobalBrowser()

    BrowserDirection.FromTabsTray -> TabsTrayFragmentDirections.actionGlobalBrowser()

    BrowserDirection.FromRecentlyClosed -> RecentlyClosedFragmentDirections.actionGlobalBrowser()

    BrowserDirection.FromStudiesFragment -> StudiesFragmentDirections.actionGlobalBrowser()

    BrowserDirection.FromReviewQualityCheck -> ReviewQualityCheckFragmentDirections.actionGlobalBrowser()

    BrowserDirection.FromAddonsManagementFragment -> AddonsManagementFragmentDirections.actionGlobalBrowser()

    BrowserDirection.FromTranslationsDialogFragment -> TranslationsDialogFragmentDirections.actionGlobalBrowser()

    BrowserDirection.FromMenuDialogFragment -> MenuDialogFragmentDirections.actionGlobalBrowser()

    BrowserDirection.FromDownloadLanguagesPreferenceFragment ->
        DownloadLanguagesPreferenceFragmentDirections.actionGlobalBrowser()

    BrowserDirection.FromWebCompatReporterFragment ->
        WebCompatReporterFragmentDirections.actionGlobalBrowser()
}

const val REQUEST_CODE_BROWSER_ROLE = 1
const val SETTINGS_SELECT_OPTION_KEY = ":settings:fragment_args_key"
const val SETTINGS_SHOW_FRAGMENT_ARGS = ":settings:show_fragment_args"
const val DEFAULT_BROWSER_APP_OPTION = "default_browser"
const val EXTERNAL_APP_BROWSER_INTENT_SOURCE = "CUSTOM_TAB"

private fun getHomeIntentSource(intent: SafeIntent): String? {
    return when {
        intent.isLauncherIntent -> HomeActivity.APP_ICON
        intent.action == Intent.ACTION_VIEW -> "LINK"
        else -> null
    }
}

/**
 * Check if the intent is coming from within this application itself or from an external one
 * when processed through the `InternalReceiverActivity`.
 */
fun Activity.isIntentInternal(): Boolean {
    val safeIntent = SafeIntent(intent)
    return safeIntent.getStringExtra(EXTRA_ACTIVITY_REFERRER_PACKAGE) == this.packageName
}

fun Activity.getIntentSessionId(intent: SafeIntent): String? = intent.getSessionId()