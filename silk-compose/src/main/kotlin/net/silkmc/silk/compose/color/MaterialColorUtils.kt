package net.silkmc.silk.compose.color

import com.github.ajalt.colormath.calculate.differenceCIE2000
import com.github.ajalt.colormath.model.RGB
import com.github.ajalt.colormath.model.RGBInt
import net.minecraft.world.level.material.MapColor
import net.silkmc.silk.compose.mixin.MapColorAccessor

private typealias ComposeColor = androidx.compose.ui.graphics.Color
private typealias ColormathColor = com.github.ajalt.colormath.Color

/**
 * Utilities for working with Minecraft's [MaterialColor].
 */
object MaterialColorUtils {

    /**
     * Constant shades which are used very often.
     */
    object ConstantShades {
        val transparent = MapColorShade(MapColor.NONE, MapColor.Brightness.LOW)
        val white = MapColorShade(MapColor.SNOW, MapColor.Brightness.HIGH)
        val nearlyBlack = MapColorShade(MapColor.COLOR_BLACK, MapColor.Brightness.LOWEST)
    }

    /**
     * A list of pre-calculated color shades. For each [MaterialColor], there
     * are 4 shades (for each [MaterialColor.Brightness]). See [MaterialColorShade].
     */
    private val materialColorShades = MapColorAccessor.getMaterialColors()
        .filter { it != null && it.col != 0 }
        .map { materialColor ->
            MapColor.Brightness.entries.map { brightness ->
                MapColorShade(materialColor, brightness)
            }
        }
        .flatten()

    /**
     * Scales the given [color] down to a [MapColor] using
     * [differenceCIE2000] and returns the [MaterialColorShade] instance
     * of that material color variant.
     *
     * @param smoothWhite if true, nearly white values will be displayed as white as well
     */
    fun toMaterialColorId(
        color: ColormathColor,
        smoothWhite: Boolean = true,
        smoothBlack: Boolean = true,
    ): MapColorShade {
        val rgb = if (color is RGB) color else color.toSRGB()

        if (rgb.alpha <= 0.1f) {
            return ConstantShades.transparent
        }

        val onWhite = rgb.onWhite()

        if (smoothWhite && onWhite.isNearly(white)) {
            return ConstantShades.white
        }

        if (smoothBlack && onWhite.isNearly(black)) {
            return ConstantShades.nearlyBlack
        }

        return materialColorShades.minByOrNull { it.lab.differenceCIE2000(onWhite) }
            ?: error("Could not find a matching material color id for $color")
    }

    private val white = RGB(1, 1, 1)
    private val black = RGB(0, 0, 0)

    private fun com.github.ajalt.colormath.Color.isNearly(color: ColormathColor) =
        differenceCIE2000(color) <= 2.5f
}

/**
 * Converts the non-shaded color value of this [MaterialColor] instance to
 * the Compose [Color] representation.
 */
fun MapColor.toCompose(): ComposeColor {
    return RGBInt(col.toUInt()).run {
        androidx.compose.ui.graphics.Color(r.toInt(), g.toInt(), b.toInt())
    }
}
