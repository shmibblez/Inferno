/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package com.shmibblez.inferno.perf

import android.app.Activity
import android.os.Bundle
import mozilla.components.concept.engine.Engine
import com.shmibblez.inferno.HomeActivity
import com.shmibblez.inferno.IntentReceiverActivity
import com.shmibblez.inferno.android.DefaultActivityLifecycleCallbacks

/**
 * Adds a profiler marker for each activity lifecycle callbacks. The callbacks are called by the
 * super method (e.g. [Activity.onCreate] so the markers occur sometime during the execution of
 * our implementation (e.g. [com.shmibblez.inferno.HomeActivity.onCreate]) rather than at the beginning
 * or end of that method.
 */
class MarkersActivityLifecycleCallbacks(
    private val engine: Engine,
) : DefaultActivityLifecycleCallbacks {

    private fun shouldSkip(): Boolean {
        return engine.profiler?.isProfilerActive() != true
    }

    override fun onActivityCreated(activity: Activity, bundle: Bundle?) {
        if (shouldSkip() ||
            // These methods are manually instrumented with duration.
            activity is HomeActivity ||
            activity is IntentReceiverActivity
        ) {
            return
        }
        engine.profiler?.addMarker(MARKER_NAME, "${activity::class.simpleName}.onCreate (via callbacks)")
    }

    override fun onActivityStarted(activity: Activity) {
        if (shouldSkip() ||
            // These methods are manually instrumented with duration.
            activity is HomeActivity
        ) {
            return
        }
        engine.profiler?.addMarker(MARKER_NAME, "${activity::class.simpleName}.onStart (via callbacks)")
    }

    override fun onActivityResumed(activity: Activity) {
        if (shouldSkip()) { return }
        engine.profiler?.addMarker(MARKER_NAME, "${activity::class.simpleName}.onResume (via callbacks)")
    }

    override fun onActivityPaused(activity: Activity) {
        if (shouldSkip()) { return }
        engine.profiler?.addMarker(MARKER_NAME, "${activity::class.simpleName}.onPause (via callbacks)")
    }

    override fun onActivityStopped(activity: Activity) {
        if (shouldSkip()) { return }
        engine.profiler?.addMarker(MARKER_NAME, "${activity::class.simpleName}.onStop (via callbacks)")
    }

    override fun onActivityDestroyed(activity: Activity) {
        if (shouldSkip()) { return }
        engine.profiler?.addMarker(MARKER_NAME, "${activity::class.simpleName}.onDestroy (via callbacks)")
    }

    companion object {
        const val MARKER_NAME = "Activity Lifecycle"
    }
}
