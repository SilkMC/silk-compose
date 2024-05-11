package net.silkmc.silk.test.compose.guis

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import net.silkmc.silk.compose.icons.McIcon
import net.silkmc.silk.compose.icons.McIcons
import net.silkmc.silk.compose.ui.McImage
import kotlin.reflect.full.declaredMemberProperties
import kotlin.reflect.typeOf

@Composable
fun ScrollTestComposable() {
    val icons = McIcons.Item::class.declaredMemberProperties
        .filter { it.isConst && it.returnType == typeOf<McIcon>() }
        .map { it.getter.call() as McIcon }

    LazyColumn {
        items(icons) { icon ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                McImage(icon, modifier = Modifier.size(50.dp))
                Spacer(Modifier.width(10.dp))
                Text(icon)
            }
        }
    }
}
