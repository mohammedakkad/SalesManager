package com.trader.testing.recomposition

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Every clickable node needs a spoken label and a touch target of at least 48 x 48 dp. */
object ClickableAccessibility {
    fun violations(ui: ComposeTestRule, minTouch: Dp = 48.dp): List<String> {
        val nodes = ui.onAllNodes(hasClickAction()).fetchSemanticsNodes()
        // Nodes scrolled out of the viewport have empty bounds; they are checked where they are visible.
        return nodes.filter { it.boundsInRoot.width > 0f && it.boundsInRoot.height > 0f }.flatMap { node ->
            val c = node.config
            val label = listOfNotNull(
                c.getOrNull(SemanticsProperties.ContentDescription)?.joinToString(),
                c.getOrNull(SemanticsProperties.Text)?.joinToString(),
                c.getOrNull(SemanticsProperties.EditableText)?.text,
                c.getOrNull(SemanticsProperties.StateDescription)
            ).joinToString().trim()
            val density = node.layoutInfo.density
            val touch = node.touchBoundsInRoot
            val w = with(density) { touch.width.toDp() }; val h = with(density) { touch.height.toDp() }
            listOfNotNull(
                if (label.isEmpty()) "node ${node.id} at ${node.boundsInRoot} has no label" else null,
                if (w < minTouch - 0.5.dp || h < minTouch - 0.5.dp) "'$label' touch target ${w.value.toInt()}x${h.value.toInt()}dp < ${minTouch.value.toInt()}dp" else null
            )
        }
    }
}
