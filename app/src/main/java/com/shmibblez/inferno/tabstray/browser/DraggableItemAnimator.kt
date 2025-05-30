/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package com.shmibblez.inferno.tabstray.browser

import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.RecyclerView

class DraggableItemAnimator : DefaultItemAnimator() {
    override fun animatePersistence(
        viewHolder: RecyclerView.ViewHolder,
        preLayoutInfo: RecyclerView.ItemAnimator.ItemHolderInfo,
        postLayoutInfo: RecyclerView.ItemAnimator.ItemHolderInfo,
    ): Boolean {
        // While being dragged, keep the tab visually in place
        if (viewHolder is AbstractBrowserTabViewHolder && viewHolder.beingDragged) {
            viewHolder.itemView.translationX -= postLayoutInfo.left - preLayoutInfo.left
            viewHolder.itemView.translationY -= postLayoutInfo.top - preLayoutInfo.top
            dispatchAnimationFinished(viewHolder)
            return false
        }
        return super.animatePersistence(viewHolder, preLayoutInfo, postLayoutInfo)
    }
}
