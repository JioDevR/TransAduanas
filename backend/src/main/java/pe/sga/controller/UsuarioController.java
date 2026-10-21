package pe.sga.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import pe.sga.config.SecurityRoles;
import pe.sga.dto.request.UsuarioCreateDTO;
import pe.sga.dto.request.UsuarioUpdateDTO;
import pe.sga.dto.response.ApiResponseDTO;
import pe.sga.dto.response.UsuarioResponseDTO;
import pe.sga.service.IReporteJasperService;
import pe.sga.service.IUsuarioService;

import java.io.ByteArrayInputStream;
import java.util.List;

@RestController
@RequestMapping("/api/v1/usuarios")
@RequiredArgsConstructor
@PreAuthorize(SecurityRoles.ACCESS_USUARIOS_READ)
public class UsuarioController {

    private final IUsuarioService usuarioService;
    private final IReporteJasperService reporteJasperService;

    @GetMapping
    public ResponseEntity<ApiResponseDTO<List<UsuarioResponseDTO>>> listar(
            @RequestParam(required = false, defaultValue = "false") boolean soloActivos) {
        List<UsuarioResponseDTO> usuarios = soloActivos
                ? usuarioService.listarActivos()
                : usuarioService.listarTodos();
        return ResponseEntity.ok(ApiResponseDTO.ok("Lista de usuarios recuperada exitosamente", usuarios));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponseDTO<UsuarioResponseDTO>> buscarPorId(@PathVariable Integer id) {
        UsuarioResponseDTO usuario = usuarioService.buscarPorId(id);
        return ResponseEntity.ok(ApiResponseDTO.ok(usuario));
    }

    @PostMapping
    @PreAuthorize(SecurityRoles.ACCESS_USUARIOS_WRITE)
    public ResponseEntity<ApiResponseDTO<UsuarioResponseDTO>> crear(@Valid @RequestBody UsuarioCreateDTO dto) {
        UsuarioResponseDTO creado = usuarioService.crear(dto);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponseDTO.ok("Usuario creado exitosamente", creado));
    }

    @PutMapping("/{id}")
    @PreAuthorize(SecurityRoles.ACCESS_USUARIOS_WRITE)
    public ResponseEntity<ApiResponseDTO<UsuarioResponseDTO>> actualizar(
            @PathVariable Integer id,
            @Valid @RequestBody UsuarioUpdateDTO dto) {
        UsuarioResponseDTO actualizado = usuarioService.actualizar(id, dto);
        return ResponseEntity.ok(ApiResponseDTO.ok("Usuario actualizado exitosamente", actualizado));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize(SecurityRoles.ACCESS_USUARIOS_WRITE)
    public ResponseEntity<ApiResponseDTO<Void>> eliminarPorEstado(@PathVariable Integer id) {
        usuarioService.eliminarPorEstado(id);
        return ResponseEntity.ok(ApiResponseDTO.ok("Usuario desactivado exitosamente (eliminación lógica por estado)", null));
    }

    @PatchMapping("/{id}/reactivar")
    @PreAuthorize(SecurityRoles.ACCESS_USUARIOS_WRITE)
    public ResponseEntity<ApiResponseDTO<Void>> reactivar(@PathVariable Integer id) {
        usuarioService.reactivar(id);
        return ResponseEntity.ok(ApiResponseDTO.ok("Usuario reactivado exitosamente", null));
    }

    @GetMapping("/exportar-pdf")
    public ResponseEntity<InputStreamResource> exportarUsuariosPdf() {
        ByteArrayInputStream pdfStream = reporteJasperService.exportarUsuariosPdf();

        HttpHeaders headers = new HttpHeaders();
        headers.add("Content-Disposition", "inline; filename=reporte-usuarios-seguridad-ti.pdf");

        return ResponseEntity.ok()
                .headers(headers)
                .contentType(MediaType.APPLICATION_PDF)
                .body(new InputStreamResource(pdfStream));
    }
}
