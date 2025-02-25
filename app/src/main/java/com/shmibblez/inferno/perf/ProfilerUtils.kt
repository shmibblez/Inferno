/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package com.shmibblez.inferno.perf

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.util.Base64
import mozilla.components.concept.fetch.MutableHeaders
import mozilla.components.concept.fetch.Request
import mozilla.components.concept.fetch.Response
import org.json.JSONObject
import com.shmibblez.inferno.R
import com.shmibblez.inferno.ext.components
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

private const val PROFILER_API = "https://api.profiler.firefox.com/compressed-store"
private const val PROFILER_SERVER_HEADER = "application/vnd.firefox-profiler+json;version=1.0"
private const val TOKEN = "profileToken"
private const val PROFILER_DATA_URL = "https://profiler.firefox.com/public/"

// IMPORTANT NOTE: Please keep the profiler presets in sync with their Firefox Desktop counterparts:
// https://searchfox.org/mozilla-central/rev/c130c69b7b863d5e28ab9524b65c27c7a9507c48/devtools/client/performance-new/shared/background.jsm.js#121

private val firefox_features = arrayOf(
    "screenshots",
    "js",
    "stackwalk",
    "cpu",
    "java",
    "processcpu",
    "ipcmessages",
    "memory",
)
private val firefox_threads = arrayOf(
    "GeckoMain",
    "Compositor",
    "Renderer",
    "SwComposite",
    "DOM Worker",
)

private val graphics_features =
    arrayOf("stackwalk", "js", "cpu", "java", "processcpu", "ipcmessages", "memory")
private val graphics_threads = arrayOf(
    "GeckoMain",
    "Compositor",
    "Renderer",
    "SwComposite",
    "RenderBackend",
    "GlyphRasterizer",
    "SceneBuilder",
    "WrWorker",
    "CanvasWorkers",
    "TextureUpdate",
)

private val media_features = arrayOf(
    "js",
    "stackwalk",
    "cpu",
    "audiocallbacktracing",
    "ipcmessages",
    "processcpu",
    "java",
    "memory",
)
private val media_threads = arrayOf(
    "cubeb", "audio", "BackgroundThreadPool", "camera", "capture", "Compositor", "decoder", "GeckoMain", "gmp",
    "graph", "grph", "InotifyEventThread", "IPDL Background", "media", "ModuleProcessThread", "PacerThread",
    "RemVidChild", "RenderBackend", "Renderer", "Socket Thread", "SwComposite",
    "webrtc", "TextureUpdate",
)

private val networking_features = arrayOf(
    "screenshots",
    "js",
    "stackwalk",
    "cpu",
    "java",
    "processcpu",
    "bandwidth",
    "ipcmessages",
    "memory",
)

private val networking_threads = arrayOf(
    "Compositor", "DNS Resolver", "DOM Worker", "GeckoMain",
    "Renderer", "Socket Thread", "StreamTrans", "SwComposite", "TRR Background",
)

/**
 * Profiler settings enum for grouping features and settings together
 */
enum class ProfilerSettings(val threads: Array<String>, val features: Array<String>) {
    Firefox(firefox_threads, firefox_features),
    Graphics(graphics_threads, graphics_features),
    Media(media_threads, media_features),
    Networking(networking_threads, networking_features),
}

/**
 * Collection of functionality to parse and save the profile returned by GeckoView.
 */
object ProfilerUtils {

    private fun saveProfileUrlToClipboard(profileResult: ByteArray, context: Context): String {
        // The profile is saved to a temporary file since our fetch API takes a file or a string.
        // Converting the ByteArray to a String would hurt the encoding, which we need to preserve.
        // The file may potentially contain sensitive data and should be handled carefully.
        val outputFile = createTemporaryFile(profileResult, context)
        try {
            val response = networkCallToProfilerServer(outputFile, context)
            val profileToken = decodeProfileToken(response)
            return PROFILER_DATA_URL + profileToken
        } finally {
            outputFile.delete()
        }
    }

    private fun finishProfileSave(context: Context, url: String, onUrlFinish: (Int) -> Unit) {
        val clipboardManager = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clipData = ClipData.newPlainText("Profiler URL", url)
        clipboardManager.setPrimaryClip(clipData)
        onUrlFinish(R.string.profiler_uploaded_url_to_clipboard)
    }

    private fun createTemporaryFile(profileResult: ByteArray, context: Context): File {
        val outputDir = context.cacheDir
        val outputFile = File.createTempFile("tempProfile", ".json", outputDir)
        FileOutputStream(outputFile).use { fileOutputStream ->
            fileOutputStream.write(profileResult)
            return outputFile
        }
    }

    private fun networkCallToProfilerServer(outputFile: File, context: Context): Response {
        val request = Request(
            url = PROFILER_API,
            method = Request.Method.POST,
            headers = MutableHeaders(
                "Accept" to PROFILER_SERVER_HEADER,
            ),
            body = Request.Body.fromFile(outputFile),
            conservative = true,
        )
        return context.components.core.client.fetch(request)
    }

    private fun decodeProfileToken(response: Response): String {
        val jwtToken = StringBuilder()
        response.body.useBufferedReader {
            val jwt = it.readText()
            val jwtSplit = jwt.split(".")
            val decodedBytes = Base64.decode(jwtSplit[1], Base64.DEFAULT)
            val decodedStr = decodedBytes.decodeToString()
            val jsonObject = JSONObject(decodedStr)
            jwtToken.append(jsonObject[TOKEN])
        }
        return jwtToken.toString()
    }

    /**
     * This will either save the profile locally or send it as a URL to the Firefox profiler server
     *
     * @param context Activity context to get access to the profiler API through components.core...
     * @param profile Data returned from GeckoView as a GZIP ByteArray
     * @param onUrlFinish function passed in to display a toast with the relevant information once the profile is saved
     */
    fun handleProfileSave(
        context: Context,
        profile: ByteArray,
        onUrlFinish: (Int) -> Unit,
    ) {
        try {
            val url = saveProfileUrlToClipboard(profile, context)
            finishProfileSave(context, url, onUrlFinish)
        } catch (e: IOException) {
            onUrlFinish(R.string.profiler_io_error)
        }
    }
}
