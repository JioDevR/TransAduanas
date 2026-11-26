package pe.sga.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.CacheControl;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import pe.sga.config.SecurityRoles;
import pe.sga.dto.response.ApiResponseDTO;
import pe.sga.dto.response.ComprobanteFacturaResponseDTO;
import pe.sga.entity.Usuario;
import pe.sga.service.IComprobanteFacturaService;
import pe.sga.service.IReporteJasperService;

import java.io.ByteArrayInputStream;
import java.util.List;

@RestController
@RequestMapping("/api/v1/finanzas/facturas")
@RequiredArgsConstructor
@PreAuthorize(SecurityRoles.ACCESS_LIQUIDACION_READ)
public class ComprobanteFacturaController {

    private final IComprobanteFacturaService facturaService;
    private final IReporteJasperService reporteJasperService;

    @GetMapping
    public ResponseEntity<ApiResponseDTO<List<ComprobanteFacturaResponseDTO>>> listar() {
        return ResponseEntity.ok(ApiResponseDTO.ok("Comprobantes emitidos recuperados", facturaService.listarTodas()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponseDTO<ComprobanteFacturaResponseDTO>> buscarPorId(@PathVariable Integer id) {
        return ResponseEntity.ok(ApiResponseDTO.ok(facturaService.buscarPorId(id)));
    }

    @PostMapping("/emitir-abono/{idPago}")
    @PreAuthorize(SecurityRoles.ACCESS_LIQUIDACION_WRITE)
    public ResponseEntity<ApiResponseDTO<ComprobanteFacturaResponseDTO>> emitirPorAbono(
            @PathVariable Integer idPago,
            @AuthenticationPrincipal Usuario usuarioAutenticado) {
        Integer idUsuario = usuarioAutenticado != null ? usuarioAutenticado.getIdUsuario() : null;
        ComprobanteFacturaResponseDTO emitida = facturaService.emitirPorAbono(idPago, idUsuario);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponseDTO.ok("Comprobante emitido exitosamente para el abono", emitida));
    }

    @PostMapping("/emitir-liquidacion/{idLiquidacion}")
    @PreAuthorize(SecurityRoles.ACCESS_LIQUIDACION_WRITE)
    public ResponseEntity<ApiResponseDTO<ComprobanteFacturaResponseDTO>> emitirPorLiquidacion(
            @PathVariable Integer idLiquidacion,
            @AuthenticationPrincipal Usuario usuarioAutenticado) {
        Integer idUsuario = usuarioAutenticado != null ? usuarioAutenticado.getIdUsuario() : null;
        ComprobanteFacturaResponseDTO emitida = facturaService.emitirPorLiquidacion(idLiquidacion, idUsuario);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponseDTO.ok("Comprobante emitido exitosamente para la liquidación", emitida));
    }

    @PatchMapping("/{id}/anular")
    @PreAuthorize(SecurityRoles.ACCESS_LIQUIDACION_WRITE)
    public ResponseEntity<ApiResponseDTO<ComprobanteFacturaResponseDTO>> anular(
            @PathVariable Integer id,
            @AuthenticationPrincipal Usuario usuarioAutenticado) {
        Integer idUsuario = usuarioAutenticado != null ? usuarioAutenticado.getIdUsuario() : null;
        return ResponseEntity.ok(ApiResponseDTO.ok("Comprobante anulado", facturaService.anular(id, idUsuario)));
    }

    @GetMapping("/{id}/pdf")
    public ResponseEntity<InputStreamResource> descargarPdf(
            @PathVariable Integer id,
            @RequestParam(defaultValue = "false") boolean download) {
        ComprobanteFacturaResponseDTO factura = facturaService.buscarPorId(id);
        ByteArrayInputStream pdf = reporteJasperService.exportarComprobanteFacturaPdf(factura);

        String safeNumber = factura.getNumeroCompleto() != null
                ? factura.getNumeroCompleto().replaceAll("[^A-Za-z0-9._-]", "-")
                : "comprobante-" + id;

        HttpHeaders headers = new HttpHeaders();
        headers.setContentDisposition(
                ContentDisposition.builder(download ? "attachment" : "inline")
                        .filename(safeNumber + ".pdf")
                        .build()
        );
        headers.setCacheControl(CacheControl.noStore());

        return ResponseEntity.ok()
                .headers(headers)
                .contentType(MediaType.APPLICATION_PDF)
                .body(new InputStreamResource(pdf));
    }
}
