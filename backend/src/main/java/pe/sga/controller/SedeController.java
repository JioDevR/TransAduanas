package pe.sga.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import pe.sga.config.SecurityRoles;
import pe.sga.dto.request.SedeCreateDTO;
import pe.sga.dto.request.SedeUpdateDTO;
import pe.sga.dto.response.ApiResponseDTO;
import pe.sga.dto.response.SedeResponseDTO;
import pe.sga.service.IReporteJasperService;
import pe.sga.service.ISedeService;

import java.io.ByteArrayInputStream;
import java.util.List;

@RestController
@RequestMapping("/api/v1/sedes")
@RequiredArgsConstructor
@PreAuthorize(SecurityRoles.ACCESS_SEDES_CATALOGOS_READ)
public class SedeController {

    private final ISedeService sedeService;
    private final IReporteJasperService reporteJasperService;

    @GetMapping
    public ResponseEntity<ApiResponseDTO<List<SedeResponseDTO>>> listar(
            @RequestParam(required = false, defaultValue = "false") boolean soloOperativas) {
        List<SedeResponseDTO> sedes = soloOperativas
                ? sedeService.listarOperativas()
                : sedeService.listarTodas();
        return ResponseEntity.ok(ApiResponseDTO.ok("Lista de sedes recuperada exitosamente", sedes));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponseDTO<SedeResponseDTO>> buscarPorId(@PathVariable Integer id) {
        SedeResponseDTO sede = sedeService.buscarPorId(id);
        return ResponseEntity.ok(ApiResponseDTO.ok(sede));
    }

    @PostMapping
    @PreAuthorize(SecurityRoles.ACCESS_SEDES_CATALOGOS_WRITE)
    public ResponseEntity<ApiResponseDTO<SedeResponseDTO>> crear(@Valid @RequestBody SedeCreateDTO dto) {
        SedeResponseDTO creada = sedeService.crear(dto);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponseDTO.ok("Sede creada exitosamente", creada));
    }

    @PutMapping("/{id}")
    @PreAuthorize(SecurityRoles.ACCESS_SEDES_CATALOGOS_WRITE)
    public ResponseEntity<ApiResponseDTO<SedeResponseDTO>> actualizar(
            @PathVariable Integer id,
            @Valid @RequestBody SedeUpdateDTO dto) {
        SedeResponseDTO actualizada = sedeService.actualizar(id, dto);
        return ResponseEntity.ok(ApiResponseDTO.ok("Sede actualizada exitosamente", actualizada));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize(SecurityRoles.ACCESS_SEDES_CATALOGOS_WRITE)
    public ResponseEntity<ApiResponseDTO<Void>> eliminarPorEstado(@PathVariable Integer id) {
        sedeService.eliminarPorEstado(id);
        return ResponseEntity.ok(ApiResponseDTO.ok("Sede desactivada exitosamente (eliminación lógica por estado)", null));
    }

    @PatchMapping("/{id}/reactivar")
    @PreAuthorize(SecurityRoles.ACCESS_SEDES_CATALOGOS_WRITE)
    public ResponseEntity<ApiResponseDTO<Void>> reactivar(@PathVariable Integer id) {
        sedeService.reactivar(id);
        return ResponseEntity.ok(ApiResponseDTO.ok("Sede reactivada exitosamente", null));
    }

    @GetMapping("/exportar-pdf")
    public ResponseEntity<InputStreamResource> exportarSedesPdf() {
        ByteArrayInputStream pdfStream = reporteJasperService.exportarSedesPdf();

        HttpHeaders headers = new HttpHeaders();
        headers.add("Content-Disposition", "inline; filename=reporte-sedes-operativas.pdf");

        return ResponseEntity.ok()
                .headers(headers)
                .contentType(MediaType.APPLICATION_PDF)
                .body(new InputStreamResource(pdfStream));
    }
}
