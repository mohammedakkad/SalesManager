package com.trader.salesmanager.ui.recomposition

import androidx.compose.runtime.Composable
import com.trader.salesmanager.ui.coverage.AppThemeUiFixture
import com.trader.testing.recomposition.RecompositionMeter
import com.trader.testing.recomposition.RecompositionTracker
import org.junit.After

/** Seller screens with the real app theme, measured by the shared [RecompositionMeter]. */
abstract class RecompositionAuditFixture : AppThemeUiFixture() {
    private val meter by lazy { RecompositionMeter(ui) }

    @After fun stopTracker() = RecompositionTracker.stop()

    protected fun measure(name: String, interaction: (() -> Unit)? = RecompositionMeter.DEFAULT, content: @Composable () -> Unit) =
        meter.measure(name, { showApp { content() } }, interaction)

    protected fun open(content: @Composable () -> Unit) = meter.open { showApp { content() } }
    protected fun idle() = meter.idle()
    protected fun after(change: () -> Unit) = meter.after(change)
}
