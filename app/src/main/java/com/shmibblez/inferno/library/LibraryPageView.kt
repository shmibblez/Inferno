/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package com.shmibblez.inferno.library

import android.content.Context
import android.view.ViewGroup
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import mozilla.components.support.ktx.android.content.getColorFromAttr
import com.shmibblez.inferno.R
import com.shmibblez.inferno.ext.asActivity
import com.shmibblez.inferno.ext.setToolbarColors

open class LibraryPageView(
    val containerView: ViewGroup,
) {
    protected val context: Context inline get() = containerView.context
    protected val activity = context.asActivity()

    protected fun setUiForNormalMode(
        title: String?,
    ) {
        updateToolbar(
            title = title,
            foregroundColor = context.getColorFromAttr(R.attr.textPrimary),
            backgroundColor = context.getColorFromAttr(R.attr.layer1),
        )
    }

    protected fun setUiForSelectingMode(
        title: String?,
    ) {
        updateToolbar(
            title = title,
            foregroundColor = ContextCompat.getColor(
                context,
                R.color.fx_mobile_text_color_oncolor_primary,
            ),
            backgroundColor = context.getColorFromAttr(R.attr.accent),
        )
    }

    private fun updateToolbar(title: String?, foregroundColor: Int, backgroundColor: Int) {
        activity?.title = title
        val toolbar = activity?.findViewById<Toolbar>(R.id.navigationToolbar)
        toolbar?.setToolbarColors(foregroundColor, backgroundColor)
        toolbar?.setNavigationIcon(R.drawable.ic_back_button_24)
        toolbar?.navigationIcon?.setTint(foregroundColor)
    }
}
