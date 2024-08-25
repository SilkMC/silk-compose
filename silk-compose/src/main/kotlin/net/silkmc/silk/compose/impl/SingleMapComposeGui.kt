package net.silkmc.silk.compose.impl

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.level.saveddata.maps.MapId
import net.silkmc.silk.compose.GuiChunk
import net.silkmc.silk.compose.color.MapColorUtils
import net.silkmc.silk.compose.util.Constants
import net.silkmc.silk.core.annotations.ExperimentalSilkApi
import net.silkmc.silk.core.annotations.InternalSilkApi
import net.silkmc.silk.core.event.Events
import net.silkmc.silk.core.event.Player
import net.silkmc.silk.core.event.Server
import org.jetbrains.skia.Pixmap
import java.util.*

@ExperimentalSilkApi
class SingleMapComposeGui(
    content: @Composable (AbstractComposeGui) -> Unit,
    backgroundColor: Color,
) : AbstractComposeGui(
    content = content,
    backgroundColor = backgroundColor,
    pixelWidth = Constants.mapPixelSize,
    pixelHeight = Constants.mapPixelSize,
    playerGuiRegistry = Collections.synchronizedMap(HashMap())
) {
    override val logName: String
        get() = "gui for single map $mapId"

    @InternalSilkApi
    companion object PlayerHolder {
        val guis: MutableSet<SingleMapComposeGui> = Collections.synchronizedSet(HashSet())

        init {
            @OptIn(ExperimentalSilkApi::class)
            Events.Server.preStop.listen {
                guis.forEach { it.close() }
                guis.clear()
            }

            @OptIn(ExperimentalSilkApi::class)
            Events.Player.preQuit.listen {
                guis.forEach { gui ->
                    gui.removeFor(it.player) }
            }
        }
    }

    private val guiChunk = GuiChunk()
    val mapId: MapId
        get() = guiChunk.mapId

    override val guiChunks: List<GuiChunk>
        get() = listOf(guiChunk)

    init { guis.add(this) }

    override suspend fun renderPixmap(pixmap: Pixmap) {
        for (x in 0 until Constants.mapPixelSize) {
            for (y in 0 until Constants.mapPixelSize) {
                val bitmapColor = pixmap.getColor(x, y)
                guiChunk.setColor(x, y, MapColorUtils.cachedBitmapColorToMapColor(bitmapColor))
            }
        }
        guiChunk.createUpdatePacket()
            ?.let { pack -> players.forEach { it.connection.send(pack) } }
    }

    override fun beforeClose() {
        super.beforeClose()
        guis.remove(this)
        players.toList().forEach(::removeFor)
    }

    override fun displayTo(player: ServerPlayer) {
        super.displayTo(player)
        player.connection.send(guiChunk.createFullPacket())
    }
}
