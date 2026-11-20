package pe.sga.functional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import pe.sga.entity.Cliente;
import pe.sga.entity.UsuarioCliente;
import pe.sga.entity.enums.RolUsuario;

import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Pruebas de Aceptación de Negocio (E2E)")
class AcceptanceBusinessTest extends FullStackTestBase {

    private UsuarioCliente cuenta(Cliente c) {
        return usuariosPortal.saveAndFlush(
                UsuarioCliente.builder()
                        .cliente(c)
                        .username("portal" + unique())
                        .passwordHash(encoder.encode("QA_Portal123!"))
                        .estadoActivo(true)
                        .build()
        );
    }

    private String portalLogin(UsuarioCliente uc) throws Exception {
        var r = call("POST", "/api/v1/portal/auth/login", null, Map.of("username", uc.getUsername(), "password", "QA_Portal123!"));
        assertEquals(200, r.statusCode(), r.body());
        return tree(r).path("data").path("token").asText();
    }

    @Test
    @DisplayName("AC01: Prospecto se formaliza como cliente regular y cliente inactivo puede reactivarse")
    void cicloComercialCliente_formalizarProspectoYReactivarClienteInactivo() throws Exception {
        var token = login(user(RolUsuario.AGENTE_COMERCIAL));
        int id = create(token, unique());

        var before = call("GET", "/api/v1/clientes/" + id, token, null);
        assertTrue(tree(before).path("data").path("esProspecto").asBoolean());

        assertEquals(200, call("PATCH", "/api/v1/clientes/" + id + "/convertir-cliente", token, null).statusCode());
        var afterFormalizar = call("GET", "/api/v1/clientes/" + id, token, null);
        assertEquals("CLIENTE", tree(afterFormalizar).path("data").path("estadoCliente").asText());
        assertFalse(tree(afterFormalizar).path("data").path("esProspecto").asBoolean());

        assertEquals(200, call("DELETE", "/api/v1/clientes/" + id, token, null).statusCode());
        assertFalse(clientes.findById(id).orElseThrow().getEstadoActivo());

        assertEquals(200, call("PATCH", "/api/v1/clientes/" + id + "/reactivar", token, null).statusCode());
        assertTrue(clientes.findById(id).orElseThrow().getEstadoActivo());
    }

    @Test
    @DisplayName("AC02: Usuario de Portal B2B consulta su propio perfil pero tiene denegado el acceso a endpoints internos")
    void portalB2B_consultaPerfilPropioYBloqueaEndpointsInternos() throws Exception {
        var c = empresa();
        var uc = cuenta(c);
        String token = portalLogin(uc);

        var r = call("GET", "/api/v1/portal/perfil", token, null);
        assertEquals(200, r.statusCode(), r.body());
        assertEquals(c.getIdCliente().intValue(), tree(r).path("data").path("idCliente").asInt());
        assertEquals(c.getNumeroDocumento(), tree(r).path("data").path("ruc").asText());

        assertEquals(403, call("GET", "/api/v1/usuarios", token, null).statusCode());
    }

    @Test
    @DisplayName("AC03: Aislamiento multi-tenant B2B permite descargar documento al propietario y deniega (403) a otro cliente")
    void aislamientoDocumentosB2B_propietarioDescargaYClienteAjenoRecibe403() throws Exception {
        var c = empresa();
        String owner = portalLogin(cuenta(c));
        String other = portalLogin(cuenta(empresa()));
        String boundary = "QA_BOUNDARY_2026";
        String body = "--" + boundary + "\r\n"
                + "Content-Disposition: form-data; name=\"files\"; filename=\"qa-evidencia.txt\"\r\n"
                + "Content-Type: text/plain\r\n\r\n"
                + "EVIDENCIA_QA_2026\r\n"
                + "--" + boundary + "--\r\n";

        var request = HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port
                        + "/api/v1/documentos/upload?categoria=GENERAL&entidadTipo=CLIENTE&idEntidad=" + c.getIdCliente()))
                .header("Authorization", "Bearer " + owner)
                .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();

        var upload = http.send(request, HttpResponse.BodyHandlers.ofString());
        assertEquals(201, upload.statusCode(), upload.body());
        int id = tree(upload).path("data").get(0).path("idDocumento").asInt();

        var download = call("GET", "/api/v1/documentos/" + id + "/descargar", owner, null);
        assertEquals(200, download.statusCode(), download.body());
        assertEquals("EVIDENCIA_QA_2026", download.body());

        var denied = call("GET", "/api/v1/documentos/" + id + "/descargar", other, null);
        assertEquals(403, denied.statusCode(), denied.body());
        assertFalse(denied.body().contains("EVIDENCIA_QA_2026"));
    }
}
