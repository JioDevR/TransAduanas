package pe.sga.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import pe.sga.config.SecurityRoles;
import pe.sga.dto.request.ControlAlmacenRequestDTO;
import pe.sga.dto.request.ControlAlmacenSalidaDTO;
import pe.sga.dto.response.ApiResponseDTO;
import pe.sga.dto.response.ControlAlmacenResponseDTO;
import pe.sga.service.IControlAlmacenService;

import java.util.List;

@RestController
@RequestMapping("/api/v1/almacen")
@RequiredArgsConstructor
@PreAuthorize(SecurityRoles.ACCESS_EXPEDIENTE_ALMACEN_READ)
public class ControlAlmacenController {

    private final IControlAlmacenService controlAlmacenService;

    @GetMapping
    public ResponseEntity<ApiResponseDTO<List<ControlAlmacenResponseDTO>>> listar(
            @RequestParam(required = false, defaultValue = "false") boolean soloActivos,
            @RequestParam(required = false) String deposito) {
        List<ControlAlmacenResponseDTO> lista;
        if (deposito != null && !deposito.trim().isEmpty()) {
            lista = controlAlmacenService.listarPorDeposito(deposito.trim());
        } else {
            lista = controlAlmacenService.listarTodos(soloActivos);
        }
        return ResponseEntity.ok(ApiResponseDTO.ok("Lista de controles de almacén recuperada", lista));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponseDTO<ControlAlmacenResponseDTO>> buscarPorId(@PathVariable Integer id) {
        ControlAlmacenResponseDTO dto = controlAlmacenService.buscarPorId(id);
        return ResponseEntity.ok(ApiResponseDTO.ok(dto));
    }

    @GetMapping("/expediente/{idExpediente}")
    public ResponseEntity<ApiResponseDTO<ControlAlmacenResponseDTO>> buscarPorExpediente(@PathVariable Integer idExpediente) {
        ControlAlmacenResponseDTO dto = controlAlmacenService.buscarPorExpediente(idExpediente);
        return ResponseEntity.ok(ApiResponseDTO.ok(dto));
    }

    @PostMapping
    @PreAuthorize(SecurityRoles.ACCESS_EXPEDIENTE_ALMACEN_WRITE)
    public ResponseEntity<ApiResponseDTO<ControlAlmacenResponseDTO>> registrarIngreso(@Valid @RequestBody ControlAlmacenRequestDTO dto) {
        ControlAlmacenResponseDTO creado = controlAlmacenService.registrarIngreso(dto);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponseDTO.ok("Ingreso a almacén registrado exitosamente", creado));
    }

    @PutMapping("/{id}")
    @PreAuthorize(SecurityRoles.ACCESS_EXPEDIENTE_ALMACEN_WRITE)
    public ResponseEntity<ApiResponseDTO<ControlAlmacenResponseDTO>> actualizar(
            @PathVariable Integer id,
            @Valid @RequestBody ControlAlmacenRequestDTO dto) {
        ControlAlmacenResponseDTO actualizado = controlAlmacenService.actualizar(id, dto);
        return ResponseEntity.ok(ApiResponseDTO.ok("Registro de almacén actualizado exitosamente", actualizado));
    }

    @PatchMapping("/{id}/autorizar-salida")
    @PreAuthorize(SecurityRoles.ACCESS_EXPEDIENTE_ALMACEN_WRITE)
    public ResponseEntity<ApiResponseDTO<ControlAlmacenResponseDTO>> autorizarSalida(
            @PathVariable Integer id,
            @RequestBody ControlAlmacenSalidaDTO dto) {
        ControlAlmacenResponseDTO actualizado = controlAlmacenService.autorizarSalida(id, dto);
        return ResponseEntity.ok(ApiResponseDTO.ok("Salida y movimiento de carga autorizado exitosamente", actualizado));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize(SecurityRoles.ACCESS_EXPEDIENTE_ALMACEN_WRITE)
    public ResponseEntity<ApiResponseDTO<Void>> eliminar(@PathVariable Integer id) {
        controlAlmacenService.anular(id);
        return ResponseEntity.ok(ApiResponseDTO.ok("Registro de almacén anulado (borrado lógico)", null));
    }
}
