package pe.sga.functional;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import pe.sga.entity.enums.RolUsuario;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@Tag("sistema")
class AdminTiFeaturesTest extends FullStackTestBase {

    @Test
    void TI01_healthCheckIncluyeBdReal() throws Exception {
        var r = call("GET", "/api/v1/health", null, null);
        assertEquals(200, r.statusCode(), r.body());
        var data = tree(r).path("data");
        assertEquals("UP", data.path("status").asText());
        assertEquals("CONECTADA", data.path("dbStatus").asText());
        assertTrue(data.path("dbLatenciaMs").asLong() >= 1L);
    }

    @Test
    void TI02_adminDashboardYEstadoSistema() throws Exception {
        var admin = user(RolUsuario.ADMINISTRADOR_TI);
        var token = login(admin);

        var dash = call("GET", "/api/v1/admin/sistema/dashboard?dias=7", token, null);
        assertEquals(200, dash.statusCode(), dash.body());
        var d = tree(dash).path("data");
        assertTrue(d.path("sistema").path("bdConectada").asBoolean());
        assertTrue(d.path("usuariosTotal").asLong() >= 1L);
        assertTrue(d.path("actividadDiaria").isArray());
        assertEquals(7, d.path("actividadDiaria").size());

        var est = call("GET", "/api/v1/admin/sistema/estado", token, null);
        assertEquals(200, est.statusCode(), est.body());
        assertTrue(tree(est).path("data").path("jvmHeapMaximoMb").asLong() > 0);
    }

    @Test
    void TI03_matrizRolesCompleta() throws Exception {
        var admin = user(RolUsuario.ADMINISTRADOR_TI);
        var token = login(admin);

        var r = call("GET", "/api/v1/admin/sistema/roles-matriz", token, null);
        assertEquals(200, r.statusCode(), r.body());
        var roles = tree(r).path("data");
        assertTrue(roles.isArray());
        assertEquals(RolUsuario.values().length, roles.size());
    }

    @Test
    void TI04_resetPasswordInternoYLoginNuevo() throws Exception {
        var admin = user(RolUsuario.ADMINISTRADOR_TI);
        var operador = user(RolUsuario.AGENTE_COMERCIAL);
        var adminToken = login(admin);

        var reset = call("PATCH", "/api/v1/usuarios/" + operador.getIdUsuario() + "/reset-password",
                adminToken, Map.of("nuevaPassword", "NuevaClave2026!"));
        assertEquals(200, reset.statusCode(), reset.body());

        var loginViejo = call("POST", "/api/v1/auth/login", null,
                Map.of("username", operador.getUsername(), "password", "QA_Test123!"));
        assertEquals(401, loginViejo.statusCode(), loginViejo.body());

        var loginNuevo = call("POST", "/api/v1/auth/login", null,
                Map.of("username", operador.getUsername(), "password", "NuevaClave2026!"));
        assertEquals(200, loginNuevo.statusCode(), loginNuevo.body());
        assertFalse(tree(loginNuevo).path("data").path("token").asText().isBlank());
    }

    @Test
    void TI05_auditoriaRegistraLoginYResetPassword() throws Exception {
        var admin = user(RolUsuario.ADMINISTRADOR_TI);
        var token = login(admin);

        // Provocar intento fallido para auditarlo
        call("POST", "/api/v1/auth/login", null,
                Map.of("username", admin.getUsername(), "password", "ClaveIncorrecta!"));

        var aud = call("GET", "/api/v1/admin/auditoria?modulo=SEGURIDAD_TI&size=20", token, null);
        assertEquals(200, aud.statusCode(), aud.body());
        var items = tree(aud).path("data").path("items");
        assertTrue(items.isArray());
        assertTrue(items.size() >= 2);
        boolean tieneFallo = false;
        for (var item : items) {
            if ("LOGIN_FALLIDO".equals(item.path("accion").asText())
                    && "FALLO".equals(item.path("resultado").asText())) {
                tieneFallo = true;
                break;
            }
        }
        assertTrue(tieneFallo, "Debe existir un evento LOGIN_FALLIDO registrado");
    }

