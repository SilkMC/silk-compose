package net.silkmc.silk.compose

import net.minecraft.network.protocol.game.ClientboundMapItemDataPacket
import net.minecraft.world.level.saveddata.maps.MapId
import net.minecraft.world.level.saveddata.maps.MapItemSavedData
import net.silkmc.silk.compose.internal.MapIdGenerator
import net.silkmc.silk.compose.util.Constants
import net.silkmc.silk.core.annotations.InternalSilkApi
import java.util.*
import kotlin.math.max
import kotlin.math.min

private val MAP_SIZE = Constants.mapPixelSize

@InternalSilkApi
class GuiChunk(
    val mapId: MapId = MapId(MapIdGenerator.nextId()),
    private val colors: ByteArray = ByteArray(MAP_SIZE * MAP_SIZE),
) {
    private var dirty = false
    private var startX = 0
    private var startY = 0
    private var endX = 0
    private var endY = 0

    fun setColor(x: Int, y: Int, colorId: Byte) {
        val previousColorId = colors[x + y * MAP_SIZE]

        if (previousColorId != colorId) {
            colors[x + y * MAP_SIZE] = colorId

            if (dirty) {
                startX = min(startX, x); startY = min(startY, y)
                endX = max(endX, x); endY = max(endY, y)
            } else {
                dirty = true
                startX = x; startY = y
                endX = x; endY = y
            }
        }
    }


    fun createFullPacket(): ClientboundMapItemDataPacket {
        val updateData = MapItemSavedData.MapPatch(0, 0, MAP_SIZE, MAP_SIZE, colors)
        return createDataPacket(updateData)
    }

    fun createUpdatePacket(): ClientboundMapItemDataPacket? {
        if (!dirty) return null
        dirty = false

        val width = endX + 1 - startX
        val height = endY + 1 - startY
        val packetColors = ByteArray(width * height)

        for (x in 0 until width) {
            for (y in 0 until height) {
                packetColors[x + y * width] = colors[startX + x + (startY + y) * MAP_SIZE]
            }
        }

        val updateData = MapItemSavedData.MapPatch(startX, startY, width, height, packetColors)
        return createDataPacket(updateData)
    }

    fun createClearPacket(): ClientboundMapItemDataPacket {
        val updateData = MapItemSavedData.MapPatch(0, 0, MAP_SIZE, MAP_SIZE, ByteArray(MAP_SIZE * MAP_SIZE))
        return createDataPacket(updateData)
    }


    private fun createDataPacket(patch: MapItemSavedData.MapPatch): ClientboundMapItemDataPacket {
        return ClientboundMapItemDataPacket(mapId, 0, false, Optional.empty(), Optional.of(patch))
    }
}
