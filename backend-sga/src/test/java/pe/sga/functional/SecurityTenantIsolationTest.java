package pe.sga.functional;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import pe.sga.entity.*;
import pe.sga.entity.enums.EstadoCotizacion;
import pe.sga.dto.portal.PortalCotizacionSolicitudDTO;
import pe.sga.repository.*;
import pe.sga.security.TemporaryCredentials;
import pe.sga.service.IPortalService;
import java.math.BigDecimal;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

/** SEG-03: HTTP local, dos empresas ficticias y persistencia H2 real. */
@Tag("seguridad")
class SecurityTenantIsolationTest extends FullStackTestBase {
    @Autowired IPortalService portal;
    @Autowired CotizacionRepository cotizaciones;
    @Autowired NotificacionClienteRepository notificaciones;

    String acceso(Cliente cliente) throws Exception {
        String password = TemporaryCredentials.generate();
        var account = usuariosPortal.saveAndFlush(UsuarioCliente.builder()
                .cliente(cliente).username("seg03_" + unique())
                .passwordHash(encoder.encode(password)).estadoActivo(true)
                .debeCambiarPassword(false).build());
        var response = call("POST", "/api/v1/portal/auth/login", null,
                Map.of("username", account.getUsername(), "password", password));
        assertEquals(200, response.statusCode());
        String token = tree(response).path("data").path("token").asText();
        assertFalse(token.isBlank());
        return token;
    }

    Cotizacion cotizacion(Cliente cliente) {
        var dto = PortalCotizacionSolicitudDTO.builder()
                .regimenAduanero("IMPORTACION_DEFINITIVA")
                .valorFob(new BigDecimal("1000.00")).build();
        int id = portal.solicitarCotizacion(cliente.getIdCliente(), dto, null).getIdCotizacion();
        var entity = cotizaciones.findById(id).orElseThrow();
        entity.setEstado(EstadoCotizacion.APROBADA);
        entity.setTotalCotizado(new BigDecimal("100.00"));
        return cotizaciones.saveAndFlush(entity);
    }

    @Test
    void SEG03_01_consultaYListadoSoloDelPropietario() throws Exception {
        var a = empresa(); var b = empresa();
        String owner = acceso(a), other = acceso(b);
        var quoteA = cotizacion(a); var quoteB = cotizacion(b);
        String path = "/api/v1/portal/cotizaciones/" + quoteA.getIdCotizacion();
        assertEquals(200, call("GET", path, owner, null).statusCode());
        var denied = call("GET", path, other, null);
        assertEquals(404, denied.statusCode());
        assertFalse(denied.body().contains(quoteA.getCodigoCotizacion()));
        var list = call("GET", "/api/v1/portal/cotizaciones", other, null);
        assertEquals(200, list.statusCode());
        boolean ownFound = false;
        for (var item : tree(list).path("data")) {
            assertNotEquals(quoteA.getIdCotizacion().intValue(), item.path("idCotizacion").asInt());
            if (item.path("idCotizacion").asInt() == quoteB.getIdCotizacion()) ownFound = true;
        }
        assertTrue(ownFound, "El listado debe conservar la operación propia");
        assertEquals(401, call("GET", path, null, null).statusCode());
        System.out.println("[SEG-03.1] APROBADO: propietario 200, otra empresa 404, listado aislado, sin sesión 401.");
    }

    @Test
    void SEG03_02_otraEmpresaNoAceptaNiModificaCotizacion() throws Exception {
        var a = empresa(); String owner = acceso(a), other = acceso(empresa());
        var quote = cotizacion(a);
        String before = quote.getObservaciones();
        String path = "/api/v1/portal/cotizaciones/" + quote.getIdCotizacion() + "/aceptar";
        assertEquals(404, call("PATCH", path, other, null).statusCode());
        var unchanged = cotizaciones.findById(quote.getIdCotizacion()).orElseThrow();
        assertEquals(before, unchanged.getObservaciones());
        assertEquals(quote.getEstado(), unchanged.getEstado());
        assertEquals(quote.getEstadoPago(), unchanged.getEstadoPago());
        assertEquals(200, call("PATCH", path, owner, null).statusCode());
        assertTrue(cotizaciones.findById(quote.getIdCotizacion()).orElseThrow()
                .getObservaciones().contains("Aceptada formalmente por el cliente"));
        System.out.println("[SEG-03.2] APROBADO: otra empresa 404 sin modificación; propietario acepta con 200.");
    }

    @Test
    void SEG03_03_notificacionAjenaNoPuedeLeerseNiMarcarse() throws Exception {
        var a = empresa(); String owner = acceso(a), other = acceso(empresa());
        String marker = "CONTENIDO_FICTICIO_SEG03_" + unique();
        var notification = notificaciones.saveAndFlush(NotificacionCliente.builder()
                .cliente(a).titulo("Prueba de aislamiento").mensaje(marker).leido(false).build());
        String path = "/api/v1/portal/notificaciones/" + notification.getIdNotificacion() + "/leer";
        var list = call("GET", "/api/v1/portal/notificaciones", other, null);
        assertEquals(200, list.statusCode());
        assertFalse(list.body().contains(marker));
        assertEquals(404, call("PATCH", path, other, null).statusCode());
        assertFalse(notificaciones.findById(notification.getIdNotificacion()).orElseThrow().getLeido());
        assertEquals(200, call("PATCH", path, owner, null).statusCode());
        assertTrue(notificaciones.findById(notification.getIdNotificacion()).orElseThrow().getLeido());
        System.out.println("[SEG-03.3] APROBADO: notificación aislada, cambio ajeno rechazado y propietario autorizado.");
    }
}
