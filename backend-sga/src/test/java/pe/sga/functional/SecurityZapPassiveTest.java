package pe.sga.functional;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import java.net.*;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.TimeUnit;
import static org.junit.jupiter.api.Assertions.*;

/** SEG-09: requiere ZAP local, se activa expresamente para no imponerlo a la suite habitual. */
@Tag("seguridad")
@EnabledIfEnvironmentVariable(named = "SEG09_ZAP_JAR", matches = ".+")
class SecurityZapPassiveTest extends FullStackTestBase {
    private int zapPort;
    private final String apiKey = UUID.randomUUID().toString();

    JsonNode api(String endpoint, String params) throws Exception {
        String url = "http://127.0.0.1:" + zapPort + "/JSON/" + endpoint + "/?apikey=" + apiKey;
        if (params != null && !params.isEmpty()) url += "&" + params;
        var response = http.send(HttpRequest.newBuilder(URI.create(url))
                .timeout(Duration.ofSeconds(10)).GET().build(), HttpResponse.BodyHandlers.ofString());
        assertEquals(200, response.statusCode(), "La API local de ZAP debe responder");
        var result = json.readTree(response.body());
        assertFalse(result.has("code"), "ZAP no debe devolver un error de API");
        return result;
    }

    @Test
    void SEG09_analisisPasivoLocalSinAtaquesActivos() throws Exception {
        Path jar = Path.of(System.getenv("SEG09_ZAP_JAR")).toAbsolutePath();
        assertTrue(Files.isRegularFile(jar), "SEG09_ZAP_JAR debe apuntar al JAR instalado de ZAP");
        Path output = Path.of("target/seg09-zap").toAbsolutePath();
        Files.createDirectories(output);
        Path profile = Files.createTempDirectory(output, "perfil-local-");
        try (var socket = new java.net.ServerSocket(0, 0, InetAddress.getLoopbackAddress())) {
            zapPort = socket.getLocalPort();
        }
        String java = Path.of(System.getProperty("java.home"), "bin", "java.exe").toString();
        var process = new ProcessBuilder(java, "-Djava.awt.headless=true", "-jar", jar.toString(),
                "-daemon", "-silent", "-host", "127.0.0.1", "-port", Integer.toString(zapPort),
                "-dir", profile.toString(), "-config", "api.key=" + apiKey)
                .directory(jar.getParent().toFile()).redirectErrorStream(true)
                .redirectOutput(profile.resolve("proceso-local.log").toFile()).start();
        boolean ready = false;
        try {
            long deadline = System.nanoTime() + Duration.ofSeconds(60).toNanos();
            String version = "";
            while (System.nanoTime() < deadline && process.isAlive()) {
                try {
                    version = api("core/view/version", "").path("version").asText();
                    if (!version.isBlank()) { ready = true; break; }
                } catch (Exception ignored) { }
                Thread.sleep(250);
            }
            assertTrue(ready, "ZAP no inició; revisar el registro local privado del proceso");
            api("core/action/setMode", "mode=safe");
            api("pscan/action/setEnabled", "enabled=true");
            var scanners = api("pscan/view/scanners", "").path("scanners");
            int enabled = 0;
            for (var scanner : scanners) if (scanner.path("enabled").asBoolean()) enabled++;
            assertTrue(enabled > 0, "Deben existir reglas pasivas habilitadas");

            String base = "http://127.0.0.1:" + port;
            var proxy = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10))
                    .proxy(ProxySelector.of(new InetSocketAddress("127.0.0.1", zapPort)))
                    .version(HttpClient.Version.HTTP_1_1).build();
            var traffic = new ArrayList<Map<String, Object>>();
            var paths = new LinkedHashMap<String, Integer>();
            paths.put("/api/v1/health", 200);
            paths.put("/api/v1/catalogos/puertos", 200);
            paths.put("/api/v1/catalogos/tipos-carga", 200);
            paths.put("/api/v1/catalogos/mercancias", 200);
            paths.put("/api/v1/clientes", 401);
            paths.put("/api/v1/portal/perfil", 401);
            for (var endpoint : paths.entrySet()) {
                var response = proxy.send(HttpRequest.newBuilder(URI.create(base + endpoint.getKey()))
                        .timeout(Duration.ofSeconds(15)).GET().build(), HttpResponse.BodyHandlers.ofString());
                assertEquals(endpoint.getValue().intValue(), response.statusCode());
                traffic.add(Map.of("ruta", endpoint.getKey(), "estado", response.statusCode()));
            }
            String baseParam = "baseurl=" + URLEncoder.encode(base, StandardCharsets.UTF_8);
            int messages = api("core/view/numberOfMessages", baseParam).path("numberOfMessages").asInt();
            assertTrue(messages >= paths.size(), "ZAP debe haber registrado el tráfico: no basta con pedir las URLs directamente");
            deadline = System.nanoTime() + Duration.ofSeconds(60).toNanos();
            int pending;
            int idle = 0;
            do {
                Thread.sleep(500);
                pending = api("pscan/view/recordsToScan", "").path("recordsToScan").asInt(-1);
                idle = pending == 0 ? idle + 1 : 0;
            } while (idle < 3 && System.nanoTime() < deadline);
            assertEquals(0, pending, "El análisis pasivo debe finalizar");
            assertEquals(3, idle, "La cola debe permanecer vacía antes de recoger alertas");
            int active = api("ascan/view/scans", "").path("scans").size();
            int spiders = api("spider/view/scans", "").path("scans").size();
            assertEquals(0, active);
            assertEquals(0, spiders);

