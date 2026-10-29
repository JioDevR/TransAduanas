package pe.sga.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import pe.sga.dto.portal.*;
import pe.sga.dto.request.LoginRequestDTO;
import pe.sga.dto.response.ApiResponseDTO;
import pe.sga.entity.UsuarioCliente;
import pe.sga.exception.BadRequestException;
import pe.sga.service.IPortalService;

import org.springframework.http.MediaType;
import org.springframework.web.multipart.MultipartFile;
import pe.sga.dto.response.DocumentoResponseDTO;
import pe.sga.entity.enums.CategoriaDocumento;
import pe.sga.entity.enums.EntidadDocumento;
import pe.sga.service.IDocumentoService;

import java.util.List;

@RestController
@RequestMapping("/api/v1/portal")
@RequiredArgsConstructor
@Slf4j
public class PortalController {

    private final IPortalService portalService;
    private final IDocumentoService documentoService;

    // =========================================================================
    // 1. AUTENTICACIÓN INDEPENDIENTE DEL PORTAL DE CLIENTES B2B
    // =========================================================================

    @PostMapping("/auth/login")
    public ResponseEntity<ApiResponseDTO<PortalAuthResponseDTO>> login(
            @Valid @RequestBody LoginRequestDTO loginRequest) {
        PortalAuthResponseDTO authResponse = portalService.autenticarCliente(loginRequest);
        return ResponseEntity.ok(ApiResponseDTO.ok("Autenticación B2B exitosa", authResponse));
    }

    @PostMapping("/auth/cambiar-password")
    @PreAuthorize("hasRole('CLIENTE_PORTAL')")
    public ResponseEntity<ApiResponseDTO<Void>> cambiarPassword(
            @Valid @RequestBody PortalCambioPasswordDTO dto,
            @AuthenticationPrincipal UsuarioCliente usuarioCliente) {
        validarUsuarioCliente(usuarioCliente);
        portalService.cambiarPassword(usuarioCliente, dto);
        return ResponseEntity.ok(ApiResponseDTO.ok("Contraseña actualizada exitosamente", null));
    }

    // =========================================================================
    // 2. PERFIL EMPRESARIAL B2B
    // =========================================================================

    @GetMapping("/perfil")
    @PreAuthorize("hasRole('CLIENTE_PORTAL')")
    public ResponseEntity<ApiResponseDTO<PortalPerfilDTO>> obtenerPerfil(
            @AuthenticationPrincipal UsuarioCliente usuarioCliente) {
        validarUsuarioCliente(usuarioCliente);
        PortalPerfilDTO perfil = portalService.obtenerPerfil(usuarioCliente);
        return ResponseEntity.ok(ApiResponseDTO.ok("Perfil empresarial obtenido", perfil));
    }

    // =========================================================================
    // 3. COTIZACIONES DEL CLIENTE
    // =========================================================================

    @GetMapping("/cotizaciones")
    @PreAuthorize("hasRole('CLIENTE_PORTAL')")
    public ResponseEntity<ApiResponseDTO<List<PortalCotizacionDTO>>> listarCotizaciones(
            @AuthenticationPrincipal UsuarioCliente usuarioCliente) {
        Integer idCliente = resolverIdCliente(usuarioCliente);
        List<PortalCotizacionDTO> cotizaciones = portalService.listarCotizaciones(idCliente);
        return ResponseEntity.ok(ApiResponseDTO.ok("Cotizaciones recuperadas exitosamente", cotizaciones));
    }

    @GetMapping("/cotizaciones/{id}")
    @PreAuthorize("hasRole('CLIENTE_PORTAL')")
    public ResponseEntity<ApiResponseDTO<PortalCotizacionDTO>> buscarCotizacionPorId(
            @PathVariable Integer id,
            @AuthenticationPrincipal UsuarioCliente usuarioCliente) {
        Integer idCliente = resolverIdCliente(usuarioCliente);
        PortalCotizacionDTO cotizacion = portalService.buscarCotizacionPorId(idCliente, id);
        return ResponseEntity.ok(ApiResponseDTO.ok("Detalle de cotización recuperado", cotizacion));
    }

