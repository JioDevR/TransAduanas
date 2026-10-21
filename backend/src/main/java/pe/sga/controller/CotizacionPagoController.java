package pe.sga.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import pe.sga.config.SecurityRoles;
import pe.sga.dto.request.CotizacionPagoRegistrarDTO;
import pe.sga.dto.request.CotizacionPagoValidarDTO;
import pe.sga.dto.response.ApiResponseDTO;
import pe.sga.dto.response.CotizacionPagoResponseDTO;
import pe.sga.entity.Usuario;
import pe.sga.entity.enums.EstadoPagoCotizacion;
import pe.sga.service.ICotizacionPagoService;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class CotizacionPagoController {

    private final ICotizacionPagoService cotizacionPagoService;

    // =========================================================================
    // ENDPOINTS EN ÁREA DE COTIZACIÓN (REGISTRO Y CONSULTA DE PAGOS / ADELANTOS)
    // =========================================================================

    @PostMapping("/api/v1/cotizaciones/{idCotizacion}/pagos")
    @PreAuthorize(SecurityRoles.ACCESS_CLIENTE_COTIZACION_WRITE)
    public ResponseEntity<ApiResponseDTO<CotizacionPagoResponseDTO>> registrarPago(
            @PathVariable Integer idCotizacion,
            @Valid @RequestBody CotizacionPagoRegistrarDTO dto,
            @AuthenticationPrincipal Usuario usuarioAutenticado) {
        dto.setIdCotizacion(idCotizacion);
        Integer idUsuario = usuarioAutenticado != null ? usuarioAutenticado.getIdUsuario() : null;
        CotizacionPagoResponseDTO response = cotizacionPagoService.registrarPago(dto, idUsuario);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponseDTO.ok("Pago o adelanto de cotización registrado y enviado a validación en Finanzas", response));
    }

    @GetMapping("/api/v1/cotizaciones/{idCotizacion}/pagos")
    @PreAuthorize(SecurityRoles.ACCESS_CLIENTE_COTIZACION_READ)
    public ResponseEntity<ApiResponseDTO<List<CotizacionPagoResponseDTO>>> listarPorCotizacion(
            @PathVariable Integer idCotizacion) {
        List<CotizacionPagoResponseDTO> response = cotizacionPagoService.listarPorCotizacion(idCotizacion);
        return ResponseEntity.ok(ApiResponseDTO.ok("Historial de pagos de la cotización recuperado", response));
    }

    // =========================================================================
    // ENDPOINTS EN MÓDULO DE FINANZAS (BANDEJA Y VALIDACIÓN DE ANTICIPOS)
    // =========================================================================

    @GetMapping("/api/v1/finanzas/anticipos")
    @PreAuthorize(SecurityRoles.ACCESS_LIQUIDACION_READ)
    public ResponseEntity<ApiResponseDTO<List<CotizacionPagoResponseDTO>>> listarAnticipos(
            @RequestParam(required = false) EstadoPagoCotizacion estado) {
        List<CotizacionPagoResponseDTO> response;
        if (estado != null) {
            response = cotizacionPagoService.listarPorEstado(estado);
        } else {
            response = cotizacionPagoService.listarTodos();
        }
        return ResponseEntity.ok(ApiResponseDTO.ok("Lista de anticipos y pagos de cotización recuperada", response));
    }

    @GetMapping("/api/v1/finanzas/anticipos/pendientes")
    @PreAuthorize(SecurityRoles.ACCESS_LIQUIDACION_READ)
    public ResponseEntity<ApiResponseDTO<List<CotizacionPagoResponseDTO>>> listarAnticiposPendientes() {
        List<CotizacionPagoResponseDTO> response = cotizacionPagoService.listarPorEstado(EstadoPagoCotizacion.PENDIENTE_REVISION);
        return ResponseEntity.ok(ApiResponseDTO.ok("Bandeja de anticipos pendientes de validación recuperada", response));
    }

    @GetMapping("/api/v1/finanzas/anticipos/{idPago}")
    @PreAuthorize(SecurityRoles.ACCESS_LIQUIDACION_READ)
    public ResponseEntity<ApiResponseDTO<CotizacionPagoResponseDTO>> buscarPorId(@PathVariable Integer idPago) {
        CotizacionPagoResponseDTO response = cotizacionPagoService.buscarPorId(idPago);
        return ResponseEntity.ok(ApiResponseDTO.ok("Detalle de pago de cotización recuperado", response));
    }

    @PatchMapping("/api/v1/finanzas/anticipos/{idPago}/validar")
    @PreAuthorize(SecurityRoles.ACCESS_LIQUIDACION_WRITE)
    public ResponseEntity<ApiResponseDTO<CotizacionPagoResponseDTO>> validarPago(
            @PathVariable Integer idPago,
            @Valid @RequestBody CotizacionPagoValidarDTO dto,
            @AuthenticationPrincipal Usuario usuarioAutenticado) {
        Integer idUsuarioFinanzas = usuarioAutenticado != null ? usuarioAutenticado.getIdUsuario() : null;
        CotizacionPagoResponseDTO response = cotizacionPagoService.validarPago(idPago, dto, idUsuarioFinanzas);
        return ResponseEntity.ok(ApiResponseDTO.ok("Validación financiera procesada exitosamente", response));
    }
}
