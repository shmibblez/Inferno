/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package com.shmibblez.inferno.components.settings

import androidx.core.content.edit
import mozilla.components.support.ktx.android.content.PreferencesHolder

class CounterPreference(
    private val holder: PreferencesHolder,
    private val key: String,
    private val maxCount: Int,
) {

    var value
        get() = holder.preferences.getInt(key, 0)
        set(value) = holder.preferences.edit {
            putInt(key, value)
        }

    fun underMaxCount() = value < maxCount

    fun increment() {
        holder.preferences.edit {
            putInt(key, value + 1)
        }
    }
}

/**
 * Property delegate for getting and an int shared preference and incrementing it.
 */
fun PreferencesHolder.counterPreference(key: String, maxCount: Int = -1) =
    CounterPreference(this, key, maxCount)
