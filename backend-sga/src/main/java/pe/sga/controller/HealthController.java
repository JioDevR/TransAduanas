package pe.sga.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pe.sga.dto.response.ApiResponseDTO;

import java.time.LocalDateTime;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/health")
public class HealthController {

    @GetMapping
    public ResponseEntity<ApiResponseDTO<Map<String, Object>>> checkHealth() {
        Map<String, Object> healthInfo = Map.of(
                "status", "UP",
                "app", "SGA Backend API",
                "version", "1.0.0-MODULO1",
                "modulo", "SGA - Módulo 1: Seguridad TI, Accesos y Catálogos",
                "timestamp", LocalDateTime.now()
        );
        return ResponseEntity.ok(ApiResponseDTO.ok("Servicio operativo", healthInfo));
    }
}
