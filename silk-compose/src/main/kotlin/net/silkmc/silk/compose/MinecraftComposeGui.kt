@file:Suppress("MemberVisibilityCanBePrivate")

package net.silkmc.silk.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.ComposeScene
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.unit.dp
import com.github.ajalt.colormath.model.SRGB
import kotlinx.coroutines.*
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.network.protocol.game.*
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.InteractionHand
import net.minecraft.world.entity.decoration.GlowItemFrame
import net.minecraft.world.item.Items
import net.silkmc.silk.compose.color.MaterialColorUtils
import net.silkmc.silk.compose.internal.MapIdGenerator
import net.silkmc.silk.compose.util.MathUtil
import net.silkmc.silk.compose.util.MathUtil.toMkArray
import net.silkmc.silk.compose.util.MathUtil.withoutAxis
import net.silkmc.silk.core.annotations.ExperimentalSilkApi
import net.silkmc.silk.core.event.Events
import net.silkmc.silk.core.event.Server
import net.silkmc.silk.core.logging.logError
import net.silkmc.silk.core.task.mcSyncLaunch
import net.silkmc.silk.core.task.silkCoroutineScope
import org.jetbrains.skia.Bitmap
import org.jetbrains.skia.Canvas
import org.jetbrains.skiko.FrameDispatcher
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import kotlin.collections.set

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
    content: @Composable BoxScope.(gui: MinecraftComposeGui) -> Unit,
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
class MinecraftComposeGui(
    val blockWidth: Int, val blockHeight: Int,
    val content: @Composable BoxScope.(gui: MinecraftComposeGui) -> Unit,
    val player: ServerPlayer,
    val position: BlockPos,
    val backgroundColor: Color,
) {

    companion object {
        private val playerGuis = ConcurrentHashMap<UUID, MinecraftComposeGui>()

        private val bitmapToMapColorCache = ConcurrentHashMap<Int, Byte>()

        init {
            @OptIn(ExperimentalSilkApi::class)
            Events.Server.postStop.listen {
                playerGuis.values.forEach { it.close() }
                playerGuis.clear()
            }

            ServerPlayConnectionEvents.DISCONNECT.register { handler, _ ->
                playerGuis[handler.player.uuid]?.close()
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

    private val coroutineContext = Executors.newSingleThreadExecutor().asCoroutineDispatcher()
    private val coroutineScope = CoroutineScope(coroutineContext)

    @OptIn(ExperimentalCoroutinesApi::class)
    private val limitedDispatcher = Dispatchers.Default.limitedParallelism(1)

    // values for geometry

    private val guiDirection = player.direction.opposite
    private val placementDirection = guiDirection.getCounterClockWise(Direction.Axis.Y)

    // the perceived position of the display is one behind the actual item frame position
    private val displayPosition = when (guiDirection) {
        Direction.EAST, Direction.SOUTH -> position
        Direction.WEST, Direction.NORTH -> position.relative(guiDirection.opposite)
        else -> position
    }

    private val planePoint = displayPosition.toMkArray()
    private val planeNormal = guiDirection.normal.toMkArray()

    private val topCorner = displayPosition
        .below(blockHeight - 1)
        .relative(
            placementDirection,
            blockWidth - (if (guiDirection == Direction.WEST || guiDirection == Direction.SOUTH) 0 else 1)
        )
        .withoutAxis(guiDirection.axis)

    private val bottomCorner = displayPosition
        .relative(placementDirection.opposite)
        .above()
        .relative(
            placementDirection,
            if (guiDirection == Direction.WEST || guiDirection == Direction.SOUTH) 1 else 0
        )
        .withoutAxis(guiDirection.axis)

    // values for rendering

    private val guiChunks = Array(blockWidth * blockHeight) { GuiChunk() }
    private fun getGuiChunk(x: Int, y: Int) = guiChunks[x + y * blockWidth]

    private var placedItemFrames = false
    private val itemFrameEntityIds = ArrayList<Int>(blockWidth * blockHeight)

    private val frameDispatcher = FrameDispatcher(coroutineContext) { updateMinecraftMaps() }
    private val scene = ComposeScene(coroutineContext) { frameDispatcher.scheduleFrame() }

    init {
        scene.setContent {
            Box(
                Modifier
                    .size((blockWidth * 128).dp, (blockHeight * 128).dp)
                    .background(backgroundColor)
            ) {
                content(this@MinecraftComposeGui)
            }
        }

        playerGuis[player.uuid]?.close()
        playerGuis[player.uuid] = this
    }

    private fun bitmapToMapColor(bitmapColor: Int): Byte {
        return bitmapToMapColorCache.getOrPut(bitmapColor) {
            val rgb = Color(bitmapColor)
                .run { SRGB(red, green, blue, alpha) }

            MaterialColorUtils.toMaterialColorId(rgb).mapByte
        }
    }

    private suspend fun updateMinecraftMaps() {
        val bitmap = Bitmap().also {
            if (!it.allocN32Pixels(blockWidth * 128, blockHeight * 128, true))
                logError("Could not allocate the required resources for rendering the compose gui!")
        }
        val canvas = Canvas(bitmap)

        scene.render(canvas, System.nanoTime())

        val connection = player.connection

        coroutineScope {
            for (xFrame in 0 until blockWidth) {
                for (yFrame in 0 until blockHeight) {
                    launch(Dispatchers.Default) {
                        val guiChunk = getGuiChunk(xFrame, yFrame)

                        for (x in 0 until 128) {
                            for (y in 0 until 128) {
                                val bitmapColor = bitmap.getColor(xFrame * 128 + x, yFrame * 128 + y)
                                guiChunk.setColor(x, y, bitmapToMapColor(bitmapColor))
                            }
                        }

                        if (!placedItemFrames) {
                            val framePos = position.below(yFrame).relative(placementDirection, xFrame)

                            // spawn the fake item frame
                            val itemFrame = GlowItemFrame(player.level, framePos, guiDirection)
                            itemFrame.isInvisible = true
                            connection.send(itemFrame.addEntityPacket)
                            withContext(limitedDispatcher) {
                                itemFrameEntityIds += itemFrame.id
                            }

                            // put the map in the item frame
                            val composeStack = Items.FILLED_MAP.defaultInstance.apply {
                                orCreateTag.putInt("map", guiChunk.mapId)
                            }
                            itemFrame.setItem(composeStack, false)
                            connection.send(ClientboundSetEntityDataPacket(itemFrame.id, itemFrame.entityData, true))

                            connection.send(guiChunk.createClearPacket())
                        }

                        // send the map data
                        val updatePacket = guiChunk.createPacket()
                        if (updatePacket != null) {
                            connection.send(updatePacket)
                        }
                    }
                }
            }
        }

        if (!placedItemFrames) {
            placedItemFrames = true
        }

        // TODO check this on macos
        //canvas.clear(0)
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

        val guiContext = coroutineContext

        silkCoroutineScope.launch {
            withContext(guiContext) {
                player.connection.send(ClientboundRemoveEntitiesPacket(*itemFrameEntityIds.toIntArray()))

                try {
                    frameDispatcher.cancel()
                    scene.close()
                } finally {
                    coroutineScope {
                        guiChunks.forEach {
                            mcSyncLaunch {
                                if (!player.hasDisconnected()) {
                                    player.connection.send(it.createClearPacket())
                                }
                            }
                        }
                    }

                    MapIdGenerator.makeOldIdsAvailable(guiChunks.map { it.mapId })
                }
            }

            guiContext.cancel()
            guiContext.close()
        }
    }
}
