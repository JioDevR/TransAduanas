package pe.sga.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import pe.sga.config.SecurityRoles;
import pe.sga.dto.request.ExpedienteActualizarDTO;
import pe.sga.dto.request.ExpedienteCanalDTO;
import pe.sga.dto.request.ExpedienteCrearDTO;
import pe.sga.dto.request.ExpedienteLevanteDTO;
import pe.sga.dto.response.ApiResponseDTO;
import pe.sga.dto.response.ExpedienteResponseDTO;
import pe.sga.entity.enums.CanalSunat;
import pe.sga.entity.enums.EstadoExpediente;
import pe.sga.service.IExpedienteService;

import java.util.List;

@RestController
@RequestMapping("/api/v1/expedientes")
@RequiredArgsConstructor
@PreAuthorize(SecurityRoles.ACCESS_EXPEDIENTE_ALMACEN_READ)
public class ExpedienteController {

    private final IExpedienteService expedienteService;

    @GetMapping
    public ResponseEntity<ApiResponseDTO<List<ExpedienteResponseDTO>>> listar(
            @RequestParam(required = false, defaultValue = "false") boolean soloActivos,
            @RequestParam(required = false) CanalSunat canal,
            @RequestParam(required = false) EstadoExpediente estado,
            @RequestParam(required = false) Integer idAgente,
            @RequestParam(required = false) Integer idCliente) {
        List<ExpedienteResponseDTO> lista;
        if (canal != null) {
            lista = expedienteService.listarPorCanal(canal);
        } else if (estado != null) {
            lista = expedienteService.listarPorEstado(estado);
        } else if (idAgente != null) {
            lista = expedienteService.listarPorAgente(idAgente);
        } else if (idCliente != null) {
            lista = expedienteService.listarPorCliente(idCliente);
        } else {
            lista = expedienteService.listarTodos(soloActivos);
        }
        return ResponseEntity.ok(ApiResponseDTO.ok("Lista de expedientes aduaneros recuperada", lista));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponseDTO<ExpedienteResponseDTO>> buscarPorId(@PathVariable Integer id) {
        ExpedienteResponseDTO dto = expedienteService.buscarPorId(id);
        return ResponseEntity.ok(ApiResponseDTO.ok(dto));
    }

    @GetMapping("/codigo/{codigo}")
    public ResponseEntity<ApiResponseDTO<ExpedienteResponseDTO>> buscarPorCodigo(@PathVariable String codigo) {
        ExpedienteResponseDTO dto = expedienteService.buscarPorCodigo(codigo);
        return ResponseEntity.ok(ApiResponseDTO.ok(dto));
    }

    @GetMapping("/cotizacion/{idCotizacion}")
    public ResponseEntity<ApiResponseDTO<ExpedienteResponseDTO>> buscarPorCotizacion(@PathVariable Integer idCotizacion) {
        ExpedienteResponseDTO dto = expedienteService.buscarPorCotizacion(idCotizacion);
        return ResponseEntity.ok(ApiResponseDTO.ok(dto));
    }

    @PostMapping
    @PreAuthorize(SecurityRoles.ACCESS_EXPEDIENTE_ALMACEN_WRITE)
    public ResponseEntity<ApiResponseDTO<ExpedienteResponseDTO>> crear(@Valid @RequestBody ExpedienteCrearDTO dto) {
        ExpedienteResponseDTO creado = expedienteService.crear(dto);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponseDTO.ok("Expediente aduanero aperturado exitosamente", creado));
    }

    @PutMapping("/{id}")
    @PreAuthorize(SecurityRoles.ACCESS_EXPEDIENTE_ALMACEN_WRITE)
    public ResponseEntity<ApiResponseDTO<ExpedienteResponseDTO>> actualizar(
            @PathVariable Integer id,
            @Valid @RequestBody ExpedienteActualizarDTO dto) {
        ExpedienteResponseDTO actualizado = expedienteService.actualizar(id, dto);
        return ResponseEntity.ok(ApiResponseDTO.ok("Expediente aduanero actualizado exitosamente", actualizado));
    }

    @PatchMapping("/{id}/canal")
    @PreAuthorize(SecurityRoles.ACCESS_EXPEDIENTE_ALMACEN_WRITE)
    public ResponseEntity<ApiResponseDTO<ExpedienteResponseDTO>> asignarCanal(
            @PathVariable Integer id,
            @Valid @RequestBody ExpedienteCanalDTO dto) {
        ExpedienteResponseDTO actualizado = expedienteService.asignarCanal(id, dto);
        return ResponseEntity.ok(ApiResponseDTO.ok("Canal SUNAT y numeración de DAM registrados", actualizado));
    }

    @PatchMapping("/{id}/levante")
    @PreAuthorize(SecurityRoles.ACCESS_EXPEDIENTE_ALMACEN_WRITE)
    public ResponseEntity<ApiResponseDTO<ExpedienteResponseDTO>> autorizarLevante(
            @PathVariable Integer id,
            @Valid @RequestBody ExpedienteLevanteDTO dto) {
        ExpedienteResponseDTO actualizado = expedienteService.autorizarLevante(id, dto);
        return ResponseEntity.ok(ApiResponseDTO.ok("Levante aduanero autorizado exitosamente", actualizado));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize(SecurityRoles.ACCESS_EXPEDIENTE_ALMACEN_WRITE)
    public ResponseEntity<ApiResponseDTO<Void>> eliminar(@PathVariable Integer id) {
        expedienteService.anular(id);
        return ResponseEntity.ok(ApiResponseDTO.ok("Expediente anulado exitosamente (borrado lógico por estado)", null));
    }
}
