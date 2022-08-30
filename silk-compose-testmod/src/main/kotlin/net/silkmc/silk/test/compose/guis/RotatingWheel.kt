package net.silkmc.silk.test.compose.guis

import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import kotlcinx.coroutines.launch
import net.silkmc.silk.test.compose.util.rememberBitmapResource

private enum class WheelState {
    STATIC, ROLLING, STOPPING;
}

@Composable
fun RotatingWheel() {
    val rotatingWheel = rememberBitmapResource("/images/rotating_wheel.png")
    val staticWheel = rememberBitmapResource("/images/static_wheel.png")

    if (rotatingWheel != null && staticWheel != null) {
        var wheelState by remember { mutableStateOf(WheelState.STATIC) }

        val rotationAnimatable = remember { Animatable(0f) }

        val rotation = when (wheelState) {
            WheelState.STATIC -> rotationAnimatable.value
            WheelState.ROLLING -> {
                val transition = rememberInfiniteTransition()
                transition.animateFloat(
                    initialValue = rotationAnimatable.value, targetValue = rotationAnimatable.value + 360f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(700, easing = LinearEasing)
                    )
                ).value
            }
            WheelState.STOPPING -> {
                LaunchedEffect(Unit) {
                    rotationAnimatable.animateTo(
                        targetValue = rotationAnimatable.value + 360,
                        animationSpec = tween(700, easing = EaseOut)
                    )
                    wheelState = WheelState.STATIC
                }
                rotationAnimatable.value
            }
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box {
                when (wheelState) {
                    WheelState.STATIC -> {
                        Button(onClick = {
                            wheelState = WheelState.ROLLING
                        }) {
                            Text("Start rolling")
                        }
                    }
                    WheelState.ROLLING -> {
                        val stopScope = rememberCoroutineScope()
                        Button(onClick = {
                            stopScope.launch {
                                rotationAnimatable.snapTo(rotation)
                                wheelState = WheelState.STOPPING
                            }
                        }) {
                            Text("stop wheel")
                        }
                    }
                    else -> {
                        Button(onClick = {}) {
                            Text("currently stopping")
                        }
                    }
                }
            }

            Box {
                Image(
                    rotatingWheel, "rotating wheel",
                    modifier = Modifier.rotate(rotation % 360)
                )
                Image(staticWheel, "static wheel")
            }
        }
    }
}
