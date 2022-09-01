package net.silkmc.silk.compose

import net.minecraft.network.protocol.game.ClientboundMapItemDataPacket
import net.minecraft.world.level.saveddata.maps.MapItemSavedData
import net.silkmc.silk.compose.internal.MapIdGenerator
import kotlin.math.max
import kotlin.math.min

internal class GuiChunk(
    val mapId: Int = MapIdGenerator.nextId(),
    private val colors: ByteArray = ByteArray(128 * 128),
) {
    private var dirty = false
    private var startX = 0
    private var startY = 0
    private var endX = 0
    private var endY = 0

    fun setColor(x: Int, y: Int, colorId: Byte) {
        val previousColorId = colors[x + y * 128]

        if (previousColorId != colorId) {
            colors[x + y * 128] = colorId

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

    fun createPacket(): ClientboundMapItemDataPacket? {
        if (!dirty) return null
        dirty = false

        val width = endX + 1 - startX
        val height = endY + 1 - startY
        val packetColors = ByteArray(width * height)

        for (x in 0 until width) {
            for (y in 0 until height) {
                packetColors[x + y * width] = colors[startX + x + (startY + y) * 128]
            }
        }

        val updateData = MapItemSavedData.MapPatch(startX, startY, width, height, packetColors)
        return ClientboundMapItemDataPacket(mapId, 0, false, null, updateData)
    }

    fun createClearPacket(): ClientboundMapItemDataPacket {
        val updateData = MapItemSavedData.MapPatch(0, 0, 128, 128, ByteArray(128 * 128))
        return ClientboundMapItemDataPacket(mapId, 0, false, null, updateData)
    }
}
