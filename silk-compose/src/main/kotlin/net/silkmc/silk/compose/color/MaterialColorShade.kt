@file:Suppress("MemberVisibilityCanBePrivate", "CanBeParameter", "unused")

package net.silkmc.silk.compose.color

import com.github.ajalt.colormath.model.LAB
import com.github.ajalt.colormath.model.RGB
import com.github.ajalt.colormath.model.RGBInt
import com.github.ajalt.colormath.transform.multiplyAlpha
import net.minecraft.world.level.material.MaterialColor

data class MaterialColorShade(
    val materialColor: MaterialColor,
    val brightness: MaterialColor.Brightness,
) {

    val rgb: RGB = if (materialColor == MaterialColor.NONE) {
        RGB(1, 1, 1, 0)
    } else {
        RGBInt(materialColor.col.toUInt()).toSRGB()
            .copy(alpha = brightness.modifier / 255f)
            .multiplyAlpha()
            .copy(alpha = 1f)
    }

    val lab: LAB = rgb.toLAB()

    val mapByte = (materialColor.id * 4 + brightness.id).toByte()
}
