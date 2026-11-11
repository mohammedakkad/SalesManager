package probe;
import android.app.Application;
import android.graphics.pdf.PdfDocument;
import android.graphics.Paint;
import java.io.ByteArrayOutputStream;
import org.junit.Test;
import static org.junit.Assert.*;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.GraphicsMode;
@RunWith(RobolectricTestRunner.class)
@Config(sdk=28, manifest=Config.NONE, application=Application.class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
public class ActualPdfRuntimeTest {
    @Test public void realPdfDocumentProducesActualPdfBytes() throws Exception {
        PdfDocument document = new PdfDocument();
        try {
            PdfDocument.Page page = document.startPage(new PdfDocument.PageInfo.Builder(300,400,1).create());
            page.getCanvas().drawText("ACTUAL-RUNTIME-123.45", 20, 30, new Paint());
            document.finishPage(page);
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            document.writeTo(output);
            assertTrue(output.size() > 10);
            assertEquals("%PDF-",new String(output.toByteArray(),0,5,java.nio.charset.StandardCharsets.US_ASCII));
        } finally { document.close(); }
    }
}
