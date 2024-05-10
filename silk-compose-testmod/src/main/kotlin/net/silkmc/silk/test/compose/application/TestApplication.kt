package net.silkmc.silk.test.compose.application

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.launchApplication
import androidx.compose.ui.window.rememberWindowState
import com.github.ajalt.colormath.calculate.isInSRGBGamut
import com.github.ajalt.colormath.model.HSV
import com.github.ajalt.colormath.model.SRGB
import com.godaddy.android.colorpicker.ClassicColorPicker
import com.godaddy.android.colorpicker.HsvColor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import net.silkmc.silk.compose.color.MapColorUtils
import net.silkmc.silk.test.compose.util.rememberBitmapResource

private var inputColor by mutableStateOf(SRGB(0, 0, 0))

// this is an actual compose desktop application to test some behaviour

fun testApplication() = CoroutineScope(Dispatchers.Default).launchApplication {
    Window(
        onCloseRequest = {
            exitApplication()
        },
        title = "Color Conversion Test",
        state = rememberWindowState(width = 800.dp, height = 600.dp)
    ) {
        Column {
            ClassicColorPicker(Modifier.weight(1f), color = HsvColor.from(Color.Red)) {
                inputColor = HSV(it.hue, it.saturation, it.value, it.alpha).toSRGB()
            }

            Row(Modifier.weight(1f).padding(10.dp)) {
                val backgroundImage = rememberBitmapResource("/images/tux.png")

                Box {
                    Box(
                        Modifier
                            .size(200.dp)
                            .background(remember(inputColor) { inputColor.run { Color(r, g, b, alpha) } })
                    )
                }

                Column {
                    val shade = remember(inputColor) {
                        val converted = MapColorUtils.toMapColorShade(inputColor)
                        if (!converted.rgb.isInSRGBGamut()) {
                            println("nicht in der range!!!!")
                        }
                        converted
                    }

                    Column {
                        Text("materialColor id = ${shade.mapColor.id}")
                        Text("brightness id = ${shade.brightness.id}")
                        Text("byte = ${shade.mapByte}")
                    }

                    Box {
                        if (backgroundImage != null) {
                            Image(backgroundImage, "tux background")
                        }

                        Box(
                            Modifier
                                .size(200.dp)
                                .background(remember(shade) { shade.rgb.run { Color(r, g, b, alpha) } })
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun TestApplicationCompanion() {
    Box(Modifier.fillMaxSize().background(inputColor.run { Color(r, g, b, alpha) }))
}
