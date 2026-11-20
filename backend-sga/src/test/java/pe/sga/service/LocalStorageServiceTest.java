package pe.sga.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.core.io.Resource;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import pe.sga.exception.BadRequestException;
import pe.sga.service.impl.LocalStorageServiceImpl;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Pruebas Unitarias - LocalStorageServiceImpl")
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
    @DisplayName("Normal: guarda y recupera archivo en estructura jerárquica de subdirectorios")
    void storeYLoadAsResource_enSubdirectorio_guardaYCargaEnEstructuraJerarquica() throws IOException {
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

        Resource resource = storageService.loadAsResource(relativePath);
        assertNotNull(resource);
        assertTrue(resource.exists());
        assertTrue(resource.isReadable());
    }

    @Test
    @DisplayName("Normal / Fallback: guarda y recupera archivo plano directamente en directorio raíz")
    void storeYLoadAsResource_sinSubdirectorio_guardaYCargaEnRaiz() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "foto.png", "image/png", "PNGDATA".getBytes(StandardCharsets.UTF_8)
        );

        String flatFilename = storageService.store(file);

        assertNotNull(flatFilename);
        assertFalse(flatFilename.contains("/"));
        assertTrue(flatFilename.endsWith(".png"));
        assertTrue(Files.exists(tempUploadDir.resolve(flatFilename)));

        Resource resource = storageService.loadAsResource(flatFilename);
        assertNotNull(resource);
        assertTrue(resource.exists());
    }

    @Test
    @DisplayName("Seguridad / Excepción: rechaza intentos de Path Traversal fuera del directorio base")
    void store_conPathTraversal_lanzaExcepcion() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "malicioso.pdf", "application/pdf", "MALICIOUS".getBytes(StandardCharsets.UTF_8)
        );

        assertThrows(BadRequestException.class, () ->
                storageService.store(file, "../../../etc/cron.d")
        );
    }

    @Test
    @DisplayName("Normal: elimina físicamente el archivo almacenado en subdirectorio")
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
