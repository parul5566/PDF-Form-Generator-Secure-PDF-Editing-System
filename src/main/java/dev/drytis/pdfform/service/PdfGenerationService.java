package dev.drytis.pdfform.service;

import dev.drytis.pdfform.model.Gstr3bData;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;

import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import com.openhtmltopdf.svgsupport.BatikSVGDrawer;

/**
 * Renders the GSTR-3B Thymeleaf template and converts the HTML to PDF
 * preserving the original layout (A4 pages, borders, FILED watermark).
 */
@Service
public class PdfGenerationService {

    private final TemplateEngine templateEngine;

    public PdfGenerationService(TemplateEngine templateEngine) {
        this.templateEngine = templateEngine;
    }

    public String renderHtml(Gstr3bData data) {
        Context ctx = new Context();
        ctx.setVariable("d", data);
        return templateEngine.process("gstr3b", ctx);
    }

    public byte[] generatePdf(Gstr3bData data) throws Exception {
        String html = renderHtml(data);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        PdfRendererBuilder builder = new PdfRendererBuilder();
        builder.useFastMode();
        builder.useSVGDrawer(new BatikSVGDrawer());
        builder.withHtmlContent(html, null);
        builder.toStream(out);
        builder.run();
        return out.toByteArray();
    }
}
