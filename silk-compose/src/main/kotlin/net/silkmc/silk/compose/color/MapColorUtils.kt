package net.silkmc.silk.compose.color

import com.github.ajalt.colormath.calculate.differenceCIE2000
import com.github.ajalt.colormath.model.RGB
import com.github.ajalt.colormath.model.RGBInt
import com.github.ajalt.colormath.model.SRGB
import net.minecraft.world.level.material.MapColor
import net.silkmc.silk.compose.mixin.MapColorAccessor
import java.util.concurrent.ConcurrentHashMap

private typealias ComposeColor = androidx.compose.ui.graphics.Color
@Suppress("FunctionName")
fun ComposeColor(color: Int) = androidx.compose.ui.graphics.Color(color)
@Suppress("SpellCheckingInspection")
private typealias ColormathColor = com.github.ajalt.colormath.Color

/**
 * Utilities for working with Minecraft's [MaterialColor].
 */
object MapColorUtils {

    /**
     * Constant shades which are used very often.
     */
    object ConstantShades {
        val transparent = MapColorShade(MapColor.NONE, MapColor.Brightness.LOW)
        val white = MapColorShade(MapColor.SNOW, MapColor.Brightness.HIGH)
        val nearlyBlack = MapColorShade(MapColor.COLOR_BLACK, MapColor.Brightness.LOWEST)
    }

    private val white = RGB(1, 1, 1)
    private val black = RGB(0, 0, 0)

    /**
     * A cache for the [MapColorShade] instances of a given bitmap color.
     * The bitmap color is the integer representation of a color in a
     * Skia [org.jetbrains.skia.Bitmap] or [org.jetbrains.skia.Pixmap].
     */
    private val bitmapToMapColorCache = ConcurrentHashMap<Int, Byte>()

    /**
     * Checks if the given [color] is nearly the same as the [color] instance.
     * The threshold is set to a CIE2000 difference of 2.5.
     */
    private fun ColormathColor.isNearly(color: ColormathColor) =
        differenceCIE2000(color) <= 2.5f

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
    fun toMapColorShade(
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

    /**
     * Converts the given [bitmapColor] to a [MapColorShade] instance.
     * Note that this function is computationally expensive and **NOT**
     * cached, use [cachedBitmapColorToMapColor] for default caching.
     *
     * Black and white colors are smoothed out by default.
     *
     * @see toMapColorShade
     */
    fun bitmapColorToMapColorShade(bitmapColor: Int): MapColorShade {
        val rgb = ComposeColor(bitmapColor)
            .run { SRGB(red, green, blue, alpha) }
        return toMapColorShade(rgb)
    }

    /**
     * Converts the given [bitmapColor] to a map color byte, which can be
     * used for sending raw map update packets.
     * This function caches the result for future calls, since conversion
     * is computationally expensive.
     *
     * Black and white colors are smoothed out by default.
     *
     * @see toMapColorShade
     * @see bitmapColorToMapColorShade
     */
    fun cachedBitmapColorToMapColor(bitmapColor: Int): Byte {
        return bitmapToMapColorCache.getOrPut(bitmapColor) {
            bitmapColorToMapColorShade(bitmapColor).mapByte
        }
    }

    @Deprecated(
        message = "Minecraft has renamed MaterialColor to MapColor, therefore this function has been renamed to toMapColorShade.",
        replaceWith = ReplaceWith("MapColorUtils.toMapColorShade(color, smoothWhite, smoothBlack)", "net.silkmc.silk.compose.color.MapColorUtils"),
    )
    fun toMaterialColorId(
        color: ColormathColor,
        smoothWhite: Boolean = true,
        smoothBlack: Boolean = true,
    ) = toMapColorShade(color, smoothWhite, smoothBlack)
}

@Deprecated(
    message = "Minecraft has renamed MaterialColor to MapColor, therefore this class has been renamed to MapColorUtils.",
    replaceWith = ReplaceWith("MapColorUtils", "net.silkmc.silk.compose.color.MapColorUtils"),
)
typealias MaterialColorUtils = MapColorUtils

/**
 * Converts the non-shaded color value of this [MaterialColor] instance to
 * the Compose [Color] representation.
 */
fun MapColor.toCompose(): ComposeColor {
    return RGBInt(col.toUInt()).run {
        androidx.compose.ui.graphics.Color(r.toInt(), g.toInt(), b.toInt())
    }
}
