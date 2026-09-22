package dev.drytis.pdfform.web;

import dev.drytis.pdfform.model.FormData;
import dev.drytis.pdfform.model.Gstr3bData;
import dev.drytis.pdfform.model.PdfDocument;
import dev.drytis.pdfform.repo.FormDataRepository;
import dev.drytis.pdfform.repo.PdfDocumentRepository;
import dev.drytis.pdfform.service.FileStorageService;
import dev.drytis.pdfform.service.PdfGenerationService;
import dev.drytis.pdfform.service.PdfSecurityService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.ByteArrayInputStream;
import java.nio.file.Path;
import java.util.Map;

@Controller
public class Gstr3bFormController {

    private final PdfGenerationService generationService;
    private final PdfSecurityService securityService;
    private final FileStorageService storage;
    private final FormDataRepository formDataRepo;
    private final PdfDocumentRepository documentRepo;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public Gstr3bFormController(PdfGenerationService generationService,
                                PdfSecurityService securityService,
                                FileStorageService storage,
                                FormDataRepository formDataRepo,
                                PdfDocumentRepository documentRepo) {
        this.generationService = generationService;
        this.securityService = securityService;
        this.storage = storage;
        this.formDataRepo = formDataRepo;
        this.documentRepo = documentRepo;
    }

    @GetMapping("/form/new")
    public String newForm(Model model) {
        model.addAttribute("d", model.containsAttribute("d") ? model.getAttribute("d") : new Gstr3bData());
        return "gstr3b-form";
    }

    @PostMapping("/form/submit")
    public String submit(@RequestParam Map<String, String> params, Model model, RedirectAttributes ra) {
        Gstr3bData data = new Gstr3bData();
        StringBuilder errors = new StringBuilder();
        for (Map.Entry<String, String> e : params.entrySet()) {
            if (e.getKey().startsWith("_")) continue; // csrf etc
            data.put(e.getKey(), e.getValue());
        }
        require(data, "gstin", "GSTIN", errors);
        require(data, "legalName", "Legal name", errors);
        require(data, "year", "Year", errors);
        require(data, "period", "Period", errors);
        require(data, "verifyDate", "Verification date", errors);
        require(data, "signatoryName", "Authorized signatory name", errors);
        for (String key : data.all().keySet()) {
            String v = data.get(key);
            if (v.isEmpty() || v.equals("-")) continue;
            if (isNumericKey(key)) {
                try {
                    double d = Double.parseDouble(v);
                    if (d < 0) errors.append(key).append(" must not be negative. ");
                } catch (NumberFormatException ex) {
                    errors.append(key).append(" must be a number. ");
                }
            }
        }
        if (errors.length() > 0) {
            model.addAttribute("d", data);
            model.addAttribute("error", "Please fix the following: " + errors);
            return "gstr3b-form";
        }

        try {
            byte[] pdf = generationService.generatePdf(data);
            pdf = securityService.encrypt(pdf);

            String reference = storage.nextReference();
            String filename = storage.nextGeneratedName(reference);

            FormData fd = new FormData();
            fd.setReferenceNo(reference);
            fd.setFieldValues(objectMapper.writeValueAsString(data.all()));
            formDataRepo.save(fd);

            PdfDocument doc = new PdfDocument();
            doc.setFormData(fd);
            doc.setDocType("GENERATED");
            doc.setGeneratedFilename(filename);
            Path stored = storage.store("generated", filename, new ByteArrayInputStream(pdf));
            doc.setFilePath(stored.toString());
            doc.setStatus("READY");
            doc.setEncryptionStatus(securityService.isEnabled() ? "ENCRYPTED" : "NONE");
            documentRepo.save(doc);

            return "redirect:/form/success/" + doc.getId();
        } catch (Exception e) {
            model.addAttribute("d", data);
            model.addAttribute("error", "PDF generation failed: " + e.getMessage());
            return "gstr3b-form";
        }
    }

    @GetMapping("/form/success/{id}")
    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public String success(@PathVariable Long id, Model model) {
        PdfDocument doc = documentRepo.findById(id).orElse(null);
        if (doc == null) return "redirect:/form/new";
        if (doc.getFormData() != null) doc.getFormData().getReferenceNo(); // init lazy
        model.addAttribute("doc", doc);
        model.addAttribute("reference", doc.getFormData() != null ? doc.getFormData().getReferenceNo() : "-");
        return "success";
    }

    private void require(Gstr3bData d, String key, String label, StringBuilder errors) {
        if (d.get(key) == null || d.get(key).isEmpty()) {
            errors.append(label).append(" is required. ");
        }
    }

    private boolean isNumericKey(String key) {
        return key.startsWith("t31_") || key.startsWith("t311_") || key.startsWith("t32_")
                || key.startsWith("t4_") || key.startsWith("t5_") || key.startsWith("t51_")
                || key.startsWith("t61_") || key.startsWith("breakup_");
    }
}
