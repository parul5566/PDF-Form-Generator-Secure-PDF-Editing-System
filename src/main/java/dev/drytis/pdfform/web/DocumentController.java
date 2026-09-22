package dev.drytis.pdfform.web;

import dev.drytis.pdfform.model.PdfDocument;
import dev.drytis.pdfform.repo.PdfDocumentRepository;
import dev.drytis.pdfform.service.FileStorageService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

@Controller
public class DocumentController {

    private final PdfDocumentRepository documentRepo;
    private final FileStorageService storage;

    public DocumentController(PdfDocumentRepository documentRepo, FileStorageService storage) {
        this.documentRepo = documentRepo;
        this.storage = storage;
    }

    @GetMapping("/documents")
    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public String history(Model model) {
        List<PdfDocument> docs = documentRepo.findAllByOrderByIdDesc();
        docs.forEach(d -> { if (d.getFormData() != null) d.getFormData().getReferenceNo(); }); // init lazy refs
        model.addAttribute("docs", docs);
        return "documents";
    }

    @GetMapping("/documents/{id}/preview")
    public void preview(@PathVariable Long id, HttpServletResponse response) throws IOException {
        serve(id, response, true);
    }

    @GetMapping("/documents/{id}/download")
    public void download(@PathVariable Long id, HttpServletResponse response) throws IOException {
        serve(id, response, false);
    }

    private void serve(Long id, HttpServletResponse response, boolean inline) throws IOException {
        PdfDocument doc = documentRepo.findById(id).orElse(null);
        if (doc == null || !"READY".equals(doc.getStatus())) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND, "Document not found");
            return;
        }
        // Resolve strictly inside the storage directory — never trust stored path alone.
        Path file;
        try {
            file = storage.resolveExisting("generated", doc.getGeneratedFilename());
        } catch (IOException e) {
            // uploaded docs live in "uploads"
            try {
                file = storage.resolveExisting("uploads", doc.getGeneratedFilename());
            } catch (IOException e2) {
                response.sendError(HttpServletResponse.SC_NOT_FOUND, "File missing");
                return;
            }
        }
        response.setContentType(MediaType.APPLICATION_PDF_VALUE);
        String disposition = (inline ? "inline" : "attachment") + "; filename=\"" + doc.getGeneratedFilename() + "\"";
        response.setHeader("Content-Disposition", disposition);
        Files.copy(file, response.getOutputStream());
        response.getOutputStream().flush();
    }
}
