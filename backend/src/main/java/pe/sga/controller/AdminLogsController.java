package pe.sga.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import pe.sga.config.SecurityRoles;
import pe.sga.dto.response.ApiResponseDTO;
import pe.sga.service.IAdminLogsService;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/admin/logs")
@RequiredArgsConstructor
@PreAuthorize(SecurityRoles.ACCESS_LOGS_READ)
public class AdminLogsController {

    private final IAdminLogsService adminLogsService;

    @GetMapping
    public ResponseEntity<ApiResponseDTO<Map<String, Object>>> getLogs(
            @RequestParam(value = "tipo", required = false, defaultValue = "all") String tipo,
            @RequestParam(value = "lines", required = false, defaultValue = "200") Integer lines
    ) {
        Map<String, Object> logsData = adminLogsService.getLogs(tipo, lines);
        return ResponseEntity.ok(ApiResponseDTO.ok("Logs recuperados con éxito", logsData));
    }

    @GetMapping("/download")
    public ResponseEntity<Resource> downloadLog(
            @RequestParam(value = "tipo", required = false, defaultValue = "all") String tipo
    ) {
        Resource resource = adminLogsService.getLogFileResource(tipo);
        String filename = resource.getFilename() != null ? resource.getFilename() : "sga-backend.log";

        return ResponseEntity.ok()
                .contentType(MediaType.TEXT_PLAIN)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .body(resource);
    }

    @PostMapping("/clear")
    public ResponseEntity<ApiResponseDTO<Void>> clearLog(
            @RequestParam(value = "tipo", required = false, defaultValue = "all") String tipo
    ) {
        adminLogsService.clearLogFile(tipo);
        return ResponseEntity.ok(ApiResponseDTO.ok("Archivo de log vaciado exitosamente", null));
    }
}
