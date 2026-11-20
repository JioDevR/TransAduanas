package pe.sga.functional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import pe.sga.entity.enums.RolUsuario;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Pruebas de Sistema API HTTP")
class SystemApiTest extends FullStackTestBase {

    @Test
    @DisplayName("Salud y Catálogo público: endpoints abiertos responden 200 sin token")
    void endpointsPublicos_healthYCatalogoPuertos_retornan200() throws Exception {
        var health = call("GET", "/api/v1/health", null, null);
        assertEquals(200, health.statusCode());
        assertEquals("UP", tree(health).path("data").path("status").asText());

        var puertos = call("GET", "/api/v1/catalogos/puertos", null, null);
        assertEquals(200, puertos.statusCode(), puertos.body());
        assertTrue(tree(puertos).path("data").isArray());
    }

    @Test
    @DisplayName("Autenticación HTTP: credenciales inválidas retornan 401 sin exponer stackTrace")
    void authLogin_credencialesIncorrectas_retorna401SinStackTrace() throws Exception {
        var u = user(RolUsuario.ADMINISTRADOR_TI);
        var r = call("POST", "/api/v1/auth/login", null, Map.of("username", u.getUsername(), "password", "INCORRECTA"));
        assertEquals(401, r.statusCode(), r.body());
        assertFalse(tree(r).path("success").asBoolean());
        assertFalse(r.body().contains("stackTrace"));
    }

    @Test
    @DisplayName("Seguridad HTTP: petición sin token o con token inválido retorna 401 JSON y rol sin permiso retorna 403")
    void seguridadHttp_sinTokenTokenInvalidoYRolSinPermiso_retornanCodigosEsperados() throws Exception {
        var sinToken = call("GET", "/api/v1/clientes", null, null);
        assertEquals(401, sinToken.statusCode());
        assertTrue(sinToken.headers().firstValue("Content-Type").orElse("").contains("application/json"));
        assertFalse(tree(sinToken).path("success").asBoolean());

        var tokenInvalido = call("GET", "/api/v1/clientes", "token-invalido", null);
        assertEquals(401, tokenInvalido.statusCode());

        var comercial = user(RolUsuario.AGENTE_COMERCIAL);
        var sinPermiso = call("GET", "/api/v1/usuarios", login(comercial), null);
        assertEquals(403, sinPermiso.statusCode(), sinPermiso.body());
        assertFalse(tree(sinPermiso).path("success").asBoolean());
    }

    @Test
    @DisplayName("Clientes HTTP: ciclo CRUD completo (crear, consultar, actualizar y baja lógica)")
    void crudClienteCompleto_creaActualizaYDesactivaLogicamente() throws Exception {
        var token = login(user(RolUsuario.AGENTE_COMERCIAL));
        String doc = unique();
        int id = create(token, doc);

        var getRes = call("GET", "/api/v1/clientes/" + id, token, null);
        assertEquals(200, getRes.statusCode());
        assertEquals(doc, tree(getRes).path("data").path("numeroDocumento").asText());

        var updateDto = dto(doc);
        updateDto.setRazonSocial("QA HTTP Actualizada");
        assertEquals(200, call("PUT", "/api/v1/clientes/" + id, token, updateDto).statusCode());
        assertEquals("QA HTTP Actualizada", clientes.findById(id).orElseThrow().getRazonSocial());

        assertEquals(200, call("DELETE", "/api/v1/clientes/" + id, token, null).statusCode());
        var afterDelete = call("GET", "/api/v1/clientes/" + id, token, null);
        assertFalse(tree(afterDelete).path("data").path("estadoActivo").asBoolean());
        assertTrue(clientes.existsById(id));
    }

    @Test
    @DisplayName("Validación y Errores HTTP: correo/longitud inválidos o duplicado retornan 400 y recurso inexistente retorna 404")
    void validacionesYErroresHttp_retornan400Y404() throws Exception {
        var token = login(user(RolUsuario.AGENTE_COMERCIAL));

        String docCorreoInvalido = unique();
        var dtoCorreoInvalido = dto(docCorreoInvalido);
        dtoCorreoInvalido.setCorreoContacto("INVALIDO");
        var resCorreo = call("POST", "/api/v1/clientes", token, dtoCorreoInvalido);
        assertEquals(400, resCorreo.statusCode(), resCorreo.body());
        assertFalse(clientes.existsByNumeroDocumento(docCorreoInvalido));
        assertTrue(tree(resCorreo).path("data").has("correoContacto"));

        var dtoLongitudInvalida = dto("1234567890123456");
        var resLongitud = call("POST", "/api/v1/clientes", token, dtoLongitudInvalida);
        assertEquals(400, resLongitud.statusCode(), resLongitud.body());

        String docDup = unique();
        create(token, docDup);
        var resDup = call("POST", "/api/v1/clientes", token, dto(docDup));
        assertEquals(400, resDup.statusCode(), resDup.body());

        var res404 = call("GET", "/api/v1/clientes/2147483647", token, null);
        assertEquals(404, res404.statusCode(), res404.body());
        assertFalse(tree(res404).path("success").asBoolean());
    }
}
