package pe.sga.functional;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;
import pe.sga.entity.enums.RolUsuario;
import pe.sga.repository.IDocumentoAdjuntoRepository;
import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

@Tag("seguridad")
@TestPropertySource(properties = {
        "app.storage.upload-dir=./target/seg08-files",
        "spring.servlet.multipart.max-file-size=1KB",
        "spring.servlet.multipart.max-request-size=8KB"
})
class SecurityUploadTest extends FullStackTestBase {
    @Autowired IDocumentoAdjuntoRepository documents;
    record Part(String name, String mime, String content) { }

    long diskFiles() throws Exception {
        Path root = Path.of("target/seg08-files");
        if (!Files.exists(root)) return 0;
        try (var stream = Files.walk(root)) { return stream.filter(Files::isRegularFile).count(); }
    }

    HttpResponse<String> upload(String token, List<Part> parts) throws Exception {
        String boundary = "SEG08_BOUNDARY";
        StringBuilder body = new StringBuilder();
        for (Part part : parts) body.append("--").append(boundary)
                .append("\r\nContent-Disposition: form-data; name=\"files\"; filename=\"")
                .append(part.name()).append("\"\r\nContent-Type: ").append(part.mime())
                .append("\r\n\r\n").append(part.content()).append("\r\n");
        body.append("--").append(boundary).append("--\r\n");
        var request = HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + "/api/v1/documentos/upload?categoria=GENERAL"))
                .header("Authorization", "Bearer " + token)
                .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                .POST(HttpRequest.BodyPublishers.ofString(body.toString(), StandardCharsets.UTF_8)).build();
        return http.send(request, HttpResponse.BodyHandlers.ofString());
    }

    void rejected(String token, List<Part> parts, int status) throws Exception {
        long rows = documents.count(), files = diskFiles();
        var response = upload(token, parts);
        assertEquals(status, response.statusCode());
        assertFalse(tree(response).path("success").asBoolean());
        assertEquals(rows, documents.count(), "No deben guardarse documentos rechazados");
        assertEquals(files, diskFiles(), "No deben quedar archivos de una solicitud rechazada");
    }

    @Test void SEG08_01_archivoPermitidoEnLimiteSeDescarga() throws Exception {
        String token = login(user(RolUsuario.ADMINISTRADOR_TI));
        String content = "A".repeat(1024);
        var response = upload(token, List.of(new Part("seg08-valido.txt", "text/plain", content)));
        assertEquals(201, response.statusCode());
        var item = tree(response).path("data").get(0);
        assertEquals(1024, item.path("tamanoBytes").asLong());
        assertNotEquals("seg08-valido.txt", item.path("nombreAlmacenado").asText());
        var download = call("GET", "/api/v1/documentos/" + item.path("idDocumento").asInt() + "/descargar", token, null);
        assertEquals(200, download.statusCode());
        assertEquals(content, download.body());
        assertTrue(download.headers().firstValue("Content-Disposition").orElse("").startsWith("attachment;"));
        System.out.println("[SEG-08.1] APROBADO: archivo permitido en límite, nombre generado y descarga íntegra.");
    }

    @Test void SEG08_02_extensionesNoPermitidas() throws Exception {
        String token = login(user(RolUsuario.ADMINISTRADOR_TI));
        for (String name : new String[]{"seg08.html", "seg08.exe", "seg08.pdf.html"}) {
            rejected(token, List.of(new Part(name, "application/octet-stream", "CONTENIDO FICTICIO INERTE")), 400);
        }
        System.out.println("[SEG-08.2] APROBADO: HTML, ejecutable y doble extensión no permitida rechazados.");
    }

    @Test void SEG08_03_mimeYContenidoIncompatibles() throws Exception {
        String token = login(user(RolUsuario.ADMINISTRADOR_TI));
        rejected(token, List.of(new Part("seg08.pdf", "text/html", "<p>FICTICIO SIN CODIGO</p>")), 400);
        rejected(token, List.of(new Part("seg08.pdf", "application/pdf", "ESTO ES TEXTO, NO PDF")), 400);
        System.out.println("[SEG-08.3] APROBADO: MIME incompatible y PDF sin firma rechazados.");
    }

    @Test void SEG08_04_archivoVacio() throws Exception {
        String token = login(user(RolUsuario.ADMINISTRADOR_TI));
        rejected(token, List.of(new Part("seg08-vacio.txt", "text/plain", "")), 400);
        System.out.println("[SEG-08.4] APROBADO: archivo vacío rechazado sin guardar.");
    }

    @Test void SEG08_05_nombreConRuta() throws Exception {
        String token = login(user(RolUsuario.ADMINISTRADOR_TI));
        rejected(token, List.of(new Part("../seg08-fuera.txt", "text/plain", "FICTICIO")), 400);
        System.out.println("[SEG-08.5] APROBADO: nombre con ruta rechazado sin escribir archivos.");
    }

    @Test void SEG08_06_excesoDeTamano() throws Exception {
        String token = login(user(RolUsuario.ADMINISTRADOR_TI));
        rejected(token, List.of(new Part("seg08-grande.txt", "text/plain", "A".repeat(1025))), 413);
        System.out.println("[SEG-08.6] APROBADO: un byte sobre el límite de prueba recibe 413.");
    }

    @Test void SEG08_07_loteMixtoNoDejaArchivosParciales() throws Exception {
        String token = login(user(RolUsuario.ADMINISTRADOR_TI));
        rejected(token, List.of(new Part("seg08-lote.txt", "text/plain", "FICTICIO"),
                new Part("seg08-lote.html", "text/html", "<p>FICTICIO</p>")), 400);
        System.out.println("[SEG-08.7] APROBADO: lote con archivo inválido rechazado antes de almacenar el válido.");
    }

    @Test void SEG08_08_mimeGenericoSeNormalizaATextoSeguro() throws Exception {
        String token = login(user(RolUsuario.ADMINISTRADOR_TI));
        String content = "<p>CONTENIDO FICTICIO SIN CODIGO</p>";
        var response = upload(token, List.of(new Part("seg08-texto.txt", "application/octet-stream", content)));
        assertEquals(201, response.statusCode());
        var item = tree(response).path("data").get(0);
        assertEquals("text/plain", item.path("formatoMime").asText());
        var preview = call("GET", "/api/v1/documentos/" + item.path("idDocumento").asInt() + "/preview", token, null);
        assertEquals(200, preview.statusCode());
        assertTrue(preview.headers().firstValue("Content-Type").orElse("").startsWith("text/plain"));
        assertEquals("nosniff", preview.headers().firstValue("X-Content-Type-Options").orElse(""));
        assertEquals(content, preview.body());
        System.out.println("[SEG-08.8] APROBADO: MIME genérico normalizado; preview de TXT permanece text/plain y nosniff.");
    }
}
