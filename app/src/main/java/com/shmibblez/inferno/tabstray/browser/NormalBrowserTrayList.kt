/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package com.shmibblez.inferno.tabstray.browser

import android.content.Context
import android.util.AttributeSet
import androidx.recyclerview.widget.ConcatAdapter
import mozilla.components.browser.tabstray.TabViewHolder
import com.shmibblez.inferno.ext.components
import com.shmibblez.inferno.proto.InfernoSettings
import com.shmibblez.inferno.tabstray.ext.browserAdapter

class NormalBrowserTrayList @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : AbstractBrowserTrayList(context, attrs, defStyleAttr) {

    private val concatAdapter by lazy { adapter as ConcatAdapter }

    private val normalTabsBinding by lazy {
        NormalTabsBinding(tabsTrayStore, context.components.core.store, concatAdapter.browserAdapter)
    }

    private val touchHelper by lazy {
        TabsTouchHelper(
            interactionDelegate = concatAdapter.browserAdapter.interactor,
            onViewHolderTouched = {
                it is TabViewHolder && swipeToDelete.isSwipeable
            },
            onViewHolderDraw = { (context.components.settings.tabTrayStyle == InfernoSettings.TabTrayStyle.TAB_TRAY_GRID).not() },
            featureNameHolder = concatAdapter.browserAdapter,
        )
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()

        normalTabsBinding.start()

        touchHelper.attachToRecyclerView(this)
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()

        normalTabsBinding.stop()

        touchHelper.attachToRecyclerView(null)
    }
}
