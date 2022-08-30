package net.silkmc.silk.test.compose.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.produceState
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.res.loadImageBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.silkmc.silk.core.logging.logInfo

@Composable
fun rememberBitmapResource(path: String): ImageBitmap? {
    return produceState<ImageBitmap?>(null) {
        value = withContext(Dispatchers.IO) {
            logInfo("Loading $path resource as image bitmap")
            loadImageBitmap(Unit::class.java.getResourceAsStream(path)!!.buffered())
        }
    }.value
}
