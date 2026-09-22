package dev.drytis.pdfform;

import dev.drytis.pdfform.model.Gstr3bData;
import dev.drytis.pdfform.service.FileStorageService;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;

import static org.junit.jupiter.api.Assertions.*;

class UnitTests {

    @Test
    void gstr3bDataDefaultsToZero() {
        Gstr3bData d = new Gstr3bData();
        assertEquals("0.00", d.num("t31_a_val"));
        assertEquals("0.00", d.num("t61_aIgst_net"));
    }

    @Test
    void numericNormalization() {
        Gstr3bData d = new Gstr3bData();
        d.put("t31_a_val", "6896.5");
        assertEquals("6896.50", d.num("t31_a_val"));
        d.put("t31_a_val", "abc");
        assertEquals("0.00", d.num("t31_a_val"));
        d.put("t31_a_val", "");
        assertEquals("0.00", d.num("t31_a_val"));
    }

    @Test
    void dashValuesArePreserved() {
        Gstr3bData d = new Gstr3bData();
        d.put("t51_interestComputed_igst", "-");
        assertEquals("-", d.num("t51_interestComputed_igst"));
    }

    @Test
    void safeNameStripsUnsafeChars() throws Exception {
        FileStorageService s = new FileStorageService();
        java.lang.reflect.Field f = FileStorageService.class.getDeclaredField("storagePath");
        f.setAccessible(true);
        f.set(s, Files.createTempDirectory("storetest").toString());
        String out = s.safeName("../../etc/passwd");
        assertFalse(out.contains("/") && out.contains(".."));
        assertEquals("report_final.pdf", s.safeName("report final.pdf".replace(' ', '_')));
        assertTrue(s.safeName("a<b>|?.pdf").matches("[A-Za-z0-9._-]+"));
    }

    @Test
    void referenceNumberFormat() throws Exception {
        FileStorageService s = new FileStorageService();
        java.lang.reflect.Field f = FileStorageService.class.getDeclaredField("storagePath");
        f.setAccessible(true);
        f.set(s, Files.createTempDirectory("storetest2").toString());
        String ref = s.nextReference();
        assertTrue(ref.matches("GSTR3B-\\d{8}-\\d{4}"), "got: " + ref);
    }

    @Test
    void pathTraversalRejected() throws Exception {
        FileStorageService s = new FileStorageService();
        java.lang.reflect.Field f = FileStorageService.class.getDeclaredField("storagePath");
        f.setAccessible(true);
        f.set(s, Files.createTempDirectory("storetest3").toString());
        assertThrows(Exception.class, () -> s.resolveExisting("generated", "../../etc/passwd"));
    }
}
