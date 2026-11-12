package pe.sga.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.core.io.Resource;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import pe.sga.exception.BadRequestException;
import pe.sga.exception.ResourceNotFoundException;
import pe.sga.service.impl.LocalStorageServiceImpl;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class LocalStorageServiceTest {

    private LocalStorageServiceImpl storageService;

    @TempDir
    Path tempUploadDir;

    @BeforeEach
    void setUp() {
        storageService = new LocalStorageServiceImpl();
        ReflectionTestUtils.setField(storageService, "uploadDir", tempUploadDir.toString());
        storageService.init();
    }

    @Test
    void store_enSubdirectorio_guardaArchivoEnEstructuraJerarquica() throws IOException {
        MockMultipartFile file = new MockMultipartFile(
                "file", "factura_comercial.pdf", "application/pdf", "Contenido DAM PDF".getBytes(StandardCharsets.UTF_8)
        );

        String relativePath = storageService.store(file, "expedientes/2026/EXP-2026-0042/dam");

        assertNotNull(relativePath);
        assertTrue(relativePath.startsWith("expedientes/2026/EXP-2026-0042/dam/"));
        assertTrue(relativePath.endsWith(".pdf"));

        Path physicalFile = tempUploadDir.resolve(relativePath);
        assertTrue(Files.exists(physicalFile));
        assertEquals("Contenido DAM PDF", Files.readString(physicalFile));
    }

    @Test
    void store_sinSubdirectorio_guardaEnRaiz() throws IOException {
        MockMultipartFile file = new MockMultipartFile(
                "file", "foto.png", "image/png", "PNGDATA".getBytes(StandardCharsets.UTF_8)
        );

        String relativePath = storageService.store(file);

        assertNotNull(relativePath);
        assertFalse(relativePath.contains("/"));
        assertTrue(relativePath.endsWith(".png"));

        Path physicalFile = tempUploadDir.resolve(relativePath);
        assertTrue(Files.exists(physicalFile));
    }

    @Test
    void loadAsResource_conRutaJerarquica_cargaExitosamente() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "voucher.jpg", "image/jpeg", "VOUCHER123".getBytes(StandardCharsets.UTF_8)
        );

        String relativePath = storageService.store(file, "pagos/2026/11/20512345678");
        Resource resource = storageService.loadAsResource(relativePath);

        assertNotNull(resource);
        assertTrue(resource.exists());
        assertTrue(resource.isReadable());
    }

    @Test
    void loadAsResource_fallbackArchivoPlano_cargaExitosamente() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "antiguo.pdf", "application/pdf", "OLD_PDF".getBytes(StandardCharsets.UTF_8)
        );

        // Guardado plano en raíz
        String flatFilename = storageService.store(file);

        // Intento de carga pasando solo el nombre
        Resource resource = storageService.loadAsResource(flatFilename);

        assertNotNull(resource);
        assertTrue(resource.exists());
    }

    @Test
    void store_conPathTraversal_lanzaExcepcion() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "malicioso.pdf", "application/pdf", "MALICIOUS".getBytes(StandardCharsets.UTF_8)
        );

        assertThrows(BadRequestException.class, () -> {
            storageService.store(file, "../../../etc/cron.d");
        });
    }

    @Test
    void delete_eliminaArchivoEnSubdirectorio() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "borrar.pdf", "application/pdf", "TO_DELETE".getBytes(StandardCharsets.UTF_8)
        );

        String relativePath = storageService.store(file, "cotizaciones/2026/COT-001");
        Path physicalFile = tempUploadDir.resolve(relativePath);
        assertTrue(Files.exists(physicalFile));

        storageService.delete(relativePath);
        assertFalse(Files.exists(physicalFile));
    }
}
