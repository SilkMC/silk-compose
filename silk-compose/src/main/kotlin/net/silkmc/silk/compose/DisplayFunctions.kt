package net.silkmc.silk.compose

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import net.minecraft.core.BlockPos
import net.minecraft.server.level.ServerPlayer
import net.silkmc.silk.compose.impl.ItemFrameMapsComposeGui


/**
 * Creates a new server-side [MinecraftComposeGui]. This allows you to use any
 * composable functions inside the [content] lambda of this function.
 *
 * @param blockWidth the width of the gui **in blocks**
 * @param blockHeight the height of the gui **in blocks**
 * @param position the position where the gui will be created in-game
 * @param backgroundColor the background color of the gui, can be transparent as well
 * @param content define your gui using composable functions in here
 *
 * @see MinecraftComposeGui
 */
fun ServerPlayer.displayComposable(
    blockWidth: Int, blockHeight: Int,
    position: BlockPos = blockPosition().relative(direction, 2),
    backgroundColor: Color = Color.White,
    content: @Composable (gui: MinecraftComposeGui) -> Unit,
): MinecraftComposeGui {
    return ItemFrameMapsComposeGui(
        content,
        backgroundColor,
        blockWidth = blockWidth, blockHeight = blockHeight,
        player = this,
        position = position,
    )
}
