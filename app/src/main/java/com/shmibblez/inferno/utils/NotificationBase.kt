/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package com.shmibblez.inferno.utils

import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.shmibblez.inferno.R

/**
 * Create a [Notification] with default behaviour and styling.
 */
fun createBaseNotification(
    context: Context,
    channelId: String,
    title: String?,
    text: String,
    onClick: PendingIntent? = null,
    onDismiss: PendingIntent? = null,
): Notification {
    return NotificationCompat.Builder(context, channelId)
        .setSmallIcon(R.drawable.ic_status_logo)
        .setContentTitle(title)
        .setContentText(text)
        .setBadgeIconType(NotificationCompat.BADGE_ICON_SMALL)
        .setColor(ContextCompat.getColor(context, R.color.primary_text_light_theme))
        .setPriority(NotificationCompat.PRIORITY_DEFAULT)
        .setShowWhen(false)
        .setContentIntent(onClick)
        .setDeleteIntent(onDismiss)
        .setAutoCancel(true)
        .build()
}
