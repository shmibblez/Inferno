/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package com.shmibblez.inferno.compose.tabstray

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.shmibblez.inferno.mozillaAndroidComponents.compose.base.annotation.LightDarkPreview
import mozilla.components.feature.tab.collections.Tab
import com.shmibblez.inferno.R
import com.shmibblez.inferno.theme.FirefoxTheme

/**
 * The background of a [Tab] that is being swiped left or right.
 *
 * @param dismissDirection [DismissDirection] of the ongoing swipe. Depending on the direction,
 * the background will also include a warning icon at the start of the swipe gesture.
 * If `null` the warning icon will be shown at both ends.
 * @param shape Shape of the background.
 */
@Composable
fun DismissedTabBackground(
    dismissDirection: SwipeToDismissBoxValue?,
    shape: Shape,
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = FirefoxTheme.colors.layer3),
        modifier = Modifier.fillMaxSize(),
        shape = shape,
        elevation = CardDefaults.cardElevation(defaultElevation =  0.dp),
    ) {
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_delete_24),
                contentDescription = null,
                modifier = Modifier
                    .padding(horizontal = 32.dp)
                    // Only show the delete icon for where the swipe starts.
                    .alpha(
                        if (dismissDirection == SwipeToDismissBoxValue.StartToEnd || dismissDirection == null) 1f else 0f,
                    ),
                tint = FirefoxTheme.colors.iconCritical,
            )

            Icon(
                painter = painterResource(R.drawable.ic_delete_24),
                contentDescription = null,
                modifier = Modifier
                    .padding(horizontal = 32.dp)
                    // Only show the delete icon for where the swipe starts.
                    .alpha(
                        if (dismissDirection == SwipeToDismissBoxValue.EndToStart || dismissDirection == null) 1f else 0f,
                    ),
                tint = FirefoxTheme.colors.iconCritical,
            )
        }
    }
}

@Composable
@LightDarkPreview
private fun DismissedTabBackgroundPreview() {
    FirefoxTheme {
        Column {
            Box(modifier = Modifier.height(56.dp)) {
                DismissedTabBackground(
                    dismissDirection = SwipeToDismissBoxValue.StartToEnd,
                    shape = RoundedCornerShape(0.dp),
                )
            }

            Spacer(Modifier.height(10.dp))

            Box(modifier = Modifier.height(56.dp)) {
                DismissedTabBackground(
                    dismissDirection = SwipeToDismissBoxValue.EndToStart,
                    shape = RoundedCornerShape(bottomStart = 8.dp, bottomEnd = 8.dp),
                )
            }

            Spacer(Modifier.height(10.dp))

            Box(modifier = Modifier.height(56.dp)) {
                DismissedTabBackground(
                    dismissDirection = null,
                    shape = RoundedCornerShape(0.dp),
                )
            }
        }
    }
}
