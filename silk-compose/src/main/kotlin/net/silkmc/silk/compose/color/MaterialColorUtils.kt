package net.silkmc.silk.compose.color

import com.github.ajalt.colormath.calculate.differenceCIE2000
import com.github.ajalt.colormath.model.LAB
import com.github.ajalt.colormath.model.RGB
import com.github.ajalt.colormath.model.RGBInt
import net.minecraft.world.level.material.MaterialColor
import net.silkmc.silk.compose.mixin.MaterialColorAccessor

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
        val transparent = MaterialColorShade(MaterialColor.NONE, MaterialColor.Brightness.LOW)
        val white = MaterialColorShade(MaterialColor.SNOW, MaterialColor.Brightness.HIGH)
        val nearlyBlack = MaterialColorShade(MaterialColor.COLOR_BLACK, MaterialColor.Brightness.LOWEST)
    }

    /**
     * A list of colors in the [LAB] representation mapped to their pre-calculated
     * [MaterialColor] id. (Pre-calculated means that they have been multiplied by 4 and
     * their shade has been added to the id.)
     */
    private val materialColorShades = MaterialColorAccessor.getMaterialColors()
        .filter { it != null && it.col != 0 }
        .map { materialColor ->
            MaterialColor.Brightness.values().map { brightness ->
                MaterialColorShade(materialColor, brightness)
            }
        }
        .flatten()

    /**
     * Scales the given [color] down to a [MaterialColor] using
     * [differenceCIE2000] and returns the [MaterialColorShade] instance
     * of that material color variant.
     *
     * @param smoothWhite if true, nearly white values will be displayed as white as well
     */
    fun toMaterialColorId(
        color: ColormathColor,
        smoothWhite: Boolean = true,
        smoothBlack: Boolean = true,
    ): MaterialColorShade {
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
fun MaterialColor.toCompose(): ComposeColor {
    return RGBInt(col.toUInt()).run {
        androidx.compose.ui.graphics.Color(r.toInt(), g.toInt(), b.toInt())
    }
}
