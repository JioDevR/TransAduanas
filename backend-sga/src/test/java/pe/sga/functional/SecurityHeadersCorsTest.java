package pe.sga.functional;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.web.FilterChainProxy;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Locale;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

/** SEG-06: respuestas HTTP locales y condición HTTPS mediante la cadena real de filtros. */
@Tag("seguridad")
class SecurityHeadersCorsTest extends FullStackTestBase {
    private static final String ALLOWED = "http://localhost:5174";
    @Autowired WebApplicationContext context;
    @Autowired FilterChainProxy securityFilters;

    HttpResponse<String> cors(String origin, String method, String headers) throws Exception {
        var request = HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + "/api/v1/clientes"))
                .header("Origin", origin);
        if (method != null) {
            request.header("Access-Control-Request-Method", method);
            if (headers != null) request.header("Access-Control-Request-Headers", headers);
            request.method("OPTIONS", HttpRequest.BodyPublishers.noBody());
        } else request.GET();
        return http.send(request.build(), HttpResponse.BodyHandlers.ofString());
    }

    void denied(HttpResponse<String> response) {
        assertEquals(403, response.statusCode());
        assertTrue(response.headers().firstValue("Access-Control-Allow-Origin").isEmpty());
        assertTrue(response.headers().firstValue("Access-Control-Allow-Credentials").isEmpty());
    }

    @Test
    void SEG06_01_cabecerasEnExitoYRechazo() throws Exception {
        for (String path : new String[]{"/api/v1/health", "/api/v1/clientes"}) {
            var response = call("GET", path, null, null);
            assertEquals(path.endsWith("health") ? 200 : 401, response.statusCode());
            assertEquals("nosniff", response.headers().firstValue("X-Content-Type-Options").orElse(""));
            assertEquals("DENY", response.headers().firstValue("X-Frame-Options").orElse(""));
            assertTrue(response.headers().firstValue("Cache-Control").orElse("").contains("no-store"));
            assertTrue(response.headers().firstValue("Content-Type").orElse("").contains("application/json"));
            assertTrue(response.headers().firstValue("Access-Control-Allow-Origin").isEmpty());
            for (String name : new String[]{"Content-Security-Policy", "Referrer-Policy", "Permissions-Policy"}) {
                System.out.println("[SEG-06.INFO] " + name + ": " +
                        (response.headers().firstValue(name).isPresent() ? "presente" : "no configurada"));
            }
        }
        System.out.println("[SEG-06.1] APROBADO: JSON, nosniff, DENY y no-store en respuestas 200 y 401.");
    }

    @Test
    void SEG06_02_origenPermitidoSinOmitirAutenticacion() throws Exception {
        var preflight = cors(ALLOWED, "GET", "authorization,content-type");
        assertEquals(200, preflight.statusCode());
        assertEquals(ALLOWED, preflight.headers().firstValue("Access-Control-Allow-Origin").orElse(""));
        assertEquals("true", preflight.headers().firstValue("Access-Control-Allow-Credentials").orElse(""));
        String allowedHeaders = preflight.headers().firstValue("Access-Control-Allow-Headers").orElse("").toLowerCase(Locale.ROOT);
        assertTrue(allowedHeaders.contains("authorization") && allowedHeaders.contains("content-type"));
        assertTrue(preflight.headers().allValues("Vary").stream().anyMatch(v -> v.toLowerCase(Locale.ROOT).contains("origin")));
        var actual = cors(ALLOWED, null, null);
        assertEquals(401, actual.statusCode());
        assertEquals(ALLOWED, actual.headers().firstValue("Access-Control-Allow-Origin").orElse(""));
        System.out.println("[SEG-06.2] APROBADO: origen permitido exacto; preflight 200; recurso protegido sigue requiriendo sesión.");
    }

    @Test
    void SEG06_03_origenesNoPermitidosNoSeReflejan() throws Exception {
        for (String origin : new String[]{"https://no-permitido.example", "null", "http://localhost:51740"}) {
            denied(cors(origin, "GET", "authorization"));
            denied(cors(origin, null, null));
        }
        System.out.println("[SEG-06.3] APROBADO: origen externo ficticio, null y puerto parecido rechazados en OPTIONS y GET.");
    }

    @Test
    void SEG06_04_metodoYCabeceraFueraDeListaRechazados() throws Exception {
        denied(cors(ALLOWED, "TRACE", "authorization"));
        denied(cors(ALLOWED, "GET", "x-seg06-no-permitida"));
        System.out.println("[SEG-06.4] APROBADO: preflight de método o cabecera no permitidos recibe 403.");
    }

    @Test
    void SEG06_05_hstsCondicionadoASolicitudSegura() throws Exception {
        var mvc = MockMvcBuilders.webAppContextSetup(context).addFilters(securityFilters).build();
        var secure = mvc.perform(get("/api/v1/health").secure(true)).andReturn().getResponse();
        assertEquals(200, secure.getStatus());
        String hsts = secure.getHeader("Strict-Transport-Security");
        assertNotNull(hsts);
        assertTrue(hsts.contains("max-age=31536000"));
        assertNull(mvc.perform(get("/api/v1/health").secure(false)).andReturn()
                .getResponse().getHeader("Strict-Transport-Security"));
        System.out.println("[SEG-06.5] APROBADO: HSTS en petición marcada segura; ausente en HTTP. MockMvc, sin conexión TLS real.");
    }
}
