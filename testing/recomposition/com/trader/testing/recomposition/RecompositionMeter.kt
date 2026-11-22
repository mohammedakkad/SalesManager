package com.trader.testing.recomposition

import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import java.io.File

/**
 * Layout Inspector substitute for JVM tests: opens content with a manually driven clock (so
 * infinite animations really run, as on a device) and counts composable bodies that ran while
 * opening (2 s), during 10 s of idle frames, and for one interaction. "app" counts only
 * composables under [appPrefix]; "total" also includes library internals.
 */
class RecompositionMeter(private val ui: ComposeTestRule, private val appPrefix: String = "com.trader") {
    data class Phase(val total: Int, val app: Int, val hot: Map<String, Int>, val appCounts: Map<String, Int>)
    data class Measurement(val open: Phase, val idle: Phase, val interaction: Phase?)

    fun phase(): Phase {
        val all = RecompositionTracker.snapshot()
        val app = all.filterKeys { it.startsWith(appPrefix) }
        return Phase(all.values.sum(), app.values.sum(), app.entries.sortedByDescending { it.value }.take(6).associate { it.toPair() }, app)
    }

    /** Starts the tracker, shows content via [show] and lets 2 s of frames settle. */
    fun open(show: () -> Unit) {
        ui.mainClock.autoAdvance = false
        RecompositionTracker.start()
        show()
        ui.mainClock.advanceTimeBy(2_000)
    }

    fun idle(): Phase {
        RecompositionTracker.reset()
        repeat(10) { ui.mainClock.advanceTimeBy(1_000) }
        return phase()
    }

    /** Recompositions caused by [change] (applied on the UI thread) once frames have settled. */
    fun after(change: () -> Unit): Phase {
        RecompositionTracker.reset()
        ui.runOnIdle { change() }
        ui.mainClock.advanceTimeBy(2_000)
        return phase()
    }

    fun measure(name: String, show: () -> Unit, interaction: (() -> Unit)? = DEFAULT): Measurement {
        open(show)
        val openPhase = phase()
        val idlePhase = idle()
        val act = interaction?.let {
            RecompositionTracker.reset()
            if (it === DEFAULT) defaultInteraction() else it()
            ui.mainClock.advanceTimeBy(2_000)
            phase()
        }
        RecompositionTracker.stop()
        return Measurement(openPhase, idlePhase, act).also { write(name, it) }
    }

    /** Type into the first text field, otherwise click the first clickable node. */
    private fun defaultInteraction() {
        if (ui.onAllNodes(hasSetTextAction()).fetchSemanticsNodes().isNotEmpty()) ui.onAllNodes(hasSetTextAction())[0].performTextInput("1")
        else ui.onAllNodes(hasClickAction())[0].performClick()
    }

    private fun write(name: String, m: Measurement) {
        val dir = File("build/recomposition/${System.getenv("RECOMPOSITION_STAGE") ?: "current"}").apply { mkdirs() }
        fun Phase?.cells() = if (this == null) "-\t-" else "$total\t$app"
        File(dir, "$name.tsv").writeText("$name\t${m.open.cells()}\t${m.idle.cells()}\t${m.interaction.cells()}\t" +
            "idle:${m.idle.hot}\tinteraction:${m.interaction?.hot}\n")
        println("RECOMPOSITION $name open=${m.open.total}/${m.open.app} idle=${m.idle.total}/${m.idle.app} " +
            "interaction=${m.interaction?.total}/${m.interaction?.app} idleHot=${m.idle.hot}")
    }

    companion object { val DEFAULT: () -> Unit = {} }
}
