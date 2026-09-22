package dev.drytis.pdfform.service;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.encryption.AccessPermission;
import org.apache.pdfbox.pdmodel.encryption.StandardProtectionPolicy;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Applies password encryption + permissions to generated PDFs (PDFBox).
 * Passwords are read from environment-backed properties only; never logged.
 */
@Service
public class PdfSecurityService {

    @Value("${pdf.security.user-password:}")
    private String userPassword;

    @Value("${pdf.security.owner-password:}")
    private String ownerPassword;

    @Value("${pdf.security.enabled:true}")
    private boolean enabled;

    public boolean isEnabled() {
        return enabled && userPassword != null && !userPassword.isBlank();
    }

    public byte[] encryptToBytes(org.apache.pdfbox.pdmodel.PDDocument doc) throws Exception {
        AccessPermission ap = new AccessPermission();
        ap.setCanPrint(true);
        ap.setCanModify(false);
        ap.setCanExtractContent(false);
        ap.setCanFillInForm(true);
        StandardProtectionPolicy spp = new StandardProtectionPolicy(
                ownerPassword == null || ownerPassword.isBlank() ? userPassword : ownerPassword,
                userPassword, ap);
        spp.setEncryptionKeyLength(128);
        doc.protect(spp);
        java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
        doc.save(out);
        return out.toByteArray();
    }

    public byte[] encrypt(byte[] input) throws Exception {
        if (!isEnabled()) return input;
        try (PDDocument doc = PDDocument.load(input)) {
            AccessPermission ap = new AccessPermission();
            ap.setCanPrint(true);          // printing allowed
            ap.setCanModify(false);        // editing denied
            ap.setCanExtractContent(false);// copy/extract denied
            ap.setCanFillInForm(true);     // form fill allowed

            StandardProtectionPolicy spp = new StandardProtectionPolicy(
                    ownerPassword == null || ownerPassword.isBlank() ? userPassword : ownerPassword,
                    userPassword, ap);
            spp.setEncryptionKeyLength(128);
            doc.protect(spp);
            java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
            doc.save(out);
            return out.toByteArray();
        }
    }
}
