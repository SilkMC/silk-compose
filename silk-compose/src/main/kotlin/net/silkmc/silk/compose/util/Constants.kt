package net.silkmc.silk.compose.util

import com.mojang.logging.LogUtils
import net.minecraft.world.item.MapItem

object Constants {

    private val LOGGER = LogUtils.getLogger()

    /**
     * The size of an in-game map in pixels.
     * It is assumed that any map is square.
     */
    val mapPixelSize = try {
        assert(MapItem.IMAGE_WIDTH == MapItem.IMAGE_HEIGHT)
        MapItem.IMAGE_WIDTH
    } catch (exc: Throwable) { // Fallback to 128 if the assert
                               // fails or on NoClassDefFoundError
        LOGGER.warn("Failed to get map pixel size, falling back to 128", exc)
        128
    }
}
