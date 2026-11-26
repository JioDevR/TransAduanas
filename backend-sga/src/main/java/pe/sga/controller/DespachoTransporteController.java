package pe.sga.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import pe.sga.config.SecurityRoles;
import pe.sga.dto.request.DespachoAsignarRecursosDTO;
import pe.sga.dto.request.DespachoEntregaDTO;
import pe.sga.dto.request.DespachoIncidenciaDTO;
import pe.sga.dto.request.DespachoRutaDTO;
import pe.sga.dto.request.DespachoTransporteCrearDTO;
import pe.sga.dto.response.ApiResponseDTO;
import pe.sga.dto.response.DespachoTransporteResponseDTO;
import pe.sga.service.IDespachoTransporteService;

import java.util.List;

@RestController
@RequestMapping("/api/v1/transporte/despachos")
@RequiredArgsConstructor
@PreAuthorize(SecurityRoles.ACCESS_TRANSPORTE_READ)
public class DespachoTransporteController {

    private final IDespachoTransporteService despachoService;

    @GetMapping
    public ResponseEntity<ApiResponseDTO<List<DespachoTransporteResponseDTO>>> listar(
            @RequestParam(required = false, defaultValue = "false") boolean soloActivos,
            @RequestParam(required = false) String estado,
            @RequestParam(required = false) Integer idConductor,
            @RequestParam(required = false) Integer idUnidad) {
        List<DespachoTransporteResponseDTO> lista = despachoService.listarConFiltros(
                soloActivos, estado, idConductor, idUnidad);
        return ResponseEntity.ok(ApiResponseDTO.ok("Lista de despachos de transporte recuperada", lista));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponseDTO<DespachoTransporteResponseDTO>> buscarPorId(@PathVariable Integer id) {
        DespachoTransporteResponseDTO dto = despachoService.buscarPorId(id);
        return ResponseEntity.ok(ApiResponseDTO.ok(dto));
    }

    @GetMapping("/tracking/{codigoTracking}")
    public ResponseEntity<ApiResponseDTO<DespachoTransporteResponseDTO>> buscarPorTracking(@PathVariable String codigoTracking) {
        DespachoTransporteResponseDTO dto = despachoService.buscarPorTracking(codigoTracking);
        return ResponseEntity.ok(ApiResponseDTO.ok(dto));
    }

    @GetMapping("/expediente/{idExpediente}")
    public ResponseEntity<ApiResponseDTO<DespachoTransporteResponseDTO>> buscarPorExpediente(@PathVariable Integer idExpediente) {
        DespachoTransporteResponseDTO dto = despachoService.buscarPorExpediente(idExpediente);
        return ResponseEntity.ok(ApiResponseDTO.ok(dto));
    }

    @PostMapping
    @PreAuthorize(SecurityRoles.ACCESS_TRANSPORTE_WRITE)
    public ResponseEntity<ApiResponseDTO<DespachoTransporteResponseDTO>> programar(@Valid @RequestBody DespachoTransporteCrearDTO dto) {
        DespachoTransporteResponseDTO creado = despachoService.programar(dto);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponseDTO.ok("Despacho de transporte programado exitosamente", creado));
    }

    @PatchMapping("/{id}/aceptar")
    @PreAuthorize(SecurityRoles.ACCESS_TRANSPORTE_WRITE)
    public ResponseEntity<ApiResponseDTO<DespachoTransporteResponseDTO>> aceptarSolicitud(@PathVariable Integer id) {
        DespachoTransporteResponseDTO actualizado = despachoService.aceptarSolicitud(id);
        return ResponseEntity.ok(ApiResponseDTO.ok("Solicitud de transporte aceptada por el área de flota", actualizado));
    }

    @PatchMapping("/{id}/asignar-recursos")
    @PreAuthorize(SecurityRoles.ACCESS_TRANSPORTE_WRITE)
    public ResponseEntity<ApiResponseDTO<DespachoTransporteResponseDTO>> asignarRecursos(
            @PathVariable Integer id,
            @Valid @RequestBody DespachoAsignarRecursosDTO dto) {
        DespachoTransporteResponseDTO actualizado = despachoService.asignarRecursos(id, dto);
        return ResponseEntity.ok(ApiResponseDTO.ok("Conductor y unidad asignados al despacho", actualizado));
    }

    @PatchMapping("/{id}/iniciar-ruta")
    @PreAuthorize(SecurityRoles.ACCESS_TRANSPORTE_WRITE)
    public ResponseEntity<ApiResponseDTO<DespachoTransporteResponseDTO>> iniciarRuta(
            @PathVariable Integer id,
            @Valid @RequestBody DespachoRutaDTO dto) {
        DespachoTransporteResponseDTO actualizado = despachoService.iniciarRuta(id, dto);
        return ResponseEntity.ok(ApiResponseDTO.ok("Ruta de transporte iniciada exitosamente", actualizado));
    }

    @PatchMapping("/{id}/confirmar-entrega")
    @PreAuthorize(SecurityRoles.ACCESS_TRANSPORTE_WRITE)
    public ResponseEntity<ApiResponseDTO<DespachoTransporteResponseDTO>> confirmarEntrega(
            @PathVariable Integer id,
            @Valid @RequestBody DespachoEntregaDTO dto) {
        DespachoTransporteResponseDTO actualizado = despachoService.confirmarEntrega(id, dto);
        return ResponseEntity.ok(ApiResponseDTO.ok("Entrega conforme registrada y unidad liberada", actualizado));
    }

    @PostMapping("/{id}/incidencia")
    @PreAuthorize(SecurityRoles.ACCESS_TRANSPORTE_WRITE)
    public ResponseEntity<ApiResponseDTO<DespachoTransporteResponseDTO>> reportarIncidencia(
            @PathVariable Integer id,
            @Valid @RequestBody DespachoIncidenciaDTO dto) {
        DespachoTransporteResponseDTO actualizado = despachoService.reportarIncidencia(id, dto.getIncidencia());
        return ResponseEntity.ok(ApiResponseDTO.ok("Incidencia reportada correctamente", actualizado));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize(SecurityRoles.ACCESS_TRANSPORTE_WRITE)
    public ResponseEntity<ApiResponseDTO<Void>> eliminar(@PathVariable Integer id) {
        despachoService.anular(id);
        return ResponseEntity.ok(ApiResponseDTO.ok("Despacho de transporte cancelado (borrado lógico)", null));
    }
}
