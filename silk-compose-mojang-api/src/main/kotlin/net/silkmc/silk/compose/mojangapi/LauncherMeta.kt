package net.silkmc.silk.compose.mojangapi

import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import java.io.File
import java.net.URL
import java.nio.channels.Channels

object LauncherMeta {

    fun downloadClientTo(file: File, serverVersion: String, logInfo: (String) -> Unit = {}) {
        val json = Json { ignoreUnknownKeys = true }

        // request version info
        logInfo("Requesting version info for version ${serverVersion}...")
        val versionInfoUrl = json.decodeFromString<VersionManifest>(URL("https://launchermeta.mojang.com/mc/game/version_manifest.json").readText())
            .versions.find { it.id == serverVersion }?.url
        val clientDownloadUrl = json.decodeFromString<VersionInfo>(URL(versionInfoUrl).readText())
            .downloads.client.url
        logInfo("Found version info for $serverVersion")

        // download client
        logInfo("Downloading ${file.name}...")
        file.outputStream().channel.use { channel ->
            Channels.newChannel(URL(clientDownloadUrl).openStream()).use { urlChannel ->
                channel.transferFrom(urlChannel, 0, Long.MAX_VALUE)
            }
        }
        logInfo("Finished downloading the client")
    }

    @Serializable
    private data class VersionManifest(val versions: List<Link>) {
        @Serializable
        data class Link(val id: String, val url: String)
    }

    @Serializable
    private data class VersionInfo(val downloads: Downloads) {
        @Serializable
        data class Downloads(val client: Client) {
            @Serializable
            data class Client(val url: String)
        }
    }
}
