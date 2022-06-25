package net.silkmc.silk.test.compose

import net.fabricmc.api.ModInitializer
import net.silkmc.silk.test.compose.commands.composeCommand

class ComposeTestmod : ModInitializer {
    override fun onInitialize() {
        composeCommand
    }
}
