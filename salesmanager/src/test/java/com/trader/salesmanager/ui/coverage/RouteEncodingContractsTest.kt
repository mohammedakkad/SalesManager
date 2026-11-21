package com.trader.salesmanager.ui.coverage

import android.app.Application
import android.net.Uri
import com.trader.salesmanager.ui.navigation.Screen
import org.junit.Test
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[28],manifest=Config.NONE,application=Application::class)
class RouteEncodingContractsTest {
    @Test fun barcodeQueryCannotBeTruncatedOrBecomeAnotherQueryParameter() {
        val barcode="SKU+50% /?&customerId=99#tail"
        val uri=Uri.parse("https://offline.invalid/"+Screen.AddProduct.createRoute(barcode))
        assertEquals(barcode,uri.getQueryParameter("barcode"));assertNull(uri.getQueryParameter("customerId"));assertNull(uri.fragment)
    }
    @Test fun missingBarcodeRetainsTheExistingEmptyQueryContract() {
        assertEquals("",Uri.parse("https://offline.invalid/"+Screen.AddProduct.createRoute()).getQueryParameter("barcode"))
    }
    @Test fun productDetailIdCannotBecomeAQueryOrFragment() {
        val id="sku+50% ?#tail";val uri=Uri.parse("https://offline.invalid/"+Screen.ProductDetail.createRoute(id))
        assertEquals(listOf("product_detail",id),uri.pathSegments);assertNull(uri.query);assertNull(uri.fragment)
    }
    @Test fun productEditorIdRoundTripsExactlyOnce() {
        val id="sku+50% ?#tail";val uri=Uri.parse("https://offline.invalid/"+Screen.EditProduct.createRoute(id))
        assertEquals(listOf("edit_product",id),uri.pathSegments);assertNull(uri.query);assertNull(uri.fragment)
    }
}
