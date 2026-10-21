package pe.sga.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.sga.dto.request.CotizacionPagoRegistrarDTO;
import pe.sga.dto.request.CotizacionPagoValidarDTO;
import pe.sga.dto.response.CotizacionPagoResponseDTO;
import pe.sga.entity.Cotizacion;
import pe.sga.entity.CotizacionPago;
import pe.sga.entity.DocumentoAdjunto;
import pe.sga.entity.Usuario;
import pe.sga.entity.enums.EstadoCotizacion;
import pe.sga.entity.enums.EstadoPagoCotizacion;
import pe.sga.exception.BadRequestException;
import pe.sga.exception.ResourceNotFoundException;
import pe.sga.repository.CotizacionPagoRepository;
import pe.sga.repository.CotizacionRepository;
import pe.sga.repository.IDocumentoAdjuntoRepository;
import pe.sga.repository.UsuarioRepository;
import pe.sga.service.ICotizacionPagoService;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CotizacionPagoServiceImpl implements ICotizacionPagoService {

    private final CotizacionPagoRepository cotizacionPagoRepository;
    private final CotizacionRepository cotizacionRepository;
    private final UsuarioRepository usuarioRepository;
    private final IDocumentoAdjuntoRepository documentoRepository;

    @Override
    @Transactional
    public CotizacionPagoResponseDTO registrarPago(CotizacionPagoRegistrarDTO dto, Integer idUsuarioRegistro) {
        Cotizacion cotizacion = cotizacionRepository.findById(dto.getIdCotizacion())
                .orElseThrow(() -> new ResourceNotFoundException("Cotización no encontrada con ID: " + dto.getIdCotizacion()));

        if (cotizacion.getEstado() != EstadoCotizacion.APROBADA) {
            throw new BadRequestException("Solo se pueden registrar pagos para cotizaciones que han sido APROBADAS técnicamente. Estado actual: " + cotizacion.getEstado());
        }

        Usuario usuario = usuarioRepository.findById(idUsuarioRegistro != null ? idUsuarioRegistro : 1)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario de registro no encontrado con ID: " + idUsuarioRegistro));

        DocumentoAdjunto documentoComprobante = null;
        if (dto.getIdDocumentoComprobante() != null) {
            documentoComprobante = documentoRepository.findById(dto.getIdDocumentoComprobante()).orElse(null);
        }

        BigDecimal tipoCambio = dto.getTipoCambio() != null && dto.getTipoCambio().compareTo(BigDecimal.ZERO) > 0
                ? dto.getTipoCambio()
                : new BigDecimal("3.7500");

        BigDecimal montoEquivalentePen;
        if ("PEN".equalsIgnoreCase(dto.getMoneda())) {
            montoEquivalentePen = dto.getMontoAbonado();
        } else {
            montoEquivalentePen = dto.getMontoAbonado().multiply(tipoCambio).setScale(2, RoundingMode.HALF_UP);
        }

        String codigoPago = generarCodigoPago();

        CotizacionPago pago = CotizacionPago.builder()
                .cotizacion(cotizacion)
                .codigoPago(codigoPago)
                .tipoAbono(dto.getTipoAbono())
                .montoAbonado(dto.getMontoAbonado())
                .moneda(dto.getMoneda().toUpperCase())
                .tipoCambio(tipoCambio)
                .montoEquivalentePen(montoEquivalentePen)
                .metodoPago(dto.getMetodoPago())
                .bancoOrigen(dto.getBancoOrigen())
                .bancoDestino(dto.getBancoDestino())
                .numeroOperacion(dto.getNumeroOperacion())
                .fechaOperacion(dto.getFechaOperacion())
                .documentoComprobante(documentoComprobante)
                .estadoPago(EstadoPagoCotizacion.PENDIENTE_REVISION)
                .usuarioRegistro(usuario)
                .fechaRegistro(LocalDateTime.now())
                .build();

        CotizacionPago guardado = cotizacionPagoRepository.save(pago);

        // Si la cotización no tiene pago confirmado, se actualiza a PENDIENTE_REVISION
        if (!"ADELANTO_APROBADO".equals(cotizacion.getEstadoPago()) && !"PAGADO_TOTAL".equals(cotizacion.getEstadoPago())) {
            cotizacion.setEstadoPago("PENDIENTE_REVISION");
            cotizacionRepository.save(cotizacion);
        }

        return mapToResponse(guardado);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CotizacionPagoResponseDTO> listarPorCotizacion(Integer idCotizacion) {
        return cotizacionPagoRepository.findByCotizacion_IdCotizacionOrderByFechaRegistroDesc(idCotizacion).stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<CotizacionPagoResponseDTO> listarPorEstado(EstadoPagoCotizacion estadoPago) {
        return cotizacionPagoRepository.findByEstadoPagoOrderByFechaRegistroDesc(estadoPago).stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<CotizacionPagoResponseDTO> listarTodos() {
        return cotizacionPagoRepository.findAll().stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    @Transactional
    public CotizacionPagoResponseDTO validarPago(Integer idPago, CotizacionPagoValidarDTO dto, Integer idUsuarioFinanzas) {
        CotizacionPago pago = cotizacionPagoRepository.findById(idPago)
                .orElseThrow(() -> new ResourceNotFoundException("Pago no encontrado con ID: " + idPago));

        Usuario finanzas = usuarioRepository.findById(idUsuarioFinanzas != null ? idUsuarioFinanzas : 1)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario de finanzas no encontrado con ID: " + idUsuarioFinanzas));

        String accion = dto.getAccion().toUpperCase();
        Cotizacion cotizacion = pago.getCotizacion();

        if ("APROBAR".equals(accion) || "APROBADO".equals(accion)) {
            pago.setEstadoPago(EstadoPagoCotizacion.APROBADO);
            pago.setUsuarioFinanzas(finanzas);
            pago.setFechaValidacion(LocalDateTime.now());
            pago.setObservacionFinanzas(dto.getObservacionFinanzas());
            cotizacionPagoRepository.save(pago);

            // Recalcular saldo y totales de cotización
            recalcularFinanzasCotizacion(cotizacion);
        } else if ("OBSERVAR".equals(accion) || "OBSERVADO".equals(accion)) {
            pago.setEstadoPago(EstadoPagoCotizacion.OBSERVADO);
            pago.setUsuarioFinanzas(finanzas);
            pago.setFechaValidacion(LocalDateTime.now());
            pago.setObservacionFinanzas(dto.getObservacionFinanzas());
            cotizacionPagoRepository.save(pago);

            recalcularFinanzasCotizacion(cotizacion);
        } else if ("RECHAZAR".equals(accion) || "RECHAZADO".equals(accion)) {
            pago.setEstadoPago(EstadoPagoCotizacion.RECHAZADO);
            pago.setUsuarioFinanzas(finanzas);
            pago.setFechaValidacion(LocalDateTime.now());
            pago.setObservacionFinanzas(dto.getObservacionFinanzas());
            cotizacionPagoRepository.save(pago);

            recalcularFinanzasCotizacion(cotizacion);
        } else {
            throw new BadRequestException("Acción no válida: " + dto.getAccion() + ". Las opciones son APROBAR, OBSERVAR o RECHAZAR.");
        }

        return mapToResponse(pago);
    }

    @Override
    @Transactional(readOnly = true)
    public CotizacionPagoResponseDTO buscarPorId(Integer idPago) {
        CotizacionPago pago = cotizacionPagoRepository.findById(idPago)
                .orElseThrow(() -> new ResourceNotFoundException("Pago no encontrado con ID: " + idPago));
        return mapToResponse(pago);
    }

    private void recalcularFinanzasCotizacion(Cotizacion cotizacion) {
        List<CotizacionPago> pagos = cotizacionPagoRepository.findByCotizacion_IdCotizacionOrderByFechaRegistroDesc(cotizacion.getIdCotizacion());

        BigDecimal totalAbonado = BigDecimal.ZERO;
        boolean tieneAprobados = false;
        boolean tienePendientes = false;
        boolean tieneObservados = false;

        for (CotizacionPago p : pagos) {
            if (p.getEstadoPago() == EstadoPagoCotizacion.APROBADO) {
                totalAbonado = totalAbonado.add(p.getMontoAbonado());
                tieneAprobados = true;
            } else if (p.getEstadoPago() == EstadoPagoCotizacion.PENDIENTE_REVISION) {
                tienePendientes = true;
            } else if (p.getEstadoPago() == EstadoPagoCotizacion.OBSERVADO) {
                tieneObservados = true;
            }
        }

        BigDecimal totalCotizado = cotizacion.getTotalCotizado() != null ? cotizacion.getTotalCotizado() : BigDecimal.ZERO;
        BigDecimal saldoPendiente = totalCotizado.subtract(totalAbonado);
        if (saldoPendiente.compareTo(BigDecimal.ZERO) < 0) {
            saldoPendiente = BigDecimal.ZERO;
        }

        cotizacion.setTotalAbonado(totalAbonado);
        cotizacion.setSaldoPendiente(saldoPendiente);

        if (totalAbonado.compareTo(totalCotizado) >= 0 && totalCotizado.compareTo(BigDecimal.ZERO) > 0) {
            cotizacion.setEstadoPago("PAGADO_TOTAL");
        } else if (tieneAprobados) {
            cotizacion.setEstadoPago("ADELANTO_APROBADO");
        } else if (tienePendientes) {
            cotizacion.setEstadoPago("PENDIENTE_REVISION");
        } else if (tieneObservados) {
            cotizacion.setEstadoPago("OBSERVADO");
        } else {
            cotizacion.setEstadoPago("SIN_PAGO");
        }

        cotizacionRepository.save(cotizacion);
    }

    private String generarCodigoPago() {
        int year = LocalDate.now().getYear();
        long count = cotizacionPagoRepository.count() + 1;
        return String.format("PAG-COT-%d-%04d", year, count);
    }

    private CotizacionPagoResponseDTO mapToResponse(CotizacionPago p) {
        Cotizacion c = p.getCotizacion();
        return CotizacionPagoResponseDTO.builder()
                .idPago(p.getIdPago())
                .idCotizacion(c != null ? c.getIdCotizacion() : null)
                .codigoCotizacion(c != null ? c.getCodigoCotizacion() : null)
                .clienteRazonSocial(c != null && c.getCliente() != null ? c.getCliente().getRazonSocial() : null)
                .clienteNumeroDocumento(c != null && c.getCliente() != null ? c.getCliente().getNumeroDocumento() : null)
                .totalCotizado(c != null ? c.getTotalCotizado() : null)
                .codigoPago(p.getCodigoPago())
                .tipoAbono(p.getTipoAbono())
                .montoAbonado(p.getMontoAbonado())
                .moneda(p.getMoneda())
                .tipoCambio(p.getTipoCambio())
                .montoEquivalentePen(p.getMontoEquivalentePen())
                .metodoPago(p.getMetodoPago())
                .bancoOrigen(p.getBancoOrigen())
                .bancoDestino(p.getBancoDestino())
                .numeroOperacion(p.getNumeroOperacion())
                .fechaOperacion(p.getFechaOperacion())
                .idDocumentoComprobante(p.getDocumentoComprobante() != null ? p.getDocumentoComprobante().getIdDocumento() : null)
                .documentoComprobanteNombre(p.getDocumentoComprobante() != null ? p.getDocumentoComprobante().getNombreOriginal() : null)
                .documentoComprobanteUrl(p.getDocumentoComprobante() != null ? p.getDocumentoComprobante().getRutaAlmacenamiento() : null)
                .estadoPago(p.getEstadoPago())
                .idUsuarioFinanzas(p.getUsuarioFinanzas() != null ? p.getUsuarioFinanzas().getIdUsuario() : null)
                .usuarioFinanzasNombre(p.getUsuarioFinanzas() != null ? p.getUsuarioFinanzas().getNombreCompleto() : null)
                .fechaValidacion(p.getFechaValidacion())
                .observacionFinanzas(p.getObservacionFinanzas())
                .idUsuarioRegistro(p.getUsuarioRegistro() != null ? p.getUsuarioRegistro().getIdUsuario() : null)
                .usuarioRegistroNombre(p.getUsuarioRegistro() != null ? p.getUsuarioRegistro().getNombreCompleto() : null)
                .fechaRegistro(p.getFechaRegistro())
                .build();
    }
}
