package pe.sga.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.core.io.InputStreamResource;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import pe.sga.config.SecurityRoles;
import pe.sga.dto.response.ApiResponseDTO;
import pe.sga.service.IAuditoriaService;
import pe.sga.service.IReporteJasperService;

import java.io.ByteArrayInputStream;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/admin/auditoria")
@RequiredArgsConstructor
@PreAuthorize(SecurityRoles.ACCESS_LOGS_READ)
public class AdminAuditoriaController {

    private final IAuditoriaService auditoriaService;
    private final IReporteJasperService reporteJasperService;

    @GetMapping
    public ResponseEntity<ApiResponseDTO<Map<String, Object>>> listar(
            @RequestParam(required = false) String buscar,
            @RequestParam(required = false) String modulo,
            @RequestParam(required = false) String resultado,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaDesde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaHasta,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        LocalDateTime desde = fechaDesde != null ? fechaDesde.atStartOfDay() : null;
        LocalDateTime hasta = fechaHasta != null ? fechaHasta.atTime(LocalTime.MAX) : null;
        Map<String, Object> data = auditoriaService.listar(buscar, modulo, resultado, desde, hasta, page, size);
        return ResponseEntity.ok(ApiResponseDTO.ok("Eventos de auditoría recuperados", data));
    }

    @GetMapping("/exportar-pdf")
    public ResponseEntity<InputStreamResource> exportarPdf(
            @RequestParam(required = false) String buscar,
            @RequestParam(required = false) String modulo,
            @RequestParam(required = false) String resultado,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaDesde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaHasta
    ) {
        LocalDateTime desde = fechaDesde != null ? fechaDesde.atStartOfDay() : null;
        LocalDateTime hasta = fechaHasta != null ? fechaHasta.atTime(LocalTime.MAX) : null;
        ByteArrayInputStream pdfStream = reporteJasperService.exportarAuditoriaTiPdf(buscar, modulo, resultado, desde, hasta);

        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=reporte-auditoria-ti.pdf");

        return ResponseEntity.ok()
                .headers(headers)
                .contentType(MediaType.APPLICATION_PDF)
                .body(new InputStreamResource(pdfStream));
    }
}
