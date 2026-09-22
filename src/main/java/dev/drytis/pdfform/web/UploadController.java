package dev.drytis.pdfform.web;

import dev.drytis.pdfform.model.PdfDocument;
import dev.drytis.pdfform.repo.PdfDocumentRepository;
import dev.drytis.pdfform.service.FileStorageService;
import dev.drytis.pdfform.service.PdfSecurityService;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.interactive.form.PDAcroForm;
import org.apache.pdfbox.pdmodel.interactive.form.PDField;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.ByteArrayInputStream;
import java.util.*;

@Controller
public class UploadController {

    private final FileStorageService storage;
    private final PdfDocumentRepository documentRepo;
    private final PdfSecurityService securityService;

    public UploadController(FileStorageService storage, PdfDocumentRepository documentRepo,
                            PdfSecurityService securityService) {
        this.storage = storage;
        this.documentRepo = documentRepo;
        this.securityService = securityService;
    }

    @GetMapping("/upload")
    public String uploadPage() {
        return "upload";
    }

    @PostMapping("/upload")
    public String upload(@RequestParam("file") MultipartFile file, Model model) {
        if (file == null || file.isEmpty()) {
            model.addAttribute("error", "Please choose a PDF file to upload.");
            return "upload";
        }
        String original = file.getOriginalFilename() == null ? "upload.pdf" : file.getOriginalFilename();
        if (!original.toLowerCase().endsWith(".pdf") || !"application/pdf".equals(file.getContentType())) {
            model.addAttribute("error", "Only PDF files are allowed.");
            return "upload";
        }
        if (file.getSize() > 20L * 1024 * 1024) {
            model.addAttribute("error", "File is too large (max 20 MB).");
            return "upload";
        }
        try {
            byte[] bytes = file.getBytes();
            // validate it is really a PDF
            if (bytes.length < 5 || !new String(bytes, 0, 5).equals("%PDF-")) {
                model.addAttribute("error", "The file is not a valid PDF.");
                return "upload";
            }
            List<Map<String, String>> fields = new ArrayList<>();
            try (PDDocument doc = PDDocument.load(bytes)) {
                PDAcroForm acro = doc.getDocumentCatalog().getAcroForm();
                if (acro == null || acro.getFields().isEmpty()) {
                    model.addAttribute("error", "This PDF has no editable (AcroForm) fields. Editing is only supported for fillable PDFs.");
                    model.addAttribute("uploaded", true);
                    // still record the upload in history
                    storeUpload(bytes, original);
                    return "upload";
                }
                for (PDField f : acro.getFields()) {
                    Map<String, String> fm = new LinkedHashMap<>();
                    fm.put("name", f.getFullyQualifiedName());
                    fm.put("type", f.getClass().getSimpleName());
                    fm.put("value", f.getValueAsString() == null ? "" : f.getValueAsString());
                    fields.add(fm);
                }
            }
            String safeName = storage.safeName(original);
            storage.store("uploads", safeName, new ByteArrayInputStream(bytes));
            model.addAttribute("fields", fields);
            model.addAttribute("storedName", safeName);
            model.addAttribute("originalName", original);
            return "upload-edit";
        } catch (Exception e) {
            model.addAttribute("error", "Could not process the PDF: " + e.getMessage());
            return "upload";
        }
    }

    private void storeUpload(byte[] bytes, String original) throws Exception {
        String safeName = storage.safeName(original);
        storage.store("uploads", safeName, new ByteArrayInputStream(bytes));
        PdfDocument doc = new PdfDocument();
        doc.setDocType("UPLOADED");
        doc.setOriginalFilename(original);
        doc.setGeneratedFilename(safeName);
        doc.setFilePath(storage.resolveExisting("uploads", safeName).toString());
        doc.setStatus("READY");
        doc.setEncryptionStatus("NONE");
        documentRepo.save(doc);
    }

    @PostMapping("/upload/save")
    public String saveEdited(@RequestParam("storedName") String storedName,
                             @RequestParam Map<String, String> params,
                             RedirectAttributes ra) {
        try {
            Path0 source = new Path0(storage.resolveExisting("uploads", storedName).toString());
            byte[] out;
            List<String> updated = new ArrayList<>();
            try (PDDocument doc = PDDocument.load(new java.io.File(source.p))) {
                PDAcroForm acro = doc.getDocumentCatalog().getAcroForm();
                if (acro != null) {
                    for (Map.Entry<String, String> e : params.entrySet()) {
                        if (!e.getKey().startsWith("field:")) continue;
                        String fieldName = e.getKey().substring(6);
                        PDField f = acro.getField(fieldName);
                        if (f != null) {
                            f.setValue(e.getValue());
                            updated.add(fieldName);
                        }
                    }
                    acro.flatten();
                }
                out = securityService.isEnabled() ? securityService.encryptToBytes(doc) : toBytes(doc);
            }
            String outName = storedName.replaceAll("(?i)\\.pdf$", "") + "-updated.pdf";
            storage.store("uploads", outName, new ByteArrayInputStream(out));
            PdfDocument doc = new PdfDocument();
            doc.setDocType("GENERATED");
            doc.setOriginalFilename(storedName);
            doc.setGeneratedFilename(outName);
            doc.setFilePath(storage.resolveExisting("uploads", outName).toString());
            doc.setStatus("READY");
            doc.setEncryptionStatus(securityService.isEnabled() ? "ENCRYPTED" : "NONE");
            documentRepo.save(doc);
            ra.addAttribute("ok", "1");
            ra.addAttribute("updated", String.join(", ", updated));
            return "redirect:/documents";
        } catch (Exception e) {
            ra.addAttribute("err", e.getMessage());
            return "redirect:/upload";
        }
    }

    private byte[] toBytes(PDDocument doc) throws Exception {
        java.io.ByteArrayOutputStream bos = new java.io.ByteArrayOutputStream();
        doc.save(bos);
        return bos.toByteArray();
    }

    /** tiny holder to avoid extra imports */
    private static class Path0 {
        final String p;
        Path0(String p) { this.p = p; }
    }
}
