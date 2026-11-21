package com.trader.qa;

import java.io.File;
import java.text.Normalizer;
import java.util.regex.Pattern;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.Test;
import static org.junit.Assert.*;
import static com.trader.qa.ArabicDigits.ar;

/** Reads only the real Android files retained with native paths and SHA256. */
public class FinancialPdfBoundaryContentTest {
    private PDDocument open(String name) throws Exception {
        File file=new File(System.getProperty("pdf.fixture.dir"),name);
        assertTrue("Missing actual Android output: "+name,file.isFile());
        return Loader.loadPDF(file);
    }
    private String text(PDDocument pdf) throws Exception {
        assertTrue(pdf.getNumberOfPages()>=1);
        String text=ArabicDigits.stripBidiControls(Normalizer.normalize(new PDFTextStripper().getText(pdf),Normalizer.Form.NFKC));
        assertFalse(text.contains("NaN"));assertFalse(text.contains("Infinity"));return text;
    }
    private void contains(String text,String... values){
        for(String expected:values)assertTrue("Missing actual PDF content: "+expected+"; reverseFound="+text.contains(new StringBuilder(expected).reverse().toString())
            +"; extractedTail="+text.substring(Math.max(0,text.length()-180)),text.contains(expected));
    }
    private void refund(String text,String amount){
        String number=Pattern.quote(amount);
        assertTrue("Refund must be a currency deduction",Pattern.compile("(?:-\\h*₪\\h*"+number+"|-\\h*"+number+"\\h*₪|₪\\h*-\\h*"+number+"|₪\\h*"+number+"\\h*-|"+number+"\\h*-\\h*₪|"+number+"\\h*₪\\h*-)").matcher(text).find());
    }
    @Test public void paidManualBankInvoiceKeepsIdentityPaymentAndEmptyItemExplanation() throws Exception {
        try(var pdf=open("invoice-manual-bank.pdf")){
            String text=text(pdf);contains(text,"#"+ar("000301"),"MANUAL-CUSTOMER",ar("31.25"),"BOUNDARY-BANK","MANUAL-PAYMENT-END");
            // RTL extraction may reorder words; require the manual-item explanation's key words.
            contains(text,"يدوي","أصناف",ar("0.00"));assertFalse(text.contains("بعد الإرجاع"));
        }
    }
    @Test public void fullyReturnedInvoiceKeepsOriginalRefundAndZeroNetWithoutNegativeQuantity() throws Exception {
        try(var pdf=open("invoice-full-return.pdf")){
            String text=text(pdf);contains(text,"#"+ar("000302"),"FULL-RETURN-ITEM","FULL-RETURN-CUSTOMER","FULL-RETURN-END",ar("80.00"),ar("0.00"),"RETURN-CASH");
            refund(text,ar("80.00"));assertFalse("Fully returned quantity4 must not survive as a standalone printed quantity",
                Pattern.compile("(?<!["+ArabicDigits.NUMBER_CHARS+"])(?:4|\u0664)(?:[.\u066B][0\u0660]+)?(?!["+ArabicDigits.NUMBER_CHARS+"])").matcher(text).find());
        }
    }
    @Test public void fractionalReturnPreservesNetWeightRefundRoundingAndActualLogoImage() throws Exception {
        try(var pdf=open("invoice-fractional-logo.pdf")){
            String text=text(pdf);contains(text,"#"+ar("000303"),"FRACTIONAL-KG","FRACTIONAL-CUSTOMER","WEIGHT-WALLET","KG",ar("2.25"),ar("15.00"),ar("39.38"),ar("5.63"),ar("33.75"),ar("0.00"));
            refund(text,ar("5.63"));boolean image=false;
            for(var page:pdf.getPages())for(var name:page.getResources().getXObjectNames())
                if(page.getResources().getXObject(name) instanceof PDImageXObject)image=true;
            assertTrue("Provided logo must be embedded in actual PDF output",image);
        }
    }
    @Test public void emptyMonthKeepsZeroTotalsAndExplicitNoSalesMessage() throws Exception {
        try(var pdf=open("monthly-report-empty.pdf")){
            String text=text(pdf);contains(text,"EMPTY-MONTH-STORE",ar("0.00"),"لا","مبيعات","الشهر");
            assertFalse(text.contains("NaN"));
        }
    }
}
