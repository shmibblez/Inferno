/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package com.shmibblez.inferno.debugsettings.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.shmibblez.inferno.mozillaAndroidComponents.compose.base.annotation.LightDarkPreview
import com.shmibblez.inferno.R
import com.shmibblez.inferno.debugsettings.navigation.DebugDrawerDestination
import com.shmibblez.inferno.theme.FirefoxTheme

/**
 * The debug drawer UI.
 *
 * @param navController [NavHostController] used to perform navigation actions on the [NavHost].
 * @param destinations The list of [DebugDrawerDestination]s (excluding home) used to populate
 * the [NavHost] with screens.
 * @param onBackButtonClick Invoked when the user taps on the back button in the app bar.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DebugDrawer(
    navController: NavHostController,
    destinations: List<DebugDrawerDestination>,
    onBackButtonClick: () -> Unit,
) {
    var backButtonVisible by remember { mutableStateOf(false) }
    var toolbarTitle by remember { mutableStateOf("") }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = toolbarTitle,
                        color = FirefoxTheme.colors.textPrimary,
                        style = FirefoxTheme.typography.headline6,
                    )
                },
                navigationIcon = {
                    if (backButtonVisible) {
                        TopBarBackButton(onClick = onBackButtonClick)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = FirefoxTheme.colors.layer1),
                expandedHeight = 5.dp,
            )
        },
        containerColor = FirefoxTheme.colors.layer1,
    ) { paddingValues ->
        NavHost(
            navController = navController,
            startDestination = DEBUG_DRAWER_HOME_ROUTE,
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
        ) {
            composable(route = DEBUG_DRAWER_HOME_ROUTE) {
                toolbarTitle = stringResource(id = R.string.debug_drawer_title)
                backButtonVisible = false
                DebugDrawerHome(destinations = destinations)
            }

            destinations.forEach { destination ->
                composable(route = destination.route) {
                    toolbarTitle = stringResource(id = destination.title)
                    backButtonVisible = true

                    destination.content()
                }
            }
        }
    }
}

@Composable
private fun TopBarBackButton(onClick: () -> Unit) {
    IconButton(
        onClick = onClick,
    ) {
        Icon(
            painter = painterResource(R.drawable.mozac_ic_back_24),
            contentDescription = stringResource(R.string.debug_drawer_back_button_content_description),
            tint = FirefoxTheme.colors.iconPrimary,
        )
    }
}

@Composable
@LightDarkPreview
private fun DebugDrawerPreview() {
    val navController = rememberNavController()
    val destinations = remember {
        List(size = 15) { index ->
            DebugDrawerDestination(
                route = "screen_$index",
                title = R.string.debug_drawer_title,
                onClick = {
                    navController.navigate(route = "screen_$index")
                },
                content = {
                    Text(
                        text = "Tool $index",
                        color = FirefoxTheme.colors.textPrimary,
                        style = FirefoxTheme.typography.headline6,
                    )
                },
            )
        }
    }

    FirefoxTheme {
        Box(modifier = Modifier.background(color = FirefoxTheme.colors.layer1)) {
            DebugDrawer(
                navController = navController,
                destinations = destinations,
                onBackButtonClick = {
                    navController.popBackStack()
                },
            )
        }
    }
}
