package pe.sga.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pe.sga.dto.request.CatalogoTipoCargaDTO;
import pe.sga.dto.response.ApiResponseDTO;
import pe.sga.dto.response.CatalogoTipoCargaResponseDTO;
import pe.sga.service.ICatalogoTipoCargaService;

import java.util.List;

@RestController
@RequestMapping("/api/v1/catalogos/tipos-carga")
@RequiredArgsConstructor
public class CatalogoTipoCargaController {

    private final ICatalogoTipoCargaService tipoCargaService;

    @GetMapping
    public ResponseEntity<ApiResponseDTO<List<CatalogoTipoCargaResponseDTO>>> listar(
            @RequestParam(required = false, defaultValue = "false") boolean soloActivos) {
        List<CatalogoTipoCargaResponseDTO> tipos = soloActivos
                ? tipoCargaService.listarActivos()
                : tipoCargaService.listarTodos();
        return ResponseEntity.ok(ApiResponseDTO.ok("Lista de tipos de carga recuperada", tipos));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponseDTO<CatalogoTipoCargaResponseDTO>> buscarPorId(@PathVariable Integer id) {
        CatalogoTipoCargaResponseDTO tipo = tipoCargaService.buscarPorId(id);
        return ResponseEntity.ok(ApiResponseDTO.ok(tipo));
    }

    @PostMapping
    @org.springframework.security.access.prepost.PreAuthorize(pe.sga.config.SecurityRoles.ACCESS_SEDES_CATALOGOS_WRITE)
    public ResponseEntity<ApiResponseDTO<CatalogoTipoCargaResponseDTO>> crear(@Valid @RequestBody CatalogoTipoCargaDTO dto) {
        CatalogoTipoCargaResponseDTO creado = tipoCargaService.crear(dto);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponseDTO.ok("Tipo de carga registrado exitosamente", creado));
    }

    @PutMapping("/{id}")
    @org.springframework.security.access.prepost.PreAuthorize(pe.sga.config.SecurityRoles.ACCESS_SEDES_CATALOGOS_WRITE)
    public ResponseEntity<ApiResponseDTO<CatalogoTipoCargaResponseDTO>> actualizar(
            @PathVariable Integer id,
            @Valid @RequestBody CatalogoTipoCargaDTO dto) {
        CatalogoTipoCargaResponseDTO actualizado = tipoCargaService.actualizar(id, dto);
        return ResponseEntity.ok(ApiResponseDTO.ok("Tipo de carga actualizado exitosamente", actualizado));
    }

    @DeleteMapping("/{id}")
    @org.springframework.security.access.prepost.PreAuthorize(pe.sga.config.SecurityRoles.ACCESS_SEDES_CATALOGOS_WRITE)
    public ResponseEntity<ApiResponseDTO<Void>> eliminarPorEstado(@PathVariable Integer id) {
        tipoCargaService.eliminarPorEstado(id);
        return ResponseEntity.ok(ApiResponseDTO.ok("Tipo de carga marcado como inactivo (eliminación por estado)", null));
    }

    @PatchMapping("/{id}/reactivar")
    @org.springframework.security.access.prepost.PreAuthorize(pe.sga.config.SecurityRoles.ACCESS_SEDES_CATALOGOS_WRITE)
    public ResponseEntity<ApiResponseDTO<Void>> reactivar(@PathVariable Integer id) {
        tipoCargaService.reactivar(id);
        return ResponseEntity.ok(ApiResponseDTO.ok("Tipo de carga reactivado exitosamente", null));
    }
}
