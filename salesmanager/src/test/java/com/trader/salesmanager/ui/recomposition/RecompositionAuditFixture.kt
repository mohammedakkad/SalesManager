package com.trader.salesmanager.ui.recomposition

import androidx.compose.runtime.Composable
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.trader.salesmanager.ui.coverage.AppThemeUiFixture
import com.trader.testing.recomposition.RecompositionTracker
import org.junit.After
import java.io.File

/**
 * Layout Inspector substitute: opens a screen with the real theme, then counts composable bodies
 * that ran (a) while opening (2 s of frames), (b) during 10 s of idle frames and (c) for one
 * interaction. The clock is driven manually so infinite animations really run, exactly as on a
 * device. App composables are those in `com.trader`; "total" also includes library internals.
 */
abstract class RecompositionAuditFixture : AppThemeUiFixture() {
    data class Phase(val total: Int, val app: Int, val hot: Map<String, Int>, val appCounts: Map<String, Int>)
    data class Measurement(val open: Phase, val idle: Phase, val interaction: Phase?)

    private val stage = System.getenv("RECOMPOSITION_STAGE") ?: "current"

    @After fun stopTracker() = RecompositionTracker.stop()

    private fun phase(): Phase {
        val all = RecompositionTracker.snapshot()
        val app = all.filterKeys { it.startsWith("com.trader") }
        return Phase(all.values.sum(), app.values.sum(), app.entries.sortedByDescending { it.value }.take(6).associate { it.toPair() }, app)
    }

    protected fun measure(name: String, interaction: (() -> Unit)? = DEFAULT, content: @Composable () -> Unit): Measurement {
        ui.mainClock.autoAdvance = false
        RecompositionTracker.start()
        showApp { content() }
        ui.mainClock.advanceTimeBy(2_000)
        val open = phase()
        RecompositionTracker.reset()
        repeat(10) { ui.mainClock.advanceTimeBy(1_000) }
        val idle = phase()
        val act = interaction?.let {
            RecompositionTracker.reset()
            if (it === DEFAULT) defaultInteraction() else it()
            ui.mainClock.advanceTimeBy(2_000)
            phase()
        }
        RecompositionTracker.stop()
        return Measurement(open, idle, act).also { write(name, it) }
    }

    /** Opens [content] with a manually driven clock and the tracker running (for guard tests). */
    protected fun open(content: @Composable () -> Unit) {
        ui.mainClock.autoAdvance = false
        RecompositionTracker.start()
        showApp { content() }
        ui.mainClock.advanceTimeBy(2_000)
    }

    /** Recompositions during 10 s without any input. */
    protected fun idle(): Phase {
        RecompositionTracker.reset()
        repeat(10) { ui.mainClock.advanceTimeBy(1_000) }
        return phase()
    }

    /** Recompositions caused by [change] (applied on the UI thread) once frames have settled. */
    protected fun after(change: () -> Unit): Phase {
        RecompositionTracker.reset()
        ui.runOnIdle { change() }
        ui.mainClock.advanceTimeBy(2_000)
        return phase()
    }

    /** Type into the first text field, otherwise click the first clickable node. */
    private fun defaultInteraction() {
        val field = ui.onAllNodes(hasSetTextAction()).fetchSemanticsNodes()
        if (field.isNotEmpty()) ui.onAllNodes(hasSetTextAction())[0].performTextInput("1")
        else ui.onAllNodes(hasClickAction())[0].performClick()
    }


    private fun write(name: String, m: Measurement) {
        val dir = File("build/recomposition/$stage").apply { mkdirs() }
        fun Phase?.cells() = if (this == null) "-\t-" else "$total\t$app"
        File(dir, "$name.tsv").writeText(
            "$name\t${m.open.cells()}\t${m.idle.cells()}\t${m.interaction.cells()}\t" +
                "idle:${m.idle.hot}\tinteraction:${m.interaction?.hot}\n"
        )
        println("RECOMPOSITION $name open=${m.open.total}/${m.open.app} idle=${m.idle.total}/${m.idle.app} " +
            "interaction=${m.interaction?.total}/${m.interaction?.app} idleHot=${m.idle.hot}")
    }

    companion object { val DEFAULT: () -> Unit = {} }
}
