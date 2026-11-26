package pe.sga.functional;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import pe.sga.entity.enums.RolUsuario;
import java.net.URI;
import java.net.http.*;
import java.nio.file.*;
import java.time.Duration;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

/** SEG-10: ensayo local pequeño y secuencial; no es una prueba de estrés ni de denegación de servicio. */
@Tag("seguridad")
@EnabledIfEnvironmentVariable(named = "SEG10_RUN", matches = "true")
class SecurityControlledPerformanceTest extends FullStackTestBase {
    private static final int ROUNDS = 10;
    private static final long INTERVAL_MS = 100;
    private static final double P95_LIMIT_MS = 2000;
    private static final double MAX_LIMIT_MS = 5000;
    record Sample(String ruta, int estado, double ms, boolean contenidoValido, String error) { }

    Sample measure(String path, String token) {
        long start = System.nanoTime();
        try {
            var request = HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + path))
                    .timeout(Duration.ofSeconds(5));
            if (token != null) request.header("Authorization", "Bearer " + token);
            var response = http.send(request.GET().build(), HttpResponse.BodyHandlers.ofString());
            double elapsed = (System.nanoTime() - start) / 1_000_000.0;
            var body = json.readTree(response.body());
            boolean valid = body.path("success").asBoolean() &&
                    (path.endsWith("health") ? "UP".equals(body.path("data").path("status").asText())
                            : body.path("data").isArray());
            return new Sample(path, response.statusCode(), elapsed, valid, "");
        } catch (Exception ex) {
            return new Sample(path, 0, (System.nanoTime() - start) / 1_000_000.0,
                    false, ex.getClass().getSimpleName());
        }
    }

    Map<String, Object> statistics(List<Sample> samples) {
        var times = samples.stream().mapToDouble(Sample::ms).sorted().toArray();
        int n = times.length;
        return Map.of("muestras", n, "minMs", times[0], "promedioMs", Arrays.stream(times).average().orElseThrow(),
                "p50Ms", times[(int) Math.ceil(0.50 * n) - 1],
                "p95Ms", times[(int) Math.ceil(0.95 * n) - 1], "maxMs", times[n - 1]);
    }

    boolean controls() throws Exception {
        var health = call("GET", "/api/v1/health", null, null);
        var denied = call("GET", "/api/v1/clientes", null, null);
        return health.statusCode() == 200 && "UP".equals(tree(health).path("data").path("status").asText())
                && denied.statusCode() == 401 && !tree(denied).path("success").asBoolean()
                && tree(denied).path("data").isNull();
    }

    @Test
    void SEG10_rendimientoSecuencialLimitadoYDisponibilidad() throws Exception {
        // Una cuenta ficticia, una sesión. No se imprime ni se exporta la credencial o el token.
        String token = login(user(RolUsuario.AGENTE_COMERCIAL));
        long clientRows = clientes.count(), userRows = usuarios.count();
        boolean before = controls();
        assertTrue(before, "Salud y autorización deben funcionar antes del ensayo");
        List<String> paths = List.of("/api/v1/health", "/api/v1/catalogos/puertos", "/api/v1/clientes");
        for (String path : paths) {
            var warmup = measure(path, path.endsWith("clientes") ? token : null);
            assertEquals(200, warmup.estado(), "Debe funcionar el calentamiento");
            assertTrue(warmup.contenidoValido());
            Thread.sleep(INTERVAL_MS);
        }
        var samples = new ArrayList<Sample>();
        long start = System.nanoTime();
        outer: for (int round = 0; round < ROUNDS; round++) {
            for (String path : paths) {
                Sample sample = measure(path, path.endsWith("clientes") ? token : null);
                samples.add(sample);
                // Detener el ensayo si aparece un error: no insistir sobre un servicio que falla.
                if (sample.estado() != 200 || !sample.contenidoValido()) break outer;
                Thread.sleep(INTERVAL_MS);
            }
        }
        double durationSeconds = (System.nanoTime() - start) / 1_000_000_000.0;
        boolean after = controls();
        boolean unchanged = clientRows == clientes.count() && userRows == usuarios.count();
        var total = statistics(samples);
        double p95 = ((Number) total.get("p95Ms")).doubleValue();
        double max = ((Number) total.get("maxMs")).doubleValue();
        long errors = samples.stream().filter(s -> s.estado() != 200 || !s.contenidoValido()).count();
        boolean passed = samples.size() == 30 && errors == 0 && after && unchanged
                && p95 <= P95_LIMIT_MS && max <= MAX_LIMIT_MS;
        var grouped = new LinkedHashMap<String, Object>();
        for (String path : paths) {
            var matching = samples.stream().filter(s -> s.ruta().equals(path)).toList();
            if (!matching.isEmpty()) grouped.put(path, statistics(matching));
        }
        var report = new LinkedHashMap<String, Object>();
        report.put("prueba", "SEG-10");
        report.put("resultado", passed ? "APROBADA" : "NO APROBADA: revisar mediciones y controles");
        report.put("entorno", "HTTP local, H2 temporal, un cliente secuencial, datos ficticios");
        report.put("criteriosReferencia", Map.of("p95MaxMs", P95_LIMIT_MS, "respuestaMaxMs", MAX_LIMIT_MS,
                "erroresPermitidos", 0, "muestrasEsperadas", 30));
        report.put("intervaloEntreSolicitudesMs", INTERVAL_MS);
        report.put("calentamientoNoMedido", 3);
        report.put("solicitudesAuxiliaresNoMedidas", 5);
        report.put("duracionMedicionSegundosIncluyePausas", durationSeconds);
        report.put("solicitudesMedidasPorSegundoIncluyePausas", samples.size() / durationSeconds);
        report.put("estadisticas", total);
        report.put("porRuta", grouped);
        report.put("errores", errors);
        report.put("controlesAntes", before);
        report.put("controlesDespues", after);
        report.put("cantidadesRegistrosSinCambios", unchanged);
        report.put("muestras", samples);
        report.put("limites", "Umbrales de referencia de este ensayo, no SLA acordado. Sin concurrencia, estrés, saturación, MySQL, frontend, TLS ni entorno de producción.");
        Path output = Path.of("target/seg10-rendimiento");
        Files.createDirectories(output);
        Files.writeString(output.resolve("resultado.json"), json.writerWithDefaultPrettyPrinter().writeValueAsString(report));
        var csv = new StringBuilder("ruta,estado,milisegundos,contenidoValido,error\n");
        for (var sample : samples) csv.append(String.format(Locale.ROOT, "%s,%d,%.6f,%s,%s%n",
                sample.ruta(), sample.estado(), sample.ms(), sample.contenidoValido(), sample.error()));
        Files.writeString(output.resolve("mediciones.csv"), csv.toString());
        System.out.printf(Locale.ROOT,
                "[SEG-10] muestras=%d; errores=%d; promedio=%.2f ms; p95=%.2f ms; max=%.2f ms; controles=%s; registros=%s%n",
                samples.size(), errors, ((Number) total.get("promedioMs")).doubleValue(), p95, max, after, unchanged);
        assertTrue(passed, "El ensayo no cumple los criterios; revisar el informe sin modificar los umbrales para ocultar el fallo");
    }
}
