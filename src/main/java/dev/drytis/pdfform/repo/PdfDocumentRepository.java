package dev.drytis.pdfform.repo;

import dev.drytis.pdfform.model.PdfDocument;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface PdfDocumentRepository extends JpaRepository<PdfDocument, Long> {
    List<PdfDocument> findAllByOrderByIdDesc();
}
