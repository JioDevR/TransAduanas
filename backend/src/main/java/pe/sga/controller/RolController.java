package pe.sga.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import pe.sga.config.SecurityRoles;
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
}
