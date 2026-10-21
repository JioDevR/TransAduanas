package pe.sga.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.sga.dto.request.LiquidacionFinancieraRequestDTO;
import pe.sga.dto.request.LiquidacionPagoDTO;
import pe.sga.dto.response.FinanzasMetricasResponseDTO;
import pe.sga.dto.response.LiquidacionEstimadoResponseDTO;
import pe.sga.dto.response.LiquidacionFinancieraResponseDTO;
import pe.sga.entity.ControlAlmacen;
import pe.sga.entity.Cotizacion;
import pe.sga.entity.DocumentoAdjunto;
import pe.sga.entity.Expediente;
import pe.sga.entity.LiquidacionFinanciera;
import pe.sga.entity.Usuario;
import pe.sga.exception.BadRequestException;
import pe.sga.exception.ResourceNotFoundException;
import pe.sga.repository.ControlAlmacenRepository;
import pe.sga.repository.ExpedienteRepository;
import pe.sga.repository.IDocumentoAdjuntoRepository;
import pe.sga.repository.LiquidacionFinancieraRepository;
import pe.sga.repository.UsuarioRepository;
import pe.sga.service.ILiquidacionFinancieraService;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class LiquidacionFinancieraServiceImpl implements ILiquidacionFinancieraService {

    private final LiquidacionFinancieraRepository liquidacionRepository;
    private final ExpedienteRepository expedienteRepository;
    private final ControlAlmacenRepository controlAlmacenRepository;
    private final UsuarioRepository usuarioRepository;
    private final IDocumentoAdjuntoRepository documentoRepository;

    @Override
    @Transactional(readOnly = true)
    public List<LiquidacionFinancieraResponseDTO> listarTodas(Boolean soloPendientes) {
        if (soloPendientes != null && soloPendientes) {
            return liquidacionRepository.findByEstadoPago("PENDIENTE").stream()
                    .map(this::mapToResponse)
                    .toList();
        }
        return liquidacionRepository.findAll().stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<LiquidacionFinancieraResponseDTO> listarPorEstado(String estadoPago) {
        return liquidacionRepository.findByEstadoPago(estadoPago).stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public LiquidacionFinancieraResponseDTO buscarPorId(Integer idLiquidacion) {
        LiquidacionFinanciera lf = liquidacionRepository.findById(idLiquidacion)
                .orElseThrow(() -> new ResourceNotFoundException("Liquidación no encontrada con ID: " + idLiquidacion));
        return mapToResponse(lf);
    }

    @Override
    @Transactional(readOnly = true)
    public LiquidacionFinancieraResponseDTO buscarPorCodigo(String codigoLiquidacion) {
        LiquidacionFinanciera lf = liquidacionRepository.findByCodigoLiquidacion(codigoLiquidacion)
                .orElseThrow(() -> new ResourceNotFoundException("Liquidación no encontrada con código: " + codigoLiquidacion));
        return mapToResponse(lf);
    }

    @Override
    @Transactional(readOnly = true)
    public LiquidacionFinancieraResponseDTO buscarPorExpediente(Integer idExpediente) {
        LiquidacionFinanciera lf = liquidacionRepository.findByExpediente_IdExpediente(idExpediente)
                .orElseThrow(() -> new ResourceNotFoundException("No existe liquidación para el expediente ID: " + idExpediente));
        return mapToResponse(lf);
    }

    @Override
    @Transactional(readOnly = true)
    public LiquidacionEstimadoResponseDTO calcularEstimado(Integer idExpediente) {
        Expediente exp = expedienteRepository.findById(idExpediente)
                .orElseThrow(() -> new ResourceNotFoundException("Expediente no encontrado con ID: " + idExpediente));

        Cotizacion cot = exp.getCotizacion();
        BigDecimal tc = new BigDecimal("3.7500");

        BigDecimal tributos = cot != null && cot.getMontoTributosEstimado() != null ? cot.getMontoTributosEstimado() : BigDecimal.ZERO;
        BigDecimal honorarios = cot != null && cot.getMontoHonorariosEstimado() != null ? cot.getMontoHonorariosEstimado() : BigDecimal.ZERO;
        BigDecimal flete = cot != null && cot.getMontoFleteEstimado() != null ? cot.getMontoFleteEstimado() : BigDecimal.ZERO;

        BigDecimal almacen = BigDecimal.ZERO;
        var almacenOpt = controlAlmacenRepository.findByExpediente_IdExpediente(idExpediente);
        if (almacenOpt.isPresent()) {
            ControlAlmacen ca = almacenOpt.get();
            // Estimado base por custodia y pesaje VGM
            almacen = new BigDecimal("450.00");
            if (ca.getCantidadBultos() != null && ca.getCantidadBultos() > 50) {
                almacen = almacen.add(new BigDecimal("250.00"));
            }
        }

        BigDecimal subtotal = honorarios.add(almacen).add(flete);
        BigDecimal igv = subtotal.multiply(new BigDecimal("0.18")).setScale(2, RoundingMode.HALF_UP);
        BigDecimal totalPen = tributos.add(subtotal).add(igv);
        BigDecimal totalUsd = tc.compareTo(BigDecimal.ZERO) > 0 ? totalPen.divide(tc, 2, RoundingMode.HALF_UP) : BigDecimal.ZERO;

        BigDecimal pctDetraccion = new BigDecimal("12.00");
        BigDecimal montoDetraccion = subtotal.add(igv).multiply(pctDetraccion).divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);

        return LiquidacionEstimadoResponseDTO.builder()
                .idExpediente(exp.getIdExpediente())
                .codigoExpediente(exp.getCodigoExpediente())
                .clienteRazonSocial(exp.getCliente() != null ? exp.getCliente().getRazonSocial() : null)
                .clienteNumeroDocumento(exp.getCliente() != null ? exp.getCliente().getNumeroDocumento() : null)
                .numeroDam(exp.getNumeroDam())
                .canalSunat(exp.getCanalSunat() != null ? exp.getCanalSunat().name() : null)
                .totalTributosSunatEstimado(tributos)
                .gastosAlmacenPuertoEstimado(almacen)
                .fleteTransporteLocalEstimado(flete)
                .honorariosAgenciaEstimado(honorarios)
                .tipoCambioSugerido(tc)
                .subtotalImponible(subtotal)
                .igvMonto(igv)
                .aplicaDetraccion(true)
                .porcentajeDetraccion(pctDetraccion)
                .montoDetraccion(montoDetraccion)
                .totalGeneralPen(totalPen)
                .totalGeneralUsd(totalUsd)
                .build();
    }

    @Override
    @Transactional
    public LiquidacionFinancieraResponseDTO emitir(LiquidacionFinancieraRequestDTO dto, Integer idUsuarioAutenticado) {
        Expediente expediente = expedienteRepository.findById(dto.getIdExpediente())
                .orElseThrow(() -> new ResourceNotFoundException("Expediente no encontrado con ID: " + dto.getIdExpediente()));

        if (liquidacionRepository.existsByExpediente_IdExpediente(dto.getIdExpediente())) {
            throw new BadRequestException("Ya existe una liquidación financiera para el expediente ID: " + dto.getIdExpediente());
        }

        Usuario liquidador = null;
        if (idUsuarioAutenticado != null) {
            liquidador = usuarioRepository.findById(idUsuarioAutenticado).orElse(null);
        }

        BigDecimal tributos = dto.getTotalTributosSunat() != null ? dto.getTotalTributosSunat() : BigDecimal.ZERO;
        BigDecimal almacen = dto.getGastosAlmacenPuerto() != null ? dto.getGastosAlmacenPuerto() : BigDecimal.ZERO;
        BigDecimal flete = dto.getFleteTransporteLocal() != null ? dto.getFleteTransporteLocal() : BigDecimal.ZERO;
        BigDecimal honorarios = dto.getHonorariosAgencia() != null ? dto.getHonorariosAgencia() : BigDecimal.ZERO;

        BigDecimal subtotal = honorarios.add(almacen).add(flete);
        BigDecimal igv = subtotal.multiply(new BigDecimal("0.18")).setScale(2, RoundingMode.HALF_UP);
        BigDecimal totalPen = tributos.add(subtotal).add(igv);

        BigDecimal tc = dto.getTipoCambio() != null && dto.getTipoCambio().compareTo(BigDecimal.ZERO) > 0
                ? dto.getTipoCambio()
                : new BigDecimal("3.7500");

        BigDecimal totalUsd = totalPen.divide(tc, 2, RoundingMode.HALF_UP);

        boolean aplicaDet = dto.getAplicaDetraccion() == null || dto.getAplicaDetraccion();
        BigDecimal pctDet = dto.getPorcentajeDetraccion() != null ? dto.getPorcentajeDetraccion() : new BigDecimal("12.00");
        BigDecimal montoDet = aplicaDet
                ? subtotal.add(igv).multiply(pctDet).divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        LocalDate emision = dto.getFechaEmision() != null ? dto.getFechaEmision() : LocalDate.now();
        String codigo = generarCodigoLiquidacion();

        DocumentoAdjunto docFactura = null;
        if (dto.getIdDocumentoFactura() != null) {
            docFactura = documentoRepository.findById(dto.getIdDocumentoFactura()).orElse(null);
        }

        LiquidacionFinanciera lf = LiquidacionFinanciera.builder()
                .codigoLiquidacion(codigo)
                .expediente(expediente)
                .usuarioLiquidador(liquidador)
                .totalTributosSunat(tributos)
                .gastosAlmacenPuerto(almacen)
                .fleteTransporteLocal(flete)
                .honorariosAgencia(honorarios)
                .subtotalImponible(subtotal)
                .igvMonto(igv)
                .aplicaDetraccion(aplicaDet)
                .porcentajeDetraccion(pctDet)
                .montoDetraccion(montoDet)
                .totalGeneralPen(totalPen)
                .totalGeneralUsd(totalUsd)
                .tipoCambio(tc)
                .estadoPago("PENDIENTE")
                .numeroFactura(dto.getNumeroFactura() != null ? dto.getNumeroFactura().trim() : null)
                .documentoFactura(docFactura)
                .fechaEmision(emision)
                .observaciones(dto.getObservaciones())
                .build();

        return mapToResponse(liquidacionRepository.save(lf));
    }

    @Override
    @Transactional
    public LiquidacionFinancieraResponseDTO emitir(LiquidacionFinancieraRequestDTO dto) {
        return emitir(dto, null);
    }

    @Override
    @Transactional
    public LiquidacionFinancieraResponseDTO actualizar(Integer idLiquidacion, LiquidacionFinancieraRequestDTO dto) {
        LiquidacionFinanciera lf = liquidacionRepository.findById(idLiquidacion)
                .orElseThrow(() -> new ResourceNotFoundException("Liquidación no encontrada con ID: " + idLiquidacion));

        if (lf.getEstadoPago().equalsIgnoreCase("PAGADO") || lf.getEstadoPago().equalsIgnoreCase("ANULADO")) {
            throw new BadRequestException("No se puede editar una liquidación en estado: " + lf.getEstadoPago());
        }

        if (dto.getTotalTributosSunat() != null) lf.setTotalTributosSunat(dto.getTotalTributosSunat());
        if (dto.getGastosAlmacenPuerto() != null) lf.setGastosAlmacenPuerto(dto.getGastosAlmacenPuerto());
        if (dto.getFleteTransporteLocal() != null) lf.setFleteTransporteLocal(dto.getFleteTransporteLocal());
        if (dto.getHonorariosAgencia() != null) lf.setHonorariosAgencia(dto.getHonorariosAgencia());
        if (dto.getTipoCambio() != null) lf.setTipoCambio(dto.getTipoCambio());
        if (dto.getAplicaDetraccion() != null) lf.setAplicaDetraccion(dto.getAplicaDetraccion());
        if (dto.getPorcentajeDetraccion() != null) lf.setPorcentajeDetraccion(dto.getPorcentajeDetraccion());
        if (dto.getNumeroFactura() != null) lf.setNumeroFactura(dto.getNumeroFactura().trim());
        if (dto.getIdDocumentoFactura() != null) {
            DocumentoAdjunto doc = documentoRepository.findById(dto.getIdDocumentoFactura()).orElse(null);
            lf.setDocumentoFactura(doc);
        }
        if (dto.getObservaciones() != null) lf.setObservaciones(dto.getObservaciones());

        // Recalcular
        BigDecimal subtotal = lf.getHonorariosAgencia().add(lf.getGastosAlmacenPuerto()).add(lf.getFleteTransporteLocal());
        BigDecimal igv = subtotal.multiply(new BigDecimal("0.18")).setScale(2, RoundingMode.HALF_UP);
        BigDecimal totalPen = lf.getTotalTributosSunat().add(subtotal).add(igv);
        BigDecimal tc = lf.getTipoCambio().compareTo(BigDecimal.ZERO) > 0 ? lf.getTipoCambio() : new BigDecimal("3.7500");
        BigDecimal totalUsd = totalPen.divide(tc, 2, RoundingMode.HALF_UP);

        BigDecimal montoDet = lf.getAplicaDetraccion()
                ? subtotal.add(igv).multiply(lf.getPorcentajeDetraccion()).divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        lf.setSubtotalImponible(subtotal);
        lf.setIgvMonto(igv);
        lf.setTotalGeneralPen(totalPen);
        lf.setTotalGeneralUsd(totalUsd);
        lf.setMontoDetraccion(montoDet);

        return mapToResponse(liquidacionRepository.save(lf));
    }

    @Override
    @Transactional
    public LiquidacionFinancieraResponseDTO registrarPago(Integer idLiquidacion, LiquidacionPagoDTO dto) {
        LiquidacionFinanciera lf = liquidacionRepository.findById(idLiquidacion)
                .orElseThrow(() -> new ResourceNotFoundException("Liquidación no encontrada con ID: " + idLiquidacion));

        if (lf.getEstadoPago().equalsIgnoreCase("PAGADO")) {
            throw new BadRequestException("Esta liquidación ya se encuentra registrada como pagada");
        }
        if (lf.getEstadoPago().equalsIgnoreCase("ANULADO")) {
            throw new BadRequestException("No se puede registrar pago para una liquidación anulada");
        }

        lf.setMetodoPago(dto.getMetodoPago().trim().toUpperCase());
        lf.setNumeroOperacion(dto.getNumeroOperacion().trim());
        if (dto.getBancoDestino() != null) {
            lf.setBancoDestino(dto.getBancoDestino().trim().toUpperCase());
        }
        if (dto.getNumeroFactura() != null && !dto.getNumeroFactura().trim().isEmpty()) {
            lf.setNumeroFactura(dto.getNumeroFactura().trim());
        }
        if (dto.getIdDocumentoVoucher() != null) {
            DocumentoAdjunto voucher = documentoRepository.findById(dto.getIdDocumentoVoucher()).orElse(null);
            lf.setDocumentoVoucher(voucher);
        }
        lf.setFechaPago(dto.getFechaPago() != null ? dto.getFechaPago() : LocalDateTime.now());
        lf.setEstadoPago("PAGADO");
        if (dto.getObservaciones() != null) {
            lf.setObservaciones(dto.getObservaciones());
        }

        return mapToResponse(liquidacionRepository.save(lf));
    }

    @Override
    @Transactional
    public void anular(Integer idLiquidacion) {
        LiquidacionFinanciera lf = liquidacionRepository.findById(idLiquidacion)
                .orElseThrow(() -> new ResourceNotFoundException("Liquidación no encontrada con ID: " + idLiquidacion));
        lf.setEstadoPago("ANULADO");
        liquidacionRepository.save(lf);
    }

    @Override
    @Transactional(readOnly = true)
    public FinanzasMetricasResponseDTO obtenerMetricas() {
        List<LiquidacionFinanciera> todas = liquidacionRepository.findAll();

        BigDecimal cobradoPen = BigDecimal.ZERO;
        BigDecimal cobradoUsd = BigDecimal.ZERO;
        BigDecimal porCobrarPen = BigDecimal.ZERO;
        BigDecimal porCobrarUsd = BigDecimal.ZERO;
        BigDecimal tributosSunat = BigDecimal.ZERO;
        BigDecimal detracciones = BigDecimal.ZERO;

        long pendientes = 0;
        long pagadas = 0;
        long anuladas = 0;

        for (LiquidacionFinanciera lf : todas) {
            String estado = lf.getEstadoPago();
            if ("PAGADO".equalsIgnoreCase(estado)) {
                pagadas++;
                cobradoPen = cobradoPen.add(lf.getTotalGeneralPen());
                cobradoUsd = cobradoUsd.add(lf.getTotalGeneralUsd());
                tributosSunat = tributosSunat.add(lf.getTotalTributosSunat());
                detracciones = detracciones.add(lf.getMontoDetraccion());
            } else if ("PENDIENTE".equalsIgnoreCase(estado)) {
                pendientes++;
                porCobrarPen = porCobrarPen.add(lf.getTotalGeneralPen());
                porCobrarUsd = porCobrarUsd.add(lf.getTotalGeneralUsd());
                tributosSunat = tributosSunat.add(lf.getTotalTributosSunat());
                detracciones = detracciones.add(lf.getMontoDetraccion());
            } else if ("ANULADO".equalsIgnoreCase(estado)) {
                anuladas++;
            }
        }

        return FinanzasMetricasResponseDTO.builder()
                .totalCobradoPen(cobradoPen)
                .totalCobradoUsd(cobradoUsd)
                .cuentasPorCobrarPen(porCobrarPen)
                .cuentasPorCobrarUsd(porCobrarUsd)
                .totalTributosSunatPen(tributosSunat)
                .totalDetraccionesPen(detracciones)
                .totalLiquidaciones((long) todas.size())
                .liquidacionesPendientes(pendientes)
                .liquidacionesPagadas(pagadas)
                .liquidacionesAnuladas(anuladas)
                .tipoCambioOficial(new BigDecimal("3.7500"))
                .build();
    }

    private String generarCodigoLiquidacion() {
        int year = LocalDate.now().getYear();
        long count = liquidacionRepository.count() + 1;
        String codigo;
        do {
            codigo = String.format("LIQ-%d-%04d", year, count);
            count++;
        } while (liquidacionRepository.existsByCodigoLiquidacion(codigo));
        return codigo;
    }

    private LiquidacionFinancieraResponseDTO mapToResponse(LiquidacionFinanciera lf) {
        String nombreDocFactura = lf.getDocumentoFactura() != null ? lf.getDocumentoFactura().getNombreOriginal() : null;
        String nombreDocVoucher = lf.getDocumentoVoucher() != null ? lf.getDocumentoVoucher().getNombreOriginal() : null;

        String nombreLiq = null;
        if (lf.getUsuarioLiquidador() != null) {
            nombreLiq = lf.getUsuarioLiquidador().getNombres() + " " + lf.getUsuarioLiquidador().getApellidos();
        }

        return LiquidacionFinancieraResponseDTO.builder()
                .idLiquidacion(lf.getIdLiquidacion())
                .codigoLiquidacion(lf.getCodigoLiquidacion())
                .idExpediente(lf.getExpediente() != null ? lf.getExpediente().getIdExpediente() : null)
                .codigoExpediente(lf.getExpediente() != null ? lf.getExpediente().getCodigoExpediente() : null)
                .numeroDam(lf.getExpediente() != null ? lf.getExpediente().getNumeroDam() : null)
                .canalSunat(lf.getExpediente() != null && lf.getExpediente().getCanalSunat() != null ? lf.getExpediente().getCanalSunat().name() : null)
                .clienteRazonSocial(lf.getExpediente() != null && lf.getExpediente().getCliente() != null ? lf.getExpediente().getCliente().getRazonSocial() : null)
                .clienteNumeroDocumento(lf.getExpediente() != null && lf.getExpediente().getCliente() != null ? lf.getExpediente().getCliente().getNumeroDocumento() : null)
                .idUsuarioLiquidador(lf.getUsuarioLiquidador() != null ? lf.getUsuarioLiquidador().getIdUsuario() : null)
                .nombreLiquidador(nombreLiq)
                .totalTributosSunat(lf.getTotalTributosSunat())
                .gastosAlmacenPuerto(lf.getGastosAlmacenPuerto())
                .fleteTransporteLocal(lf.getFleteTransporteLocal())
                .honorariosAgencia(lf.getHonorariosAgencia())
                .subtotalImponible(lf.getSubtotalImponible())
                .igvMonto(lf.getIgvMonto())
                .aplicaDetraccion(lf.getAplicaDetraccion())
                .porcentajeDetraccion(lf.getPorcentajeDetraccion())
                .montoDetraccion(lf.getMontoDetraccion())
                .totalGeneralPen(lf.getTotalGeneralPen())
                .totalGeneralUsd(lf.getTotalGeneralUsd())
                .tipoCambio(lf.getTipoCambio())
                .estadoPago(lf.getEstadoPago())
                .numeroFactura(lf.getNumeroFactura())
                .idDocumentoFactura(lf.getDocumentoFactura() != null ? lf.getDocumentoFactura().getIdDocumento() : null)
                .nombreDocumentoFactura(nombreDocFactura)
                .metodoPago(lf.getMetodoPago())
                .bancoDestino(lf.getBancoDestino())
                .numeroOperacion(lf.getNumeroOperacion())
                .idDocumentoVoucher(lf.getDocumentoVoucher() != null ? lf.getDocumentoVoucher().getIdDocumento() : null)
                .nombreDocumentoVoucher(nombreDocVoucher)
                .fechaEmision(lf.getFechaEmision())
                .fechaPago(lf.getFechaPago())
                .observaciones(lf.getObservaciones())
                .build();
    }
}
