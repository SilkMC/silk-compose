@file:Suppress("MemberVisibilityCanBePrivate", "CanBeParameter", "unused")

package net.silkmc.silk.compose.color

import com.github.ajalt.colormath.model.LAB
import com.github.ajalt.colormath.model.RGB
import com.github.ajalt.colormath.model.RGBInt
import com.github.ajalt.colormath.transform.multiplyAlpha
import net.minecraft.world.level.material.MapColor

/**
 * A pre-calculated color shade of the [mapColor] with
 * the given [brightness].
 */
data class MapColorShade(
    val mapColor: MapColor,
    val brightness: MapColor.Brightness,
) {

    val rgb: RGB = if (mapColor == MapColor.NONE) {
        RGB(1, 1, 1, 0)
    } else {
        RGBInt(mapColor.col.toUInt()).toSRGB()
            .copy(alpha = brightness.modifier / 255f)
            .multiplyAlpha()
            .copy(alpha = 1f)
    }

    val lab: LAB = rgb.toLAB()

    val mapByte = (mapColor.id * 4 + brightness.id).toByte()

    @Deprecated(
        message = "Minecraft has renamed MaterialColor to MapColor, therefore this property has been renamed to mapColor.",
        replaceWith = ReplaceWith("mapColor", "net.silkmc.silk.compose.color.MapColorShade"),
    )
    val materialColor get() = mapColor
}

@Deprecated(
    message = "Minecraft has renamed MaterialColor to MapColor, therefore this class has been renamed to MapColorShade.",
    replaceWith = ReplaceWith("MapColorShade", "net.silkmc.silk.compose.color.MapColorShade"),
)
typealias MaterialColorShade = MapColorShade
