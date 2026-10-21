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
        List<DespachoTransporteResponseDTO> lista;
        if (estado != null && !estado.trim().isEmpty()) {
            lista = despachoService.listarPorEstado(estado.trim());
        } else if (idConductor != null) {
            lista = despachoService.listarPorConductor(idConductor);
        } else if (idUnidad != null) {
            lista = despachoService.listarPorUnidad(idUnidad);
        } else {
            lista = despachoService.listarTodos(soloActivos);
        }
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

    @GetMapping("/mi-despacho-activo")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponseDTO<DespachoTransporteResponseDTO>> obtenerMiDespachoActivo(
            @org.springframework.security.core.annotation.AuthenticationPrincipal pe.sga.entity.Usuario usuarioAutenticado) {
        if (usuarioAutenticado == null) {
            return ResponseEntity.ok(ApiResponseDTO.ok("Usuario no autenticado", null));
        }
        DespachoTransporteResponseDTO dto = despachoService.obtenerDespachoActivoPorUsuario(usuarioAutenticado.getIdUsuario());
        return ResponseEntity.ok(ApiResponseDTO.ok("Despacho activo del conductor", dto));
    }

    @GetMapping("/mis-despachos")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponseDTO<List<DespachoTransporteResponseDTO>>> listarMisDespachos(
            @org.springframework.security.core.annotation.AuthenticationPrincipal pe.sga.entity.Usuario usuarioAutenticado) {
        if (usuarioAutenticado == null) {
            return ResponseEntity.ok(ApiResponseDTO.ok("Usuario no autenticado", List.of()));
        }
        List<DespachoTransporteResponseDTO> lista = despachoService.listarMisDespachos(usuarioAutenticado.getIdUsuario());
        return ResponseEntity.ok(ApiResponseDTO.ok("Historial de despachos del conductor", lista));
    }

    @PostMapping("/{id}/incidencia")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponseDTO<DespachoTransporteResponseDTO>> reportarIncidencia(
            @PathVariable Integer id,
            @RequestBody java.util.Map<String, String> body) {
        String incidencia = body.getOrDefault("incidencia", "Incidencia no detallada");
        DespachoTransporteResponseDTO actualizado = despachoService.reportarIncidencia(id, incidencia);
        return ResponseEntity.ok(ApiResponseDTO.ok("Incidencia reportada correctamente", actualizado));
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
            @RequestBody DespachoRutaDTO dto) {
        DespachoTransporteResponseDTO actualizado = despachoService.iniciarRuta(id, dto);
        return ResponseEntity.ok(ApiResponseDTO.ok("Ruta de transporte iniciada exitosamente", actualizado));
    }

    @PatchMapping("/{id}/confirmar-entrega")
    @PreAuthorize(SecurityRoles.ACCESS_TRANSPORTE_WRITE)
    public ResponseEntity<ApiResponseDTO<DespachoTransporteResponseDTO>> confirmarEntrega(
            @PathVariable Integer id,
            @RequestBody DespachoEntregaDTO dto) {
        DespachoTransporteResponseDTO actualizado = despachoService.confirmarEntrega(id, dto);
        return ResponseEntity.ok(ApiResponseDTO.ok("Entrega conforme registrada y unidad liberada", actualizado));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize(SecurityRoles.ACCESS_TRANSPORTE_WRITE)
    public ResponseEntity<ApiResponseDTO<Void>> eliminar(@PathVariable Integer id) {
        despachoService.anular(id);
        return ResponseEntity.ok(ApiResponseDTO.ok("Despacho de transporte cancelado (borrado lógico)", null));
    }
}
