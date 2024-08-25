@file:Suppress("MemberVisibilityCanBePrivate")

package net.silkmc.silk.compose.impl

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.InternalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asComposeCanvas
import androidx.compose.ui.scene.MultiLayerComposeScene
import androidx.compose.ui.unit.IntSize
import kotlinx.coroutines.*
import net.minecraft.ChatFormatting
import net.minecraft.server.level.ServerPlayer
import net.silkmc.silk.compose.GuiChunk
import net.silkmc.silk.compose.internal.MapIdGenerator
import net.silkmc.silk.core.logging.logError
import net.silkmc.silk.core.logging.logWarning
import net.silkmc.silk.core.task.mcCoroutineScope
import net.silkmc.silk.core.task.silkCoroutineScope
import net.silkmc.silk.core.text.sendText
import org.jetbrains.skia.Pixmap
import org.jetbrains.skia.Surface
import org.jetbrains.skiko.FrameDispatcher
import org.jetbrains.skiko.MainUIDispatcher
import java.util.*
import kotlin.time.Duration.Companion.seconds


/**
 * The abstract basis for custom Compose GUIs. This class contains a
 * [MultiLayerComposeScene] and calls the [renderPixmap] function on
 * each change to the UI.
 */
@OptIn(InternalComposeUiApi::class)
abstract class AbstractComposeGui(
    val content: @Composable (gui: AbstractComposeGui) -> Unit,
    val backgroundColor: Color,
    val pixelWidth: Int,
    val pixelHeight: Int,
    private val playerGuiRegistry: MutableMap<UUID, AbstractComposeGui>,
) {
    /**
     * The name with which this gui will appear in log messages.
     * It should be a short and unique name, making it easy to identify
     * the specific instance.
     */
    protected abstract val logName: String

    /**
     * The [singleThreadDispatcher] without exception handling.
     * This is needed for using the dispatcher inside [close] without
     * potentially causing a stack overflow.
     *
     * @see singleThreadDispatcher
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    private val rawSingleThreadDispatcher = MainUIDispatcher.limitedParallelism(1)

    /**
     * The coroutine scope used for updating the gui. This scope is limited to
     * one thread.
     * This scope also has an exception handler, which will close the gui
     * and send an error message to the player if an exception occurs.
     *
     * This is the [rawSingleThreadDispatcher] with an exception handler.
     */
    protected val singleThreadDispatcher = rawSingleThreadDispatcher +
        CoroutineExceptionHandler { _, throwable -> onException(throwable) }

    /**
     * A coroutine scope using [singleThreadDispatcher] as its context.
     * This scope is used for launching gui rleated coroutines in this class.
     * It is cancelled when the gui is closed.
     */
    protected val coroutineScope = CoroutineScope(singleThreadDispatcher)


    /**
     * The [FrameDispatcher] used for updating the gui. This is required
     * as the gui is rendered on maps, which need to be updated on changes.
     *
     * @see renderFrame
     */
    private val frameDispatcher = FrameDispatcher(singleThreadDispatcher) {
        renderFrame() }

    /**
     * A skiko surface, as created in [androidx.compose.ui.ImageComposeScene]
     */
    protected val surface = Surface.makeRasterN32Premul(pixelWidth, pixelHeight)

    /**
     * The native skia canvas, as obtained from the skia surface.
     */
    protected val nativeCanvas = surface.canvas

    /**
     * The compose canvas, which is a wrapped skia canvas obtained from the skia surface.
     */
    protected val canvas = nativeCanvas.asComposeCanvas()

    /**
     * An [androidx.compose.ui.scene.ComposeScene], as used in
     * [androidx.compose.ui.ImageComposeScene]. We cannot use the latter,
     * as it provides no invalidation mechanism, which is required for
     * updating the in-game gui.
     */
    protected val scene = MultiLayerComposeScene(
        coroutineContext = singleThreadDispatcher,
        invalidate = { frameDispatcher.scheduleFrame() },
        size = IntSize(pixelWidth, pixelHeight),
    )


    /**
     * A set of players that have this gui open. This set is used to close
     * the gui for all players when the gui is closed and to send updates
     * to all players.
     */
    protected val players: MutableSet<ServerPlayer> = Collections.synchronizedSet(HashSet())

    abstract val guiChunks: List<GuiChunk>


    init {
        coroutineScope.launch {
            scene.setContent {
                Box(Modifier.fillMaxSize().background(backgroundColor)) {
                    content(this@AbstractComposeGui)
                }
            }
        }
    }

    /**
     * Renders a frame of the Compose gui. This function is called when
     * the gui has invalidations.
     */
    private suspend fun renderFrame() {
        nativeCanvas.clear(org.jetbrains.skia.Color.TRANSPARENT)
        scene.render(canvas, System.nanoTime())

        val image = surface.makeImageSnapshot()
        val pixmap = image.peekPixels() ?: return

        renderPixmap(pixmap)

        pixmap.close()
        image.close()
    }

    /**
     * Allows the implementation of this gui to render the pixmap to
     * any target in-game.
     */
    protected abstract suspend fun renderPixmap(pixmap: Pixmap)

    /**
     * Safely removes this gui. This function will be called automatically
     * if the server shuts down.
     */
    fun close() {
        beforeClose()

        suspend fun actualClose() {
            try {
                frameDispatcher.cancel()
                coroutineScope.cancel()
                val closeResult = withTimeoutOrNull(10.seconds) {
                    withContext(singleThreadDispatcher) {
                        surface.close()
                        scene.close()
                    }
                }
                if (closeResult == null) {
                    logError("Timed out while closing internal surface and scene for gui '$logName'")
                }
            } finally {
                mcCoroutineScope.launch {
                    afterClose()
                }
            }
        }

        val closeTimeoutDuration = 30.seconds
        silkCoroutineScope.launch {
            val result = withTimeoutOrNull(closeTimeoutDuration) {
                actualClose()
            }
            if (result == null) {
                logError("Closing the compose gui '$logName' took too long, timed out after $closeTimeoutDuration")
            }
        }
    }

    protected open fun beforeClose() { }
    protected open fun afterClose() {
        MapIdGenerator.makeOldIdsAvailable(guiChunks.map { it.mapId.id })
    }

    protected open fun onException(throwable: Throwable) {
        logError(buildString {
            appendLine("An error occurred in the coroutine scope of gui '$logName'")
            appendLine("Stack trace:")
            append(throwable.stackTraceToString())
        })
        logWarning("Closing gui '$logName'")
        val closePlayers = players.toList()
        close()
        for (player in closePlayers) {
            player.sendText("The gui you had open has been closed due to an internal error.") {
                color = ChatFormatting.RED.color }
        }
    }

    open fun displayTo(player: ServerPlayer) {
        playerGuiRegistry[player.uuid]?.removeFor(player)
        playerGuiRegistry[player.uuid] = this
    }

    open fun removeFor(player: ServerPlayer) {
        players.remove(player)
        playerGuiRegistry.remove(player.uuid)
        for (chunk in guiChunks) {
            if (!player.hasDisconnected()) {
                // clear the map (there is no map removal packet)
                player.connection.send(chunk.createClearPacket())
            }
        }
    }
}
