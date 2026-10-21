package pe.sga.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import pe.sga.config.SecurityRoles;
import pe.sga.dto.request.RolDTO;
import pe.sga.dto.response.ApiResponseDTO;
import pe.sga.dto.response.RolResponseDTO;
import pe.sga.service.IRolService;

import java.util.List;

@RestController
@RequestMapping("/api/v1/roles")
@RequiredArgsConstructor
@PreAuthorize(SecurityRoles.ACCESS_ROLES_READ)
public class RolController {

    private final IRolService rolService;

    @GetMapping
    public ResponseEntity<ApiResponseDTO<List<RolResponseDTO>>> listar(
            @RequestParam(required = false, defaultValue = "false") boolean soloActivos) {
        List<RolResponseDTO> roles = soloActivos
                ? rolService.listarActivos()
                : rolService.listarTodos();
        return ResponseEntity.ok(ApiResponseDTO.ok("Lista de roles recuperada exitosamente", roles));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponseDTO<RolResponseDTO>> buscarPorId(@PathVariable Integer id) {
        RolResponseDTO rol = rolService.buscarPorId(id);
        return ResponseEntity.ok(ApiResponseDTO.ok(rol));
    }

    @PostMapping
    @PreAuthorize(SecurityRoles.ACCESS_ROLES_WRITE)
    public ResponseEntity<ApiResponseDTO<RolResponseDTO>> crear(@Valid @RequestBody RolDTO dto) {
        RolResponseDTO creado = rolService.crear(dto);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponseDTO.ok("Rol creado exitosamente", creado));
    }

    @PutMapping("/{id}")
    @PreAuthorize(SecurityRoles.ACCESS_ROLES_WRITE)
    public ResponseEntity<ApiResponseDTO<RolResponseDTO>> actualizar(
            @PathVariable Integer id,
            @Valid @RequestBody RolDTO dto) {
        RolResponseDTO actualizado = rolService.actualizar(id, dto);
        return ResponseEntity.ok(ApiResponseDTO.ok("Rol actualizado exitosamente", actualizado));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize(SecurityRoles.ACCESS_ROLES_WRITE)
    public ResponseEntity<ApiResponseDTO<Void>> eliminarPorEstado(@PathVariable Integer id) {
        rolService.eliminarPorEstado(id);
        return ResponseEntity.ok(ApiResponseDTO.ok("Rol desactivado exitosamente (eliminación lógica por estado)", null));
    }

    @PatchMapping("/{id}/reactivar")
    @PreAuthorize(SecurityRoles.ACCESS_ROLES_WRITE)
    public ResponseEntity<ApiResponseDTO<Void>> reactivar(@PathVariable Integer id) {
        rolService.reactivar(id);
        return ResponseEntity.ok(ApiResponseDTO.ok("Rol reactivado exitosamente", null));
    }
}
