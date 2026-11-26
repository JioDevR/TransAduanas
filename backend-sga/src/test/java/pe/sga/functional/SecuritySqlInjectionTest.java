package pe.sga.functional;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import pe.sga.entity.UsuarioCliente;
import pe.sga.entity.enums.RolUsuario;
import pe.sga.security.TemporaryCredentials;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.net.http.HttpResponse;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

/** SEG-04: entradas no destructivas contra HTTP local y H2 temporal. */
@Tag("seguridad")
class SecuritySqlInjectionTest extends FullStackTestBase {
    private static final String INPUT = "' OR '1'='1";

    private String encoded(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8).replace("+", "%20");
    }

    private void sinDetallesSql(HttpResponse<String> response) {
        String body = response.body().toLowerCase(java.util.Locale.ROOT);
        for (String diagnostic : new String[]{"jdbc:", "sqlstate", "org.hibernate",
                "syntax error", "java.sql.", "org.h2.", "sqlgrammar"}) {
            assertFalse(body.contains(diagnostic), "La respuesta no debe exponer detalles del motor SQL");
        }
    }

    private void loginRechazado(String path, String username, String password) throws Exception {
        var response = call("POST", path, null, Map.of("username", username, "password", password));
        assertEquals(401, response.statusCode());
        assertFalse(tree(response).path("success").asBoolean());
        assertTrue(tree(response).path("data").path("token").asText().isBlank());
        sinDetallesSql(response);
    }

    @Test
    void SEG04_01_loginInternoNoOmiteCredenciales() throws Exception {
        var account = user(RolUsuario.AGENTE_COMERCIAL);
        long before = usuarios.count();
        assertFalse(login(account).isBlank());
        loginRechazado("/api/v1/auth/login", INPUT, TemporaryCredentials.generate());
        loginRechazado("/api/v1/auth/login", account.getUsername(), INPUT);
        assertEquals(before, usuarios.count());
        assertFalse(login(account).isBlank());
        System.out.println("[SEG-04.1] APROBADO: login interno rechaza ambas entradas con 401; acceso válido funciona.");
    }

    @Test
    void SEG04_02_loginPortalNoOmiteCredenciales() throws Exception {
        String password = TemporaryCredentials.generate();
        var account = usuariosPortal.saveAndFlush(UsuarioCliente.builder()
                .cliente(empresa()).username("seg04_" + unique())
                .passwordHash(encoder.encode(password)).estadoActivo(true)
                .debeCambiarPassword(false).build());
        String path = "/api/v1/portal/auth/login";
        assertEquals(200, call("POST", path, null,
                Map.of("username", account.getUsername(), "password", password)).statusCode());
        long before = usuariosPortal.count();
        loginRechazado(path, INPUT, password);
        loginRechazado(path, account.getUsername(), INPUT);
        assertEquals(before, usuariosPortal.count());
        assertEquals(200, call("POST", path, null,
                Map.of("username", account.getUsername(), "password", password)).statusCode());
        System.out.println("[SEG-04.2] APROBADO: portal rechaza ambas entradas con 401 y conserva el acceso válido.");
    }

    @Test
    void SEG04_03_busquedaInterpretaEntradaComoTextoLiteral() throws Exception {
        String token = login(user(RolUsuario.AGENTE_COMERCIAL));
        var matching = empresa();
        String term = "SEG04_" + unique() + " " + INPUT;
        matching.setRazonSocial("Empresa ficticia " + term);
        clientes.saveAndFlush(matching);
        var control = empresa();
        long before = clientes.count();
        var response = call("GET", "/api/v1/clientes?buscar=" + encoded(term), token, null);
        assertEquals(200, response.statusCode());
        assertEquals(1, tree(response).path("data").size());
        assertEquals(matching.getIdCliente().intValue(),
                tree(response).path("data").get(0).path("idCliente").asInt());
        sinDetallesSql(response);
        var empty = call("GET", "/api/v1/clientes?buscar=" + encoded(term + "_SIN_COINCIDENCIA"), token, null);
        assertEquals(200, empty.statusCode());
        assertEquals(0, tree(empty).path("data").size());
        assertEquals(before, clientes.count());
        assertEquals(control.getRazonSocial(), clientes.findById(control.getIdCliente()).orElseThrow().getRazonSocial());
        System.out.println("[SEG-04.3] APROBADO: búsqueda literal devuelve solo la coincidencia, no amplía resultados ni modifica registros.");
    }

    @Test
    void SEG04_04_documentoNoAmpliaConsultaNiExponeSql() throws Exception {
        String token = login(user(RolUsuario.AGENTE_COMERCIAL));
        var control = empresa();
        String path = "/api/v1/clientes/documento/";
        assertEquals(200, call("GET", path + encoded(control.getNumeroDocumento()), token, null).statusCode());
        long before = clientes.count();
        var response = call("GET", path + encoded(INPUT), token, null);
        assertEquals(404, response.statusCode());
        assertFalse(tree(response).path("success").asBoolean());
        sinDetallesSql(response);
        assertEquals(before, clientes.count());
        assertEquals(200, call("GET", path + encoded(control.getNumeroDocumento()), token, null).statusCode());
        System.out.println("[SEG-04.4] APROBADO: consulta por documento rechaza entrada ajena con 404; registro válido sigue accesible.");
    }
}
