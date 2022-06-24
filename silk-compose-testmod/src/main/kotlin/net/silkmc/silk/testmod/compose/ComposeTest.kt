package net.silkmc.silk.test.compose

import androidx.compose.foundation.layout.Column
import net.silkmc.silk.compose.displayComposable
import net.silkmc.silk.compose.ui.McWindowHeader
import net.silkmc.silk.test.commands.testCommand
import net.silkmc.silk.test.compose.game.FallingBallsGameComposable
import net.silkmc.silk.test.compose.guis.ScrollTestComposable
import net.silkmc.silk.test.compose.guis.GeneralTestComposable

val composeCommand = testCommand("compose") {
    literal("general_test") runs {
        source.playerOrException.displayComposable(4, 3) {
            Column {
                McWindowHeader(it, "Test Window")
                GeneralTestComposable()
            }
        }
    }

    literal("falling_balls") runs {
        source.playerOrException.displayComposable(8, 6) {
            McWindowHeader(it)
            FallingBallsGameComposable()
        }
    }

    literal("scroll") runs {
        source.playerOrException.displayComposable(5, 4) {
            ScrollTestComposable()
        }
    }
}
