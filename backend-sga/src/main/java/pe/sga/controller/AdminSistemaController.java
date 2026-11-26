package pe.sga.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import pe.sga.config.SecurityRoles;
import pe.sga.dto.response.AdminAlmacenamientoDTO;
import pe.sga.dto.response.AdminDashboardResponseDTO;
import pe.sga.dto.response.AdminSistemaEstadoDTO;
import pe.sga.dto.response.ApiResponseDTO;
import pe.sga.dto.response.RolMatrizDTO;
import pe.sga.service.IAdminSistemaService;
import pe.sga.service.IReporteJasperService;

import java.io.ByteArrayInputStream;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/admin/sistema")
@RequiredArgsConstructor
@PreAuthorize(SecurityRoles.ACCESS_USUARIOS_READ)
public class AdminSistemaController {

    private final IAdminSistemaService adminSistemaService;
    private final IReporteJasperService reporteJasperService;

    @GetMapping("/dashboard")
    @PreAuthorize(SecurityRoles.ACCESS_LOGS_READ)
    public ResponseEntity<ApiResponseDTO<AdminDashboardResponseDTO>> obtenerDashboard(
            @RequestParam(defaultValue = "7") int dias,
            @RequestParam(required = false, defaultValue = "TODOS") String modulo
    ) {
        return ResponseEntity.ok(ApiResponseDTO.ok(
                "Dashboard TI recuperado exitosamente",
                adminSistemaService.obtenerDashboard(dias, modulo)
        ));
    }

    @GetMapping("/estado")
    public ResponseEntity<ApiResponseDTO<AdminSistemaEstadoDTO>> obtenerEstado() {
        return ResponseEntity.ok(ApiResponseDTO.ok(
                "Estado técnico del sistema recuperado exitosamente",
                adminSistemaService.obtenerEstadoSistema()
        ));
    }

    @GetMapping("/roles-matriz")
    public ResponseEntity<ApiResponseDTO<List<RolMatrizDTO>>> obtenerMatrizRoles() {
        return ResponseEntity.ok(ApiResponseDTO.ok(
                "Matriz de roles y accesos recuperada exitosamente",
                adminSistemaService.obtenerMatrizRoles()
        ));
    }

    @GetMapping("/almacenamiento")
    public ResponseEntity<ApiResponseDTO<AdminAlmacenamientoDTO>> obtenerAlmacenamiento(
            @RequestParam(required = false) String buscar,
            @RequestParam(required = false) String entidad,
            @RequestParam(required = false) String categoria,
            @RequestParam(required = false) String integridad,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ResponseEntity.ok(ApiResponseDTO.ok(
                "Diagnóstico de almacenamiento recuperado exitosamente",
                adminSistemaService.obtenerDiagnosticoAlmacenamiento(buscar, entidad, categoria, integridad, page, size)
        ));
    }

    @PostMapping("/almacenamiento/depurar-huerfanos")
    @PreAuthorize(SecurityRoles.ACCESS_LOGS_READ)
    public ResponseEntity<ApiResponseDTO<Map<String, Object>>> depurarHuerfanos() {
        return ResponseEntity.ok(ApiResponseDTO.ok(
                "Depuración de archivos huérfanos ejecutada exitosamente",
                adminSistemaService.depurarArchivosHuerfanos()
        ));
    }

    @GetMapping("/exportar-pdf")
    @PreAuthorize(SecurityRoles.ACCESS_LOGS_READ)
    public ResponseEntity<InputStreamResource> exportarSistemaPdf(
            @RequestParam(defaultValue = "7") int dias
    ) {
        ByteArrayInputStream pdfStream = reporteJasperService.exportarSistemaTiPdf(dias);

        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=reporte-estado-sistema-ti.pdf");

        return ResponseEntity.ok()
                .headers(headers)
                .contentType(MediaType.APPLICATION_PDF)
                .body(new InputStreamResource(pdfStream));
    }
}
