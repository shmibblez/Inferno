package com.shmibblez.inferno.browser.readermode


import android.content.Context
import android.util.Log
import androidx.annotation.VisibleForTesting
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.shmibblez.inferno.browser.infernoFeatureState.InfernoFeatureState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.mapNotNull
import mozilla.components.browser.state.action.EngineAction
import mozilla.components.browser.state.action.ReaderAction
import mozilla.components.browser.state.selector.selectedTab
import mozilla.components.browser.state.state.TabSessionState
import mozilla.components.browser.state.store.BrowserStore
import mozilla.components.concept.engine.Engine
import mozilla.components.concept.engine.webextension.MessageHandler
import mozilla.components.concept.engine.webextension.Port
import mozilla.components.feature.readerview.UUIDCreator
import mozilla.components.feature.readerview.onReaderViewStatusChange
import mozilla.components.lib.state.ext.flowScoped
import mozilla.components.support.base.log.logger.Logger
import mozilla.components.support.ktx.kotlinx.coroutines.flow.filterChanged
import mozilla.components.support.webextensions.WebExtensionController
import org.json.JSONObject
import java.lang.ref.WeakReference
import java.net.URLEncoder
import java.util.Locale
import java.util.UUID

@Composable
fun rememberInfernoReaderViewFeatureState(
    context: Context,
    engine: Engine,
    store: BrowserStore,
    createUUID: UUIDCreator = { UUID.randomUUID().toString() },
    onReaderViewStatusChange: onReaderViewStatusChange = { _, _ -> },
): InfernoReaderViewFeatureState {
    val state = InfernoReaderViewFeatureState(
        context = context,
        engine = engine,
        store = store,
        createUUID = createUUID,
        onReaderViewStatusChange = onReaderViewStatusChange
    )

    DisposableEffect(null) {
        state.start()

        onDispose {
            state.stop()
        }
    }
//    return rememberSaveable(saver = InfernoReaderViewFeatureState.Saver) {
//        InfernoReaderViewFeatureState()
//    }

    return remember { state }
}

