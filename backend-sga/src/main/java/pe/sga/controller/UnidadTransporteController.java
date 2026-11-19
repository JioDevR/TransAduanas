package pe.sga.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import pe.sga.config.SecurityRoles;
import pe.sga.dto.request.UnidadTransporteRequestDTO;
import pe.sga.dto.response.ApiResponseDTO;
import pe.sga.dto.response.UnidadTransporteResponseDTO;
import pe.sga.service.IUnidadTransporteService;

import java.util.List;

@RestController
@RequestMapping("/api/v1/transporte/unidades")
@RequiredArgsConstructor
@PreAuthorize(SecurityRoles.ACCESS_TRANSPORTE_READ)
public class UnidadTransporteController {

    private final IUnidadTransporteService unidadService;

    @GetMapping
    public ResponseEntity<ApiResponseDTO<List<UnidadTransporteResponseDTO>>> listar(
            @RequestParam(required = false, defaultValue = "false") boolean soloActivos,
            @RequestParam(required = false, defaultValue = "false") boolean soloDisponibles) {
        List<UnidadTransporteResponseDTO> lista;
        if (soloDisponibles) {
            lista = unidadService.listarDisponibles();
        } else if (soloActivos) {
            lista = unidadService.listarActivas();
        } else {
            lista = unidadService.listarTodas();
        }
        return ResponseEntity.ok(ApiResponseDTO.ok("Lista de unidades de transporte recuperada", lista));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponseDTO<UnidadTransporteResponseDTO>> buscarPorId(@PathVariable Integer id) {
        UnidadTransporteResponseDTO dto = unidadService.buscarPorId(id);
        return ResponseEntity.ok(ApiResponseDTO.ok(dto));
    }

    @GetMapping("/placa/{placaTracto}")
    public ResponseEntity<ApiResponseDTO<UnidadTransporteResponseDTO>> buscarPorPlaca(@PathVariable String placaTracto) {
        UnidadTransporteResponseDTO dto = unidadService.buscarPorPlaca(placaTracto);
        return ResponseEntity.ok(ApiResponseDTO.ok(dto));
    }

    @PostMapping
    @PreAuthorize(SecurityRoles.ACCESS_TRANSPORTE_WRITE)
    public ResponseEntity<ApiResponseDTO<UnidadTransporteResponseDTO>> crear(@Valid @RequestBody UnidadTransporteRequestDTO dto) {
        UnidadTransporteResponseDTO creado = unidadService.crear(dto);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponseDTO.ok("Unidad de transporte registrada exitosamente", creado));
    }

    @PutMapping("/{id}")
    @PreAuthorize(SecurityRoles.ACCESS_TRANSPORTE_WRITE)
    public ResponseEntity<ApiResponseDTO<UnidadTransporteResponseDTO>> actualizar(
            @PathVariable Integer id,
            @Valid @RequestBody UnidadTransporteRequestDTO dto) {
        UnidadTransporteResponseDTO actualizado = unidadService.actualizar(id, dto);
        return ResponseEntity.ok(ApiResponseDTO.ok("Unidad de transporte actualizada exitosamente", actualizado));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize(SecurityRoles.ACCESS_TRANSPORTE_WRITE)
    public ResponseEntity<ApiResponseDTO<Void>> eliminar(@PathVariable Integer id) {
        unidadService.eliminar(id);
        return ResponseEntity.ok(ApiResponseDTO.ok("Unidad de transporte desactivada (borrado lógico)", null));
    }

    @PatchMapping("/{id}/reactivar")
    @PreAuthorize(SecurityRoles.ACCESS_TRANSPORTE_WRITE)
    public ResponseEntity<ApiResponseDTO<Void>> reactivar(@PathVariable Integer id) {
        unidadService.reactivar(id);
        return ResponseEntity.ok(ApiResponseDTO.ok("Unidad de transporte reactivada exitosamente", null));
    }
}
