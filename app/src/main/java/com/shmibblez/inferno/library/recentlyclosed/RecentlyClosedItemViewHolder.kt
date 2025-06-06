/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package com.shmibblez.inferno.library.recentlyclosed

import android.view.View
import androidx.recyclerview.widget.RecyclerView
import mozilla.components.browser.state.state.recover.TabState
import com.shmibblez.inferno.R
import com.shmibblez.inferno.databinding.HistoryListItemBinding
import com.shmibblez.inferno.ext.hideAndDisable
import com.shmibblez.inferno.ext.showAndEnable
import com.shmibblez.inferno.selection.SelectionHolder

class RecentlyClosedItemViewHolder(
    view: View,
    private val recentlyClosedFragmentInteractor: RecentlyClosedFragmentInteractor,
    private val selectionHolder: SelectionHolder<TabState>,
) : RecyclerView.ViewHolder(view) {

    private val binding = HistoryListItemBinding.bind(view)

    private var item: TabState? = null

    init {
        binding.historyLayout.overflowView.apply {
            setImageResource(R.drawable.ic_close_24)
            contentDescription = view.context.getString(R.string.history_delete_item)
            setOnClickListener {
                val item = item ?: return@setOnClickListener
                recentlyClosedFragmentInteractor.onDelete(item)
            }
        }
    }

    fun bind(item: TabState) {
        binding.historyLayout.titleView.text =
            item.title.ifEmpty { item.url }
        binding.historyLayout.urlView.text = item.url

        binding.historyLayout.setSelectionInteractor(
            item,
            selectionHolder,
            recentlyClosedFragmentInteractor,
        )
        binding.historyLayout.changeSelected(item in selectionHolder.selectedItems)

        if (this.item?.url != item.url) {
            binding.historyLayout.loadFavicon(item.url)
        }

        if (selectionHolder.selectedItems.isEmpty()) {
            binding.historyLayout.overflowView.showAndEnable()
        } else {
            binding.historyLayout.overflowView.hideAndDisable()
        }

        this.item = item
    }

    companion object {
        val LAYOUT_ID
 = R.layout.history_list_item
    }
}
