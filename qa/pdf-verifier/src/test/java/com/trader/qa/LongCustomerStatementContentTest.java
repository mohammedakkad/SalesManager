package com.trader.qa;

import java.io.File;
import java.text.Normalizer;
import java.util.regex.Pattern;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.Test;
import static org.junit.Assert.*;

/** Only actual Android-generated input. This test never creates a substitute PDF. */
public class LongCustomerStatementContentTest {
    @Test public void completeStatementKeepsAllSixtyRowsAndUnchangedFinancialTotalsAcrossPages() throws Exception {
        File file=new File(System.getProperty("pdf.fixture.dir"),"customer-statement-long.pdf");
        assertTrue("Missing actual Android statement",file.isFile());
        try(var pdf=Loader.loadPDF(file)){
            String text=Normalizer.normalize(new PDFTextStripper().getText(pdf),Normalizer.Form.NFKC);
            assertTrue("A complete60-row statement must paginate; actualPages="+pdf.getNumberOfPages(),pdf.getNumberOfPages()>1);
            assertTrue(text.contains("LONG-STATEMENT-CUSTOMER"));
            for(int id=21001;id<=21060;id++)
                assertTrue("Missing customer-history row #"+id,Pattern.compile("#"+id+"(?![0-9])").matcher(text).find());
            for(String amount:new String[]{"2287.50","1162.50","1125"})
                assertTrue("Missing unchanged financial total "+amount,Pattern.compile("(?<![0-9.])"+Pattern.quote(amount)+"(?![0-9.])").matcher(text).find());
            assertFalse(text.contains("NaN"));assertFalse(text.contains("Infinity"));
        }
    }
}
