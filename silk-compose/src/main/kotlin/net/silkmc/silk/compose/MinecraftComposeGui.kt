@file:Suppress("MemberVisibilityCanBePrivate")

package net.silkmc.silk.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.InternalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asComposeCanvas
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.scene.MultiLayerComposeScene
import androidx.compose.ui.unit.IntSize
import kotlinx.coroutines.*
import net.minecraft.ChatFormatting
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.core.component.DataComponents
import net.minecraft.network.protocol.game.*
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.InteractionHand
import net.minecraft.world.entity.decoration.GlowItemFrame
import net.minecraft.world.item.Items
import net.silkmc.silk.compose.color.MapColorUtils
import net.silkmc.silk.compose.internal.MapIdGenerator
import net.silkmc.silk.compose.util.MathUtil
import net.silkmc.silk.compose.util.MathUtil.toMkArray
import net.silkmc.silk.compose.util.MathUtil.withoutAxis
import net.silkmc.silk.core.annotations.ExperimentalSilkApi
import net.silkmc.silk.core.event.Events
import net.silkmc.silk.core.event.Player
import net.silkmc.silk.core.event.Server
import net.silkmc.silk.core.item.itemStack
import net.silkmc.silk.core.logging.logError
import net.silkmc.silk.core.logging.logWarning
import net.silkmc.silk.core.task.mcCoroutineScope
import net.silkmc.silk.core.task.silkCoroutineScope
import net.silkmc.silk.core.text.sendText
import org.jetbrains.skia.Surface
import org.jetbrains.skiko.FrameDispatcher
import org.jetbrains.skiko.MainUIDispatcher
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import kotlin.collections.set
import kotlin.time.Duration.Companion.seconds

/**
 * Creates a new server-side [MinecraftComposeGui]. This allows you to use any
 * composable functions inside the [content] lambda of this function.
 *
 * @param blockWidth the width of the gui **in blocks**
 * @param blockHeight the height of the gui **in blocks**
 * @param position the position where the gui will be created in-game
 * @param backgroundColor the background color of the gui, can be transparent as well
 * @param content define your gui using composable functions in here
 *
 * @see MinecraftComposeGui
 */
fun ServerPlayer.displayComposable(
    blockWidth: Int, blockHeight: Int,
    position: BlockPos = blockPosition().relative(direction, 2),
    backgroundColor: Color = Color.White,
    content: @Composable (gui: MinecraftComposeGui) -> Unit,
) = MinecraftComposeGui(
    blockWidth, blockHeight,
    content,
    this,
    position,
    backgroundColor
)

/**
 * A server-side gui making use of Compose and Compose UI. You may create this gui using
 * [displayComposable].
 * Internally, the gui is rendered on maps, which are placed inside invisible item frames.
 * Everything only happens through packets, therefore the gui does not really exist on
 * the server.
 * If you want to remove this gui, call the [close] function.
 */
