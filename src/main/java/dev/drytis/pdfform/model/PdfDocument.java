package dev.drytis.pdfform.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "pdf_documents")
public class PdfDocument {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "form_data_id")
    private FormData formData;

    @Column(name = "doc_type", nullable = false, length = 20)
    private String docType; // GENERATED | UPLOADED

    @Column(name = "original_filename")
    private String originalFilename;

    @Column(name = "generated_filename", nullable = false)
    private String generatedFilename;

    @Column(name = "file_path", nullable = false)
    private String filePath;

    @Column(nullable = false, length = 20)
    private String status; // READY | FAILED

    @Column(name = "encryption_status", nullable = false, length = 20)
    private String encryptionStatus; // NONE | ENCRYPTED

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        createdAt = updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public FormData getFormData() { return formData; }
    public void setFormData(FormData formData) { this.formData = formData; }
    public String getDocType() { return docType; }
    public void setDocType(String docType) { this.docType = docType; }
    public String getOriginalFilename() { return originalFilename; }
    public void setOriginalFilename(String originalFilename) { this.originalFilename = originalFilename; }
    public String getGeneratedFilename() { return generatedFilename; }
    public void setGeneratedFilename(String generatedFilename) { this.generatedFilename = generatedFilename; }
    public String getFilePath() { return filePath; }
    public void setFilePath(String filePath) { this.filePath = filePath; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getEncryptionStatus() { return encryptionStatus; }
    public void setEncryptionStatus(String encryptionStatus) { this.encryptionStatus = encryptionStatus; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
