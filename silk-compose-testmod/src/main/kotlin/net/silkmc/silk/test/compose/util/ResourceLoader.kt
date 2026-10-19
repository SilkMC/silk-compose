package net.silkmc.silk.test.compose.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.produceState
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.decodeToImageBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.silkmc.silk.core.logging.logInfo
import org.jetbrains.compose.resources.decodeToImageBitmap
import java.io.InputStream

// resources must be loaded via a class of this mod, on paper each plugin has its own classloader
private object ResourceAnchor

@Composable
fun rememberBitmapResource(path: String): ImageBitmap? {
    return produceState<ImageBitmap?>(null) {
        value = withContext(Dispatchers.IO) {
            logInfo("Loading $path resource as image bitmap")
            ResourceAnchor::class.java.getResourceAsStream(path)!!.buffered().readAllBytes().decodeToImageBitmap()
        }
    }.value
}