@OptIn(InternalComposeUiApi::class)
class MinecraftComposeGui(
    val blockWidth: Int, val blockHeight: Int,
    val content: @Composable (gui: MinecraftComposeGui) -> Unit,
    val player: ServerPlayer,
    val position: BlockPos,
    val backgroundColor: Color,
) {

    companion object {
        private val playerGuis = ConcurrentHashMap<UUID, MinecraftComposeGui>()

        init {
            @OptIn(ExperimentalSilkApi::class)
            Events.Server.preStop.listen {
                playerGuis.values.forEach { it.close() }
                playerGuis.clear()
            }

            @OptIn(ExperimentalSilkApi::class)
            Events.Player.preQuit.listen { event ->
                playerGuis[event.player.uuid]?.close()
            }
        }

        internal fun onSwingHand(player: ServerPlayer, packet: ServerboundSwingPacket) {
            if (packet.hand == InteractionHand.MAIN_HAND) {
                playerGuis[player.uuid]?.onLeftClick()
            }
        }

        internal fun onUpdateSelectedSlot(player: ServerPlayer, packet: ServerboundSetCarriedItemPacket): Boolean {
            val gui = playerGuis[player.uuid] ?: return false

            val slotPair = player.inventory.selected to packet.slot
            val (prevSlot, newSlot) = slotPair

            val scrollDelta = when {
                slotPair == 8 to 0 -> 1f
                slotPair == 0 to 8 -> -1f
                prevSlot < newSlot -> 1f
                prevSlot > newSlot -> -1f
                else -> return false
            }

            return gui.onScroll(scrollDelta * 3)
        }
    }

    val pixelWidth = blockWidth * 128
    val pixelHeight = blockHeight * 128

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
    private val singleThreadDispatcher = rawSingleThreadDispatcher +
        CoroutineExceptionHandler { _, throwable -> onException(throwable) }

    /**
     * A coroutine scope using [singleThreadDispatcher] as its context.
     * This scope is used for launching gui rleated coroutines in this class.
     * It is cancelled when the gui is closed.
     */
    private val coroutineScope = CoroutineScope(singleThreadDispatcher)


    private val guiDirection = player.direction.opposite
    private val placementDirection = guiDirection.getCounterClockWise(Direction.Axis.Y)

    /**
     * The perceived position of the display is one behind the actual item frame position.
     */
    private val displayPosition = when (guiDirection) {
        Direction.EAST, Direction.SOUTH -> position
        Direction.WEST, Direction.NORTH -> position.relative(guiDirection.opposite)
        else -> position
    }

    private val planePoint = displayPosition.toMkArray()
    private val planeNormal = guiDirection.normal.toMkArray()

    /**
     * The top left corner of the gui in the world.
     */
    private val topCorner = displayPosition
        .below(blockHeight - 1)
        .relative(
            placementDirection,
            blockWidth - (if (guiDirection == Direction.WEST || guiDirection == Direction.SOUTH) 0 else 1)
        )
        .withoutAxis(guiDirection.axis)

    /**
     * The bottom right corner of the gui in the world.
     */
    private val bottomCorner = displayPosition
        .relative(placementDirection.opposite)
        .above()
        .relative(
            placementDirection,
            if (guiDirection == Direction.WEST || guiDirection == Direction.SOUTH) 1 else 0
        )
        .withoutAxis(guiDirection.axis)


    /**
     * The gui is divided into chunks of 128x128 pixels. Each chunk has its own
     * [GuiChunk] which is used for updating exactly one specific map.
     */
    private val guiChunks = Array(blockWidth * blockHeight) { GuiChunk() }
    private fun getGuiChunk(x: Int, y: Int) = guiChunks[x + y * blockWidth]

    /**
     * A list of entity IDs of the item frames used for displaying the gui.
     * This list is used for removing the item frames when the gui is closed.
     *
     * @see close
     */
    @Volatile
    private var itemFrameEntityIds: Set<Int> = emptySet()


    /**
     * The [FrameDispatcher] used for updating the gui. This is required
     * as the gui is rendered on maps, which need to be updated on changes.
     *
     * @see updateMinecraftMaps
     */
    private val frameDispatcher = FrameDispatcher(singleThreadDispatcher) {
        updateMinecraftMaps() }

    /**
     * A skiko surface, as created in [androidx.compose.ui.ImageComposeScene]
     */
    val surface = Surface.makeRasterN32Premul(pixelWidth, pixelHeight)

    /**
     * The native skia canvas, as obtained from the skia surface.
     */
    val nativeCanvas = surface.canvas

    /**
     * The compose canvas, which is a wrapped skia canvas obtained from the skia surface.
     */
    val canvas = nativeCanvas.asComposeCanvas()

    /**
     * An [androidx.compose.ui.scene.ComposeScene], as used in
     * [androidx.compose.ui.ImageComposeScene]. We cannot use the latter,
     * as it provides no invalidation mechanism, which is required for
     * updating the in-game gui.
     */
    private val scene = MultiLayerComposeScene(
        coroutineContext = singleThreadDispatcher,
        invalidate = { frameDispatcher.scheduleFrame() },
        size = IntSize(pixelWidth, pixelHeight),
    )


    init {
        playerGuis[player.uuid]?.close()
        playerGuis[player.uuid] = this

        coroutineScope.launch {
            createFakeItemFrames()
            scene.setContent {
                Box(Modifier
                    .fillMaxSize()
                    .background(backgroundColor).fillMaxSize()) {
                    content(this@MinecraftComposeGui)
                }
            }
        }
    }

    private fun createFakeItemFrames() {
        val entityIds = HashSet<Int>(blockWidth * blockHeight)
        for (xFrame in 0 until blockWidth) {
            for (yFrame in 0 until blockHeight) {
                val guiChunk = getGuiChunk(xFrame, yFrame)
                val connection = player.connection

                val framePos = position.below(yFrame).relative(placementDirection, xFrame)

                // spawn the fake item frame
                val itemFrame = GlowItemFrame(player.level(), framePos, guiDirection)
                itemFrame.isInvisible = true
                connection.send(itemFrame.addEntityPacket)
                entityIds.add(itemFrame.id)

                // put the map in the item frame
                val composeStack = itemStack(Items.FILLED_MAP) {
                    set(DataComponents.MAP_ID, guiChunk.mapId)
                }
                itemFrame.setItem(composeStack, false)
                val newData = itemFrame.entityData.packDirty()
                if (newData != null) {
                    connection.send(ClientboundSetEntityDataPacket(itemFrame.id, newData))
                }

                connection.send(guiChunk.createClearPacket())
            }
        }
        itemFrameEntityIds = entityIds
    }

    private suspend fun updateMinecraftMaps() {
        nativeCanvas.clear(org.jetbrains.skia.Color.TRANSPARENT)
        scene.render(canvas, System.nanoTime())

        val image = surface.makeImageSnapshot()
        val pixmap = image.peekPixels() ?: return

        coroutineScope {
            for (xFrame in 0 until blockWidth) {
                for (yFrame in 0 until blockHeight) {
                    launch(Dispatchers.Default) {
                        val guiChunk = getGuiChunk(xFrame, yFrame)

                        for (x in 0 until 128) {
                            for (y in 0 until 128) {
                                val bitmapColor = pixmap.getColor(xFrame * 128 + x, yFrame * 128 + y)
                                guiChunk.setColor(x, y, MapColorUtils.cachedBitmapColorToMapColor(bitmapColor))
                            }
                        }

                        val updatePacket = guiChunk.createPacket()
                        if (updatePacket != null) {
                            player.connection.send(updatePacket)
                        }
                    }
                }
            }
        }
        pixmap.close()
        image.close()
    }

    private fun calculateOffset(): Offset? {
        val intersection = MathUtil.rayPlaneIntersection(
            player.eyePosition.toMkArray(),
            player.lookAngle.toMkArray(),
            planePoint,
            planeNormal
        )

        val (worldX, worldY) = intersection.withoutAxis(guiDirection.axis)

        val (topX, topY) = topCorner
        val (bottomX, bottomY) = bottomCorner

        val planeX = when (worldX) {
            in bottomX..topX -> worldX - bottomX
            in topX..bottomX -> bottomX - worldX
            else -> return null
        }

        val planeY = when (worldY) {
            in bottomY..topY -> worldY - bottomY
            in topY..bottomY -> bottomY - worldY
            else -> return null
        }

        return Offset((planeX * 128).toFloat(), (planeY * 128).toFloat())
    }

    private fun onLeftClick() {
        val offset = calculateOffset() ?: return

        coroutineScope.launch {
            // ensure that the following press-release combo is in correct order, therefore release
            scene.sendPointerEvent(PointerEventType.Release, offset)

            scene.sendPointerEvent(PointerEventType.Press, offset)
            scene.sendPointerEvent(PointerEventType.Release, offset)
        }
    }

    private fun onScroll(delta: Float): Boolean {
        val offset = calculateOffset() ?: return false

        // only reset the slot if the player is directly looking at the gui
        player.connection.send(ClientboundSetCarriedItemPacket(4))
        player.inventory.selected = 4

        coroutineScope.launch {
            scene.sendPointerEvent(PointerEventType.Scroll, offset, Offset(0f, delta))
        }
        return true
    }

    /**
     * Safely removes this gui. This function will be called automatically
     * if the server shuts down.
     */
    fun close() {
        playerGuis.remove(player.uuid, this)

        suspend fun actualClose() {
            // visually remove the item frames
            player.connection.send(ClientboundRemoveEntitiesPacket(*itemFrameEntityIds.toIntArray()))

            try {
                // cancel and close everything
                frameDispatcher.cancel()
                coroutineScope.cancel()
                val closeResult = withTimeoutOrNull(10.seconds) {
                    withContext(singleThreadDispatcher) {
                        surface.close()
                        scene.close()
                    }
                }
                if (closeResult == null) {
                    logError("Timed out while closing internal compose gui surface and scene for player '${player.scoreboardName}'")
                }
            } finally {
                // clear the maps (there is no map removal packet)
                mcCoroutineScope.launch {
                    for (chunk in guiChunks) {
                        if (!player.hasDisconnected()) {
                            player.connection.send(chunk.createClearPacket())
                        }
                    }
                }
                MapIdGenerator.makeOldIdsAvailable(guiChunks.map { it.mapId.id })
            }
        }

        val closeTimeoutDuration = 30.seconds
        silkCoroutineScope.launch {
            val result = withTimeoutOrNull(closeTimeoutDuration) {
                actualClose()
            }
            if (result == null) {
                logError("Closing the compose gui for player '${player.scoreboardName}' took too long, timed out after $closeTimeoutDuration")
            }
        }
    }

    private fun onException(throwable: Throwable) {
        logError(buildString {
            appendLine("An error occurred in the coroutine scope of a compose gui for player '${player.scoreboardName}'")
            appendLine("Stack trace:")
            append(throwable.stackTraceToString())
        })
        logWarning("Closing gui for player '${player.scoreboardName}'")
        close()
        player.sendText("The gui you had open has been closed due to an internal error.") {
            color = ChatFormatting.RED.color }
    }
}
