package pe.sga.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pe.sga.dto.request.CatalogoMercanciaRequestDTO;
import pe.sga.dto.response.ApiResponseDTO;
import pe.sga.dto.response.CatalogoMercanciaResponseDTO;
import pe.sga.service.ICatalogoMercanciaService;

import java.util.List;

@RestController
@RequestMapping("/api/v1/catalogos/mercancias")
@RequiredArgsConstructor
public class CatalogoMercanciaController {

    private final ICatalogoMercanciaService mercanciaService;

    @GetMapping
    public ResponseEntity<ApiResponseDTO<List<CatalogoMercanciaResponseDTO>>> listar(
            @RequestParam(required = false, defaultValue = "false") boolean soloActivos,
            @RequestParam(required = false) String categoria) {
        List<CatalogoMercanciaResponseDTO> lista;
        if (categoria != null && !categoria.trim().isEmpty()) {
            lista = mercanciaService.listarPorCategoria(categoria.trim());
        } else if (soloActivos) {
            lista = mercanciaService.listarActivos();
        } else {
            lista = mercanciaService.listarTodos();
        }
        return ResponseEntity.ok(ApiResponseDTO.ok("Lista de mercancías recuperada exitosamente", lista));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponseDTO<CatalogoMercanciaResponseDTO>> buscarPorId(@PathVariable Integer id) {
        CatalogoMercanciaResponseDTO dto = mercanciaService.buscarPorId(id);
        return ResponseEntity.ok(ApiResponseDTO.ok(dto));
    }

    @GetMapping("/codigo/{codigo}")
    public ResponseEntity<ApiResponseDTO<CatalogoMercanciaResponseDTO>> buscarPorCodigo(@PathVariable String codigo) {
        CatalogoMercanciaResponseDTO dto = mercanciaService.buscarPorCodigo(codigo);
        return ResponseEntity.ok(ApiResponseDTO.ok(dto));
    }

    @PostMapping
    @org.springframework.security.access.prepost.PreAuthorize(pe.sga.config.SecurityRoles.ACCESS_SEDES_CATALOGOS_WRITE)
    public ResponseEntity<ApiResponseDTO<CatalogoMercanciaResponseDTO>> crear(@Valid @RequestBody CatalogoMercanciaRequestDTO dto) {
        CatalogoMercanciaResponseDTO creado = mercanciaService.crear(dto);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponseDTO.ok("Mercancía registrada exitosamente en el catálogo", creado));
    }

    @PutMapping("/{id}")
    @org.springframework.security.access.prepost.PreAuthorize(pe.sga.config.SecurityRoles.ACCESS_SEDES_CATALOGOS_WRITE)
    public ResponseEntity<ApiResponseDTO<CatalogoMercanciaResponseDTO>> actualizar(
            @PathVariable Integer id,
            @Valid @RequestBody CatalogoMercanciaRequestDTO dto) {
        CatalogoMercanciaResponseDTO actualizado = mercanciaService.actualizar(id, dto);
        return ResponseEntity.ok(ApiResponseDTO.ok("Mercancía actualizada exitosamente", actualizado));
    }

    @DeleteMapping("/{id}")
    @org.springframework.security.access.prepost.PreAuthorize(pe.sga.config.SecurityRoles.ACCESS_SEDES_CATALOGOS_WRITE)
    public ResponseEntity<ApiResponseDTO<Void>> eliminarPorEstado(@PathVariable Integer id) {
        mercanciaService.eliminarPorEstado(id);
        return ResponseEntity.ok(ApiResponseDTO.ok("Mercancía marcada como inactiva (borrado lógico)", null));
    }

    @PatchMapping("/{id}/reactivar")
    @org.springframework.security.access.prepost.PreAuthorize(pe.sga.config.SecurityRoles.ACCESS_SEDES_CATALOGOS_WRITE)
    public ResponseEntity<ApiResponseDTO<Void>> reactivar(@PathVariable Integer id) {
        mercanciaService.reactivar(id);
        return ResponseEntity.ok(ApiResponseDTO.ok("Mercancía reactivada exitosamente", null));
    }
}
