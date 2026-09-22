package dev.drytis.pdfform.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class FileStorageService {

    @Value("${storage.path}")
    private String storagePath;

    private static final String SAFE_NAME = "[^A-Za-z0-9._-]";
    private final AtomicLong counter = new AtomicLong(System.currentTimeMillis() % 100000);

    public Path baseDir() throws IOException {
        Path base = Path.of(storagePath);
        Files.createDirectories(base);
        return base;
    }

    public Path dirFor(String sub) throws IOException {
        Path dir = baseDir().resolve(sub);
        Files.createDirectories(dir);
        return dir;
    }

    /** Sanitized, collision-safe filename. Never trust client input. */
    public String safeName(String original) {
        String name = original == null ? "file" : original;
        name = name.replaceAll(SAFE_NAME, "_");
        if (name.length() > 80) name = name.substring(name.length() - 80);
        return name;
    }

    /** e.g. GSTR3B-20260921-4821 */
    public String nextReference() {
        String date = LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE);
        int rand = ThreadLocalRandom.current().nextInt(1000, 9999);
        return "GSTR3B-" + date + "-" + rand;
    }

    public String nextGeneratedName(String reference) {
        return reference + ".pdf";
    }

    public Path store(String sub, String filename, InputStream data) throws IOException {
        Path dir = dirFor(sub);
        Path target = dir.resolve(filename).normalize();
        if (!target.startsWith(dir)) throw new IOException("Unsafe path");
        Files.copy(data, target, StandardCopyOption.REPLACE_EXISTING);
        return target;
    }

    public Path resolveExisting(String sub, String filename) throws IOException {
        Path dir = dirFor(sub);
        Path target = dir.resolve(filename).normalize();
        if (!target.startsWith(dir)) throw new IOException("Unsafe path");
        if (!Files.exists(target)) throw new NoSuchFileException(filename);
        return target;
    }
}
