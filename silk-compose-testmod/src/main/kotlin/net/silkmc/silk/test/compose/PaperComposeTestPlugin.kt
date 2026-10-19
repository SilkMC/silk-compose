package net.silkmc.silk.test.compose

import net.silkmc.silk.test.compose.commands.composeCommand
import org.bukkit.plugin.java.JavaPlugin

class PaperComposeTestPlugin : JavaPlugin() {
    override fun onEnable() {
        composeCommand
    }
}
