package pe.sga.functional;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import pe.sga.entity.enums.RolUsuario;
import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Locale;
import static org.junit.jupiter.api.Assertions.*;

/** SEG-07: validación y deserialización con HTTP real, datos ficticios y H2. */
@Tag("seguridad")
class SecurityInputValidationTest extends FullStackTestBase {
    void errorSeguro(HttpResponse<String> response, int status) throws Exception {
        assertEquals(status, response.statusCode());
        assertFalse(tree(response).path("success").asBoolean());
        assertTrue(response.headers().firstValue("Content-Type").orElse("").contains("application/json"));
        String body = response.body().toLowerCase(Locale.ROOT);
        for (String detail : new String[]{"stacktrace", "java.lang.", "org.springframework.",
                "jdbc:", "sqlstate", "tools.jackson.", "com.fasterxml.", "exception"}) {
            assertFalse(body.contains(detail), "No deben revelarse detalles internos");
        }
    }

    void rechazadoSinInsertar(String token, Object body, String field) throws Exception {
        long before = clientes.count();
        var response = call("POST", "/api/v1/clientes", token, body);
        errorSeguro(response, 400);
        if (field != null) assertTrue(tree(response).path("data").has(field));
        assertEquals(before, clientes.count(), "La solicitud inválida no debe crear registros");
    }

    @Test
    void SEG07_01_obligatoriosNulosVaciosYEnBlanco() throws Exception {
        String token = login(user(RolUsuario.AGENTE_COMERCIAL));
        rechazadoSinInsertar(token, "{}", "numeroDocumento");
        var input = dto(unique()); input.setNumeroDocumento(null);
        rechazadoSinInsertar(token, input, "numeroDocumento");
        input = dto(unique()); input.setRazonSocial("   ");
        rechazadoSinInsertar(token, input, "razonSocial");
        input = dto(unique()); input.setCorreoContacto("");
        rechazadoSinInsertar(token, input, "correoContacto");
        System.out.println("[SEG-07.1] APROBADO: campos ausentes, nulos, vacíos y blancos rechazados sin insertar.");
    }

    @Test
    void SEG07_02_formatoYLongitudesMaximas() throws Exception {
        String token = login(user(RolUsuario.AGENTE_COMERCIAL));
        var input = dto(unique()); input.setCorreoContacto("correo-invalido");
        rechazadoSinInsertar(token, input, "correoContacto");
        input = dto("1".repeat(16));
        rechazadoSinInsertar(token, input, "numeroDocumento");
        input = dto(unique()); input.setRazonSocial("A".repeat(151));
        rechazadoSinInsertar(token, input, "razonSocial");
        input = dto(unique()); input.setTelefonoContacto("1".repeat(21));
        rechazadoSinInsertar(token, input, "telefonoContacto");
        System.out.println("[SEG-07.2] APROBADO: correo inválido y límites excedidos reciben 400 sin insertar.");
    }

    @Test
    void SEG07_03_jsonMalformadoYTiposIncompatibles() throws Exception {
        String token = login(user(RolUsuario.AGENTE_COMERCIAL));
        for (String body : new String[]{"{", "[]", "null",
                "{\"razonSocial\":{\"valor\":\"ficticio\"}}",
                "{\"estadoActivo\":\"valor-no-booleano\"}"}) {
            rechazadoSinInsertar(token, body, null);
        }
        // Un error de lectura no debe dejar el endpoint inutilizable.
        int id = create(token, unique());
        assertTrue(clientes.existsById(id));
        System.out.println("[SEG-07.3] APROBADO: JSON y tipos incompatibles reciben 400; una solicitud válida posterior funciona.");
    }

    @Test
    void SEG07_04_tipoDeContenidoNoSoportado() throws Exception {
        String token = login(user(RolUsuario.AGENTE_COMERCIAL));
        long before = clientes.count();
        var request = HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + "/api/v1/clientes"))
                .header("Authorization", "Bearer " + token).header("Content-Type", "text/plain")
                .POST(HttpRequest.BodyPublishers.ofString("datos ficticios SEG07")).build();
        errorSeguro(http.send(request, HttpResponse.BodyHandlers.ofString()), 415);
        assertEquals(before, clientes.count());
        System.out.println("[SEG-07.4] APROBADO: contenido incompatible recibe 415 sin insertar ni mostrar detalles internos.");
    }

    @Test
    void SEG07_05_limiteValidoYActualizacionInvalida() throws Exception {
        String token = login(user(RolUsuario.AGENTE_COMERCIAL));
        var input = dto(unique() + "1234");
        input.setRazonSocial("A".repeat(150));
        var created = call("POST", "/api/v1/clientes", token, input);
        assertEquals(201, created.statusCode());
        int id = tree(created).path("data").path("idCliente").asInt();
        assertTrue(id > 0);
        var original = clientes.findById(id).orElseThrow();
        assertEquals(150, original.getRazonSocial().length());
        assertEquals(15, original.getNumeroDocumento().length());
        long before = clientes.count();
        input.setRazonSocial("B".repeat(151));
        errorSeguro(call("PUT", "/api/v1/clientes/" + id, token, input), 400);
        var after = clientes.findById(id).orElseThrow();
        assertEquals(original.getRazonSocial(), after.getRazonSocial());
        assertEquals(original.getCorreoContacto(), after.getCorreoContacto());
        assertEquals(before, clientes.count());
        System.out.println("[SEG-07.5] APROBADO: límites válidos aceptados; actualización inválida conserva el registro original.");
    }
}
