/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package com.shmibblez.inferno.compose

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.selection.toggleable
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.dp
import com.shmibblez.inferno.compose.base.InfernoSwitch
import com.shmibblez.inferno.compose.base.InfernoText
import com.shmibblez.inferno.compose.base.InfernoTextStyle
import com.shmibblez.inferno.ext.infernoTheme

private const val DISABLED_ALPHA = 0.5f

/**
 * UI for a switch with label that can be on or off.
 *
 * @param label Text to be displayed next to the switch.
 * @param checked Whether or not the switch is checked.
 * @param modifier Modifier to be applied to the switch layout.
 * @param description An optional description text below the label.
 * @param enabled Whether the switch is enabled or grayed out.
 * @param onCheckedChange Invoked when Switch is being clicked, therefore the change of checked
 * state is requested.
 */
@Composable
fun SwitchWithLabel(
    label: String,
    checked: Boolean,
    modifier: Modifier = Modifier,
    description: String? = null,
    enabled: Boolean = true,
    onCheckedChange: ((Boolean) -> Unit),
) {
    Row(
        modifier = Modifier
            .toggleable(
                value = checked,
                enabled = enabled,
                role = Role.Switch,
                onValueChange = onCheckedChange,
            ).then(
                modifier,
            ),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            modifier = Modifier
                .weight(1f),
        ) {
            InfernoText(
                text = label,
                modifier = Modifier
                    .defaultMinSize(minHeight = 24.dp)
                    .wrapContentHeight(),
                fontColor = if (enabled) {
                    LocalContext.current.infernoTheme().value.primaryTextColor
                } else {
                    LocalContext.current.infernoTheme().value.secondaryTextColor
                },
//                style = FirefoxTheme.typography.subtitle1,
                infernoStyle = InfernoTextStyle.SmallSecondary,
            )

            description?.let {
                InfernoText(
                    text = description,
                    modifier = Modifier
                        .defaultMinSize(minHeight = 20.dp)
                        .wrapContentHeight(),
                    fontColor = if (enabled) {
                        LocalContext.current.infernoTheme().value.primaryTextColor
                    } else {
                        LocalContext.current.infernoTheme().value.secondaryTextColor
                    },
                    infernoStyle = InfernoTextStyle.Normal,
//                    style = FirefoxTheme.typography.body2,
                )
            }
        }

        InfernoSwitch(
            modifier = Modifier.clearAndSetSemantics {},
            checked = checked,
            onCheckedChange = onCheckedChange,
            enabled = enabled,
        )
    }
}

///**
// * UI for a switch that can be on or off.
// *
// * @param checked Whether or not the switch is checked.
// * @param onCheckedChange Invoked when Switch is being clicked, therefore the change of checked
// * state is requested.
// * @param modifier Modifier to be applied to the switch layout.
// * @param enabled Whether the switch is enabled or grayed out.
// */
//@Composable
//private fun Switch(
//    checked: Boolean,
//    onCheckedChange: ((Boolean) -> Unit),
//    modifier: Modifier = Modifier,
//    enabled: Boolean = true,
//) {
//    MaterialSwitch(
//        checked = checked,
//        onCheckedChange = onCheckedChange,
//        modifier = modifier,
//        enabled = enabled,
//        colors = SwitchDefaults.colors(
//            uncheckedThumbColor = FirefoxTheme.colors.formOff,
//            uncheckedTrackColor = FirefoxTheme.colors.formSurface,
//            checkedThumbColor = FirefoxTheme.colors.formOn,
//            checkedTrackColor = FirefoxTheme.colors.formSurface,
//            disabledUncheckedThumbColor = FirefoxTheme.colors.formOff
//                .copy(alpha = DISABLED_ALPHA)
//                .compositeOver(FirefoxTheme.colors.formSurface),
//            disabledUncheckedTrackColor = FirefoxTheme.colors.formSurface.copy(alpha = DISABLED_ALPHA),
//            disabledCheckedThumbColor = FirefoxTheme.colors.formOn
//                .copy(alpha = DISABLED_ALPHA)
//                .compositeOver(FirefoxTheme.colors.formSurface),
//            disabledCheckedTrackColor = FirefoxTheme.colors.formSurface.copy(alpha = DISABLED_ALPHA),
//        ),
//    )
//}

//@LightDarkPreview
//@Composable
//private fun SwitchWithLabelPreview() {
//    FirefoxTheme {
//        Column(
//            modifier = Modifier
//                .background(FirefoxTheme.colors.layer1)
//                .padding(16.dp),
//            verticalArrangement = Arrangement.spacedBy(8.dp),
//        ) {
//            Text(
//                text = "Enabled",
//                style = FirefoxTheme.typography.headline7,
//                color = FirefoxTheme.colors.textPrimary,
//            )
//
//            Spacer(Modifier.height(8.dp))
//
//            var enabledSwitchState by remember { mutableStateOf(false) }
//            SwitchWithLabel(
//                label = if (enabledSwitchState) "On" else "Off",
//                checked = enabledSwitchState,
//                description = "Description text",
//            ) { enabledSwitchState = it }
//
//            Text(
//                text = "Disabled",
//                style = FirefoxTheme.typography.headline7,
//                color = FirefoxTheme.colors.textPrimary,
//            )
//
//            Spacer(Modifier.height(8.dp))
//
//            var disabledSwitchStateOff by remember { mutableStateOf(false) }
//            SwitchWithLabel(
//                label = "Off",
//                checked = disabledSwitchStateOff,
//                enabled = false,
//            ) { disabledSwitchStateOff = it }
//
//            var disabledSwitchStateOn by remember { mutableStateOf(true) }
//            SwitchWithLabel(
//                label = "On",
//                checked = disabledSwitchStateOn,
//                enabled = false,
//            ) { disabledSwitchStateOn = it }
//
//            Text(
//                text = "Nested",
//                style = FirefoxTheme.typography.headline7,
//                color = FirefoxTheme.colors.textPrimary,
//            )
//
//            Spacer(Modifier.height(8.dp))
//
//            Row {
//                Spacer(Modifier.weight(1f))
//
//                var nestedSwitchState by remember { mutableStateOf(false) }
//                SwitchWithLabel(
//                    label = "Nested",
//                    checked = nestedSwitchState,
//                    modifier = Modifier.weight(1f),
//                ) { nestedSwitchState = it }
//            }
//        }
//    }
//}