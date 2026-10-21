package pe.sga.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import pe.sga.config.SecurityRoles;
import pe.sga.dto.request.ClienteRequestDTO;
import pe.sga.dto.response.ApiResponseDTO;
import pe.sga.dto.response.ClienteResponseDTO;
import pe.sga.service.IClienteService;

import java.util.List;

@RestController
@RequestMapping("/api/v1/clientes")
@RequiredArgsConstructor
@PreAuthorize(SecurityRoles.ACCESS_CLIENTE_COTIZACION_READ)
public class ClienteController {

    private final IClienteService clienteService;

    @GetMapping
    public ResponseEntity<ApiResponseDTO<List<ClienteResponseDTO>>> listar(
            @RequestParam(required = false, defaultValue = "false") boolean soloActivos,
            @RequestParam(required = false) Boolean prospecto,
            @RequestParam(required = false) String buscar) {
        List<ClienteResponseDTO> clientes;
        if (buscar != null && !buscar.trim().isEmpty()) {
            clientes = clienteService.buscarPorTermino(buscar, soloActivos);
        } else if (prospecto != null) {
            clientes = clienteService.listarPorTipoProspecto(prospecto, soloActivos);
        } else if (soloActivos) {
            clientes = clienteService.listarActivos();
        } else {
            clientes = clienteService.listarTodos();
        }
        return ResponseEntity.ok(ApiResponseDTO.ok("Lista de clientes recuperada exitosamente", clientes));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponseDTO<ClienteResponseDTO>> buscarPorId(@PathVariable Integer id) {
        ClienteResponseDTO cliente = clienteService.buscarPorId(id);
        return ResponseEntity.ok(ApiResponseDTO.ok(cliente));
    }

    @GetMapping("/documento/{numeroDocumento}")
    public ResponseEntity<ApiResponseDTO<ClienteResponseDTO>> buscarPorNumeroDocumento(@PathVariable String numeroDocumento) {
        ClienteResponseDTO cliente = clienteService.buscarPorNumeroDocumento(numeroDocumento);
        return ResponseEntity.ok(ApiResponseDTO.ok(cliente));
    }

    @PostMapping
    @PreAuthorize(SecurityRoles.ACCESS_CLIENTE_COTIZACION_WRITE)
    public ResponseEntity<ApiResponseDTO<ClienteResponseDTO>> crear(@Valid @RequestBody ClienteRequestDTO dto) {
        ClienteResponseDTO creado = clienteService.crear(dto);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponseDTO.ok("Cliente registrado exitosamente", creado));
    }

    @PutMapping("/{id}")
    @PreAuthorize(SecurityRoles.ACCESS_CLIENTE_COTIZACION_WRITE)
    public ResponseEntity<ApiResponseDTO<ClienteResponseDTO>> actualizar(
            @PathVariable Integer id,
            @Valid @RequestBody ClienteRequestDTO dto) {
        ClienteResponseDTO actualizado = clienteService.actualizar(id, dto);
        return ResponseEntity.ok(ApiResponseDTO.ok("Cliente actualizado exitosamente", actualizado));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize(SecurityRoles.ACCESS_CLIENTE_COTIZACION_WRITE)
    public ResponseEntity<ApiResponseDTO<Void>> eliminar(@PathVariable Integer id) {
        clienteService.eliminar(id);
        return ResponseEntity.ok(ApiResponseDTO.ok("Cliente desactivado exitosamente (borrado lógico por estado)", null));
    }

    @PatchMapping("/{id}/reactivar")
    @PreAuthorize(SecurityRoles.ACCESS_CLIENTE_COTIZACION_WRITE)
    public ResponseEntity<ApiResponseDTO<Void>> reactivar(@PathVariable Integer id) {
        clienteService.reactivar(id);
        return ResponseEntity.ok(ApiResponseDTO.ok("Cliente reactivado exitosamente", null));
    }

    @PatchMapping("/{id}/convertir-cliente")
    @PreAuthorize(SecurityRoles.ACCESS_CLIENTE_COTIZACION_WRITE)
    public ResponseEntity<ApiResponseDTO<ClienteResponseDTO>> convertirEnCliente(@PathVariable Integer id) {
        ClienteResponseDTO formalizado = clienteService.convertirEnCliente(id);
        return ResponseEntity.ok(ApiResponseDTO.ok("Prospecto formalizado como cliente regular", formalizado));
    }
}
