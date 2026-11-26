package pe.sga.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.InputStreamResource;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import pe.sga.config.SecurityRoles;
import pe.sga.dto.request.CotizacionAsignarDTO;
import pe.sga.dto.request.CotizacionEstadoDTO;
import pe.sga.dto.request.CotizacionRequestDTO;
import pe.sga.dto.request.CotizacionRevisionDTO;
import pe.sga.dto.response.ApiResponseDTO;
import pe.sga.dto.response.CotizacionResponseDTO;
import pe.sga.entity.Usuario;
import pe.sga.entity.enums.EstadoCotizacion;
import pe.sga.service.ICotizacionService;
import pe.sga.service.IReporteJasperService;

import java.io.ByteArrayInputStream;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/cotizaciones")
@RequiredArgsConstructor
@PreAuthorize(SecurityRoles.ACCESS_CLIENTE_COTIZACION_READ)
public class CotizacionController {

    private final ICotizacionService cotizacionService;
    private final IReporteJasperService reporteJasperService;

    @GetMapping
    public ResponseEntity<ApiResponseDTO<List<CotizacionResponseDTO>>> listar(
            @RequestParam(required = false, defaultValue = "false") boolean soloActivas,
            @RequestParam(required = false) EstadoCotizacion estado,
            @RequestParam(required = false) Integer idCliente,
            @RequestParam(required = false) Integer idUsuarioComercial,
            @AuthenticationPrincipal Usuario usuarioActual) {
        List<CotizacionResponseDTO> cotizaciones = cotizacionService.listar(
                soloActivas, estado, idCliente, idUsuarioComercial, usuarioActual
        );
        return ResponseEntity.ok(ApiResponseDTO.ok("Lista de cotizaciones recuperada exitosamente", cotizaciones));
    }

    @GetMapping("/sin-asignar")
    public ResponseEntity<ApiResponseDTO<List<CotizacionResponseDTO>>> listarSinAsignar() {
        List<CotizacionResponseDTO> cotizaciones = cotizacionService.listarSinAsignar();
        return ResponseEntity.ok(ApiResponseDTO.ok("Solicitudes sin asignar recuperadas exitosamente", cotizaciones));
    }

    @GetMapping("/reporte-comercial-pdf")
    public ResponseEntity<InputStreamResource> exportarReporteComercialPdf(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            @RequestParam(required = false) Integer idUsuarioComercial,
            @RequestParam(required = false) String regimen,
            @AuthenticationPrincipal Usuario usuarioActual) {
        ByteArrayInputStream pdfStream = reporteJasperService.exportarReporteComercialPdf(
                desde, hasta, idUsuarioComercial, regimen, usuarioActual
        );

        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=reporte-gestion-comercial.pdf");

        return ResponseEntity.ok()
                .headers(headers)
                .contentType(MediaType.APPLICATION_PDF)
                .body(new InputStreamResource(pdfStream));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponseDTO<CotizacionResponseDTO>> buscarPorId(
            @PathVariable Integer id,
            @AuthenticationPrincipal Usuario usuarioActual) {
        CotizacionResponseDTO cotizacion = cotizacionService.buscarPorId(id, usuarioActual);
        return ResponseEntity.ok(ApiResponseDTO.ok(cotizacion));
    }

    @GetMapping("/codigo/{codigo}")
    public ResponseEntity<ApiResponseDTO<CotizacionResponseDTO>> buscarPorCodigo(
            @PathVariable String codigo,
            @AuthenticationPrincipal Usuario usuarioActual) {
        CotizacionResponseDTO cotizacion = cotizacionService.buscarPorCodigo(codigo, usuarioActual);
        return ResponseEntity.ok(ApiResponseDTO.ok(cotizacion));
    }

    @PostMapping
    @PreAuthorize(SecurityRoles.ACCESS_CLIENTE_COTIZACION_WRITE)
    public ResponseEntity<ApiResponseDTO<CotizacionResponseDTO>> crear(
            @Valid @RequestBody CotizacionRequestDTO dto,
            @AuthenticationPrincipal Usuario usuarioActual) {
        CotizacionResponseDTO creada = cotizacionService.crear(dto, usuarioActual);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponseDTO.ok("Cotización registrada exitosamente", creada));
    }

