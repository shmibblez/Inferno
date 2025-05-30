/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package com.shmibblez.inferno.tabstray.browser

import android.content.Context
import android.util.AttributeSet
import androidx.annotation.VisibleForTesting
import androidx.annotation.VisibleForTesting.Companion.PACKAGE_PRIVATE
import com.shmibblez.inferno.ext.components
import com.shmibblez.inferno.proto.InfernoSettings

class PrivateBrowserTrayList @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : AbstractBrowserTrayList(context, attrs, defStyleAttr) {

    private val privateTabsBinding by lazy {
        PrivateTabsBinding(tabsTrayStore, context.components.core.store, adapter as BrowserTabsAdapter)
    }

    private val touchHelper by lazy {
        TabsTouchHelper(
            interactionDelegate = (adapter as BrowserTabsAdapter).delegate,
            onViewHolderTouched = { swipeToDelete.isSwipeable },
            onViewHolderDraw = { (context.components.settings.tabTrayStyle == InfernoSettings.TabTrayStyle.TAB_TRAY_GRID).not() },
            featureNameHolder = (adapter as BrowserTabsAdapter),
        )
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()

        privateTabsBinding.start()
        swipeToDelete.start()

        adapter?.onAttachedToRecyclerView(this)

        touchHelper.attachToRecyclerView(this)
    }

    @VisibleForTesting(otherwise = PACKAGE_PRIVATE)
    public override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()

        privateTabsBinding.stop()
        swipeToDelete.stop()

        // Notify the adapter that it is released from the view preemptively.
        adapter?.onDetachedFromRecyclerView(this)

        touchHelper.attachToRecyclerView(null)
    }
}
