package pe.sga.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pe.sga.dto.request.CatalogoPuertoDTO;
import pe.sga.dto.response.ApiResponseDTO;
import pe.sga.dto.response.CatalogoPuertoResponseDTO;
import pe.sga.service.ICatalogoPuertoService;
import pe.sga.service.IReporteJasperService;

import java.io.ByteArrayInputStream;
import java.util.List;

@RestController
@RequestMapping("/api/v1/catalogos/puertos")
@RequiredArgsConstructor
public class CatalogoPuertoController {

    private final ICatalogoPuertoService puertoService;
    private final IReporteJasperService reporteJasperService;

    @GetMapping
    public ResponseEntity<ApiResponseDTO<List<CatalogoPuertoResponseDTO>>> listar(
            @RequestParam(required = false, defaultValue = "false") boolean soloActivos) {
        List<CatalogoPuertoResponseDTO> puertos = soloActivos
                ? puertoService.listarActivos()
                : puertoService.listarTodos();
        return ResponseEntity.ok(ApiResponseDTO.ok("Lista de puertos recuperada", puertos));
    }

    @GetMapping("/exportar-pdf")
    public ResponseEntity<InputStreamResource> exportarPuertosPdf() {
        ByteArrayInputStream pdfStream = reporteJasperService.exportarPuertosPdf();

        HttpHeaders headers = new HttpHeaders();
        headers.add("Content-Disposition", "inline; filename=reporte-puertos-aduaneros.pdf");

        return ResponseEntity.ok()
                .headers(headers)
                .contentType(MediaType.APPLICATION_PDF)
                .body(new InputStreamResource(pdfStream));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponseDTO<CatalogoPuertoResponseDTO>> buscarPorId(@PathVariable Integer id) {
        CatalogoPuertoResponseDTO puerto = puertoService.buscarPorId(id);
        return ResponseEntity.ok(ApiResponseDTO.ok(puerto));
    }

    @PostMapping
    @org.springframework.security.access.prepost.PreAuthorize(pe.sga.config.SecurityRoles.ACCESS_SEDES_CATALOGOS_WRITE)
    public ResponseEntity<ApiResponseDTO<CatalogoPuertoResponseDTO>> crear(@Valid @RequestBody CatalogoPuertoDTO dto) {
        CatalogoPuertoResponseDTO creado = puertoService.crear(dto);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponseDTO.ok("Puerto registrado exitosamente", creado));
    }

    @PutMapping("/{id}")
    @org.springframework.security.access.prepost.PreAuthorize(pe.sga.config.SecurityRoles.ACCESS_SEDES_CATALOGOS_WRITE)
    public ResponseEntity<ApiResponseDTO<CatalogoPuertoResponseDTO>> actualizar(
            @PathVariable Integer id,
            @Valid @RequestBody CatalogoPuertoDTO dto) {
        CatalogoPuertoResponseDTO actualizado = puertoService.actualizar(id, dto);
        return ResponseEntity.ok(ApiResponseDTO.ok("Puerto actualizado exitosamente", actualizado));
    }

    @DeleteMapping("/{id}")
    @org.springframework.security.access.prepost.PreAuthorize(pe.sga.config.SecurityRoles.ACCESS_SEDES_CATALOGOS_WRITE)
    public ResponseEntity<ApiResponseDTO<Void>> eliminarPorEstado(@PathVariable Integer id) {
        puertoService.eliminarPorEstado(id);
        return ResponseEntity.ok(ApiResponseDTO.ok("Puerto marcado como inactivo (eliminación por estado)", null));
    }

    @PatchMapping("/{id}/reactivar")
    @org.springframework.security.access.prepost.PreAuthorize(pe.sga.config.SecurityRoles.ACCESS_SEDES_CATALOGOS_WRITE)
    public ResponseEntity<ApiResponseDTO<Void>> reactivar(@PathVariable Integer id) {
        puertoService.reactivar(id);
        return ResponseEntity.ok(ApiResponseDTO.ok("Puerto reactivado exitosamente", null));
    }
}
