package com.trader.salesmanager.ui.coverage

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Text
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.*
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.text.AnnotatedString
import com.trader.salesmanager.ui.components.*
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class SharedComponentTransitionsUiTest:OfflineSellerUiTest() {
    @Test fun paidStatusChangesBothWaysWithoutLeavingTheOldLabelOnScreen(){
        val paid=mutableStateOf(false)
        show{StatusChip(paid.value)}
        ui.onNodeWithText("غير مدفوع").assertExists()
        ui.runOnIdle{paid.value=true}
        ui.onNodeWithText("مدفوع").assertExists();ui.onNodeWithText("غير مدفوع").assertDoesNotExist()
        ui.runOnIdle{paid.value=false}
        ui.onNodeWithText("غير مدفوع").assertExists();ui.onNodeWithText("مدفوع").assertDoesNotExist()
    }
    @Test fun animatedMoneyCounterSettlesOnNewPositiveNegativeAndZeroAmounts(){
        val value=mutableStateOf(1250.25)
        show{AnimatedCounter(value.value)}
        ui.onNodeWithText("1,250.25").assertExists()
        ui.runOnIdle{value.value=-75.5};ui.mainClock.advanceTimeBy(6000)
        ui.onNodeWithText("-75.50").assertExists();ui.onNodeWithText("1,250.25").assertDoesNotExist()
        ui.runOnIdle{value.value=0.0};ui.mainClock.advanceTimeBy(6000)
        ui.onNodeWithText("0.00").assertExists()
    }
    @Test fun settledMoneyCounterPreservesCentsBeyondFloatPrecision(){
        val value=mutableStateOf(16777217.25)
        show{AnimatedCounter(value.value)}
        ui.onNodeWithText("16,777,217.25").assertExists()
        ui.runOnIdle{value.value=16777218.75};ui.mainClock.advanceTimeBy(6000)
        ui.onNodeWithText("16,777,218.75").assertExists()
    }
    @Test fun finiteDoubleAmountsOutsideFloatRangeDoNotBecomeInfinity(){
        val amount=1.0e40
        show{AnimatedCounter(amount)}
        ui.onNodeWithText(java.lang.String.format(java.util.Locale.US,"%,.2f",amount)).assertExists()
        ui.onNodeWithText("Infinity").assertDoesNotExist()
    }
    @Test fun countFormattingChangesPrecisionAndCurrencyAffixesWithItsInputs(){
        val precision=mutableStateOf(0);val prefix=mutableStateOf("₪ ");val suffix=mutableStateOf("")
        show{AnimatedCounter(1234.5,decimals=precision.value,prefix=prefix.value,suffix=suffix.value)}
        ui.onNodeWithText("₪ 1,235").assertExists()
        ui.runOnIdle{precision.value=2;prefix.value="";suffix.value=" شيكل"}
        ui.onNodeWithText("1,234.50 شيكل").assertExists();ui.onNodeWithText("₪ 1,235").assertDoesNotExist()
    }
    @Test fun searchEditsUseTheCurrentCallbackAndClearingRemovesTheQuery(){
        val query=mutableStateOf("");val received=mutableListOf<String>()
        show{ModernSearchBar(query.value,{query.value=it;received+=it})}
        ui.onNode(hasSetTextAction()).performTextInput("قهوة 42")
        ui.onNode(hasSetTextAction()).assertTextContains("قهوة 42")
        ui.onNodeWithContentDescription("مسح البحث").performClick()
        ui.onNode(hasSetTextAction()).assert(SemanticsMatcher.expectValue(SemanticsProperties.EditableText,AnnotatedString("")))
        assertEquals("",received.last())
        ui.mainClock.advanceTimeBy(1000)
        ui.onNodeWithContentDescription("مسح البحث").assertDoesNotExist()
    }
    @Test fun emptySearchHasNoClearActionAndShowsTheProvidedHint(){
        val query=mutableStateOf("")
        show{ModernSearchBar(query.value,{query.value=it},placeholder="اسم أو هاتف")}
        ui.onNodeWithText("اسم أو هاتف").assertExists()
        ui.onNodeWithContentDescription("مسح البحث").assertDoesNotExist()
        ui.onNode(hasSetTextAction()).performTextInput("123")
        ui.onNodeWithContentDescription("مسح البحث").assertExists()
    }
    @Test fun clearingAfterParentChangesTheQueryUsesTheLatestVisibleValue(){
        val query=mutableStateOf("قديم");val received=mutableListOf<String>()
        show{Column{Text("الاستعلام:${query.value}");ModernSearchBar(query.value,{query.value=it;received+=it},placeholder="بحث الزبائن")}}
        ui.runOnIdle{query.value="جديد"}
        ui.onNodeWithText("الاستعلام:جديد").assertExists()
        ui.onNodeWithContentDescription("مسح البحث").performClick()
        ui.onNodeWithText("الاستعلام:").assertExists();assertEquals(listOf(""),received)
    }
}