class InfernoReaderViewFeatureState internal constructor(
    context: Context,
    internal val engine: Engine,
    internal val store: BrowserStore,
    internal val createUUID: UUIDCreator = { UUID.randomUUID().toString() },
    internal val onReaderViewStatusChange: onReaderViewStatusChange = { _, _ ->  },
//    private val prefetchStrategy: InfernoReaderViewFeaturePrefetchStrategy = InfernoReaderViewFeaturePrefetchStrategy(),
    ): InfernoFeatureState {

    private var scope: CoroutineScope? = null

    @VisibleForTesting
    var readerBaseUrl: String? = null

    @VisibleForTesting
    // This is an internal var to make it mutable for unit testing purposes only
    internal var extensionController = WebExtensionController(
        READER_VIEW_EXTENSION_ID,
        READER_VIEW_EXTENSION_URL,
        READER_VIEW_CONTENT_PORT,
    )

    internal val config = InfernoReaderViewConfig(context) { message ->
        val engineSession = store.state.selectedTab?.engineState?.engineSession
        extensionController.sendContentMessage(message, engineSession, READER_VIEW_ACTIVE_CONTENT_PORT)
    }

    var active by mutableStateOf(false)
        private set

    private var readerable by mutableStateOf(false)
//        private set

    enum class FontType(val value: String) { SANSSERIF("sans-serif"), SERIF("serif") }
    enum class ColorScheme { LIGHT, SEPIA, DARK }

    override fun start() {
        ensureExtensionInstalled()

        scope = store.flowScoped { flow ->
            flow.mapNotNull { state -> state.tabs }
                .filterChanged {
                    it.readerState
                }
                .collect { tab ->
                    active = tab.readerState.active
                    readerable = tab.readerState.readerable
                    if (tab.readerState.connectRequired) {
                        connectReaderViewContentScript(tab)
                    }
                    if (tab.readerState.checkRequired) {
                        checkReaderState(tab)
                    }
                    if (tab.id == store.state.selectedTabId) {
                        maybeNotifyReaderStatusChange(tab.readerState.readerable, tab.readerState.active)
                    }
                }
        }
    }

    override fun stop() {
        scope?.cancel()
    }

    /**
     * Shows the reader view UI.
     * @return whether operation success
     */
    fun showReaderView(session: TabSessionState? = store.state.selectedTab): Boolean {
        session?.let {
            if (!it.readerState.active) {
                val id = createUUID()
                extensionController.sendContentMessage(
                    createCachePageMessage(id),
                    it.engineState.engineSession,
                    READER_VIEW_CONTENT_PORT,
                )

                val readerUrl = extensionController.createReaderUrl(it.content.url, id) ?: run {
                Log.e("InfernoReaderViewState", "unable to create ReaderUrl")
                Logger.error("FeatureReaderView unable to create ReaderUrl.")
                    return@let
                }

                store.dispatch(EngineAction.LoadUrlAction(it.id, readerUrl))
                store.dispatch(ReaderAction.UpdateReaderActiveAction(it.id, true))
                return true
            }
        }
        return false
    }

    /**
     * Hides the reader view UI.
     */
    fun hideReaderView(session: TabSessionState? = store.state.selectedTab) {
        session?.let { it ->
            if (it.readerState.active) {
                store.dispatch(ReaderAction.UpdateReaderActiveAction(it.id, false))
                store.dispatch(ReaderAction.UpdateReaderableAction(it.id, false))
                store.dispatch(ReaderAction.ClearReaderActiveUrlAction(it.id))
                if (it.content.canGoBack) {
                    it.engineState.engineSession?.goBack(false)
                } else {
                    extensionController.sendContentMessage(
                        createHideReaderMessage(),
                        it.engineState.engineSession,
                        READER_VIEW_ACTIVE_CONTENT_PORT,
                    )
                }
            }
        }
    }

    @VisibleForTesting
    internal fun checkReaderState(session: TabSessionState? = store.state.selectedTab) {
        session?.engineState?.engineSession?.let { engineSession ->
            val message = createCheckReaderStateMessage()
            if (extensionController.portConnected(engineSession, READER_VIEW_CONTENT_PORT)) {
                extensionController.sendContentMessage(message, engineSession, READER_VIEW_CONTENT_PORT)
            }
            if (extensionController.portConnected(engineSession, READER_VIEW_ACTIVE_CONTENT_PORT)) {
                extensionController.sendContentMessage(message, engineSession, READER_VIEW_ACTIVE_CONTENT_PORT)
            }
            store.dispatch(ReaderAction.UpdateReaderableCheckRequiredAction(session.id, false))
        }
    }

    @VisibleForTesting
    internal fun connectReaderViewContentScript(session: TabSessionState? = store.state.selectedTab) {
        session?.engineState?.engineSession?.let { engineSession ->
            extensionController.registerContentMessageHandler(
                engineSession,
                ActiveReaderViewContentMessageHandler(store, session.id, WeakReference(config)),
                READER_VIEW_ACTIVE_CONTENT_PORT,
            )
            extensionController.registerContentMessageHandler(
                engineSession,
                ReaderViewContentMessageHandler(store, session.id),
                READER_VIEW_CONTENT_PORT,
            )
            store.dispatch(ReaderAction.UpdateReaderConnectRequiredAction(session.id, false))
        }
    }

    private var lastNotified: Pair<Boolean, Boolean>? = null

    @VisibleForTesting
    internal fun maybeNotifyReaderStatusChange(readerable: Boolean = false, active: Boolean = false) {
        // Make sure we only notify the UI if needed (an actual change happened) to prevent
        // it from unnecessarily invalidating toolbar/menu items.
        if (lastNotified == null || lastNotified != Pair(readerable, active)) {
            onReaderViewStatusChange(readerable, active)
            lastNotified = Pair(readerable, active)
        }
    }

    private fun ensureExtensionInstalled() {
        extensionController.install(
            engine,
            onSuccess = {
                it.getMetadata()?.run {
                    readerBaseUrl = baseUrl
                } ?: run {
                    Log.e("InfernoReaderViewState", "ReaderView extension missing Metadata")
                    Logger.error("ReaderView extension missing Metadata")
                }

                connectReaderViewContentScript()
            },
        )
    }

    /**
     * Handles content messages from regular pages.
     */
    private open class ReaderViewContentMessageHandler(
        protected val store: BrowserStore,
        protected val sessionId: String,
    ) : MessageHandler {
        override fun onPortConnected(port: Port) {
            port.postMessage(createCheckReaderStateMessage())
        }

        override fun onPortMessage(message: Any, port: Port) {
            if (message is JSONObject) {
                val readerable = message.optBoolean(READERABLE_RESPONSE_MESSAGE_KEY, false)
                store.dispatch(ReaderAction.UpdateReaderableAction(sessionId, readerable))
            }
        }
    }

    /**
     * Handles content messages from active reader pages.
     */
    private class ActiveReaderViewContentMessageHandler(
        store: BrowserStore,
        sessionId: String,
        // This needs to be a weak reference because the engine session this message handler will be
        // attached to has a longer lifespan than the feature instance i.e. a tab can remain open,
        // but we don't want to prevent the feature (and therefore its context/fragment) from
        // being garbage collected. The config has references to both the context and feature.
        private val config: WeakReference<InfernoReaderViewConfig>,
    ) : ReaderViewContentMessageHandler(store, sessionId) {

        override fun onPortMessage(message: Any, port: Port) {
            super.onPortMessage(message, port)

            if (message is JSONObject) {
                val baseUrl = message.getString(BASE_URL_RESPONSE_MESSAGE_KEY)
                store.dispatch(ReaderAction.UpdateReaderBaseUrlAction(sessionId, baseUrl))

                port.postMessage(createShowReaderMessage(config.get(), store.state.selectedTab?.readerState?.scrollY))

                val activeUrl = message.getString(ACTIVE_URL_RESPONSE_MESSAGE_KEY)
                store.dispatch(ReaderAction.UpdateReaderActiveUrlAction(sessionId, activeUrl))
            }
        }
    }

    private fun WebExtensionController.createReaderUrl(url: String, id: String): String? {
        val colorScheme = config.colorScheme.name.lowercase(Locale.ROOT)
        // Encode the original page url, otherwise when the readerview page will try to
        // parse the url and retrieve the readerview url params (ir and colorScheme)
        // the parser may get confused because the original webpage url being interpolated
        // may also include its own search params non-escaped (See Bug 1860490).
        val encodedUrl = URLEncoder.encode(url, "UTF-8")
        return readerBaseUrl?.let { it + "readerview.html?url=$encodedUrl&id=$id&colorScheme=$colorScheme" }
    }

    companion object {
//        val Saver: Saver<InfernoReaderViewFeatureState, *> = infernoReaderViewSaver(
//            save = {listOf(0)},
//            restore ={ InfernoReaderViewFeatureState()},
//        )
//        internal fun saver(): Saver<InfernoReaderViewFeatureState, *> = infernoReaderViewSaver(
//            save = {listOf(0)},
//            restore ={ InfernoReaderViewFeatureState()},
//        )

        private val logger = Logger("ReaderView")

        internal const val READER_VIEW_EXTENSION_ID = "readerview@mozac.org"

        // Name of the port connected to all pages for checking whether or not
        // a page is readerable (see readerview_content.js).
        internal const val READER_VIEW_CONTENT_PORT = "mozacReaderview"

        // Name of the port connected to active reader pages for updating
        // appearance configuration (see readerview.js).
        internal const val READER_VIEW_ACTIVE_CONTENT_PORT = "mozacReaderviewActive"
        internal const val READER_VIEW_EXTENSION_URL = "resource://android/assets/extensions/readerview/"

        // Constants for building messages sent to the web extension:
        // Change the font type: {"action": "setFontType", "value": "sans-serif"}
        // Show reader view: {"action": "show", "value": {"fontSize": 3, "fontType": "serif", "colorScheme": "dark"}}
        internal const val ACTION_MESSAGE_KEY = "action"
        internal const val ACTION_CACHE_PAGE = "cachePage"
        internal const val ACTION_SHOW = "show"
        internal const val ACTION_HIDE = "hide"
        internal const val ACTION_CHECK_READER_STATE = "checkReaderState"
        internal const val ACTION_SET_COLOR_SCHEME = "setColorScheme"
        internal const val ACTION_CHANGE_FONT_SIZE = "changeFontSize"
        internal const val ACTION_SET_FONT_TYPE = "setFontType"
        internal const val ACTION_VALUE = "value"
        internal const val ACTION_VALUE_SHOW_FONT_SIZE = "fontSize"
        internal const val ACTION_VALUE_SHOW_FONT_TYPE = "fontType"
        internal const val ACTION_VALUE_SHOW_COLOR_SCHEME = "colorScheme"
        internal const val ACTION_VALUE_SCROLLY = "scrollY"
        internal const val ACTION_VALUE_ID = "id"
        internal const val READERABLE_RESPONSE_MESSAGE_KEY = "readerable"
        internal const val BASE_URL_RESPONSE_MESSAGE_KEY = "baseUrl"
        internal const val ACTIVE_URL_RESPONSE_MESSAGE_KEY = "activeUrl"

        // Constants for storing the reader mode config in shared preferences
        internal const val SHARED_PREF_NAME = "mozac_feature_reader_view"
        internal const val COLOR_SCHEME_KEY = "mozac-readerview-colorscheme"
        internal const val FONT_TYPE_KEY = "mozac-readerview-fonttype"
        internal const val FONT_SIZE_KEY = "mozac-readerview-fontsize"
        internal const val FONT_SIZE_DEFAULT = 3

        internal fun createCheckReaderStateMessage(): JSONObject {
            return JSONObject().put(ACTION_MESSAGE_KEY, ACTION_CHECK_READER_STATE)
        }

        internal fun createCachePageMessage(id: String): JSONObject {
            return JSONObject()
                .put(ACTION_MESSAGE_KEY, ACTION_CACHE_PAGE)
                .put(ACTION_VALUE_ID, id)
        }

        internal fun createShowReaderMessage(config: InfernoReaderViewConfig?, scrollY: Int? = null): JSONObject {
            if (config == null) {
                logger.warn("No config provided. Falling back to default values.")
            }

            val fontSize = config?.fontSize ?: FONT_SIZE_DEFAULT
            val fontType = config?.fontType ?: FontType.SERIF
            val colorScheme = config?.colorScheme ?: ColorScheme.LIGHT
            val configJson = JSONObject()
                .put(ACTION_VALUE_SHOW_FONT_SIZE, fontSize)
                .put(ACTION_VALUE_SHOW_FONT_TYPE, fontType.value.lowercase(Locale.ROOT))
                .put(ACTION_VALUE_SHOW_COLOR_SCHEME, colorScheme.name.lowercase(Locale.ROOT))
            if (scrollY != null) {
                configJson.put(ACTION_VALUE_SCROLLY, scrollY)
            }
            return JSONObject()
                .put(ACTION_MESSAGE_KEY, ACTION_SHOW)
                .put(ACTION_VALUE, configJson)
        }

        internal fun createHideReaderMessage(): JSONObject {
            return JSONObject().put(ACTION_MESSAGE_KEY, ACTION_HIDE)
        }
    }
}


