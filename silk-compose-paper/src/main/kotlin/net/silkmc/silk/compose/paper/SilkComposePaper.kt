package net.silkmc.silk.compose.paper

import net.silkmc.silk.compose.impl.ItemFrameMapsComposeGui
import net.silkmc.silk.core.annotations.InternalSilkApi
import net.silkmc.silk.paper.conversions.mcPlayer
import net.silkmc.silk.paper.events.listen
import org.jetbrains.kotlinx.multik.api.mk
import org.bukkit.event.player.PlayerAnimationEvent
import org.bukkit.event.player.PlayerAnimationType
import org.bukkit.event.player.PlayerItemHeldEvent
import org.bukkit.plugin.java.JavaPlugin

/**
 * Replaces the fabric packet listener mixin with bukkit events.
 */
@OptIn(InternalSilkApi::class)
class SilkComposePaper : JavaPlugin() {
    override fun onEnable() {
        // multik finds its engine via ServiceLoader with the context classloader,
        // which can't see this plugin on paper; the engine is cached after the first lookup
        val thread = Thread.currentThread()
        val previousClassLoader = thread.contextClassLoader
        thread.contextClassLoader = javaClass.classLoader
        try {
            mk.linalg
        } finally {
            thread.contextClassLoader = previousClassLoader
        }

        listen<PlayerAnimationEvent>(this) {
            if (it.animationType == PlayerAnimationType.ARM_SWING) {
                ItemFrameMapsComposeGui.onSwingHand(it.player.mcPlayer)
            }
        }

        listen<PlayerItemHeldEvent>(this) {
            if (ItemFrameMapsComposeGui.onUpdateSelectedSlot(it.player.mcPlayer, it.newSlot)) {
                it.isCancelled = true
            }
        }
    }
}
