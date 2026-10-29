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
import pe.sga.entity.NotificacionCliente;
import pe.sga.repository.CotizacionPagoRepository;
import pe.sga.repository.CotizacionRepository;
import pe.sga.repository.IDocumentoAdjuntoRepository;
import pe.sga.repository.NotificacionClienteRepository;
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
    private final NotificacionClienteRepository notificacionClienteRepository;

    @Override
    @Transactional
    public CotizacionPagoResponseDTO registrarPago(CotizacionPagoRegistrarDTO dto, Integer idUsuarioRegistro) {
        Cotizacion cotizacion = cotizacionRepository.findById(dto.getIdCotizacion())
                .orElseThrow(() -> new ResourceNotFoundException("Cotización no encontrada con ID: " + dto.getIdCotizacion()));

        if (cotizacion.getEstado() != EstadoCotizacion.APROBADA) {
            throw new BadRequestException("Solo se pueden registrar pagos para cotizaciones que han sido APROBADAS técnicamente. Estado actual: " + cotizacion.getEstado());
        }

        Usuario usuario = null;
        if (idUsuarioRegistro != null && usuarioRepository.existsById(idUsuarioRegistro)) {
            usuario = usuarioRepository.findById(idUsuarioRegistro).get();
        } else {
            usuario = usuarioRepository.findAll().stream().findFirst()
                    .orElse(null);
        }

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

            // Notificación al portal de clientes B2B (Diagrama: Pago Aprobado / Habilitado para apertura de expediente)
            if (cotizacion != null && cotizacion.getCliente() != null) {
                NotificacionCliente notif = NotificacionCliente.builder()
                        .cliente(cotizacion.getCliente())
                        .titulo("Anticipo de Pago Validado: " + pago.getCodigoPago())
                        .mensaje("Su abono de " + pago.getMoneda() + " " + pago.getMontoAbonado()
                                + " (Operación N° " + pago.getNumeroOperacion() + ") para la cotización "
                                + cotizacion.getCodigoCotizacion()
                                + " ha sido verificado y aprobado por Finanzas. Su servicio queda expedito para la apertura del expediente aduanero.")
                        .tipo("PAGO")
                        .leido(false)
                        .enlace("/pagos")
                        .fechaCreacion(LocalDateTime.now())
                        .build();
                notificacionClienteRepository.save(notif);
            }
        } else if ("OBSERVAR".equals(accion) || "OBSERVADO".equals(accion)) {
            pago.setEstadoPago(EstadoPagoCotizacion.OBSERVADO);
            pago.setUsuarioFinanzas(finanzas);
            pago.setFechaValidacion(LocalDateTime.now());
            pago.setObservacionFinanzas(dto.getObservacionFinanzas());
            cotizacionPagoRepository.save(pago);

            recalcularFinanzasCotizacion(cotizacion);

            // Notificación al portal de clientes B2B (Diagrama BPMN: "Not. pago invalido")
            if (cotizacion != null && cotizacion.getCliente() != null) {
                String motivo = dto.getObservacionFinanzas() != null && !dto.getObservacionFinanzas().isBlank()
                        ? dto.getObservacionFinanzas()
                        : "El comprobante o datos de operación no coinciden con extractos bancarios.";
                NotificacionCliente notif = NotificacionCliente.builder()
                        .cliente(cotizacion.getCliente())
                        .titulo("Abono Bancario Observado por Finanzas: " + pago.getCodigoPago())
                        .mensaje("Su abono de " + pago.getMoneda() + " " + pago.getMontoAbonado()
                                + " (Operación N° " + pago.getNumeroOperacion() + ") ha sido observado por el área de Finanzas. "
                                + "Motivo: " + motivo + ". Ingrese a la sección de Cobros y Pagos para subsanar el voucher o corregir los datos.")
                        .tipo("PAGO")
                        .leido(false)
                        .enlace("/pagos")
                        .fechaCreacion(LocalDateTime.now())
                        .build();
                notificacionClienteRepository.save(notif);
            }
        } else if ("RECHAZAR".equals(accion) || "RECHAZADO".equals(accion)) {
            pago.setEstadoPago(EstadoPagoCotizacion.RECHAZADO);
            pago.setUsuarioFinanzas(finanzas);
            pago.setFechaValidacion(LocalDateTime.now());
            pago.setObservacionFinanzas(dto.getObservacionFinanzas());
            cotizacionPagoRepository.save(pago);

            recalcularFinanzasCotizacion(cotizacion);

            if (cotizacion != null && cotizacion.getCliente() != null) {
                String motivo = dto.getObservacionFinanzas() != null && !dto.getObservacionFinanzas().isBlank()
                        ? dto.getObservacionFinanzas()
                        : "Comprobante rechazado por Finanzas.";
                NotificacionCliente notif = NotificacionCliente.builder()
                        .cliente(cotizacion.getCliente())
                        .titulo("Abono Rechazado por Finanzas: " + pago.getCodigoPago())
                        .mensaje("Su comprobante de abono (Operación N° " + pago.getNumeroOperacion()
                                + ") fue rechazado. Motivo: " + motivo + ". Ingrese al portal para regularizar su pago.")
                        .tipo("PAGO")
                        .leido(false)
                        .enlace("/pagos")
                        .fechaCreacion(LocalDateTime.now())
                        .build();
                notificacionClienteRepository.save(notif);
            }
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
                BigDecimal montoEnUsd = p.getMontoAbonado() != null ? p.getMontoAbonado() : BigDecimal.ZERO;
                if ("PEN".equalsIgnoreCase(p.getMoneda())) {
                    BigDecimal tc = (p.getTipoCambio() != null && p.getTipoCambio().compareTo(BigDecimal.ZERO) > 0)
                            ? p.getTipoCambio()
                            : new BigDecimal("3.7500");
                    montoEnUsd = montoEnUsd.divide(tc, 2, RoundingMode.HALF_UP);
                }
                totalAbonado = totalAbonado.add(montoEnUsd);
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
        String prefix = String.format("PAG-COT-%d-", year);
        long nextSeq = cotizacionPagoRepository.findUltimoPagoPorPrefijo(prefix)
                .map(CotizacionPago::getCodigoPago)
                .map(cod -> {
                    try {
                        String numPart = cod.substring(prefix.length());
                        return Long.parseLong(numPart) + 1;
                    } catch (Exception e) {
                        return cotizacionPagoRepository.count() + 1;
                    }
                })
                .orElseGet(() -> cotizacionPagoRepository.count() + 1);

        String codigo;
        do {
            codigo = String.format("PAG-COT-%d-%04d", year, nextSeq);
            nextSeq++;
        } while (cotizacionPagoRepository.existsByCodigoPago(codigo));
        return codigo;
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
