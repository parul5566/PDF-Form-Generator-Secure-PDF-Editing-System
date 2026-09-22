package dev.drytis.pdfform.repo;

import dev.drytis.pdfform.model.FormData;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FormDataRepository extends JpaRepository<FormData, Long> {
}
