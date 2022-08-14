package net.silkmc.silk.test.compose.commands

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import net.silkmc.silk.commands.command
import net.silkmc.silk.compose.displayComposable
import net.silkmc.silk.compose.ui.McWindowHeader
import net.silkmc.silk.test.compose.game.FallingBallsGameComposable
import net.silkmc.silk.test.compose.guis.GeneralTestComposable
import net.silkmc.silk.test.compose.guis.RotatingWheel
import net.silkmc.silk.test.compose.guis.ScrollTestComposable

val composeCommand = command("compose") {
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

    literal("wheel") runs {
        source.playerOrException.displayComposable(
            4, 4,
            backgroundColor = Color.White.copy(0f)
        ) {
            RotatingWheel()
        }
    }
}
