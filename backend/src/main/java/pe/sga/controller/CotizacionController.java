package pe.sga.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import pe.sga.config.SecurityRoles;
import pe.sga.dto.request.CotizacionEstadoDTO;
import pe.sga.dto.request.CotizacionRequestDTO;
import pe.sga.dto.request.CotizacionRevisionDTO;
import pe.sga.dto.response.ApiResponseDTO;
import pe.sga.dto.response.CotizacionResponseDTO;
import pe.sga.entity.enums.EstadoCotizacion;
import pe.sga.service.ICotizacionService;

import java.util.List;

@RestController
@RequestMapping("/api/v1/cotizaciones")
@RequiredArgsConstructor
@PreAuthorize(SecurityRoles.ACCESS_CLIENTE_COTIZACION_READ)
public class CotizacionController {

    private final ICotizacionService cotizacionService;

    @GetMapping
    public ResponseEntity<ApiResponseDTO<List<CotizacionResponseDTO>>> listar(
            @RequestParam(required = false, defaultValue = "false") boolean soloActivas,
            @RequestParam(required = false) EstadoCotizacion estado,
            @RequestParam(required = false) Integer idCliente) {
        List<CotizacionResponseDTO> cotizaciones;
        if (idCliente != null) {
            cotizaciones = cotizacionService.listarPorCliente(idCliente);
        } else if (estado != null) {
            cotizaciones = cotizacionService.listarPorEstado(estado);
        } else {
            cotizaciones = cotizacionService.listarTodas(soloActivas);
        }
        return ResponseEntity.ok(ApiResponseDTO.ok("Lista de cotizaciones recuperada exitosamente", cotizaciones));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponseDTO<CotizacionResponseDTO>> buscarPorId(@PathVariable Integer id) {
        CotizacionResponseDTO cotizacion = cotizacionService.buscarPorId(id);
        return ResponseEntity.ok(ApiResponseDTO.ok(cotizacion));
    }

    @GetMapping("/codigo/{codigo}")
    public ResponseEntity<ApiResponseDTO<CotizacionResponseDTO>> buscarPorCodigo(@PathVariable String codigo) {
        CotizacionResponseDTO cotizacion = cotizacionService.buscarPorCodigo(codigo);
        return ResponseEntity.ok(ApiResponseDTO.ok(cotizacion));
    }

    @PostMapping
    @PreAuthorize(SecurityRoles.ACCESS_CLIENTE_COTIZACION_WRITE)
    public ResponseEntity<ApiResponseDTO<CotizacionResponseDTO>> crear(@Valid @RequestBody CotizacionRequestDTO dto) {
        CotizacionResponseDTO creada = cotizacionService.crear(dto);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponseDTO.ok("Cotización registrada exitosamente", creada));
    }

    @PutMapping("/{id}")
    @PreAuthorize(SecurityRoles.ACCESS_CLIENTE_COTIZACION_WRITE)
    public ResponseEntity<ApiResponseDTO<CotizacionResponseDTO>> actualizar(
            @PathVariable Integer id,
            @Valid @RequestBody CotizacionRequestDTO dto) {
        CotizacionResponseDTO actualizada = cotizacionService.actualizar(id, dto);
        return ResponseEntity.ok(ApiResponseDTO.ok("Cotización actualizada exitosamente", actualizada));
    }

    @PatchMapping("/{id}/revision")
    @PreAuthorize(SecurityRoles.ACCESS_COTIZACION_APROBACION)
    public ResponseEntity<ApiResponseDTO<CotizacionResponseDTO>> revisarTecnicamente(
            @PathVariable Integer id,
            @Valid @RequestBody CotizacionRevisionDTO dto) {
        CotizacionResponseDTO revisada = cotizacionService.revisarTecnicamente(id, dto);
        return ResponseEntity.ok(ApiResponseDTO.ok("Revisión técnica de cotización registrada", revisada));
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
    public ResponseEntity<ApiResponseDTO<Void>> eliminar(@PathVariable Integer id) {
        cotizacionService.anular(id);
        return ResponseEntity.ok(ApiResponseDTO.ok("Cotización anulada exitosamente (borrado lógico por estado)", null));
    }
}
