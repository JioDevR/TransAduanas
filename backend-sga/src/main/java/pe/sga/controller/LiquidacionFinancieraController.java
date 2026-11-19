package pe.sga.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import pe.sga.config.SecurityRoles;
import pe.sga.dto.request.LiquidacionFinancieraRequestDTO;
import pe.sga.dto.request.LiquidacionPagoDTO;
import pe.sga.dto.response.ApiResponseDTO;
import pe.sga.dto.response.FinanzasMetricasResponseDTO;
import pe.sga.dto.response.LiquidacionEstimadoResponseDTO;
import pe.sga.dto.response.LiquidacionFinancieraResponseDTO;
import pe.sga.entity.Usuario;
import pe.sga.service.ILiquidacionFinancieraService;

import java.util.List;

@RestController
@RequestMapping("/api/v1/finanzas/liquidaciones")
@RequiredArgsConstructor
@PreAuthorize(SecurityRoles.ACCESS_LIQUIDACION_READ)
public class LiquidacionFinancieraController {

    private final ILiquidacionFinancieraService liquidacionService;

    @GetMapping
    public ResponseEntity<ApiResponseDTO<List<LiquidacionFinancieraResponseDTO>>> listar(
            @RequestParam(required = false, defaultValue = "false") boolean soloPendientes,
            @RequestParam(required = false) String estado) {
        List<LiquidacionFinancieraResponseDTO> lista;
        if (estado != null && !estado.trim().isEmpty()) {
            lista = liquidacionService.listarPorEstado(estado.trim());
        } else {
            lista = liquidacionService.listarTodas(soloPendientes);
        }
        return ResponseEntity.ok(ApiResponseDTO.ok("Lista de liquidaciones financieras recuperada", lista));
    }

    @GetMapping("/metricas")
    public ResponseEntity<ApiResponseDTO<FinanzasMetricasResponseDTO>> obtenerMetricas() {
        FinanzasMetricasResponseDTO metricas = liquidacionService.obtenerMetricas();
        return ResponseEntity.ok(ApiResponseDTO.ok("Métricas financieras consolidadas", metricas));
    }

    @GetMapping("/calcular-estimado/{idExpediente}")
    public ResponseEntity<ApiResponseDTO<LiquidacionEstimadoResponseDTO>> calcularEstimado(@PathVariable Integer idExpediente) {
        LiquidacionEstimadoResponseDTO estimado = liquidacionService.calcularEstimado(idExpediente);
        return ResponseEntity.ok(ApiResponseDTO.ok("Estimado de liquidación calculado exitosamente", estimado));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponseDTO<LiquidacionFinancieraResponseDTO>> buscarPorId(@PathVariable Integer id) {
        LiquidacionFinancieraResponseDTO dto = liquidacionService.buscarPorId(id);
        return ResponseEntity.ok(ApiResponseDTO.ok(dto));
    }

    @GetMapping("/codigo/{codigo}")
    public ResponseEntity<ApiResponseDTO<LiquidacionFinancieraResponseDTO>> buscarPorCodigo(@PathVariable String codigo) {
        LiquidacionFinancieraResponseDTO dto = liquidacionService.buscarPorCodigo(codigo);
        return ResponseEntity.ok(ApiResponseDTO.ok(dto));
    }

    @GetMapping("/expediente/{idExpediente}")
    public ResponseEntity<ApiResponseDTO<LiquidacionFinancieraResponseDTO>> buscarPorExpediente(@PathVariable Integer idExpediente) {
        LiquidacionFinancieraResponseDTO dto = liquidacionService.buscarPorExpediente(idExpediente);
        return ResponseEntity.ok(ApiResponseDTO.ok(dto));
    }

    @PostMapping
    @PreAuthorize(SecurityRoles.ACCESS_LIQUIDACION_WRITE)
    public ResponseEntity<ApiResponseDTO<LiquidacionFinancieraResponseDTO>> emitir(
            @Valid @RequestBody LiquidacionFinancieraRequestDTO dto,
            @AuthenticationPrincipal Usuario usuarioAutenticado) {
        Integer idUsuario = usuarioAutenticado != null ? usuarioAutenticado.getIdUsuario() : null;
        LiquidacionFinancieraResponseDTO creada = liquidacionService.emitir(dto, idUsuario);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponseDTO.ok("Liquidación financiera emitida exitosamente", creada));
    }

    @PutMapping("/{id}")
    @PreAuthorize(SecurityRoles.ACCESS_LIQUIDACION_WRITE)
    public ResponseEntity<ApiResponseDTO<LiquidacionFinancieraResponseDTO>> actualizar(
            @PathVariable Integer id,
            @Valid @RequestBody LiquidacionFinancieraRequestDTO dto) {
        LiquidacionFinancieraResponseDTO actualizada = liquidacionService.actualizar(id, dto);
        return ResponseEntity.ok(ApiResponseDTO.ok("Liquidación financiera actualizada exitosamente", actualizada));
    }

    @PatchMapping("/{id}/pago")
    @PreAuthorize(SecurityRoles.ACCESS_LIQUIDACION_WRITE)
    public ResponseEntity<ApiResponseDTO<LiquidacionFinancieraResponseDTO>> registrarPago(
            @PathVariable Integer id,
            @Valid @RequestBody LiquidacionPagoDTO dto) {
        LiquidacionFinancieraResponseDTO pagada = liquidacionService.registrarPago(id, dto);
        return ResponseEntity.ok(ApiResponseDTO.ok("Pago de liquidación registrado exitosamente", pagada));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize(SecurityRoles.ACCESS_LIQUIDACION_WRITE)
    public ResponseEntity<ApiResponseDTO<Void>> eliminar(@PathVariable Integer id) {
        liquidacionService.anular(id);
        return ResponseEntity.ok(ApiResponseDTO.ok("Liquidación financiera anulada (borrado lógico)", null));
    }
}
