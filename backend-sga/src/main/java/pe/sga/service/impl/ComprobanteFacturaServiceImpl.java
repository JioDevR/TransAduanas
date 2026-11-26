package pe.sga.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.sga.dto.response.ComprobanteFacturaResponseDTO;
import pe.sga.entity.Cliente;
import pe.sga.entity.ComprobanteFactura;
import pe.sga.entity.Cotizacion;
import pe.sga.entity.CotizacionPago;
import pe.sga.entity.Expediente;
import pe.sga.entity.LiquidacionFinanciera;
import pe.sga.entity.Usuario;
import pe.sga.entity.enums.EstadoPagoCotizacion;
import pe.sga.entity.enums.TipoAbonoCotizacion;
import pe.sga.exception.BadRequestException;
import pe.sga.exception.ResourceNotFoundException;
import pe.sga.repository.ComprobanteFacturaRepository;
import pe.sga.repository.CotizacionPagoRepository;
import pe.sga.repository.LiquidacionFinancieraRepository;
import pe.sga.repository.UsuarioRepository;
import pe.sga.service.IAuditoriaService;
import pe.sga.service.IComprobanteFacturaService;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ComprobanteFacturaServiceImpl implements IComprobanteFacturaService {

    private static final BigDecimal FACTOR_IGV = new BigDecimal("1.18");
    private static final BigDecimal DEFAULT_TC = new BigDecimal("3.7500");

    private final ComprobanteFacturaRepository facturaRepository;
    private final CotizacionPagoRepository cotizacionPagoRepository;
    private final LiquidacionFinancieraRepository liquidacionRepository;
    private final UsuarioRepository usuarioRepository;
    private IAuditoriaService auditoriaService;

    @Autowired(required = false)
    public void setAuditoriaService(IAuditoriaService auditoriaService) {
        this.auditoriaService = auditoriaService;
    }

    private void auditar(String accion, Integer idComprobante, String detalle) {
        if (auditoriaService != null) {
            auditoriaService.registrar("FINANZAS", accion, "COMPROBANTE_FACTURA",
                    idComprobante != null ? String.valueOf(idComprobante) : null, detalle, "EXITO");
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<ComprobanteFacturaResponseDTO> listarTodas() {
        return facturaRepository.findAllByOrderByFechaEmisionDesc().stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ComprobanteFacturaResponseDTO> listarPorCliente(Integer idCliente) {
        return facturaRepository.findByCliente_IdClienteOrderByFechaEmisionDesc(idCliente).stream()
                .filter(f -> "EMITIDA".equalsIgnoreCase(f.getEstado()))
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public ComprobanteFacturaResponseDTO buscarPorId(Integer idComprobante) {
        ComprobanteFactura cf = facturaRepository.findById(idComprobante)
                .orElseThrow(() -> new ResourceNotFoundException("Comprobante de factura no encontrado con ID: " + idComprobante));
        return mapToResponse(cf);
    }

    @Override
    @Transactional(readOnly = true)
    public ComprobanteFacturaResponseDTO buscarPorIdParaCliente(Integer idCliente, Integer idComprobante) {
        ComprobanteFactura cf = facturaRepository.findById(idComprobante)
                .orElseThrow(() -> new ResourceNotFoundException("Comprobante de factura no encontrado con ID: " + idComprobante));
        if (cf.getCliente() == null || !cf.getCliente().getIdCliente().equals(idCliente)) {
            throw new AccessDeniedException("El comprobante solicitado no pertenece a su empresa");
        }
        return mapToResponse(cf);
    }

    @Override
    @Transactional
    public ComprobanteFacturaResponseDTO emitirPorAbono(Integer idPago, Integer idUsuarioEmisor) {
        CotizacionPago pago = cotizacionPagoRepository.findById(idPago)
                .orElseThrow(() -> new ResourceNotFoundException("Abono no encontrado con ID: " + idPago));

        if (pago.getEstadoPago() != EstadoPagoCotizacion.APROBADO) {
            throw new BadRequestException("Solo se pueden emitir facturas para abonos en estado APROBADO.");
        }

        Usuario emisor = idUsuarioEmisor != null
                ? usuarioRepository.findById(idUsuarioEmisor).orElse(null)
                : null;

        return emitirAutomaticoPorAbono(pago, emisor);
    }

    @Override
    @Transactional
    public ComprobanteFacturaResponseDTO emitirAutomaticoPorAbono(CotizacionPago pago, Usuario usuarioEmisor) {
        if (pago == null) {
            throw new BadRequestException("El pago es requerido para emitir la factura");
        }

        Optional<ComprobanteFactura> existente = facturaRepository
                .findFirstByPagoCotizacion_IdPagoAndEstadoOrderByIdComprobanteDesc(pago.getIdPago(), "EMITIDA");
        if (existente.isPresent()) {
            return mapToResponse(existente.get());
        }

        Cotizacion cotizacion = pago.getCotizacion();
        Cliente cliente = cotizacion != null ? cotizacion.getCliente() : null;
        if (cliente == null) {
            throw new BadRequestException("El abono no tiene un cliente asociado para facturar");
        }

        boolean esFactura = isClienteFactura(cliente);
        String tipoComprobante = esFactura ? "FACTURA" : "BOLETA";
        String serie = esFactura ? "F001" : "B001";
        int correlativo = siguienteCorrelativo(serie);
        String numeroCompleto = formatearNumero(serie, correlativo);

        BigDecimal total = pago.getMontoAbonado() != null ? pago.getMontoAbonado().setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO;
        BigDecimal subtotal = total.divide(FACTOR_IGV, 2, RoundingMode.HALF_UP);
        BigDecimal igv = total.subtract(subtotal).setScale(2, RoundingMode.HALF_UP);
        BigDecimal tc = pago.getTipoCambio() != null && pago.getTipoCambio().compareTo(BigDecimal.ZERO) > 0
                ? pago.getTipoCambio()
                : DEFAULT_TC;
        String moneda = pago.getMoneda() != null ? pago.getMoneda().toUpperCase() : "USD";
        BigDecimal totalPen = pago.getMontoEquivalentePen() != null
                ? pago.getMontoEquivalentePen().setScale(2, RoundingMode.HALF_UP)
                : ("PEN".equals(moneda) ? total : total.multiply(tc).setScale(2, RoundingMode.HALF_UP));

        String tipoAbonoDesc = resolveTipoAbonoDescripcion(pago.getTipoAbono());
        String codigoCot = cotizacion.getCodigoCotizacion() != null ? cotizacion.getCodigoCotizacion() : "COT";
        String concepto = tipoAbonoDesc + " por servicios logísticos y agenciamiento aduanero — Cotización "
                + codigoCot + " (Abono " + pago.getCodigoPago() + ", Op. " + pago.getNumeroOperacion() + ")";

        ComprobanteFactura cf = ComprobanteFactura.builder()
                .tipoComprobante(tipoComprobante)
                .serie(serie)
                .correlativo(correlativo)
                .numeroCompleto(numeroCompleto)
                .origenTipo("ABONO_COTIZACION")
                .pagoCotizacion(pago)
                .cliente(cliente)
                .clienteRazonSocial(cliente.getRazonSocial())
                .clienteNumeroDocumento(cliente.getNumeroDocumento())
                .clienteDireccion(cliente.getDireccionFiscal())
                .codigoReferencia(codigoCot + " / " + pago.getCodigoPago())
                .concepto(concepto)
                .moneda(moneda)
                .tipoCambio(tc)
                .inafecto(BigDecimal.ZERO)
                .subtotalGravado(subtotal)
                .igvMonto(igv)
                .totalComprobante(total)
                .totalEquivalentePen(totalPen)
                .estado("EMITIDA")
                .fechaEmision(LocalDateTime.now())
                .usuarioEmisor(usuarioEmisor != null ? usuarioEmisor : pago.getUsuarioFinanzas())
                .observaciones("Emisión automática al aprobar abono bancario " + pago.getCodigoPago())
                .build();

        ComprobanteFactura guardada = facturaRepository.save(cf);
        auditar("EMITIR_FACTURA_ABONO", guardada.getIdComprobante(),
                guardada.getNumeroCompleto() + " emitida para abono " + pago.getCodigoPago() + " (" + moneda + " " + total + ")");
        return mapToResponse(guardada);
    }

    @Override
    @Transactional
    public ComprobanteFacturaResponseDTO emitirPorLiquidacion(Integer idLiquidacion, Integer idUsuarioEmisor) {
        LiquidacionFinanciera lf = liquidacionRepository.findById(idLiquidacion)
                .orElseThrow(() -> new ResourceNotFoundException("Liquidación no encontrada con ID: " + idLiquidacion));

        if (!"PAGADO".equalsIgnoreCase(lf.getEstadoPago())) {
            throw new BadRequestException("Solo se pueden emitir comprobantes de liquidaciones ya cobradas (PAGADO).");
        }

        Usuario emisor = idUsuarioEmisor != null
                ? usuarioRepository.findById(idUsuarioEmisor).orElse(null)
                : null;

        return emitirAutomaticoPorLiquidacion(lf, emisor);
    }

    @Override
    @Transactional
    public ComprobanteFacturaResponseDTO emitirAutomaticoPorLiquidacion(LiquidacionFinanciera lf, Usuario usuarioEmisor) {
        if (lf == null) {
            throw new BadRequestException("La liquidación es requerida para emitir el comprobante");
        }

        Optional<ComprobanteFactura> existente = facturaRepository
                .findFirstByLiquidacion_IdLiquidacionAndEstadoOrderByIdComprobanteDesc(lf.getIdLiquidacion(), "EMITIDA");
        if (existente.isPresent()) {
            return mapToResponse(existente.get());
        }

        Expediente exp = lf.getExpediente();
        Cliente cliente = exp != null ? exp.getCliente() : null;
        if (cliente == null) {
            throw new BadRequestException("La liquidación no tiene un cliente asociado para facturar");
        }

        boolean esFactura = isClienteFactura(cliente);
        String tipoComprobante = esFactura ? "FACTURA" : "BOLETA";
        String serie = esFactura ? "F001" : "B001";
        int correlativo = siguienteCorrelativo(serie);
        String numeroCompleto = formatearNumero(serie, correlativo);

        BigDecimal inafecto = lf.getTotalTributosSunat() != null ? lf.getTotalTributosSunat().setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO;
        BigDecimal subtotal = lf.getSubtotalImponible() != null ? lf.getSubtotalImponible().setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO;
        BigDecimal igv = lf.getIgvMonto() != null ? lf.getIgvMonto().setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO;
        BigDecimal total = lf.getTotalGeneralPen() != null ? lf.getTotalGeneralPen().setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO;
        BigDecimal tc = lf.getTipoCambio() != null && lf.getTipoCambio().compareTo(BigDecimal.ZERO) > 0
                ? lf.getTipoCambio()
                : DEFAULT_TC;

        String codExp = exp.getCodigoExpediente() != null ? exp.getCodigoExpediente() : "EXP";
        String concepto = "Liquidación aduanera consolidada — Expediente " + codExp
                + (exp.getNumeroDam() != null ? " (DAM " + exp.getNumeroDam() + ")" : "")
                + " — Servicios portuarios, transporte local, honorarios de agencia y reembolso de tributos SUNAT";

        ComprobanteFactura cf = ComprobanteFactura.builder()
                .tipoComprobante(tipoComprobante)
                .serie(serie)
                .correlativo(correlativo)
                .numeroCompleto(numeroCompleto)
                .origenTipo("LIQUIDACION_ADUANERA")
                .liquidacion(lf)
                .cliente(cliente)
                .clienteRazonSocial(cliente.getRazonSocial())
                .clienteNumeroDocumento(cliente.getNumeroDocumento())
                .clienteDireccion(cliente.getDireccionFiscal())
                .codigoReferencia(lf.getCodigoLiquidacion() + " / " + codExp)
                .concepto(concepto)
                .moneda("PEN")
                .tipoCambio(tc)
                .inafecto(inafecto)
                .subtotalGravado(subtotal)
                .igvMonto(igv)
                .totalComprobante(total)
                .totalEquivalentePen(total)
                .estado("EMITIDA")
                .fechaEmision(LocalDateTime.now())
                .usuarioEmisor(usuarioEmisor != null ? usuarioEmisor : lf.getUsuarioLiquidador())
                .observaciones("Emisión automática por liquidación cobrada " + lf.getCodigoLiquidacion())
                .build();

        ComprobanteFactura guardada = facturaRepository.save(cf);
        if (lf.getNumeroFactura() == null || lf.getNumeroFactura().isBlank()) {
            lf.setNumeroFactura(guardada.getNumeroCompleto());
            liquidacionRepository.save(lf);
        }

        auditar("EMITIR_FACTURA_LIQUIDACION", guardada.getIdComprobante(),
                guardada.getNumeroCompleto() + " emitida para liquidación " + lf.getCodigoLiquidacion() + " (S/ " + total + ")");
        return mapToResponse(guardada);
    }

    @Override
    @Transactional
    public ComprobanteFacturaResponseDTO anular(Integer idComprobante, Integer idUsuario) {
        ComprobanteFactura cf = facturaRepository.findById(idComprobante)
                .orElseThrow(() -> new ResourceNotFoundException("Comprobante no encontrado con ID: " + idComprobante));

        if ("ANULADA".equalsIgnoreCase(cf.getEstado())) {
            throw new BadRequestException("El comprobante ya se encuentra anulado.");
        }

        cf.setEstado("ANULADA");
        ComprobanteFactura guardada = facturaRepository.save(cf);
        auditar("ANULAR_FACTURA", guardada.getIdComprobante(),
                "Comprobante " + guardada.getNumeroCompleto() + " anulado");
        return mapToResponse(guardada);
    }

    private boolean isClienteFactura(Cliente cliente) {
        if (cliente.getTipoDocumento() != null && "RUC".equalsIgnoreCase(cliente.getTipoDocumento().trim())) {
            return true;
        }
        return cliente.getNumeroDocumento() != null && cliente.getNumeroDocumento().trim().length() == 11;
    }

    private int siguienteCorrelativo(String serie) {
        Integer max = facturaRepository.findMaxCorrelativoBySerie(serie);
        int next = (max != null ? max : 0) + 1;
        while (facturaRepository.existsByNumeroCompleto(formatearNumero(serie, next))) {
            next++;
        }
        return next;
    }

    private String formatearNumero(String serie, int correlativo) {
        return String.format("%s-%06d", serie, correlativo);
    }

    private String resolveTipoAbonoDescripcion(TipoAbonoCotizacion tipo) {
        if (tipo == null) return "Anticipo de servicio";
        return switch (tipo) {
            case ADELANTO -> "Anticipo / Adelanto parcial";
            case PAGO_TOTAL -> "Cancelación total (100%)";
            case GARANTIA_CREDITO -> "Abono / Garantía de crédito";
        };
    }

    private ComprobanteFacturaResponseDTO mapToResponse(ComprobanteFactura cf) {
        CotizacionPago pago = cf.getPagoCotizacion();
        Cotizacion cot = pago != null ? pago.getCotizacion() : null;
        LiquidacionFinanciera liq = cf.getLiquidacion();

        String bancoDestino = pago != null ? pago.getBancoDestino() : (liq != null ? liq.getBancoDestino() : null);
        String numeroOperacion = pago != null ? pago.getNumeroOperacion() : (liq != null ? liq.getNumeroOperacion() : null);

        String emisorNombre = null;
        if (cf.getUsuarioEmisor() != null) {
            emisorNombre = cf.getUsuarioEmisor().getNombres() + " " + cf.getUsuarioEmisor().getApellidos();
        }

        return ComprobanteFacturaResponseDTO.builder()
                .idComprobante(cf.getIdComprobante())
                .tipoComprobante(cf.getTipoComprobante())
                .serie(cf.getSerie())
                .correlativo(cf.getCorrelativo())
                .numeroCompleto(cf.getNumeroCompleto())
                .origenTipo(cf.getOrigenTipo())
                .idPagoCotizacion(pago != null ? pago.getIdPago() : null)
                .codigoPago(pago != null ? pago.getCodigoPago() : null)
                .idCotizacion(cot != null ? cot.getIdCotizacion() : null)
                .codigoCotizacion(cot != null ? cot.getCodigoCotizacion() : null)
                .idLiquidacion(liq != null ? liq.getIdLiquidacion() : null)
                .codigoLiquidacion(liq != null ? liq.getCodigoLiquidacion() : null)
                .idCliente(cf.getCliente() != null ? cf.getCliente().getIdCliente() : null)
                .clienteRazonSocial(cf.getClienteRazonSocial())
                .clienteNumeroDocumento(cf.getClienteNumeroDocumento())
                .clienteDireccion(cf.getClienteDireccion())
                .codigoReferencia(cf.getCodigoReferencia())
                .concepto(cf.getConcepto())
                .moneda(cf.getMoneda())
                .tipoCambio(cf.getTipoCambio())
                .inafecto(cf.getInafecto())
                .subtotalGravado(cf.getSubtotalGravado())
                .igvMonto(cf.getIgvMonto())
                .totalComprobante(cf.getTotalComprobante())
                .totalEquivalentePen(cf.getTotalEquivalentePen())
                .bancoDestino(bancoDestino)
                .numeroOperacion(numeroOperacion)
                .estado(cf.getEstado())
                .fechaEmision(cf.getFechaEmision())
                .idUsuarioEmisor(cf.getUsuarioEmisor() != null ? cf.getUsuarioEmisor().getIdUsuario() : null)
                .usuarioEmisorNombre(emisorNombre)
                .observaciones(cf.getObservaciones())
                .build();
    }
}
