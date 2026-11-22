package com.trader.salesmanager.ui.coverage

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.width
import com.trader.core.ui.WideScreenContainer
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
class WideScreenContainerTest : OfflineSellerUiTest() {
    private fun content() = show { WideScreenContainer { Box(Modifier.fillMaxSize().testTag("content")) } }

    @Config(qualifiers = "ar-w360dp-h800dp") @Test fun phoneUsesTheFullWidth() {
        content()
        assertEquals(ui.onRoot().getBoundsInRoot().width, ui.onNodeWithTag("content").getBoundsInRoot().width)
    }

    @Config(qualifiers = "ar-w1280dp-h800dp") @Test fun wideWindowIsACentredColumnOf840dp() {
        content()
        val root = ui.onRoot().getBoundsInRoot(); val box = ui.onNodeWithTag("content").getBoundsInRoot()
        assertEquals(840.dp, box.width)
        assertEquals("centred", (root.right - box.right).value, (box.left - root.left).value, 0.5f)
    }
}
