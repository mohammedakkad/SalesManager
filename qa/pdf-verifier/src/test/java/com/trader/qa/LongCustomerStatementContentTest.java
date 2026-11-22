package com.trader.qa;

import java.io.File;
import java.text.Normalizer;
import java.util.regex.Pattern;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.Test;
import static org.junit.Assert.*;
import static com.trader.qa.ArabicDigits.ar;

/** Only actual Android-generated input. This test never creates a substitute PDF. */
public class LongCustomerStatementContentTest {
    @Test public void completeStatementKeepsAllSixtyRowsAndUnchangedFinancialTotalsAcrossPages() throws Exception {
        File file=new File(System.getProperty("pdf.fixture.dir"),"customer-statement-long.pdf");
        assertTrue("Missing actual Android statement",file.isFile());
        try(var pdf=Loader.loadPDF(file)){
            String text=ArabicDigits.stripBidiControls(Normalizer.normalize(new PDFTextStripper().getText(pdf),Normalizer.Form.NFKC));
            assertTrue("A complete60-row statement must paginate; actualPages="+pdf.getNumberOfPages(),pdf.getNumberOfPages()>1);
            assertTrue(text.contains("LONG-STATEMENT-CUSTOMER"));
            for(int id=21001;id<=21060;id++)
                assertTrue("Missing customer-history row #"+id,Pattern.compile(Pattern.quote("#"+ar(String.valueOf(id)))+"(?!["+ArabicDigits.NUMBER_CHARS+"])").matcher(text).find());
            for(String amount:new String[]{ar("2,287.50"),ar("1,162.50"),ar("1,125.00")})
                assertTrue("Missing unchanged financial total "+amount,Pattern.compile("(?<!["+ArabicDigits.NUMBER_CHARS+"])"+Pattern.quote(amount)+"(?!["+ArabicDigits.NUMBER_CHARS+"])").matcher(text).find());
            assertFalse(text.contains("NaN"));assertFalse(text.contains("Infinity"));
        }
    }
}
