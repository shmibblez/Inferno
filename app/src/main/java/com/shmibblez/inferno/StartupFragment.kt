/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package com.shmibblez.inferno

import androidx.fragment.app.Fragment
import com.shmibblez.inferno.home.HomeFragment

/**
 * This empty fragment serves as a start destination in our navigation
 * graph. It contains no layout and is fast to create compared to our
 * [HomeFragment], which would otherwise be the start destination.
 *
 * When our [HomeActivity] is created we make a decision which fragment
 * to navigate to, which makes sure we only render the [HomeFragment]
 * as needed.
 */
class StartupFragment : Fragment()
