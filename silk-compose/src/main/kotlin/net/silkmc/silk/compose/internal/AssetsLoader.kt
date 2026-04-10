package net.silkmc.silk.compose.internal

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.decodeToImageBitmap
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.minecraft.SharedConstants
import net.silkmc.silk.compose.icons.McIcon
import net.silkmc.silk.compose.mojangapi.LauncherMeta
import net.silkmc.silk.core.Silk
import net.silkmc.silk.core.logging.logError
import net.silkmc.silk.core.logging.logInfo
import net.silkmc.silk.core.task.silkCoroutineScope
import java.nio.file.Files
import java.nio.file.NoSuchFileException
import kotlin.io.path.*

@OptIn(ExperimentalPathApi::class)
object AssetsLoader {
    private val assetsStorage = (Silk.server?.serverDirectory ?: Path(".")).resolve("server-assets")
    private val assetsPath = assetsStorage.resolve("assets")

    private val loadedAssets = CompletableDeferred<Boolean>()

    init {
        silkCoroutineScope.launch(Dispatchers.IO) {
            val serverVersion = SharedConstants.getCurrentVersion().name()
            val versionFile = assetsStorage.resolve("version.txt")

            if (
                (if (versionFile.exists()) versionFile.readText() != serverVersion else true) || !assetsStorage.resolve(
                    "assets"
                ).exists()
            ) {
                val clientFile = assetsStorage.resolve("client-${serverVersion}.jar")
                clientFile.parent.createDirectories()
                if (clientFile.notExists()) {
                    clientFile.createFile()
                }
                LauncherMeta.downloadClientTo(clientFile.toFile(), serverVersion, ::logInfo)

                // extract assets
                logInfo("Extracting assets...")
                assetsStorage.resolve("assets").deleteRecursively()
                Files.walk(clientFile).forEach { assetPath ->
                    if ((assetPath.isDirectory()) || assetPath.fileName.toString().endsWith("png"))
                        Files.copy(assetPath, assetsPath.resolve(assetPath.pathString))

                }
                clientFile.deleteIfExists()
                logInfo("Finished extracting assets")

                // save downloaded version
                versionFile.createFile()
                versionFile.writeText(serverVersion)
            }

            loadedAssets.complete(true)
        }
    }

    suspend fun loadImage(icon: McIcon): ImageBitmap? {
        loadedAssets.await()

        return withContext(Dispatchers.IO) {
            try {
                assetsPath.resolve("minecraft/textures/${icon}").inputStream().readAllBytes().decodeToImageBitmap()
            } catch (ignored: NoSuchFileException) {
                logError("Cannot load or find image file for given icon: $icon")
                null
            }
        }
    }
}