    @PostMapping("/cotizaciones/solicitar")
    @PreAuthorize("hasRole('CLIENTE_PORTAL')")
    public ResponseEntity<ApiResponseDTO<PortalCotizacionDTO>> solicitarCotizacion(
            @Valid @RequestBody PortalCotizacionSolicitudDTO dto,
            @AuthenticationPrincipal UsuarioCliente usuarioCliente) {
        Integer idCliente = resolverIdCliente(usuarioCliente);
        Integer idUsuarioCliente = usuarioCliente.getIdUsuarioCliente();
        PortalCotizacionDTO creada = portalService.solicitarCotizacion(idCliente, dto, idUsuarioCliente);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponseDTO.ok("Solicitud de cotización enviada a evaluación técnica", creada));
    }

    @PatchMapping("/cotizaciones/{id}/aceptar")
    @PreAuthorize("hasRole('CLIENTE_PORTAL')")
    public ResponseEntity<ApiResponseDTO<PortalCotizacionDTO>> aceptarCotizacion(
            @PathVariable Integer id,
            @AuthenticationPrincipal UsuarioCliente usuarioCliente) {
        Integer idCliente = resolverIdCliente(usuarioCliente);
        PortalCotizacionDTO aceptada = portalService.aceptarCotizacion(idCliente, id);
        return ResponseEntity.ok(ApiResponseDTO.ok("Cotización aceptada por el cliente", aceptada));
    }

    // =========================================================================
    // 4. EXPEDIENTES Y SEGUIMIENTO ADUANERO (TRACKING DAM)
    // =========================================================================

    @GetMapping("/expedientes")
    @PreAuthorize("hasRole('CLIENTE_PORTAL')")
    public ResponseEntity<ApiResponseDTO<List<PortalExpedienteDTO>>> listarExpedientes(
            @AuthenticationPrincipal UsuarioCliente usuarioCliente) {
        Integer idCliente = resolverIdCliente(usuarioCliente);
        List<PortalExpedienteDTO> expedientes = portalService.listarExpedientes(idCliente);
        return ResponseEntity.ok(ApiResponseDTO.ok("Expedientes aduaneros recuperados exitosamente", expedientes));
    }

    @GetMapping("/expedientes/{id}")
    @PreAuthorize("hasRole('CLIENTE_PORTAL')")
    public ResponseEntity<ApiResponseDTO<PortalExpedienteDTO>> buscarExpedientePorId(
            @PathVariable Integer id,
            @AuthenticationPrincipal UsuarioCliente usuarioCliente) {
        Integer idCliente = resolverIdCliente(usuarioCliente);
        PortalExpedienteDTO expediente = portalService.buscarExpedientePorId(idCliente, id);
        return ResponseEntity.ok(ApiResponseDTO.ok("Detalle de tracking de expediente recuperado", expediente));
    }

    @GetMapping("/expedientes/{id}/documentos")
    @PreAuthorize("hasRole('CLIENTE_PORTAL')")
    public ResponseEntity<ApiResponseDTO<List<DocumentoResponseDTO>>> listarDocumentosExpediente(
            @PathVariable Integer id,
            @AuthenticationPrincipal UsuarioCliente usuarioCliente) {
        Integer idCliente = resolverIdCliente(usuarioCliente);
        // Valida que el expediente exista y pertenezca al cliente autenticado
        portalService.buscarExpedientePorId(idCliente, id);
        List<DocumentoResponseDTO> documentos = documentoService.listar(
                EntidadDocumento.EXPEDIENTE, null, id, null, null
        );
        return ResponseEntity.ok(ApiResponseDTO.ok("Documentos del expediente recuperados exitosamente", documentos));
    }

    // =========================================================================
    // 5. COBROS, PAGOS Y LIQUIDACIONES
    // =========================================================================

    @GetMapping("/pagos/resumen")
    @PreAuthorize("hasRole('CLIENTE_PORTAL')")
    public ResponseEntity<ApiResponseDTO<PortalResumenFinancieroDTO>> obtenerResumenFinanciero(
            @AuthenticationPrincipal UsuarioCliente usuarioCliente) {
        Integer idCliente = resolverIdCliente(usuarioCliente);
        PortalResumenFinancieroDTO resumen = portalService.obtenerResumenFinanciero(idCliente);
        return ResponseEntity.ok(ApiResponseDTO.ok("Resumen financiero consolidado", resumen));
    }

    @GetMapping("/pagos/obligaciones-pendientes")
    @PreAuthorize("hasRole('CLIENTE_PORTAL')")
    public ResponseEntity<ApiResponseDTO<List<PortalObligacionPagoDTO>>> listarObligacionesPendientes(
            @AuthenticationPrincipal UsuarioCliente usuarioCliente) {
        Integer idCliente = resolverIdCliente(usuarioCliente);
        List<PortalObligacionPagoDTO> obligaciones = portalService.listarObligacionesPendientes(idCliente);
        return ResponseEntity.ok(ApiResponseDTO.ok("Obligaciones financieras pendientes recuperadas", obligaciones));
    }

    @GetMapping("/pagos/anticipos")
    @PreAuthorize("hasRole('CLIENTE_PORTAL')")
    public ResponseEntity<ApiResponseDTO<List<PortalPagoAnticipoDTO>>> listarAnticipos(
            @AuthenticationPrincipal UsuarioCliente usuarioCliente) {
        Integer idCliente = resolverIdCliente(usuarioCliente);
        List<PortalPagoAnticipoDTO> anticipos = portalService.listarAnticipos(idCliente);
        return ResponseEntity.ok(ApiResponseDTO.ok("Historial de anticipos de cotización recuperado", anticipos));
    }

    @GetMapping("/pagos/liquidaciones")
    @PreAuthorize("hasRole('CLIENTE_PORTAL')")
    public ResponseEntity<ApiResponseDTO<List<PortalLiquidacionDTO>>> listarLiquidaciones(
            @AuthenticationPrincipal UsuarioCliente usuarioCliente) {
        Integer idCliente = resolverIdCliente(usuarioCliente);
        List<PortalLiquidacionDTO> liquidaciones = portalService.listarLiquidaciones(idCliente);
        return ResponseEntity.ok(ApiResponseDTO.ok("Liquidaciones de despacho recuperadas", liquidaciones));
    }

    @PostMapping("/pagos/registrar-voucher")
    @PreAuthorize("hasRole('CLIENTE_PORTAL')")
    public ResponseEntity<ApiResponseDTO<PortalPagoAnticipoDTO>> registrarVoucher(
            @Valid @RequestBody PortalRegistroVoucherDTO dto,
            @AuthenticationPrincipal UsuarioCliente usuarioCliente) {
        Integer idCliente = resolverIdCliente(usuarioCliente);
        Integer idUsuarioCliente = usuarioCliente.getIdUsuarioCliente();
        PortalPagoAnticipoDTO registrado = portalService.registrarVoucherPago(idCliente, dto, idUsuarioCliente);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponseDTO.ok("Comprobante de pago registrado y enviado a validación contable", registrado));
    }

    @PostMapping(value = "/pagos/upload-comprobante", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('CLIENTE_PORTAL')")
    public ResponseEntity<ApiResponseDTO<DocumentoResponseDTO>> subirComprobanteVoucher(
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal UsuarioCliente usuarioCliente) {
        Integer idCliente = resolverIdCliente(usuarioCliente);
        List<DocumentoResponseDTO> docs = documentoService.subirDocumentos(
                List.of(file),
                CategoriaDocumento.GENERAL,
                "Voucher de abono bancario registrado por cliente en portal B2B",
                EntidadDocumento.CLIENTE,
                idCliente,
                null
        );
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponseDTO.ok("Voucher almacenado exitosamente", docs.get(0)));
    }

    @PutMapping("/pagos/anticipos/{idPago}/corregir")
    @PreAuthorize("hasRole('CLIENTE_PORTAL')")
    public ResponseEntity<ApiResponseDTO<PortalPagoAnticipoDTO>> corregirVoucher(
            @PathVariable Integer idPago,
            @Valid @RequestBody PortalCorreccionPagoDTO dto,
            @AuthenticationPrincipal UsuarioCliente usuarioCliente) {
        Integer idCliente = resolverIdCliente(usuarioCliente);
        Integer idUsuarioCliente = usuarioCliente.getIdUsuarioCliente();
        PortalPagoAnticipoDTO corregido = portalService.corregirPagoAnticipo(idCliente, idPago, dto, idUsuarioCliente);
        return ResponseEntity.ok(ApiResponseDTO.ok("Registro de pago subsanado exitosamente y devuelto a revisión contable", corregido));
    }

    // =========================================================================
    // 6. NOTIFICACIONES DEL CLIENTE B2B
    // =========================================================================

    @GetMapping("/notificaciones")
    @PreAuthorize("hasRole('CLIENTE_PORTAL')")
    public ResponseEntity<ApiResponseDTO<List<NotificacionClienteDTO>>> listarNotificaciones(
            @AuthenticationPrincipal UsuarioCliente usuarioCliente) {
        Integer idCliente = resolverIdCliente(usuarioCliente);
        List<NotificacionClienteDTO> notificaciones = portalService.listarNotificaciones(idCliente);
        return ResponseEntity.ok(ApiResponseDTO.ok("Notificaciones recuperadas exitosamente", notificaciones));
    }

    @GetMapping("/notificaciones/resumen")
    @PreAuthorize("hasRole('CLIENTE_PORTAL')")
    public ResponseEntity<ApiResponseDTO<Long>> contarNotificacionesNoLeidas(
            @AuthenticationPrincipal UsuarioCliente usuarioCliente) {
        Integer idCliente = resolverIdCliente(usuarioCliente);
        long noLeidas = portalService.contarNotificacionesNoLeidas(idCliente);
        return ResponseEntity.ok(ApiResponseDTO.ok("Total no leídas", noLeidas));
    }

    @PatchMapping("/notificaciones/{id}/leer")
    @PreAuthorize("hasRole('CLIENTE_PORTAL')")
    public ResponseEntity<ApiResponseDTO<Void>> marcarNotificacionLeida(
            @PathVariable Integer id,
            @AuthenticationPrincipal UsuarioCliente usuarioCliente) {
        Integer idCliente = resolverIdCliente(usuarioCliente);
        portalService.marcarNotificacionLeida(idCliente, id);
        return ResponseEntity.ok(ApiResponseDTO.ok("Notificación marcada como leída", null));
    }

    @PatchMapping("/notificaciones/marcar-todas-leidas")
    @PreAuthorize("hasRole('CLIENTE_PORTAL')")
    public ResponseEntity<ApiResponseDTO<Void>> marcarTodasNotificacionesLeidas(
            @AuthenticationPrincipal UsuarioCliente usuarioCliente) {
        Integer idCliente = resolverIdCliente(usuarioCliente);
        portalService.marcarTodasNotificacionesLeidas(idCliente);
        return ResponseEntity.ok(ApiResponseDTO.ok("Todas las notificaciones marcadas como leídas", null));
    }

    // =========================================================================
    // AUXILIARES
    // =========================================================================

    private void validarUsuarioCliente(UsuarioCliente uc) {
        if (uc == null || uc.getCliente() == null) {
            throw new BadRequestException("Acceso exclusivo para usuarios del portal empresarial de clientes");
        }
    }

    private Integer resolverIdCliente(UsuarioCliente uc) {
        validarUsuarioCliente(uc);
        return uc.getCliente().getIdCliente();
    }
}
