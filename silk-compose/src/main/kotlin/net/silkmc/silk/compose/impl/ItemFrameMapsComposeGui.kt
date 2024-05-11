package net.silkmc.silk.compose.impl

import androidx.compose.runtime.Composable
import androidx.compose.ui.InternalComposeUiApi
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import net.minecraft.ChatFormatting
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.core.component.DataComponents
import net.minecraft.network.protocol.game.*
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.InteractionHand
import net.minecraft.world.entity.decoration.GlowItemFrame
import net.minecraft.world.item.Items
import net.silkmc.silk.compose.GuiChunk
import net.silkmc.silk.compose.color.MapColorUtils
import net.silkmc.silk.compose.displayComposable
import net.silkmc.silk.compose.internal.MapIdGenerator
import net.silkmc.silk.compose.util.Constants
import net.silkmc.silk.compose.util.MathUtil
import net.silkmc.silk.compose.util.MathUtil.toMkArray
import net.silkmc.silk.compose.util.MathUtil.withoutAxis
import net.silkmc.silk.core.annotations.ExperimentalSilkApi
import net.silkmc.silk.core.annotations.InternalSilkApi
import net.silkmc.silk.core.event.Events
import net.silkmc.silk.core.event.Player
import net.silkmc.silk.core.event.Server
import net.silkmc.silk.core.item.itemStack
import net.silkmc.silk.core.text.sendText
import org.jetbrains.skia.Pixmap
import java.util.*
import java.util.concurrent.ConcurrentHashMap


/**
 * A server-side gui making use of Compose and Compose UI. You may create this gui using
 * [displayComposable].
 * Internally, the gui is rendered on maps, which are placed inside invisible item frames.
 * Everything only happens through packets, therefore the gui does not really exist on
 * the server.
 * If you want to remove this gui, call the [close] function.
 */
@OptIn(InternalComposeUiApi::class)
@InternalSilkApi
class ItemFrameMapsComposeGui(
    content: @Composable (gui: AbstractComposeGui) -> Unit,
    backgroundColor: Color,
    val blockWidth: Int, val blockHeight: Int,
    val player: ServerPlayer,
    val position: BlockPos,
) : AbstractComposeGui(
    content = content,
    backgroundColor = backgroundColor,
    pixelWidth = blockWidth * Constants.mapPixelSize,
    pixelHeight = blockHeight * Constants.mapPixelSize,
) {
    override val logName: String
        get() = "item frame gui for ${player.gameProfile.name}"

    @InternalSilkApi
    companion object PlayerHolder {
        private val playerGuis = ConcurrentHashMap<UUID, ItemFrameMapsComposeGui>()

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

        @JvmStatic
        fun onSwingHand(player: ServerPlayer, packet: ServerboundSwingPacket) {
            if (packet.hand == InteractionHand.MAIN_HAND) {
                playerGuis[player.uuid]?.onLeftClick()
            }
        }

        @JvmStatic
        fun onUpdateSelectedSlot(player: ServerPlayer, packet: ServerboundSetCarriedItemPacket): Boolean {
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


    init {
        playerGuis[player.uuid]?.close()
        playerGuis[player.uuid] = this

        coroutineScope.launch {
            createFakeItemFrames()
        }
    }

    /**
     * Sends packets to the player to create the item frames and the maps inside them.
     * Also sends the initial empty map data to the player.
     * This function is called when the gui is opened.
     */
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

                connection.send(guiChunk.createFullPacket())
            }
        }
        itemFrameEntityIds = entityIds
    }

    override suspend fun renderPixmap(pixmap: Pixmap) {
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

                        val updatePacket = guiChunk.createUpdatePacket()
                        if (updatePacket != null) {
                            player.connection.send(updatePacket)
                        }
                    }
                }
            }
        }
    }

    /**
     * Calculate intersection of the player's look vector with the plane of the gui.
     * If the intersection is within the bounds of the gui, return the offset of the intersection
     * as gui coordinates.
     */
    private fun calculatePlayerOffset(): Offset? {
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
        val offset = calculatePlayerOffset() ?: return

        coroutineScope.launch {
            // ensure that the following press-release combo is in correct order, therefore release
            scene.sendPointerEvent(PointerEventType.Release, offset)

            scene.sendPointerEvent(PointerEventType.Press, offset)
            scene.sendPointerEvent(PointerEventType.Release, offset)
        }
    }

    private fun onScroll(delta: Float): Boolean {
        val offset = calculatePlayerOffset() ?: return false

        // only reset the slot if the player is directly looking at the gui
        player.connection.send(ClientboundSetCarriedItemPacket(4))
        player.inventory.selected = 4

        coroutineScope.launch {
            scene.sendPointerEvent(PointerEventType.Scroll, offset, Offset(0f, delta))
        }
        return true
    }

    override fun beforeClose() {
        super.beforeClose()
        playerGuis.remove(player.uuid, this)
        // visually remove the item frames
        player.connection.send(ClientboundRemoveEntitiesPacket(*itemFrameEntityIds.toIntArray()))
    }

    override fun afterClose() {
        super.afterClose()
        for (chunk in guiChunks) {
            if (!player.hasDisconnected()) {
                // clear the map (there is no map removal packet)
                player.connection.send(chunk.createClearPacket())
            }
        }
        MapIdGenerator.makeOldIdsAvailable(guiChunks.map { it.mapId.id })
    }

    override fun onException(throwable: Throwable) {
        super.onException(throwable)
        player.sendText("The gui you had open has been closed due to an internal error.") {
            color = ChatFormatting.RED.color }
    }
}
