package pe.sga.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import pe.sga.config.SecurityRoles;
import pe.sga.dto.request.ConductorRequestDTO;
import pe.sga.dto.response.ApiResponseDTO;
import pe.sga.dto.response.ConductorResponseDTO;
import pe.sga.service.IConductorService;

import java.util.List;

@RestController
@RequestMapping("/api/v1/transporte/conductores")
@RequiredArgsConstructor
@PreAuthorize(SecurityRoles.ACCESS_TRANSPORTE_READ)
public class ConductorController {

    private final IConductorService conductorService;

    @GetMapping
    public ResponseEntity<ApiResponseDTO<List<ConductorResponseDTO>>> listar(
            @RequestParam(required = false, defaultValue = "false") boolean soloActivos) {
        List<ConductorResponseDTO> lista = soloActivos
                ? conductorService.listarActivos()
                : conductorService.listarTodos();
        return ResponseEntity.ok(ApiResponseDTO.ok("Lista de conductores recuperada", lista));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponseDTO<ConductorResponseDTO>> buscarPorId(@PathVariable Integer id) {
        ConductorResponseDTO dto = conductorService.buscarPorId(id);
        return ResponseEntity.ok(ApiResponseDTO.ok(dto));
    }

    @GetMapping("/licencia/{numeroLicencia}")
    public ResponseEntity<ApiResponseDTO<ConductorResponseDTO>> buscarPorLicencia(@PathVariable String numeroLicencia) {
        ConductorResponseDTO dto = conductorService.buscarPorLicencia(numeroLicencia);
        return ResponseEntity.ok(ApiResponseDTO.ok(dto));
    }

    @PostMapping
    @PreAuthorize(SecurityRoles.ACCESS_TRANSPORTE_WRITE)
    public ResponseEntity<ApiResponseDTO<ConductorResponseDTO>> crear(@Valid @RequestBody ConductorRequestDTO dto) {
        ConductorResponseDTO creado = conductorService.crear(dto);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponseDTO.ok("Conductor registrado exitosamente", creado));
    }

    @PutMapping("/{id}")
    @PreAuthorize(SecurityRoles.ACCESS_TRANSPORTE_WRITE)
    public ResponseEntity<ApiResponseDTO<ConductorResponseDTO>> actualizar(
            @PathVariable Integer id,
            @Valid @RequestBody ConductorRequestDTO dto) {
        ConductorResponseDTO actualizado = conductorService.actualizar(id, dto);
        return ResponseEntity.ok(ApiResponseDTO.ok("Conductor actualizado exitosamente", actualizado));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize(SecurityRoles.ACCESS_TRANSPORTE_WRITE)
    public ResponseEntity<ApiResponseDTO<Void>> eliminar(@PathVariable Integer id) {
        conductorService.eliminar(id);
        return ResponseEntity.ok(ApiResponseDTO.ok("Conductor desactivado exitosamente (borrado lógico)", null));
    }

    @PatchMapping("/{id}/reactivar")
    @PreAuthorize(SecurityRoles.ACCESS_TRANSPORTE_WRITE)
    public ResponseEntity<ApiResponseDTO<Void>> reactivar(@PathVariable Integer id) {
        conductorService.reactivar(id);
        return ResponseEntity.ok(ApiResponseDTO.ok("Conductor reactivado exitosamente", null));
    }
}
