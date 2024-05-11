package net.silkmc.silk.compose.impl

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.level.saveddata.maps.MapId
import net.silkmc.silk.compose.GuiChunk
import net.silkmc.silk.compose.color.MapColorUtils
import net.silkmc.silk.compose.internal.MapIdGenerator
import net.silkmc.silk.compose.util.Constants
import net.silkmc.silk.core.annotations.ExperimentalSilkApi
import net.silkmc.silk.core.annotations.InternalSilkApi
import net.silkmc.silk.core.event.Events
import net.silkmc.silk.core.event.Player
import net.silkmc.silk.core.logging.logInfo
import org.jetbrains.skia.Pixmap
import java.util.*
import java.util.concurrent.ConcurrentHashMap

@ExperimentalSilkApi
class SingleMapComposeGui(
    content: @Composable (AbstractComposeGui) -> Unit,
    backgroundColor: Color,
    val player: ServerPlayer,
) : AbstractComposeGui(
    content = content,
    backgroundColor = backgroundColor,
    pixelWidth = Constants.mapPixelSize,
    pixelHeight = Constants.mapPixelSize,
) {
    override val logName: String
        get() = "gui for single map $mapId"

    @InternalSilkApi
    companion object PlayerHolder {
        private val playerGuis = ConcurrentHashMap<ServerPlayer, MutableSet<SingleMapComposeGui>>()

        fun registerGui(player: ServerPlayer, gui: SingleMapComposeGui) {
            playerGuis
                .computeIfAbsent(player) { Collections.synchronizedSet(HashSet()) }
                .add(gui)
        }

        fun unregisterGui(player: ServerPlayer, gui: SingleMapComposeGui) {
            playerGuis[player]?.remove(gui)
        }

        fun isRegistered(player: ServerPlayer, gui: SingleMapComposeGui): Boolean {
            return playerGuis[player]?.contains(gui) == true
        }

        init {
            @OptIn(ExperimentalSilkApi::class)
            Events.Player.preQuit.listen {
                logInfo("player ${it.player.gameProfile.name} is quitting, cleaning up single map guis")
                playerGuis.remove(it.player)
                    ?.forEach(AbstractComposeGui::close)
            }
        }
    }

    private val guiChunk = GuiChunk()
    val mapId: MapId
        get() = guiChunk.mapId

    init {
        registerGui(player, this)
    }

    override suspend fun renderPixmap(pixmap: Pixmap) {
        for (x in 0 until Constants.mapPixelSize) {
            for (y in 0 until Constants.mapPixelSize) {
                val bitmapColor = pixmap.getColor(x, y)
                guiChunk.setColor(x, y, MapColorUtils.cachedBitmapColorToMapColor(bitmapColor))
            }
        }
        guiChunk.createUpdatePacket()
            ?.let(player.connection::send)
    }

    override fun beforeClose() {
        super.beforeClose()
        unregisterGui(player, this)
    }

    override fun afterClose() {
        super.afterClose()
        if (!player.hasDisconnected()) {
            player.connection.send(guiChunk.createClearPacket())
        }
        MapIdGenerator.makeOldIdsAvailable(listOf(mapId.id))
    }
}
