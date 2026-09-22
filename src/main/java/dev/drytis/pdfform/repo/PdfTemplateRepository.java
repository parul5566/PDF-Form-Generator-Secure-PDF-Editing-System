package dev.drytis.pdfform.repo;

import dev.drytis.pdfform.model.PdfTemplate;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface PdfTemplateRepository extends JpaRepository<PdfTemplate, Long> {
    Optional<PdfTemplate> findByNameAndActiveTrue(String name);
}