    @Test
    void TI06_permisosSoloAdminTiEnDashboardYAuditoria() throws Exception {
        var comercial = user(RolUsuario.AGENTE_COMERCIAL);
        var tokenCom = login(comercial);
        assertEquals(403, call("GET", "/api/v1/admin/sistema/dashboard", tokenCom, null).statusCode());
        assertEquals(403, call("GET", "/api/v1/admin/auditoria", tokenCom, null).statusCode());

        var gerente = user(RolUsuario.GERENTE_GENERAL);
        var tokenGer = login(gerente);
        assertEquals(403, call("GET", "/api/v1/admin/sistema/dashboard", tokenGer, null).statusCode());
        assertEquals(200, call("GET", "/api/v1/admin/sistema/estado", tokenGer, null).statusCode());
        assertEquals(200, call("GET", "/api/v1/admin/sistema/roles-matriz", tokenGer, null).statusCode());
    }

    @Test
    void TI07_auditoriaRegistraOperacionesDeNegocioPorServicio() throws Exception {
        var comercial = user(RolUsuario.AGENTE_COMERCIAL);
        var tokenCom = login(comercial);
        String doc = unique();
        int idCliente = create(tokenCom, doc);
        assertTrue(idCliente > 0);

        var admin = user(RolUsuario.ADMINISTRADOR_TI);
        var tokenAdmin = login(admin);
        var aud = call("GET", "/api/v1/admin/auditoria?modulo=COMERCIAL&size=20", tokenAdmin, null);
        assertEquals(200, aud.statusCode(), aud.body());
        var items = tree(aud).path("data").path("items");
        assertTrue(items.isArray());

        boolean encontroCrearCliente = false;
        for (var item : items) {
            if ("CREAR_CLIENTE".equals(item.path("accion").asText())
                    && "CLIENTE".equals(item.path("entidad").asText())
                    && item.path("idEntidad").asInt() == idCliente
                    && "EXITO".equals(item.path("resultado").asText())) {
                encontroCrearCliente = true;
                break;
            }
        }
        assertTrue(encontroCrearCliente, "Debe registrarse CREAR_CLIENTE en Auditoría TI bajo módulo COMERCIAL");
    }

    @Test
    void TI08_diagnosticoAlmacenamientoYDepuracion() throws Exception {
        var comercial = user(RolUsuario.AGENTE_COMERCIAL);
        var tokenCom = login(comercial);
        assertEquals(403, call("GET", "/api/v1/admin/sistema/almacenamiento", tokenCom, null).statusCode());

        var gerente = user(RolUsuario.GERENTE_GENERAL);
        var tokenGer = login(gerente);
        assertEquals(200, call("GET", "/api/v1/admin/sistema/almacenamiento", tokenGer, null).statusCode());
        assertEquals(403, call("POST", "/api/v1/admin/sistema/almacenamiento/depurar-huerfanos", tokenGer, null).statusCode());

        var admin = user(RolUsuario.ADMINISTRADOR_TI);
        var tokenAdmin = login(admin);
        var diag = call("GET", "/api/v1/admin/sistema/almacenamiento?page=0&size=10", tokenAdmin, null);
        assertEquals(200, diag.statusCode(), diag.body());
        var data = tree(diag).path("data");
        assertTrue(data.has("directorioRaiz"));
        assertTrue(data.path("porEntidad").isArray());
        assertTrue(data.path("porExtension").isArray());
        assertTrue(data.path("items").isArray());

        var purge = call("POST", "/api/v1/admin/sistema/almacenamiento/depurar-huerfanos", tokenAdmin, null);
        assertEquals(200, purge.statusCode(), purge.body());
        assertTrue(tree(purge).path("data").has("archivosEliminados"));
    }
}
