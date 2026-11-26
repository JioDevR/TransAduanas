package pe.sga.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pe.sga.dto.response.ApiResponseDTO;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/health")
public class HealthController {

    private DataSource dataSource;

    @Autowired(required = false)
    public void setDataSource(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @GetMapping
    public ResponseEntity<ApiResponseDTO<Map<String, Object>>> checkHealth() {
        boolean bdOk = true;
        String dbStatus = "NO_VERIFICADA";
        long dbLatenciaMs = 0L;

        if (dataSource != null) {
            long t0 = System.nanoTime();
            try (Connection conn = dataSource.getConnection();
                 Statement stmt = conn.createStatement()) {
                stmt.execute("SELECT 1");
                dbLatenciaMs = Math.max(1L, (System.nanoTime() - t0) / 1_000_000L);
                dbStatus = "CONECTADA";
            } catch (Exception ex) {
                bdOk = false;
                dbStatus = "DESCONECTADA";
            }
        }

        Map<String, Object> healthInfo = new LinkedHashMap<>();
        healthInfo.put("status", bdOk ? "UP" : "DEGRADED");
        healthInfo.put("app", "SGA Backend API");
        healthInfo.put("version", "0.5.1");
        healthInfo.put("modulo", "SGA - Administración TI, Seguridad y Operaciones");
        healthInfo.put("dbStatus", dbStatus);
        healthInfo.put("dbLatenciaMs", dbLatenciaMs);
        healthInfo.put("timestamp", LocalDateTime.now());
        return ResponseEntity.ok(ApiResponseDTO.ok(bdOk ? "Servicio operativo" : "Servicio con base de datos degradada", healthInfo));
    }
}