    @PutMapping("/{id}")
    @PreAuthorize(SecurityRoles.ACCESS_CLIENTE_COTIZACION_WRITE)
    public ResponseEntity<ApiResponseDTO<CotizacionResponseDTO>> actualizar(
            @PathVariable Integer id,
            @Valid @RequestBody CotizacionRequestDTO dto,
            @AuthenticationPrincipal Usuario usuarioActual) {
        CotizacionResponseDTO actualizada = cotizacionService.actualizar(id, dto, usuarioActual);
        return ResponseEntity.ok(ApiResponseDTO.ok("Cotización actualizada exitosamente", actualizada));
    }

    @PatchMapping("/{id}/asignar")
    @PreAuthorize(SecurityRoles.ACCESS_CLIENTE_COTIZACION_WRITE)
    public ResponseEntity<ApiResponseDTO<CotizacionResponseDTO>> asignar(
            @PathVariable Integer id,
            @RequestBody(required = false) CotizacionAsignarDTO dto,
            @AuthenticationPrincipal Usuario usuarioActual) {
        Integer idUsuarioComercial = dto != null ? dto.getIdUsuarioComercial() : null;
        CotizacionResponseDTO asignada = cotizacionService.asignarAsesor(id, idUsuarioComercial, usuarioActual);
        return ResponseEntity.ok(ApiResponseDTO.ok("Cotización asignada exitosamente al asesor comercial", asignada));
    }

    @PatchMapping("/{id}/revision")
    @PreAuthorize(SecurityRoles.ACCESS_COTIZACION_APROBACION)
    public ResponseEntity<ApiResponseDTO<CotizacionResponseDTO>> revisarTecnicamente(
            @PathVariable Integer id,
            @Valid @RequestBody CotizacionRevisionDTO dto) {
        CotizacionResponseDTO revisada = cotizacionService.revisarTecnicamente(id, dto);
        return ResponseEntity.ok(ApiResponseDTO.ok("Revisión técnica de cotización registrada", revisada));
    }

    @PatchMapping("/{id}/enviar-aduanas")
    @PreAuthorize(SecurityRoles.ACCESS_CLIENTE_COTIZACION_WRITE)
    public ResponseEntity<ApiResponseDTO<CotizacionResponseDTO>> enviarAGestionAduanera(
            @PathVariable Integer id,
            @AuthenticationPrincipal Usuario usuarioActual) {
        CotizacionResponseDTO enviada = cotizacionService.enviarAGestionAduanera(id, usuarioActual);
        return ResponseEntity.ok(ApiResponseDTO.ok("Cotización enviada a Gestión Aduanera para evaluación técnica", enviada));
    }

    @PatchMapping("/{id}/estado")
    @PreAuthorize(SecurityRoles.ACCESS_COTIZACION_APROBACION)
    public ResponseEntity<ApiResponseDTO<CotizacionResponseDTO>> cambiarEstado(
            @PathVariable Integer id,
            @Valid @RequestBody CotizacionEstadoDTO dto) {
        CotizacionResponseDTO actualizada = cotizacionService.cambiarEstado(id, dto.getEstado(), dto.getMotivoObservacion());
        return ResponseEntity.ok(ApiResponseDTO.ok("Estado de cotización actualizado a " + dto.getEstado(), actualizada));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize(SecurityRoles.ACCESS_CLIENTE_COTIZACION_WRITE)
    public ResponseEntity<ApiResponseDTO<Void>> eliminar(
            @PathVariable Integer id,
            @AuthenticationPrincipal Usuario usuarioActual) {
        cotizacionService.anular(id, usuarioActual);
        return ResponseEntity.ok(ApiResponseDTO.ok("Cotización anulada exitosamente (borrado lógico por estado)", null));
    }
}
