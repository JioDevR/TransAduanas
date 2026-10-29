package pe.sga.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import pe.sga.config.SecurityRoles;
import pe.sga.dto.portal.admin.*;
import pe.sga.dto.response.ApiResponseDTO;
import pe.sga.service.IUsuarioClienteService;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/usuarios-portal")
@RequiredArgsConstructor
@Slf4j
@PreAuthorize(SecurityRoles.ACCESS_USUARIOS_READ)
public class UsuarioClienteAdminController {

    private final IUsuarioClienteService usuarioClienteService;

    @GetMapping
    public ResponseEntity<ApiResponseDTO<List<UsuarioClienteResponseDTO>>> listar(
            @RequestParam(required = false) Integer idCliente,
            @RequestParam(required = false, defaultValue = "false") boolean soloActivos,
            @RequestParam(required = false) String buscar) {
        List<UsuarioClienteResponseDTO> lista = usuarioClienteService.listar(idCliente, soloActivos, buscar);
        return ResponseEntity.ok(ApiResponseDTO.ok("Lista de usuarios del portal recuperada exitosamente", lista));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponseDTO<UsuarioClienteResponseDTO>> buscarPorId(@PathVariable Integer id) {
        UsuarioClienteResponseDTO usuario = usuarioClienteService.buscarPorId(id);
        return ResponseEntity.ok(ApiResponseDTO.ok(usuario));
    }

    @PostMapping
    @PreAuthorize(SecurityRoles.ACCESS_USUARIOS_WRITE)
    public ResponseEntity<ApiResponseDTO<UsuarioClienteResponseDTO>> crear(
            @Valid @RequestBody UsuarioClienteCreateDTO dto) {
        log.info("Petición administrativa para aprovisionar usuario portal: username={}, idCliente={}",
                dto.getUsername(), dto.getIdCliente());
        UsuarioClienteResponseDTO creado = usuarioClienteService.crear(dto);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponseDTO.ok("Usuario del portal creado exitosamente", creado));
    }

    @PutMapping("/{id}")
    @PreAuthorize(SecurityRoles.ACCESS_USUARIOS_WRITE)
    public ResponseEntity<ApiResponseDTO<UsuarioClienteResponseDTO>> actualizar(
            @PathVariable Integer id,
            @Valid @RequestBody UsuarioClienteUpdateDTO dto) {
        UsuarioClienteResponseDTO actualizado = usuarioClienteService.actualizar(id, dto);
        return ResponseEntity.ok(ApiResponseDTO.ok("Usuario del portal actualizado exitosamente", actualizado));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize(SecurityRoles.ACCESS_USUARIOS_WRITE)
    public ResponseEntity<ApiResponseDTO<Void>> eliminarPorEstado(@PathVariable Integer id) {
        usuarioClienteService.eliminarPorEstado(id);
        return ResponseEntity.ok(ApiResponseDTO.ok("Usuario del portal desactivado exitosamente (eliminación lógica por estado)", null));
    }

    @PatchMapping("/{id}/reactivar")
    @PreAuthorize(SecurityRoles.ACCESS_USUARIOS_WRITE)
    public ResponseEntity<ApiResponseDTO<Void>> reactivar(@PathVariable Integer id) {
        usuarioClienteService.reactivar(id);
        return ResponseEntity.ok(ApiResponseDTO.ok("Usuario del portal reactivado exitosamente", null));
    }

    @PatchMapping("/{id}/reset-password")
    @PreAuthorize(SecurityRoles.ACCESS_USUARIOS_WRITE)
    public ResponseEntity<ApiResponseDTO<Void>> resetearPassword(
            @PathVariable Integer id,
            @Valid @RequestBody UsuarioClienteResetPasswordDTO dto) {
        usuarioClienteService.resetearPassword(id, dto);
        return ResponseEntity.ok(ApiResponseDTO.ok("Contraseña del usuario restablecida exitosamente", null));
    }
}