            var alerts = api("core/view/alerts", baseParam + "&start=0&count=10000").path("alerts");
            var sanitized = new ArrayList<Map<String, Object>>();
            int high = 0, medium = 0, low = 0, info = 0;
            for (JsonNode alert : alerts) {
                String risk = alert.path("risk").asText();
                switch (risk.toLowerCase(Locale.ROOT)) {
                    case "high" -> high++;
                    case "medium" -> medium++;
                    case "low" -> low++;
                    default -> info++;
                }
                var row = new LinkedHashMap<String, Object>();
                for (String field : new String[]{"pluginId", "name", "risk", "confidence", "url", "cweid", "solution"}) {
                    row.put(field, alert.path(field).asText());
                }
                sanitized.add(row);
            }
            var report = new LinkedHashMap<String, Object>();
            report.put("prueba", "SEG-09");
            report.put("zapVersion", version);
            report.put("modo", "safe; pasivo; sin spider ni escaneo activo");
            report.put("reglasPasivasHabilitadas", enabled);
            report.put("mensajesRegistrados", messages);
            report.put("colaPendiente", pending);
            report.put("escaneosActivos", active);
            report.put("spiders", spiders);
            report.put("solicitudes", traffic);
            report.put("conteos", Map.of("altas", high, "medias", medium, "bajas", low, "informativas", info));
            report.put("alertas", sanitized);
            report.put("resultado", high + medium == 0 ? "APROBADA con revisión de alertas restantes" : "CON HALLAZGOS: revisar alertas altas/medias");
            report.put("alcance", "Seis rutas JSON del backend sin sesión; no frontend, no rutas autenticadas, no TLS real");
            Files.writeString(output.resolve("resultado.json"), json.writerWithDefaultPrettyPrinter().writeValueAsString(report));
            System.out.printf("[SEG-09] ZAP %s; reglas=%d; mensajes=%d; cola=%d; altas=%d; medias=%d; bajas=%d; informativas=%d%n",
                    version, enabled, messages, pending, high, medium, low, info);
            assertEquals(0, high + medium, "ZAP detectó alertas altas/medias; se conserva el informe para revisarlas");
        } finally {
            if (ready) {
                try { api("core/action/shutdown", ""); } catch (Exception ignored) { }
            }
            if (!process.waitFor(10, TimeUnit.SECONDS)) {
                process.destroy();
                if (!process.waitFor(3, TimeUnit.SECONDS)) process.destroyForcibly();
            }
        }
    }
}
