package com.trader.salesmanager.util.export

import com.trader.core.domain.model.*
import com.trader.salesmanager.ui.reports.*
import org.junit.*
import org.junit.Assert.*
import org.w3c.dom.Document
import java.io.File
import java.nio.file.Files
import java.util.zip.ZipFile
import javax.xml.parsers.DocumentBuilderFactory

@org.junit.runner.RunWith(org.robolectric.RobolectricTestRunner::class)
@org.robolectric.annotation.Config(sdk=[28],manifest=org.robolectric.annotation.Config.NONE,application=android.app.Application::class)
class WorkbookExportIntegrityTest {
    private val dir=Files.createTempDirectory("seller-workbook-").toFile()
    @After fun close() { dir.deleteRecursively() }
    private fun xml(file:File,entry:String):Document=ZipFile(file).use { zip ->
        val factory=DocumentBuilderFactory.newInstance();factory.isNamespaceAware=true
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl",true)
        zip.getInputStream(zip.getEntry(entry)).use { factory.newDocumentBuilder().parse(it) }
    }
    private fun rows(file:File):List<List<String>> {
        val shared=xml(file,"xl/sharedStrings.xml").getElementsByTagName("t")
        val pool=(0 until shared.length).map { shared.item(it).textContent }
        val sheet=xml(file,"xl/worksheets/sheet1.xml");assertEquals(0,sheet.getElementsByTagName("f").length)
        val rows=sheet.getElementsByTagName("row")
        return (0 until rows.length).map { i ->
            val cells=(rows.item(i) as org.w3c.dom.Element).getElementsByTagName("c")
            (0 until cells.length).map { j ->
                val c=cells.item(j) as org.w3c.dom.Element
                val values=c.getElementsByTagName("v")
                if(values.length==0) "" else if(c.getAttribute("t")=="s") {
                    val index=values.item(0).textContent.toInt()
                    assertTrue("String index $index outside serialized shared-string table",index in pool.indices)
                    pool[index]
                } else values.item(0).textContent
            }
        }
    }
    @Test fun workbookRoundTripsArabicEscapingAndLiteralFormulaText() {
        val writer=XlsxWriter();val style=XlsxWriter.CellStyle(wrapText=true)
        val text="متجر & <نقد> \"آمن\"";writer.addRow(text to style,"=2+2" to style,12.5 to style,null to style,true to style)
        val file=File(dir,"roundtrip.xlsx");writer.write(file)
        assertEquals(listOf(text,"=2+2","12.5","","true"),rows(file).single())
        writer.addRow("لاحق" to style);writer.write(file)
        assertEquals("لاحق",rows(file).last().first())
    }
    @Test fun styleCountsAndFillReferencesMatchActualXmlElements() {
        val writer=XlsxWriter();val color="FF10B981"
        writer.addRow("أ" to XlsxWriter.CellStyle(bold=true,bgColor=color),"ب" to XlsxWriter.CellStyle(bgColor=color))
        val file=File(dir,"styles.xlsx");writer.write(file)
        val styles=xml(file,"xl/styles.xml");val fills=styles.getElementsByTagName("fills").item(0) as org.w3c.dom.Element
        val items=fills.getElementsByTagName("fill")
        assertEquals(items.length,fills.getAttribute("count").toInt())
        val xfs=(styles.getElementsByTagName("cellXfs").item(0) as org.w3c.dom.Element).getElementsByTagName("xf")
        for(i in 1 until xfs.length) {
            val id=(xfs.item(i) as org.w3c.dom.Element).getAttribute("fillId").toInt()
            assertTrue("Solid fills cannot reference reserved gray125",id>=2)
            val foreground=(items.item(id) as org.w3c.dom.Element).getElementsByTagName("fgColor").item(0) as org.w3c.dom.Element
            assertEquals(color,foreground.getAttribute("rgb"))
        }
    }
    @Test fun salesWorkbookContainsCorrectPaidDebtAndCustomerIdentity() {
        val tx=listOf(Transaction(1,1,"أحمد & شركاؤه",100.0,isPaid=true,paymentMethodName="نقد"),Transaction(2,2,"زبون دين",200.0,isPaid=false))
        val file=ExportManager.generateSalesReportExcel(dir,tx,"الشهر","متجر",listOf(DaySalesEntry("01/01",300.0,100.0)),listOf(CustomerRank("أحمد & شركاؤه",100.0)),listOf(PaymentShare("نقد",100.0)))
        val data=rows(file)
        assertEquals("300.0",data.first { "الإجمالي" in it }.last())
        assertEquals("100.0",data.first { "مدفوع" in it }.last())
        assertEquals("200.0",data.first { "غير مدفوع" in it }.last())
        assertTrue(data.flatten().contains("أحمد & شركاؤه"));assertTrue(data.flatten().contains("المبيعات اليومية"))
    }
    @Test fun inventoryWorkbookPreservesQuantitiesValuesAndStockStatus() {
        val units=listOf(0.0,1.0,10.0).mapIndexed { i,q -> ProductUnit(id="u$i",productId="p",unitLabel="حبة$i",price=25.0,quantityInStock=q,lowStockThreshold=2.0) }
        val file=ExportManager.generateInventoryExcel(dir,listOf(ProductWithUnits(Product(id="p",name="أرز",merchantId="fixture"),units)),"متجر")
        val data=rows(file)
        for(status in listOf("نفد","منخفض","متوفر")) assertTrue(data.flatten().contains(status))
        assertEquals("275.0",data.first { "إجمالي قيمة المخزون" in it }.last())
    }
}
