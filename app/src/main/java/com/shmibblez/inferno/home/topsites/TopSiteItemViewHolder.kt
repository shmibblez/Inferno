/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package com.shmibblez.inferno.home.topsites

import android.annotation.SuppressLint
import android.content.res.ColorStateList
import android.view.MotionEvent
import android.view.View
import android.widget.PopupWindow
import androidx.annotation.VisibleForTesting
import androidx.appcompat.content.res.AppCompatResources.getDrawable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.core.widget.TextViewCompat
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import mozilla.components.feature.top.sites.TopSite
import mozilla.components.lib.state.ext.flowScoped
import mozilla.components.support.ktx.android.content.getColorFromAttr
//import com.shmibblez.inferno.GleanMetrics.Pings
//import com.shmibblez.inferno.GleanMetrics.TopSites
import com.shmibblez.inferno.R
import com.shmibblez.inferno.components.AppStore
import com.shmibblez.inferno.databinding.TopSiteItemBinding
import com.shmibblez.inferno.ext.bitmapForUrl
import com.shmibblez.inferno.ext.components
import com.shmibblez.inferno.ext.isSystemInDarkTheme
import com.shmibblez.inferno.ext.loadIntoView
import com.shmibblez.inferno.home.sessioncontrol.TopSiteInteractor
import com.shmibblez.inferno.settings.SupportUtils
import com.shmibblez.inferno.utils.view.ViewHolder

@SuppressLint("ClickableViewAccessibility")
class TopSiteItemViewHolder(
    view: View,
    appStore: AppStore,
    private val viewLifecycleOwner: LifecycleOwner,
    private val interactor: TopSiteInteractor,
) : ViewHolder(view) {
    private lateinit var topSite: TopSite
    private val binding = TopSiteItemBinding.bind(view)

    init {
        itemView.setOnLongClickListener {
            interactor.onTopSiteLongClicked(topSite)

            val topSiteMenu = TopSiteItemMenu(
                context = view.context,
                topSite = topSite,
            ) { item ->
                when (item) {
                    is TopSiteItemMenu.Item.OpenInPrivateTab -> interactor.onOpenInPrivateTabClicked(
                        topSite,
                    )
                    is TopSiteItemMenu.Item.EditTopSite -> interactor.onEditTopSiteClicked(
                        topSite,
                    )
                    is TopSiteItemMenu.Item.RemoveTopSite -> {
                        interactor.onRemoveTopSiteClicked(topSite)
                    }
                    is TopSiteItemMenu.Item.Settings -> interactor.onSettingsClicked()
                    is TopSiteItemMenu.Item.SponsorPrivacy -> interactor.onSponsorPrivacyClicked()
                }
            }
            val menu = topSiteMenu.menuBuilder.build(view.context).show(anchor = it)

            it.setOnTouchListener { v, event ->
                onTouchEvent(v, event, menu)
            }

            true
        }

        appStore.flowScoped(viewLifecycleOwner) { flow ->
            flow.map { state -> state.wallpaperState }
                .distinctUntilChanged()
                .collect { currentState ->
                    var backgroundColor = ContextCompat.getColor(view.context, R.color.fx_mobile_layer_color_2)

                    currentState.runIfWallpaperCardColorsAreAvailable { cardColorLight, cardColorDark ->
                        backgroundColor = if (view.context.isSystemInDarkTheme()) {
                            cardColorDark
                        } else {
                            cardColorLight
                        }
                    }

                    binding.faviconCard.setCardBackgroundColor(backgroundColor)

                    val textColor = currentState.currentWallpaper.textColor
                    if (textColor != null) {
                        val color = Color(textColor).toArgb()
                        val colorList = ColorStateList.valueOf(color)
                        binding.topSiteTitle.setTextColor(color)
                        binding.topSiteSubtitle.setTextColor(color)
                        TextViewCompat.setCompoundDrawableTintList(binding.topSiteTitle, colorList)
                    } else {
                        binding.topSiteTitle.setTextColor(
                            view.context.getColorFromAttr(R.attr.textPrimary),
                        )
                        binding.topSiteSubtitle.setTextColor(
                            view.context.getColorFromAttr(R.attr.textSecondary),
                        )
                        TextViewCompat.setCompoundDrawableTintList(binding.topSiteTitle, null)
                    }
                }
        }
    }

    fun bind(topSite: TopSite, position: Int) {
        itemView.setOnClickListener {
            interactor.onSelectTopSite(topSite, position)
        }

        binding.topSiteTitle.text = topSite.title
        binding.topSiteSubtitle.isVisible = topSite is TopSite.Provided

        if (topSite is TopSite.Pinned || topSite is TopSite.Default) {
            val pinIndicator = getDrawable(itemView.context, R.drawable.ic_new_pin)
            binding.topSiteTitle.setCompoundDrawablesWithIntrinsicBounds(pinIndicator, null, null, null)
        } else {
            binding.topSiteTitle.setCompoundDrawablesWithIntrinsicBounds(null, null, null, null)
        }

        if (topSite is TopSite.Provided) {
            viewLifecycleOwner.lifecycleScope.launch(IO) {
                itemView.context.components.core.client.bitmapForUrl(topSite.imageUrl)?.let { bitmap ->
                    withContext(Main) {
                        binding.faviconImage.setImageBitmap(bitmap)
                        submitTopSitesImpressionPing(topSite, position)
                    }
                }
            }
        } else {
            when (topSite.url) {
                SupportUtils.POCKET_TRENDING_URL -> {
                    binding.faviconImage.setImageDrawable(getDrawable(itemView.context, R.drawable.ic_pocket))
                }
                SupportUtils.BAIDU_URL -> {
                    binding.faviconImage.setImageDrawable(getDrawable(itemView.context, R.drawable.ic_baidu))
                }
                SupportUtils.JD_URL -> {
                    binding.faviconImage.setImageDrawable(getDrawable(itemView.context, R.drawable.ic_jd))
                }
                SupportUtils.PDD_URL -> {
                    binding.faviconImage.setImageDrawable(getDrawable(itemView.context, R.drawable.ic_pdd))
                }
                SupportUtils.TC_URL -> {
                    binding.faviconImage.setImageDrawable(getDrawable(itemView.context, R.drawable.ic_tc))
                }
                SupportUtils.MEITUAN_URL -> {
                    binding.faviconImage.setImageDrawable(getDrawable(itemView.context, R.drawable.ic_meituan))
                }
                else -> {
                    itemView.context.components.core.icons.loadIntoView(binding.faviconImage, topSite.url)
                }
            }
        }

        this.topSite = topSite
    }

    @VisibleForTesting
    internal fun submitTopSitesImpressionPing(topSite: TopSite.Provided, position: Int) {
//        TopSites.contileImpression.record(
//            TopSites.ContileImpressionExtra(
//                position = position + 1,
//                source = "newtab",
//            ),
//        )

//        topSite.id?.let { TopSites.contileTileId.set(it) }
//        topSite.title?.let { TopSites.contileAdvertiser.set(it.lowercase()) }
//        TopSites.contileReportingUrl.set(topSite.impressionUrl)
//        Pings.topsitesImpression.submit()
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun onTouchEvent(
        v: View,
        event: MotionEvent,
        menu: PopupWindow,
    ): Boolean {
        if (event.action == MotionEvent.ACTION_CANCEL) {
            menu.dismiss()
        }
        return v.onTouchEvent(event)
    }

    companion object {
        val LAYOUT_ID
 = R.layout.top_site_item
    }
}
