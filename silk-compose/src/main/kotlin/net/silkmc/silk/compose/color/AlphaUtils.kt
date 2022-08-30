package net.silkmc.silk.compose.color

import com.github.ajalt.colormath.Color
import com.github.ajalt.colormath.model.RGB

fun Color.onWhite(): RGB {
    if (alpha >= 1f) return this.toSRGB()
    return toSRGB().run {
        RGB(
            r = 1f - alpha * (1f - r),
            g = 1f - alpha * (1f - g),
            b = 1f - alpha * (1f - b),
            alpha = 1f
        )
    }
}
